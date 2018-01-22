/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.storage.memory;

import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import io.spine.core.TenantId;
import io.spine.server.entity.EntityRecord;
import io.spine.server.projection.ProjectionStorage;
import io.spine.server.storage.RecordStorage;
import io.spine.server.tenant.TenantFunction;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newConcurrentMap;

/**
 * The in-memory implementation of {@link ProjectionStorage}.
 *
 * @param <I> the type of stream projection IDs
 * @author Alexander Litus
 */
public class InMemoryProjectionStorage<I> extends ProjectionStorage<I> {

    /** The storage for projection entities. */
    private final InMemoryRecordStorage<I> recordStorage;

    /** The time of the last handled event per tenant. */
    private final Map<TenantId, Timestamp> timestampOfLastEvent = newConcurrentMap();

    public static <I>
    InMemoryProjectionStorage<I> newInstance(InMemoryRecordStorage<I> entityStorage) {
        return new InMemoryProjectionStorage<>(entityStorage);
    }

    private InMemoryProjectionStorage(InMemoryRecordStorage<I> recordStorage) {
        super(recordStorage.isMultitenant());
        this.recordStorage = recordStorage;
    }

    StorageSpec<I> getSpec() {
        return recordStorage.getSpec();
    }

    @Override
    public Iterator<I> index() {
        return recordStorage.index();
    }

    @Override
    public void writeLastHandledEventTime(final Timestamp timestamp) {
        checkNotNull(timestamp);
        final TenantFunction<Void> func = new TenantFunction<Void>(isMultitenant()) {
            @Nullable
            @Override
            public Void apply(@Nullable TenantId tenantId) {
                checkNotNull(tenantId);
                timestampOfLastEvent.put(tenantId, timestamp);
                return null;
            }
        };
        func.execute();
    }

    @Override
    public Timestamp readLastHandledEventTime() {
        final TenantFunction<Timestamp> func = new TenantFunction<Timestamp>(isMultitenant()) {
            @Nullable
            @Override
            public Timestamp apply(@Nullable TenantId tenantId) {
                checkNotNull(tenantId);
                final Timestamp result = timestampOfLastEvent.get(tenantId);
                return result;
            }
        };
        return func.execute();
    }

    @Override
    public RecordStorage<I> recordStorage() {
        return recordStorage;
    }

    @Override
    public void close() {
        recordStorage.close();
        super.close();
    }

    @Override
    public boolean delete(I id) {
        return recordStorage().delete(id);
    }

    @Override
    protected Iterator<EntityRecord> readMultipleRecords(Iterable<I> ids) {
        final Iterator<EntityRecord> result = recordStorage.readMultiple(ids);
        return result;
    }

    @Override
    protected Iterator<EntityRecord> readMultipleRecords(Iterable<I> ids, FieldMask fieldMask) {
        return recordStorage.readMultiple(ids, fieldMask);
    }

    @Override
    protected Iterator<EntityRecord> readAllRecords() {
        final Iterator<EntityRecord> result = recordStorage.readAll();
        return result;
    }

    @Override
    protected Iterator<EntityRecord> readAllRecords(FieldMask fieldMask) {
        final Iterator<EntityRecord> result = recordStorage.readAll(fieldMask);
        return result;
    }
}
