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

import com.google.protobuf.FieldMask;
import io.spine.core.BoundedContextName;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.stand.AggregateStateId;
import io.spine.server.stand.StandStorage;
import io.spine.server.storage.RecordReadRequest;
import io.spine.server.storage.RecordStorage;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterators.filter;
import static io.spine.type.TypeUrl.ofEnclosed;

/**
 * In-memory implementation of {@link StandStorage}.
 *
 * <p>Uses a {@link java.util.concurrent.ConcurrentMap ConcurrentMap} for internal storage.
 *
 * @author Alex Tymchenko
 */
class InMemoryStandStorage extends StandStorage {

    private static final String TYPE_URL_MISMATCH_MESSAGE_PATTERN =
            "The typeUrl of the record (%s) does not correspond to id (for type %s)";

    private final InMemoryRecordStorage<AggregateStateId> recordStorage;

    private InMemoryStandStorage(Builder builder) {
        super(builder.isMultitenant());
        /*
            We do not use the StandStorage in a way that requires the `entityStateUrl` parameter
            passed. Therefore simply pass the `TypeUrl` of `EntityRecord`
        */
        recordStorage = new InMemoryRecordStorage<>(
                StorageSpec.of(checkNotNull(builder.boundedContextName),
                               TypeUrl.of(EntityRecord.class),
                               AggregateStateId.class),
                builder.isMultitenant(),
                AbstractVersionableEntity.class);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public Iterator<EntityRecord> readAllByType(TypeUrl type) {
        return readAllByType(type, FieldMask.getDefaultInstance());
    }

    @Override
    public Iterator<EntityRecord> readAllByType(TypeUrl type,
                                                FieldMask fieldMask) {
        Iterator<EntityRecord> allRecords = readAll(fieldMask);
        Iterator<EntityRecord> result = filterByType(allRecords, type);
        return result;
    }

    private static Iterator<EntityRecord> filterByType(Iterator<EntityRecord> records,
                                                       TypeUrl expectedType) {
        Iterator<EntityRecord> result =
                filter(records,
                       entityRecord -> {
                           checkNotNull(entityRecord);
                           TypeUrl actualType = ofEnclosed(entityRecord.getState());
                           return expectedType.equals(actualType);
                       });
        return result;
    }

    @Override
    public Iterator<AggregateStateId> index() {
        return recordStorage().index();
    }

    @Override
    public boolean delete(AggregateStateId id) {
        return recordStorage().delete(id);
    }

    @Override
    protected Optional<EntityRecord> readRecord(AggregateStateId id) {
        RecordReadRequest<AggregateStateId> request = new RecordReadRequest<>(id);
        return recordStorage().read(request);
    }

    @Override
    protected Iterator<EntityRecord> readMultipleRecords(Iterable<AggregateStateId> ids) {
        return recordStorage().readMultiple(ids);
    }

    @Override
    protected Iterator<@Nullable EntityRecord> readMultipleRecords(Iterable<AggregateStateId> ids,
                                                                   FieldMask fieldMask) {
        return recordStorage().readMultiple(ids, fieldMask);
    }

    @Override
    protected Iterator<EntityRecord> readAllRecords() {
        return recordStorage().readAll();
    }

    @Override
    protected Iterator<EntityRecord> readAllRecords(FieldMask fieldMask) {
        return recordStorage().readAll(fieldMask);
    }

    @Override
    protected Iterator<EntityRecord> readAllRecords(EntityQuery<AggregateStateId> query,
                                                    FieldMask fieldMask) {
        return recordStorage().readAll(query, fieldMask);
    }

    @Override
    protected void writeRecord(AggregateStateId id, EntityRecordWithColumns record) {
        TypeUrl recordType = TypeUrl.parse(record.getRecord()
                                                       .getState()
                                                       .getTypeUrl());
        TypeUrl recordTypeFromId = id.getStateType();
        checkState(recordTypeFromId.equals(recordType),
                   TYPE_URL_MISMATCH_MESSAGE_PATTERN,
                   recordType,
                   recordTypeFromId);
        recordStorage().write(id, record);
    }

    @Override
    protected void writeRecords(Map<AggregateStateId, EntityRecordWithColumns> records) {
        for (Map.Entry<AggregateStateId, EntityRecordWithColumns> record : records.entrySet()) {
            writeRecord(record.getKey(), record.getValue());
        }
    }

    @Override
    protected RecordStorage<AggregateStateId> recordStorage() {
        return recordStorage;
    }

    public static class Builder {

        private BoundedContextName boundedContextName;
        private boolean multitenant;

        public boolean isMultitenant() {
            return multitenant;
        }

        public Builder setBoundedContextName(BoundedContextName boundedContextName) {
            this.boundedContextName = boundedContextName;
            return this;
        }

        public Builder setMultitenant(boolean multitenant) {
            this.multitenant = multitenant;
            return this;
        }

        /**
         * Builds an instance of in-memory stand storage.
         *
         * @return an instance of in-memory storage
         */
        public InMemoryStandStorage build() {
            InMemoryStandStorage result = new InMemoryStandStorage(this);
            return result;
        }
    }
}
