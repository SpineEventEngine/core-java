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
import com.google.protobuf.Message;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with {@code Stringifier}s.
 *
 * @author Alexander Yevsyukov
 * @author Illia Shepilov
 */
public final class Stringifiers {

    private Stringifiers() {
        // Disable instantiation of this utility class.
    }

    /**
     * Converts the passed value to the string representation.
     *
     * <p>Use this method for converting non-generic objects. For generic objects,
     * please use {@link #toString(Object, Type)}.
     *
     * @param object the object to convert
     * @param <T>    the type of the object
     * @return the string representation of the passed object
     */
    public static <T> String toString(T object) {
        checkNotNull(object);
        return toString(object, object.getClass());
    }

    /**
     * Converts the passed value to the string representation.
     *
     * <p>This method must be used of the passed object is a generic type.
     *
     * @param object  to object to convert
     * @param typeOfT the type of the passed object
     * @param <T>     the type of the object to convert
     * @return the string representation of the passed object
     * @throws MissingStringifierException if passed value cannot be converted
     */
    @SuppressWarnings("unchecked") // It is OK because the type is checked before cast.
    public static <T> String toString(T object, Type typeOfT) {
        checkNotNull(object);
        checkNotNull(typeOfT);

        final Stringifier<T> stringifier = StringifierRegistry.getStringifier(typeOfT);
        final String result = stringifier.convert(object);
        return result;
    }

    /**
     * Converts string value to the specified type.
     *
     * @param str     the string to convert
     * @param typeOfT the type into which to convert the string
     * @param <T>     the type of the value to return
     * @return the parsed value from string
     * @throws MissingStringifierException if passed value cannot be converted
     */
    public static <T> T fromString(String str, Type typeOfT) {
        checkNotNull(str);
        checkNotNull(typeOfT);

        final Stringifier<T> stringifier = StringifierRegistry.getStringifier(typeOfT);
        final T result = stringifier.reverse()
                                    .convert(str);
        return result;
    }

    /**
     * Obtains {@code Stringifier} for the map with default delimiter for the passed map elements.
     *
     * @param keyClass   the class of keys are maintained by this map
     * @param valueClass the class  of mapped values
     * @param <K>        the type of keys are maintained by this map
     * @param <V>        the type of the values stored in this map
     * @return the stringifier for the map
     */
    public static <K, V> Stringifier<Map<K, V>> newForMapOf(Class<K> keyClass,
                                                            Class<V> valueClass) {
        checkNotNull(keyClass);
        checkNotNull(valueClass);
        final Stringifier<Map<K, V>> mapStringifier = new MapStringifier<>(keyClass, valueClass);
        return mapStringifier;
    }

    /**
     * Obtains {@code Stringifier} for the map with custom delimiter for the passed map elements.
     *
     * @param keyClass   the class of keys are maintained by this map
     * @param valueClass the class  of mapped values
     * @param delimiter  the delimiter for the passed map elements via string
     * @param <K>        the type of keys are maintained by this map
     * @param <V>        the type of mapped values
     * @return the stringifier for the map
     */
    public static <K, V> Stringifier<Map<K, V>> newForMapOf(Class<K> keyClass,
                                                            Class<V> valueClass,
                                                            char delimiter) {
        checkNotNull(keyClass);
        checkNotNull(valueClass);
        checkNotNull(delimiter);
        final Stringifier<Map<K, V>> mapStringifier =
                new MapStringifier<>(keyClass, valueClass, delimiter);
        return mapStringifier;
    }

    /**
     * Obtains {@code Stringifier} for {@code Boolean} values.
     */
    public static Stringifier<Boolean> forBoolean() {
        return BooleanStringifier.getInstance();
    }

    /**
     * Obtains {@code Stringifier} for {@code Integer} values.
     */
    public static Stringifier<Integer> forInteger() {
        return IntegerStringifier.getInstance();
    }

    /**
     * Obtains {@code Stringifier} for {@code Long} values.
     */
    public static Stringifier<Long> forLong() {
        return LongStringifier.getInstance();
    }

    /**
     * Obtains {@code Stringifier} for {@code String} values.
     *
     * <p>Simply returns passed strings.
     */
    static Stringifier<String> forString() {
        return NoOpStringifier.getInstance();
    }

    /**
     * Obtains {@code Stringifier} for list with default delimiter for the passed list elements.
     *
     * @param elementClass the class of the list elements
     * @param <T>          the type of the elements in this list
     * @return the stringifier for the list
     */
    public static <T> Stringifier<List<T>> newForListOf(Class<T> elementClass) {
        checkNotNull(elementClass);
        final Stringifier<List<T>> listStringifier = new ListStringifier<>(elementClass);
        return listStringifier;
    }

    /**
     * Obtains {@code Stringifier} for list with the custom delimiter for the passed list elements.
     *
     * @param elementClass the class of the list elements
     * @param delimiter    the delimiter or the list elements passed via string
     * @param <T>          the type of the elements in this list
     * @return the stringifier for the list
     */
    public static <T> Stringifier<List<T>> newForListOf(Class<T> elementClass, char delimiter) {
        checkNotNull(elementClass);
        checkNotNull(delimiter);
        final Stringifier<List<T>> listStringifier =
                new ListStringifier<>(elementClass, delimiter);
        return listStringifier;
    }

    /**
     * Obtains the default {@code Stringifier} for the {@code Message} classes.
     *
     * @param messageClass the message class
     * @param <T>          the type of the message
     * @return the default stringifier
     */
    static <T extends Message> Stringifier<T> newForMessage(Class<T> messageClass) {
        checkNotNull(messageClass);
        final DefaultMessageStringifier<T> defaultStringifier =
                new DefaultMessageStringifier<>(messageClass);
        return defaultStringifier;
    }

    /**
     * Creates the {@code Escaper} which escapes contained '\' and passed characters.
     *
     * @param charToEscape the char to escape
     * @return the constructed escaper
     */
    static Escaper createEscaper(char charToEscape) {
        final String escapedChar = "\\" + charToEscape;
        final Escaper result = Escapers.builder()
                                       .addEscape('\"', "\\\"")
                                       .addEscape(charToEscape, escapedChar)
                                       .build();
        return result;
    }
}
