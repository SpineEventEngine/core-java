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
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;

import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
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

    /**
     * The delimiter for the passed elements in the {@code String} representation,
     * {@code DEFAULT_ELEMENT_DELIMITER} by default.
     */
    private final char delimiter;
    private final Class<K> keyClass;
    private final Class<V> valueClass;
    private final Escaper escaper;
    private final Splitter.MapSplitter splitter;

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
        this.escaper = Stringifiers.createEscaper(delimiter);
        this.splitter = getMapSplitter(createBucketPattern(delimiter), createKeyValuePattern());
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
        this.escaper = Stringifiers.createEscaper(delimiter);
        this.splitter = getMapSplitter(createBucketPattern(delimiter), createKeyValuePattern());
    }

    private static Splitter.MapSplitter getMapSplitter(String bucketPattern,
                                                       String keyValuePattern) {
        final Splitter.MapSplitter result =
                Splitter.onPattern(bucketPattern)
                        .withKeyValueSeparator(Splitter.onPattern(keyValuePattern));
        return result;
    }

    private static String createBucketPattern(char delimiter) {
        return Pattern.compile("(?<!\\\\)\\\\\\" + delimiter)
                      .pattern();
    }

    private static String createKeyValuePattern() {
        return Pattern.compile("(?<!\\\\)" + KEY_VALUE_DELIMITER)
                      .pattern();
    }

    @Override
    protected String toString(Map<K, V> obj) {
        final Maps.EntryTransformer<K, V, Map.Entry<String, String>> transformer =
                entryTransformer();
        final Collection<Map.Entry<String, String>> convertedEntries =
                Maps.transformEntries(obj, transformer)
                    .values();

       final Function<Map.Entry<String, String>, Map.Entry<String, String>> function =
               quoteTransformer();
        final Map<String, String> resultMap = newHashMap();
        for (Map.Entry<String, String> entry : convertedEntries) {
            Map.Entry<String, String> quotedEntry = function.apply(entry);
            checkNotNull(quotedEntry);
            resultMap.put(quotedEntry.getKey(), quotedEntry.getValue());
        }

        final String result = Joiner.on(delimiter)
                                    .withKeyValueSeparator(KEY_VALUE_DELIMITER)
                                    .join(resultMap);
        return result;
    }

    @Override
    protected Map<K, V> fromString(String s) {
        final String escapedString = escaper.escape(s);
        final Map<K, V> resultMap = newHashMap();

        final Map<String, String> buckets = splitter.split(escapedString);
        final Maps.EntryTransformer<String, String, Map.Entry<String, String>> unquoteTransformer =
                unquoteTransformer();
        final Map<String, Map.Entry<String, String>> transformedMap =
                Maps.transformEntries(buckets, unquoteTransformer);

        for (Map.Entry<String, String> entry : transformedMap.values()) {
            final Map.Entry<K, V> convertedEntry = convert(entry.getKey(), entry.getValue());
            resultMap.put(convertedEntry.getKey(), convertedEntry.getValue());
        }

        return resultMap;
    }

    private Map.Entry<K, V> convert(String keyToConvert, String value) {
        try {
            final K convertedKey = Stringifiers.convert(keyToConvert, keyClass);
            final V convertedValue = Stringifiers.convert(value, valueClass);
            final Map.Entry<K, V> convertedBucket =
                    new AbstractMap.SimpleEntry<>(convertedKey, convertedValue);
            return convertedBucket;
        } catch (Throwable e) {
            throw newIllegalArgumentException("The exception occurred during the conversion", e);
        }
    }

    private Maps.EntryTransformer<K, V, Map.Entry<String, String>> entryTransformer() {
        return new Maps.EntryTransformer<K, V, Map.Entry<String, String>>() {
            @Override
            public Map.Entry<String, String> transformEntry(@Nullable K key, @Nullable V value) {
                checkNotNull(key);
                checkNotNull(value);

                final String convertedKey = Stringifiers.toString(key, keyClass);
                final String convertedValue = Stringifiers.toString(value, valueClass);
                final AbstractMap.SimpleEntry<String, String> entry =
                        new AbstractMap.SimpleEntry<>(convertedKey, convertedValue);
                return entry;
            }
        };
    }

    private static Function<Map.Entry<String, String>,
                            Map.Entry<String, String>> quoteTransformer() {
        final Function<Map.Entry<String, String>, Map.Entry<String, String>> function =
                new Function<Map.Entry<String, String>, Map.Entry<String, String>>() {
                    @Nullable
                    @Override
                    public Map.Entry<String, String> apply(
                            @Nullable Map.Entry<String, String> input) {
                        checkNotNull(input);
                        final String key = input.getKey();
                        final String value = input.getValue();
                        checkNotNull(key);
                        checkNotNull(value);

                        final String quotedKey = ItemQuoter.quote(key);
                        final String quotedValue = ItemQuoter.quote(value);
                        final Map.Entry<String, String> result =
                                new AbstractMap.SimpleEntry<>(quotedKey, quotedValue);
                        return result;
                    }
                };
        return function;
    }

    private static Maps.EntryTransformer<String,
                                         String,
                                         Map.Entry<String, String>> unquoteTransformer() {
        return new Maps.EntryTransformer<String, String, Map.Entry<String, String>>() {
            @Override
            public Map.Entry<String, String> transformEntry(@Nullable String key,
                                                            @Nullable String value) {
                checkNotNull(key);
                checkNotNull(value);

                if (!ItemQuoter.isQuotedString(key) || !ItemQuoter.isQuotedString(value)) {
                    final String exMessage =
                            "Illegal key-value format. The key-value should be quoted " +
                            "and separated with the `" + KEY_VALUE_DELIMITER + "` character.";
                    throw newIllegalArgumentException(exMessage);
                }

                final String unquotedKey = ItemQuoter.unquote(key);
                final String unquotedValue = ItemQuoter.unquote(value);
                final Map.Entry<String, String> result =
                        new AbstractMap.SimpleEntry<>(unquotedKey, unquotedValue);
                return result;
            }
        };
    }
}
