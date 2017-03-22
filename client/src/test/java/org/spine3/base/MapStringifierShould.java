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
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.junit.Test;
import org.spine3.test.types.Task;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.spine3.base.Stringifiers.mapStringifier;

/**
 * @author Illia Shepilov
 */
@SuppressWarnings({"SerializableInnerClassWithNonSerializableOuterClass",
                   "SerializableNonStaticInnerClassWithoutSerialVersionUID"})
                   // It is OK for test methods.
public class MapStringifierShould {

    @Test
    public void convert_string_to_map() throws ParseException {
        final String rawMap = "1\\:1972-01-01T10:00:20.021-05:00";
        final Type type = new TypeToken<Map<Long, Timestamp>>(){}.getType();
        final Stringifier<Map<Long, Timestamp>> stringifier =
                mapStringifier(Long.class, Timestamp.class);
        StringifierRegistry.getInstance()
                           .register(stringifier, type);
        final Map<Long, Timestamp> actualMap = stringifier.fromString(rawMap);
        final Map<Long, Timestamp> expectedMap = newHashMap();
        expectedMap.put(1L, Timestamps.parse("1972-01-01T10:00:20.021-05:00"));
        assertThat(actualMap, is(expectedMap));
    }

    @Test
    public void convert_map_to_string() {
        final Map<String, Integer> mapToConvert = createTestMap();
        final Type type = new TypeToken<Map<String, Integer>>(){}.getType();
        final Stringifier<Map<String, Integer>> stringifier =
                mapStringifier(String.class, Integer.class);
        StringifierRegistry.getInstance()
                           .register(stringifier, type);
        final String convertedMap = stringifier.toString(mapToConvert);
        assertEquals(mapToConvert.toString(), convertedMap);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_passed_parameter_does_not_match_expected_format() {
        final String incorrectRawMap = "first\\:1\\,second\\:2";
        final Type type = new TypeToken<Map<Integer, Integer>>(){}.getType();
        final Stringifier<Map<Integer, Integer>> stringifier =
                mapStringifier(Integer.class, Integer.class);
        StringifierRegistry.getInstance()
                           .register(stringifier, type);
        stringifier.fromString(incorrectRawMap);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_occurred_exception_during_conversion() {
        final Type type = new TypeToken<Map<Task, Long>>(){}.getType();
        final Stringifier<Map<Task, Long>> stringifier = mapStringifier(Task.class, Long.class);
        StringifierRegistry.getInstance()
                           .register(stringifier, type);
        stringifier.fromString("first\\:first\\:first");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_key_value_delimiter_is_wrong() {
        final Type type = new TypeToken<Map<Long, Long>>(){}.getType();
        final Stringifier<Map<Long, Long>> stringifier = mapStringifier(Long.class, Long.class);
        StringifierRegistry.getInstance()
                           .register(stringifier, type);
        stringifier.fromString("1\\-1");
    }

    @Test
    public void convert_map_with_custom_delimiter() {
        final String rawMap = "first\\:1\\|second\\:2\\|third\\:3";
        final Type type = new TypeToken<Map<Long, Task>>() {}.getType();
        final Stringifier<Map<String, Integer>> stringifier =
                mapStringifier(String.class, Integer.class, "|");
        StringifierRegistry.getInstance()
                           .register(stringifier, type);

        final Map<String, Integer> convertedMap = stringifier.fromString(rawMap);
        assertThat(convertedMap, is(createTestMap()));
    }

    private static Map<String, Integer> createTestMap() {
        final Map<String, Integer> expectedMap = newHashMap();
        expectedMap.put("first", 1);
        expectedMap.put("second", 2);
        expectedMap.put("third", 3);
        return expectedMap;
    }
}
