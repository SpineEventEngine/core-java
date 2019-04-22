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

package io.spine.server.command.model;

import io.spine.base.EventMessage;
import io.spine.server.EventProducer;
import io.spine.server.command.CommandHandler;
import io.spine.server.model.EventsResult;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;

import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * The wrapper for a command handler method.
 */
public final class CommandHandlerMethod
        extends CommandAcceptingMethod<CommandHandler, EventClass, CommandHandlerMethod.Result> {

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method
     *         command handler method
     */
    CommandHandlerMethod(Method method, ParameterSpec<CommandEnvelope> params) {
        super(method, params);
    }

    /**
     * Transforms the passed raw method output into a list of event messages.
     */
    @Override
    protected Result toResult(CommandHandler target, Object rawMethodOutput) {
        return new Result(target, rawMethodOutput);
    }

    /**
     * The result of a command handler method execution.
     */
    public static final class Result extends EventsResult {

        private Result(EventProducer producer, Object rawMethodResult) {
            super(producer, rawMethodResult);
            List<EventMessage> eventMessages = toMessages(rawMethodResult);
            List<EventMessage> filtered = filterIgnored(eventMessages);
            
            //TODO:2018-07-25:dmytro.kuzmin: Prohibit returning empty events from `ProcessManager`
            // in favor of "Expect<...>" construction.
            // See https://github.com/SpineEventEngine/core-java/issues/790.
            checkState(!filtered.isEmpty(), "Command handling method did not produce events");
            setMessages(filtered);
        }
    }
}
