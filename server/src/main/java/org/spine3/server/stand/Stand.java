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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.spine3.base.Responses;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.client.EntityIdFilter;
import org.spine3.client.Query;
import org.spine3.client.QueryResponse;
import org.spine3.client.Target;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.KnownTypes;
import org.spine3.protobuf.Timestamps;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.EntityRepository;
import org.spine3.server.entity.Repository;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.storage.StandStorage;
import org.spine3.server.storage.memory.InMemoryStandStorage;
import org.spine3.type.ClassName;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;

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

    /**
     * Persistent storage for the latest {@link org.spine3.server.aggregate.Aggregate} states.
     *
     * <p>Any {@code Aggregate} state delivered to this instance of {@code Stand} is persisted to this storage.
     */
    private final StandStorage storage;

    /**
     * A set of callbacks to be executed upon the incoming updates.
     *
     * <p>Each callback is triggerred if the entity with a matching {@code TypeUrl} is delivered to this {@code Stand}.
     * <p>There may be any number of callbacks for a given {@code TypeUrl}.
     */
    private final ConcurrentMap<TypeUrl, Set<StandUpdateCallback>> callbacks = new ConcurrentHashMap<>();

    /** An instance of executor used to invoke callbacks */
    private final Executor callbackExecutor;

    /** The mapping between {@code TypeUrl} instances and repositories providing the entities of this type */
    private final ConcurrentMap<TypeUrl, EntityRepository<?, ? extends Entity, ? extends Message>> typeToRepositoryMap = new ConcurrentHashMap<>();

    /**
     * Store the known {@link org.spine3.server.aggregate.Aggregate} types in order to distinguish them among all
     * instances of {@code TypeUrl}.
     *
     * <p>Once this instance of {@code Stand} receives an update as {@link Any}, the {@code Aggregate} states
     * are persisted for further usage. While the rest of entity updates are not; they are only propagated to
     * the registered callbacks.
     */
    private final Set<TypeUrl> knownAggregateTypes = Sets.newConcurrentHashSet();

    private Stand(Builder builder) {
        storage = builder.getStorage();
        callbackExecutor = builder.getCallbackExecutor();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Update the state of an entity inside of the current instance of {@code Stand}.
     *
     * <p>In case the entity update represents the new {@link org.spine3.server.aggregate.Aggregate} state,
     * store the new value for the {@code Aggregate} to each of the configured instances of {@link StandStorage}.
     *
     * <p>Each {@code Aggregate } state value is stored as one-to-one to its {@link org.spine3.protobuf.TypeUrl} obtained
     * via {@link Any#getTypeUrl()}.
     *
     * <p>In case {@code Stand} already contains the state for this {@code Aggregate}, the value will be replaced.
     *
     * <p>The state updates which are not originated from the {@code Aggregate} are not stored in the {@code Stand}.
     *
     * <p>In any case, the state update is then propagated to the callbacks. The set of matched callbacks
     * is determined by filtering all the registered callbacks by the entity {@code TypeUrl}.
     *
     * <p>The matching callbacks are executed with the {@link #callbackExecutor}.
     *
     * @param entityState the entity state
     */
    @SuppressWarnings("MethodWithMultipleLoops")    /* It's fine, since the second loop is most likely
                                                     * executed in async fashion. */
    public void update(final Object id, final Any entityState) {
        final String typeUrlString = entityState.getTypeUrl();
        final TypeUrl typeUrl = TypeUrl.of(typeUrlString);

        final boolean isAggregateUpdate = knownAggregateTypes.contains(typeUrl);

        if (isAggregateUpdate) {
            final AggregateStateId aggregateStateId = AggregateStateId.of(id, typeUrl);

            final EntityStorageRecord record = EntityStorageRecord.newBuilder()
                                                                  .setState(entityState)
                                                                  .setWhenModified(Timestamps.getCurrentTime())
                                                                  .build();
            storage.write(aggregateStateId, record);
        }

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

    /**
     * Read all {@link Entity} types exposed for reading by this instance of {@code Stand}.
     *
     * <p>The result includes all values from {@link #getKnownAggregateTypes()} as well.
     *
     * @return the set of types as {@link TypeUrl} instances
     */
    @CheckReturnValue
    public ImmutableSet<TypeUrl> getAvailableTypes() {
        final ImmutableSet.Builder<TypeUrl> resultBuilder = ImmutableSet.builder();
        final Set<TypeUrl> projectionTypes = typeToRepositoryMap.keySet();
        resultBuilder.addAll(projectionTypes)
                     .addAll(knownAggregateTypes);
        final ImmutableSet<TypeUrl> result = resultBuilder.build();
        return result;
    }

    /**
     * Read all {@link org.spine3.server.aggregate.Aggregate} entity types exposed for reading
     * by this instance of {@code Stand}.
     *
     * @return the set of types as {@link TypeUrl} instances
     */
    @CheckReturnValue
    public ImmutableSet<TypeUrl> getKnownAggregateTypes() {
        final ImmutableSet<TypeUrl> result = ImmutableSet.copyOf(knownAggregateTypes);
        return result;
    }

    /**
     * Read a particular set of items from the read-side of the application and feed the result into an instance
     *
     * <p>{@link Query} defines the query target and the expected detail level for response.
     *
     * <p>The query results are fed to an instance of {@link StreamObserver<QueryResponse>}.
     *
     * @param query            an instance of query
     * @param responseObserver an observer to feed the query results to.
     */
    public void execute(Query query, StreamObserver<QueryResponse> responseObserver) {
        final ImmutableCollection<Any> readResult = internalExecute(query);
        final QueryResponse response = QueryResponse.newBuilder()
                                                    .addAllMessages(readResult)
                                                    .setResponse(Responses.ok())
                                                    .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private ImmutableCollection<Any> internalExecute(Query query) {

        final ImmutableSet.Builder<Any> resultBuilder = ImmutableSet.builder();

        final Target target = query.getTarget();

        final String type = target.getType();
        final TypeUrl typeUrl = KnownTypes.getTypeUrl(type);
        final EntityRepository<?, ? extends Entity, ? extends Message> repository = typeToRepositoryMap.get(typeUrl);

        if (repository != null) {

            // the target references an entity state
            ImmutableCollection<? extends Entity> entities = fetchFromEntityRepository(target, repository);

            feedEntitiesToBuilder(resultBuilder, entities);
        } else if (knownAggregateTypes.contains(typeUrl)) {

            // the target relates to an {@code Aggregate} state
            ImmutableCollection<EntityStorageRecord> stateRecords = fetchFromStandStorage(target, typeUrl);

            feedStateRecordsToBuilder(resultBuilder, stateRecords);
        }

        final ImmutableSet<Any> result = resultBuilder.build();

        return result;
    }

    private ImmutableCollection<EntityStorageRecord> fetchFromStandStorage(Target target, final TypeUrl typeUrl) {
        final ImmutableCollection<EntityStorageRecord> result;

        if (target.getIncludeAll()) {
            result = storage.readAllByType(typeUrl);

        } else {
            final EntityFilters filters = target.getFilters();
            if (filters != null && filters.getIdFilter() != null) {
                final EntityIdFilter idFilter = filters.getIdFilter();
                final Collection<AggregateStateId> stateIds = Collections2.transform(idFilter.getIdsList(), aggregateStateIdTransformer(typeUrl));

                final Iterable<EntityStorageRecord> bulkReadResults = storage.readBulk(stateIds);
                result = FluentIterable.from(bulkReadResults)
                                       .filter(new Predicate<EntityStorageRecord>() {
                                           @Override
                                           public boolean apply(@Nullable EntityStorageRecord input) {
                                               return input != null;
                                           }
                                       })
                                       .toList();
            } else {
                result = ImmutableList.of();
            }

        }
        return result;
    }

    private static Function<EntityId, AggregateStateId> aggregateStateIdTransformer(final TypeUrl typeUrl) {
        return new Function<EntityId, AggregateStateId>() {
            @Nullable
            @Override
            public AggregateStateId apply(@Nullable EntityId input) {
                checkNotNull(input);

                final AggregateStateId stateId = AggregateStateId.of(input.getId(), typeUrl);
                return stateId;
            }
        };
    }

    private static ImmutableCollection<? extends Entity> fetchFromEntityRepository(Target target, EntityRepository<?, ? extends Entity, ?> repository) {
        final ImmutableCollection<? extends Entity> result;
        if (target.getIncludeAll()) {
            result = repository.findAll();
        } else {
            final EntityFilters filters = target.getFilters();
            result = repository.findAll(filters);
        }
        return result;
    }

    private static void feedEntitiesToBuilder(ImmutableSet.Builder<Any> resultBuilder, ImmutableCollection<? extends Entity> all) {
        for (Entity record : all) {
            final Message state = record.getState();
            final Any packedState = AnyPacker.pack(state);
            resultBuilder.add(packedState);
        }
    }

    private static void feedStateRecordsToBuilder(ImmutableSet.Builder<Any> resultBuilder, ImmutableCollection<EntityStorageRecord> all) {
        for (EntityStorageRecord record : all) {
            final Message state = record.getState();
            final Any packedState = AnyPacker.pack(state);
            resultBuilder.add(packedState);
        }
    }


    /**
     * Register a supplier for the objects of a certain {@link TypeUrl} to be able
     * to read them in response to a {@link org.spine3.client.Query}.
     *
     * <p>In case the supplier is an instance of {@link AggregateRepository}, the {@code Repository} is not registered
     * as type supplier, since the {@code Aggregate} reads are performed by accessing
     * the latest state in the supplied {@code StandStorage}.
     *
     * <p>However, the type of the {@code AggregateRepository} instance is recorded for the postponed processing
     * of updates.
     *
     * @see #update(Object, Any)
     */
    @SuppressWarnings("ChainOfInstanceofChecks")
    public <I, E extends Entity<I, ?>> void registerTypeSupplier(Repository<I, E> repository) {
        final TypeUrl entityType = repository.getEntityStateType();

        if (repository instanceof EntityRepository) {
            typeToRepositoryMap.put(entityType, (EntityRepository<I, E, ? extends Message>) repository);
        }
        if (repository instanceof AggregateRepository) {
            knownAggregateTypes.add(entityType);
        }

    }

    // TODO[alex.tymchenko]: perhaps, we need to close Stand instead of doing this upon repository shutdown (see usages).
    public void deregisterSupplierForType(TypeUrl typeUrl) {
        typeToRepositoryMap.remove(typeUrl);
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
        private StandStorage storage;
        private Executor callbackExecutor;


        /**
         * Set an instance of {@link StandStorage} to be used to persist the latest an Aggregate states.
         *
         * <p>If no {@code storage} is assigned, the {@link InMemoryStandStorage} is be set by default.
         *
         * @param storage an instance of {@code StandStorage}
         * @return this instance of {@code Builder}
         */
        public Builder setStorage(StandStorage storage) {
            this.storage = storage;
            return this;
        }


        public Executor getCallbackExecutor() {
            return callbackExecutor;
        }

        /**
         * Set an {@code Executor} to be used for executing callback methods.
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

        public StandStorage getStorage() {
            return storage;
        }


        /**
         * Build an instance of {@code Stand}.
         *
         * @return the instance of Stand
         */
        public Stand build() {

            if (storage == null) {
                storage = InMemoryStandStorage.newBuilder()
                                              .build();
            }

            if (callbackExecutor == null) {
                callbackExecutor = MoreExecutors.directExecutor();
            }

            final Stand result = new Stand(this);
            return result;
        }
    }
}
