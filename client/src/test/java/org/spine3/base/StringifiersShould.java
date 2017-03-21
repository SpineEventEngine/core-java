/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spine3.base;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.junit.Test;
import org.spine3.test.identifiers.IdWithPrimitiveFields;
import org.spine3.test.types.Task;
import org.spine3.type.ParametrizedTypeImpl;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Map;
import java.util.Random;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

@SuppressWarnings({"SerializableNonStaticInnerClassWithoutSerialVersionUID",
        "SerializableInnerClassWithNonSerializableOuterClass",
        "DuplicateStringLiteralInspection"})
// It is OK for test methods.
public class StringifiersShould {

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Stringifiers.class);
    }

    private static final Stringifier<IdWithPrimitiveFields> ID_TO_STRING_CONVERTER =
            new Stringifier<IdWithPrimitiveFields>() {
                @Override
                protected String toString(IdWithPrimitiveFields id) {
                    return id.getName();
                }

                @Override
                protected IdWithPrimitiveFields fromString(String str) {
                    return IdWithPrimitiveFields.newBuilder()
                                                .setName(str)
                                                .build();
                }
            };

    @Test
    public void convert_to_string_registered_id_message_type() {
        StringifierRegistry.getInstance()
                           .register(ID_TO_STRING_CONVERTER, IdWithPrimitiveFields.class);

        final String testId = "testId 123456";
        final IdWithPrimitiveFields id = IdWithPrimitiveFields.newBuilder()
                                                              .setName(testId)
                                                              .build();
        final String result = idToString(id);
        assertEquals(testId, result);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    // OK as these are standard Stringifiers we add ourselves.
    @Test
    public void handle_null_in_standard_converters() {
        final StringifierRegistry registry = StringifierRegistry.getInstance();

        assertNull(registry.get(Timestamp.class)
                           .get()
                           .convert(null));
        assertNull(registry.get(EventId.class)
                           .get()
                           .convert(null));
        assertNull(registry.get(CommandId.class)
                           .get()
                           .convert(null));
    }

    @Test
    public void return_false_on_attempt_to_find_unregistered_type() {
        assertFalse(StringifierRegistry.getInstance()
                                       .hasStringifierFor(Random.class));
    }

    @Test
    public void convert_string_to_map() throws ParseException {
        final String rawMap = "1\\:1972-01-01T10:00:20.021-05:00";
        final Type type = ParametrizedTypeImpl.make(Map.class,
                                                    new Class[]{Long.class, Timestamp.class});
        final Stringifier<Map<Long, Timestamp>> stringifier =
                new Stringifiers.MapStringifier<>(Long.class, Timestamp.class);
        StringifierRegistry.getInstance()
                           .register(stringifier, type);
        final Map<Long, Timestamp> actualMap = Stringifiers.fromString(rawMap, type);
        final Map<Long, Timestamp> expectedMap = newHashMap();
        expectedMap.put(1L, Timestamps.parse("1972-01-01T10:00:20.021-05:00"));
        assertThat(actualMap, is(expectedMap));
    }

    @Test
    public void convert_map_to_string() {
        final Map<String, Integer> mapToConvert = createTestMap();
        final Type type = ParametrizedTypeImpl.make(Map.class,
                                                    new Class[]{String.class, Integer.class});
        final Stringifiers.MapStringifier<String, Integer> stringifier =
                new Stringifiers.MapStringifier<>(String.class, Integer.class);
        StringifierRegistry.getInstance()
                           .register(stringifier, type);
        final String convertedMap = Stringifiers.toString(mapToConvert, type);
        assertEquals(mapToConvert.toString(), convertedMap);
    }

    @Test(expected = IllegalConversionArgumentException.class)
    public void throw_exception_when_passed_parameter_does_not_match_expected_format() {
        final String incorrectRawMap = "first\\:1\\,second\\:2";
        final Type type = ParametrizedTypeImpl.make(Map.class,
                                                    new Class[]{Integer.class, Integer.class});
        final Stringifiers.MapStringifier<Integer, Integer> stringifier =
                new Stringifiers.MapStringifier<>(Integer.class, Integer.class);
        StringifierRegistry.getInstance()
                           .register(stringifier, type);
        Stringifiers.fromString(incorrectRawMap, type);
    }

    @Test(expected = MissingStringifierException.class)
    public void throw_exception_when_required_stringifier_is_not_found() {
        final Type type = ParametrizedTypeImpl.make(Map.class, new Class[]{Long.class, Task.class});
        Stringifiers.fromString("1\\:1", type);
    }

    @Test(expected = IllegalConversionArgumentException.class)
    public void throw_exception_when_occurred_exception_during_conversion() {
        final Type type = ParametrizedTypeImpl.make(Map.class,
                                                    new Class[]{Task.class, Long.class});
        final Stringifiers.MapStringifier<Task, Long> stringifier =
                new Stringifiers.MapStringifier<>(Task.class, Long.class);
        StringifierRegistry.getInstance()
                           .register(stringifier, type);
        Stringifiers.fromString("first\\:first\\:first", type);
    }

    @Test(expected = IllegalConversionArgumentException.class)
    public void throw_exception_when_key_value_delimiter_is_wrong() {
        final Type type = ParametrizedTypeImpl.make(Map.class,
                                                    new Class[]{Long.class, Long.class});
        final Stringifiers.MapStringifier<Long, Long> stringifier =
                new Stringifiers.MapStringifier<>(Long.class, Long.class);
        StringifierRegistry.getInstance()
                           .register(stringifier, type);
        Stringifiers.fromString("1\\-1", type);
    }

    @Test
    public void convert_map_with_custom_delimiter() {
        final String rawMap = "first\\:1\\|second\\:2\\|third\\:3";
        final Type type = ParametrizedTypeImpl.make(Map.class,
                                                    new Class[]{String.class, Integer.class});
        final Stringifier<Map<String, Integer>> stringifier =
                new Stringifiers.MapStringifier<>(String.class, Integer.class, "|");

        StringifierRegistry.getInstance()
                           .register(stringifier, type);

        final Map<String, Integer> convertedMap = Stringifiers.fromString(rawMap, type);
        assertThat(convertedMap, is(createTestMap()));
    }

    private static Map<String, Integer> createTestMap() {
        final Map<String, Integer> expectedMap = newHashMap();
        expectedMap.put("first", 1);
        expectedMap.put("second", 2);
        expectedMap.put("third", 3);
        return expectedMap;
    }

    @Test
    public void pass_the_null_tolerance_check() {
        final NullPointerTester tester = new NullPointerTester();
        tester.testStaticMethods(Stringifiers.class, NullPointerTester.Visibility.PACKAGE);
    }

    @SuppressWarnings("EmptyClass") // is the part of the test.
    @Test(expected = MissingStringifierException.class)
    public void raise_exception_on_missing_stringifer() {
        Stringifiers.toString(new Object() {
        });
    }
}
