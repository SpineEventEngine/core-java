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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Error;
import org.spine3.base.Errors;
import org.spine3.base.Failure;
import org.spine3.base.FailureThrowable;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.server.storage.CommandOperation;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.TenantAwareFunction;
import org.spine3.server.storage.TenantAwareOperation;

import javax.annotation.Nullable;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.spine3.server.command.CommandRecords.toCommandIterator;

/**
 * Manages storage of commands received by a Bounded Context.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
public class CommandStore implements AutoCloseable {

    private final CommandStorage storage;

    /**
     * Creates a new instance.
     */
    public CommandStore(StorageFactory storageFactory) {
        final CommandStorage storage = new CommandStorage();
        storage.initStorage(storageFactory);
        this.storage = storage;

    }

    StatusService createStatusService(Log log) {
        final StatusService result = new StatusService(this, log);
        return result;
    }

    private void checkNotClosed() {
        checkState(isOpen(), "The CommandStore is closed.");
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

    /**
     * Stores the command.
     *
     * <p>The underlying storage must be opened.
     *
     * @param command the command to store
     * @throws IllegalStateException if the storage is closed
     */
    public void store(final Command command) {
        final TenantAwareOperation op = new Operation(this, command) {
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
     */
    public void store(final Command command, final Error error) {
        final TenantAwareOperation op = new Operation(this, command) {
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
     */
    void storeWithError(Command command, CommandException exception) {
        store(command, exception.getError());
    }

    /**
     * Stores a command with the given status.
     *
     * @param command a command to store
     * @param status a command status
     */
    public void store(final Command command, final CommandStatus status) {
        final TenantAwareOperation op = new Operation(this, command) {
            @Override
            public void run() {
                storage.store(command, status);
            }
        };
        op.execute();
    }

    /**
     * Sets the status of the command to {@link CommandStatus#OK}
     */
    private void setCommandStatusOk(CommandEnvelope commandEnvelope) {
        final TenantAwareOperation op = new Operation(this, commandEnvelope) {
            @Override
            public void run() {
                storage.setOkStatus(commandId());
            }
        };
        op.execute();
    }

    /**
     * Updates the status of the command with the passed error.
     *  @param commandEnvelope the ID of the command
     * @param error the error, which occurred during command processing
     */
    private void updateStatus(CommandEnvelope commandEnvelope, final Error error) {
        final TenantAwareOperation op = new CommandOperation(commandEnvelope.getCommand()) {
            @Override
            public void run() {
                storage.updateStatus(commandId(), error);
            }
        };
        op.execute();
    }

    /**
     * Updates the status of the command processing with the exception.
     */
    private void updateStatus(CommandEnvelope commandEnvelope, Exception exception) {
        updateStatus(commandEnvelope, Errors.fromException(exception));
    }

    /**
     * Updates the status of the command with the business failure.
     *  @param commandEnvelope the command to update
     * @param failure the business failure occurred during command processing
     */
    private void updateStatus(CommandEnvelope commandEnvelope, final Failure failure) {
        final TenantAwareOperation op = new Operation(this, commandEnvelope) {
            @Override
            public void run() {
                storage.updateStatus(commandId(), failure);
            }
        };
        op.execute();
    }

    /**
     * Returns an iterator over all commands with the given status.
     *
     * @param status a command status to search by
     * @return commands with the given status
     */
    Iterator<Command> iterator(final CommandStatus status) {
        final Func<CommandStatus, Iterator<Command>> func =
                new Func<CommandStatus, Iterator<Command>>(this) {
            @Override
            public Iterator<Command> apply(@Nullable final CommandStatus input) {
                checkNotNull(input);
                final Iterator<Command> commands = toCommandIterator(storage.iterator(status));
                return commands;
            }
        };
        return func.execute(status);
    }

    @VisibleForTesting
    ProcessingStatus getStatus(CommandId commandId) {
        final Func<CommandId, ProcessingStatus> func = new Func<CommandId, ProcessingStatus>(this) {
            @Override
            public ProcessingStatus apply(@Nullable CommandId input) {
                checkNotNull(input);
                final ProcessingStatus status = storage.getStatus(input);
                return status;
            }
        };
        return func.execute(commandId);
    }

    /**
     * The service for updating a status of a command.
     */
    static class StatusService {

        private final CommandStore commandStore;
        private final Log log;

        private StatusService(CommandStore commandStore, Log log) {
            this.commandStore = commandStore;
            this.log = log;
        }

        void setOk(CommandEnvelope commandEnvelope) {
            commandStore.setCommandStatusOk(commandEnvelope);
        }

        void setToError(CommandEnvelope commandEnvelope, Error error) {
            commandStore.updateStatus(commandEnvelope, error);
        }

        @SuppressWarnings("ChainOfInstanceofChecks") // OK for this rare case
        void updateCommandStatus(CommandEnvelope commandEnvelope, Throwable cause) {
            final Message commandMessage = commandEnvelope.getMessage();
            final CommandId commandId = commandEnvelope.getCommandId();
            if (cause instanceof FailureThrowable) {
                final FailureThrowable failure = (FailureThrowable) cause;
                log.failureHandling(failure, commandMessage, commandId);
                commandStore.updateStatus(commandEnvelope, failure.toFailure());
            } else if (cause instanceof Exception) {
                final Exception exception = (Exception) cause;
                log.errorHandling(exception, commandMessage, commandId);
                commandStore.updateStatus(commandEnvelope, exception);
            } else {
                log.errorHandlingUnknown(cause, commandMessage, commandId);
                final Error error = Errors.fromThrowable(cause);
                commandStore.updateStatus(commandEnvelope, error);
            }
        }
    }

    /**
     * An abstract Command Store operation, which ensures that the store is open.
     */
    private abstract static class Operation extends CommandOperation {
        private final CommandStore store;

        private Operation(CommandStore store, Command command) {
            super(command);
            this.store = store;
        }

        private Operation(CommandStore store, CommandEnvelope commandEnvelope) {
            super(commandEnvelope.getCommand());
            this.store = store;
        }

        @Override
        public void execute() {
            store.checkNotClosed();
            super.execute();
        }
    }

    /**
     * An abstract Command Store function which ensures that the store is open.
     */
    private abstract static class Func<F, T> extends TenantAwareFunction<F, T> {
        private final CommandStore store;

        private Func(CommandStore store) {
            this.store = store;
        }

        @Override
        public T execute(F input) {
            store.checkNotClosed();
            return super.execute(input);
        }
    }
}
