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
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Illia Shepilov
 */
public class ListStringifierShould {

    @Test
    public void convert_string_with_backslashes_to_list_and_backward() {
        final String stringToConvert = "\"\\\"\\1\\\"\\\"";
        Stringifier<List<String>> stringifier = Stringifiers.newForListOf(String.class);
        final List<String> convertedList = stringifier.fromString(stringToConvert);
        final String convertedString = stringifier.toString(convertedList);
        assertEquals(stringToConvert, convertedString);
    }

    @Test
    public void convert_string_to_list_of_strings() {
        final String stringToConvert = "\"1\\\"\",\"2\",\"3\\\"\",\"4\",\"5\"";
        final List<String> actualList = Stringifiers.newForListOf(String.class).reverse()
                                                    .convert(stringToConvert);
        assertNotNull(actualList);

        final List<String> expectedList = newArrayList("1\"", "2", "3\"", "4", "5");
        assertThat(actualList, is(expectedList));
    }

    @Test
    public void convert_string_to_list_of_strings_and_backward() {
        final String stringToConvert = "\"1\\\"\",\"2\",\"3\\\"\",\"4\",\"5\"";
        final Stringifier<List<String>> stringifier = Stringifiers.newForListOf(String.class);
        final List<String> actualList = stringifier.reverse()
                                                   .convert(stringToConvert);
        final String convertedList = stringifier.convert(actualList);
        assertEquals(stringToConvert, convertedList);
    }

    @Test
    public void convert_list_of_strings_to_string() {
        final List<String> listToConvert = newArrayList("1", "2", "3", "4", "5");
        final String actual = Stringifiers.newForListOf(String.class).convert(listToConvert);

        assertEquals(escapeString(listToConvert.toString()), actual);
    }

    @Test
    public void convert_list_of_integers_to_string() {
        final List<Integer> listToConvert = newArrayList(1, 2, 3, 4, 5);
        final String actual = Stringifiers.newForListOf(Integer.class).convert(listToConvert);
        assertEquals(escapeString(listToConvert.toString()), actual);
    }

    @Test
    public void convert_string_to_list_of_integers() {
        final String stringToConvert = "\"1\"|\"2\"|\"3\"|\"4\"|\"5\"";
        final Stringifier<List<Integer>> stringifier = Stringifiers.newForListOf(Integer.class, '|');
        final List<Integer> actualList = stringifier.fromString(stringToConvert);
        assertNotNull(actualList);

        final List<Integer> expectedList = newArrayList(1, 2, 3, 4, 5);
        assertThat(actualList, is(expectedList));
    }

    @Test(expected = MissingStringifierException.class)
    public void emit_exception_when_list_type_does_not_have_appropriate_stringifier() {
        final String stringToConvert = "\"{value:123456}\"";
        final Stringifier<List<Object>> stringifier = Stringifiers.newForListOf(Object.class);
        stringifier.fromString(stringToConvert);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_when_element_is_null() {
        final List<String> listToConvert = newArrayList("1", "2", null, "4");
        Stringifiers.newForListOf(String.class).toString(listToConvert);
    }

    private static String escapeString(String stringToEscape) {
        final Escaper escaper = Escapers.builder()
                                        .addEscape(' ', "")
                                        .addEscape('[', "\"")
                                        .addEscape(']', "\"")
                                        .addEscape(',', "\",\"")
                                        .build();
        return escaper.escape(stringToEscape);
    }
}
