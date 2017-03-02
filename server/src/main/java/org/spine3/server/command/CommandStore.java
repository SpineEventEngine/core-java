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
import org.spine3.base.CommandStatus;
import org.spine3.base.Error;
import org.spine3.base.Errors;
import org.spine3.base.Failure;
import org.spine3.base.FailureThrowable;
import org.spine3.server.command.error.CommandException;
import org.spine3.server.storage.TenantDataOperation;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkState;

/**
 * Manages commands received by the system.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
public class CommandStore implements AutoCloseable {

    private final CommandStorage storage;

    /**
     * Creates a new instance.
     *
     * @param storage an underlying storage
     */
    public CommandStore(CommandStorage storage) {
        this.storage = storage;
    }

    /**
     * Stores the command.
     *
     * <p>The underlying storage must be opened.
     *
     * @param command the command to store
     * @throws IllegalStateException if the storage is closed
     */
    public void store(final Command command) {
        checkNotClosed();

        final TenantDataOperation op = new TenantDataOperation(command) {
            @Override
            public void run() {
                storage.store(command);
            }
        };

        op.execute();
    }

    /**
     * Stores a command with the error status.
     *
     * @param command a command to store
     * @param error an error occurred
     * @throws IllegalStateException if the storage is closed
     */
    public void store(final Command command, final Error error) {
        checkNotClosed();

        final TenantDataOperation op = new TenantDataOperation(command) {
            @Override
            public void run() {
                storage.store(command, error);
            }
        };

        op.execute();
    }

    /**
     * Stores the command with the error status.
     *
     * @param command the command to store
     * @param exception an exception occurred to convert to {@link org.spine3.base.Error Error}
     * @throws IllegalStateException if the storage is closed
     */
    public void store(Command command, Exception exception) {
        checkNotClosed();
        storage.store(command, Errors.fromException(exception));
    }

    /**
     * Stores the command with the error status.
     *
     * @param command the command to store
     * @param exception the exception occurred, which encloses {@link org.spine3.base.Error Error}
     *                  to store
     * @throws IllegalStateException if the storage is closed
     */
    public void storeWithError(Command command, CommandException exception) {
        store(command, exception.getError());
    }

    /**
     * Stores a command with the given status.
     *
     * @param command a command to store
     * @param status a command status
     * @throws IllegalStateException if the storage is closed
     */
    public void store(final Command command, final CommandStatus status) {
        checkNotClosed();

        final TenantDataOperation op = new TenantDataOperation(command) {
            @Override
            public void run() {
                storage.store(command, status);
            }
        };

        op.execute();
    }

    //TODO:2017-02-14:alexander.yevsyukov: Have reads as tenant-data ops.

    /**
     * Returns an iterator over all commands with the given status.
     *
     * @param status a command status to search by
     * @return commands with the given status
     * @throws IllegalStateException if the storage is closed
     */
    public Iterator<Command> iterator(CommandStatus status) {
        checkNotClosed();
        final Iterator<Command> commands = storage.iterator(status);
        return commands;
    }

    /**
     * Sets the status of the command to {@link CommandStatus#OK}
     */
    private void setCommandStatusOk(CommandId commandId) {
        checkNotClosed();
        storage.setOkStatus(commandId);
    }

    /**
     * Updates the status of the command processing with the exception.
     */
    private void updateStatus(CommandId commandId, Exception exception) {
        updateStatus(commandId, Errors.fromException(exception));
    }

    /**
     * Updates the status of the command with the passed error.
     *
     * @param commandId the ID of the command
     * @param error the error, which occurred during command processing
     * @throws IllegalStateException if the storage is closed
     */
    private void updateStatus(CommandId commandId, Error error) {
        checkNotClosed();
        storage.updateStatus(commandId, error);
    }

    /**
     * Updates the status of the command with the business failure.
     *
     * @param commandId the command to update
     * @param failure the business failure occurred during command processing
     * @throws IllegalStateException if the storage is closed
     */
    private void updateStatus(CommandId commandId, Failure failure) {
        checkNotClosed();
        storage.updateStatus(commandId, failure);
    }

    @Override
    public void close() throws Exception {
        storage.close();
    }

    /** Returns {@code true} if the store is open, {@code false} otherwise */
    boolean isOpen() {
        final boolean isOpened = storage.isOpen();
        return isOpened;
    }

    private void checkNotClosed() {
        checkState(isOpen(), "The CommandStore is closed.");
    }

    /**
     * The service for updating a status of a command.
     */
    static class StatusService {

        private final CommandStore commandStore;
        private final Log log;

        StatusService(CommandStore commandStore, Log log) {
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
                    commandStore.updateStatus(commandId(), error);
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
}
