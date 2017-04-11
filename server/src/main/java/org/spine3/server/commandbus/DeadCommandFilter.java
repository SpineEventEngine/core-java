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

package org.spine3.server.commandbus;

import com.google.common.base.Optional;
import io.grpc.stub.StreamObserver;
import org.spine3.base.Command;
import org.spine3.base.Response;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.server.Statuses;
import org.spine3.type.CommandClass;

import static com.google.common.base.Preconditions.checkNotNull;

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
    public boolean accept(CommandEnvelope envelope, StreamObserver<Response> responseObserver) {
        if (!hasDispatcher(envelope.getMessageClass())) {
            final Command command = envelope.getCommand();
            final CommandException unsupported = new UnsupportedCommandException(command);
            commandBus.commandStore()
                      .storeWithError(command, unsupported);
            responseObserver.onError(Statuses.invalidArgumentWithCause(unsupported));
            return false;
        }
        return true;
    }

    @Override
    public void onClose(CommandBus commandBus) {
        // Do nothing.
    }
}
