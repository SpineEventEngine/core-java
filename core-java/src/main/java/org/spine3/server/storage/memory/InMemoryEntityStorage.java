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

import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.EntityStorageRecord;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;
import static org.spine3.util.Identifiers.idToString;

/**
 * Memory-based implementation of {@link EntityStorage}.
 *
 * @author Alexander Litus
 */
class InMemoryEntityStorage<I> extends EntityStorage<I> {

    private final Map<String, EntityStorageRecord> storage = newHashMap();

    protected static <I> InMemoryEntityStorage<I> newInstance() {
        return new InMemoryEntityStorage<>();
    }

    @Nullable
    @Override
    public EntityStorageRecord read(I id) {
        final String idString = idToString(id);
        final EntityStorageRecord record = storage.get(idString);
        return record;
    }

    @Override
    public void write(EntityStorageRecord record) {
        checkArgument(record.hasState(), "entity state");

        final String id = record.getEntityId();
        storage.put(id, record);
    }

    /**
     * Clears all data in the storage.
     */
    protected void clear() {
        storage.clear();
    }
}
