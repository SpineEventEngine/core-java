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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import org.spine3.SPI;
import org.spine3.base.Command;
import org.spine3.base.CommandEnvelope;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Error;
import org.spine3.base.Errors;
import org.spine3.base.Failure;
import org.spine3.base.FailureThrowable;
import org.spine3.base.Stringifiers;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.error.CommandException;
import org.spine3.server.entity.DefaultRecordBasedRepository;
import org.spine3.server.storage.TenantDataOperation;

import java.util.Iterator;

import static org.spine3.base.CommandStatus.RECEIVED;

//TODO:2017-02-15:alexander.yevsyukov: Update Javadoc after migration to this class.
/**
 * This is Repository-based implementation of Command Store, which is going to
 * {@link CommandStore} .
 *
 * @author Alexander Yevsyukov
 */
@SPI
public class NewCommandStore
        extends DefaultRecordBasedRepository<CommandId, CommandEntity, CommandRecord> {

    /**
     * {@inheritDoc}
     */
    protected NewCommandStore(BoundedContext boundedContext) {
        super(boundedContext);
    }

    /**
     * Stores a command with the {@link CommandStatus#RECEIVED} status by
     * a command ID from a command context.
     *
     * <p>Rewrites it if a command with such command ID already exists in the storage.
     *
     * @param command a complete command to store
     */
    public void store(Command command) {
        checkNotClosed();
        store(command, RECEIVED);
    }

     /**
     * Stores a command with the given status.
     *
     * @param command a command to store
     * @param status a command status
     */
    public void store(Command command, CommandStatus status) {
        checkNotClosed();
        final CommandEntity commandEntity = CommandEntity.createForStatus(command, status);
        store(commandEntity);
    }

    /**
     * Stores a command with the {@link CommandStatus#ERROR} status by
     * a command ID from a command context.
     *
     * @param command a command to store
     * @param error   an error occurred
     */
    public void store(Command command, Error error) {
        checkNotClosed();
        final CommandEntity commandEntity = CommandEntity.createForError(command, error);
        store(commandEntity);
    }

    /**
     * Stores the command with the error status.
     *
     * @param command the command to store
     * @param exception an exception occurred to convert to {@link org.spine3.base.Error Error}
     */
    public void store(Command command, Exception exception) {
        checkNotClosed();
        store(command, Errors.fromException(exception));
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

        //TODO:2017-03-02:alexander.yevsyukov: Implement
        // final Iterator<Command> commands = storage.iterator(status);

        return ImmutableList.<Command>of().iterator();
    }

    /**
     * Sets the status of the command to {@link CommandStatus#OK}
     */
    private void setCommandStatusOk(CommandId commandId) {
        checkNotClosed();
        final CommandEntity entity = loadEntity(commandId);
        entity.setOkStatus();
        store(entity);
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
     */
    private void updateStatus(CommandId commandId, Error error) {
        checkNotClosed();
        final CommandEntity entity = loadEntity(commandId);
        entity.setToError(error);
        store(entity);
    }

    /**
     * Updates the status of the command with the business failure.
     *
     * @param commandId the command to update
     * @param failure the business failure occurred during command processing
     */
    private void updateStatus(CommandId commandId, Failure failure) {
        checkNotClosed();
        final CommandEntity entity = loadEntity(commandId);
        entity.setToFailure(failure);
        store(entity);
    }


    private CommandEntity loadEntity(CommandId commandId) {
        final Optional<CommandEntity> loaded = load(commandId);
        if (!loaded.isPresent()) {
            final String idStr = Stringifiers.idToString(commandId);
            throw new IllegalStateException("Unable to load entity for command ID: " + idStr);
        }

        return loaded.get();
    }

    /**
     * The service for updating a status of a command.
     */
    @SuppressWarnings("unused")
    static class StatusService {

        private final NewCommandStore commandStore;

        private final Log log;
        StatusService(NewCommandStore commandStore, Log log) {
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
                    final Message commandMessage = commandEnvelope.getMessage();
                    final CommandId commandId = commandEnvelope.getCommandId();

                    if (cause instanceof FailureThrowable) {
                        final FailureThrowable failure = (FailureThrowable) cause;
                        log.failureHandling(failure, commandMessage, commandId);
                        commandStore.updateStatus(commandId, failure.toFailure());
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
