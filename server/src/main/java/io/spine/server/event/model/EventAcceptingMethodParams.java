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
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.model.MethodParams;
import io.spine.server.model.ParameterSpec;
import io.spine.server.type.EventEnvelope;

/**
 * Allowed combinations of parameters for the methods, that accept {@code Event}s.
 */
@Immutable
enum EventAcceptingMethodParams implements ParameterSpec<EventEnvelope> {

    MESSAGE(EventMessage.class) {
        @Override
        public Object[] extractArguments(EventEnvelope event) {
            return new Object[]{event.message()};
        }

        @Override
        public boolean matches(MethodParams params) {
            return super.matches(params) && params.is(GeneratedMessageV3.class);
        }
    },

    MESSAGE_EVENT_CTX(EventMessage.class, EventContext.class) {
        @Override
        public Object[] extractArguments(EventEnvelope event) {
            return new Object[]{event.message(), event.context()};
        }

        @Override
        public boolean matches(MethodParams params) {
            return super.matches(params) && params.firstIs(GeneratedMessageV3.class);
        }
    },

    MESSAGE_COMMAND_CTX(RejectionMessage.class, CommandContext.class) {
        @Override
        public Object[] extractArguments(EventEnvelope event) {
            Message message = event.message();
            RejectionEnvelope rejection = RejectionEnvelope.from(event);
            CommandContext context =
                    rejection.getOrigin()
                             .getContext();
            return new Object[]{message, context};
        }

        @Override
        public boolean matches(MethodParams params) {
            return super.matches(params) && params.firstIs(GeneratedMessageV3.class);
        }
    },

    MESSAGE_COMMAND_MSG(RejectionMessage.class, CommandMessage.class) {
        @Override
        public Object[] extractArguments(EventEnvelope event) {
            Message message = event.message();
            RejectionEnvelope rejection = RejectionEnvelope.from(event);
            Message commandMessage = rejection.getOriginMessage();
            return new Object[]{message, commandMessage};
        }

        @Override
        public boolean matches(MethodParams params) {
            return super.matches(params)
                    && params.are(GeneratedMessageV3.class, GeneratedMessageV3.class);
        }
    },

    MESSAGE_COMMAND_MSG_COMMAND_CTX(
            RejectionMessage.class, CommandMessage.class, CommandContext.class) {
        @Override
        public Object[] extractArguments(EventEnvelope event) {
            Message message = event.message();
            RejectionEnvelope rejection = RejectionEnvelope.from(event);
            Command origin = rejection.getOrigin();
            CommandMessage commandMessage = origin.enclosedMessage();
            CommandContext context = origin.context();
            return new Object[]{message, commandMessage, context};
        }

        @Override
        public boolean matches(MethodParams params) {
            return super.matches(params) && params.are(GeneratedMessageV3.class,
                                                       GeneratedMessageV3.class,
                                                       CommandContext.class);
        }
    };

    private final ImmutableList<Class<?>> expectedParameters;

    EventAcceptingMethodParams(Class<?>... parameters) {
        this.expectedParameters = ImmutableList.copyOf(parameters);
    }

    @Override
    public boolean matches(MethodParams params) {
        return params.match(expectedParameters);
    }

    /**
     * Verifies if command message is one of the expected parameters.
     */
    public boolean acceptsCommand() {
        return expectedParameters.contains(CommandMessage.class);
    }
}