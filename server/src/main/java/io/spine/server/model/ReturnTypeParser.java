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

@SuppressWarnings("UnstableApiUsage") // Guava's `TypeToken` will most probably be OK.
abstract class ReturnTypeParser {

    private static final Map<Class<?>, Provider> providers = parserProviders();

    private final Type type;

    /** Forbids inheriting the class except for inner classes. */
    private ReturnTypeParser(Type type) {
        this.type = type;
    }

    static ReturnTypeParser forMethod(Method method) {
        checkNotNull(method);
        Type type = method.getGenericReturnType();
        Optional<ReturnTypeParser> parser = forType(type);
        checkArgument(parser.isPresent(),
                      "There is no known parser for the return type %s of the method %s",
                      type.getTypeName(), method.getName());
        return parser.get();
    }

    abstract ImmutableSet<Class<? extends Message>> parseProducedMessages();

    protected Type type() {
        return type;
    }

    private static Optional<ReturnTypeParser> forType(Type type) {
        Class<?> rawType = TypeToken.of(type)
                                    .getRawType();
        Optional<Provider> provider = chooseProvider(rawType);
        Optional<ReturnTypeParser> result = provider.map(p -> p.apply(type));
        return result;
    }

    @SuppressWarnings("ComparatorMethodParameterNotUsed") // See doc.
    private static Optional<Provider> chooseProvider(Class<?> rawType) {
        Optional<Provider> result = providers
                .keySet()
                .stream()
                .filter(clazz -> clazz.isAssignableFrom(rawType))
                .sorted((o1, o2) -> o1.isAssignableFrom(o2) ? 1 : -1)
                .map(providers::get)
                .findFirst();
        return result;
    }

    private static Map<Class<?>, Provider> parserProviders() {
        Map<Class<?>, Provider> result = newHashMap();

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

    private static class Noop extends ReturnTypeParser {

        private Noop(Type type) {
            super(type);
        }

        @Override
        ImmutableSet<Class<? extends Message>> parseProducedMessages() {
            return ImmutableSet.of();
        }
    }

    private static class MessageType extends ReturnTypeParser {

        private final ImmutableSet<Class<? extends Message>> tooBroadTypes;

        private MessageType(Type type, ImmutableSet<Class<? extends Message>> tooBroadTypes) {
            super(type);
            this.tooBroadTypes = tooBroadTypes;
        }

        @SuppressWarnings("unchecked") // Checked logically.
        @Override
        public ImmutableSet<Class<? extends Message>> parseProducedMessages() {
            TypeToken<?> token = TypeToken.of(type());
            Class<? extends Message> returnType = (Class<? extends Message>) token.getRawType();
            if (tooBroadTypes.contains(returnType)) {
                return ImmutableSet.of();
            }
            return ImmutableSet.of(returnType);
        }
    }

    private static class ParameterizedType<T> extends ReturnTypeParser {

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
        public ImmutableSet<Class<? extends Message>> parseProducedMessages() {
            TypeToken<?> token = tokenFor(type());
            ImmutableSet.Builder<Class<? extends Message>> produced = ImmutableSet.builder();
            for (TypeVariable<?> typeParam : token.getRawType()
                                                  .getTypeParameters()) {
                TypeToken<?> resolved = token.resolveType(typeParam);
                Optional<ReturnTypeParser> parser = forType(resolved.getType());
                parser.ifPresent(p -> produced.addAll(p.parseProducedMessages()));
            }
            return produced.build();
        }

        @SuppressWarnings("unchecked") // Checked logically.
        private TypeToken<?> tokenFor(Type type) {
            if (targetSupertype != null) {
                TypeToken<? extends T> current = (TypeToken<? extends T>) TypeToken.of(type);
                return current.getSupertype(targetSupertype);
            }
            return TypeToken.of(type);
        }
    }

    @FunctionalInterface
    private interface Provider extends Function<Type, ReturnTypeParser> {
    }
}
