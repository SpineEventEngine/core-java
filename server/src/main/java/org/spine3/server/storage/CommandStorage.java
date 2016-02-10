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
import org.spine3.server.aggregate.AggregateId;
import org.spine3.server.command.CommandStore;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Identifiers.idToString;

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
     * @param command a command to store
     * @param aggregateId an aggregate ID to store
     */
    public void store(Command command, AggregateId aggregateId) {
        checkNotNull(aggregateId);
        checkNotNull(command);
        checkNotClosed();

        final Any wrappedMessage = command.getMessage();
        final CommandContext context = command.getContext();
        final TypeName commandType = TypeName.ofEnclosed(wrappedMessage);
        final CommandId commandId = context.getCommandId();
        final String commandIdStr = commandId.getUuid();
        final String aggregateIdStr = idToString(aggregateId.value());
        final CommandStorageRecord.Builder builder = CommandStorageRecord.newBuilder()
                .setTimestamp(context.getTimestamp())
                .setCommandType(commandType.nameOnly())
                .setCommandId(commandIdStr)
                .setAggregateIdType(aggregateId.getShortTypeName())
                .setAggregateId(aggregateIdStr)
                .setMessage(wrappedMessage)
                .setContext(context);

        checkRecord(builder);

        write(commandId, builder.build());
    }

    private static void checkRecord(CommandStorageRecordOrBuilder record) {
        checkNotEmptyOrBlank(record.getCommandType(), "command type");
        checkNotEmptyOrBlank(record.getCommandId(), "command ID");
        checkNotEmptyOrBlank(record.getAggregateIdType(), "aggregate ID type");
        checkNotEmptyOrBlank(record.getAggregateId(), "aggregate ID");
        final Timestamp timestamp = record.getTimestamp();
        final long seconds = timestamp.getSeconds();
        checkArgument(seconds > 0, "Command time must be set.");
        checkArgument(record.hasMessage(), "Command message must be set.");
        checkArgument(record.hasContext(), "Command context must be set.");
    }

    private static String checkNotEmptyOrBlank(String stringToCheck, String fieldName) {
        checkArgument(!stringToCheck.isEmpty(), fieldName + " must not be an empty string.");
        checkArgument(stringToCheck.trim().length() > 0, fieldName + " must not be a blank string.");
        return stringToCheck;
    }

    @Nullable
    @Override
    public CommandStorageRecord read(CommandId id) {
        throw new UnsupportedOperationException("Reading is not implemented because there is no need yet.");
    }
}
