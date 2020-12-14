/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.projection;

import io.spine.annotation.Internal;
import io.spine.annotation.SPI;
import io.spine.client.ResponseFormat;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.Columns;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.RecordReadRequest;
import io.spine.server.storage.RecordStorage;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * The storage used by projection repositories for keeping {@link Projection}s
 * and the timestamp of the last event processed by the projection repository.
 *
 * <p>This timestamp is used for 'catch-up' operation of the projection repositories.
 *
 * @param <I> the type of stream projection IDs
 */
@SPI
public abstract class ProjectionStorage<I> extends RecordStorage<I> {

    protected ProjectionStorage(Class<? extends Projection<?, ?, ?>> projectionClass,
                                boolean multitenant) {
        super(projectionClass, multitenant);
    }

    @Internal
    @Override
    public Columns columns() {
        return recordStorage().columns();
    }

    @Override
    protected Optional<EntityRecord> readRecord(I id) {
        RecordStorage<I> storage = recordStorage();
        RecordReadRequest<I> request = new RecordReadRequest<>(id);
        Optional<EntityRecord> record = storage.read(request);
        return record;
    }

    @Override
    protected void writeRecord(I id, EntityRecordWithColumns record) {
        RecordStorage<I> storage = recordStorage();
        storage.write(id, record);
    }

    @Override
    protected void writeRecords(Map<I, EntityRecordWithColumns> records) {
        RecordStorage<I> storage = recordStorage();
        storage.write(records);
    }

    @Override
    protected Iterator<EntityRecord>
    readAllRecords(EntityQuery<I> query, ResponseFormat format) {
        RecordStorage<I> storage = recordStorage();
        return storage.readAll(query, format);
    }

    /** Returns an entity storage implementation. */
    protected abstract RecordStorage<I> recordStorage();
}
