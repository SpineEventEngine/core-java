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
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.DispatchedCommand;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.model.HandlerKey;
import io.spine.server.model.MessageAcceptor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.of;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The strategy of event accepting method invocation.
 *
 * <p>An event accepting method is an event {@linkplain io.spine.core.Subscribe subscriber} or
 * an event {@link io.spine.server.event.React reactor}.
 *
 * @author Dmytro Dashenkov
 */
@Immutable
enum EventAcceptor implements MessageAcceptor<EventEnvelope> {

    /**
     * Method accepting only the event message.
     */
    MESSAGE(of(Message.class), false) {
        @Override
        List<?> arguments(EventEnvelope envelope) {
            Message message = envelope.getMessage();
            return of(message);
        }
    },

    /**
     * Method accepting the event message and the {@link EventContext}.
     */
    MESSAGE_EVENT_CTX(of(Message.class, EventContext.class), false) {
        @Override
        List<?> arguments(EventEnvelope envelope) {
            Message message = envelope.getMessage();
            EventContext context = envelope.getEventContext();
            return of(message, context);
        }
    },

    /**
     * Method accepting the rejection event message and the {@link CommandContext}.
     */
    MESSAGE_COMMAND_CTX(of(Message.class, CommandContext.class), false) {
        @Override
        List<?> arguments(EventEnvelope envelope) {
            Message message = envelope.getMessage();
            RejectionEnvelope rejection = RejectionEnvelope.from(envelope);
            CommandContext context = rejection.getOrigin()
                                              .getContext();
            return of(message, context);
        }
    },

    /**
     * Method accepting the rejection event message and the rejected command message.
     */
    MESSAGE_COMMAND_MSG(of(Message.class, Message.class), true) {
        @Override
        List<?> arguments(EventEnvelope envelope) {
            Message message = envelope.getMessage();
            RejectionEnvelope rejection = RejectionEnvelope.from(envelope);
            Message commandMessage = rejection.getOriginMessage();
            return of(message, commandMessage);
        }
    },

    /**
     * Method accepting the rejection event message, the rejected command message, and
     * its {@link CommandContext}.
     */
    MESSAGE_COMMAND_MSG_COMMAND_CTX(of(Message.class, Message.class, CommandContext.class), true) {
        @Override
        List<?> arguments(EventEnvelope envelope) {
            Message message = envelope.getMessage();
            RejectionEnvelope rejection = RejectionEnvelope.from(envelope);
            DispatchedCommand origin = rejection.getOrigin();
            Message commandMessage = unpack(origin.getMessage());
            CommandContext context = origin.getContext();
            return of(message, commandMessage, context);
        }
    };

    private final ImmutableList<Class<?>> expectedParameters;
    private final boolean awareOfCommandType;

    EventAcceptor(ImmutableList<Class<?>> types, boolean awareOfCommandType) {
        this.expectedParameters = types;
        this.awareOfCommandType = awareOfCommandType;
    }

    /**
     * Obtains an instance of {@code EventAcceptor} for the given method.
     *
     * @param parameters the event accepting method parameter types
     * @return instance of {@code EventAcceptor} or {@link Optional#empty()} if the given method is
     * not an event accepting method.
     */
    static Optional<EventAcceptor> findFor(Class<?>[] parameters) {
        List<Class<?>> params = ImmutableList.copyOf(parameters);
        Optional<EventAcceptor> result = Stream.of(values())
                                               .filter(acceptor -> acceptor.matches(params))
                                               .findFirst();
        return result;
    }

    /**
     * Obtains an instance of {@code EventAcceptor} for the given method and throws an
     * {@link IllegalStateException} if the method is not an event accepting method.
     *
     * @param method the event accepting method
     * @return instance of {@code EventAcceptor}
     */
    static EventAcceptor from(Method method) {
        EventAcceptor acceptor = findFor(method.getParameterTypes())
                .orElseThrow(() -> newIllegalStateException(
                        "Method %s is not a valid event acceptor.", method.toString())
                );
        method.setAccessible(true);
        return acceptor;
    }

    /**
     * Constructs a list of arguments of the event accepting method according to the signature
     * represented by this instance.
     *
     * <p>Do not call this method directly. See {@link #invoke(Object, Method, EventEnvelope)}
     * instead.
     *
     * @param envelope the {@link EventEnvelope} to disassemble into method arguments
     * @return list of arguments for an event accepting method with the signature matching this
     *         instance
     */
    abstract List<?> arguments(EventEnvelope envelope);

    /**
     * Invokes the given event accepting method with the given event as an argument.
     *
     * @param receiver       the object which owns the method
     * @param acceptorMethod the event accepting method
     * @param event          the event argument
     * @return the method invocation result
     * @throws InvocationTargetException if the method throws
     */
    @Override
    public @Nullable Object invoke(Object receiver, Method acceptorMethod, EventEnvelope event)
            throws InvocationTargetException {
        Object[] arguments = arguments(event).toArray();
        try {
            return acceptorMethod.invoke(receiver, arguments);
        } catch (IllegalAccessException e) {
            throw newIllegalStateException(e, "Method %s is inaccessible.", acceptorMethod);
        }
    }

    /**
     * Creates the {@link HandlerKey} for the given method according to the signature represented by
     * this instance.
     *
     * @param method handler method
     * @return {@link HandlerKey} for the given method
     */
    HandlerKey createKey(Method method) {
        Class<?>[] types = method.getParameterTypes();
        @SuppressWarnings("unchecked")
        Class<? extends Message> eventMessageClass = (Class<? extends Message>) types[0];
        EventClass eventClass = EventClass.from(eventMessageClass);
        if (!awareOfCommandType) {
            return HandlerKey.of(eventClass);
        } else {
            @SuppressWarnings("unchecked")
            Class<? extends Message> commandMessageClass = (Class<? extends Message>) types[1];
            CommandClass commandClass = CommandClass.from(commandMessageClass);
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
