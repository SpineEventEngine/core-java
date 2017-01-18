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
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.util.Timestamps;
import org.spine3.Internal;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.google.protobuf.TextFormat.shortDebugString;
import static org.spine3.protobuf.AnyPacker.unpack;

/**
 * This class manages conversion of identifies to/from string.
 *
 * <p>In addition to utility methods for the conversion, it provides {@link ConverterRegistry}
 * which allows to provide custom conversion logic for user-defined types of identifies.
 *
 * @author Alexander Litus
 */
public class Identifiers {

    /** A {@code null} ID string representation. */
    public static final String NULL_ID = "NULL";

    /** An empty ID string representation. */
    public static final String EMPTY_ID = "EMPTY";

    /** The suffix of ID fields. */
    public static final String ID_PROPERTY_SUFFIX = "id";

    private static final Pattern PATTERN_COLON_SPACE = Pattern.compile(": ");
    private static final Pattern PATTERN_COLON = Pattern.compile(":");
    private static final Pattern PATTERN_T = Pattern.compile("T");
    private static final String EQUAL_SIGN = "=";

    private Identifiers() {}

    /**
     * Verifies if the passed class of identifiers is supported.
     *
     * @param <I> the type of the ID
     * @param idClass the class of IDs
     * @throws IllegalArgumentException if the class of IDs is not supported
     */
    public static <I> void checkSupported(Class<I> idClass) {
        Identifier.Type.getType(idClass);
    }

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
     *                                  the passed {@link Message} instance has no fields
     * @see ConverterRegistry
     */
    public static <I> String idToString(@Nullable I id) {
        if (id == null) {
            return NULL_ID;
        }

        final Identifier<?> identifier;
        if (id instanceof Any) {
            final Message unpacked = unpack((Any) id);
            identifier = Identifier.fromMessage(unpacked);
        } else {
            identifier = Identifier.from(id);
        }

        final String result = identifier.toString();
        return result;
    }

    static String idMessageToString(Message message) {
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
        final Collection<Object> values = message.getAllFields().values();
        final String result;
        if (values.isEmpty()) {
            result = EMPTY_ID;
        } else if (values.size() == 1) {
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
     * Wraps the passed ID value into an instance of {@link Any}.
     *
     * <p>The passed value must be of one of the supported types listed below.
     * The type of the value wrapped into the returned instance is defined by the type
     * of the passed value:
     * <ul>
     *      <li>For classes implementing {@link Message} — the value of the message itself
     *      <li>For {@code String} — {@link StringValue}
     *      <li>For {@code Long} — {@link UInt64Value}
     *      <li>For {@code Integer} — {@link UInt32Value}
     * </ul>
     *
     * @param id  the value to wrap
     * @param <I> the type of the value
     * @return instance of {@link Any} with the passed value
     * @throws IllegalArgumentException if the passed value is not of the supported type
     */
    public static <I> Any idToAny(I id) {
        final Identifier<I> identifier = Identifier.from(id);
        final Any anyId = identifier.pack();
        return anyId;
    }

    /**
     * Extracts ID object from the passed {@link Any} instance.
     *
     * <p>Returned type depends on the type of the message wrapped into {@code Any}.
     *
     * @param any the ID value wrapped into {@code Any}
     * @return <ul>
     * <li>{@code String} value if {@link StringValue} is unwrapped
     * <li>{@code Integer} value if {@link UInt32Value} is unwrapped
     * <li>{@code Long} value if {@link UInt64Value} is unwrapped
     * <li>unwrapped {@code Message} instance if its type is none of the above
     * </ul>
     */
    public static Object idFromAny(Any any) {
        final Object result = Identifier.Type.unpack(any);
        return result;
    }

    /**
     * Converts the passed timestamp to the string, which will serve as ID.
     *
     * @param timestamp the value to convert
     * @return string representation of timestamp-based ID
     */
    public static String timestampToIdString(Timestamp timestamp) {
        String result = Timestamps.toString(timestamp);
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

    /**
     * Obtains a default value for an identifier of the passed class.
     */
    @Internal
    public static <I> I getDefaultValue(Class<I> idClass) {
        return Identifier.getDefaultValue(idClass);
    }

    static class TimestampToStringConverter implements Function<Timestamp, String> {
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
