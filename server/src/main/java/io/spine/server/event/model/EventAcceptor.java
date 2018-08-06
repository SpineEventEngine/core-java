/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.event.model;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.DispatchedCommand;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.model.HandlerKey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.of;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * @author Dmytro Dashenkov
 */
enum EventAcceptor {

    MESSAGE(of(Message.class), false) {
        @Override
        List<?> arguments(EventEnvelope envelope) {
            Message message = envelope.getMessage();
            return of(message);
        }
    },
    MESSAGE_EVENT_CXT(of(Message.class, EventContext.class), false) {
        @Override
        List<?> arguments(EventEnvelope envelope) {
            Message message = envelope.getMessage();
            EventContext context = envelope.getEventContext();
            return of(message, context);
        }
    },
    MESSAGE_COMMAND_CXT(of(Message.class, CommandContext.class), false) {
        @Override
        List<?> arguments(EventEnvelope envelope) {
            Message message = envelope.getMessage();
            CommandContext context = envelope.getRejectionOrigin()
                                             .getContext();
            return of(message, context);
        }
    },
    MESSAGE_COMMAND_MSG(of(Message.class, Message.class), true) {
        @Override
        List<?> arguments(EventEnvelope envelope) {
            Message message = envelope.getMessage();
            Message commandMessage = unpack(envelope.getRejectionOrigin()
                                                    .getMessage());
            return of(message, commandMessage);
        }
    },
    MESSAGE_COMMAND_MSG_COMMAND_CXT(of(Message.class, Message.class, CommandContext.class), true) {
        @Override
        List<?> arguments(EventEnvelope envelope) {
            Message message = envelope.getMessage();
            DispatchedCommand origin = envelope.getRejectionOrigin();
            Message commandMessage = unpack(origin.getMessage());
            CommandContext context = origin.getContext();
            return of(message, commandMessage, context);
        }
    };

    private final ImmutableList<Class<?>> expectedParameters;
    private final boolean awareOfCommandType;

    EventAcceptor(ImmutableList<Class<?>> types, boolean type) {
        this.expectedParameters = types;
        awareOfCommandType = type;
    }

    static Optional<EventAcceptor> findFor(Method method) {
        List<Class<?>> parameters = copyOf(method.getParameterTypes());
        Optional<EventAcceptor> result = Stream.of(values())
                                               .filter(acceptor -> acceptor.matches(parameters))
                                               .findFirst();
        return result;
    }

    static EventAcceptor from(Method method) {
        EventAcceptor acceptor = findFor(method)
                .orElseThrow(() -> newIllegalStateException(
                        "Method %s is not a valid event acceptor.", method.toString())
                );
        method.setAccessible(true);
        return acceptor;
    }

    abstract List<?> arguments(EventEnvelope envelope);

    Object accept(Object receiver, Method acceptorMethod, EventEnvelope envelope)
            throws InvocationTargetException {
        Object[] arguments = arguments(envelope).toArray();
        try {
            return acceptorMethod.invoke(receiver, arguments);
        } catch (IllegalAccessException e) {
            throw newIllegalStateException(e, "Method %s is inaccessible.", acceptorMethod);
        }
    }

    HandlerKey createKey(Method method) {
        Class<?>[] types = method.getParameterTypes();
        @SuppressWarnings("unchecked")
        Class<? extends Message> eventMessageClass = (Class<? extends Message>) types[0];
        EventClass eventClass = EventClass.of(eventMessageClass);
        if (!awareOfCommandType) {
            return HandlerKey.of(eventClass);
        } else {
            @SuppressWarnings("unchecked")
            Class<? extends Message> commandMessageClass = (Class<? extends Message>) types[1];
            CommandClass commandClass = CommandClass.of(commandMessageClass);
            return HandlerKey.of(eventClass, commandClass);
        }
    }

    private boolean matches(List<Class<?>> methodParams) {
        if (methodParams.size() != expectedParameters.size()) {
            return false;
        }
        for (int i = 0; i < methodParams.size(); i++) {
            Class<?> actual = methodParams.get(i);
            Class<?> expected = expectedParameters.get(i);
            if (!expected.isAssignableFrom(actual)) {
                return false;
            }
        }
        return true;
    }
}
