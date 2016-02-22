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

package org.spine3.server.command;

import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.base.Errors;
import org.spine3.base.Failure;
import org.spine3.server.EntityId;
import org.spine3.server.aggregate.AggregateIdFunction;
import org.spine3.server.storage.CommandStorage;

import static com.google.common.base.Preconditions.checkState;

/**
 * Manages commands received by the system.
 *
 * @author Mikhail Mikhaylov
 */
public class CommandStore implements AutoCloseable {

    private final CommandStorage storage;

    public CommandStore(CommandStorage storage) {
        this.storage = storage;
    }

    /**
     * Stores the command.
     *
     * <p>The underlying storage must be opened.
     *
     * @param command the command to store
     */
    public void store(Command command) {
        checkState(storage.isOpen(), "Unable to store to closed storage.");

        final Message message = Commands.getMessage(command);
        final Object idValue = AggregateIdFunction.newInstance().getId(message, CommandContext.getDefaultInstance());
        final EntityId entityId = EntityId.of(idValue);
        storage.store(command, entityId);
    }

    @Override
    public void close() throws Exception {
        storage.close();
    }

    /**
     * @return true if the store is open, false otherwise
     */
    public boolean isOpen() {
        return storage.isOpen();
    }

    /**
     * @return true if the store is closed, false otherwise
     */
    public boolean isClosed() {
        return !isOpen();
    }

    /**
     * Sets the status of the command to {@link org.spine3.base.CommandStatus#OK}
     */
    public void setCommandStatusOk(CommandId commandId) {
        storage.setOkStatus(commandId);
    }

    /**
     * Updates the status of the command processing with the exception.
     *
     * @param commandId the ID of the command
     * @param exception the exception occurred during command processing
     */
    public void updateStatus(CommandId commandId, Exception exception) {
        storage.updateStatus(commandId, Errors.fromException(exception));
    }

    /**
     * Updates the status of the command with the passed error.
     *
     * @param commandId the ID of the command
     * @param error the error, which ocurred during command processing
     */
    public void updateStatus(CommandId commandId, org.spine3.base.Error error) {
        storage.updateStatus(commandId, error);
    }

    /**
     * Updates the status of the command with the business failure.
     *
     * @param commandId the ID of the command
     * @param failure the business failure occurred during command processing
     */
    public void updateStatus(CommandId commandId, Failure failure) {
        storage.updateStatus(commandId, failure);
    }
}
