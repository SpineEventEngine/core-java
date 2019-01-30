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

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.spine.base.FieldPath;
import io.spine.server.model.declare.MethodSignature;
import io.spine.type.MessageClass;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static io.spine.validate.Validate.isNotDefault;

/**
 * A scanner of a model class.
 *
 * <p>An instance of this class helps to disassemble a Java class into several parts, which are of
 * interest of a domain model.
 */
public final class ClassScanner {

    private final Class<?> declaringClass;

    private ClassScanner(Class<?> cls) {
        this.declaringClass = cls;
    }

    /**
     * Creates a scanner for the given class.
     *
     * @param cls
     *         the class to scan
     * @return new instance of {@code ClassScanner}
     */
    public static ClassScanner of(Class<?> cls) {
        checkNotNull(cls);
        ClassScanner scanner = new ClassScanner(cls);
        return scanner;
    }

    /**
     * Finds handler methods in the scanned class by the given signature.
     *
     * @param signature
     *         the handler {@linkplain MethodSignature signature}
     * @param <H>
     *         the type of the handler methods
     * @return map of {@link HandlerTypeInfo}s to the handler methods of the given type
     */
    <H extends HandlerMethod<?, ?, ?, ?, ?>> ImmutableMultimap<HandlerTypeInfo, H>
    findMethodsBy(MethodSignature<H, ?> signature) {
        MethodScan<H> operation = new MethodScan<>(declaringClass, signature);
        ImmutableMultimap<HandlerTypeInfo, H> result = operation.perform();
        return result;
    }

    /**
     * A class method scan operation.
     *
     * <p>Finds handler methods in the given class by the given {@link MethodSignature}.
     *
     * @param <H>
     *         the type of handler method to find
     */
    private static final class MethodScan<H extends HandlerMethod<?, ?, ?, ?, ?>> {

        private final Class<?> declaringClass;
        private final Multimap<HandlerTypeInfo, H> handlers;
        private final Map<HandlerId, H> seenMethods;
        private final MethodSignature<H, ?> signature;
        private final Map<MessageClass, FilteredHandler<H>> fieldFilters;

        private MethodScan(Class<?> declaringClass,
                           MethodSignature<H, ?> signature) {
            this.declaringClass = declaringClass;
            this.signature = signature;
            this.handlers = HashMultimap.create();
            this.seenMethods = newHashMap();
            this.fieldFilters = newHashMap();
        }

        /**
         * Performs the operation.
         *
         * <p>Multiple calls to this method may cause {@link DuplicateHandlerMethodError}s.
         *
         * @return a map of {@link HandlerTypeInfo}s to the method handlers
         */
        private ImmutableMultimap<HandlerTypeInfo, H> perform() {
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
                                                   .getRawMethod();
                String methodName = alreadyPresent.getName();
                String duplicateMethodName = handler.getRawMethod().getName();
                throw new DuplicateHandlerMethodError(declaringClass, id,
                                                      methodName, duplicateMethodName);
            } else {
                seenMethods.put(id, handler);
            }
        }

        private void checkNotClashes(H handler) {
            MessageClass handledClass = handler.getMessageClass();
            FieldPath field = handler.filter().getField();
            if (isNotDefault(field)) {
                FilteredHandler<H> previousValue = fieldFilters.put(
                        handledClass,
                        new FilteredHandler<>(handler, field)
                );
                if (previousValue != null && previousValue.fieldDiffersFrom(field)) {
                    throw new HandlerFieldFilterClashError(declaringClass,
                                                           handler.getRawMethod(),
                                                           previousValue.handler.getRawMethod());
                }
            }
        }
    }

    /**
     * A pair of a {@link HandlerMethod} and the field to filter its events by.
     *
     * @param <H>
     *         the type of handler method
     */
    private static final class FilteredHandler<H extends HandlerMethod<?, ?, ?, ?, ?>> {

        private final H handler;
        private final FieldPath filteredField;

        private FilteredHandler(H handler, FieldPath field) {
            this.handler = handler;
            this.filteredField = field;
        }

        private boolean fieldDiffersFrom(FieldPath path) {
            return !filteredField.equals(path);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FilteredHandler<?> handler1 = (FilteredHandler<?>) o;
            return Objects.equal(handler, handler1.handler) &&
                    Objects.equal(filteredField, handler1.filteredField);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(handler, filteredField);
        }
    }
}
