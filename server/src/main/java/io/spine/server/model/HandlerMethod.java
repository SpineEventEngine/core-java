/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.model;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import io.spine.core.MessageEnvelope;
import io.spine.type.MessageClass;

import java.lang.reflect.Method;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Describes a method that accepts a message and optionally its context.
 *
 * @param <T> the type of the target object
 * @param <M> the type of the incoming message class
 * @param <E> the type of the {@link MessageEnvelope} wrapping the method arguments
 * @param <R> the type of the method result object
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 */
@Immutable
public interface HandlerMethod<T,
                               M extends MessageClass,
                               E extends MessageEnvelope<?, ?, ?>,
                               R extends MethodResult> {

    /**
     * @return the type of the incoming message class
     */
    M getMessageClass();

    /**
     * @return the key of the handler method
     */
    HandlerKey key();

    /**
     * @return the set of method attributes configured for this method
     */
    Set<MethodAttribute<?>> getAttributes();

    /**
     * @return the handling method
     */
    Method getRawMethod();

    /**
     * Invokes the method to handle {@code message} with the {@code context}.
     *
     * @param target
     *         the target object on which call the method
     * @param envelope
     *         the {@link MessageEnvelope} wrapping the method arguments
     * @return the result of message handling
     */
    @CanIgnoreReturnValue
    R invoke(T target, E envelope);

    /**
     * Tells if the passed method is {@linkplain ExternalAttribute#EXTERNAL external}.
     */
    default boolean isExternal() {
        return getAttributes().contains(ExternalAttribute.EXTERNAL);
    }

    /**
     * Tells if the passed method is domestic, that is not marked as
     * {@linkplain ExternalAttribute#EXTERNAL external}).
     */
    default boolean isDomestic() {
        return !isExternal();
    }

    /**
     * Ensures that the {@code external} attribute of the {@linkplain HandlerMethod method} is
     * the one expected.
     *
     * <p>This method is for checking that an {@code external} attribute of a message context
     * matches the one set for the handler method.
     *
     * @param expectedValue
     *         an expected value of the {@code external} attribute
     * @throws IllegalArgumentException
     *         is thrown if the value does not meet the expectation.
     * @see ExternalAttribute
     */
    default void ensureExternalMatch(boolean expectedValue) {
        checkArgument(isExternal() == expectedValue,
                      "Mismatch of `external` value for the handler method %s. " +
                              "Expected `external` = %s, but got the other way around.", this,
                      expectedValue);
    }
}
