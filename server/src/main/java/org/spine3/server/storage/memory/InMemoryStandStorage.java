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
import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.storage.StandStorage;

import java.util.Set;
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

    private final ConcurrentMap<TypeUrl, Any> aggregateStates;

    private InMemoryStandStorage(Builder builder) {
        super(builder.isMultitenant());
        aggregateStates = builder.getSeedDataMap();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public Any read(TypeUrl id) {
        final Any result = aggregateStates.get(id);
        return result;
    }

    @Override
    public void write(TypeUrl id, Any record) {
        final TypeUrl recordType = TypeUrl.of(record.getTypeUrl());
        checkState(id == recordType, "The typeUrl of the record does not correspond to id");

        doPut(aggregateStates, record);
    }

    @Override
    public void write(Any aggregateState) {
        doPut(aggregateStates, aggregateState);
    }

    public static class Builder {

        private final Set<Any> seedData = Sets.newHashSet();
        private ConcurrentMap<TypeUrl, Any> seedDataMap;
        private boolean multitenant;

        /**
         * Add a seed data item.
         *
         * <p>Use this method to pre-fill the {@link StandStorage}.
         *
         * <p>The argument value must not be null.
         *
         * @param stateObject the state object to be added
         * @return {@code this} instance of {@code Builder}
         */
        public Builder addStateObject(Any stateObject) {
            checkNotNull(stateObject, "stateObject must not be null");
            seedData.add(stateObject);
            return this;
        }

        /**
         * Remove a previously added seed data item.
         *
         * <p>The argument value must not be null.
         *
         * @param stateObject the state object to be removed
         * @return {@code this} instance of {@code Builder}
         */
        public Builder removeStateObject(Any stateObject) {
            checkNotNull(stateObject, "Cannot remove null stateObject");
            seedData.remove(stateObject);
            return this;
        }

        private ConcurrentMap<TypeUrl, Any> buildSeedDataMap() {
            final ConcurrentMap<TypeUrl, Any> stateMap = new MapMaker().initialCapacity(seedData.size())
                                                                       .makeMap();
            for (final Any any : seedData) {
                doPut(stateMap, any);
            }
            return stateMap;
        }

        /**
         * Do not expose the contents to public, as the returned {@code ConcurrentMap} must be mutable.
         */
        private ConcurrentMap<TypeUrl, Any> getSeedDataMap() {
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

    private static void doPut(final ConcurrentMap<TypeUrl, Any> targetMap, final Any any) {
        final String typeUrlString = any.getTypeUrl();
        final TypeUrl typeUrl = TypeUrl.of(typeUrlString);
        targetMap.put(typeUrl, any);
    }

}
