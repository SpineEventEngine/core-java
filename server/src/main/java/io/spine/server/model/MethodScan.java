/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import io.spine.type.MessageClass;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
    private final MethodSignature<H, ?> signature;
    private final Multimap<DispatchKey, H> handlers;
    private final Map<DispatchKey, H> seenMethods;
    private final Map<MessageClass<?>, HandlerMethod<?, ?, ?, ?>> messageToHandler;

    /**
     * Finds handler methods in the scanned class by the given signature.
     *
     * @param <H>
     *         the type of the handler methods
     * @param declaringClass
     *         the scanned class
     * @param signature
     *         the handler {@linkplain MethodSignature signature}
     * @return the map with handler methods of the given type
     */
    static <H extends HandlerMethod<?, ?, ?, ?>> ImmutableSetMultimap<DispatchKey, H>
    findMethodsBy(Class<?> declaringClass, MethodSignature<H, ?> signature) {
        var operation = new MethodScan<>(declaringClass, signature);
        var result = operation.perform();
        return result;
    }

    private MethodScan(Class<?> declaringClass, MethodSignature<H, ?> signature) {
        this.declaringClass = declaringClass;
        this.signature = signature;
        this.handlers = HashMultimap.create();
        this.seenMethods = new HashMap<>();
        this.messageToHandler = new HashMap<>();
    }

    /**
     * Performs the operation.
     *
     * <p>Multiple calls to this method may cause {@link DuplicateHandlerMethodError}s.
     */
    private ImmutableSetMultimap<DispatchKey, H> perform() {
        var declaredMethods = declaringClass.getDeclaredMethods();
        for (var method : declaredMethods) {
            if (!method.isBridge() && !method.isSynthetic()) {
                scanMethod(method);
            }
        }
        return ImmutableSetMultimap.copyOf(handlers);
    }

    private void scanMethod(Method method) {
        signature.classify(method)
                 .ifPresent(this::remember);
    }

    private void remember(H handler) {
        checkNotRemembered(handler);
        checkForFilterClashes(handler);
        handlers.put(handler.key().withoutFilter(), handler);
    }

    private void checkNotRemembered(H handler) {
        var key = handler.key();
        if (seenMethods.containsKey(key)) {
            var alreadyPresent = seenMethods.get(key)
                                            .rawMethod();
            var methodName = alreadyPresent.getName();
            var duplicateMethodName = handler.rawMethod().getName();
            throw new DuplicateHandlerMethodError(
                    declaringClass, key, methodName, duplicateMethodName
            );
        } else {
            seenMethods.put(key, handler);
        }
    }

    private void checkForFilterClashes(HandlerMethod<?, ?, ?, ?> handler) {
        var filter = handler.filter();
        if (filter.acceptsAll()) {
            return;
        }
        var handledClass = handler.messageClass();

        // It is OK to keep only the last filtering handler in the map (and not all of them)
        // because filtered fields are required to be the same.
        var existingHandler = messageToHandler.put(handledClass, handler);
        if (existingHandler != null && !filter.sameField(existingHandler.filter())) {
            // There is already a handler for this message class.
            // Check that the field which is used as the condition for filtering is the same.
            // It's not allowed to have filtered handlers by various fields because it
            // makes the dispatching ambiguous: "Do we need to dispatch to this handler
            // and that handler too?"
            //
            // We allow multiple handlers for the same message type with filters by
            // different values. It allows to split logic into smaller methods instead of having
            // if-else chains (that branch by different values) inside a bigger handler method.
            //
            throw new HandlerFieldFilterClashError(
                    declaringClass, handler.rawMethod(), existingHandler.rawMethod()
            );
        }
    }
}
