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
import com.google.common.escape.Escapers;

import java.util.AbstractMap;
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
 *  final Type type = Types.mapTypeOf(String.class, Long.class);
 *  StringifierRegistry.getInstance().register(stringifier, type);
 *
 *  // Obtain already registered `MapStringifier`.
 *  final Stringifier<Map<String, Long>> mapStringifier = StringifierRegistry.getInstance()
 *                                                                           .getStringifier(type);
 *
 *  // Convert to string.
 *  final Map<String, Long> mapToConvert = newHashMap();
 *  mapToConvert.put("first", 1);
 *  mapToConvert.put("second", 2);
 *
 *  // The result is: \"first\":\"1\",\"second\":\"2\".
 *  final String convertedString = mapStringifier.toString(mapToConvert);
 *
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
    private static final char KEY_VALUE_DELIMITER = ':';
    private static final char QUOTE = '"';

    /**
     * The delimiter for the passed elements in the {@code String} representation,
     * {@code DEFAULT_ELEMENT_DELIMITER} by default.
     */
    private final char delimiter;
    private final Class<K> keyClass;
    private final Class<V> valueClass;
    private final String bucketPattern;
    private final String keyValuePattern;
    private final Escaper escaper;

    /**
     * Creates a {@code MapStringifier}.
     *
     * <p>The {@code DEFAULT_ELEMENT_DELIMITER} is used for key-value
     * separation in {@code String} representation of the {@code Map}.
     *
     * @param keyClass   the class of the key elements
     * @param valueClass the class of the value elements
     */
    MapStringifier(Class<K> keyClass, Class<V> valueClass) {
        super();
        this.keyClass = keyClass;
        this.valueClass = valueClass;
        this.delimiter = DEFAULT_ELEMENT_DELIMITER;
        escaper = createEscaper(delimiter);
        bucketPattern = createBucketPattern(delimiter);
        keyValuePattern = createKeyValuePattern();
    }

    /**
     * Creates a {@code MapStringifier}.
     *
     * <p>The specified delimiter is used for key-value separation
     * in {@code String} representation of the {@code Map}.
     *
     * @param keyClass   the class of the key elements
     * @param valueClass the class of the value elements
     * @param delimiter  the delimiter for the passed elements via string
     */
    MapStringifier(Class<K> keyClass, Class<V> valueClass, char delimiter) {
        super();
        this.keyClass = keyClass;
        this.valueClass = valueClass;
        this.delimiter = delimiter;
        escaper = createEscaper(delimiter);
        bucketPattern = createBucketPattern(delimiter);
        keyValuePattern = createKeyValuePattern();
    }

    private static String createBucketPattern(char delimiter) {
        return Pattern.compile("(?<!\\\\)\\\\\\" + delimiter)
                      .pattern();
    }

    private static String createKeyValuePattern() {
        return Pattern.compile("(?<!\\\\)\\\\" + KEY_VALUE_DELIMITER)
                      .pattern();
    }

    @Override
    protected String toString(Map<K, V> obj) {
        final StringBuilder stringBuilder = new StringBuilder(0);
        for (Map.Entry<K, V> entry : obj.entrySet()) {
            stringBuilder.append(QUOTE)
                         .append(entry.getKey())
                         .append(QUOTE)
                         .append(KEY_VALUE_DELIMITER)
                         .append(QUOTE)
                         .append(entry.getValue())
                         .append(QUOTE)
                         .append(delimiter);
        }
        final int length = stringBuilder.length();
        final String result = stringBuilder.substring(0, length - 1);
        return result;
    }

    @Override
    protected Map<K, V> fromString(String s) {
        final String escapedString = escaper.escape(s);
        final String[] buckets = escapedString.split(bucketPattern);
        final Map<K, V> resultMap = newHashMap();

        for (String bucket : buckets) {
            final Map.Entry<K, V> convertedBucket = convert(bucket);
            resultMap.put(convertedBucket.getKey(), convertedBucket.getValue());
        }
        return resultMap;
    }

    private Map.Entry<K, V> convert(String bucketToConvert) {
        final String[] keyValue = bucketToConvert.split(keyValuePattern);
        checkKeyValue(keyValue);

        final String key = unquote(keyValue[0]);
        final String value = unquote(keyValue[1]);

        try {
            final K convertedKey = convert(keyClass, key);
            final V convertedValue = convert(valueClass, value);
            final Map.Entry<K, V> convertedBucket =
                    new AbstractMap.SimpleEntry<>(convertedKey, convertedValue);
            return convertedBucket;
        } catch (Throwable e) {
            throw newIllegalArgumentException("The exception is occurred during the conversion", e);
        }
    }

    @SuppressWarnings("unchecked") // It is safe because the type is checked before the cast.
    private static <I> I convert(Class<I> elementClass, String elementToConvert) {
        if (isString(elementClass)) {
            return (I) elementToConvert;
        }

        final I convertedValue = Stringifiers.fromString(elementToConvert, elementClass);
        return convertedValue;

    }

    private static void checkKeyValue(String[] keyValue) {
        if (keyValue.length != 2 || !isQuotedKeyValue(keyValue)) {
            final String exMessage =
                    "Illegal key-value format. The key-value should be quoted " +
                    "and separated with the `" + KEY_VALUE_DELIMITER + "` character.";
            throw newIllegalArgumentException(exMessage);
        }
    }

    private static boolean isString(Class<?> aClass) {
        return String.class.equals(aClass);
    }

    private static String unquote(String value) {
        final String unquotedValue = Pattern.compile("\\\\")
                                            .matcher(value.substring(2, value.length() - 2))
                                            .replaceAll("");
        return unquotedValue;
    }

    private static boolean isQuotedKeyValue(String[] keyValue) {
        final String key = keyValue[0];
        final String value = keyValue[1];

        final int keyLength = key.length();
        final int valueLength = value.length();

        if (keyLength < 2 || valueLength < 2) {
            return false;
        }

        final boolean result = isQuote(key.charAt(1)) && isQuote(key.charAt(keyLength - 1)) &&
                               isQuote(value.charAt(1)) && isQuote(value.charAt(valueLength - 1));
        return result;
    }

    private static boolean isQuote(char character) {
        return character == QUOTE;
    }

    private static Escaper createEscaper(char charToEscape) {
        final String escapedChar = "\\" + charToEscape;
        final Escaper result = Escapers.builder()
                                       .addEscape('\"', "\\\"")
                                       .addEscape(':', "\\:")
                                       .addEscape(charToEscape, escapedChar)
                                       .build();
        return result;
    }
}
