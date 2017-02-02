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
import com.google.common.collect.Iterators;
import com.google.protobuf.Message;
import org.spine3.SPI;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Commands;
import org.spine3.base.Error;
import org.spine3.base.Failure;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.command.storage.CommandStorageRecord;
import org.spine3.server.entity.idfunc.GetTargetIdFromCommand;
import org.spine3.server.storage.AbstractStorage;

import javax.annotation.Nullable;
import java.util.Iterator;

import static org.spine3.base.CommandStatus.ERROR;
import static org.spine3.base.CommandStatus.RECEIVED;
import static org.spine3.base.Commands.generateId;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Stringifiers.EMPTY_ID;
import static org.spine3.base.Stringifiers.idToString;
import static org.spine3.protobuf.Timestamps.getCurrentTime;
import static org.spine3.validate.Validate.checkNotDefault;

/**
 * A storage used by {@link CommandStore} for keeping command data.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class CommandStorage extends AbstractStorage<CommandId, CommandStorageRecord> {

    protected CommandStorage(boolean multitenant) {
        super(multitenant);
    }

    /**
     * Stores a command with the {@link CommandStatus#RECEIVED} status by a command ID from a command context.
     *
     * <p>Rewrites it if a command with such command ID already exists in the storage.
     *
     * @param command a complete command to store
     * @throws IllegalStateException if the storage is closed
     */
    protected void store(Command command) {
        store(command, RECEIVED);
    }

    /**
     * Stores a command with the given status by a command ID from a command context.
     *
     * @param command a complete command to store
     * @param status  a command status
     * @throws IllegalStateException if the storage is closed
     */
    protected void store(Command command, CommandStatus status) {
        checkNotClosed();

        final CommandStorageRecord record = newRecordBuilder(command, status).build();
        final CommandId commandId = getId(command);
        write(commandId, record);
    }

    /**
     * Stores a command with the {@link CommandStatus#ERROR} status by a command ID from a command context.
     *
     * <p>If there is no ID, a new one is generated is used.
     *
     * @param command a command to store
     * @param error   an error occurred
     * @throws IllegalStateException if the storage is closed
     */
    protected void store(Command command, Error error) {
        checkNotClosed();
        checkNotDefault(error);

        CommandId id = getId(command);
        if (idToString(id).equals(EMPTY_ID)) {
            id = generateId();
        }
        final CommandStorageRecord record = newRecordBuilder(command, ERROR)
                .setError(error)
                .setCommandId(idToString(id))
                .build();
        write(id, record);
    }

    /**
     * Returns an iterator over all commands with the given status.
     *
     * @param status a command status to search by
     * @return commands with the given status
     * @throws IllegalStateException if the storage is closed
     */
    protected Iterator<Command> iterator(CommandStatus status) {
        checkNotClosed();
        final Iterator<CommandStorageRecord> recordIterator = read(status);
        final Iterator<Command> commandIterator = toCommandIterator(recordIterator);
        return commandIterator;
    }

    /**
     * Reads all command records with the given status.
     *
     * @param status a command status to search by
     * @return records with the given status
     */
    protected abstract Iterator<CommandStorageRecord> read(CommandStatus status);

    /** Updates the status of the command to {@link CommandStatus#OK}. */
    protected abstract void setOkStatus(CommandId commandId);

    /** Updates the status of the command with the error. */
    protected abstract void updateStatus(CommandId commandId, Error error);

    /** Updates the status of the command with the business failure. */
    protected abstract void updateStatus(CommandId commandId, Failure failure);

    /**
     * Creates a command storage record builder passed on the passed parameters.
     *
     * <p>{@code targetId} and {@code targetIdType} are set to empty strings if the command is not for an entity.
     *
     * @param command a command to convert to a record
     * @param status  a command status to set to a record
     * @return a storage record
     */
    @VisibleForTesting
    static CommandStorageRecord.Builder newRecordBuilder(Command command, CommandStatus status) {
        final CommandContext context = command.getContext();

        final CommandId commandId = context.getCommandId();
        final String commandIdString = idToString(commandId);

        final Message commandMessage = Commands.getMessage(command);
        final String commandType = TypeUrl.of(commandMessage).getSimpleName();

        final Optional targetIdOptional = GetTargetIdFromCommand.asOptional(commandMessage);
        final String targetIdString;
        final String targetIdType;
        if (targetIdOptional.isPresent()) {
            final Object targetId = targetIdOptional.get();
            targetIdString = idToString(targetId);
            targetIdType = targetId.getClass()
                                   .getName();
        } else { // the command is not for an entity
            targetIdString = "";
            targetIdType = "";
        }

        final CommandStorageRecord.Builder builder = CommandStorageRecord.newBuilder()
                                                                         .setMessage(command.getMessage())
                                                                         .setTimestamp(getCurrentTime())
                                                                         .setCommandType(commandType)
                                                                         .setCommandId(commandIdString)
                                                                         .setStatus(status)
                                                                         .setTargetIdType(targetIdType)
                                                                         .setTargetId(targetIdString)
                                                                         .setContext(context);
        return builder;
    }

    /** Converts {@code CommandStorageRecord}s to {@code Command}s. */
    @VisibleForTesting
    static Iterator<Command> toCommandIterator(Iterator<CommandStorageRecord> records) {
        return Iterators.transform(records, TO_COMMAND);
    }

    private static final Function<CommandStorageRecord, Command> TO_COMMAND = new Function<CommandStorageRecord, Command>() {
        @Override
        public Command apply(@Nullable CommandStorageRecord record) {
            if (record == null) {
                return Command.getDefaultInstance();
            }
            final Command cmd = Commands.create(record.getMessage(), record.getContext());
            return cmd;
        }
    };
}
