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

import io.spine.base.Error;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.CommandStatus;
import io.spine.core.Rejection;
import io.spine.server.commandbus.CommandRecord;
import io.spine.server.entity.AbstractEntity;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.CommandStatus.ERROR;
import static io.spine.core.CommandStatus.OK;
import static io.spine.core.CommandStatus.REJECTED;

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

        CommandId commandId = command.getId();
        CEntity entity = create(commandId);
        entity.setCommandAndStatus(command, status);
        return entity;
    }

    static CEntity createForError(Command command, Error error) {
        checkNotNull(command);
        checkNotNull(error);

        CommandId id = Records.getOrGenerateCommandId(command);

        CEntity result = create(id);
        result.setError(id, command, error);
        return result;
    }

    private void setCommandAndStatus(Command command, CommandStatus status) {
        CommandRecord record = Records.newRecordBuilder(command, status, null)
                                      .build();
        updateState(record);
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    private void setError(CommandId id, Command command, Error error) {
        CommandRecord.Builder builder = Records.newRecordBuilder(command, ERROR, id);
        builder.getStatusBuilder()
               .setError(error);
        CommandRecord record = builder.build();
        updateState(record);
    }

    private CommandRecord.Builder stateBuilder() {
        return getState().toBuilder();
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    void setOkStatus() {
        CommandRecord.Builder builder = stateBuilder();
        builder.getStatusBuilder()
               .setCode(OK);
        CommandRecord record = builder.build();
        updateState(record);
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    void setToError(Error error) {
        CommandRecord.Builder builder = stateBuilder();
        builder.getStatusBuilder()
               .setCode(ERROR)
               .setError(error);
        CommandRecord record = builder.build();
        updateState(record);
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    void setToRejected(Rejection rejection) {
        CommandRecord.Builder builder = stateBuilder();
        builder.getStatusBuilder()
               .setCode(REJECTED)
               .setRejection(rejection);
        CommandRecord record = builder.build();
        updateState(record);
    }
}
