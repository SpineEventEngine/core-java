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
package org.spine3.server.stand;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.spine3.base.Queries;
import org.spine3.base.Responses;
import org.spine3.base.Version;
import org.spine3.client.Query;
import org.spine3.client.QueryResponse;
import org.spine3.client.Subscription;
import org.spine3.client.Target;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.entity.AbstractVersionableEntity;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.RecordBasedRepository;
import org.spine3.server.entity.Repository;
import org.spine3.server.storage.memory.InMemoryStorageFactory;

import javax.annotation.CheckReturnValue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A container for storing the latest {@link org.spine3.server.aggregate.Aggregate Aggregate}
 * states.
 *
 * <p>Provides an optimal way to access the latest state of published aggregates
 * for read-side services. The aggregate states are delivered to the instance of {@code Stand}
 * through {@link StandFunnel} from {@link AggregateRepository} instances.
 *
 * <p>In order to provide a flexibility in defining data access policies,
 * {@code Stand} contains only the states of published aggregates.
 * Please refer to {@link org.spine3.server.aggregate.Aggregate Aggregate} for details on
 * publishing aggregates.
 *
 * <p>Each {@link org.spine3.server.BoundedContext BoundedContext} contains only one
 * instance of {@code Stand}.
 *
 * @author Alex Tymchenko
 */
public class Stand implements AutoCloseable {

    /**
     * Persistent storage for the latest {@code Aggregate} states.
     *
     * <p>Any {@code Aggregate} state delivered to this instance of {@code Stand} is
     * persisted to this storage.
     */
    private final StandStorage storage;

    /**
     * Manages the subscriptions for this instance of {@code Stand}.
     */
    private final SubscriptionRegistry subscriptionRegistry = new SubscriptionRegistry();

    /**
     * An instance of executor used to invoke callbacks
     */
    private final Executor callbackExecutor;

    /**
     * The mapping between {@code TypeUrl} instances and repositories providing
     * the entities of this type.
     */
    private final ConcurrentMap<TypeUrl,
            RecordBasedRepository<?, ? extends Entity, ? extends Message>> typeToRepositoryMap =
            new ConcurrentHashMap<>();

    /**
     * Stores  known {@code Aggregate} types in order to distinguish
     * them among all instances of {@code TypeUrl}.
     *
     * <p>Once this instance of {@code Stand} receives an update as {@link Any},
     * the {@code Aggregate} states are persisted for further usage.
     * The entities that are not {@code Aggregate} are not persisted,
     * and only propagated to the registered callbacks.
     */
    private final Set<TypeUrl> knownAggregateTypes = Sets.newConcurrentHashSet();

