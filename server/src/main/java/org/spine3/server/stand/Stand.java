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
package org.spine3.server.stand;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import org.spine3.server.storage.StandStorage;
import org.spine3.server.storage.memory.InMemoryStandStorage;

import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

/**
 * A container for storing the lastest {@link org.spine3.server.aggregate.Aggregate} states.
 *
 * <p>Provides an optimal way to access the latest state of published aggregates for read-side services.
 * The aggregate states are delivered to the instance of {@code Stand} through {@link StandFunnel}
 * from {@link org.spine3.server.aggregate.AggregateRepository} instances.
 *
 * <p>In order to provide a flexibility in defining data access policies, {@code Stand} contains only the states
 * of published aggregates. Please refer to {@link org.spine3.server.aggregate.Aggregate} for publication description.
 *
 * <p>Each {@link org.spine3.server.BoundedContext} contains the only instance of {@code Stand}.
 *
 * @author Alex Tymchenko
 */
public class Stand {

    private final ImmutableSet<StandStorage> storages;

    private Stand(Builder builder) {
        storages = builder.getEnabledStorages();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Store the new value for the {@link org.spine3.server.aggregate.Aggregate} to each of the configured
     * instances of {@link StandStorage}.
     *
     * <p>Each state value is stored as one-to-one to its {@link org.spine3.protobuf.TypeUrl} obtained
     * via {@link Any#getTypeUrl()}.
     *
     * <p>In case {@code Stand} already contains the state for this {@code Aggregate}, the value will be replaced.
     *
     * <p>The state of an {@code Aggregate} must not be null.
     *
     * @param newAggregateState the state of an {@link org.spine3.server.aggregate.Aggregate} put into Stand
     */
    public void updateAggregateState(Any newAggregateState) {
        // TODO[alex.tymchenko]: should we also check the given Aggregate is allowed to be published? Or is it an AggregateRepository responsibility?
        checkState(newAggregateState != null, "newAggregateState must not be null");

        for (StandStorage storage : storages) {
            storage.write(newAggregateState);
        }
    }

    public static class Builder {
        private final Set<StandStorage> userProvidedStorages = Sets.newHashSet();
        private ImmutableSet<StandStorage> enabledStorages;


        public Builder addStorage(StandStorage storage) {
            userProvidedStorages.add(storage);
            return this;
        }

        public Builder removeStorage(StandStorage storage) {
            userProvidedStorages.remove(storage);
            return this;
        }

        @SuppressWarnings("ReturnOfCollectionOrArrayField") // the collection is immutable
        public ImmutableSet<StandStorage> getEnabledStorages() {
            return enabledStorages;
        }


        private ImmutableSet<StandStorage> composeEnabledStorages() {
            final ImmutableSet.Builder<StandStorage> builder = ImmutableSet.builder();
            if (userProvidedStorages.isEmpty()) {
                final InMemoryStandStorage inMemoryStandStorage = InMemoryStandStorage.newBuilder()
                                                                                      .build();
                builder.add(inMemoryStandStorage);
            }
            builder.addAll(userProvidedStorages);
            return builder.build();
        }


        /**
         * Build an instance of {@code Stand}
         *
         * @return the instance of Stand
         */
        public Stand build() {
            this.enabledStorages = composeEnabledStorages();
            final Stand result = new Stand(this);
            return result;
        }
    }
}
