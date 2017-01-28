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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.entity.FieldMasks;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.storage.Predicates;
import org.spine3.server.storage.RecordStorage;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * Memory-based implementation of {@link RecordStorage}.
 *
 * @param <I> the type of entity IDs
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
class InMemoryRecordStorage<I> extends RecordStorage<I> {

    private final MultitenantStorage<I, TenantRecords<I>> multitenantStorage;

    protected InMemoryRecordStorage(boolean multitenant) {
        super(multitenant);
        this.multitenantStorage = new MultitenantStorage<I, TenantRecords<I>>(multitenant) {
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
    protected Iterable<EntityStorageRecord> readMultipleRecords(final Iterable<I> givenIds, FieldMask fieldMask) {
        final TenantRecords<I> storage = getStorage();

        // It is not possible to return an immutable collection, since {@code null} may be present in it.
        final Collection<EntityStorageRecord> result = new LinkedList<>();

        for (I givenId : givenIds) {
            final EntityStorageRecord matchingResult = findAndApplyFieldMask(storage, givenId, fieldMask);
            result.add(matchingResult);
        }
        return result;
    }

    private EntityStorageRecord findAndApplyFieldMask(TenantRecords<I> storage,
                                                      I givenId,
                                                      FieldMask fieldMask) {
        EntityStorageRecord matchingResult = null;
        for (I recordId : storage.filtered.keySet()) {
            if (recordId.equals(givenId)) {
                final EntityStorageRecord record = storage.get(recordId);
                if (record == null) {
                    continue;
                }
                EntityStorageRecord.Builder matchingRecord = record.toBuilder();
                final Any state = matchingRecord.getState();
                final TypeUrl typeUrl = TypeUrl.of(state.getTypeUrl());
                final Message wholeState = AnyPacker.unpack(state);
                final Message maskedState = FieldMasks.applyMask(fieldMask, wholeState, typeUrl);
                final Any processed = AnyPacker.pack(maskedState);

                matchingRecord.setState(processed);
                matchingResult = matchingRecord.build();
            }
        }
        return matchingResult;
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
        if (fieldMask.getPathsList()
                     .isEmpty()) {
            return readAllRecords();
        }

        final TenantRecords<I> storage = getStorage();

        if (storage.isEmpty()) {
            return ImmutableMap.of();
        }

        final ImmutableMap.Builder<I, EntityStorageRecord> result = ImmutableMap.builder();

        for (Map.Entry<I, EntityStorageRecord> storageEntry : storage.filtered.entrySet()) {
            final I id = storageEntry.getKey();
            final EntityStorageRecord rawRecord = storageEntry.getValue();
            final TypeUrl type = TypeUrl.of(rawRecord.getState()
                                                     .getTypeUrl());
            final Any recordState = rawRecord.getState();
            final Message stateAsMessage = AnyPacker.unpack(recordState);
            final Message processedState = FieldMasks.applyMask(fieldMask, stateAsMessage, type);
            final Any packedState = AnyPacker.pack(processedState);
            final EntityStorageRecord resultingRecord = EntityStorageRecord.newBuilder()
                                                                           .setState(packedState)
                                                                           .build();
            result.put(id, resultingRecord);
        }

        return result.build();
    }

    protected static <I> InMemoryRecordStorage<I> newInstance(boolean multitenant) {
        return new InMemoryRecordStorage<>(multitenant);
    }

    private TenantRecords<I> getStorage() {
        return multitenantStorage.getStorage();
    }

    @Override
    protected EntityStorageRecord readRecord(I id) {
        return getStorage().get(id);
    }

    @Override
    protected void writeRecord(I id, EntityStorageRecord record) {
        getStorage().put(id, record);
    }

    /**
     * The memory-based storage for {@code EntityStorageRecord} that represents
     * all storage operations available for data of a single tenant.
     */
    private static class TenantRecords<I> implements TenantStorage<I, EntityStorageRecord> {

        private final Map<I, EntityStorageRecord> records = newHashMap();
        private final Map<I, EntityStorageRecord> filtered = Maps.filterValues(records, Predicates.isVisible);

        @Override
        public void put(I id, EntityStorageRecord record) {
            records.put(id, record);
        }

        @Nullable
        @Override
        public EntityStorageRecord get(I id) {
            final EntityStorageRecord record = records.get(id);
            if (!Predicates.isVisible.apply(record)) {
                return null;
            }
            return record;
        }

        private boolean markArchived(I id) {
            final EntityStorageRecord record = records.get(id);
            if (record == null) {
                return false;
            }
            if (record.getArchived()) {
                return false;
            }
            final EntityStorageRecord archivedRecord = record.toBuilder()
                                                             .setArchived(true)
                                                             .build();
            records.put(id, archivedRecord);
            return true;
        }

        private Map<I, EntityStorageRecord> filtered() {
            return filtered;
        }

        private Map<I, EntityStorageRecord> readAllRecords() {
            final Map<I, EntityStorageRecord> filtered = filtered();
            final ImmutableMap<I, EntityStorageRecord> result = ImmutableMap.copyOf(filtered);
            return result;
        }

        @Override
        public boolean isEmpty() {
            return filtered.isEmpty();
        }
    }
}
