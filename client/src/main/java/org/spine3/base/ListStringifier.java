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

import com.google.common.escape.Escaper;

import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.base.Stringifiers.createEscaper;
import static org.spine3.base.Stringifiers.isQuotedString;
import static org.spine3.base.Stringifiers.unquote;
import static org.spine3.util.Exceptions.newIllegalArgumentException;

/**
 * The stringifier for the {@code List} classes.
 *
 * <p> The stringifier for the type of the elements in the list
 * should be registered in the {@code StringifierRegistry} class
 * for the correct usage of the {@code ListStringifier}.
 *
 * <h3>Example</h3>
 *
 * {@code
 *    // Stringifier creation.
 *    final Stringifier<List<Integer>> listStringifier = Stringifiers.listStringifier();
 *
 *    // The registration of the stringifier.
 *    final Stringifier<List<Integer>> listStringifer = Stringifiers.listStringifier();
 *    final Type type = Types.listTypeOf(Integer.class);
 *    StringifierRegistry.getInstance().register(listStringifier, type);
 *
 *    // Obtain already registered stringifier.
 *    final Stringifier<List<Integer>> listStringifier = StringifierRegistry.getInstance()
 *                                                                          .getStringifier(type);
 *
 *    // Convert to string.
 *    final List<Integer> listToConvert = newArrayList(1, 2, 3);
 *
 *    // The result is: \"1\",\"2\",\"3\".
 *    final String convertedString = listStringifer.toString(listToConvert);
 *
 *    // Convert from string.
 *    final String stringToConvert = ...
 *    final List<Integer> convertedList = listStringifier.fromString(stringToConvert);
 * }
 *
 * @param <T> the type of the elements in the list.
 */
class ListStringifier<T> extends Stringifier<List<T>> {

    private static final char DEFAULT_ELEMENT_DELIMITER = ',';

    /**
     * The delimiter for the passed elements in the {@code String} representation,
     * {@code DEFAULT_ELEMENT_DELIMITER} by default.
     */
    private final char delimiter;
    private final Class<T> listGenericClass;
    private final String elementDelimiterPattern;
    private final Escaper escaper;

    /**
     * That constructor should be used when a custom delimiter is not needed.
     *
     * <p>The {@code DEFAULT_ELEMENT_DELIMITER} will be used.
     *
     * @param listGenericClass the class of the list elements
     */
    ListStringifier(Class<T> listGenericClass) {
        super();
        this.listGenericClass = listGenericClass;
        this.delimiter = DEFAULT_ELEMENT_DELIMITER;
        escaper = createEscaper(delimiter);
        elementDelimiterPattern = createElementDelimiterPattern(delimiter);
    }

    /**
     * That constructor should be used for providing a custom
     * delimiter of the elements during conversion.
     *
     * @param listGenericClass the class of the list elements
     * @param delimiter        the delimiter for the passed elements via string
     */
    ListStringifier(Class<T> listGenericClass, char delimiter) {
        super();
        this.listGenericClass = listGenericClass;
        this.delimiter = delimiter;
        escaper = createEscaper(delimiter);
        elementDelimiterPattern = createElementDelimiterPattern(delimiter);
    }

    private static String createElementDelimiterPattern(char delimiter) {
        return Pattern.compile("(?<!\\\\)\\\\\\" + delimiter)
                      .pattern();
    }

    @Override
    protected String toString(List<T> list) {
        final StringBuilder stringBuilder = new StringBuilder(0);
        for (T element : list) {
            final char quote = '"';
            stringBuilder.append(quote)
                         .append(element)
                         .append(quote)
                         .append(delimiter);
        }
        final int length = stringBuilder.length();
        final String result = stringBuilder.substring(0, length - 1);
        return result;
    }

    @Override
    protected List<T> fromString(String s) {
        final String escapedString = escaper.escape(s);
        final String[] elements = escapedString.split(elementDelimiterPattern);

        final List<T> result = newArrayList();
        for (String element : elements) {
            checkElement(element);

            final T convertedValue = Stringifiers.convert(unquote(element), listGenericClass);
            result.add(convertedValue);
        }
        return result;
    }

    private static void checkElement(CharSequence element) {
        final boolean isQuoted = isQuotedString(element);
        if (!isQuoted) {
            throw newIllegalArgumentException("Illegal format of the element");
        }
    }

}
