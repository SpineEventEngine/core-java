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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.escape.Escaper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.base.ListStringifier.QuotedListItem.of;
import static org.spine3.base.ListStringifier.QuotedListItem.parse;
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

    private static final String ELEMENT_IS_NULL_EX_MSG = "The list element cannot be null.";
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
     * Creates a {@code ListStringifier}.
     *
     * <p>The {@code DEFAULT_ELEMENT_DELIMITER} is used for element
     * separation in {@code String} representation of the {@code List}.
     *
     * @param listGenericClass the class of the list elements
     */
    ListStringifier(Class<T> listGenericClass) {
        super();
        this.listGenericClass = listGenericClass;
        this.delimiter = DEFAULT_ELEMENT_DELIMITER;
        escaper = Stringifiers.QuotedItem.createEscaper(delimiter);
        elementDelimiterPattern = createElementDelimiterPattern(delimiter);
    }

    /**
     * Creates a {@code ListStringifier}.
     *
     * <p>The specified delimiter is used for element separation
     * in {@code String} representation of the {@code List}.
     *
     * @param listGenericClass the class of the list elements
     * @param delimiter        the delimiter for the passed elements via string
     */
    ListStringifier(Class<T> listGenericClass, char delimiter) {
        super();
        this.listGenericClass = listGenericClass;
        this.delimiter = delimiter;
        escaper = Stringifiers.QuotedItem.createEscaper(delimiter);
        elementDelimiterPattern = createElementDelimiterPattern(delimiter);
    }

    private static String createElementDelimiterPattern(char delimiter) {
        return Pattern.compile("(?<!\\\\)\\\\\\" + delimiter)
                      .pattern();
    }

    @Override
    protected String toString(List<T> list) {
        final Function<T, QuotedListItem<T>> function = new Function<T, QuotedListItem<T>>() {
            @Nullable
            @Override
            public QuotedListItem<T> apply(@Nullable T input) {
                if (input == null) {
                    throw newIllegalArgumentException(ELEMENT_IS_NULL_EX_MSG);
                }
                return of(input);
            }
        };
        final List<QuotedListItem<T>> quotedItems = Lists.transform(list, function);
        final String result = Joiner.on(delimiter)
                                    .join(quotedItems);
        return result;
    }

    @Override
    protected List<T> fromString(String s) {
        final String escapedString = escaper.escape(s);
        final Splitter splitter = Splitter.onPattern(elementDelimiterPattern);
        final List<String> elements = newArrayList(splitter.split(escapedString));
        final Function<String, T> function = new Function<String, T>() {
            @Nullable
            @Override
            public T apply(@Nullable String input) {
                if (input == null) {
                    throw newIllegalArgumentException(ELEMENT_IS_NULL_EX_MSG);
                }
                return parse(input, listGenericClass);
            }
        };
        final List<T> result = newArrayList(Lists.transform(elements, function));
        return result;
    }

    /**
     * Encloses and discloses each element of the list into and from quotes
     *
     * @param <T> the type of the elements in the list
     */
    static class QuotedListItem<T> extends Stringifiers.QuotedItem {

        private final T object;
        private final Class<T> genericClass;

        @SuppressWarnings("unchecked")
        // It is OK because the class is same.
        private QuotedListItem(T object) {
            this.object = object;
            this.genericClass = (Class<T>) object.getClass();
        }

        static <T> QuotedListItem<T> of(T object) {
            return new QuotedListItem<>(object);
        }

        static <T> T parse(String elementToParse, Class<T> genericClass) {
            checkElement(elementToParse);
            return convert(unquote(elementToParse), genericClass);
        }

        private static void checkElement(CharSequence element) {
            final boolean isQuoted = isQuotedString(element);
            if (!isQuoted) {
                throw newIllegalArgumentException("Illegal format of the element: " + element);
            }
        }

        @Override
        public String toString() {
            if (isString(genericClass)) {
                return quote((String) object);
            }
            return quote(Stringifiers.toString(object, genericClass));
        }
    }
}
