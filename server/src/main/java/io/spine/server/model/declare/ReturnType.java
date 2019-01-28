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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;

@SuppressWarnings("UnstableApiUsage")
@Internal
public final class ReturnType {

    private final Type type;
    private final ImmutableSet<Class<? extends Message>> emittedMessages;

    private ReturnType(Type type, ImmutableSet<Class<? extends Message>> emittedMessages) {
        this.type = type;
        this.emittedMessages = emittedMessages;
    }

    @VisibleForTesting
    public static ReturnType
    of(Type type, ImmutableSet<Class<? extends Message>> emittedMessages) {
        checkNotNull(type);
        checkNotNull(emittedMessages);
        return new ReturnType(type, emittedMessages);
    }

    static ReturnType of(Method method) {
        checkNotNull(method);

        Class<?> returnType = method.getReturnType();
        Optional<Parser> parser = Parser.forClass(returnType);

        checkArgument(parser.isPresent(),
                      "There is no known return type parser for return type %s of method %s",
                      returnType.getTypeName(), method.getName());
        Type type = method.getGenericReturnType();
        ImmutableSet<Class<? extends Message>> emitted = parser.get()
                                                               .parseEmittedMessages(type);
        return of(type, emitted);
    }

    public Type type() {
        return type;
    }

    public ImmutableSet<Class<? extends Message>> emittedMessages() {
        return emittedMessages;
    }

    private static final Map<Class<?>, Parser> parsers = parsers();

    private static Map<Class<?>, Parser> parsers() {
        Map<Class<?>, Parser> result = newHashMap();

        result.put(void.class, new NoopParser());
        result.put(Nothing.class, new NoopParser());
        result.put(Empty.class, new NoopParser());

        result.put(CommandMessage.class,
                   new MessageTypeParser(ImmutableSet.of(CommandMessage.class)));
        result.put(EventMessage.class,
                   new MessageTypeParser(ImmutableSet.of(EventMessage.class,
                                                         RejectionMessage.class)));
        result.put(Optional.class, new ParameterizedTypeParser<>());
        result.put(Tuple.class, new ParameterizedTypeParser<>());
        result.put(Either.class, new ParameterizedTypeParser<>());

        result.put(Iterable.class, new ParameterizedTypeParser<>(Iterable.class));
        return result;
    }

    private interface Parser {

        ImmutableSet<Class<? extends Message>> parseEmittedMessages(Type type);

        @SuppressWarnings("ComparatorMethodParameterNotUsed") // See doc.
        static Optional<Parser> forClass(Class<?> rawType) {
            Optional<Parser> result = parsers
                    .keySet()
                    .stream()
                    .filter(clazz -> clazz.isAssignableFrom(rawType))
                    .sorted((o1, o2) -> o1.isAssignableFrom(o2) ? 1 : -1)
                    .map(parsers::get)
                    .findFirst();
            return result;
        }
    }

    private static class NoopParser implements Parser {

        @Override
        public ImmutableSet<Class<? extends Message>> parseEmittedMessages(Type type) {
            return ImmutableSet.of();
        }
    }

    private static class MessageTypeParser implements Parser {

        private final ImmutableSet<Class<? extends Message>> ignoredTypes;

        private MessageTypeParser(ImmutableSet<Class<? extends Message>> ignoredTypes) {
            this.ignoredTypes = ignoredTypes;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ImmutableSet<Class<? extends Message>> parseEmittedMessages(Type type) {
            TypeToken<?> token = TypeToken.of(type);
            Class<? extends Message> returnType = (Class<? extends Message>) token.getRawType();
            if (ignoredTypes.contains(returnType)) {
                return ImmutableSet.of();
            }
            return ImmutableSet.of(returnType);
        }
    }

    private static class ParameterizedTypeParser<T> implements Parser {

        @Nullable
        private final Class<T> targetSupertype;

        private ParameterizedTypeParser() {
            this(null);
        }

        private ParameterizedTypeParser(@Nullable Class<T> targetSupertype) {
            this.targetSupertype = targetSupertype;
        }

        @Override
        public ImmutableSet<Class<? extends Message>> parseEmittedMessages(Type type) {
            TypeToken<?> token = tokenFor(type);
            ImmutableSet.Builder<Class<? extends Message>> emitted = ImmutableSet.builder();
            for (TypeVariable<?> typeParam : token.getRawType()
                                                  .getTypeParameters()) {
                TypeToken<?> resolved = token.resolveType(typeParam);
                Optional<Parser> parser = Parser.forClass(resolved.getRawType());
                Type resolvedType = resolved.getType();
                parser.ifPresent(a -> emitted.addAll(a.parseEmittedMessages(resolvedType)));
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
}
