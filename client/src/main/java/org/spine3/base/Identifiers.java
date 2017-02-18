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

import com.google.protobuf.Any;
import org.spine3.Internal;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with identifiers.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public class Identifiers {

    /** The suffix of ID fields. */
    public static final String ID_PROPERTY_SUFFIX = "id";

    private Identifiers() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Verifies if the passed class of identifiers is supported.
     *
     * @param <I> the type of the ID
     * @param idClass the class of IDs
     * @throws IllegalArgumentException if the class of IDs is not of supported type
     */
    private static <I> void checkSupported(Class<I> idClass) {
        checkNotNull(idClass);
        Identifier.Type.getType(idClass);
    }

    /**
     * Ensures that the passed ID is of supported type.
     *
     * <p>The following types are supported:
     *   <ul>
     *      <li>{@code String}
     *      <li>{@code Long}
     *      <li>{@code Integer}
     *      <li>A class implementing {@link com.google.protobuf.Message Message}
     *   </ul>
     *
     * <p>Consider using {@code Message}-based IDs if you want to have typed IDs in your code,
     * and/or if you need to have IDs with some structure inside.
     * Examples of such structural IDs are:
     *   <ul>
     *      <li>EAN value used in bar codes
     *      <li>ISBN
     *      <li>Phone number
     *      <li>email address as a couple of local-part and domain
     *   </ul>
     *
     * @param id the identifier
     * @param <I> the type of the identifier
     * @return the passed value
     * @throws IllegalArgumentException if the ID is not of supported type
     */
    public static <I> I checkSupported(I id) {
        checkSupported(id.getClass());
        return id;
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
        final Identifier<I> identifier = Identifier.from(id);
        final Any anyId = identifier.pack();
        return anyId;
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
        final Object result = Identifier.Type.unpack(any);
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
        return Identifier.getDefaultValue(idClass);
    }
}
