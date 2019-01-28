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
import io.spine.server.model.Nothing;
import io.spine.server.tuple.EitherOf2;
import io.spine.server.tuple.Pair;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * A wrapper around method's return type which knows how to get emitted messages (events and
 * commands) of the method.
 *
 * @see io.spine.server.model.MethodResult
 */
@SuppressWarnings({"UnstableApiUsage" /* To avoid re-implementing same thing from scratch. */,
        "unchecked" /* A lot of logically checked return type casts. */})
public enum ReturnType {

    VOID(void.class) {
        @Override
        protected ImmutableSet<Class<? extends Message>> gatherEmittedMessages(Method method) {
            return ImmutableSet.of();
        }
    },

    NOTHING(Nothing.class) {
        @Override
        protected ImmutableSet<Class<? extends Message>> gatherEmittedMessages(Method method) {
            return ImmutableSet.of();
        }
    },

    COMMAND_MESSAGE(CommandMessage.class) {
        @SuppressWarnings("unchecked") // Checked when matching method's return type.
        @Override
        protected ImmutableSet<Class<? extends Message>> gatherEmittedMessages(Method method) {
            Class<? extends CommandMessage> returnType =
                    (Class<? extends CommandMessage>) method.getReturnType();
            if (!CommandMessage.class.equals(returnType)) {
                return ImmutableSet.of(returnType);
            }
            return ImmutableSet.of();
        }
    },

    EVENT_MESSAGE(EventMessage.class, NOTHING) {
        @SuppressWarnings("unchecked") // Checked when matching method's return type.
        @Override
        protected ImmutableSet<Class<? extends Message>> gatherEmittedMessages(Method method) {
            Class<? extends EventMessage> returnType =
                    (Class<? extends EventMessage>) method.getReturnType();
            if (!EventMessage.class.equals(returnType)) {
                return ImmutableSet.of(returnType);
            }
            return ImmutableSet.of();
        }
    },

    OPTIONAL(Optional.class) {
        @Override
        protected ImmutableSet<Class<? extends Message>> gatherEmittedMessages(Method method) {
            Type type = method.getGenericReturnType();
            TypeToken<Optional> token = (TypeToken<Optional>) TypeToken.of(type);
            TypeVariable<Class<Optional>> typeParam = Optional.class.getTypeParameters()[0];
            Class<?> paramValue = token.resolveType(typeParam)
                                       .getRawType();
            if (CommandMessage.class.isAssignableFrom(paramValue)) {
                // Call CommandMessage return type analyzer.
            } else if (EventMessage.class.isAssignableFrom(paramValue)) {
                // Call EventMessage return type analyzer.
            }
            // The method returns optional of some supertype, like 'Optional<Message>'.
            return ImmutableSet.of();
        }
    },

    PAIR(Pair.class) {
        @Override
        protected ImmutableSet<Class<? extends Message>> gatherEmittedMessages(Method method) {
            Type type = method.getGenericReturnType();
            TypeToken<Pair> token = (TypeToken<Pair>) TypeToken.of(type);
            TypeVariable<Class<Pair>>[] typeParams = Pair.class.getTypeParameters();
            Class<?> firstParamValue = token.resolveType(typeParams[0])
                                            .getRawType();

            Set<Class<? extends Message>> emittedTypes = newHashSet();
            // We know for sure the first param must be 'Message'.
            if (CommandMessage.class.isAssignableFrom(firstParamValue)) {
                // Call CommandMessage return type analyzer.
                // Add to emittedTypes;
            } else if (EventMessage.class.isAssignableFrom(firstParamValue)) {
                // Call EventMessage return type analyzer.
                // Add to emittedTypes;
            }
            Class<?> secondParamValue = token.resolveType(typeParams[1])
                                             .getRawType();
            // Second param can be 1. Message, 2. Optional, 3. Either
            if (CommandMessage.class.isAssignableFrom(secondParamValue)) {
                // Call CommandMessage return type analyzer.
                // Add to emittedTypes;
            } else if (EventMessage.class.isAssignableFrom(secondParamValue)) {
                // Call EventMessage return type analyzer.
                // Add to emittedTypes;
            } else if (Optional.class.isAssignableFrom(secondParamValue)) {
                // Call Optional return type analyzer.
                // Add to emittedTypes;
            } else if (EitherOf2.class.isAssignableFrom(secondParamValue)) {
                // Call Either_of_2 return type analyzer.
                // Add to emittedTypes;
            }
            return ImmutableSet.copyOf(emittedTypes);
        }
    },

    EITHER_OF_2(EitherOf2.class) {
        @Override
        protected ImmutableSet<Class<? extends Message>> gatherEmittedMessages(Method method) {
            return ImmutableSet.of();
        }
    },

    ITERABLE(Iterable.class, PAIR, EITHER_OF_2) {
        /**
         * {@inheritDoc}
         *
         * <p>We must take care that the return type can be some descendant of {@link Iterable}.
         */
        @SuppressWarnings("unchecked") // Checked when matching method's return type.
        @Override
        protected ImmutableSet<Class<? extends Message>> gatherEmittedMessages(Method method) {
            Type type = method.getGenericReturnType();
            TypeToken<? extends Iterable> token =
                    (TypeToken<? extends Iterable>) TypeToken.of(type);
            TypeToken<?> iterable = token.getSupertype(Iterable.class);
            TypeVariable<Class<Iterable>> typeParam = Iterable.class.getTypeParameters()[0];
            Class<?> paramValue = iterable.resolveType(typeParam)
                                          .getRawType();
            if (CommandMessage.class.isAssignableFrom(paramValue)) {
                // Call CommandMessage return type analyzer.
            } else if (EventMessage.class.isAssignableFrom(paramValue)) {
                // Call EventMessage return type analyzer.
            }
            // The method returns iterable of some supertype, like 'Iterable<Message>' or its
            // descendant (like 'Either').
            return ImmutableSet.of();
        }
    };

    private final Class<?> returnType;
    private final ImmutableSet<ReturnType> specialCases;

    ReturnType(Class<?> type, ReturnType... specialCases) {
        returnType = type;
        this.specialCases = ImmutableSet.copyOf(specialCases);
    }

    Optional<ReturnType> getMatching(Class<?> methodReturnType) {
        Optional<ReturnType> specialCase = specialCases
                .stream()
                .filter(type -> type.matches(methodReturnType))
                .findFirst();
        if (specialCase.isPresent()) {
            return specialCase;
        }
        Optional<ReturnType> result = matches(methodReturnType)
                                      ? Optional.of(this)
                                      : Optional.empty();
        return result;
    }

    private boolean matches(Class<?> methodReturnType) {
        return returnType.isAssignableFrom(methodReturnType);
    }

    ImmutableSet<Class<? extends Message>> emittedMessages(Method method) {
        Class<?> returnType = method.getReturnType();
        ImmutableSet<Class<? extends Message>> result = getMatching(returnType)
                .orElseThrow(() -> signatureMismatchError(method))
                .gatherEmittedMessages(method);
        return result;
    }

    private IllegalArgumentException signatureMismatchError(Method method) {
        return newIllegalArgumentException(
                "Trying to gather emitted messages for method %s on mismatching return type %s",
                method.getName(),
                this.name()
        );
    }

    protected abstract ImmutableSet<Class<? extends Message>> gatherEmittedMessages(Method method);
}
