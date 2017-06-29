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

package io.spine.server.commandstore;

import io.spine.base.Error;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.CommandStatus;
import io.spine.core.Failure;
import io.spine.server.commandbus.CommandRecord;
import io.spine.server.entity.AbstractEntity;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.CommandStatus.ERROR;
import static io.spine.core.CommandStatus.FAILURE;
import static io.spine.core.CommandStatus.OK;

/**
 * An entity for storing a command and its processing status.
 *
 * @author Alexander Yevyukov
 */
class CEntity extends AbstractEntity<CommandId, CommandRecord> {

    /**
     * {@inheritDoc}
     */
    private CEntity(CommandId id) {
        super(id);
    }

    private static CEntity create(CommandId commandId) {
        return new CEntity(commandId);
    }

    static CEntity createForStatus(Command command, CommandStatus status) {
        checkNotNull(command);
        checkNotNull(status);

        final CommandId commandId = command.getId();
        final CEntity entity = create(commandId);
        entity.setCommandAndStatus(command, status);
        return entity;
    }

    static CEntity createForError(Command command, Error error) {
        checkNotNull(command);
        checkNotNull(error);

        final CommandId id = Records.getOrGenerateCommandId(command);

        final CEntity result = create(id);
        result.setError(id, command, error);
        return result;
    }

    private void setCommandAndStatus(Command command, CommandStatus status) {
        final CommandRecord record = Records.newRecordBuilder(command,
                                                              status,
                                                              null).build();
        updateState(record);
    }

    private void setError(CommandId id, Command command, Error error) {
        final CommandRecord.Builder builder = Records.newRecordBuilder(command, ERROR, id);
        builder.getStatusBuilder()
               .setError(error);
        final CommandRecord record = builder.build();
        updateState(record);
    }

    void setOkStatus() {
        final CommandRecord.Builder builder = getState().toBuilder();
        builder.getStatusBuilder()
               .setCode(OK);
        final CommandRecord record = builder.build();
        updateState(record);
    }

    void setToError(Error error) {
        final CommandRecord.Builder builder = getState().toBuilder();
        builder.getStatusBuilder()
               .setCode(ERROR)
               .setError(error);
        final CommandRecord record = builder.build();
        updateState(record);
    }

    void setToFailure(Failure failure) {
        final CommandRecord.Builder builder = getState().toBuilder();
        builder.getStatusBuilder()
               .setCode(FAILURE)
               .setFailure(failure);
        final CommandRecord record = builder.build();
        updateState(record);
    }
}
