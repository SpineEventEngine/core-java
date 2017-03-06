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
import org.spine3.SPI;
import org.spine3.base.Command;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Error;
import org.spine3.base.Errors;
import org.spine3.base.Failure;
import org.spine3.base.Stringifiers;
import org.spine3.server.BoundedContext;
import org.spine3.server.entity.DefaultRecordBasedRepository;

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
public class NewCommandStorage
        extends DefaultRecordBasedRepository<CommandId, CommandEntity, CommandRecord> {

    /**
     * {@inheritDoc}
     */
    protected NewCommandStorage(BoundedContext boundedContext) {
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
    protected void store(Command command) {
        checkNotClosed();
        store(command, RECEIVED);
    }

     /**
     * Stores a command with the given status.
     *
     * @param command a command to store
     * @param status a command status
     */
    protected void store(Command command, CommandStatus status) {
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
    protected void store(Command command, Error error) {
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
    protected void store(Command command, Exception exception) {
        checkNotClosed();
        store(command, Errors.fromException(exception));
    }

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
}
