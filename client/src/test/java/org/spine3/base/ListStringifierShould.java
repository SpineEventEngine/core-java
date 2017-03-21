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

import org.junit.Test;
import org.spine3.test.types.Task;
import org.spine3.type.ParametrizedTypeImpl;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.spine3.base.Stringifiers.listStringifier;

/**
 * @author Illia Shepilov
 */
public class ListStringifierShould {

    @Test
    public void convert_string_to_list_of_strings() {
        final String stringToConvert = "1,2,3,4,5";
        final List<String> actualList = listStringifier(String.class).reverse()
                                                                     .convert(stringToConvert);
        assertNotNull(actualList);

        final List<String> expectedList = Arrays.asList(stringToConvert.split(","));
        assertThat(actualList, is(expectedList));
    }

    @Test
    public void convert_list_of_strings_to_string() {
        final List<String> listToConvert = newArrayList("1", "2", "3", "4", "5");
        final String actual = listStringifier(String.class).convert(listToConvert);
        assertEquals(listToConvert.toString(), actual);
    }

    @Test
    public void convert_list_of_integers_to_string() {
        final List<Integer> listToConvert = newArrayList(1, 2, 3, 4, 5);
        final String actual = listStringifier(Integer.class).convert(listToConvert);
        assertEquals(listToConvert.toString(), actual);
    }

    @Test
    public void convert_string_to_list_of_integers() {
        final String stringToConvert = "1|2|3|4|5";
        final String delimiter = "|";
        final Type type = ParametrizedTypeImpl.from(List.class, new Type[]{Integer.class});
        final Stringifier<List<Integer>> stringifier = listStringifier(Integer.class, delimiter);
        StringifierRegistry.getInstance()
                           .register(stringifier, type);
        final List<Integer> actualList = Stringifiers.fromString(stringToConvert, type);
        assertNotNull(actualList);

        final List<Integer> expectedList = newArrayList(1, 2, 3, 4, 5);
        assertThat(actualList, is(expectedList));
    }

    @Test(expected = IllegalConversionArgumentException.class)
    public void emit_exception_when_list_type_does_not_have_appropriate_stringifier() {
        final String stringToConvert = "{value:123456}";
        new ListStringifier<>(Task.class).reverse()
                                         .convert(stringToConvert);
    }
}
