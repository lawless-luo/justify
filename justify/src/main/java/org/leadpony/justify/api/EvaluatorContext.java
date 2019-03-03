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
package org.leadpony.justify.api;

import javax.json.JsonPointer;
import javax.json.stream.JsonParser;

/**
 * A context shared by all evaluators participating in the current validation.
 *
 * <p>
 * Note that this type is not intended to be used directly by end users.
 * </p>
 *
 * @author leadpony
 */
public interface EvaluatorContext {

    /**
     * Returns the parser being used while validating the instance.
     *
     * @return the current parser, never be {@code null}.
     */
    JsonParser getParser();

    /**
     * Returns the current location in the instance as a JSON pointer.
     *
     * @return the JSON pointer which points to the current location in the
     *         instance.
     */
    JsonPointer getPointer();
}