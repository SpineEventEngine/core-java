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

import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.projection.ProjectionStorage;
import org.spine3.server.storage.RecordStorage;

import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The in-memory implementation of {@link ProjectionStorage}.
 *
 * @param <I> the type of stream projection IDs
 * @author Alexander Litus
 */
class InMemoryProjectionStorage<I> extends ProjectionStorage<I> {

    private final InMemoryRecordStorage<I> recordStorage;

    /** The time of the last handled event. */
    private Timestamp timestampOfLastEvent;

    public static <I> InMemoryProjectionStorage<I> newInstance(InMemoryRecordStorage<I> entityStorage) {
        return new InMemoryProjectionStorage<>(entityStorage);
    }

    private InMemoryProjectionStorage(InMemoryRecordStorage<I> recordStorage) {
        super(recordStorage.isMultitenant());
        this.recordStorage = recordStorage;
    }

    @Override
    public Iterator<I> index() {
        return recordStorage.index();
    }

    @Override
    public void writeLastHandledEventTime(Timestamp timestamp) {
        checkNotNull(timestamp);
        this.timestampOfLastEvent = timestamp;
    }

    @Override
    public Timestamp readLastHandledEventTime() {
        return timestampOfLastEvent;
    }

    @Override
    public RecordStorage<I> recordStorage() {
        return recordStorage;
    }

    @Override
    public void close() throws Exception {
        recordStorage.close();
        super.close();
    }

    @Override
    public void markArchived(I id) {
        recordStorage().markArchived(id);
    }

    @Override
    public void markDeleted(I id) {
        recordStorage().markDeleted(id);
    }

    @Override
    public boolean delete(I id) {
        return recordStorage().delete(id);
    }

    @Override
    protected Iterable<EntityRecord> readMultipleRecords(Iterable<I> ids) {
        final Iterable<EntityRecord> result = recordStorage.readMultiple(ids);
        return result;
    }

    @Override
    protected Iterable<EntityRecord> readMultipleRecords(Iterable<I> ids, FieldMask fieldMask) {
        return recordStorage.readMultiple(ids, fieldMask);
    }

    @Override
    protected Map<I, EntityRecord> readAllRecords() {
        final Map<I, EntityRecord> result = recordStorage.readAll();
        return result;
    }

    @Override
    protected Map<I, EntityRecord> readAllRecords(FieldMask fieldMask) {
        final Map<I, EntityRecord> result = recordStorage.readAll(fieldMask);
        return result;
    }
}
