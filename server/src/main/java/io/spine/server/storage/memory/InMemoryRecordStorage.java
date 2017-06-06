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

package io.spine.server.storage.memory;

import com.google.common.base.Optional;
import com.google.protobuf.FieldMask;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.RecordStorageIO;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Memory-based implementation of {@link RecordStorage}.
 *
 * @param <I> the type of entity IDs
 * @author Alexander Litus
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 */
class InMemoryRecordStorage<I> extends RecordStorage<I> {

    private final TypeUrl entityStateUrl;
    private final MultitenantStorage<TenantRecords<I>> multitenantStorage;

    InMemoryRecordStorage(TypeUrl entityStateUrl, boolean multitenant) {
        super(multitenant);
        this.entityStateUrl = entityStateUrl;
        this.multitenantStorage = new MultitenantStorage<TenantRecords<I>>(multitenant) {
            @Override
            TenantRecords<I> createSlice() {
                return new TenantRecords<>();
            }
        };
    }

    protected static <I> InMemoryRecordStorage<I> newInstance(TypeUrl entityStateUrl,
                                                              boolean multitenant) {
        return new InMemoryRecordStorage<>(entityStateUrl, multitenant);
    }

    @Override
    public Iterator<I> index() {
        return getStorage().index();
    }

    @Override
    public boolean delete(I id) {
        return getStorage().delete(id);
    }

    @Override
    protected Iterable<EntityRecord> readMultipleRecords(final Iterable<I> givenIds,
                                                         FieldMask fieldMask) {
        final TenantRecords<I> storage = getStorage();

        // It is not possible to return an immutable collection,
        // since null may be present in it.
        final Collection<EntityRecord> result = new LinkedList<>();

        for (I givenId : givenIds) {
            final EntityRecord matchingResult = storage.findAndApplyFieldMask(givenId, fieldMask);
            result.add(matchingResult);
        }
        return result;
    }

    @Override
    protected Iterable<EntityRecord> readMultipleRecords(Iterable<I> ids) {
        return readMultipleRecords(ids, FieldMask.getDefaultInstance());
    }

    @Override
    protected Map<I, EntityRecord> readAllRecords() {
        return getStorage().readAllRecords();
    }

    @Override
    protected Map<I, EntityRecord> readAllRecords(FieldMask fieldMask) {
        return getStorage().readAllRecords(fieldMask);
    }

    @Override
    protected Map<I, EntityRecord> readAllRecords(EntityQuery<I> query, FieldMask fieldMask) {
        return getStorage().readAllRecords(query, fieldMask);
    }

    private TenantRecords<I> getStorage() {
        return multitenantStorage.getStorage();
    }

    @Override
    protected Optional<EntityRecord> readRecord(I id) {
        return getStorage().get(id)
                           .transform(EntityRecordUnpacker.INSTANCE);
    }

    @Override
    protected void writeRecord(I id, EntityRecordWithColumns record) {
        getStorage().put(id, record);
    }

    @Override
    protected void writeRecords(Map<I, EntityRecordWithColumns> records) {
        final TenantRecords<I> storage = getStorage();
        for (Map.Entry<I, EntityRecordWithColumns> record : records.entrySet()) {
            storage.put(record.getKey(), record.getValue());
        }
    }

    /*
     * Beam support
     ******************/

    @Override
    public RecordStorageIO<I> getIO(Class<I> idClass) {
        return new InMemoryRecordStorageIO<>(idClass, entityStateUrl);
    }
}
