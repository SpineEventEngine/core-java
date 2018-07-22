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

import com.google.protobuf.Message;
import io.spine.type.MessageClass;

import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Describes a method that accepts a message and optionally its context.
 *
 * <p>Two message handlers are equivalent when they refer to the same method on the
 * same object (not class).
 *
 * @param <M> the type of the message class
 * @param <C> the type of the message context or {@link com.google.protobuf.Empty Empty} if
 *            a context parameter is never used
 *            
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 */
public interface HandlerMethod<M extends MessageClass, C extends Message> {

    M getMessageClass();

    HandlerKey key();

    Set<MethodAttribute<?>> getAttributes();

    /**
     * Invokes the method to handle {@code message} with the {@code context}.
     *
     * @param target  the target object on which call the method
     * @param message the message to handle
     * @param context the context of the message
     * @return the result of message handling
     */
    Object invoke(Object target, Message message, C context);

    /**
     * Creates a predicate to remove the {@linkplain HandlerMethod handler methods}
     * that are not marked {@linkplain ExternalAttribute#EXTERNAL external}.
     *
     * @param <M> the type of the {@code HandlerMethod} to apply this predicate to
     * @return the predicate
     */
    static <M extends HandlerMethod<?, ?>> Predicate<M> external() {
        return input -> {
            M method = checkNotNull(input);
            boolean result = isExternal(method);
            return result;
        };
    }

    /**
     * Creates a predicate to remove the {@linkplain HandlerMethod handler methods}
     * that are marked {@linkplain ExternalAttribute#EXTERNAL external}.
     *
     * @param <M> the type of the {@code HandlerMethod} to apply this predicate to
     * @return the predicate
     */
    static <M extends HandlerMethod<?, ?>> Predicate<M> domestic() {
        return input -> {
            M method = checkNotNull(input);
            boolean result = !isExternal(method);
            return result;
        };
    }

    /**
     * Verifies if the passed method is external.
     */
    static <M extends HandlerMethod<?, ?>> boolean isExternal(M method) {
        return method.getAttributes()
                     .contains(ExternalAttribute.EXTERNAL);
    }

    /**
     * Ensures that the {@code external} attribute of the {@linkplain HandlerMethod method} is
     * the one expected.
     *
     * <p>{@link IllegalArgumentException} is thrown if the value does not meet the expectations.
     *
     * @param method           the method to check
     * @param shouldBeExternal an expected value of {@code external} attribute.
     * @see ExternalAttribute
     */
    static void ensureExternalMatch(HandlerMethod<?, ?> method, boolean shouldBeExternal) {
        checkArgument(isExternal(method) == shouldBeExternal,
                      "Mismatch of `external` value for the handler method %s. " +
                              "Expected `external` = %s, but got the other way around.", method,
                      shouldBeExternal);
    }
}
