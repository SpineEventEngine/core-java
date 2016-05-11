/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.StringValue;
import com.google.protobuf.StringValueOrBuilder;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.util.TimeUtil;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.protobuf.TextFormat.shortDebugString;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.protobuf.Values.newStringValue;

/**
 * This class manages conversion of identifies to/from string.
 *
 * <p>In addition to utility methods for the conversion, it provides {@link ConverterRegistry}
 * which allows to provide custom conversion logic for user-defined types of identifies.
 *
 * @author Alexander Litus
 */
public class Identifiers {

    private Identifiers() {}

    /**
     * A {@code null} ID string representation.
     */
    public static final String NULL_ID = "NULL";

    /**
     * An empty ID string representation.
     */
    public static final String EMPTY_ID = "EMPTY";

    /**
     * The suffix of ID fields.
     */
    public static final String ID_PROPERTY_SUFFIX = "id";

    private static final Pattern PATTERN_COLON_SPACE = Pattern.compile(": ");
    private static final Pattern PATTERN_COLON = Pattern.compile(":");
    private static final Pattern PATTERN_T = Pattern.compile("T");
    private static final String EQUAL_SIGN = "=";


    /**
     * Converts the passed ID value into the string representation.
     *
     * @param id  the value to convert
     * @param <I> the type of the ID
     * @return <ul>
     * <li>for classes implementing {@link Message} &mdash; a Json form;
     * <li>for {@code String}, {@code Long}, {@code Integer} &mdash; the result of {@link Object#toString()};
     * <li>for {@code null} ID &mdash; the {@link #NULL_ID};
     * <li>if the result is empty or blank string &mdash; the {@link #EMPTY_ID}.
     * </ul>
     * @throws IllegalArgumentException if the passed type isn't one of the above or
     *         the passed {@link Message} instance has no fields
     * @see ConverterRegistry
     */
    public static <I> String idToString(@Nullable I id) {
        if (id == null) {
            return NULL_ID;
        }
        String result;
        if (isStringOrNumber(id)) {
            result = id.toString();
        } else if (id instanceof Any) {
            final Message messageFromAny = fromAny((Any) id);
            result = idMessageToString(messageFromAny);
        } else if (id instanceof Message) {
            result = idMessageToString((Message) id);
        } else {
            throw unsupportedIdType(id);
        }
        result = result.trim();
        if (result.isEmpty()) {
            result = EMPTY_ID;
        }
        return result;
    }

    private static <I> boolean isStringOrNumber(I id) {
        final boolean result = id instanceof String
                || id instanceof Integer
                || id instanceof Long;
        return result;
    }

    private static String idMessageToString(Message message) {
        final String result;
        final ConverterRegistry registry = ConverterRegistry.getInstance();
        if (registry.containsConverter(message)) {
            final Function<Message, String> converter = registry.getConverter(message);
            result = converter.apply(message);
        } else {
            result = convert(message);
        }
        return result;
    }

    private static String convert(Message message) {
        final String result;
        final Collection<Object> values = message.getAllFields().values();
        if (values.isEmpty()) {
            throw new IllegalArgumentException("ID must have at least one field. Encountered: " +
                                                       message.getClass().getName());
        }
        if (values.size() == 1) {
            final Object object = values.iterator().next();
            if (object instanceof Message) {
                result = idMessageToString((Message) object);
            } else {
                result = object.toString();
            }
        } else {
            result = messageWithMultipleFieldsToString(message);
        }
        return result;
    }

    private static String messageWithMultipleFieldsToString(MessageOrBuilder message) {
        String result = shortDebugString(message);
        result = PATTERN_COLON_SPACE.matcher(result).replaceAll(EQUAL_SIGN);
        return result;
    }

    /**
     * Wraps the passed ID value into an instance of {@link com.google.protobuf.Any}.
     *
     * <p>The passed value must be of one of the supported types listed below.
     * The type of the value wrapped into the returned instance is defined by the type
     * of the passed value:
     * <ul>
     * <li>For classes implementing {@link com.google.protobuf.Message} — the value of the message itself
     * <li>For {@code String} — {@link com.google.protobuf.StringValue}
     * <li>For {@code Long} — {@link com.google.protobuf.UInt64Value}
     * <li>For {@code Integer} — {@link com.google.protobuf.UInt32Value}
     * </ul>
     *
     * @param id  the value to wrap
     * @param <I> the type of the value
     * @return instance of {@link com.google.protobuf.Any} with the passed value
     * @throws IllegalArgumentException if the passed value is not of the supported type
     */
    public static <I> Any idToAny(I id) {
        final Any anyId;
        //noinspection IfStatementWithTooManyBranches,ChainOfInstanceofChecks
        if (id instanceof Message) {
            final Message message = (Message) id;
            anyId = toAny(message);
        } else if (id instanceof String) {
            final String strValue = (String) id;
            anyId = toAny(newStringValue(strValue));
        } else if (id instanceof Integer) {
            final Integer intValue = (Integer) id;
            anyId = toAny(UInt32Value.newBuilder().setValue(intValue).build());
        } else if (id instanceof Long) {
            final Long longValue = (Long) id;
            anyId = toAny(UInt64Value.newBuilder().setValue(longValue).build());
        } else {
            throw unsupportedIdType(id);
        }
        return anyId;
    }

