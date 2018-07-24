/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.command.dispatch;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.ThrowableMessage;
import io.spine.core.CommandEnvelope;
import io.spine.core.RejectionEventContext;
import io.spine.server.command.model.CommandHandlerMethod;
import io.spine.server.model.HandlerMethodFailedException;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.base.Throwables.getStackTraceAsString;

/**
 * A {@link MessageDispatchFactory message dispatch factory} that deals
 * with {@link CommandEnvelope command envelopes}.
 *
 * @author Mykhailo Drachuk
 * @author Dmytro Dashenkov
 */
public final class CommandDispatchFactory extends MessageDispatchFactory<CommandEnvelope,
                                                                         CommandHandlerMethod> {
    CommandDispatchFactory(CommandEnvelope command) {
        super(command);
    }

    /** {@inheritDoc} */
    @Override
    public Dispatch<CommandEnvelope> to(Object context, CommandHandlerMethod method) {
        checkNotNull(context);
        checkNotNull(method);
        return new CommandMethodDispatch(envelope(), method, context);
    }

    /**
     * A dispatch of a {@link CommandEnvelope commands envelope}
     * to a {@link CommandHandlerMethod command handler method}.
     */
    private static class CommandMethodDispatch extends Dispatch<CommandEnvelope> {

        private final CommandHandlerMethod method;
        private final Object context;

        private CommandMethodDispatch(CommandEnvelope command,
                                      CommandHandlerMethod method,
                                      Object context) {
            super(command);
            this.method = method;
            this.context = context;
        }

        @Override
        protected DispatchResult dispatch() {
            try {
                return invoke();
            } catch (HandlerMethodFailedException exception) {
                return forRejection(exception);
            }
        }

        private DispatchResult invoke() {
            CommandEnvelope command = envelope();
            List<? extends Message> events = method.invoke(context, command.getMessage(),
                                                           command.getCommandContext());
            return DispatchResult.ofEvents(events, command);
        }

        private DispatchResult forRejection(HandlerMethodFailedException exception) {
            ThrowableMessage throwableMessage = rejectionFrom(exception);
            Message rejection = throwableMessage.getMessageThrown();
            RejectionEventContext context = rejectionContextFrom(throwableMessage);
            return DispatchResult.ofRejection(rejection, envelope(), context);
        }

        private static ThrowableMessage rejectionFrom(HandlerMethodFailedException exception)
                throws HandlerMethodFailedException {
            Throwable cause = getRootCause(exception);
            if (cause instanceof ThrowableMessage) {
                return (ThrowableMessage) cause;
            } else {
                throw exception;
            }
        }

        private RejectionEventContext rejectionContextFrom(Throwable throwable) {
            Any commandMessage = envelope().getCommand()
                                           .getMessage();
            String stacktrace = getStackTraceAsString(throwable);
            RejectionEventContext context = RejectionEventContext
                    .newBuilder()
                    .setCommandMessage(commandMessage)
                    .setStacktrace(stacktrace)
                    .build();
            return context;
        }
    }
}
