/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.model.AllowedParams;
import io.spine.server.model.MethodParams;
import io.spine.server.model.ParameterSpec;
import io.spine.server.model.TypeMatcher;
import io.spine.server.type.EventEnvelope;

import static io.spine.server.model.TypeMatcher.classImplementing;
import static io.spine.server.model.TypeMatcher.exactly;

/**
 * Allowed combinations of parameters for the methods, that accept {@code Event}s.
 */
@Immutable
enum EventAcceptingMethodParams implements ParameterSpec<EventEnvelope> {

    MESSAGE(classImplementing(EventMessage.class)) {

        @Override
        public Object[] extractArguments(EventEnvelope event) {
            return new Object[]{event.message()};
        }
    },

    MESSAGE_EVENT_CTX(classImplementing(EventMessage.class), exactly(EventContext.class)) {

        @Override
        public Object[] extractArguments(EventEnvelope event) {
            return new Object[]{event.message(), event.context()};
        }
    },

    MESSAGE_COMMAND_CTX(classImplementing(RejectionMessage.class), exactly(CommandContext.class)) {

        @Override
        public Object[] extractArguments(EventEnvelope event) {
            Message message = event.message();
            RejectionEnvelope rejection = RejectionEnvelope.from(event);
            CommandContext context =
                    rejection.getOrigin()
                             .getContext();
            return new Object[]{message, context};
        }
    },

    MESSAGE_COMMAND_MSG(classImplementing(RejectionMessage.class),
                        classImplementing(CommandMessage.class)) {

        @Override
        public boolean acceptsCommand() {
            return true;
        }

        @Override
        public Object[] extractArguments(EventEnvelope event) {
            Message message = event.message();
            RejectionEnvelope rejection = RejectionEnvelope.from(event);
            Message commandMessage = rejection.getOriginMessage();
            return new Object[]{message, commandMessage};
        }
    },

    MESSAGE_COMMAND_MSG_COMMAND_CTX(classImplementing(RejectionMessage.class),
                                    classImplementing(CommandMessage.class),
                                    exactly(CommandContext.class)) {

        @Override
        public boolean acceptsCommand() {
            return true;
        }

        @Override
        public Object[] extractArguments(EventEnvelope event) {
            Message message = event.message();
            RejectionEnvelope rejection = RejectionEnvelope.from(event);
            Command origin = rejection.getOrigin();
            CommandMessage commandMessage = origin.enclosedMessage();
            CommandContext context = origin.context();
            return new Object[]{message, commandMessage, context};
        }
    };

    private static final AllowedParams<EventEnvelope> PARAMS = new AllowedParams<>(values());

    private final ImmutableList<TypeMatcher> criteria;

    /**
     * Obtains specification of parameters allowed for event-handling methods.
     */
    static AllowedParams<EventEnvelope> allowed() {
        return PARAMS;
    }

    EventAcceptingMethodParams(TypeMatcher... criteria) {
        this.criteria = ImmutableList.copyOf(criteria);
    }

    @Override
    public boolean matches(MethodParams params) {
        return params.match(criteria);
    }

    /**
     * Verifies if command message is one of the expected parameters.
     */
    public boolean acceptsCommand() {
        return false;
    }
}
