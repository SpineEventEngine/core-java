/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

final class ReturnTypeAnalyzer {

    private final Method method;

    ReturnTypeAnalyzer(Method method) {
        this.method = method;
    }

    ImmutableSet<Class<? extends Message>> scanForEmittedMessages() {
        Class<?> type = method.getReturnType();
        Type genericType = method.getGenericReturnType();
        return ImmutableSet.of();
    }

    private Optional<?> a() {
        Class<?> producedType = method.getReturnType();

        if (Optional.class.isAssignableFrom(producedType)) {
            ParameterizedType parameterized = (ParameterizedType) method.getGenericReturnType();
            Type firstGenericParam = parameterized.getActualTypeArguments()[0];
            producedType = (Class) firstGenericParam;
        } else if (Iterable.class.isAssignableFrom(producedType)) {
            TypeToken token = TypeToken.of(method.getGenericReturnType());
            TypeToken supertype = token.getSupertype(Iterable.class);
        }
        // todo use common IGNORED_MESSAGES with method result filters.
        // todo check both Iterable/Optional/raw type is not one of the Message, EventMessage,
        // todo SerializableMessage.
        // todo for iterable/optional: check type assignableFrom EventMessage, then check it's not EventMessage.
        // todo for raw type we know it's EventMessage, so just check it's not it.
        if (Nothing.class.isAssignableFrom(producedType)
                || Empty.class.isAssignableFrom(producedType)) {
            return Optional.empty();
        }
        if (Message.class.isAssignableFrom(producedType)) {
            Class<? extends Message> messageClass = (Class<? extends Message>) producedType;
            return Optional.of(messageClass);
        }
        return Optional.empty();
    }
}
