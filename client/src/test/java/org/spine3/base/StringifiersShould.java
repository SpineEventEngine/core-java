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

import com.google.common.reflect.TypeToken;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.junit.Test;
import org.spine3.protobuf.Timestamps2;
import org.spine3.test.identifiers.IdWithPrimitiveFields;
import org.spine3.test.identifiers.TimestampFieldId;
import org.spine3.test.types.Task;
import org.spine3.validate.IllegalConversionArgumentException;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;
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
                protected String doForward(IdWithPrimitiveFields id) {
                    return id.getName();
                }

                @Override
                protected IdWithPrimitiveFields doBackward(String s) {
                    return IdWithPrimitiveFields.newBuilder()
                                                .setName(s)
                                                .build();
                }
            };

    @Test
    public void convert_to_string_registered_id_message_type() {
        StringifierRegistry.getInstance()
                           .register(TypeToken.of(IdWithPrimitiveFields.class),
                                     ID_TO_STRING_CONVERTER);

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

        assertNull(registry.get(TypeToken.of(Timestamp.class))
                           .get()
                           .convert(null));
        assertNull(registry.get(TypeToken.of(EventId.class))
                           .get()
                           .convert(null));
        assertNull(registry.get(TypeToken.of(CommandId.class))
                           .get()
                           .convert(null));
    }

    @Test
    public void return_false_on_attempt_to_find_unregistered_type() {
        assertFalse(StringifierRegistry.getInstance()
                                       .hasStringifierFor(TypeToken.of(Random.class)));
    }

    @Test
    public void convert_command_id_to_string() {
        final CommandId id = Commands.generateId();

        final String actual = Stringifiers.toString(id, TypeToken.of(CommandId.class));
        assertEquals(idToString(id), actual);
    }

    @Test
    public void convert_string_to_command_id() {
        final String id = newUuid();
        final CommandId expected = CommandId.newBuilder()
                                            .setUuid(id)
                                            .build();

        final CommandId actual = Stringifiers.parse(id, TypeToken.of(CommandId.class));
        assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_try_to_convert_inappropriate_string_to_timestamp() {
        final String time = Timestamps2.getCurrentTime()
                                       .toString();
        Stringifiers.parse(time, TypeToken.of(Timestamp.class));
    }

    @Test
    public void convert_string_to_timestamp() throws ParseException {
        final String date = "1972-01-01T10:00:20.021-05:00";
        final Timestamp expected = Timestamps.parse(date);
        final Timestamp actual = Stringifiers.parse(date, TypeToken.of(Timestamp.class));
        assertEquals(expected, actual);
    }

    @Test(expected = IllegalConversionArgumentException.class)
    public void throw_exception_when_string_cannot_be_converted_to_timestamp() throws
                                                                               ParseException {
        final String date = "incorrect date";
        Stringifiers.parse(date, TypeToken.of(Timestamp.class));
    }

    @Test
    public void convert_timestamp_to_string() {
        final Timestamp currentTime = getCurrentTime();
        final TimestampFieldId id = TimestampFieldId.newBuilder()
                                                    .setId(currentTime)
                                                    .build();
        final String expected = Stringifiers.toString(currentTime, TypeToken.of(Timestamp.class));
        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_event_id_to_string() {
        final EventId id = Events.generateId();
        final String actual = Stringifiers.toString(id, TypeToken.of(EventId.class));

        assertEquals(idToString(id), actual);
    }

    @Test
    public void convert_string_to_event_id() {
        final String id = newUuid();
        final EventId expected = EventId.newBuilder()
                                        .setUuid(id)
                                        .build();
        final EventId actual = Stringifiers.parse(id, TypeToken.of(EventId.class));
        assertEquals(expected, actual);
    }

    @Test
    public void convert_string_to_map() throws ParseException {
        final String rawMap = "1\\:1972-01-01T10:00:20.021-05:00";
        final TypeToken<Map<Long, Timestamp>> typeToken = new TypeToken<Map<Long, Timestamp>>() {
        };
        final Stringifier<Map<Long, Timestamp>> stringifier =
                new Stringifiers.MapStringifier<>(Long.class, Timestamp.class);
        StringifierRegistry.getInstance()
                           .register(typeToken, stringifier);
        final Map<Long, Timestamp> actualMap = Stringifiers.parse(rawMap, typeToken);
        final Map<Long, Timestamp> expectedMap = newHashMap();
        expectedMap.put(1L, Timestamps.parse("1972-01-01T10:00:20.021-05:00"));
        assertThat(actualMap, is(expectedMap));
    }

    @Test
    public void convert_map_to_string() {
        final Map<String, Integer> mapToConvert = createTestMap();
        final TypeToken<Map<String, Integer>> typeToken = new TypeToken<Map<String, Integer>>() {
        };
        final String convertedMap = Stringifiers.toString(mapToConvert, typeToken);
        assertEquals(mapToConvert.toString(), convertedMap);
    }

    @Test(expected = IllegalConversionArgumentException.class)
    public void throw_exception_when_passed_parameter_does_not_match_expected_format() {
        final String incorrectRawMap = "first\\:1\\,second\\:2";
        final TypeToken<Map<Integer, Integer>> typeToken = new TypeToken<Map<Integer, Integer>>() {
        };
        final Stringifiers.MapStringifier<Integer, Integer> stringifier =
                new Stringifiers.MapStringifier<>(Integer.class, Integer.class);
        StringifierRegistry.getInstance()
                           .register(typeToken, stringifier);
        Stringifiers.parse(incorrectRawMap, typeToken);
    }

    @Test(expected = IllegalConversionArgumentException.class)
    public void throw_exception_when_required_stringifier_is_not_found() {
        final TypeToken<Map<Long, Task>> typeToken = new TypeToken<Map<Long, Task>>() {
        };
        Stringifiers.parse("1\\:1", typeToken);
    }

    @Test(expected = IllegalConversionArgumentException.class)
    public void throw_exception_when_occurred_exception_during_conversion() {
        final TypeToken<Map<Task, Long>> typeToken = new TypeToken<Map<Task, Long>>() {
        };
        final Stringifiers.MapStringifier<Task, Long> stringifier =
                new Stringifiers.MapStringifier<>(Task.class, Long.class);
        StringifierRegistry.getInstance()
                           .register(typeToken, stringifier);
        Stringifiers.parse("first\\:first\\:first", typeToken);
    }

    @Test(expected = IllegalConversionArgumentException.class)
    public void throw_exception_when_key_value_delimiter_is_wrong() {
        final TypeToken<Map<Long, Long>> typeToken = new TypeToken<Map<Long, Long>>() {
        };
        Stringifiers.parse("1\\-1", typeToken);
    }

    @Test
    public void convert_map_with_custom_delimiter() {
        final String rawMap = "first\\:1\\|second\\:2\\|third\\:3";
        final TypeToken<Map<String, Integer>> typeToken = new TypeToken<Map<String, Integer>>() {
        };
        final Stringifier<Map<String, Integer>> stringifier =
                new Stringifiers.MapStringifier<>(String.class, Integer.class, "|");

        StringifierRegistry.getInstance()
                           .register(typeToken, stringifier);

        final Map<String, Integer> convertedMap = Stringifiers.parse(rawMap, typeToken);
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
    public void convert_string_to_list_of_strings() {
        final String stringToConvert = "1,2,3,4,5";
        final List<String> actualList =
                new Stringifiers.ListStringifier<>(String.class).reverse()
                                                                .convert(stringToConvert);
        assertNotNull(actualList);

        final List<String> expectedList = Arrays.asList(stringToConvert.split(","));
        assertThat(actualList, is(expectedList));
    }

    @Test
    public void convert_list_of_strings_to_string() {
        final List<String> listToConvert = newArrayList("1", "2", "3", "4", "5");
        final String actual =
                new Stringifiers.ListStringifier<>(String.class).convert(listToConvert);
        assertEquals(listToConvert.toString(), actual);
    }

    @Test
    public void convert_list_of_integers_to_string() {
        final List<Integer> listToConvert = newArrayList(1, 2, 3, 4, 5);
        final String actual =
                new Stringifiers.ListStringifier<>(Integer.class).convert(listToConvert);
        assertEquals(listToConvert.toString(), actual);
    }

    @Test
    public void convert_string_to_list_of_integers() {
        final String stringToConvert = "1|2|3|4|5";
        final String delimiter = "\\|";
        final List<Integer> actualList =
                new Stringifiers.ListStringifier<>(Integer.class,
                                                   delimiter).reverse()
                                                             .convert(stringToConvert);
        assertNotNull(actualList);

        final List<Integer> expectedList = newArrayList(1, 2, 3, 4, 5);
        assertThat(actualList, is(expectedList));
    }

    @Test(expected = IllegalConversionArgumentException.class)
    public void emit_exception_when_list_type_does_not_have_appropriate_stringifier() {
        final String stringToConvert = "{value:123456}";
        new Stringifiers.ListStringifier<>(TaskId.class).reverse()
                                                        .convert(stringToConvert);
    }

    @Test
    public void convert_integer_to_string() {
        final Integer integerToConvert = 1;
        final String convertedValue =
                new Stringifiers.IntegerStringifier().convert(integerToConvert);
        assertEquals(integerToConvert.toString(), convertedValue);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        final NullPointerTester tester = new NullPointerTester();
        tester.testStaticMethods(Stringifiers.class, NullPointerTester.Visibility.PACKAGE);
    }
}
