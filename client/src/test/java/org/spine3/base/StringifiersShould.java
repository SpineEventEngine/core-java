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
import java.util.Map;
import java.util.Random;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

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
    public void convert_string_to_map() {
        final String rawMap = "first:1,second:2,third:3";
        final Map<String, Integer> actualMap =
                new Stringifiers.MapStringifier<>(String.class, Integer.class).reverse()
                                                                              .convert(rawMap);
        final Map<String, Integer> expectedMap = createTestMap();
        assertThat(actualMap, is(expectedMap));
    }

    @Test
    public void convert_map_to_string() {
        final Map<String, Integer> mapToConvert = createTestMap();
        final String convertedMap =
                new Stringifiers.MapStringifier<>(String.class,
                                                  Integer.class).convert(mapToConvert);
        assertEquals(mapToConvert.toString(), convertedMap);
    }

    @Test(expected = IllegalConversionArgumentException.class)
    public void throw_exception_when_passed_parameter_does_not_match_expected_format() {
        final String incorrectRawMap = "{first-1}, second-2";
        new Stringifiers.MapStringifier<>(String.class, Integer.class).reverse()
                                                                      .convert(incorrectRawMap);
    }

    @Test(expected = IllegalConversionArgumentException.class)
    public void throw_exception_when_required_stringifier_is_not_found() {
        new Stringifiers.MapStringifier<>(Long.class, Task.class).reverse()
                                                                 .convert("1:1");
    }

    @Test(expected = IllegalConversionArgumentException.class)
    public void throw_exception_when_occured_exception_during_conversion() {
        new Stringifiers.MapStringifier<>(Long.class, Long.class).reverse()
                                                                 .convert("first:first");
    }

    @Test
    public void convert_map_with_custom_delimiter() {
        final String rawMap = "first:1|second:2|third:3";
        final Map<String, Integer> convertedMap =
                new Stringifiers.MapStringifier<>(String.class,
                                                  Integer.class,
                                                  "\\|").reverse()
                                                        .convert(rawMap);
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
}
