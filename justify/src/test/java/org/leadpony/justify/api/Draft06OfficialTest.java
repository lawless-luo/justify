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

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.leadpony.justify.internal.annotation.Spec;

/**
 * The official test suite tests for Draft-06.
 *
 * @author leadpony
 */
@Spec(SpecVersion.DRAFT_06)
public class Draft06OfficialTest extends AbstractOfficialTest {

    private static final String[] mandatory = {
    };

    public static Stream<Fixture> mandatory() {
        return generateFixtures(mandatory);
    }

    private static final String[] optional = {
            "optional/bignum.json",
            // Draft-06 does not have the "regex" format.
            //"optional/ecmascript-regex.json",
            "optional/format.json",
            "optional/zeroTerminatedFloats.json",
    };

    public static Stream<Fixture> optional() {
        return generateFixtures(optional);
    }

    @ParameterizedTest
    @MethodSource("optional")
    public void testOptional(Fixture fixture) {
        test(fixture);
    }
}
