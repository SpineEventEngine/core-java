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

import com.google.common.primitives.Ints;

import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.collect.Maps.newHashMap;
import static org.spine3.util.Exceptions.newIllegalArgumentException;

/**
 * The stringifier for the {@code Map} classes.
 *
 * <p>The stringifier for the type of the elements in the map
 * should be registered in the {@code StringifierRegistry} class
 * for the correct usage of {@code MapStringifier}.
 *
 * <h3>Example</h3>
 *
 * {@code
 *  // The registration of the stringifier.
 *  final Type type = new TypeToken<Map<String, Long>>(){}.getType();
 *  StringifierRegistry.getInstance().register(stringifier, type);
 *
 *  // Obtain already registered `MapStringifier`.
 *  final Stringifier<Map<String, Long>> mapStringifier = StringifierRegistry.getInstance()
 *                                                                           .getStringifier(type);
 *
 *  // Convert to string.
 *  final Map<String, Long> mapToConvert = ...
 *  final String convertedString = mapStringifier.toString(mapToConvert);
 *
 *  // Convert from string.
 *  final String stringToConvert = ...
 *  final Map<String, Long> convertedMap = mapStringifier.fromString(stringToConvert);
 * }
 *
 * @param <K> the type of the keys in the map
 * @param <V> the type of the values in the map
 */
class MapStringifier<K, V> extends Stringifier<Map<K, V>> {

    private static final char DEFAULT_ELEMENT_DELIMITER = ',';
    private static final String ESCAPE_SEQUENCE = "\\";
    private static final String KEY_VALUE_DELIMITER = ESCAPE_SEQUENCE + ':';

    /**
     * The delimiter for the passed elements in the {@code String} representation,
     * {@code DEFAULT_ELEMENT_DELIMITER} by default.
     */
    private final String delimiter;
    private final Class<K> keyClass;
    private final Class<V> valueClass;

    MapStringifier(Class<K> keyClass, Class<V> valueClass) {
        super();
        this.keyClass = keyClass;
        this.valueClass = valueClass;
        this.delimiter = ESCAPE_SEQUENCE + DEFAULT_ELEMENT_DELIMITER;
    }

    /**
     * That constructor should be used when need to use
     * a custom delimiter of the elements during conversion.
     *
     * @param keyClass   the class of the key elements
     * @param valueClass the class of the value elements
     * @param delimiter  the delimiter for the passed elements via string
     */
    MapStringifier(Class<K> keyClass, Class<V> valueClass, String delimiter) {
        super();
        this.keyClass = keyClass;
        this.valueClass = valueClass;
        this.delimiter = ESCAPE_SEQUENCE + delimiter;
    }

    @Override
    protected String toString(Map<K, V> obj) {
        final String result = obj.toString();
        return result;
    }

    @Override
    protected Map<K, V> fromString(String s) {
        final String[] buckets = s.split(Pattern.quote(delimiter));
        final Map<K, V> resultMap = newHashMap();

        for (String bucket : buckets) {
            saveConvertedBucket(resultMap, bucket);
        }
        Ints.stringConverter();
        return resultMap;
    }

    private Map<K, V> saveConvertedBucket(Map<K, V> resultMap, String element) {
        final String[] keyValue = element.split(Pattern.quote(KEY_VALUE_DELIMITER));
        checkKeyValue(keyValue);

        final String key = keyValue[0];
        final String value = keyValue[1];

        try {
            final K convertedKey = Stringifiers.convert(key, keyClass);
            final V convertedValue = Stringifiers.convert(value, valueClass);
            resultMap.put(convertedKey, convertedValue);
            return resultMap;
        } catch (Throwable e) {
            throw newIllegalArgumentException("The exception is occurred during the conversion", e);
        }
    }

    @SuppressWarnings("unchecked") // It is OK because class is verified.
    private static <I> I convert(Class<I> elementClass, String elementToConvert) {

        if (isInteger(elementClass)) {
            return (I) Ints.stringConverter()
                           .convert(elementToConvert);
        }

        if (isLong(elementClass)) {
            return (I) Longs.stringConverter()
                            .convert(elementToConvert);
        }

        if (isString(elementClass)) {
            return (I) elementToConvert;
        }

        final I convertedValue = Stringifiers.fromString(elementToConvert, elementClass);
        return convertedValue;

    }

    private static void checkKeyValue(String[] keyValue) {
        if (keyValue.length != 2) {
            final String exMessage =
                    "Illegal key-value format. The value should " +
                    "be separated with a single `:` character.";
            throw newIllegalArgumentException(exMessage);
        }
    }

    private static boolean isString(Class<?> aClass) {
        return String.class.equals(aClass);
    }

    private static boolean isLong(Class<?> aClass) {
        return Long.class.equals(aClass);
    }

    private static boolean isInteger(Class<?> aClass) {
        return Integer.class.equals(aClass);
    }
}
