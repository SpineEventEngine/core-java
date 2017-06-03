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

import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * The stringifier for the {@code Map} classes.
 *
 * <p>The stringifier for the type of the elements in the map
 * should be registered in the {@code StringifierRegistry} class
 * for the correct usage of {@code MapStringifier}.
 *
 * <h3>Example</h3>
 * <pre>    {@code
 *   // The registration of the stringifier.
 *   final Type type = Types.mapTypeOf(String.class, Long.class);
 *   StringifierRegistry.getInstance().register(stringifier, type);
 *
 *   // Obtain already registered `MapStringifier`.
 *   final Stringifier<Map<String, Long>> mapStringifier = StringifierRegistry.getInstance()
 *                                                                            .getStringifier(type);
 *
 *   // Convert to string.
 *   final Map<String, Long> mapToConvert = newHashMap();
 *   mapToConvert.put("first", 1);
 *   mapToConvert.put("second", 2);
 *
 *   // The result is: \"first\":\"1\",\"second\":\"2\".
 *   final String convertedString = mapStringifier.toString(mapToConvert);
 *
 *
 *   // Convert from string.
 *   final String stringToConvert = ...
 *   final Map<String, Long> convertedMap = mapStringifier.fromString(stringToConvert); }
 * </pre>
 *
 * @param <K> the type of the keys in the map
 * @param <V> the type of the values in the map
 * @author Illia Shepilov
 */
final class MapStringifier<K, V> extends Stringifier<Map<K, V>> {

    private static final char DEFAULT_ELEMENT_DELIMITER = ',';
    private static final char KEY_VALUE_DELIMITER = ':';

    /**
     * The delimiter for the passed elements in the {@code String} representation,
     * {@code DEFAULT_ELEMENT_DELIMITER} by default.
     */
    private final char delimiter;
    private final Escaper escaper;
    private final Splitter.MapSplitter splitter;
    private final Stringifier<K> keyStringifier;
    private final Stringifier<V> valueStringifier;

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
        this.keyStringifier = StringifierRegistry.getStringifier(keyClass);
        this.valueStringifier = StringifierRegistry.getStringifier(valueClass);
        this.delimiter = delimiter;
        this.escaper = Stringifiers.createEscaper(delimiter);
        this.splitter = createMapSplitter(Quoter.createDelimiterPattern(delimiter),
                                          createKeyValuePattern());
    }

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
        this(keyClass, valueClass, DEFAULT_ELEMENT_DELIMITER);
    }

    private static Splitter.MapSplitter createMapSplitter(String bucketPattern,
                                                          String keyValuePattern) {
        final Splitter.MapSplitter result =
                Splitter.onPattern(bucketPattern)
                        .withKeyValueSeparator(Splitter.onPattern(keyValuePattern));
        return result;
    }

    private static String createKeyValuePattern() {
        return Pattern.compile("(?<!\\\\)" + KEY_VALUE_DELIMITER)
                      .pattern();
    }

    @Override
    protected String toString(Map<K, V> obj) {
        final Converter<String, String> quoter = Quoter.forMaps();
        final Converter<K, String> keyConverter = keyStringifier.andThen(quoter);
        final Converter<V, String> valueConverter = valueStringifier.andThen(quoter);

        final Map<String, String> resultMap = newLinkedHashMap();
        for (Map.Entry<K, V> entry : obj.entrySet()) {
            final String convertedKey = keyConverter.convert(entry.getKey());
            final String convertedValue = valueConverter.convert(entry.getValue());
            resultMap.put(convertedKey, convertedValue);
        }
        final String result = Joiner.on(delimiter)
                                    .withKeyValueSeparator(KEY_VALUE_DELIMITER)
                                    .join(resultMap);
        return result;
    }

    @Override
    protected Map<K, V> fromString(String s) {
        final String escapedString = escaper.escape(s);
        final Map<String, String> buckets = splitter.split(escapedString);
        final Map<K, V> resultMap = convert(buckets);
        return resultMap;
    }

    private Map<K, V> convert(Map<String, String> buckets) {
        final Converter<String, String> quoter = Quoter.forMaps();
        final Converter<String, K> keyConverter = quoter.reverse()
                                                        .andThen(keyStringifier.reverse());
        final Converter<String, V> valueConverter = quoter.reverse()
                                                          .andThen(valueStringifier.reverse());
        final Map<K, V> resultMap = newHashMap();
        try {
            for (Map.Entry<String, String> bucket : buckets.entrySet()) {
                final K convertedKey = keyConverter.convert(bucket.getKey());
                final V convertedValue = valueConverter.convert(bucket.getValue());
                resultMap.put(convertedKey, convertedValue);
            }
            return resultMap;
        } catch (Throwable e) {
            throw newIllegalArgumentException("The exception occurred during the conversion", e);
        }
    }
}
