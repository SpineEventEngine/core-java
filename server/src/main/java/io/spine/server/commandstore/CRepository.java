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

import com.google.common.annotations.VisibleForTesting;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.CommandStatus;
import io.spine.core.Rejection;
import io.spine.server.commandbus.CommandRecord;
import io.spine.server.commandbus.ProcessingStatus;
import io.spine.server.entity.DefaultRecordBasedRepository;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.spine.core.CommandStatus.RECEIVED;

/**
 * The storage of commands and their processing status.
 *
 * <p>This class allows to hide implementation details of storing commands.
 * {@link CommandStore} serves as a facade, hiding the fact that the {@code CommandStorage}
 * is a {@code Repository}.
 *
 * @author Alexander Yevsyukov
 */
class CRepository extends DefaultRecordBasedRepository<CommandId, CEntity, CommandRecord> {

    /** The function to obtain a {@code CommandRecord} from {@code CommandEntity}. */
    private static final Function<CEntity, CommandRecord> GET_RECORD =
            new Function<CEntity, CommandRecord>() {
        @Override
        public @Nullable CommandRecord apply(@Nullable CEntity input) {
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
        CEntity entity = CEntity.createForStatus(command, status);
        store(entity);
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
        CEntity entity = CEntity.createForError(command, error);
        store(entity);
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
        Iterator<CEntity> filteredEntities = iterator(new MatchesStatus(status));
        final Spliterator<CEntity> entitySpliterator = Spliterators.spliteratorUnknownSize(
                filteredEntities, Spliterator.ORDERED);
        Iterator<CommandRecord> transformed = StreamSupport.stream(entitySpliterator, false)
                                                           .map(getRecordFunc())
                                                           .collect(Collectors.toList())
                                                           .iterator();
        return transformed;
    }

    ProcessingStatus getStatus(CommandId commandId) {
        checkNotClosed();
        CEntity entity = loadEntity(commandId);
        return entity.getState()
                     .getStatus();
    }

    /**
     * Sets the status of the command to {@link CommandStatus#OK}
     */
    void setOkStatus(CommandId commandId) {
        checkNotClosed();
        CEntity entity = loadEntity(commandId);
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
        CEntity entity = loadEntity(commandId);
        entity.setToError(error);
        store(entity);
    }

    /**
     * Updates the status of the command with the rejection.
     *
     * @param commandId the command to update
     * @param rejection why the command was rejected
     */
    void updateStatus(CommandId commandId, Rejection rejection) {
        checkNotClosed();
        CEntity entity = loadEntity(commandId);
        entity.setToRejected(rejection);
        store(entity);
    }

    private CEntity loadEntity(CommandId commandId) {
        Optional<CEntity> loaded = find(commandId);
        if (!loaded.isPresent()) {
            String idStr = Identifier.toString(commandId);
            throw new IllegalStateException("Unable to load entity for command ID: " + idStr);
        }

        return loaded.get();
    }

    /**
     * The predicate that filters entities by their processing status code.
     */
    private static class MatchesStatus implements Predicate<CEntity> {

        private final CommandStatus commandStatus;

        private MatchesStatus(CommandStatus commandStatus) {
            this.commandStatus = commandStatus;
        }

        @Override
        public boolean test(@Nullable CEntity input) {
            if (input == null) {
                return false;
            }
            boolean result = input.getState()
                                  .getStatus()
                                  .getCode() == commandStatus;
            return result;
        }
    }

    @Override
    public boolean isOpen() {
        return isStorageAssigned();
    }

    @VisibleForTesting
    static Function<CEntity, CommandRecord> getRecordFunc() {
        return GET_RECORD;
    }
}
