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

package io.spine.server.event.model;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.core.CommandContext;
import io.spine.core.DispatchedCommand;
import io.spine.core.EventContext;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.server.type.EventEnvelope;

import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.model.declare.MethodParams.consistsOfTypes;

/**
 * Allowed combinations of parameters for the methods, that accept {@code Event}s.
 */
@Immutable
enum EventAcceptingMethodParams implements ParameterSpec<EventEnvelope> {

    MESSAGE(ImmutableList.of(EventMessage.class), false) {
        @Override
        public Object[] extractArguments(EventEnvelope envelope) {
            return new Object[] {envelope.message()};
        }
    },

    MESSAGE_EVENT_CTX(ImmutableList.of(EventMessage.class, EventContext.class), false) {
        @Override
        public Object[] extractArguments(EventEnvelope envelope) {
            return new Object[] {envelope.message(), envelope.getEventContext()};
        }
    },

    MESSAGE_COMMAND_CTX(ImmutableList.of(RejectionMessage.class, CommandContext.class), false) {
        @Override
        public Object[] extractArguments(EventEnvelope envelope) {
            Message message = envelope.message();
            RejectionEnvelope rejection = RejectionEnvelope.from(envelope);
            CommandContext context = rejection.getOrigin()
                                              .getContext();
            return new Object[] {message, context};
        }
    },

    MESSAGE_COMMAND_MSG(ImmutableList.of(RejectionMessage.class, CommandMessage.class), true) {
        @Override
        public Object[] extractArguments(EventEnvelope envelope) {
            Message message = envelope.message();
            RejectionEnvelope rejection = RejectionEnvelope.from(envelope);
            Message commandMessage = rejection.getOriginMessage();
            return new Object[] {message, commandMessage};
        }
    },

    MESSAGE_COMMAND_MSG_COMMAND_CTX(ImmutableList.of(RejectionMessage.class,
                                                     CommandMessage.class,
                                                     CommandContext.class),
                                    true) {
        @Override
        public Object[] extractArguments(EventEnvelope envelope) {
            Message message = envelope.message();
            RejectionEnvelope rejection = RejectionEnvelope.from(envelope);
            DispatchedCommand origin = rejection.getOrigin();
            Message commandMessage = unpack(origin.getMessage());
            CommandContext context = origin.getContext();

            return new Object[] {message, commandMessage, context};
        }
    };

    private final ImmutableList<Class<?>> expectedParameters;
    private final boolean awareOfCommandType;

    EventAcceptingMethodParams(ImmutableList<Class<?>> parameters, boolean type) {
        expectedParameters = parameters;
        awareOfCommandType = type;
    }

    @Override
    public boolean matches(Class<?>[] methodParams) {
        return consistsOfTypes(methodParams, expectedParameters);
    }

    public boolean isAwareOfCommandType() {
        return awareOfCommandType;
    }
}
