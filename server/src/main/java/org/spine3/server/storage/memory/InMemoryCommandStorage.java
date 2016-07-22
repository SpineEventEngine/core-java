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

package org.spine3.server.storage.memory;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Error;
import org.spine3.base.Failure;
import org.spine3.server.storage.CommandStorage;
import org.spine3.server.storage.CommandStorageRecord;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.validate.Validate.checkNotDefault;
import static org.spine3.validate.Validate.isNotDefault;

/* package */ class InMemoryCommandStorage extends CommandStorage {

    private final Map<CommandId, CommandStorageRecord> storage = newHashMap();

    protected InMemoryCommandStorage(boolean multitenant) {
        super(multitenant);
    }

    @Override
    public void write(CommandId id, CommandStorageRecord record) {
        checkNotClosed();
        checkNotDefault(id);
        checkNotDefault(record);
        put(id, record);
    }

    @Override
    public CommandStorageRecord read(CommandId id) {
        checkNotClosed();
        checkNotDefault(id);
        final CommandStorageRecord record = get(id);
        return record;
    }

    @Override
    protected Iterator<CommandStorageRecord> read(final CommandStatus status) {
        checkNotClosed();
        final Collection<CommandStorageRecord> records = storage.values();
        final Iterator<CommandStorageRecord> filteredRecords = filterByStatus(records.iterator(), status);
        return filteredRecords;
    }

    private static Iterator<CommandStorageRecord> filterByStatus(Iterator<CommandStorageRecord> records,
                                                                 final CommandStatus status) {
        return Iterators.filter(records, new Predicate<CommandStorageRecord>() {
            @Override
            public boolean apply(@Nullable CommandStorageRecord record) {
                if (record == null) {
                    return false;
                }
                final boolean statusMatches = record.getStatus() == status;
                return statusMatches;
            }
        });
    }

    @Override
    public void updateStatus(CommandId id, Error error) {
        checkNotClosed();
        checkNotNull(id);
        checkNotNull(error);
        final CommandStorageRecord record = check(get(id), id);
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
        final CommandStorageRecord record = check(get(id), id);
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
        final CommandStorageRecord record = check(get(id), id);
        final CommandStorageRecord updatedRecord = record.toBuilder()
                                                         .setStatus(CommandStatus.OK)
                                                         .build();
        put(id, updatedRecord);
    }

    private void put(CommandId id, CommandStorageRecord record) {
        storage.put(id, record);
    }

    private CommandStorageRecord get(CommandId id) {
        final CommandStorageRecord record = storage.get(id);
        if (record == null) {
            return CommandStorageRecord.getDefaultInstance();
        }
        return record;
    }

    private static CommandStorageRecord check(CommandStorageRecord record, CommandId id) {
        checkState(isNotDefault(record), "No record found for command ID: " + idToString(id));
        return record;
    }
}
