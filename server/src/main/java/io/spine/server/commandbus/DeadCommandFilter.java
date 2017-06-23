/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.commandbus;

import com.google.common.base.Optional;
import io.spine.base.Command;
import io.spine.base.Error;
import io.spine.base.IsSent;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.CommandValidationError.UNSUPPORTED_COMMAND;
import static io.spine.server.bus.Buses.reject;
import static io.spine.server.transport.Statuses.invalidArgumentWithCause;
import static io.spine.util.Exceptions.toError;

/**
 * Filters out commands that do not have registered dispatchers.
 *
 * @author Alexander Yevsyukov
 */
class DeadCommandFilter implements CommandBusFilter {

    private final CommandBus commandBus;

    DeadCommandFilter(CommandBus commandBus) {
        this.commandBus = checkNotNull(commandBus);
    }

    private boolean hasDispatcher(CommandClass commandClass) {
        final Optional<CommandDispatcher> dispatcher = commandBus.registry()
                                                                 .getDispatcher(commandClass);
        return dispatcher.isPresent();
    }

    @Override
    public Optional<IsSent> accept(CommandEnvelope envelope) {
        if (!hasDispatcher(envelope.getMessageClass())) {
            final Command command = envelope.getCommand();
            final CommandException unsupported = new UnsupportedCommandException(command);
            commandBus.commandStore().storeWithError(command, unsupported);
            final Throwable throwable = invalidArgumentWithCause(unsupported,
                                                                 unsupported.getError());
            final Error error = toError(throwable, UNSUPPORTED_COMMAND.getNumber());
            final IsSent result = reject(envelope.getId(), error);
            return Optional.of(result);
        }
        return Optional.absent();
    }

    @Override
    public void onClose(CommandBus commandBus) {
        // Do nothing.
    }
}
