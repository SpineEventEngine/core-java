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

import com.google.common.base.Optional;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.reflect.TypeToken;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import java.text.ParseException;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static org.spine3.util.Exceptions.conversionArgumentException;

/**
 * Utility class for working with {@code Stringifier}s.
 *
 * @author Alexander Yevsyukov
 * @author Illia Shepilov
 */
public class Stringifiers {

    private static final String CONVERSION_EXCEPTION = "Occurred exception during conversion";

    private Stringifiers() {
        // Disable instantiation of this utility class.
    }

    /**
     * Converts the passed value to the string representation.
     *
     * @param valueToConvert value to convert
     * @param typeToken      the type token of the passed value
     * @param <I>            the type of the value to convert
     * @return the string representation of the passed value
     * @throws org.spine3.validate.IllegalConversionArgumentException if passed value cannot be converted
     */
    public static <I> String toString(I valueToConvert, TypeToken<I> typeToken) {
        checkNotNull(valueToConvert);
        checkNotNull(typeToken);

        final Stringifier<I> stringifier = getStringifier(typeToken);
        final String result = stringifier.convert(valueToConvert);
        return result;
    }

    /**
     * Parses string to the appropriate value.
     *
     * @param valueToParse value to convert
     * @param typeToken    the type token of the returned value
     * @param <I>          the type of the value to return
     * @return the parsed value from string
     * @throws org.spine3.validate.IllegalConversionArgumentException if passed string cannot be parsed
     */
    public static <I> I parse(String valueToParse, TypeToken<I> typeToken) {
        checkNotNull(valueToParse);
        checkNotNull(typeToken);

        final Stringifier<I> stringifier = getStringifier(typeToken);
        final I result = stringifier.reverse()
                                    .convert(valueToParse);
        return result;
    }

    private static <I> Stringifier<I> getStringifier(TypeToken<I> typeToken) {
        final Optional<Stringifier<I>> keyStringifierOpt = StringifierRegistry.getInstance()
                                                                              .get(typeToken);

        if (!keyStringifierOpt.isPresent()) {
            final String exMessage =
                    format("Stringifier for the %s is not provided", typeToken);
            throw conversionArgumentException(exMessage);
        }
        final Stringifier<I> stringifier = keyStringifierOpt.get();
        return stringifier;
    }

    protected static class TimestampStringifer extends Stringifier<Timestamp> {

        private static final Pattern PATTERN_COLON = Pattern.compile(":");
        private static final Pattern PATTERN_T = Pattern.compile("T");

        @Override
        protected String doForward(Timestamp timestamp) {
            final String result = toIdString(timestamp);
            return result;
        }

        @Override
        protected Timestamp doBackward(String s) {
            try {
                return Timestamps.parse(s);
            } catch (ParseException ignored) {
                throw conversionArgumentException(CONVERSION_EXCEPTION);
            }
        }

        /**
         * Converts the passed timestamp to the string.
         *
         * @param timestamp the value to convert
         * @return the string representation of the timestamp
         */
        private static String toIdString(Timestamp timestamp) {
            checkNotNull(timestamp);
            String result = Timestamps.toString(timestamp);
            result = PATTERN_COLON.matcher(result)
                                  .replaceAll("-");
            result = PATTERN_T.matcher(result)
                              .replaceAll("_T");

            return result;
        }
    }

    protected static class EventIdStringifier extends Stringifier<EventId> {
        @Override
        protected String doForward(EventId eventId) {
            final String result = eventId.getUuid();
            return result;
        }

        @Override
        protected EventId doBackward(String s) {
            final EventId result = EventId.newBuilder()
                                          .setUuid(s)
                                          .build();
            return result;
        }
    }

    protected static class CommandIdStringifier extends Stringifier<CommandId> {
        @Override
        protected String doForward(CommandId commandId) {
            final String result = commandId.getUuid();
            return result;
        }

