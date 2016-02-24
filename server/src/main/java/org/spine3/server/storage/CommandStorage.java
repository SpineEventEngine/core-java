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
import com.google.protobuf.Timestamp;
import org.spine3.SPI;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Error;
import org.spine3.base.Failure;
import org.spine3.server.command.CommandStore;
import org.spine3.server.entity.EntityId;
import org.spine3.type.TypeName;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.validate.Validate.*;

/**
 * A storage used by {@link CommandStore} for keeping command data.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class CommandStorage extends AbstractStorage<CommandId, CommandStorageRecord> {

    //TODO:2016-02-18:alexander.yevsyukov: Define constraints the command declaration and use our validation to check the passed parameter.
    /**
     * Stores a command by a command ID from a command context.
     *
     * <p>Rewrites it if a command with such command ID already exists in the storage.
     *
     * @param command a command to store
     * @param aggregateId an aggregate ID to store
     */
    public void store(Command command, EntityId aggregateId) {
        checkNotNull(aggregateId);
        checkNotNull(command);
        checkNotClosed();

        checkArgument(command.hasMessage(), "Command message must be set.");
        final Any wrappedMessage = command.getMessage();

        checkArgument(command.hasContext(), "Command context must be set.");
        final CommandContext context = command.getContext();

        final CommandId commandId = checkValid(context.getCommandId());
        final String commandIdString = commandId.getUuid();

        final String commandType = TypeName.ofEnclosed(wrappedMessage).nameOnly();
        checkNotEmptyOrBlank(commandType, "command type");

        final String aggregateIdString = aggregateId.toString();
        checkNotEmptyOrBlank(aggregateIdString, "aggregate ID");

        final String aggregateIdType = checkNotEmptyOrBlank(aggregateId.getShortTypeName(), "aggregate ID type");
        final Timestamp timestamp = checkTimestamp(context.getTimestamp(), "Command time");

        final CommandStorageRecord.Builder builder = CommandStorageRecord.newBuilder()
                .setMessage(wrappedMessage)
                .setTimestamp(timestamp)
                .setCommandType(commandType)
                .setCommandId(commandIdString)
                .setStatus(CommandStatus.RECEIVED)
                .setAggregateIdType(aggregateIdType)
                .setAggregateId(aggregateIdString)
                .setContext(context);
        write(commandId, builder.build());
    }

    /**
     * Updates the status of the command to {@link org.spine3.base.CommandStatus#OK}
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
