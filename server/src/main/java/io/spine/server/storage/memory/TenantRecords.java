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

package io.spine.server.storage.memory;

import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.entity.storage.QueryParameters;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Maps.newConcurrentMap;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.entity.EntityWithLifecycle.Predicates.isRecordWithColumnsActive;
import static io.spine.server.entity.FieldMasks.applyMask;
import static io.spine.server.storage.memory.EntityRecordComparator.orderedBy;

/**
 * The memory-based storage for {@link EntityRecord} that represents
 * all storage operations available for data of a single tenant.
 *
 * @author Alexander Yevsyukov
 * @author Dmitry Ganzha
 */
class TenantRecords<I> implements TenantStorage<I, EntityRecordWithColumns> {

    private final Map<I, EntityRecordWithColumns> records = newConcurrentMap();
    private final Map<I, EntityRecordWithColumns> activeRecords =
            filterValues(records, isRecordWithColumnsActive()::test);
    private static final EntityRecordUnpacker UNPACKER = EntityRecordUnpacker.INSTANCE;

    @Override
    public Iterator<I> index() {
        Iterator<I> result = activeRecords.keySet()
                                          .iterator();
        return result;
    }

    @Override
    public void put(I id, EntityRecordWithColumns record) {
        records.put(id, record);
    }

    @Override
    public Optional<EntityRecordWithColumns> get(I id) {
        EntityRecordWithColumns record = records.get(id);
        return Optional.ofNullable(record);
    }

    boolean delete(I id) {
        return records.remove(id) != null;
    }

    private Map<I, EntityRecordWithColumns> activeRecords() {
        return activeRecords;
    }

    Iterator<EntityRecord> readAllRecords() {
        return activeRecords()
                .values()
                .stream()
                .map(UNPACKER)
                .iterator();
    }

    Iterator<EntityRecord> readAllRecords(FieldMask fieldMask) {
        if (fieldMask.getPathsCount() == 0) {
            return readAllRecords();
        }

        return activeRecords()
                .values()
                .stream()
                .map(UNPACKER)
                .map(new FieldMaskApplier(fieldMask))
                .iterator();
    }

    Iterator<EntityRecord> readAllRecords(EntityQuery<I> query, FieldMask fieldMask) {
        return findRecords(query)
                .map(UNPACKER)
                .map(new FieldMaskApplier(fieldMask))
                .iterator();
    }

    private Stream<EntityRecordWithColumns> findRecords(EntityQuery<I> query) {
        Map<I, EntityRecordWithColumns> records = filterRecords(query);
        QueryParameters parameters = query.getParameters();
        Stream<EntityRecordWithColumns> stream = records.values()
                                                        .stream();
        if (parameters.ordered()) {
            stream = stream.sorted(orderedBy(parameters.orderBy()));
        }
        if (parameters.limited()) {
            stream = stream.limit(parameters.limit());
        }
        return stream;
    }

    /**
     * Filters the records returning only the ones matching the
     * {@linkplain EntityQuery entity query}.
     */
    private Map<I, EntityRecordWithColumns> filterRecords(EntityQuery<I> query) {
        EntityQueryMatcher<I> matcher = new EntityQueryMatcher<>(query);
        return filterValues(records, matcher::test);
    }

    @Nullable
    EntityRecord findAndApplyFieldMask(I targetId, FieldMask fieldMask) {
        EntityRecordWithColumns recordWithColumns = activeRecords().get(targetId);
        if (recordWithColumns == null) {
            return null;
        }
        EntityRecord record = recordWithColumns.getRecord();
        Any recordState = record.getState();
        Any maskedState = maskAny(recordState, fieldMask);
        EntityRecord maskedRecord = record.toBuilder()
                                          .setState(maskedState)
                                          .build();
        return maskedRecord;
    }

    private static Any maskAny(Any message, FieldMask mask) {
        Message stateMessage = unpack(message);
        Message maskedMessage = applyMask(mask, stateMessage);
        Any result = pack(maskedMessage);
        return result;
    }

    @Override
    public boolean isEmpty() {
        return activeRecords.isEmpty();
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

        @Override
        public @Nullable EntityRecord apply(@Nullable EntityRecord input) {
            checkNotNull(input);
            Any packedState = input.getState();
            Message state = unpack(packedState);
            Message maskedState = applyMask(fieldMask, state);
            Any repackedState = pack(maskedState);
            EntityRecord result = EntityRecord
                    .newBuilder(input)
                    .setState(repackedState)
                    .build();
            return result;
        }
    }
}