    /**
     * Used to return an empty result collection for {@link Query}.
     */
    private static final QueryProcessor NOOP_PROCESSOR = new NoopQueryProcessor();

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
     * <p>In case the entity update represents the new
     * {@link org.spine3.server.aggregate.Aggregate} Aggregate state,
     * store the new value for the {@code Aggregate} to each of the configured instances of
     * {@link StandStorage}.
     *
     * <p>Each {@code Aggregate} state value is stored as one-to-one to its
     * {@link org.spine3.protobuf.TypeUrl TypeUrl} obtained via {@link Any#getTypeUrl()}.
     *
     * <p>In case {@code Stand} already contains the state for this {@code Aggregate},
     * the value will be replaced.
     *
     * <p>The state updates which are not originated from the {@code Aggregate} are not
     * stored in the {@code Stand}.
     *
     * <p>In any case, the state update is then propagated to the callbacks.
     * The set of matched callbacks is determined by filtering all the registered callbacks
     * by the entity {@code TypeUrl}.
     *
     * <p>The matching callbacks are executed with the {@link #callbackExecutor}.
     *  @param id            the entity identifier
     * @param entityState   the entity state
     * @param entityVersion the version of the entity
     */
    void update(Object id, Any entityState, Version entityVersion) {
        final String typeUrlString = entityState.getTypeUrl();
        final TypeUrl typeUrl = TypeUrl.of(typeUrlString);

        final boolean isAggregateUpdate = knownAggregateTypes.contains(typeUrl);

        if (isAggregateUpdate) {
            final AggregateStateId aggregateStateId = AggregateStateId.of(id, typeUrl);

            final EntityRecord record =
                    EntityRecord.newBuilder()
                                .setState(entityState)
                                .setVersion(entityVersion)
                                .build();
            storage.write(aggregateStateId, record);
        }

        notifyMatchingSubscriptions(id, entityState, typeUrl);
    }

    /**
     * Subscribe for all further changes of an entity state, which satisfies the {@link Target}.
     *
     * <p>Once this instance of {@code Stand} receives an update of an entity
     * with the given {@code TypeUrl}, all such callbacks are executed.
     *
     * @param target an instance {@link Target}, defining the entity and criteria,
     *               which changes should be propagated to the {@code callback}
     */
    @CheckReturnValue
    public Subscription subscribe(Target target) {
        final Subscription subscription = subscriptionRegistry.addSubscription(target);
        return subscription;
    }

    /**
     * Activate the subscription created via {@link #subscribe(Target)}.
     *
     * <p>After the activation, the clients will start receiving the updates via
     * {@code EntityUpdateCallback} upon the changes in the entities, defined by
     * the {@code Target} attribute used for this subscription.
     *
     * @param subscription the subscription to activate.
     * @param callback     an instance of {@link EntityUpdateCallback} executed upon entity update.
     * @see #subscribe(Target)
     */
    public void activate(Subscription subscription, EntityUpdateCallback callback) {
        subscriptionRegistry.activate(subscription, callback);
    }

    /**
     * Cancel the {@link Subscription}.
     *
     * <p>Typically invoked to cancel the previous
     * {@link #activate(Subscription, EntityUpdateCallback) activate()} call.
     *
     * <p>After this method is called, the subscribers stop receiving the updates,
     * related to the given {@code Subscription}.
     *
     * @param subscription a subscription to cancel.
     */
    public void cancel(Subscription subscription) {
        subscriptionRegistry.removeSubscription(subscription);
    }

    /**
     * Read all {@link Entity} types exposed for reading by this instance of {@code Stand}.
     *
     * <p>Use {@link Stand#registerTypeSupplier(Repository)} to expose a type.
     *
     * <p>The result includes all values from {@link #getExposedAggregateTypes()} as well.
     *
     * @return the set of types as {@link TypeUrl} instances
     */
    @CheckReturnValue
    public ImmutableSet<TypeUrl> getExposedTypes() {
        final ImmutableSet.Builder<TypeUrl> resultBuilder = ImmutableSet.builder();
        final Set<TypeUrl> projectionTypes = typeToRepositoryMap.keySet();
        resultBuilder.addAll(projectionTypes)
                     .addAll(knownAggregateTypes);
        final ImmutableSet<TypeUrl> result = resultBuilder.build();
        return result;
    }

    /**
     * Read all {@link org.spine3.server.aggregate.Aggregate Aggregate} entity types
     * exposed for reading by this instance of {@code Stand}.
     *
     * <p>Use {@link Stand#registerTypeSupplier(Repository)} to expose an {@code Aggregate} type.
     *
     * @return the set of types as {@link TypeUrl} instances
     */
    @CheckReturnValue
    public ImmutableSet<TypeUrl> getExposedAggregateTypes() {
        final ImmutableSet<TypeUrl> result = ImmutableSet.copyOf(knownAggregateTypes);
        return result;
    }

    /**
     * Read a particular set of items from the read-side of the application and
     * feed the result into an instance.
     *
     * <p>{@link Query} defines the query target and the expected detail level for response.
     *
     * <p>The query results are fed to an instance of {@link StreamObserver<QueryResponse>}.
     *
     * @param query            an instance of query
     * @param responseObserver an observer to feed the query results to.
     */
    public void execute(Query query, StreamObserver<QueryResponse> responseObserver) {

        final TypeUrl type = Queries.typeOf(query);
        checkNotNull(type, "Query target type unknown");
        final QueryProcessor queryProcessor = processorFor(type);

        final ImmutableCollection<Any> readResult = queryProcessor.process(query);
        final QueryResponse response = QueryResponse.newBuilder()
                                                    .addAllMessages(readResult)
                                                    .setResponse(Responses.ok())
                                                    .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void notifyMatchingSubscriptions(Object id, final Any entityState, TypeUrl typeUrl) {
        if (subscriptionRegistry.hasType(typeUrl)) {
            final Set<SubscriptionRecord> allRecords = subscriptionRegistry.byType(typeUrl);

            for (final SubscriptionRecord subscriptionRecord : allRecords) {

                final boolean subscriptionIsActive = subscriptionRecord.isActive();
                final boolean stateMatches = subscriptionRecord.matches(typeUrl, id, entityState);
                if (subscriptionIsActive && stateMatches) {
                    callbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            subscriptionRecord.getCallback()
                                              .onStateChanged(entityState);
                        }
                    });
                }
            }
        }
    }

