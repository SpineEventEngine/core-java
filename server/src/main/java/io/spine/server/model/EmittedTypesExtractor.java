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
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.server.tuple.Either;
import io.spine.server.tuple.Tuple;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;

@SuppressWarnings("UnstableApiUsage")
abstract class EmittedTypesExtractor {

    private static final Map<Class<?>, ExtractorProvider> extractorProviders =
            extractorProviders();

    private final Type type;

    /** Forbids inheriting the class except for inner classes. */
    private EmittedTypesExtractor(Type type) {
        this.type = type;
    }

    static EmittedTypesExtractor forMethod(Method method) {
        checkNotNull(method);

        Type type = method.getGenericReturnType();
        Optional<EmittedTypesExtractor> extractor = forType(type);
        checkArgument(extractor.isPresent(),
                      "There is no known return type parser for return type %s of method %s",
                      type.getTypeName(), method.getName());
        return extractor.get();
    }

    abstract ImmutableSet<Class<? extends Message>> extract();

    protected Type type() {
        return type;
    }

    private static Optional<EmittedTypesExtractor> forType(Type type) {
        Class<?> rawType = TypeToken.of(type)
                                    .getRawType();
        Optional<ExtractorProvider> provider = chooseProvider(rawType);
        Optional<EmittedTypesExtractor> result = provider.map(p -> p.apply(type));
        return result;
    }

    @SuppressWarnings("ComparatorMethodParameterNotUsed") // See doc.
    private static Optional<ExtractorProvider> chooseProvider(Class<?> rawType) {
        Optional<ExtractorProvider> result = extractorProviders
                .keySet()
                .stream()
                .filter(clazz -> clazz.isAssignableFrom(rawType))
                .sorted((o1, o2) -> o1.isAssignableFrom(o2) ? 1 : -1)
                .map(extractorProviders::get)
                .findFirst();
        return result;
    }

    private static Map<Class<?>, ExtractorProvider> extractorProviders() {
        Map<Class<?>, ExtractorProvider> result = newHashMap();

        result.put(void.class, Noop::new);
        result.put(Nothing.class, Noop::new);
        result.put(Empty.class, Noop::new);

        result.put(CommandMessage.class, type ->
                new MessageType(type, ImmutableSet.of(CommandMessage.class)));
        result.put(EventMessage.class, type ->
                new MessageType(type, ImmutableSet.of(EventMessage.class,
                                                      RejectionMessage.class)));
        result.put(Optional.class, ParameterizedType::new);
        result.put(Tuple.class, ParameterizedType::new);
        result.put(Either.class, ParameterizedType::new);

        result.put(Iterable.class, type -> new ParameterizedType<>(type, Iterable.class));
        return result;
    }

    private static class Noop extends EmittedTypesExtractor {

        private Noop(Type type) {
            super(type);
        }

        @Override
        ImmutableSet<Class<? extends Message>> extract() {
            return ImmutableSet.of();
        }
    }

    private static class MessageType extends EmittedTypesExtractor {

        private final ImmutableSet<Class<? extends Message>> ignoredTypes;

        private MessageType(Type type, ImmutableSet<Class<? extends Message>> ignoredTypes) {
            super(type);
            this.ignoredTypes = ignoredTypes;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ImmutableSet<Class<? extends Message>> extract() {
            TypeToken<?> token = TypeToken.of(type());
            Class<? extends Message> returnType = (Class<? extends Message>) token.getRawType();
            if (ignoredTypes.contains(returnType)) {
                return ImmutableSet.of();
            }
            return ImmutableSet.of(returnType);
        }
    }

    private static class ParameterizedType<T> extends EmittedTypesExtractor {

        @Nullable
        private final Class<T> targetSupertype;

        private ParameterizedType(Type type) {
            this(type, null);
        }

        private ParameterizedType(Type type, @Nullable Class<T> supertype) {
            super(type);
            targetSupertype = supertype;
        }

        @Override
        public ImmutableSet<Class<? extends Message>> extract() {
            TypeToken<?> token = tokenFor(type());
            ImmutableSet.Builder<Class<? extends Message>> emitted = ImmutableSet.builder();
            for (TypeVariable<?> typeParam : token.getRawType()
                                                  .getTypeParameters()) {
                TypeToken<?> resolved = token.resolveType(typeParam);
                Optional<EmittedTypesExtractor> provider = forType(resolved.getType());
                provider.ifPresent(extractor -> emitted.addAll(extractor.extract()));
            }
            return emitted.build();
        }

        @SuppressWarnings("unchecked")
        private TypeToken<?> tokenFor(Type type) {
            if (targetSupertype != null) {
                TypeToken<? extends T> current = (TypeToken<? extends T>) TypeToken.of(type);
                return current.getSupertype(targetSupertype);
            }
            return TypeToken.of(type);
        }
    }

    @FunctionalInterface
    private interface ExtractorProvider extends Function<Type, EmittedTypesExtractor> {
    }
}
