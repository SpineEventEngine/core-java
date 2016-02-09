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

import org.spine3.base.CommandId;
import org.spine3.server.storage.CommandStorage;
import org.spine3.server.storage.CommandStorageRecord;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static org.spine3.protobuf.Messages.checkNotDefault;

/*package*/ class InMemoryCommandStorage extends CommandStorage {

    private final Map<CommandId, CommandStorageRecord> storage = newHashMap();

    @Override
    public void write(CommandId id, CommandStorageRecord record) {
        checkNotNull(id);
        checkNotDefault(id);
        checkNotNull(record);

        final String commandId =  record.getCommandId();
        if (commandId.isEmpty() || commandId.trim().isEmpty()) {
            throw new IllegalArgumentException("Command id in the record can not be empty or blank.");
        }

        storage.put(id, record);
    }

    @Nullable
    @Override
    @SuppressWarnings("RefusedBequest") // ignores the method from the superclass because it throws an exception
    public CommandStorageRecord read(CommandId id) {
        checkNotNull(id);
        final CommandStorageRecord result = storage.get(id);
        return result;
    }
}
