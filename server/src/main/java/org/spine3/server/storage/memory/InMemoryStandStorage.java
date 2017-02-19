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
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.protobuf.FieldMask;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.stand.AggregateStateId;
import org.spine3.server.stand.StandStorage;
import org.spine3.server.storage.EntityRecord;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * In-memory implementation of {@link StandStorage}.
 *
 * <p>Uses a {@link java.util.concurrent.ConcurrentMap ConcurrentMap} for internal storage.
 *
 * @author Alex Tymchenko
 */
class InMemoryStandStorage extends StandStorage {

    private static final String TYPE_URL_MISMATCH_MESSAGE_PATTERN
            = "The typeUrl of the record (%s) does not correspond to id (for type %s)";

    private final InMemoryRecordStorage<AggregateStateId> recordStorage;

    private InMemoryStandStorage(Builder builder) {
        super(builder.isMultitenant());
        recordStorage = new InMemoryRecordStorage<>(builder.isMultitenant());
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public ImmutableCollection<EntityRecord> readAllByType(final TypeUrl type) {
        return readAllByType(type, FieldMask.getDefaultInstance());
    }

    @Override
    public ImmutableCollection<EntityRecord> readAllByType(final TypeUrl type, FieldMask fieldMask) {
        final Map<AggregateStateId, EntityRecord> allRecords = readAll(fieldMask);
        final Map<AggregateStateId, EntityRecord> resultMap = Maps.filterKeys(allRecords, new Predicate<AggregateStateId>() {
            @Override
            public boolean apply(@Nullable AggregateStateId stateId) {
                checkNotNull(stateId);
                final boolean typeMatches = stateId.getStateType()
                                                   .equals(type);
                return typeMatches;
            }
        });

        final ImmutableList<EntityRecord> result = ImmutableList.copyOf(resultMap.values());
        return result;
    }

    @Override
    public boolean markArchived(AggregateStateId id) {
        return recordStorage.markArchived(id);
    }

    @Override
    public boolean markDeleted(AggregateStateId id) {
        return recordStorage.markDeleted(id);
    }

    @Override
    public boolean delete(AggregateStateId id) {
        return recordStorage.delete(id);
    }

    @Nullable
    @Override
    protected Optional<EntityRecord> readRecord(AggregateStateId id) {
        return recordStorage.read(id);
    }

    @Override
    protected Iterable<EntityRecord> readMultipleRecords(Iterable<AggregateStateId> ids) {
        return recordStorage.readMultiple(ids);
    }

    @Override
    protected Iterable<EntityRecord> readMultipleRecords(Iterable<AggregateStateId> ids, FieldMask fieldMask) {
        return recordStorage.readMultiple(ids, fieldMask);
    }

    @Override
    protected Map<AggregateStateId, EntityRecord> readAllRecords() {
        return recordStorage.readAll();
    }

    @Override
    protected Map<AggregateStateId, EntityRecord> readAllRecords(FieldMask fieldMask) {
        return recordStorage.readAll(fieldMask);
    }

    @Override
    protected void writeRecord(AggregateStateId id, EntityRecord record) {
        final TypeUrl recordType = TypeUrl.of(record.getState()
                                                    .getTypeUrl());
        final TypeUrl recordTypeFromId = id.getStateType();
        checkState(
                recordTypeFromId.equals(recordType),
                String.format(TYPE_URL_MISMATCH_MESSAGE_PATTERN, recordType, recordTypeFromId));

        recordStorage.write(id, record);
    }

    @Override
    protected void writeRecords(Map<AggregateStateId, EntityRecord> records) {
        for (Map.Entry<AggregateStateId, EntityRecord> record : records.entrySet()) {
            writeRecord(record.getKey(), record.getValue());
        }
    }

    public static class Builder {

        private boolean multitenant;

        public boolean isMultitenant() {
            return multitenant;
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
            final InMemoryStandStorage result = new InMemoryStandStorage(this);
            return result;
        }
    }
}
