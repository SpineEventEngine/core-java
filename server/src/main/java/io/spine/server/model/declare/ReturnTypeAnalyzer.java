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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

@SuppressWarnings("Duplicates") // todo temporarily
abstract class ReturnTypeAnalyzer {

    private static final Map<Class<?>, AnalyzerSupplier> suppliers = analyzerSuppliers();

    // todo try merge these
    private final Class<?> rawType;
    private final Type genericType;

    ReturnTypeAnalyzer(Class<?> rawType, Type genericType) {
        this.rawType = rawType;
        this.genericType = genericType;
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
        Type genericReturnType = method.getGenericReturnType();
        Optional<AnalyzerSupplier> supplierForClass = supplierFor(rawReturnType);

        checkArgument(supplierForClass.isPresent(),
                      "There is no known return type analyzer for return type %s of method %s",
                      rawReturnType.getSimpleName(), method.getName());

        AnalyzerSupplier supplier = supplierForClass.get();
        ReturnTypeAnalyzer analyzer = supplier.get(rawReturnType, genericReturnType);
        return analyzer;
    }

    Class<?> rawType() {
        return rawType;
    }

    Type genericType() {
        return genericType;
    }

    protected abstract ImmutableSet<Class<? extends Message>> extractEmittedTypes();

    private static Map<Class<?>, AnalyzerSupplier> analyzerSuppliers() {
        Map<Class<?>, AnalyzerSupplier> result = newHashMap();

        result.put(void.class, NoopAnalyzer::new);
        result.put(Nothing.class, NoopAnalyzer::new);

        result.put(CommandMessage.class, (rawType, genericType) ->
                new SimpleMessage(rawType, genericType,
                                  ImmutableSet.of(CommandMessage.class)));

        result.put(EventMessage.class, (rawType, genericType) ->
                new SimpleMessage(rawType, genericType,
                                  ImmutableSet.of(EventMessage.class, RejectionMessage.class)));

        result.put(Optional.class, ParameterizedType::new);
        result.put(Tuple.class, ParameterizedType::new);
        result.put(Either.class, ParameterizedType::new);

        result.put(Iterable.class, (rawType, genericType) ->
                new ParameterizedSupertype<>(rawType, genericType, Iterable.class));

        return result;
    }

    private static class NoopAnalyzer extends ReturnTypeAnalyzer {

        private NoopAnalyzer(Class<?> rawType, Type genericType) {
            super(rawType, genericType);
        }

        @Override
        protected ImmutableSet<Class<? extends Message>> extractEmittedTypes() {
            return ImmutableSet.of();
        }
    }

    private static class SimpleMessage extends ReturnTypeAnalyzer {

        private final ImmutableSet<Class<? extends Message>> ignoredTypes;

        private SimpleMessage(Class<?> rawType,
                              Type genericType,
                              ImmutableSet<Class<? extends Message>> ignoredTypes) {
            super(rawType, genericType);
            this.ignoredTypes = ignoredTypes;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ImmutableSet<Class<? extends Message>> extractEmittedTypes() {
            Class<? extends Message> returnType = (Class<? extends Message>) rawType();
            if (ignoredTypes.contains(returnType)) {
                return ImmutableSet.of();
            }
            return ImmutableSet.of(returnType);
        }
    }

    private static class ParameterizedType extends ReturnTypeAnalyzer {

        private ParameterizedType(Class<?> rawType, Type genericType) {
            super(rawType, genericType);
        }

        @Override
        protected ImmutableSet<Class<? extends Message>> extractEmittedTypes() {
            TypeToken<?> token = TypeToken.of(genericType());
            Set<Class<? extends Message>> emitted = newHashSet();
            for (TypeVariable<?> typeParam : rawType().getTypeParameters()) {
                TypeToken<?> resolved = token.resolveType(typeParam);
                Class<?> resolvedRawType = resolved.getRawType();
                Optional<AnalyzerSupplier> supplierForType = supplierFor(resolvedRawType);
                if (supplierForType.isPresent()) {
                    Type resolvedGenericType = resolved.getType();
                    AnalyzerSupplier supplier = supplierForType.get();
                    ReturnTypeAnalyzer analyzer =
                            supplier.get(resolvedRawType, resolvedGenericType);
                    emitted.addAll(analyzer.extractEmittedTypes());
                }
            }
            return ImmutableSet.copyOf(emitted);
        }
    }

    private static class ParameterizedSupertype<T> extends ReturnTypeAnalyzer {

        private final Class<T> supertype;

        // todo try make first param Class<? extends T>
        private ParameterizedSupertype(Class<?> rawType, Type genericType, Class<T> supertype) {
            super(rawType, genericType);
            this.supertype = supertype;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected ImmutableSet<Class<? extends Message>> extractEmittedTypes() {
            TypeToken<? extends T> token = (TypeToken<? extends T>) TypeToken.of(genericType());
            TypeToken<?> supertype = token.getSupertype(this.supertype);
            Set<Class<? extends Message>> emitted = newHashSet();
            for (TypeVariable<?> typeParam : this.supertype.getTypeParameters()) {
                TypeToken<?> resolved = supertype.resolveType(typeParam);
                Class<?> resolvedRawType = resolved.getRawType();
                AnalyzerSupplier supplier = suppliers.get(resolvedRawType);
                if (supplier != null) {
                    Type resolvedGenericType = resolved.getType();
                    ReturnTypeAnalyzer analyzer =
                            supplier.get(resolvedRawType, resolvedGenericType);
                    emitted.addAll(analyzer.extractEmittedTypes());
                }
            }
            return ImmutableSet.copyOf(emitted);
        }
    }

    @FunctionalInterface
    private interface AnalyzerSupplier {
        ReturnTypeAnalyzer get(Class<?> rawType, Type genericType);
    }
}
