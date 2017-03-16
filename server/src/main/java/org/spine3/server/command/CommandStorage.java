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
import com.google.common.collect.Iterators;
import org.spine3.annotations.SPI;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Error;
import org.spine3.base.Failure;
import org.spine3.server.storage.AbstractStorage;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.Iterator;

import static org.spine3.base.CommandStatus.ERROR;
import static org.spine3.base.CommandStatus.RECEIVED;
import static org.spine3.base.Commands.generateId;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Identifiers.EMPTY_ID;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;
import static org.spine3.validate.Validate.checkNotDefault;

/**
 * A storage used by {@link CommandStore} for keeping command data.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class CommandStorage extends AbstractStorage<CommandId, CommandRecord> {

    protected CommandStorage(boolean multitenant) {
        super(multitenant);
    }

    /**
     * Stores a command with the {@link CommandStatus#RECEIVED} status by
     * a command ID from a command context.
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

        final CommandRecord record = newRecordBuilder(command, status, null).build();
        final CommandId commandId = getId(command);
        write(commandId, record);
    }

    /**
     * Stores a command with the {@link CommandStatus#ERROR} status by
     * a command ID from a command context.
     *
     * @param command a command to store
     * @param error   an error occurred
     * @throws IllegalStateException if the storage is closed
     */
    protected void store(Command command, Error error) {
        checkNotClosed();
        checkNotDefault(error);

        final CommandId id = getOrGenerateCommandId(command);

        final CommandRecord.Builder builder = newRecordBuilder(command, ERROR, id);
        builder.getStatusBuilder()
               .setError(error);
        final CommandRecord record = builder.build();

        write(id, record);
    }

    /**
     * Obtains or generates a {@code CommandId} from the passed command.
     *
     * <p>We don't have a command ID in the passed command.
     * But need an ID to store the error in the record associated
     * with this command. So, the ID will be generated.
     *
     * <p>We pass this ID to the record, so that it has an identity.
     * But this ID does not belong to the command.
     *
     * <p>Therefore, commands without ID can be found by records
     * where `command.context.command_id` field is empty.
     */
    private static CommandId getOrGenerateCommandId(Command command) {
        CommandId id = getId(command);
        if (idToString(id).equals(EMPTY_ID)) {
            id = generateId();
        }
        return id;
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
        final Iterator<CommandRecord> recordIterator = read(status);
        final Iterator<Command> commandIterator = toCommandIterator(recordIterator);
        return commandIterator;
    }

    /**
     * Reads all command records with the given status.
     *
     * @param status a command status to search by
     * @return records with the given status
     */
    protected abstract Iterator<CommandRecord> read(CommandStatus status);

    /** Updates the status of the command to {@link CommandStatus#OK}. */
    protected abstract void setOkStatus(CommandId commandId);

    /** Updates the status of the command with the error. */
    protected abstract void updateStatus(CommandId commandId, Error error);

    /** Updates the status of the command with the business failure. */
    protected abstract void updateStatus(CommandId commandId, Failure failure);

    /**
     * Creates a command storage record builder passed on the passed parameters.
     *
     * <p>{@code targetId} and {@code targetIdType} are set to empty strings if
     * the command is not for an entity.
     *
     * @param command
     *            a command to convert to a record. This includes instances of faulty commands.
     *            An example of such a fault is missing command ID.
     * @param status
     *            a command status to set in the record
     * @param generatedCommandId
     *            a command ID to used because the passed command does not have own ID.
     *            If the command has own ID this parameter is {@code null}.
     * @return a storage record
     */
    @VisibleForTesting
    static CommandRecord.Builder newRecordBuilder(Command command,
                                                  CommandStatus status,
                                                  @Nullable CommandId generatedCommandId) {
        final CommandContext context = command.getContext();

        final CommandId commandId = generatedCommandId != null
                                    ? generatedCommandId
                                    : context.getCommandId();

        final String commandType = TypeName.ofCommand(command)
                                           .getSimpleName();

        final CommandRecord.Builder builder =
                CommandRecord.newBuilder()
                             .setCommandId(commandId)
                             .setCommandType(commandType)
                             .setCommand(command)
                             .setTimestamp(getCurrentTime())
                             .setStatus(ProcessingStatus.newBuilder()
                                                        .setCode(status));
        return builder;
    }

    /** Converts {@code CommandStorageRecord}s to {@code Command}s. */
    @VisibleForTesting
    static Iterator<Command> toCommandIterator(Iterator<CommandRecord> records) {
        return Iterators.transform(records, TO_COMMAND);
    }

    private static final Function<CommandRecord, Command> TO_COMMAND = new Function<CommandRecord, Command>() {
        @Override
        public Command apply(@Nullable CommandRecord record) {
            if (record == null) {
                return Command.getDefaultInstance();
            }
            final Command cmd = record.getCommand();
            return cmd;
        }
    };
}
