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

import com.google.common.base.Converter;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.escape.Escaper;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * The stringifier for the {@code List} classes.
 *
 * <p> The stringifier for the type of the elements in the list
 * should be registered in the {@code StringifierRegistry} class
 * for the correct usage of the {@code ListStringifier}.
 *
 * <h3>Example</h3>
 * <pre>    {@code
 *   // Stringifier creation.
 *   final Stringifier<List<Integer>> listStringifier = Stringifiers.listStringifier();
 *
 *   // The registration of the stringifier.
 *   final Stringifier<List<Integer>> listStringifer = Stringifiers.listStringifier();
 *   final Type type = Types.listTypeOf(Integer.class);
 *   StringifierRegistry.getInstance().register(listStringifier, type);
 *
 *   // Obtain already registered stringifier.
 *   final Stringifier<List<Integer>> listStringifier = StringifierRegistry.getInstance()
 *                                                                         .getStringifier(type);
 *
 *   // Convert to string.
 *   final List<Integer> listToConvert = newArrayList(1, 2, 3);
 *
 *   // The result is: \"1\",\"2\",\"3\".
 *   final String convertedString = listStringifer.toString(listToConvert);
 *
 *   // Convert from string.
 *   final String stringToConvert = ...
 *   final List<Integer> convertedList = listStringifier.fromString(stringToConvert);}
 * </pre>
 *
 * @param <T> the type of the elements in the list.
 * @author Illia Shepilov
 */
final class ListStringifier<T> extends Stringifier<List<T>> {

    private static final char DEFAULT_ELEMENT_DELIMITER = ',';

    /**
     * The delimiter for the passed elements in the {@code String} representation,
     * {@code DEFAULT_ELEMENT_DELIMITER} by default.
     */
    private final char delimiter;
    private final Escaper escaper;
    private final Splitter splitter;
    private final Stringifier<T> elementStringifier;

    /**
     * Creates a {@code ListStringifier}.
     *
     * <p>The specified delimiter is used for element separation
     * in the {@code String} representation of the {@code List}.
     *
     * @param listGenericClass the class of the list elements
     * @param delimiter        the delimiter for the passed elements via string
     */
    ListStringifier(Class<T> listGenericClass, char delimiter) {
        super();
        this.elementStringifier = StringifierRegistry.getStringifier(listGenericClass);
        this.delimiter = delimiter;
        this.escaper = Stringifiers.createEscaper(delimiter);
        this.splitter = Splitter.onPattern(Quoter.createDelimiterPattern(delimiter));
    }

    /**
     * Creates a {@code ListStringifier}.
     *
     * <p>The {@code DEFAULT_ELEMENT_DELIMITER} is used for element
     * separation in the {@code String} representation of the {@code List}.
     *
     * @param listGenericClass the class of the list elements
     */
    ListStringifier(Class<T> listGenericClass) {
        this(listGenericClass, DEFAULT_ELEMENT_DELIMITER);
    }

    @Override
    protected String toString(List<T> list) {
        final Converter<String, String> quoter = Quoter.forLists();
        final List<String> convertedItems = newArrayList();
        for (T item : list) {
            final String convertedItem = elementStringifier.andThen(quoter)
                                                           .convert(item);
            convertedItems.add(convertedItem);
        }
        final String result = Joiner.on(delimiter)
                                    .join(convertedItems);
        return result;
    }

    @Override
    protected List<T> fromString(String s) {
        final String escapedString = escaper.escape(s);
        final List<String> items = newArrayList(splitter.split(escapedString));
        final Converter<String, String> quoter = Quoter.forLists();
        final Converter<String, T> converter = quoter.reverse()
                                                     .andThen(elementStringifier.reverse());
        final List<T> result = newArrayList();
        for (String item : items) {
            final T convertedItem = converter.convert(item);
            result.add(convertedItem);
        }
        return result;
    }
}
