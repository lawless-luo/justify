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
package org.leadpony.justify.internal.schema.binding;

import java.util.ArrayList;
import java.util.List;

import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.internal.keyword.Keyword;

/**
 * @author leadpony
 */
abstract class AbstractSchemaListBinder extends AbstractBinder {

    @Override
    public void fromJson(JsonParser parser, BindingContext context) {
        Event event = parser.next();
        if (event == Event.START_ARRAY) {
            List<JsonSchema> subschemas = new ArrayList<>();
            while ((event = parser.next()) != Event.END_ARRAY) {
                if (canReadSubschema(event)) {
                    subschemas.add(context.readSchema(event));
                } else {
                    skipValue(event, parser);
                }
            }
            if (!subschemas.isEmpty()) {
                context.addKeyword(createKeyword(subschemas));
            }
        } else {
            skipValue(event, parser);
        }
    }

    public abstract Keyword createKeyword(List<JsonSchema> subschemas);
}
