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

package org.spine3.server.command;

import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandId;
import org.spine3.base.Error;
import org.spine3.base.Errors;
import org.spine3.base.FailureThrowable;
import org.spine3.server.storage.TenantDataOperation;

import static org.spine3.base.Commands.getId;

/**
 * The service for updating a status of a command.
 *
 * @author Alexander Yevsyukov
 */
class CommandStatusService {

    private final CommandStore commandStore;
    private final Log log;

    CommandStatusService(CommandStore commandStore, Log log) {
        this.commandStore = commandStore;
        this.log = log;
    }

    void setOk(final CommandEnvelope commandEnvelope) {
        final TenantDataOperation op = new TenantDataOperation(commandEnvelope.getCommand()) {
            @Override
            public void run() {
                commandStore.setCommandStatusOk(commandEnvelope.getCommandId());
            }
        };
        op.execute();
    }

    void setToError(CommandEnvelope commandEnvelope, final Error error) {
        final Command command = commandEnvelope.getCommand();
        final TenantDataOperation op = new TenantDataOperation(command) {
            @Override
            public void run() {
                commandStore.updateStatus(getId(command), error);
            }
        };
        op.execute();
    }

    void updateCommandStatus(final CommandEnvelope commandEnvelope, final Throwable cause) {
        final TenantDataOperation op = new TenantDataOperation(commandEnvelope.getCommand()) {
            @SuppressWarnings("ChainOfInstanceofChecks") // OK for this rare case
            @Override
            public void run() {
                final Message commandMessage = commandEnvelope.getCommandMessage();
                final CommandId commandId = commandEnvelope.getCommandId();

                if (cause instanceof FailureThrowable) {
                    final FailureThrowable failure = (FailureThrowable) cause;
                    log.failureHandling(failure, commandMessage, commandId);
                    commandStore.updateStatus(commandId, failure.toMessage());
                } else if (cause instanceof Exception) {
                    final Exception exception = (Exception) cause;
                    log.errorHandling(exception, commandMessage, commandId);
                    commandStore.updateStatus(commandId, exception);
                } else {
                    log.errorHandlingUnknown(cause, commandMessage, commandId);
                    final Error error = Errors.fromThrowable(cause);
                    commandStore.updateStatus(commandId, error);
                }
            }
        };
        op.execute();
    }
}
