/*
 * Copyright 2018-2019 the Justify authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.leadpony.justify.internal.keyword.combiner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.leadpony.justify.api.Evaluator;
import org.leadpony.justify.api.EvaluatorContext;
import org.leadpony.justify.api.InstanceType;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.ProblemDispatcher;
import org.leadpony.justify.internal.base.json.ParserEvents;
import org.leadpony.justify.internal.evaluator.AbstractConjunctiveItemsEvaluator;
import org.leadpony.justify.internal.evaluator.AbstractDisjunctiveItemsEvaluator;
import org.leadpony.justify.internal.evaluator.EvaluatorDecorator;
import org.leadpony.justify.internal.keyword.ArrayKeyword;
import org.leadpony.justify.internal.keyword.Evaluatable;
import org.leadpony.justify.internal.keyword.Keyword;

/**
 * "items" combiner.
 *
 * @author leadpony
 */
public abstract class Items extends Combiner implements ArrayKeyword {

    public static Items of(JsonSchema subschema) {
        return new BroadcastItems(subschema);
    }

    public static Items of(List<JsonSchema> subschemas) {
        return new DiscreteItems(subschemas);
    }

    @Override
    public String name() {
        return "items";
    }

    /**
     * "items" keyword with single schema.
     *
     * @author leadpony
     */
    static class BroadcastItems extends Items {

        private final JsonSchema subschema;

        BroadcastItems(JsonSchema subschema) {
            this.subschema = subschema;
        }

        @Override
        protected Evaluator doCreateEvaluator(EvaluatorContext context, InstanceType type) {
            if (subschema == JsonSchema.FALSE) {
                return createForbiddenItemsEvaluator(context);
            } else {
                return createItemsEvaluator(context);
            }
        }

        @Override
        protected Evaluator doCreateNegatedEvaluator(EvaluatorContext context, InstanceType type) {
            if (subschema == JsonSchema.TRUE || subschema == JsonSchema.EMPTY) {
                return createNegatedForbiddenItemsEvaluator(context);
            } else {
                return createNegatedItemsEvaluator(context);
            }
        }

        @Override
        public void addToJson(JsonObjectBuilder builder, JsonBuilderFactory builderFactory) {
            builder.add(name(), subschema.toJson());
        }

        @Override
        public boolean hasSubschemas() {
            return true;
        }

        @Override
        public Stream<JsonSchema> getSubschemas() {
            return Stream.of(subschema);
        }

        @Override
        public JsonSchema getSubschema(Iterator<String> jsonPointer) {
            return subschema;
        }

        private Evaluator createItemsEvaluator(EvaluatorContext context) {
            JsonSchema subschema = this.subschema;
            return new AbstractConjunctiveItemsEvaluator(context) {
                @Override
                public void updateChildren(Event event, JsonParser parser) {
                    if (ParserEvents.isValue(event)) {
                        InstanceType type = ParserEvents.toBroadInstanceType(event);
                        append(subschema.createEvaluator(getContext(), type));
                    }
                }
            };
        }

        private Evaluator createNegatedItemsEvaluator(EvaluatorContext context) {
            JsonSchema subschema = this.subschema;
            return new AbstractDisjunctiveItemsEvaluator(context, this) {
                @Override
                public void updateChildren(Event event, JsonParser parser) {
                    if (ParserEvents.isValue(event)) {
                        InstanceType type = ParserEvents.toBroadInstanceType(event);
                        append(subschema.createNegatedEvaluator(context, type));
                    }
                }
            };
        }

        private Evaluator createForbiddenItemsEvaluator(EvaluatorContext context) {
            return new AbstractConjunctiveItemsEvaluator(context) {
                private int itemIndex;
                @Override
                public void updateChildren(Event event, JsonParser parser) {
                    if (ParserEvents.isValue(event)) {
                        append(new RedundantItemEvaluator(context, itemIndex++, subschema));
                    }
                }
            };
        }

        private Evaluator createNegatedForbiddenItemsEvaluator(EvaluatorContext context) {
            return new AbstractDisjunctiveItemsEvaluator(context, this) {
                private int itemIndex;
                @Override
                public void updateChildren(Event event, JsonParser parser) {
                    if (ParserEvents.isValue(event)) {
                        append(new RedundantItemEvaluator(context, itemIndex++, subschema));
                    }
                }
            };
        }
    }

    /**
     * "items" keyword with array of subschemas.
     *
     * @author leadpony
     */
    static class DiscreteItems extends Items {

        private final List<JsonSchema> subschemas;
        private JsonSchema defaultSchema = JsonSchema.TRUE;
        private List<JsonValue> defaultValues;

        DiscreteItems(List<JsonSchema> subschemas) {
            this.subschemas = subschemas;
            this.defaultValues = findDefaultValues(subschemas);
        }

        @Override
        protected Evaluator doCreateEvaluator(EvaluatorContext context, InstanceType type) {
            return decorateEvaluator(createItemsEvaluator(context), context);
        }

        @Override
        protected Evaluator doCreateNegatedEvaluator(EvaluatorContext context, InstanceType type) {
            return decorateEvaluator(createNegatedItemsEvaluator(context), context);
        }

