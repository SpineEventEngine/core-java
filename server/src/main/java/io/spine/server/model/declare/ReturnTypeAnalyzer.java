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

package io.spine.server.model.declare;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.server.model.Nothing;
import io.spine.server.tuple.Either;
import io.spine.server.tuple.Tuple;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;

abstract class ReturnTypeAnalyzer {

    private static final Map<Class<?>, AnalyzerSupplier> suppliers = analyzerSuppliers();

    private final Type type;

    ReturnTypeAnalyzer(Type type) {
        this.type = type;
    }

    @SuppressWarnings("ComparatorMethodParameterNotUsed") // See doc.
    private static Optional<AnalyzerSupplier> supplierFor(Class<?> rawType) {
        Optional<AnalyzerSupplier> result = suppliers
                .keySet()
                .stream()
                .filter(clazz -> clazz.isAssignableFrom(rawType))
                .sorted((o1, o2) -> o1.isAssignableFrom(o2) ? 1 : -1)
                .map(suppliers::get)
                .findFirst();
        return result;
    }

    static ReturnTypeAnalyzer forMethod(Method method) {
        Class<?> rawReturnType = method.getReturnType();
        Optional<AnalyzerSupplier> supplierForClass = supplierFor(rawReturnType);

        checkArgument(supplierForClass.isPresent(),
                      "There is no known return type analyzer for return type %s of method %s",
                      rawReturnType.getSimpleName(), method.getName());

        AnalyzerSupplier supplier = supplierForClass.get();
        Type genericReturnType = method.getGenericReturnType();
        ReturnTypeAnalyzer analyzer = supplier.get(genericReturnType);
        return analyzer;
    }

    Type type() {
        return type;
    }

    protected abstract ImmutableSet<Class<? extends Message>> extractEmittedTypes();

    private static Map<Class<?>, AnalyzerSupplier> analyzerSuppliers() {
        Map<Class<?>, AnalyzerSupplier> result = newHashMap();

        result.put(void.class, NoopAnalyzer::new);
        result.put(Nothing.class, NoopAnalyzer::new);

        result.put(CommandMessage.class, type ->
                new SimpleMessage(type, ImmutableSet.of(CommandMessage.class)));

        result.put(EventMessage.class, type ->
                new SimpleMessage(type, ImmutableSet.of(EventMessage.class,
                                                        RejectionMessage.class)));

        result.put(Optional.class, ParameterizedType::new);
        result.put(Tuple.class, ParameterizedType::new);
        result.put(Either.class, ParameterizedType::new);

        result.put(Iterable.class, type -> new ParameterizedType<>(type, Iterable.class));

        return result;
    }

    private static class NoopAnalyzer extends ReturnTypeAnalyzer {

        private NoopAnalyzer(Type genericType) {
            super(genericType);
        }

        @Override
        protected ImmutableSet<Class<? extends Message>> extractEmittedTypes() {
            return ImmutableSet.of();
        }
    }

    private static class SimpleMessage extends ReturnTypeAnalyzer {

        private final ImmutableSet<Class<? extends Message>> ignoredTypes;

        private SimpleMessage(Type genericType,
                              ImmutableSet<Class<? extends Message>> ignoredTypes) {
            super(genericType);
            this.ignoredTypes = ignoredTypes;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ImmutableSet<Class<? extends Message>> extractEmittedTypes() {
            TypeToken<?> token = TypeToken.of(type());
            Class<? extends Message> returnType = (Class<? extends Message>) token.getRawType();
            if (ignoredTypes.contains(returnType)) {
                return ImmutableSet.of();
            }
            return ImmutableSet.of(returnType);
        }
    }

    private static class ParameterizedType<T> extends ReturnTypeAnalyzer {

        @Nullable
        private final Class<T> targetSupertype;

        private ParameterizedType(Type type, @Nullable Class<T> targetSupertype) {
            super(type);
            this.targetSupertype = targetSupertype;
        }

        private ParameterizedType(Type type) {
            this(type, null);
        }

        @Override
        protected ImmutableSet<Class<? extends Message>> extractEmittedTypes() {
            TypeToken<?> token;

            if (targetSupertype != null) {
                TypeToken<? extends T> current = (TypeToken<? extends T>) TypeToken.of(type());
                token = current.getSupertype(targetSupertype);
            } else {
                token = TypeToken.of(type());
            }
            Class<?> rawType = token.getRawType();
            ImmutableSet.Builder<Class<? extends Message>> emitted = ImmutableSet.builder();
            for (TypeVariable<?> typeParam : rawType.getTypeParameters()) {
                TypeToken<?> resolved = token.resolveType(typeParam);
                Class<?> resolvedRawType = resolved.getRawType();
                Optional<AnalyzerSupplier> supplierForType = supplierFor(resolvedRawType);
                if (supplierForType.isPresent()) {
                    Type resolvedGenericType = resolved.getType();
                    AnalyzerSupplier supplier = supplierForType.get();
                    ReturnTypeAnalyzer analyzer = supplier.get(resolvedGenericType);
                    emitted.addAll(analyzer.extractEmittedTypes());
                }
            }
            return emitted.build();
        }
    }

    @FunctionalInterface
    private interface AnalyzerSupplier {

        ReturnTypeAnalyzer get(Type returnType);
    }
}
