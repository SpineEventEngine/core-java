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

import com.google.common.base.Optional;
import com.google.protobuf.FieldMask;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.storage.RecordStorage;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

/**
 * Memory-based implementation of {@link RecordStorage}.
 *
 * @param <I> the type of entity IDs
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
class InMemoryRecordStorage<I> extends RecordStorage<I> {

    private final MultitenantStorage<TenantRecords<I>> multitenantStorage;

    protected InMemoryRecordStorage(boolean multitenant) {
        super(multitenant);
        this.multitenantStorage = new MultitenantStorage<TenantRecords<I>>(multitenant) {
            @Override
            TenantRecords<I> createSlice() {
                return new TenantRecords<>();
            }
        };
    }

    @Override
    public boolean markArchived(I id) {
        return getStorage().markArchived(id);
    }

    @Override
    public boolean markDeleted(I id) {
        return getStorage().markDeleted(id);
    }

    @Override
    public boolean delete(I id) {
        return getStorage().delete(id);
    }

    @Override
    protected Iterable<EntityStorageRecord> readMultipleRecords(final Iterable<I> givenIds, FieldMask fieldMask) {
        final TenantRecords<I> storage = getStorage();

        // It is not possible to return an immutable collection, since {@code null} may be present in it.
        final Collection<EntityStorageRecord> result = new LinkedList<>();

        for (I givenId : givenIds) {
            final EntityStorageRecord matchingResult = storage.findAndApplyFieldMask(givenId, fieldMask);
            result.add(matchingResult);
        }
        return result;
    }

    @Override
    protected Iterable<EntityStorageRecord> readMultipleRecords(Iterable<I> ids) {
        return readMultipleRecords(ids, FieldMask.getDefaultInstance());
    }

    @Override
    protected Map<I, EntityStorageRecord> readAllRecords() {
        return getStorage().readAllRecords();
    }

    @Override
    protected Map<I, EntityStorageRecord> readAllRecords(FieldMask fieldMask) {
        return getStorage().readAllRecords(fieldMask);
    }

    protected static <I> InMemoryRecordStorage<I> newInstance(boolean multitenant) {
        return new InMemoryRecordStorage<>(multitenant);
    }

    private TenantRecords<I> getStorage() {
        return multitenantStorage.getStorage();
    }

    @Override
    protected Optional<EntityStorageRecord> readRecord(I id) {
        return getStorage().get(id);
    }

    @Override
    protected void writeRecord(I id, EntityStorageRecord record) {
        getStorage().put(id, record);
    }

    @Override
    protected void writeRecords(Map<I, EntityStorageRecord> records) {
        final TenantRecords<I> storage = getStorage();
        for (Map.Entry<I, EntityStorageRecord> record : records.entrySet()) {
            storage.put(record.getKey(), record.getValue());
        }
    }
}