        @Override
        public void addToJson(JsonObjectBuilder builder, JsonBuilderFactory builderFactory) {
            JsonArrayBuilder itemsBuilder = builderFactory.createArrayBuilder();
            subschemas.stream().map(JsonSchema::toJson).forEachOrdered(itemsBuilder::add);
            builder.add(name(), itemsBuilder.build());
        }

        @Override
        public void addToEvaluatables(List<Evaluatable> evaluatables, Map<String, Keyword> keywords) {
            if (keywords.containsKey("additionalItems")) {
                AdditionalItems additionalItems = (AdditionalItems)keywords.get("additionalItems");
                this.defaultSchema = additionalItems.getSubschema();
            }
            evaluatables.add(this);
        }

        @Override
        public boolean hasSubschemas() {
            return !subschemas.isEmpty();
        }

        @Override
        public Stream<JsonSchema> getSubschemas() {
            return this.subschemas.stream();
        }

        @Override
        public JsonSchema getSubschema(Iterator<String> jsonPointer) {
            if (jsonPointer.hasNext()) {
                try {
                    int index = Integer.parseInt(jsonPointer.next());
                    if (index < subschemas.size()) {
                        return subschemas.get(index);
                    }
                } catch (NumberFormatException e) {
                }
            }
            return null;
        }

        private JsonSchema findSubschemaAt(int itemIndex) {
            if (itemIndex < subschemas.size()) {
                return subschemas.get(itemIndex);
            } else {
                return defaultSchema;
            }
        }

        private Evaluator createSubschemaEvaluator(EvaluatorContext context, int itemIndex, JsonSchema subschema, InstanceType type) {
            if (subschema == JsonSchema.FALSE) {
                return new RedundantItemEvaluator(context, itemIndex, subschema);
            } else {
                return subschema.createEvaluator(context, type);
            }
        }

        private Evaluator createNegatedSubschemaEvaluator(EvaluatorContext context, int itemIndex, JsonSchema subschema, InstanceType type) {
            if (subschema == JsonSchema.TRUE || subschema == JsonSchema.EMPTY) {
                return new RedundantItemEvaluator(context, itemIndex, subschema);
            } else {
                return subschema.createNegatedEvaluator(context, type);
            }
        }

        private Evaluator createItemsEvaluator(EvaluatorContext context) {
            return new AbstractConjunctiveItemsEvaluator(context) {
                private int itemIndex;
                @Override
                public void updateChildren(Event event, JsonParser parser) {
                    if (ParserEvents.isValue(event)) {
                        InstanceType type = ParserEvents.toBroadInstanceType(event);
                        JsonSchema subschema = findSubschemaAt(itemIndex);
                        append(createSubschemaEvaluator(context, itemIndex, subschema, type));
                        ++itemIndex;
                    }
                }
            };
        }

        private Evaluator createNegatedItemsEvaluator(EvaluatorContext context) {
            return new AbstractDisjunctiveItemsEvaluator(context, this) {
                private int itemIndex;
                @Override
                public void updateChildren(Event event, JsonParser parser) {
                    if (ParserEvents.isValue(event)) {
                        InstanceType type = ParserEvents.toBroadInstanceType(event);
                        JsonSchema subschema = findSubschemaAt(itemIndex);
                        append(createNegatedSubschemaEvaluator(context, itemIndex, subschema, type));
                        ++itemIndex;
                    }
                }
            };
        }

        private List<JsonValue> findDefaultValues(List<JsonSchema> subschemas) {
            for (int i = subschemas.size() - 1; i >= 0; i--) {
                if (subschemas.get(i).containsKeyword("default")) {
                    List<JsonValue> values = new ArrayList<>(i + 1);
                    for (int j = 0; j <= i; j++) {
                        values.add(subschemas.get(j).defaultValue());
                    }
                    return values;
                }
            }
            return null;
        }

        private Evaluator decorateEvaluator(Evaluator evaluator, EvaluatorContext context) {
            if (context.acceptsDefaultValues() && defaultValues != null) {
                evaluator = new ItemsDefaultEvaluator(evaluator, context, defaultValues);
            }
            return evaluator;
        }
    }

    private static class ItemsDefaultEvaluator extends EvaluatorDecorator {

        private final List<JsonValue> defaultValues;
        private int size;

        ItemsDefaultEvaluator(Evaluator evaluator, EvaluatorContext context, List<JsonValue> defaultValues) {
            super(evaluator, context);
            this.defaultValues = defaultValues;
        }

        @Override
        public Result evaluate(Event event, int depth, ProblemDispatcher dispatcher) {
            Result result = super.evaluate(event, depth, dispatcher);
            if (depth == 1 && ParserEvents.isValue(event)) {
                ++size;
            } else if (depth == 0 && event == Event.END_ARRAY) {
                supplyDefaultValues(size);
                return result;
            }
            return Result.PENDING;
        }

        private void supplyDefaultValues(int size) {
            if (size < defaultValues.size()) {
                List<JsonValue> valuesToPut= new ArrayList<>();
                int i = size;
                while (i < defaultValues.size()) {
                    JsonValue value = defaultValues.get(i++);
                    if (value != null) {
                        valuesToPut.add(value);
                    } else {
                        break;
                    }
                }
                if (!valuesToPut.isEmpty()) {
                    getContext().putDefaultItems(valuesToPut);
                }
            }
        }
    }
}