    /**
     * Register a supplier for the objects of a certain {@link TypeUrl} to be able
     * to read them in response to a {@link org.spine3.client.Query Query}.
     *
     * <p>In case the supplier is an instance of {@link AggregateRepository}, the {@code Repository}
     * is not registered as type supplier, since the {@code Aggregate} reads are performed
     * by accessing the latest state in the supplied {@code StandStorage}.
     *
     * <p>However, the type of the {@code AggregateRepository} instance is recorded for
     * the postponed processing of updates.
     *
     * @see #update(Object, Any, Version)
     */
    @SuppressWarnings("ChainOfInstanceofChecks")
    public <I, E extends AbstractVersionableEntity<I, ?>>
           void registerTypeSupplier(Repository<I, E> repository) {
        final TypeUrl entityType = repository.getEntityStateType();

        if (repository instanceof RecordBasedRepository) {
            typeToRepositoryMap.put(entityType,
                                    (RecordBasedRepository<I, E, ? extends Message>) repository);
        }
        if (repository instanceof AggregateRepository) {
            knownAggregateTypes.add(entityType);
        }
    }

    /**
     * Dumps all {@link TypeUrl}-to-{@link RecordBasedRepository} relations.
     */
    @Override
    public void close() throws Exception {
        typeToRepositoryMap.clear();
        storage.close();
    }

    /**
     * A contract for the callbacks to be executed upon entity state change.
     *
     * @see #activate(Subscription, EntityUpdateCallback)
     * @see #cancel(Subscription)
     */
    public interface EntityUpdateCallback {

        /**
         * Called when a certain entity state is updated.
         *
         * @param newEntityState new state of the entity
         */
        void onStateChanged(Any newEntityState);
    }

    /**
     * Factory method which determines a proper {@link QueryProcessor} implementation
     * depending on {@link TypeUrl} of the incoming {@link Query#getTarget()}.
     *
     * <p>As {@code Stand} accumulates the read-side updates from various repositories,
     * the {@code Query} processing varies a lot. The target type of the incoming {@code Query}
     * tells the {@code Stand} about the essence of the object queried. Thus making it possible
     * to pick a proper strategy for data fetch.
     *
     * @param type the target type of the {@code Query}
     * @return suitable implementation of {@code QueryProcessor}
     */
    private QueryProcessor processorFor(TypeUrl type) {
        final QueryProcessor result;

        final RecordBasedRepository<?, ? extends Entity, ? extends Message> repository =
                typeToRepositoryMap.get(type);

        if (repository != null) {

            // The query target is an {@code Entity}.
            result = new EntityQueryProcessor(repository);
        } else if (getExposedAggregateTypes().contains(type)) {

            // The query target is an {@code Aggregate} state.
            result = new AggregateQueryProcessor(storage, type);
        } else {

            // This type points to an objects, that are not exposed via the current
            // instance of {@code Stand}.
            result = NOOP_PROCESSOR;
        }
        return result;
    }

    public static class Builder {
        private StandStorage storage;
        private Executor callbackExecutor;

        /**
         * Set an instance of {@link StandStorage} to be used to persist
         * the latest aggregate states.
         *
         * <p>If no {@code storage} is assigned,
         * {@linkplain InMemoryStorageFactory#createStandStorage() in-memory storage}
         * will be used.
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
         * <p>If the {@code Executor} is not set,
         * {@link MoreExecutors#directExecutor() directExecutor()} will be used.
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
                storage = InMemoryStorageFactory.getInstance()
                                                .createStandStorage();
            }
            if (callbackExecutor == null) {
                callbackExecutor = MoreExecutors.directExecutor();
            }

            final Stand result = new Stand(this);
            return result;
        }
    }
}
