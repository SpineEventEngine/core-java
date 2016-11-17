/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.protobuf.FieldMask;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.stand.AggregateStateId;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.storage.StandStorage;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * In-memory implementation of {@link StandStorage}.
 *
 * <p>Uses a {@link ConcurrentMap} for internal storage.
 *
 * @author Alex Tymchenko
 */
public class InMemoryStandStorage extends StandStorage {

    private final InMemoryRecordStorage<AggregateStateId> recordStorage;

    private InMemoryStandStorage(Builder builder) {
        super(builder.isMultitenant());
        recordStorage = new InMemoryRecordStorage<>(builder.isMultitenant());
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public ImmutableCollection<EntityStorageRecord> readAllByType(final TypeUrl type) {
        return readAllByType(type, FieldMask.getDefaultInstance());
    }

    @Override
    public ImmutableCollection<EntityStorageRecord> readAllByType(final TypeUrl type, FieldMask fieldMask) {
        final Map<AggregateStateId, EntityStorageRecord> allRecords = readAll(fieldMask);
        final Map<AggregateStateId, EntityStorageRecord> resultMap = Maps.filterKeys(allRecords, new Predicate<AggregateStateId>() {
            @Override
            public boolean apply(@Nullable AggregateStateId stateId) {
                checkNotNull(stateId);
                final boolean typeMatches = stateId.getStateType()
                                                   .equals(type);
                return typeMatches;
            }
        });

        final ImmutableList<EntityStorageRecord> result = ImmutableList.copyOf(resultMap.values());
        return result;
    }

    @Nullable
    @Override
    protected EntityStorageRecord readRecord(AggregateStateId id) {
        final EntityStorageRecord result = recordStorage.read(id);
        return result;
    }

    @Override
    protected Iterable<EntityStorageRecord> readMultipleRecords(Iterable<AggregateStateId> ids) {
        final Iterable<EntityStorageRecord> result = recordStorage.readMultiple(ids);
        return result;
    }

    @Override
    protected Iterable<EntityStorageRecord> readMultipleRecords(Iterable<AggregateStateId> ids, FieldMask fieldMask) {
        final Iterable<EntityStorageRecord> result = recordStorage.readMultiple(ids, fieldMask);
        return result;
    }

    @Override
    protected Map<AggregateStateId, EntityStorageRecord> readAllRecords() {
        final Map<AggregateStateId, EntityStorageRecord> result = recordStorage.readAll();
        return result;
    }

    @Override
    protected Map<AggregateStateId, EntityStorageRecord> readAllRecords(FieldMask fieldMask) {
        final Map<AggregateStateId, EntityStorageRecord> result = recordStorage.readAll(fieldMask);
        return result;
    }

    @Override
    protected void writeRecord(AggregateStateId id, EntityStorageRecord record) {
        final TypeUrl recordType = TypeUrl.of(record.getState()
                                                    .getTypeUrl());
        final TypeUrl recordTypeFromId = id.getStateType();
        checkState(
                recordTypeFromId.equals(recordType),
                String.format("The typeUrl of the record (%s) does not correspond to id (for type %s)",
                              recordType, recordTypeFromId));

        recordStorage.write(id, record);
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
