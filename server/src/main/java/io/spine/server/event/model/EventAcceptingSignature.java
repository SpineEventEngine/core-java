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
import io.spine.server.model.MethodSignature;

import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.model.MethodSignatures.consistsOfTypes;

/**
 * @author Alex Tymchenko
 */
@Immutable
enum EventAcceptingSignature implements MethodSignature<EventEnvelope> {

    MESSAGE(of(Message.class), false) {
        @Override
        public Object[] extractArguments(EventEnvelope envelope) {
            return new Object[] {envelope.getMessage()};
        }
    },

    MESSAGE_EVENT_CTX(of(Message.class, EventContext.class), false) {
        @Override
        public Object[] extractArguments(EventEnvelope envelope) {
            return new Object[] {envelope.getMessage(), envelope.getEventContext()};
        }
    },

    MESSAGE_COMMAND_CTX(of(Message.class, CommandContext.class), false) {
        @Override
        public Object[] extractArguments(EventEnvelope envelope) {
            Message message = envelope.getMessage();
            RejectionEnvelope rejection = RejectionEnvelope.from(envelope);
            CommandContext context = rejection.getOrigin()
                                              .getContext();
            return new Object[] {message, context};
        }
    },

    MESSAGE_COMMAND_MSG(of(Message.class, Message.class), true) {
        @Override
        public Object[] extractArguments(EventEnvelope envelope) {
            Message message = envelope.getMessage();
            RejectionEnvelope rejection = RejectionEnvelope.from(envelope);
            Message commandMessage = rejection.getOriginMessage();
            return new Object[] {message, commandMessage};
        }
    },

    MESSAGE_COMMAND_MSG_COMMAND_CTX(of(Message.class, Message.class, CommandContext.class), true) {
        @Override
        public Object[] extractArguments(EventEnvelope envelope) {
            Message message = envelope.getMessage();
            RejectionEnvelope rejection = RejectionEnvelope.from(envelope);
            DispatchedCommand origin = rejection.getOrigin();
            Message commandMessage = unpack(origin.getMessage());
            CommandContext context = origin.getContext();

            return new Object[] {message, commandMessage, context};
        }
    };

    private final ImmutableList<Class<?>> expectedParameters;
    private final boolean awareOfCommandType;

    EventAcceptingSignature(ImmutableList<Class<?>> parameters, boolean type) {
        expectedParameters = parameters;
        awareOfCommandType = type;
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

    @Override
    public boolean matches(Class<?>[] methodParams) {
        return consistsOfTypes(methodParams, expectedParameters);
    }

    public boolean isAwareOfCommandType() {
        return awareOfCommandType;
    }
}
