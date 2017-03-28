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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;

import javax.annotation.Nullable;
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
        final Maps.EntryTransformer<K, V, QuotedMapItem<K, V>> transformer =
                getFromMapEntryTransformer();
        final Map<K, QuotedMapItem<K, V>> escapedMap = Maps.transformEntries(obj, transformer);
        final String result = Joiner.on(delimiter)
                                    .join(escapedMap.values());
        return result;
    }

    @Override
    protected Map<K, V> fromString(String s) {
        final String escapedString = escaper.escape(s);
        final Map<K, V> resultMap = newHashMap();

        final Map<String, String> buckets = splitter.split(escapedString);
        final Maps.EntryTransformer<String, String, Map.Entry<K, V>> transformer =
                getToMapEntryTransformer();
        final Map<String, Map.Entry<K, V>> transformedMap =
                Maps.transformEntries(buckets, transformer);

        for (Map.Entry<K, V> bucket : transformedMap.values()) {
            resultMap.put(bucket.getKey(), bucket.getValue());
        }
        return resultMap;
    }

    private static Splitter.MapSplitter getMapSplitter(String bucketPattern,
                                                       String keyValuePattern) {
        final Splitter.MapSplitter result =
                Splitter.onPattern(bucketPattern)
                        .withKeyValueSeparator(Splitter.onPattern(keyValuePattern));
        return result;
    }

    private Maps.EntryTransformer<K, V, QuotedMapItem<K, V>> getFromMapEntryTransformer() {
        return new Maps.EntryTransformer<K, V, QuotedMapItem<K, V>>() {
            @Override
            public QuotedMapItem<K, V> transformEntry(@Nullable K key, @Nullable V value) {
                if (key == null || value == null) {
                    throw newIllegalArgumentException("The key and value cannot be null.");
                }
                return QuotedMapItem.of(key, value);
            }
        };
    }

    private Maps.EntryTransformer<String, String, Map.Entry<K, V>> getToMapEntryTransformer() {
        return new Maps.EntryTransformer<String, String, Map.Entry<K, V>>() {
            @Override
            public Map.Entry<K, V> transformEntry(@Nullable String key,
                                                  @Nullable String value) {
                final Map.Entry<K, V> result =
                        QuotedMapItem.parse(new AbstractMap.SimpleEntry<>(key, value),
                                            new AbstractMap.SimpleEntry<>(keyClass, valueClass));
                return result;
            }
        };
    }

    /**
     * Encloses and discloses each key value from the {@code Map} into and from quotes.
     *
     * @param <K> the type of the keys in the map
     * @param <V> the type of the values in the map
     */
    private static class QuotedMapItem<K, V> extends Stringifiers.QuotedItem {
        private final K key;
        private final V value;
        private final Class<K> keyClass;
        private final Class<V> valueClass;

        @SuppressWarnings("unchecked")
        // It is OK because the classes are same.
        private QuotedMapItem(K key, V value) {
            this.key = key;
            this.value = value;
            this.keyClass = (Class<K>) key.getClass();
            this.valueClass = (Class<V>) value.getClass();
        }

       private static <K, V> QuotedMapItem<K, V> of(K key, V value) {
            return new QuotedMapItem<>(key, value);
        }

        static <K, V> Map.Entry<K, V> parse(Map.Entry<String, String> entryToParse,
                                            Map.Entry<Class<K>, Class<V>> classEntry) {
            checkKeyValue(entryToParse);
            return convert(entryToParse, classEntry);
        }

        private static <K, V> Map.Entry<K, V> convert(Map.Entry<String, String> entryToParse,
                                                      Map.Entry<Class<K>, Class<V>> classEntry) {
            final String key = Stringifiers.QuotedItem.unquote(entryToParse.getKey());
            final String value = Stringifiers.QuotedItem.unquote(entryToParse.getValue());

            try {
                final K convertedKey = Stringifiers.convert(key, classEntry.getKey());
                final V convertedValue = Stringifiers.convert(value,
                                                              classEntry.getValue());
                final Map.Entry<K, V> convertedBucket =
                        new AbstractMap.SimpleEntry<>(convertedKey, convertedValue);
                return convertedBucket;
            } catch (Throwable e) {
                throw newIllegalArgumentException("The exception occurred during the conversion",
                                                  e);
            }
        }

        private static void checkKeyValue(Map.Entry<String, String> entryToCheck) {
            if (!isQuotedKeyValue(entryToCheck.getKey(), entryToCheck.getValue())) {
                final String exMessage =
                        "Illegal key-value format. The key-value should be quoted " +
                        "and separated with the `" + KEY_VALUE_DELIMITER + "` character.";
                throw newIllegalArgumentException(exMessage);
            }
        }

        private static boolean isQuotedKeyValue(CharSequence key, CharSequence value) {
            final boolean result = Stringifiers.QuotedItem.isQuotedString(key) &&
                                   Stringifiers.QuotedItem.isQuotedString(value);
            return result;
        }

        @Override
        public String toString() {
            final String keyToQuote = Stringifiers.toString(key, keyClass);
            final String valueToQuote =  Stringifiers.toString(value, valueClass);
            final String result = quote(keyToQuote) + KEY_VALUE_DELIMITER + quote(valueToQuote);
            return result;
        }
    }
}
