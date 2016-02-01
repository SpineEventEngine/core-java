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

package org.spine3.server;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;

/**
 * A base for {@link Entity} ID value object.
 *
 * <p>An entity ID value can be of one of the following types:
 *   <ul>
 *      <li>String</li>
 *      <li>Long</li>
 *      <li>Integer</li>
 *      <li>A class implementing {@link Message}</li>
 *   </ul>
 *
 * <p>Consider using {@code Message}-based IDs if you want to have typed IDs in your code, and/or
 * if you need to have IDs with some structure inside. Examples of such structural IDs are:
 *   <ul>
 *      <li>EAN value used in bar codes</li>
 *      <li>ISBN</li>
 *      <li>Phone number</li>
 *      <li>email address as a couple of local-part and domain</li>
 *   </ul>
 *
 * @param <I> the type of entity IDs
 *
 * @author Alexander Yevsyukov
 * @author Alexander Litus
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods") // OK in this case
public abstract class EntityId<I> {

    /**
     * The value of the id.
     */
    private final I value;

    private static final ImmutableSet<Class<?>> SUPPORTED_TYPES = ImmutableSet.<Class<?>>builder()
            .add(String.class)
            .add(Long.class)
            .add(Integer.class)
            .add(Message.class)
            .build();

    protected EntityId(I value) {
        checkNotNull(value);
        checkType(value);
        this.value = value;
    }

    /**
     * Ensures that the type of the {@code entityId} is supported.
     *
     * @param entityId the ID of the entity to check
     * @throws IllegalArgumentException if the ID is not of one of the supported types
     */
    public static <I> void checkType(I entityId) {
        final Class<?> idClass = entityId.getClass();
        if (SUPPORTED_TYPES.contains(idClass)) {
            return;
        }
        if (!Message.class.isAssignableFrom(idClass)){
            throw unsupportedIdType(idClass);
        }
    }

    /**
     * Returns the short name of the type of underlying value.
     *
     * @return
     *  <ul>
     *      <li>Short Protobuf type name if the value is {@link Message}.</li>
     *      <li>Simple class name of the value, otherwise.</li>
     *  </ul>
     */
    public String getShortTypeName() {
        if (this.value instanceof Message) {
            //noinspection TypeMayBeWeakened
            final Message message = (Message)this.value;
            final Descriptors.Descriptor descriptor = message.getDescriptorForType();
            final String result = descriptor.getName();
            return result;
        } else {
            final String result = value.getClass().getSimpleName();
            return result;
        }
    }

    @Override
    public String toString() {
        final String result = Identifiers.idToString(value());
        return result;
    }

    public I value() {
        return this.value;
    }

    private static IllegalArgumentException unsupportedIdType(Class<?> idClass) {
        final String message = "Expected one of the following ID types: " + supportedTypesToString() +
                "; found: " + idClass.getName();
        throw new IllegalArgumentException(message);
    }

    private static String supportedTypesToString() {
        final Iterable<String> classStrings = transform(SUPPORTED_TYPES, new Function<Class<?>, String>() {
            @Override
            @SuppressWarnings("NullableProblems") // OK in this case
            public String apply(Class<?> clazz) {
                return clazz.getSimpleName();
            }
        });
        final String result = Joiner.on(", ").join(classStrings);
        return result;
    }
}