        @Override
        protected CommandId doBackward(String s) {
            final CommandId result = CommandId.newBuilder()
                                              .setUuid(s)
                                              .build();
            return result;
        }
    }

    /**
     * The stringifier for the {@code Map} classes.
     *
     * <p> The converter for the type of the elements in the map
     * should be registered in the {@code StringifierRegistry} class
     * for the correct usage of the {@code MapStringifier} converter.
     *
     * @param <K> the type of the keys in the map
     * @param <V> the type of the values in the map
     */
    protected static class MapStringifier<K, V> extends Stringifier<Map<K, V>> {

        private static final String DEFAULT_ELEMENTS_DELIMITER = ",";
        private static final String KEY_VALUE_DELIMITER = ":";

        /**
         * The delimiter for the passed elements in the {@code String} representation,
         * {@code DEFAULT_ELEMENTS_DELIMITER} by default.
         */
        private final String delimiter;
        private final Class<K> keyClass;
        private final Class<V> valueClass;

        public MapStringifier(Class<K> keyClass, Class<V> valueClass) {
            super();
            this.keyClass = keyClass;
            this.valueClass = valueClass;
            this.delimiter = DEFAULT_ELEMENTS_DELIMITER;
        }

        /**
         * That constructor should be used when need to use
         * a custom delimiter of the elements during conversion.
         *
         * @param keyClass   the class of the key elements
         * @param valueClass the class of the value elements
         * @param delimiter  the delimiter for the passed elements via string
         */
        public MapStringifier(Class<K> keyClass, Class<V> valueClass, String delimiter) {
            super();
            this.keyClass = keyClass;
            this.valueClass = valueClass;
            this.delimiter = delimiter;
        }

        @Override
        protected String doForward(Map<K, V> map) {
            final String result = map.toString();
            return result;
        }

        @Override
        protected Map<K, V> doBackward(String s) {
            final String[] buckets = s.split(delimiter);
            final Map<K, V> resultMap = newHashMap();

            for (String bucket : buckets) {
                saveConvertedBucket(resultMap, bucket);
            }
            Ints.stringConverter();
            return resultMap;
        }

        private Map<K, V> saveConvertedBucket(Map<K, V> resultMap, String element) {
            final String[] keyValue = element.split(KEY_VALUE_DELIMITER);
            checkKeyValue(keyValue);

            final String key = keyValue[0];
            final String value = keyValue[1];

            try {
                final K convertedKey = getConvertedElement(keyClass, key);
                final V convertedValue = getConvertedElement(valueClass, value);
                resultMap.put(convertedKey, convertedValue);
                return resultMap;
            } catch (Throwable ignored) {
                throw conversionArgumentException(CONVERSION_EXCEPTION);
            }
        }

        @SuppressWarnings("unchecked") // It is OK because class is verified.
        private static <I> I getConvertedElement(Class<I> elementClass, String elementToConvert) {

            if (isIntegerClass(elementClass)) {
                return (I) Ints.stringConverter()
                               .convert(elementToConvert);
            }

            if (isLongClass(elementClass)) {
                return (I) Longs.stringConverter()
                                .convert(elementToConvert);
            }

            if (isStringClass(elementClass)) {
                return (I) elementToConvert;
            }

            final I convertedValue = parse(elementToConvert, TypeToken.of(elementClass));
            return convertedValue;

        }

        private static void checkKeyValue(String[] keyValue) {
            if (keyValue.length != 2) {
                final String exMessage =
                        "Illegal key - value format, key value should be " +
                        "separated with single `:` character";
                throw conversionArgumentException(exMessage);
            }
        }
    }

    private static boolean isStringClass(Class<?> aClass) {
        return String.class.equals(aClass);
    }

    private static boolean isLongClass(Class<?> aClass) {
        return Long.class.equals(aClass);
    }

    private static boolean isIntegerClass(Class<?> aClass) {
        return Integer.class.equals(aClass);
    }
}
