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

package org.spine3.server.storage;

import com.google.common.base.Optional;
import com.google.protobuf.FieldMask;
import org.spine3.base.EventId;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.storage.EntityQuery;
import org.spine3.server.entity.storage.EntityRecordWithColumns;
import org.spine3.server.event.EventStoreIO;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.users.TenantId;

import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link RecordStorage} for events.
 *
 * @author Dmytro Dashenkov
 * @deprecated as a temporal solution before the {@link EventStreamQuery} processing is implemented
 * using Entity Columns feature
 */
@Deprecated
public abstract class EventRecordStorage extends RecordStorage<EventId> {

    private final RecordStorage<EventId> delegate;

    protected EventRecordStorage(RecordStorage<EventId> storage) {
        super(storage.isMultitenant());
        this.delegate = storage;
    }

    protected abstract Map<EventId, EntityRecord> readRecords(EventStreamQuery query);

    public Map<EventId, EntityRecord> readAll(EventStreamQuery query) {
        checkNotClosed();
        checkNotNull(query);

        return readRecords(query);
    }

    @Override
    public Iterator<EventId> index() {
        return delegate.index();
    }

    @Override
    public boolean delete(EventId id) {
        return delegate.delete(id);
    }

    @Override
    protected Optional<EntityRecord> readRecord(EventId id) {
        return delegate.read(id);
    }

    @Override
    protected Iterable<EntityRecord> readMultipleRecords(Iterable<EventId> ids) {
        return delegate.readMultiple(ids);
    }

    @Override
    protected Iterable<EntityRecord> readMultipleRecords(Iterable<EventId> ids,
                                                         FieldMask fieldMask) {
        return delegate.readMultiple(ids, fieldMask);
    }

    @Override
    protected Map<EventId, EntityRecord> readAllRecords() {
        return delegate.readAll();
    }

    @Override
    protected Map<EventId, EntityRecord> readAllRecords(FieldMask fieldMask) {
        return delegate.readAll(fieldMask);
    }

    @Override
    protected Map<EventId, EntityRecord> readAllRecords(EntityQuery<EventId> query,
                                                        FieldMask fieldMask) {
        return delegate.readAll(query, fieldMask);
    }

    @Override
    protected void writeRecord(EventId id, EntityRecordWithColumns record) {
        delegate.write(id, record);
    }

    @Override
    protected void writeRecords(Map<EventId, EntityRecordWithColumns> records) {
        delegate.write(records);
    }

    protected RecordStorage<EventId> getDelegateStorage() {
        return delegate;
    }

    /*
     * Beam support
     *****************/

    public abstract EventStoreIO.QueryFn queryFn(TenantId tenantId);
}
