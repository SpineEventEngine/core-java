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

package org.spine3.server.storage.memory;

import com.google.common.base.Optional;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Error;
import org.spine3.base.Failure;
import org.spine3.server.command.CommandStorage;
import org.spine3.server.command.storage.CommandStorageRecord;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.spine3.base.Stringifiers.idToString;
import static org.spine3.validate.Validate.checkNotDefault;

/**
 * In-memory implementation of {@code CommandStorage}.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
class InMemoryCommandStorage extends CommandStorage {

    private final MultitenantStorage<TenantCommands> multitenantStorage;

    protected InMemoryCommandStorage(boolean multitenant) {
        super(multitenant);
        this.multitenantStorage = new MultitenantStorage<TenantCommands>(multitenant) {
            @Override
            TenantCommands createSlice() {
                return new TenantCommands();
            }
        };
    }

    @Override
    public void write(CommandId id, CommandStorageRecord record) {
        checkNotClosed();
        checkNotDefault(id);
        checkNotDefault(record);
        put(id, record);
    }

    @Override
    public Optional<CommandStorageRecord> read(CommandId id) {
        checkNotClosed();
        checkNotDefault(id);
        final Optional<CommandStorageRecord> record = get(id);
        return record;
    }

    private TenantCommands getStorage() {
        return multitenantStorage.getStorage();
    }

    @Override
    protected Iterator<CommandStorageRecord> read(final CommandStatus status) {
        checkNotClosed();
        return getStorage().getByStatus(status);
    }

    @Override
    public void updateStatus(CommandId id, Error error) {
        checkNotClosed();
        checkNotNull(id);
        checkNotNull(error);
        final CommandStorageRecord record = checkFound(get(id), id);
        final CommandStorageRecord updatedRecord = record.toBuilder()
                                                         .setStatus(CommandStatus.ERROR)
                                                         .setError(error)
                                                         .build();
        put(id, updatedRecord);
    }

    @Override
    public void updateStatus(CommandId id, Failure failure) {
        checkNotClosed();
        checkNotNull(id);
        checkNotNull(failure);
        final CommandStorageRecord record = checkFound(get(id), id);
        final CommandStorageRecord updatedRecord = record.toBuilder()
                                                         .setStatus(CommandStatus.FAILURE)
                                                         .setFailure(failure)
                                                         .build();
        put(id, updatedRecord);
    }

    @Override
    public void setOkStatus(CommandId id) {
        checkNotClosed();
        checkNotNull(id);
        final CommandStorageRecord record = checkFound(get(id), id);
        final CommandStorageRecord updatedRecord = record.toBuilder()
                                                         .setStatus(CommandStatus.OK)
                                                         .build();
        put(id, updatedRecord);
    }

    private void put(CommandId id, CommandStorageRecord record) {
        getStorage().put(id, record);
    }

    private Optional<CommandStorageRecord> get(CommandId id) {
        return getStorage().get(id);
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "OptionalUsedAsFieldOrParameterType"})
        // We do check. It's the purpose of this method.
    private static CommandStorageRecord checkFound(Optional<CommandStorageRecord> record, CommandId id) {
        checkState(record.isPresent(), "No record found for command ID: " + idToString(id));
        return record.get();
    }
}
