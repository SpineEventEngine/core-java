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

package io.spine.string;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.junit.Test;

import java.text.ParseException;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static io.spine.string.Stringifiers.newForMapOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Illia Shepilov
 */
public class MapStringifierShould {

    @Test
    public void convert_string_to_map() throws ParseException {
        final String rawMap = "\"1\":\"1972-01-01T10\\:00\\:20.021-05\\:00\"," +
                              "\"2\":\"1972-01-01T10\\:00\\:20.021-05\\:00\"";
        final Stringifier<Map<Long, Timestamp>> stringifier =
                Stringifiers.newForMapOf(Long.class, Timestamp.class);
        final Map<Long, Timestamp> actualMap = stringifier.fromString(rawMap);
        final Map<Long, Timestamp> expectedMap = newHashMap();

        final String timeToParse = "1972-01-01T10:00:20.021-05:00";
        expectedMap.put(1L, Timestamps.parse(timeToParse));
        expectedMap.put(2L, Timestamps.parse(timeToParse));
        assertThat(actualMap, is(expectedMap));
    }

    @Test
    public void convert_map_to_string() {
        final Map<String, Integer> mapToConvert = createTestMap();
        final Stringifier<Map<String, Integer>> stringifier =
                Stringifiers.newForMapOf(String.class, Integer.class);
        final String convertedMap = stringifier.toString(mapToConvert);
        assertEquals(convertMapToString(mapToConvert).length(), convertedMap.length());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_passed_parameter_does_not_match_expected_format() {
        final String incorrectRawMap = "\"first\":\"1\",\"second\":\"2\"";
        final Stringifier<Map<Integer, Integer>> stringifier =
                Stringifiers.newForMapOf(Integer.class, Integer.class);
        stringifier.fromString(incorrectRawMap);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_occurred_exception_during_conversion() {
        final Stringifier<Map<Long, Long>> stringifier = Stringifiers.newForMapOf(Long.class, Long.class);
        stringifier.fromString("first\":\"first\":\"first\"");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_key_value_delimiter_is_wrong() {
        final Stringifier<Map<Long, Long>> stringifier = Stringifiers.newForMapOf(Long.class, Long.class);
        stringifier.fromString("1\\-1");
    }

    @Test
    public void convert_map_with_custom_delimiter() {
        final String rawMap = "\"first\":\"1\"|\"second\":\"2\"|\"third\":\"3\"";
        final Stringifier<Map<String, Integer>> stringifier =
                newForMapOf(String.class, Integer.class, '|');
        final Map<String, Integer> convertedMap = stringifier.fromString(rawMap);
        assertThat(convertedMap, is(createTestMap()));
    }

    @Test
    public void convert_from_map_to_string_and_backward() {
        final Map<String, Integer> mapToConvert = createTestMap();
        final Stringifier<Map<String, Integer>> stringifier =
                Stringifiers.newForMapOf(String.class, Integer.class);
        final String convertedString = stringifier.toString(mapToConvert);
        final Map<String, Integer> actualMap = stringifier.fromString(convertedString);
        assertEquals(mapToConvert, actualMap);
    }

    @Test
    public void convert_string_which_contains_delimiter_in_content_to_map() {
        final String stringToConvert = "\"1\\\"\\\"\":\"one\\,\",\"2\\:\":\"two\"";
        final Stringifier<Map<String, String>> stringifier = Stringifiers.newForMapOf(String.class,
                                                                                      String.class);
        final Map<String, String> actualMap = stringifier.fromString(stringToConvert);

        assertEquals("one,", actualMap.get("1\"\""));
        assertEquals("two", actualMap.get("2:"));
    }

    @Test
    public void convert_string_which_contains_delimiter_in_content_to_map_and_backward() {
        final String stringToConvert = "\"1\\\"\\\"\":\"one\\,\"";
        final Stringifier<Map<String, String>> stringifier = Stringifiers.newForMapOf(String.class,
                                                                                      String.class);
        final Map<String, String> actualMap = stringifier.fromString(stringToConvert);
        final String convertedMap = stringifier.toString(actualMap);

        assertEquals(stringToConvert, convertedMap);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_value_has_not_prior_quote() {
        final String stringToConvert = "\"1\":2\"";
        tryToConvert(stringToConvert);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_key_has_not_prior_quote() {
        final String stringToConvert = "1\":\"2\"";
        tryToConvert(stringToConvert);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_value_has_not_further_quote() {
        final String stringToConvert = "\"1\":\"2";
        tryToConvert(stringToConvert);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_key_has_not_further_quote() {
        final String stringToConvert = "\"1:\"2\"";
        tryToConvert(stringToConvert);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_key_value_are_unquoted() {
        final String stringToConvert = "1:2";
        tryToConvert(stringToConvert);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_when_key_is_null() {
        final Stringifier<Map<String, String>> stringifier =
                Stringifiers.newForMapOf(String.class, String.class);
        final Map<String, String> mapWithNulls = newHashMap();
        mapWithNulls.put("1", "2");
        mapWithNulls.put(null, "2");
        stringifier.toString(mapWithNulls);
    }

    private static void tryToConvert(String stringToConvert) {
        final Stringifier<Map<String, String>> stringifier = Stringifiers.newForMapOf(String.class,
                                                                                      String.class);
        stringifier.fromString(stringToConvert);
    }

    private static String convertMapToString(Map<String, Integer> mapToConvert) {
        final Escaper escaper = Escapers.builder()
                                        .addEscape(' ', "")
                                        .addEscape('{', "\"")
                                        .addEscape('}', "\"")
                                        .addEscape('=', "\":\"")
                                        .addEscape(',', "\",\"")
                                        .build();
        return escaper.escape(mapToConvert.toString());
    }

    private static Map<String, Integer> createTestMap() {
        final Map<String, Integer> expectedMap = newHashMap();
        expectedMap.put("first", 1);
        expectedMap.put("second", 2);
        expectedMap.put("third", 3);
        return expectedMap;
    }
}
