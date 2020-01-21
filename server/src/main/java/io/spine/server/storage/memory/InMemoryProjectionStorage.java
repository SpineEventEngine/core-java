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

package io.spine.server.storage.memory;

import com.google.protobuf.FieldMask;
import io.spine.client.ResponseFormat;
import io.spine.server.entity.EntityRecord;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionStorage;
import io.spine.server.storage.RecordStorage;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;

/**
 * The in-memory implementation of {@link ProjectionStorage}.
 *
 * @param <I>
 *         the type of stream projection IDs
 */
public final class InMemoryProjectionStorage<I> extends ProjectionStorage<I> {

    /** The storage for projection entities. */
    private final InMemoryRecordStorage<I> recordStorage;

    InMemoryProjectionStorage(Class<? extends Projection<?, ?, ?>> projectionClass,
                              InMemoryRecordStorage<I> recordStorage) {
        super(projectionClass, recordStorage.isMultitenant());
        this.recordStorage = recordStorage;
    }

    @Override
    public Iterator<I> index() {
        return recordStorage.index();
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
    protected Iterator<@Nullable EntityRecord> readMultipleRecords(Iterable<I> ids,
                                                                   FieldMask fieldMask) {
        return recordStorage.readMultiple(ids, fieldMask);
    }

    @Override
    protected Iterator<EntityRecord> readAllRecords(ResponseFormat format) {
        Iterator<EntityRecord> result = recordStorage.readAll(format);
        return result;
    }
}
