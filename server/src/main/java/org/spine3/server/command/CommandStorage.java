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
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import org.spine3.base.Command;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Error;
import org.spine3.base.Failure;
import org.spine3.base.Identifiers;
import org.spine3.server.entity.DefaultRecordBasedRepository;

import javax.annotation.Nullable;
import java.util.Iterator;

import static com.google.common.collect.Iterators.transform;
import static org.spine3.base.CommandStatus.RECEIVED;

/**
 * The storage of commands and their processing status.
 *
 * <p>This class allows to hide implementation details of storing commands.
 * {@link CommandStore} serves as a facade, hiding the fact that the {@code CommandStorage}
 * is a {@code Repository}.
 *
 * @author Alexander Yevsyukov
 */
class CommandStorage extends DefaultRecordBasedRepository<CommandId, CommandEntity, CommandRecord> {

    /** The function to obtain a {@code CommandRecord} from {@code CommandEntity}. */
    private static final Function<CommandEntity, CommandRecord> GET_RECORD =
            new Function<CommandEntity, CommandRecord>() {
        @Nullable
        @Override
        public CommandRecord apply(@Nullable CommandEntity input) {
            if (input == null) {
                return null;
            }
            return input.getState();
        }
    };

    /**
     * Stores a command with the {@link CommandStatus#RECEIVED} status by
     * a command ID from a command context.
     *
     * <p>Rewrites it if a command with such command ID already exists in the storage.
     *
     * @param command a complete command to store
     */
    void store(Command command) {
        checkNotClosed();
        store(command, RECEIVED);
    }

    /**
     * Stores a command with the given status.
     *
     * @param command a command to store
     * @param status  a command status
     */
    void store(Command command, CommandStatus status) {
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
    void store(Command command, Error error) {
        checkNotClosed();
        final CommandEntity commandEntity = CommandEntity.createForError(command, error);
        store(commandEntity);
    }

    /**
     * Returns an iterator over all commands with the given status.
     *
     * @param status a command status to search by
     * @return commands with the given status
     * @throws IllegalStateException if the storage is closed
     */
    Iterator<CommandRecord> iterator(CommandStatus status) {
        checkNotClosed();
        final Iterator<CommandEntity> filteredEntities = iterator(new MatchesStatus(status));
        final Iterator<CommandRecord> transformed = transform(filteredEntities, getRecordFunc());
        return transformed;
    }

    ProcessingStatus getStatus(CommandId commandId) {
        checkNotClosed();
        final CommandEntity entity = loadEntity(commandId);
        return entity.getState()
                     .getStatus();
    }

    /**
     * Sets the status of the command to {@link CommandStatus#OK}
     */
    void setOkStatus(CommandId commandId) {
        checkNotClosed();
        final CommandEntity entity = loadEntity(commandId);
        entity.setOkStatus();
        store(entity);
    }

    /**
     * Updates the status of the command with the passed error.
     *
     * @param commandId the ID of the command
     * @param error     the error, which occurred during command processing
     */
    void updateStatus(CommandId commandId, Error error) {
        checkNotClosed();
        final CommandEntity entity = loadEntity(commandId);
        entity.setToError(error);
        store(entity);
    }

    /**
     * Updates the status of the command with the business failure.
     *
     * @param commandId the command to update
     * @param failure   the business failure occurred during command processing
     */
    void updateStatus(CommandId commandId, Failure failure) {
        checkNotClosed();
        final CommandEntity entity = loadEntity(commandId);
        entity.setToFailure(failure);
        store(entity);
    }

    private CommandEntity loadEntity(CommandId commandId) {
        final Optional<CommandEntity> loaded = find(commandId);
        if (!loaded.isPresent()) {
            final String idStr = Identifiers.idToString(commandId);
            throw new IllegalStateException("Unable to load entity for command ID: " + idStr);
        }

        return loaded.get();
    }

    /**
     * The predicate that filters entities by their processing status code.
     */
    private static class MatchesStatus implements Predicate<CommandEntity> {

        private final CommandStatus commandStatus;

        private MatchesStatus(CommandStatus commandStatus) {
            this.commandStatus = commandStatus;
        }

        @Override
        public boolean apply(@Nullable CommandEntity input) {
            if (input == null) {
                return false;
            }
            final boolean result = input.getState()
                                        .getStatus()
                                        .getCode() == commandStatus;
            return result;
        }
    }

    boolean isOpen() {
        return storageAssigned();
    }

    @VisibleForTesting
    static Function<CommandEntity, CommandRecord> getRecordFunc() {
        return GET_RECORD;
    }
}
