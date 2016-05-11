/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.SPI;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Commands;
import org.spine3.base.Error;
import org.spine3.base.Failure;
import org.spine3.server.command.CommandStore;
import org.spine3.server.command.CommandValidator;
import org.spine3.server.entity.GetTargetIdFromCommand;
import org.spine3.type.TypeName;

import static org.spine3.base.Commands.generateId;
import static org.spine3.base.Identifiers.EMPTY_ID;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.validate.Validate.checkNotDefault;

/**
 * A storage used by {@link CommandStore} for keeping command data.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class CommandStorage extends AbstractStorage<CommandId, CommandStorageRecord> {

    /**
     * Stores a command by a command ID from a command context.
     *
     * <p>Rewrites it if a command with such command ID already exists in the storage.
     *
     * @param command a command to store
     */
    public void store(Command command) {
        checkNotClosed();
        CommandValidator.checkCommand(command);

        final CommandStorageRecord record = toStorageRecord(command);
        final CommandId commandId = command.getContext().getCommandId();
        write(commandId, record);
    }

    /**
     * Stores a command with the {@link CommandStatus#ERROR} status by a command ID from a command context.
     *
     * <p>If there is no ID, a new one is generated is used.
     *
     * @param command a command to store
     * @param error an error occurred
     */
    public void store(Command command, Error error) {
        checkNotClosed();
        checkNotDefault(error);
        CommandId id = command.getContext().getCommandId();
        if (idToString(id).equals(EMPTY_ID)) {
            id = generateId();
        }
        final CommandStorageRecord record = toStorageRecord(command)
                .toBuilder()
                .setStatus(CommandStatus.ERROR)
                .setError(error)
                .setCommandId(idToString(id))
                .build();
        write(id, record);
    }

    @VisibleForTesting
    /* package */ static CommandStorageRecord toStorageRecord(Command command) {
        final CommandContext context = command.getContext();
        final CommandId commandId = context.getCommandId();
        final String commandIdString = idToString(commandId);

        final Any messageAny = command.getMessage();

        final Message commandMessage = Commands.getMessage(command);
        final String commandType = TypeName.of(commandMessage).nameOnly();

        final Object targetId = GetTargetIdFromCommand.asNullableObject(commandMessage);
        final String targetIdString;
        final String targetIdType;
        if (targetId != null) {
            targetIdString = idToString(targetId);
            targetIdType = targetId.getClass().getName();
        } else { // the command is not for an entity
            targetIdString = "";
            targetIdType = "";
        }

        final CommandStorageRecord.Builder builder = CommandStorageRecord.newBuilder()
                .setMessage(messageAny)
                .setTimestamp(context.getTimestamp())
                .setCommandType(commandType)
                .setCommandId(commandIdString)
                .setStatus(CommandStatus.RECEIVED)
                .setTargetIdType(targetIdType)
                .setTargetId(targetIdString)
                .setContext(context);
        return builder.build();
    }

    /**
     * Updates the status of the command to {@link CommandStatus#OK}
     */
    public abstract void setOkStatus(CommandId commandId);

    /**
     * Updates the status of the command with the error.
     */
    public abstract void updateStatus(CommandId commandId, Error error);

    /**
     * Updates the status of the command with the business failure.
     */
    public abstract void updateStatus(CommandId commandId, Failure failure);
}
