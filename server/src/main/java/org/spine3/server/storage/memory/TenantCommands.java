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
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.server.command.CommandRecord;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * The storage of commands for a tenant.
 *
 * @author Alexander Yevsyukov
 */
class TenantCommands implements TenantStorage<CommandId, CommandRecord> {

    private final Map<CommandId, CommandRecord> storage = newHashMap();

    @Nullable
    @Override
    public Optional<CommandRecord> get(CommandId id) {
        final Optional<CommandRecord> record = Optional.fromNullable(storage.get(id));
        return record;
    }

    Iterator<CommandRecord> getByStatus(CommandStatus status) {
        final Collection<CommandRecord> records = storage.values();
        final Iterator<CommandRecord> filteredRecords = filterByStatus(records.iterator(), status);
        return filteredRecords;
    }

    @Override
    public void put(CommandId id, CommandRecord record) {
        storage.put(id, record);
    }

    @Override
    public boolean isEmpty() {
        return storage.isEmpty();
    }

    private static Iterator<CommandRecord> filterByStatus(Iterator<CommandRecord> records,
                                                          final CommandStatus status) {
        return Iterators.filter(records, new Predicate<CommandRecord>() {
            @Override
            public boolean apply(@Nullable CommandRecord record) {
                if (record == null) {
                    return false;
                }
                final boolean statusMatches = record.getStatus()
                                                    .getCode() == status;
                return statusMatches;
            }
        });
    }
}
