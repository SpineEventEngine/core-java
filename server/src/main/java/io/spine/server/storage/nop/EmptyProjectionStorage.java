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

package io.spine.server.storage.nop;

import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import io.spine.server.entity.EntityRecord;
import io.spine.server.projection.ProjectionStorage;
import io.spine.server.storage.RecordReadRequest;
import io.spine.server.storage.RecordStorage;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;

final class EmptyProjectionStorage<I>
        extends ProjectionStorage<I>
        implements EmptyStorageWithLifecycleFlags<I, EntityRecord, RecordReadRequest<I>> {

    private final EmptyRecordStorage<I> delegate;

    EmptyProjectionStorage(EmptyRecordStorage<I> delegate) {
        super(delegate.isMultitenant());
        this.delegate = delegate;
    }

    @Override
    protected void writeLastHandledEventTime(Timestamp time) {
        // NOP.
    }

    @Override
    protected @Nullable Timestamp readLastHandledEventTime() {
        return null;
    }

    @Override
    protected RecordStorage<I> recordStorage() {
        return delegate;
    }

    @Override
    public boolean delete(I id) {
        return delegate.delete(id);
    }

    @Override
    protected Iterator<@Nullable EntityRecord> readMultipleRecords(Iterable<I> ids) {
        return delegate.readMultipleRecords(ids);
    }

    @Override
    protected Iterator<@Nullable EntityRecord> readMultipleRecords(Iterable<I> ids,
                                                                   FieldMask fieldMask) {
        return delegate.readMultipleRecords(ids, fieldMask);
    }

    @Override
    protected Iterator<EntityRecord> readAllRecords() {
        return delegate.readAllRecords();
    }

    @Override
    protected Iterator<EntityRecord> readAllRecords(FieldMask fieldMask) {
        return delegate.readAllRecords(fieldMask);
    }
}