    /**
     * Extracts ID object from the passed {@link com.google.protobuf.Any} instance.
     *
     * <p>Returned type depends on the type of the message wrapped into {@code Any}.
     *
     * @param idInAny the ID value wrapped into {@code Any}
     * @return <ul>
     * <li>{@code String} value if {@link com.google.protobuf.StringValue} is unwrapped
     * <li>{@code Integer} value if {@link com.google.protobuf.UInt32Value} is unwrapped
     * <li>{@code Long} value if {@link com.google.protobuf.UInt64Value} is unwrapped
     * <li>unwrapped {@code Message} instance if its type is none of the above
     * </ul>
     */
    public static Object idFromAny(Any idInAny) {
        final Message extracted = fromAny(idInAny);

        //noinspection ChainOfInstanceofChecks
        if (extracted instanceof StringValue) {
            final StringValueOrBuilder stringValue = (StringValue) extracted;
            return stringValue.getValue();
        }
        if (extracted instanceof UInt32Value) {
            final UInt32Value uInt32Value = (UInt32Value) extracted;
            return uInt32Value.getValue();
        }
        if (extracted instanceof UInt64Value) {
            final UInt64Value uInt64Value = (UInt64Value) extracted;
            return uInt64Value.getValue();
        }
        return extracted;
    }

    /**
     * Converts the passed timestamp to the string, which will serve as ID.
     *
     * @param timestamp the value to convert
     * @return string representation of timestamp-based ID
     */
    public static String timestampToIdString(Timestamp timestamp) {
        String result = TimeUtil.toString(timestamp);
        result = PATTERN_COLON.matcher(result).replaceAll("-");
        result = PATTERN_T.matcher(result).replaceAll("_T");

        return result;
    }

    /**
     * Generates a new random UUID.
     *
     * @return the generated value
     * @see UUID#randomUUID()
     */
    public static String newUuid() {
        final String id = UUID.randomUUID().toString();
        return id;
    }

    private static <I> IllegalArgumentException unsupportedIdType(I id) {
        return new IllegalArgumentException("ID of unsupported type encountered: " + id);
    }

    /**
     * The registry of converters of ID types to string representations.
     */
    public static class ConverterRegistry {

        private final Map<Class<?>, Function<?, String>> entries = newHashMap(
                ImmutableMap.<Class<?>, Function<?, String>>builder()
                        .put(Timestamp.class, new TimestampToStringConverter())
                        .put(EventId.class, new EventIdToStringConverter())
                        .put(CommandId.class, new CommandIdToStringConverter())
                        .build()
        );

        private ConverterRegistry() {
        }

        public <I extends Message> void register(Class<I> idClass, Function<I, String> converter) {
            checkNotNull(idClass);
            checkNotNull(converter);
            entries.put(idClass, converter);
        }

        public <I> Function<I, String> getConverter(I id) {
            checkNotNull(id);
            checkNotClass(id);

            final Function<?, String> func = entries.get(id.getClass());

            @SuppressWarnings("unchecked") /** The cast is safe as we check the first type when adding.
                @see #register(Class, com.google.common.base.Function) */
            final Function<I, String> result = (Function<I, String>) func;
            return result;
        }

        private static <I> void checkNotClass(I id) {
            if (id instanceof Class) {
                checkArgument(false, "Class instance passed instead of value: " + id.toString());
            }
        }

        public <I> boolean containsConverter(I id) {
            final Class<?> idClass = id.getClass();
            final boolean contains = entries.containsKey(idClass) && (entries.get(idClass) != null);
            return contains;
        }

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final ConverterRegistry value = new ConverterRegistry();
        }

        public static ConverterRegistry getInstance() {
            return Singleton.INSTANCE.value;
        }

    }

    private static class TimestampToStringConverter implements Function<Timestamp, String> {
        @Override
        public String apply(@Nullable Timestamp timestamp) {
            if (timestamp == null) {
                return NULL_ID;
            }
            final String result = timestampToIdString(timestamp);
            return result;
        }
    }

    public static class EventIdToStringConverter implements Function<EventId, String> {
        @Override
        public String apply(@Nullable EventId eventId) {
            if (eventId == null) {
                return NULL_ID;
            }
            return eventId.getUuid();
        }

    }

    public static class CommandIdToStringConverter implements Function<CommandId, String> {
        @Override
        public String apply(@Nullable CommandId commandId) {
            if (commandId == null) {
                return NULL_ID;
            }
            return commandId.getUuid();
        }
    }
}
