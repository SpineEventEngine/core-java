/*
 * Copyright 2018, TeamDev. All rights reserved.
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
package io.spine.server.stand;

import com.google.protobuf.FieldMask;
import io.spine.annotation.Internal;
import io.spine.annotation.SPI;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityColumnCache;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.storage.RecordStorage;
import io.spine.type.TypeUrl;

import java.util.Iterator;

/**
 * Serves as a storage for the latest
 * {@link io.spine.server.aggregate.Aggregate Aggregate} states.
 *
 * <p>Used by an instance of {@link Stand} to optimize
 * the {@code Aggregate} state fetch performance.
 *
 * @author Alex Tymchenko
 * @see com.google.protobuf.Any#getTypeUrl() Any.getTypeUrl()
 * @see Stand
 */
@SPI
public abstract class StandStorage extends RecordStorage<AggregateStateId> {

    protected StandStorage(boolean multitenant) {
        super(multitenant);
    }

    /**
     * Reads all the state records by the given type.
     *
     * @param type a {@link TypeUrl} instance
     * @return the state records which {@link com.google.protobuf.Any#getTypeUrl() Any.getTypeUrl()}
     * equals the argument value
     */
    public abstract Iterator<EntityRecord> readAllByType(TypeUrl type);

    /**
     * Reads all the state records by the given type.
     *
     * @param type a {@link TypeUrl} instance
     * @return the state records which {@link com.google.protobuf.Any#getTypeUrl() Any.getTypeUrl()}
     * equals the argument value
     */
    public abstract Iterator<EntityRecord> readAllByType(TypeUrl type, FieldMask fieldMask);

    @Internal
    @Override
    public final EntityColumnCache entityColumnCache() {
        return recordStorage().entityColumnCache();
    }

    /**
     * Reads all the state records from the storage.
     *
     * <p>This method overrides the behaviour of the superclass. Since {@code StandStorage} does not
     * support Entity Columns, the passed {@link EntityQuery} does not affect the resulting
     * {@code Map} in any way.
     *
     * <p>Calling this method is equivalent to calling {@code readAll(fieldMask)}.
     *
     * @param query     ignored
     * @param fieldMask the fields to retrieve
     * @return all the records with the {@link FieldMask} applied
     * @see #readAll(FieldMask)
     */
    @Override
    public Iterator<EntityRecord>
    readAll(EntityQuery<AggregateStateId> query, FieldMask fieldMask) {
        return readAll(fieldMask);
    }

    @Override
    protected Iterator<EntityRecord>
    readAllRecords(EntityQuery<AggregateStateId> query, FieldMask fieldMask) {
        throw new IllegalStateException("Call #readAll(EntityQuery, FieldMask) instead.");
    }

    /** Returns an entity storage implementation. */
    protected abstract RecordStorage<?> recordStorage();
}
