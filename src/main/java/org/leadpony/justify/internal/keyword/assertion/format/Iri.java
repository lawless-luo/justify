/*
 * Copyright 2018 the Justify authors.
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

package org.leadpony.justify.internal.keyword.assertion.format;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Format attribute representing "iri" attribute.
 * 
 * @author leadpony
 * 
 * @see <a href="https://tools.ietf.org/html/rfc3987">
 * "Internationalized Resource Identifiers (IRIs)", RFC 3987</a>
 */
public class Iri implements StringFormatAttribute {

    @Override
    public String name() {
        return "iri";
    }

    @Override
    public boolean test(String value) {
        try {
            URI uri = new URI(value);
            return uri.isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}