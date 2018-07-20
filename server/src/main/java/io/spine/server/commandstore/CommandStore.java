/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.commandstore;

import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.base.Errors;
import io.spine.base.ThrowableMessage;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.CommandStatus;
import io.spine.core.Commands;
import io.spine.core.Rejection;
import io.spine.core.TenantId;
import io.spine.server.commandbus.CommandException;
import io.spine.server.commandbus.CommandRecord;
import io.spine.server.commandbus.Log;
import io.spine.server.commandbus.ProcessingStatus;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.CommandOperation;
import io.spine.server.tenant.TenantAwareFunction;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.server.tenant.TenantIndex;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.core.Rejections.toRejection;

/**
 * Manages storage of commands received by a Bounded Context.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
public class CommandStore implements AutoCloseable {

    private final CRepository repository;
    private final TenantIndex tenantIndex;

    /**
     * Creates a new instance.
     */
    public CommandStore(StorageFactory storageFactory, TenantIndex tenantIndex) {
        checkNotNull(storageFactory);
        this.tenantIndex = checkNotNull(tenantIndex);
        CRepository repository = new CRepository();
        repository.initStorage(storageFactory);
        this.repository = repository;
    }

    private void checkNotClosed() {
        checkState(isOpen(), "Read/write operation on the closed CommandStore (%s)", this);
    }

    @Override
    public void close() throws Exception {
        repository.close();
    }

    /** Returns {@code true} if the store is open, {@code false} otherwise */
    public boolean isOpen() {
        boolean isOpened = repository.isOpen();
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
    public void store(Command command) {
        keepTenantId(command);
        TenantAwareOperation op = new Operation(this, command) {
            @Override
            public void run() {
                repository.store(command);
            }
        };
        op.execute();
    }

    public TenantIndex getTenantIndex() {
        return tenantIndex;
    }

    private void keepTenantId(Command command) {
        tenantIndex.keep(command.getContext()
                                .getActorContext()
                                .getTenantId());
    }

    /**
     * Stores a command with the error status.
     *
     * @param command a command to store
     * @param error an error occurred
     */
    public void store(Command command, Error error) {
        keepTenantId(command);
        TenantAwareOperation op = new Operation(this, command) {
            @Override
            public void run() {
                repository.store(command, error);
            }
        };
        op.execute();
    }

    /**
     * Stores the command with the error status.
     *
     * @param command the command to store
     * @param exception an exception occurred to convert to {@link io.spine.base.Error Error}
     */
    public void store(Command command, Exception exception) {
        checkNotClosed();
        keepTenantId(command);
        TenantAwareOperation op = new Operation(this, command) {
            @Override
            public void run() {
                repository.store(command, Errors.fromThrowable(exception));
            }
        };
        op.execute();
    }

    /**
     * Stores the command with the error status.
     *
     * @param command the command to store
     * @param exception the exception occurred, which encloses {@link io.spine.base.Error Error}
     *                  to store
     */
    public void storeWithError(Command command, CommandException exception) {
        store(command, exception.asError());
    }

    /**
     * Stores a command with the given status.
     *
     * @param command a command to store
     * @param status a command status
     */
    public void store(Command command, CommandStatus status) {
        keepTenantId(command);
        TenantAwareOperation op = new Operation(this, command) {
            @Override
            public void run() {
                repository.store(command, status);
            }
        };
        op.execute();
    }

    /**
     * Sets the status of the command to {@link CommandStatus#OK}
     */
    public void setCommandStatusOk(CommandEnvelope commandEnvelope) {
        keepTenantId(commandEnvelope.getCommand());
        TenantAwareOperation op = new Operation(this, commandEnvelope) {
            @Override
            public void run() {
                repository.setOkStatus(commandId());
            }
        };
        op.execute();
    }

    /**
     * Updates the status of the command with the passed error.
     *
     * @param commandEnvelope the ID of the command
     * @param error           the error, which occurred during command processing
     */
    private void updateStatus(CommandEnvelope commandEnvelope, Error error) {
        keepTenantId(commandEnvelope.getCommand());
        TenantAwareOperation op = new CommandOperation(commandEnvelope.getCommand()) {
            @Override
            public void run() {
                repository.updateStatus(commandId(), error);
            }
        };
        op.execute();
    }

    /**
     * Updates the status of the command processing with the exception.
     */
    private void updateStatus(CommandEnvelope commandEnvelope, Exception exception) {
        updateStatus(commandEnvelope, Errors.fromThrowable(exception));
    }

    /**
     * Updates the status of the command with the rejection.
     * @param commandEnvelope the command to update
     * @param rejection       why the command was rejected
     */
    private void updateStatus(CommandEnvelope commandEnvelope, Rejection rejection) {
        keepTenantId(commandEnvelope.getCommand());
        TenantAwareOperation op = new Operation(this, commandEnvelope) {
            @Override
            public void run() {
                repository.updateStatus(commandId(), rejection);
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
    public Iterator<Command> iterator(CommandStatus status) {
        Func<CommandStatus, Iterator<Command>> func =
                new Func<CommandStatus, Iterator<Command>>(this) {
            @Override
            public Iterator<Command> apply(@Nullable CommandStatus input) {
                checkNotNull(input);
                Iterator<CommandRecord> recordIterator = repository.iterator(status);
                Iterator<Command> commands = Records.toCommandIterator(recordIterator);
                return commands;
            }
        };
        return func.execute(status);
    }

    /**
     * Obtains the processing status for the command with the passed ID.
     *
     * <p>Invoking this method must be performed within a {@link TenantAwareFunction} or
     * {@link TenantAwareOperation}.
     */
    public ProcessingStatus getStatus(CommandId commandId) {
        Func<CommandId, ProcessingStatus> func = new Func<CommandId, ProcessingStatus>(this) {
            @Override
            public ProcessingStatus apply(@Nullable CommandId input) {
                checkNotNull(input);
                ProcessingStatus status = repository.getStatus(input);
                return status;
            }
        };
        return func.execute(commandId);
    }

    /**
     * Obtains the processing status for the passed command.
     */
    public ProcessingStatus getStatus(Command command) {
        TenantId tenantId = Commands.getTenantId(command);
        TenantAwareFunction<CommandId, ProcessingStatus> func =
                new TenantAwareFunction<CommandId, ProcessingStatus>(tenantId) {
                    @Override
                    public @Nullable ProcessingStatus apply(@Nullable CommandId commandId) {
                        checkNotNull(commandId);
                        return getStatus(commandId);
                    }
                };
        return func.execute(command.getId());
    }

    @SuppressWarnings("ChainOfInstanceofChecks") // OK for this consolidated error handling.
    public void updateCommandStatus(CommandEnvelope commandEnvelope, Throwable cause, Log log) {
        Message commandMessage = commandEnvelope.getMessage();
        CommandId commandId = commandEnvelope.getId();
        if (cause instanceof ThrowableMessage) {
            ThrowableMessage throwableMessage = (ThrowableMessage) cause;
            log.rejectedWith(throwableMessage, commandMessage, commandId);
            updateStatus(commandEnvelope,
                         toRejection(throwableMessage, commandEnvelope.getCommand()));
        } else if (cause instanceof Exception) {
            Exception exception = (Exception) cause;
            log.errorHandling(exception, commandMessage, commandId);
            updateStatus(commandEnvelope, exception);
        } else {
            log.errorHandlingUnknown(cause, commandMessage, commandId);
            Error error = Errors.fromThrowable(cause);
            updateStatus(commandEnvelope, error);
        }
    }

    public void setToError(CommandEnvelope commandEnvelope, Error error) {
        updateStatus(commandEnvelope, error);
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
            super();
            this.store = store;
        }

        @Override
        public T execute(F input) {
            store.checkNotClosed();
            return super.execute(input);
        }
    }
}
