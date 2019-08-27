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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.spine.base.FieldPath;
import io.spine.type.MessageClass;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.spine.validate.Validate.isNotDefault;

/**
 * A class method scan operation.
 *
 * <p>Finds handler methods in the given class by the given {@link MethodSignature}.
 *
 * @param <H>
 *         the type of handler method to find
 */
final class MethodScan<H extends HandlerMethod<?, ?, ?, ?>> {

    private final Class<?> declaringClass;
    private final Multimap<HandlerTypeInfo, H> handlers;
    private final Map<HandlerId, H> seenMethods;
    private final MethodSignature<H, ?> signature;
    private final Map<MessageClass, FilteringHandler<H>> fieldFilters;

    private MethodScan(Class<?> declaringClass, MethodSignature<H, ?> signature) {
        this.declaringClass = declaringClass;
        this.signature = signature;
        this.handlers = HashMultimap.create();
        this.seenMethods = new HashMap<>();
        this.fieldFilters = new HashMap<>();
    }

    /**
     * Finds handler methods in the scanned class by the given signature.
     *
     * @param <H>
     *         the type of the handler methods
     * @param declaringClass
     *         the scanned class
     * @param signature
     *         the handler {@linkplain MethodSignature signature}
     * @return map of {@link HandlerTypeInfo}s to the handler methods of the given type
     */
    static <H extends HandlerMethod<?, ?, ?, ?>> ImmutableMultimap<HandlerTypeInfo, H>
    findMethodsBy(Class<?> declaringClass, MethodSignature<H, ?> signature) {
        MethodScan<H> operation = new MethodScan<>(declaringClass, signature);
        ImmutableMultimap<HandlerTypeInfo, H> result = operation.perform();
        return result;
    }

    /**
     * Performs the operation.
     *
     * <p>Multiple calls to this method may cause {@link DuplicateHandlerMethodError}s.
     *
     * @return a map of {@link HandlerTypeInfo}s to the method handlers
     */
    ImmutableMultimap<HandlerTypeInfo, H> perform() {
        Method[] declaredMethods = declaringClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            scanMethod(method);
        }
        return ImmutableMultimap.copyOf(handlers);
    }

    private void scanMethod(Method method) {
        Optional<H> handlerMethod = signature.create(method);
        if (handlerMethod.isPresent()) {
            H handler = handlerMethod.get();
            remember(handler);
        }
    }

    private void remember(H handler) {
        checkNotRemembered(handler);
        checkNotClashes(handler);
        HandlerId id = handler.id();
        handlers.put(id.getType(), handler);
    }

    private void checkNotRemembered(H handler) {
        HandlerId id = handler.id();
        if (seenMethods.containsKey(id)) {
            Method alreadyPresent = seenMethods.get(id)
                                               .rawMethod();
            String methodName = alreadyPresent.getName();
            String duplicateMethodName = handler.rawMethod().getName();
            throw new DuplicateHandlerMethodError(declaringClass, id,
                                                  methodName, duplicateMethodName);
        } else {
            seenMethods.put(id, handler);
        }
    }

    private void checkNotClashes(H handler) {
        MessageClass handledClass = handler.messageClass();
        FieldPath field = handler.filter().getField();
        if (isNotDefault(field)) {
            FilteringHandler<H> previousValue = fieldFilters.put(
                    handledClass,
                    new FilteringHandler<>(handler, field)
            );
            if (previousValue != null && previousValue.fieldDiffersFrom(field)) {
                throw new HandlerFieldFilterClashError(declaringClass,
                                                       handler.rawMethod(),
                                                       previousValue.handler().rawMethod());
            }
        }
    }
}
