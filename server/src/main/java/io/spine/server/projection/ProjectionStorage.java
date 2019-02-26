/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.annotation.SPI;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityColumnCache;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.RecordReadRequest;
import io.spine.server.storage.RecordStorage;
import org.checkerframework.checker.nullness.qual.Nullable;

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
 * @author Alexander Litus
 */
@SPI
public abstract class ProjectionStorage<I> extends RecordStorage<I> {

    protected ProjectionStorage(boolean multitenant) {
        super(multitenant);
    }

    @Internal
    @Override
    public final EntityColumnCache entityColumnCache() {
        return recordStorage().entityColumnCache();
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
    protected Iterator<EntityRecord> readAllRecords(EntityQuery<I> query, FieldMask fieldMask) {
        RecordStorage<I> storage = recordStorage();
        return storage.readAll(query, fieldMask);
    }

    /**
     * Writes the time of the last handled event to the storage.
     *
     * @param time the time of the event
     */
    protected abstract void writeLastHandledEventTime(Timestamp time);

    /**
     * Reads the time of the last handled event from the storage.
     *
     * @return the time of the last event or {@code null} if there is no event in the storage
     */
    protected abstract @Nullable Timestamp readLastHandledEventTime();

    /** Returns an entity storage implementation. */
    protected abstract RecordStorage<I> recordStorage();
}
