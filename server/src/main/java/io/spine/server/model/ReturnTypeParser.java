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
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.server.tuple.Either;
import io.spine.server.tuple.Tuple;
import io.spine.type.MessageClass;

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

@SuppressWarnings({"UnstableApiUsage" /* Guava's Reflection API will most probably be OK. */,
        "unchecked" /* A lot of unchecked operations due to `TypeToken` nature. */})
abstract class ReturnTypeParser {

    private static final Map<Class<?>, Provider> parserProviders = parserProviders();

    private final Type type;

    /** Forbids inheriting except for inner classes. */
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

    abstract ImmutableSet<MessageClass<?>> parseProducedMessages();

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
        Optional<Provider> result = parserProviders
                .keySet()
                .stream()
                .filter(cls -> cls.isAssignableFrom(rawType))
                .sorted((cls1, cls2) -> cls1.isAssignableFrom(cls2) ? 1 : -1)
                .map(parserProviders::get)
                .findFirst();
        return result;
    }

    private static Map<Class<?>, Provider> parserProviders() {
        Map<Class<?>, Provider> result = newHashMap();

        result.put(void.class, NoopParser::new);
        result.put(Nothing.class, NoopParser::new);
        result.put(Empty.class, NoopParser::new);

        result.put(CommandMessage.class, CommandMessageParser::new);
        result.put(EventMessage.class, EventMessageParser::new);

        result.put(Optional.class, ParameterizedTypeParser::new);
        result.put(Tuple.class, ParameterizedTypeParser::new);
        result.put(Either.class, ParameterizedTypeParser::new);

        result.put(Iterable.class, type -> new ParameterizedTypeParser<>(type, Iterable.class));
        return result;
    }

    private static class NoopParser extends ReturnTypeParser {

        private NoopParser(Type type) {
            super(type);
        }

        @Override
        ImmutableSet<MessageClass<?>> parseProducedMessages() {
            return ImmutableSet.of();
        }
    }

    private static class CommandMessageParser extends ReturnTypeParser {

        private CommandMessageParser(Type type) {
            super(type);
        }

        @Override
        ImmutableSet<MessageClass<?>> parseProducedMessages() {
            TypeToken<?> token = TypeToken.of(type());
            Class<? extends CommandMessage> returnType =
                    (Class<? extends CommandMessage>) token.getRawType();
            if (CommandMessage.class.equals(returnType)) {
                return ImmutableSet.of();
            }
            CommandClass commandClass = CommandClass.from(returnType);
            return ImmutableSet.of(commandClass);
        }
    }

    private static class EventMessageParser extends ReturnTypeParser {

        private EventMessageParser(Type type) {
            super(type);
        }

        @Override
        ImmutableSet<MessageClass<?>> parseProducedMessages() {
            TypeToken<?> token = TypeToken.of(type());
            Class<? extends EventMessage> returnType =
                    (Class<? extends EventMessage>) token.getRawType();
            if (EventMessage.class.equals(returnType)
                    || RejectionMessage.class.equals(returnType)) {
                return ImmutableSet.of();
            }
            EventClass eventClass = EventClass.from(returnType);
            return ImmutableSet.of(eventClass);
        }
    }

    private static class ParameterizedTypeParser<T> extends ReturnTypeParser {

        @Nullable
        private final Class<T> targetSupertype;

        private ParameterizedTypeParser(Type type) {
            this(type, null);
        }

        private ParameterizedTypeParser(Type type, @Nullable Class<T> targetSupertype) {
            super(type);
            this.targetSupertype = targetSupertype;
        }

        @Override
        public ImmutableSet<MessageClass<?>> parseProducedMessages() {
            TypeToken<?> token = tokenFor(type());
            ImmutableSet.Builder<MessageClass<?>> produced = ImmutableSet.builder();
            for (TypeVariable<?> typeParam : token.getRawType()
                                                  .getTypeParameters()) {
                TypeToken<?> resolved = token.resolveType(typeParam);
                Optional<ReturnTypeParser> parser = forType(resolved.getType());
                parser.ifPresent(p -> produced.addAll(p.parseProducedMessages()));
            }
            return produced.build();
        }

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
