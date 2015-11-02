/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.util;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.*;
import com.google.protobuf.util.TimeUtil;
import org.spine3.protobuf.Messages;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.protobuf.TextFormat.shortDebugString;
import static org.spine3.protobuf.Messages.fromAny;

/*
 * Utility class for Entity ids conversation.
 */
@SuppressWarnings("UtilityClass")
public class Identifiers {

    /*
     * Delimiter between user id and time in string representation
     */
    public static final char USER_ID_AND_TIME_DELIMITER = '@';

    /*
     * Delimiter between command time and event time delta in string representation
     */
    public static final char TIME_DELIMITER = '+';

    /*
     * Null message or field string representation
     */
    public static final String NULL_ID_OR_FIELD = "NULL";

    /**
     * The suffix of ID fields.
     */
    public static final String ID_PROPERTY_SUFFIX = "id";

    /**
     * Aggregate ID must be the first field of aggregate commands.
     */
    public static final int AGGREGATE_ID_FIELD_INDEX_IN_COMMANDS = 0;

    private static final Pattern PATTERN_COLON_SPACE = Pattern.compile(": ");
    private static final Pattern PATTERN_COLON = Pattern.compile(":");
    private static final Pattern PATTERN_T = Pattern.compile("T");
    private static final String EQUAL_SIGN = "=";


    private Identifiers() {}

    /**
     * Converts the passed ID value into the string representation.
     *
     * @param id  the value to convert
     * @param <I> the type of the ID
     * @return <ul>
     * <li>For classes implementing {@link Message} — Json form</li>
     * <li>For {@code String}, {@code Long}, {@code Integer} — the result of {@link Object#toString()}</li>
     * </ul>
     * @throws IllegalArgumentException if the passed type isn't one of the above or
     *         the passed {@link Message} instance has no fields
     */
    @SuppressWarnings({"ChainOfInstanceofChecks", "IfStatementWithTooManyBranches"})
    public static <I> String idToString(@Nullable I id) {

        if (id == null) {
            return NULL_ID_OR_FIELD;
        }

        String result;

        final boolean isStringOrNumber = id instanceof String
                || id instanceof Integer
                || id instanceof Long;

        if (isStringOrNumber) {
            result = id.toString();
        } else if (id instanceof Any) {
            final Message messageFromAny = fromAny((Any) id);
            result = idMessageToString(messageFromAny);
        } else if (id instanceof Message) {
            result = idMessageToString((Message) id);
        } else {
            throw unsupportedIdType(id);
        }

        if (isNullOrEmpty(result) || result.trim().isEmpty()) {
            result = NULL_ID_OR_FIELD;
        }

        result = result.trim();

        return result;
    }

    private static String idMessageToString(Message message) {

        final String result;
        final IdConverterRegistry registry = IdConverterRegistry.getInstance();

        if (registry.containsConverter(message)) {
            final Function<Message, String> converter = registry.getConverter(message);
            result = converter.apply(message);
        } else {
            result = convert(message);
        }

        return result;
    }

    @SuppressWarnings({"TypeMayBeWeakened", "IfMayBeConditional"})
    private static String convert(Message message) {

        final String result;
        final Collection<Object> values = message.getAllFields().values();

        if (values.isEmpty()) {
            throw new IllegalArgumentException("ID must have at least one field. Encountered: " + message);
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
     * <p/>
     * <p>The passed value must be of one of the supported types listed below.
     * The type of the value wrapped into the returned instance is defined by the type
     * of the passed value:
     * <ul>
     * <li>For classes implementing {@link com.google.protobuf.Message} — the value of the message itself</li>
     * <li>For {@code String} — {@link com.google.protobuf.StringValue}</li>
     * <li>For {@code Long} — {@link com.google.protobuf.UInt64Value}</li>
     * <li>For {@code Integer} — {@link com.google.protobuf.UInt32Value}</li>
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
            anyId = Messages.toAny(message);
        } else if (id instanceof String) {
            final String s = (String) id;
            anyId = Messages.toAny(StringValue.newBuilder().setValue(s).build());
        } else if (id instanceof Integer) {
            final Integer intValue = (Integer) id;
            anyId = Messages.toAny(UInt32Value.newBuilder().setValue(intValue).build());
        } else if (id instanceof Long) {
            final Long longValue = (Long) id;
            anyId = Messages.toAny(UInt64Value.newBuilder().setValue(longValue).build());
        } else {
            throw unsupportedIdType(id);
        }
        return anyId;
    }

    /**
     * Extracts ID object from the passed {@link com.google.protobuf.Any} instance.
     * <p/>
     * <p>Returned type depends on the type of the message wrapped into {@code Any}.
     *
     * @param idInAny the ID value wrapped into {@code Any}
     * @return <ul>
     * <li>{@code String} value if {@link com.google.protobuf.StringValue} is unwrapped</li>
     * <li>{@code Integer} value if {@link com.google.protobuf.UInt32Value} is unwrapped</li>
     * <li>{@code Long} value if {@link com.google.protobuf.UInt64Value} is unwrapped</li>
     * <li>unwrapped {@code Message} instance if its type is none of the above</li>
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
     * Converts the passed timestamp to human-readable string representation.
     *
     * @param timestamp  the value to convert
     * @return string representation of timestamp
     */
    public static String timestampToString(Timestamp timestamp) {
        String result = TimeUtil.toString(timestamp);
        result = PATTERN_COLON.matcher(result).replaceAll("-");
        result = PATTERN_T.matcher(result).replaceAll("_T");

        return result;
    }

    private static <I> IllegalArgumentException unsupportedIdType(I id) {
        return new IllegalArgumentException("ID of unsupported type encountered: " + id);
    }

    /**
     * The registry of converters of ID types to string representations.
     */
    public static class IdConverterRegistry {

        private final Map<Class<?>, Function<?, String>> entries = newHashMap(
                ImmutableMap.<Class<?>, Function<?, String>>builder()
                        .put(Timestamp.class, new TimestampToStringConverter())
                        .build()
        );

        private IdConverterRegistry() {
        }

        public <I extends Message> void register(Class<I> idClass, Function<I, String> converter) {
            checkNotNull(idClass);
            checkNotNull(converter);
            entries.put(idClass, converter);
        }

        public <I> Function<I, String> getConverter(I id) {
            final Function<?, String> func = entries.get(id.getClass());

            @SuppressWarnings("unchecked") /** The cast is safe as we check the first type when adding.
                @see #register(Class, com.google.common.base.Function) */
            final Function<I, String> result = (Function<I, String>) func;
            return result;
        }

        public <I> boolean containsConverter(I id) {
            final Class<?> idClass = id.getClass();
            final boolean contains = entries.containsKey(idClass) && (entries.get(idClass) != null);
            return contains;
        }

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final IdConverterRegistry value = new IdConverterRegistry();
        }

        public static IdConverterRegistry getInstance() {
            return Singleton.INSTANCE.value;
        }

    }

    private static class TimestampToStringConverter implements Function<Timestamp, String> {
        @Override
        public String apply(@Nullable Timestamp timestamp) {
            if (timestamp == null) {
                return NULL_ID_OR_FIELD;
            }
            final String result = timestampToString(timestamp);
            return result;
        }
    }
}
