/*
 *
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
 *
 */
package org.spine3.server.storage.memory;

import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.stand.AggregateStateId;
import org.spine3.server.storage.StandStorage;

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

    private final ConcurrentMap<AggregateStateId, Any> aggregateStates;

    private InMemoryStandStorage(Builder builder) {
        super(builder.isMultitenant());
        aggregateStates = builder.getSeedDataMap();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public Any read(AggregateStateId id) {
        final Any result = aggregateStates.get(id);
        return result;
    }

    @Override
    public void write(AggregateStateId id, Any record) {
        final TypeUrl recordType = TypeUrl.of(record.getTypeUrl());
        checkState(id.getStateType() == recordType, "The typeUrl of the record does not correspond to id");

        doPut(aggregateStates, id, record);
    }

    public static class Builder {

        private final Map<Object, Any> seedData = Maps.newHashMap();
        private ConcurrentMap<AggregateStateId, Any> seedDataMap;
        private boolean multitenant;

        /**
         * Add a seed data item.
         *
         * <p>Use this method to pre-fill the {@link StandStorage}.
         *
         * <p>The argument value must not be null.
         *
         * @param id          the id of state object
         * @param stateObject the state object to be added
         * @return {@code this} instance of {@code Builder}
         */
        public Builder addStateObject(Object id, Any stateObject) {
            checkNotNull(stateObject, "stateObject must not be null");
            checkNotNull(id, "id must not be null");

            seedData.put(id, stateObject);
            return this;
        }

        /**
         * Remove a previously added seed data item.
         *
         * <p>The argument value must not be null.
         *
         * @param id the ID of state object to be removed
         * @return {@code this} instance of {@code Builder}
         */
        public Builder removeStateObject(Object id) {
            checkNotNull(id, "Cannot remove the object with null");
            seedData.remove(id);
            return this;
        }

        private ConcurrentMap<AggregateStateId, Any> buildSeedDataMap() {
            final ConcurrentMap<AggregateStateId, Any> stateMap = new MapMaker().initialCapacity(seedData.size())
                                                                                .makeMap();
            for (Object id : seedData.keySet()) {
                final Any state = seedData.get(id);
                doPut(stateMap, id, state);
            }
            return stateMap;
        }

        /**
         * Do not expose the contents to public, as the returned {@code ConcurrentMap} must be mutable.
         */
        private ConcurrentMap<AggregateStateId, Any> getSeedDataMap() {
            return seedDataMap;
        }

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
            this.seedDataMap = buildSeedDataMap();
            final InMemoryStandStorage result = new InMemoryStandStorage(this);
            return result;
        }

    }

    private static void doPut(final ConcurrentMap<AggregateStateId, Any> targetMap, final Object id, final Any any) {
        final String typeUrlString = any.getTypeUrl();
        final TypeUrl typeUrl = TypeUrl.of(typeUrlString);
        final AggregateStateId statId = AggregateStateId.of(id, typeUrl);
        targetMap.put(statId, any);
    }

}
