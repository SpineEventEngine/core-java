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
import org.spine3.server.command.GetTargetIdFromCommand;
import org.spine3.server.error.MissingEntityIdException;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.validate.Validate.*;

/**
 * A storage used by {@link CommandStore} for keeping command data.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class CommandStorage extends AbstractStorage<CommandId, CommandStorageRecord> {

    private static final GetTargetIdFromCommand<Object, Message> ID_FUNCTION = GetTargetIdFromCommand.newInstance();

    //TODO:2016-02-18:alexander.yevsyukov: Define constraints the command declaration and use our validation to check the passed parameter.
    /**
     * Stores a command by a command ID from a command context.
     *
     * <p>Rewrites it if a command with such command ID already exists in the storage.
     *
     * @param command a command to store
     */
    public void store(Command command) {
        checkNotClosed();
        checkCommand(command);

        final CommandStorageRecord record = toStorageRecord(command);
        final CommandId commandId = command.getContext().getCommandId();
        write(commandId, record);
    }

    private static void checkCommand(Command command) {
        checkArgument(command.hasMessage(), "Command message must be set.");

        checkArgument(command.hasContext(), "Command context must be set.");
        final CommandContext context = command.getContext();

        checkValid(context.getCommandId());

        checkTimestamp(context.getTimestamp(), "Command time");

        final Message commandMessage = Commands.getMessage(command);
        final String commandType = TypeName.of(commandMessage).nameOnly();
        checkNotEmptyOrBlank(commandType, "command type");

        final Object targetId = tryToGetTargetId(commandMessage);
        if (targetId != null) {
            final String targetIdString = idToString(targetId);
            checkNotEmptyOrBlank(targetIdString, "command target ID");
            final String targetIdType = targetId.getClass().getName();
            checkNotEmptyOrBlank(targetIdType, "command target ID type");
        }
    }

    private static CommandStorageRecord toStorageRecord(Command command) {
        final CommandContext context = command.getContext();
        final CommandId commandId = checkValid(context.getCommandId());
        final String commandIdString = commandId.getUuid();

        final Any messageAny = command.getMessage();

        final Message commandMessage = Commands.getMessage(command);
        final String commandType = TypeName.of(commandMessage).nameOnly();

        final Object targetId = tryToGetTargetId(commandMessage);
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
     * Tries to obtain a target ID.
     *
     * @return an ID or {@code null} if {@link GetTargetIdFromCommand#getId(Message, Message)}
     * throws an exception (in the case if the command is not for an entity)
     */
    @Nullable
    private static Object tryToGetTargetId(Message commandMessage) {
        try {
            final Object id = ID_FUNCTION.getId(commandMessage, CommandContext.getDefaultInstance());
            return id;
        } catch (MissingEntityIdException | ClassCastException ignored) {
            return null;
        }
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
