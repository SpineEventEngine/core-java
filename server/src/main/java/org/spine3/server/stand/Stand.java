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
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Any;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.storage.StandStorage;
import org.spine3.server.storage.memory.InMemoryStandStorage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

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
    private final ConcurrentMap<TypeUrl, Set<StandUpdateCallback>> callbacks = new ConcurrentHashMap<>();
    private final Executor callbackExecutor;

    private Stand(Builder builder) {
        storages = builder.getEnabledStorages();
        callbackExecutor = builder.getCallbackExecutor();
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

        for (StandStorage storage : storages) {
            storage.write(newAggregateState);
        }
        feedToCallbacks(newAggregateState, callbacks, callbackExecutor);
    }

    /**
     * Update the state of an entity inside of the current instance of {@code Stand}.
     *
     * @param entityState the entity state
     */
    public void update(Any entityState) {
        feedToCallbacks(entityState, callbacks, callbackExecutor);
    }

    /**
     * Watch for a change of an entity state with a certain {@link TypeUrl}.
     *
     * <p>Once this instance of {@code Stand} receives an update of an entity with the given {@code TypeUrl},
     * all such callbacks are executed.
     *
     * @param typeUrl  an instance of entity {@link TypeUrl} to watch for changes
     * @param callback an instance of {@link StandUpdateCallback} executed upon entity update.
     */
    public void watch(TypeUrl typeUrl, StandUpdateCallback callback) {
        if (!callbacks.containsKey(typeUrl)) {
            final Set<StandUpdateCallback> emptySet = Collections.synchronizedSet(new HashSet<StandUpdateCallback>());
            callbacks.put(typeUrl, emptySet);
        }

        callbacks.get(typeUrl)
                 .add(callback);
    }

    /**
     * Stop watching for a change of an entity state with a certain {@link TypeUrl}.
     *
     * <p>Typically invoked to cancel the previous {@link #watch(TypeUrl, StandUpdateCallback)} call with the same arguments.
     * <p>If no {@code watch} method was executed for the same {@code TypeUrl} and {@code StandUpdateCallback},
     * then {@code unwatch} has no effect.
     *
     * @param typeUrl  an instance of entity {@link TypeUrl} to stop watch for changes
     * @param callback an instance of {@link StandUpdateCallback} to be cancelled upon entity update.
     */
    public void unwatch(TypeUrl typeUrl, StandUpdateCallback callback) {
        final Set<StandUpdateCallback> registeredCallbacks = callbacks.get(typeUrl);

        if (registeredCallbacks != null && registeredCallbacks.contains(callback)) {
            registeredCallbacks.remove(callback);
        }
    }


    private static void feedToCallbacks(
            final Any entityState,
            final ConcurrentMap<TypeUrl, Set<StandUpdateCallback>> callbacks,
            final Executor callbackExecutor
    ) {
        final String typeUrlString = entityState.getTypeUrl();
        final TypeUrl typeUrl = TypeUrl.of(typeUrlString);

        if (callbacks.containsKey(typeUrl)) {
            for (final StandUpdateCallback callback : callbacks.get(typeUrl)) {

                callbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onEntityStateUpdate(entityState);
                    }
                });
            }
        }
    }

    /**
     * A contract for the callbacks to be executed upon entity state change.
     *
     * @see #watch(TypeUrl, StandUpdateCallback)
     * @see #unwatch(TypeUrl, StandUpdateCallback)
     */
    @SuppressWarnings("InterfaceNeverImplemented")      //it's OK, there may be no callbacks in the codebase
    public interface StandUpdateCallback {

        void onEntityStateUpdate(Any newEntityState);
    }


    public static class Builder {
        private final Set<StandStorage> userProvidedStorages = Sets.newHashSet();
        private ImmutableSet<StandStorage> enabledStorages;
        private Executor callbackExecutor;


        /**
         * Add an instance of {@link StandStorage} to be used to persist the latest an Aggregate states.
         *
         * @param storage an instance of {@code StandStorage}
         * @return this instance of {@code Builder}
         */
        public Builder addStorage(StandStorage storage) {
            userProvidedStorages.add(storage);
            return this;
        }

        public Builder removeStorage(StandStorage storage) {
            userProvidedStorages.remove(storage);
            return this;
        }

        public Executor getCallbackExecutor() {
            return callbackExecutor;
        }

        /**
         * Sets an {@code Executor} to be used for executing callback methods.
         *
         * <p>If the {@code Executor} is not set, {@link MoreExecutors#directExecutor()} will be used.
         *
         * @param callbackExecutor the instance of {@code Executor}
         * @return this instance of {@code Builder}
         */
        public Builder setCallbackExecutor(Executor callbackExecutor) {
            this.callbackExecutor = callbackExecutor;
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
            if (callbackExecutor == null) {
                callbackExecutor = MoreExecutors.directExecutor();
            }

            final Stand result = new Stand(this);
            return result;
        }
    }
}
