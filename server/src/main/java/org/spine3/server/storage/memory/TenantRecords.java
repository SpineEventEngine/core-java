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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.entity.FieldMasks;
import org.spine3.server.entity.status.EntityStatus;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.storage.Predicates;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * The memory-based storage for {@code EntityStorageRecord} that represents
 * all storage operations available for data of a single tenant.
 *
 * @author Alexander Yevsyukov
 */
class TenantRecords<I> implements TenantStorage<I, EntityStorageRecord> {

    private final Map<I, EntityStorageRecord> records = newHashMap();
    private final Map<I, EntityStorageRecord> filtered = Maps.filterValues(records, Predicates.isVisible());

    @Override
    public void put(I id, EntityStorageRecord record) {
        records.put(id, record);
    }

    @Override
    public Optional<EntityStorageRecord> get(I id) {
        final EntityStorageRecord record = records.get(id);
        return Optional.fromNullable(record);
    }

    boolean markArchived(I id) {
        final EntityStorageRecord record = records.get(id);
        if (record == null) {
            return false;
        }
        final EntityStatus currentStatus = record.getEntityStatus();
        if (currentStatus.getArchived()) {
            return false;
        }
        final EntityStorageRecord archivedRecord = record.toBuilder()
                                                         .setEntityStatus(currentStatus.toBuilder()
                                                                                       .setArchived(true))
                                                         .build();
        records.put(id, archivedRecord);
        return true;
    }

    boolean markDeleted(I id) {
        final EntityStorageRecord record = records.get(id);
        if (record == null) {
            return false;
        }

        final EntityStatus currentStatus = record.getEntityStatus();
        if (currentStatus.getDeleted()) {
            return false;
        }
        final EntityStorageRecord deletedRecord = record.toBuilder()
                                                        .setEntityStatus(currentStatus.toBuilder()
                                                                                      .setDeleted(true))
                                                        .build();
        records.put(id, deletedRecord);
        return true;
    }

    boolean delete(I id) {
        return records.remove(id) != null;
    }

    private Map<I, EntityStorageRecord> filtered() {
        return filtered;
    }

    Map<I, EntityStorageRecord> readAllRecords() {
        final Map<I, EntityStorageRecord> filtered = filtered();
        final ImmutableMap<I, EntityStorageRecord> result = ImmutableMap.copyOf(filtered);
        return result;
    }

    EntityStorageRecord findAndApplyFieldMask(I givenId, FieldMask fieldMask) {
        EntityStorageRecord matchingResult = null;
        for (I recordId : filtered.keySet()) {
            if (recordId.equals(givenId)) {
                final Optional<EntityStorageRecord> record = get(recordId);
                if (!record.isPresent()) {
                    continue;
                }
                EntityStorageRecord.Builder matchingRecord = record.get().toBuilder();
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

    Map<I, EntityStorageRecord> readAllRecords(FieldMask fieldMask) {
        if (fieldMask.getPathsList()
                     .isEmpty()) {
            return readAllRecords();
        }

        if (isEmpty()) {
            return ImmutableMap.of();
        }

        final ImmutableMap.Builder<I, EntityStorageRecord> result = ImmutableMap.builder();

        for (Map.Entry<I, EntityStorageRecord> storageEntry : filtered.entrySet()) {
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

    @Override
    public boolean isEmpty() {
        return filtered.isEmpty();
    }
}
