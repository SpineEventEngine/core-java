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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.type.TypeUrl;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.transformValues;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.entity.EntityWithLifecycle.Predicates.isRecordWithColumnsVisible;
import static io.spine.server.entity.FieldMasks.applyMask;

/**
 * The memory-based storage for {@code EntityStorageRecord} that represents
 * all storage operations available for data of a single tenant.
 *
 * @author Alexander Yevsyukov
 */
class TenantRecords<I> implements TenantStorage<I, EntityRecordWithColumns> {

    private final Map<I, EntityRecordWithColumns> records = newHashMap();
    private final Map<I, EntityRecordWithColumns> filtered =
            filterValues(records, isRecordWithColumnsVisible());

    @Override
    public Iterator<I> index() {
        final Iterator<I> result = filtered.keySet()
                                           .iterator();
        return result;
    }

    @Override
    public void put(I id, EntityRecordWithColumns record) {
        records.put(id, record);
    }

    @Override
    public Optional<EntityRecordWithColumns> get(I id) {
        final EntityRecordWithColumns record = records.get(id);
        return Optional.fromNullable(record);
    }

    boolean delete(I id) {
        return records.remove(id) != null;
    }

    private Map<I, EntityRecordWithColumns> filtered() {
        return filtered;
    }

    Map<I, EntityRecord> readAllRecords() {
        final Map<I, EntityRecordWithColumns> filtered = filtered();
        final Map<I, EntityRecord> records = transformValues(filtered,
                                                             EntityRecordUnpacker.INSTANCE);
        final ImmutableMap<I, EntityRecord> result = ImmutableMap.copyOf(records);
        return result;
    }

    Map<I, EntityRecord> readAllRecords(EntityQuery<I> query, FieldMask fieldMask) {
        final Map<I, EntityRecordWithColumns> filtered = filterValues(records,
                                                                      new EntityQueryMatcher<>(query));
        final Map<I, EntityRecord> records = transformValues(filtered,
                                                             EntityRecordUnpacker.INSTANCE);
        final Function<EntityRecord, EntityRecord> fieldMaskApplier =
                new FieldMaskApplier(fieldMask);
        final Map<I, EntityRecord> maskedRecords = transformValues(records, fieldMaskApplier);
        final ImmutableMap<I, EntityRecord> result = ImmutableMap.copyOf(maskedRecords);
        return result;
    }

    EntityRecord findAndApplyFieldMask(I givenId, FieldMask fieldMask) {
        EntityRecord matchingResult = null;
        for (I recordId : filtered.keySet()) {
            if (recordId.equals(givenId)) {
                final Optional<EntityRecordWithColumns> record = get(recordId);
                if (!record.isPresent()) {
                    continue;
                }
                EntityRecord.Builder matchingRecord = record.get()
                                                            .getRecord()
                                                            .toBuilder();
                final Any state = matchingRecord.getState();
                final TypeUrl typeUrl = TypeUrl.parse(state.getTypeUrl());
                final Message wholeState = unpack(state);
                final Message maskedState = applyMask(fieldMask, wholeState, typeUrl);
                final Any processed = pack(maskedState);

                matchingRecord.setState(processed);
                matchingResult = matchingRecord.build();
            }
        }
        return matchingResult;
    }

    Map<I, EntityRecord> readAllRecords(FieldMask fieldMask) {
        if (fieldMask.getPathsList()
                     .isEmpty()) {
            return readAllRecords();
        }

        if (isEmpty()) {
            return ImmutableMap.of();
        }

        final ImmutableMap.Builder<I, EntityRecord> result = ImmutableMap.builder();

        for (Map.Entry<I, EntityRecordWithColumns> storageEntry : filtered.entrySet()) {
            final I id = storageEntry.getKey();
            final EntityRecord rawRecord = storageEntry.getValue()
                                                       .getRecord();
            final TypeUrl type = TypeUrl.parse(rawRecord.getState()
                                                        .getTypeUrl());
            final Any recordState = rawRecord.getState();
            final Message stateAsMessage = unpack(recordState);
            final Message processedState = applyMask(fieldMask, stateAsMessage, type);
            final Any packedState = pack(processedState);
            final EntityRecord resultingRecord = EntityRecord.newBuilder()
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

    /**
     * A {@link Function} transforming the {@link EntityRecord} state by applying the given
     * {@link FieldMask} to it.
     *
     * <p>The resulting {@link EntityRecord} has the same fields as the given one except
     * the {@code state} field, which is masked.
     */
    private static class FieldMaskApplier implements Function<EntityRecord, EntityRecord> {

        private final FieldMask fieldMask;

        private FieldMaskApplier(FieldMask fieldMask) {
            this.fieldMask = fieldMask;
        }

        @Nullable
        @Override
        public EntityRecord apply(@Nullable EntityRecord input) {
            checkNotNull(input);
            final Any packedState = input.getState();
            final Message state = unpack(packedState);
            final TypeUrl typeUrl = TypeUrl.ofEnclosed(packedState);
            final Message maskedState = applyMask(fieldMask, state, typeUrl);
            final Any repackedState = pack(maskedState);
            final EntityRecord result = EntityRecord.newBuilder(input)
                                                    .setState(repackedState)
                                                    .build();
            return result;
        }
    }
}
