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

import com.google.common.reflect.TypeToken;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import org.spine3.annotations.Internal;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.TextFormat.shortDebugString;
import static org.spine3.protobuf.AnyPacker.unpack;

/**
 * Utility class for working with identifiers.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public class Identifiers {

    /** The suffix of ID fields. */
    public static final String ID_PROPERTY_SUFFIX = "id";
    /** A {@code null} ID string representation. */
    public static final String NULL_ID = "NULL";
    /** An empty ID string representation. */
    public static final String EMPTY_ID = "EMPTY";
    private static final Pattern PATTERN_COLON_SPACE = Pattern.compile(": ");
    private static final String EQUAL_SIGN = "=";

    private Identifiers() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Ensures that the passed class of identifiers is supported.
     *
     * <p>The following types of IDs are supported:
     *   <ul>
     *      <li>{@code String}
     *      <li>{@code Long}
     *      <li>{@code Integer}
     *      <li>A class implementing {@link com.google.protobuf.Message Message}
     *   </ul>
     *
     * <p>Consider using {@code Message}-based IDs if you want to have typed IDs in your code,
     * and/or if you need to have IDs with some structure inside.
     *
     * <p>Examples of such structural IDs are:
     *   <ul>
     *      <li>EAN value used in bar codes
     *      <li>ISBN
     *      <li>Phone number
     *      <li>Email address as a couple of local-part and domain
     *   </ul>
     * @param <I> the type of the ID
     * @param idClass the class of IDs
     * @throws IllegalArgumentException if the class of IDs is not of supported type
     */
    public static <I> void checkSupported(Class<I> idClass) {
        checkNotNull(idClass);
        final Messagifier.Type type = Messagifier.Type.getType(idClass);
        if (!type.isSupportedIdType()) {
            throw Messagifier.Type.unsupportedClass(idClass);
        }
    }

    /**
     * Wraps the passed ID value into an instance of {@link Any}.
     *
     * <p>The passed value must be of one of the supported types listed below.
     * The type of the value wrapped into the returned instance is defined by the type
     * of the passed value:
     * <ul>
     *      <li>For classes implementing {@link com.google.protobuf.Message Message} — the value
     *      of the message itself
     *      <li>For {@code String} — {@link com.google.protobuf.StringValue StringValue}
     *      <li>For {@code Long} — {@link com.google.protobuf.UInt64Value UInt64Value}
     *      <li>For {@code Integer} — {@link com.google.protobuf.UInt32Value UInt32Value}
     * </ul>
     *
     * @param id  the value to wrap
     * @param <I> the type of the value
     * @return instance of {@link Any} with the passed value
     * @throws IllegalArgumentException if the passed value is not of the supported type
     */
    public static <I> Any idToAny(I id) {
        checkNotNull(id);
        checkSupported(id.getClass());
        return Messagifiers.toAny(id);
    }

    /**
     * Extracts ID object from the passed {@code Any} instance.
     *
     * <p>Returned type depends on the type of the message wrapped into {@code Any}:
     * <ul>
     * <li>{@code String} for unwrapped {@link com.google.protobuf.StringValue StringValue}
     * <li>{@code Integer} for unwrapped {@link com.google.protobuf.UInt32Value UInt32Value}
     * <li>{@code Long} for unwrapped {@link com.google.protobuf.UInt64Value UInt64Value}
     * <li>unwrapped {@code Message} instance if its type is none of the above
     * </ul>
     *
     * @param any the ID value wrapped into {@code Any}
     * @return unwrapped ID
     */
    public static Object idFromAny(Any any) {
        checkNotNull(any);
        final Object result = Messagifier.Type.unpack(any);
        checkSupported(result.getClass());
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
        checkNotNull(idClass);
        checkSupported(idClass);
        return Messagifier.getDefaultValue(idClass);
    }

    /**
     * Converts the passed ID value into the string representation.
     *
     * @param id  the value to convert
     * @param <I> the type of the ID
     * @return <ul>
     *              <li>for classes implementing {@link Message} &mdash; a Json form;
     *              <li>for {@code String}, {@code Long}, {@code Integer} &mdash;
     *              the result of {@link Object#toString()};
     *              <li>for {@code null} ID &mdash; the {@link #NULL_ID};
     *              <li>if the result is empty or blank string &mdash; the {@link #EMPTY_ID}.
     *         </ul>
     * @throws IllegalArgumentException if the passed type isn't one of the above or
     *                                  the passed {@link Message} instance has no fields
     * @see StringifierRegistry
     */
    public static <I> String idToString(@Nullable I id) {
        if (id == null) {
            return NULL_ID;
        }

        checkSupported(id.getClass());

        final Messagifier<?> identifier;
        if (id instanceof Any) {
            final Message unpacked = unpack((Any) id);
            identifier = Messagifier.fromMessage(unpacked);
        } else {
            identifier = Messagifier.from(id);
        }

        final String result = identifier.toString();
        return result;
    }

    @SuppressWarnings("unchecked") // OK to cast to String as output type of Stringifier.
    static String idMessageToString(Message message) {
        checkNotNull(message);
        final String result;
        final StringifierRegistry registry = StringifierRegistry.getInstance();
        final Class<? extends Message> msgClass = message.getClass();
        final TypeToken<? extends Message> msgToken = TypeToken.of(msgClass);
        if (registry.hasStringifierFor(msgToken)) {
            @SuppressWarnings("OptionalGetWithoutIsPresent") // OK as we check for presence above.
            final Stringifier converter = registry.get(msgToken)
                                                  .get();
            result = (String) converter.convert(message);
        } else {
            result = convert(message);
        }
        return result;
    }

    private static String convert(Message message) {
        final Collection<Object> values = message.getAllFields()
                                                 .values();
        final String result;
        if (values.isEmpty()) {
            result = EMPTY_ID;
        } else if (values.size() == 1) {
            final Object object = values.iterator()
                                        .next();
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
        result = PATTERN_COLON_SPACE.matcher(result)
                                    .replaceAll(EQUAL_SIGN);
        return result;
    }
}
