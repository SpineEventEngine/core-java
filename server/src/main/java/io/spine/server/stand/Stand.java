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
package io.spine.server.stand;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.client.EntityStateUpdate;
import io.spine.client.Queries;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.Subscription;
import io.spine.client.Topic;
import io.spine.core.Response;
import io.spine.core.Responses;
import io.spine.core.TenantId;
import io.spine.core.Version;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.EntityStateEnvelope;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.Repository;
import io.spine.server.entity.VersionableEntity;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.tenant.EntityUpdateOperation;
import io.spine.server.tenant.QueryOperation;
import io.spine.server.tenant.SubscriptionOperation;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.grpc.StreamObservers.ack;
import static io.spine.protobuf.TypeConverter.toAny;

/**
 * A container for storing the latest {@link io.spine.server.aggregate.Aggregate Aggregate}
 * states.
 *
 * <p>Provides an optimal way to access the latest state of published aggregates
 * for read-side services. The aggregate states are delivered to the instance of {@code Stand}
 * from {@link AggregateRepository} instances.
 *
 * <p>In order to provide a flexibility in defining data access policies,
 * {@code Stand} contains only the states of published aggregates.
 * Please refer to {@link io.spine.server.aggregate.Aggregate Aggregate} for details on
 * publishing aggregates.
 *
 * <p>Each {@link io.spine.server.BoundedContext BoundedContext} contains only one
 * instance of {@code Stand}.
 *
 * @author Alex Tymchenko
 */
@SuppressWarnings("OverlyCoupledClass")
public class Stand implements AutoCloseable {

    /**
     * Used to return an empty result collection for {@link Query}.
     */
    private static final QueryProcessor NOOP_PROCESSOR = new NoopQueryProcessor();

    /**
     * Persistent storage for the latest {@code Aggregate} states.
     *
     * <p>Any {@code Aggregate} state delivered to this instance of {@code Stand} is
     * persisted to this storage.
     *
     * <p>The storage is {@code null} if it was not passed to the builder and this instance is not
     * yet added to a {@code BoundedContext}.
     */
    private @Nullable StandStorage storage;

    /**
     * Manages the subscriptions for this instance of {@code Stand}.
     */
    private final SubscriptionRegistry subscriptionRegistry;

    /**
     * Manages the {@linkplain TypeUrl types}, exposed via this instance of {@code Stand}.
     */
    private final TypeRegistry typeRegistry;

    /**
     * An instance of executor used to invoke callbacks.
     */
    private final Executor callbackExecutor;

    private final boolean multitenant;

    private final TopicValidator topicValidator;
    private final QueryValidator queryValidator;
    private final SubscriptionValidator subscriptionValidator;

    private Stand(Builder builder) {
        storage = builder.getStorage();
        callbackExecutor = builder.getCallbackExecutor();
        multitenant = builder.multitenant != null
                      ? builder.multitenant
                      : false;
        subscriptionRegistry = builder.getSubscriptionRegistry();
        typeRegistry = builder.getTypeRegistry();
        topicValidator = builder.getTopicValidator();
        queryValidator = builder.getQueryValidator();
        subscriptionValidator = builder.getSubscriptionValidator();
    }

    public void onCreated(BoundedContext parent) {
        if (storage == null) {
            storage = parent.getStorageFactory()
                            .createStandStorage();
        }
    }

    /**
     * Posts the state of an {@link VersionableEntity} to this {@link Stand}.
     *
     * @param entity the entity which state should be delivered to the {@code Stand}
     */
    public void post(TenantId tenantId, VersionableEntity entity) {
        EntityStateEnvelope envelope = EntityStateEnvelope.of(entity, tenantId);
        update(envelope);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Updates the state of an entity inside of the current instance of {@code Stand}.
     *
     * <p>In case the entity update represents the new
     * {@link io.spine.server.aggregate.Aggregate Aggregate} state,
     * store the new value for the {@code Aggregate} to each of the configured instances of
     * {@link StandStorage}.
     *
     * <p>Each {@code Aggregate} state value is stored as one-to-one to its
     * {@link TypeUrl TypeUrl} obtained via {@link Any#getTypeUrl()}.
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
     *
     * @param envelope the updated entity state,
     *                 packed as {@linkplain EntityStateEnvelope envelope}
     */
    void update(EntityStateEnvelope<?, ?> envelope) {
        EntityUpdateOperation op = new EntityUpdateOperation(envelope) {

            @Override
            public void run() {
                Object id = envelope.getEntityId();
                Message entityState = envelope.getMessage();
                Any packedState = AnyPacker.pack(entityState);

                TypeUrl entityTypeUrl = TypeUrl.of(entityState);
                boolean aggregateUpdate = typeRegistry.hasAggregateType(entityTypeUrl);

                if (aggregateUpdate) {
                    Optional<Version> entityVersion = envelope.getEntityVersion();
                    checkState(entityVersion.isPresent(),
                               "The aggregate version must be set in order to update Stand. " +
                                       "Actual envelope: {}", envelope);

                    @SuppressWarnings("OptionalGetWithoutIsPresent")    // checked above.
                    Version versionValue = entityVersion.get();
                    AggregateStateId aggregateStateId = AggregateStateId.of(id,
                                                                            entityTypeUrl);

                    EntityRecord record = EntityRecord.newBuilder()
                                                      .setState(packedState)
                                                      .setVersion(versionValue)
                                                      .build();
                    getStorage().write(aggregateStateId, record);
                }
                notifyMatchingSubscriptions(id, packedState, entityTypeUrl);
            }
        };
        op.execute();
    }

    @Internal
    @VisibleForTesting
    public boolean isMultitenant() {
        return multitenant;
    }

    /**
     * Subscribes for all further changes of an entity state, which satisfies the {@link Topic}.
     *
     * <p>Once this instance of {@code Stand} receives an update of an entity
     * with the given {@code TypeUrl}, all such callbacks are executed.
     *
     * @param topic an instance {@link Topic}, defining the entity and criteria,
     *              which changes should be propagated to the {@code callback}
     */
    public void subscribe(Topic topic, StreamObserver<Subscription> responseObserver) {
        topicValidator.validate(topic, responseObserver);

        TenantId tenantId = topic.getContext()
                                 .getTenantId();
        TenantAwareOperation op = new TenantAwareOperation(tenantId) {

            @Override
            public void run() {
                Subscription subscription = subscriptionRegistry.add(topic);
                responseObserver.onNext(subscription);
                responseObserver.onCompleted();
            }
        };
        op.execute();
    }

    /**
     * Activates the subscription created via {@link #subscribe(Topic, StreamObserver)
     * subscribe() method call}.
     *
     * <p>After the activation, the clients will start receiving the updates via
     * {@code EntityUpdateCallback} upon the changes in the entities, defined by
     * the {@code Target} attribute used for this subscription.
     *
     * @param subscription the subscription to activate.
     * @param callback     an instance of {@link EntityUpdateCallback} executed upon entity update.
     * @see #subscribe(Topic, StreamObserver)
     */
    public void activate(Subscription subscription,
                         EntityUpdateCallback callback,
                         StreamObserver<Response> responseObserver) {
        checkNotNull(subscription);
        checkNotNull(callback);

        subscriptionValidator.validate(subscription, responseObserver);

        SubscriptionOperation op = new SubscriptionOperation(subscription) {
            @Override
            public void run() {
                subscriptionRegistry.activate(subscription, callback);
                ack(responseObserver);
            }
        };

        op.execute();
    }

    /**
     * Cancels the {@link Subscription}.
     *
     * <p>Typically invoked to cancel the previous
     * {@link #activate(Subscription, EntityUpdateCallback, StreamObserver) activate()} call.
     *
     * <p>After this method is called, the subscribers stop receiving the updates,
     * related to the given {@code Subscription}.
     *
     * @param subscription a subscription to cancel.
     */
    public void cancel(Subscription subscription,
                       StreamObserver<Response> responseObserver) {
        subscriptionValidator.validate(subscription, responseObserver);

        SubscriptionOperation op = new SubscriptionOperation(subscription) {

            @Override
            public void run() {
                subscriptionRegistry.remove(subscription);
                ack(responseObserver);
            }
        };
        op.execute();
    }

    /**
     * Reads all {@link Entity} types exposed for reading by this instance of {@code Stand}.
     *
     * <p>Use {@link Stand#registerTypeSupplier(Repository)} to expose a type.
     *
     * <p>The result includes all values from {@link #getExposedAggregateTypes()} as well.
     *
     * @return the set of types as {@link TypeUrl} instances
     */
    public ImmutableSet<TypeUrl> getExposedTypes() {
        return typeRegistry.getTypes();
    }

    /**
     * Reads all {@link io.spine.server.aggregate.Aggregate Aggregate} entity types
     * exposed for reading by this instance of {@code Stand}.
     *
     * <p>Use {@link Stand#registerTypeSupplier(Repository)} to expose an {@code Aggregate} type.
     *
     * @return the set of types as {@link TypeUrl} instances
     */
    public ImmutableSet<TypeUrl> getExposedAggregateTypes() {
        return typeRegistry.getAggregateTypes();
    }

    /**
     * Reads a particular set of items from the read-side of the application and
     * feed the result into an instance.
     *
     * <p>{@link Query} defines the query target and the expected detail level for response.
     *
     * <p>The query results are fed to an instance
     * of {@link StreamObserver}&lt;{@link QueryResponse}&gt;.
     *
     * @param query            an instance of query
     * @param responseObserver an observer to feed the query results to.
     */
    public void execute(Query query,
                        StreamObserver<QueryResponse> responseObserver) {
        queryValidator.validate(query, responseObserver);

        TypeUrl type = Queries.typeOf(query);
        QueryProcessor queryProcessor = processorFor(type);

        QueryOperation op = new QueryOperation(query) {
            @Override
            public void run() {
                ImmutableCollection<Any> readResult = queryProcessor.process(query());
                QueryResponse response = QueryResponse.newBuilder()
                                                      .addAllMessages(readResult)
                                                      .setResponse(Responses.ok())
                                                      .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
        op.execute();
    }

    private void notifyMatchingSubscriptions(Object id, Any entityState, TypeUrl typeUrl) {
        if (subscriptionRegistry.hasType(typeUrl)) {
            Set<SubscriptionRecord> allRecords = subscriptionRegistry.byType(typeUrl);

            for (SubscriptionRecord subscriptionRecord : allRecords) {

                boolean subscriptionIsActive = subscriptionRecord.isActive();
                boolean stateMatches = subscriptionRecord.matches(typeUrl, id, entityState);
                if (subscriptionIsActive && stateMatches) {
                    Runnable action = notifySubscriptionAction(subscriptionRecord,
                                                               id, entityState);
                    callbackExecutor.execute(action);
                }
            }
        }
    }

    /**
     * Registers a supplier for the objects of a certain {@link TypeUrl} to be able
     * to read them in response to a {@link Query Query}.
     *
     * <p>In case the supplier is an instance of {@link AggregateRepository}, the {@code Repository}
     * is not registered as type supplier, since the {@code Aggregate} reads are performed
     * by accessing the latest state in the supplied {@code StandStorage}.
     *
     * <p>However, the type of the {@code AggregateRepository} instance is recorded for
     * the postponed processing of updates.
     *
     * @see #update(EntityStateEnvelope)
     */
    public <I, E extends VersionableEntity<I, ?>>
           void registerTypeSupplier(Repository<I, E> repository) {
        typeRegistry.register(repository);
    }

    /**
     * Dumps all {@link TypeUrl}-to-{@link RecordBasedRepository} relations.
     */
    @Override
    public void close() throws Exception {
        typeRegistry.close();

        if (storage != null) {
            storage.close();
        }
    }

    /**
     * A contract for the callbacks to be executed upon entity state change.
     *
     * @see #activate(Subscription, EntityUpdateCallback, StreamObserver)
     * @see #cancel(Subscription, StreamObserver)
     */
    public interface EntityUpdateCallback {

        /**
         * Called when a certain entity state is updated.
         *
         * @param newEntityState new state of the entity
         */
        void onStateChanged(EntityStateUpdate newEntityState);
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
        QueryProcessor result;

        Optional<? extends RecordBasedRepository<?, ?, ?>> repository =
                typeRegistry.getRecordRepository(type);

        if (repository.isPresent()) {

            // The query target is an {@code Entity}.
            result = new EntityQueryProcessor(repository.get());
        } else if (getExposedAggregateTypes().contains(type)) {

            // The query target is an {@code Aggregate} state.
            result = new AggregateQueryProcessor(getStorage(), type);
        } else {

            // This type points to an object, that are not exposed via the current
            // instance of {@code Stand}.
            result = NOOP_PROCESSOR;
        }
        return result;
    }

    private StandStorage getStorage() {
        checkState(storage != null, "Stand %s does not have a storage assigned", this);
        return storage;
    }

    /**
     * Creates the subscribers notification action.
     *
     * <p>The resulting action retrieves the {@linkplain EntityUpdateCallback subscriber callback}
     * and invokes it with the given Entity ID and state.
     *
     * @param subscriptionRecord the attributes of the target subscription
     * @param id                 the ID of the updated Entity
     * @param entityState        the new state of the updated Entity
     * @return a routine delivering the subscription update to the target subscriber
     */
    private static Runnable notifySubscriptionAction(SubscriptionRecord subscriptionRecord,
                                                     Object id, Any entityState) {
        Runnable result = new Runnable() {
            @Override
            public void run() {
                EntityUpdateCallback callback = subscriptionRecord.getCallback();
                checkNotNull(callback, "Notifying by a non-activated subscription.");
                Any entityId = toAny(id);
                EntityStateUpdate stateUpdate = EntityStateUpdate.newBuilder()
                                                                 .setId(entityId)
                                                                 .setState(entityState)
                                                                 .build();
                callback.onStateChanged(stateUpdate);
            }
        };
        return result;
    }

    public static class Builder {

        /**
         * The multi-tenancy flag for the {@code Stand} to build.
         *
         * <p>The value of this field should be equal to that of corresponding
         * {@linkplain io.spine.server.BoundedContextBuilder BoundedContextBuilder} and is not
         * supposed to be {@linkplain #setMultitenant(Boolean) set directly}.
         *
         * <p>If set directly, the value would be matched to the multi-tenancy flag of aggregating
         * {@code BoundedContext}.
         */
        private @MonotonicNonNull Boolean multitenant;

        private StandStorage storage;
        private Executor callbackExecutor;
        private SubscriptionRegistry subscriptionRegistry;
        private TypeRegistry typeRegistry;
        private TopicValidator topicValidator;
        private QueryValidator queryValidator;
        private SubscriptionValidator subscriptionValidator;

        /**
         * Sets an instance of {@link StandStorage} to be used to persist
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
         * Sets an {@code Executor} to be used for executing callback methods.
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

        @Internal
        @CanIgnoreReturnValue
        public Builder setMultitenant(@Nullable Boolean multitenant) {
            this.multitenant = multitenant;
            return this;
        }

        @Internal
        public @Nullable Boolean isMultitenant() {
            return multitenant;
        }

        private SubscriptionRegistry getSubscriptionRegistry() {
            return subscriptionRegistry;
        }

        private TopicValidator getTopicValidator() {
            return topicValidator;
        }

        private QueryValidator getQueryValidator() {
            return queryValidator;
        }

        private SubscriptionValidator getSubscriptionValidator() {
            return subscriptionValidator;
        }

        private TypeRegistry getTypeRegistry() {
            return typeRegistry;
        }

        /**
         * Builds an instance of {@code Stand}.
         *
         * <p>This method is supposed to be called internally when building aggregating
         * {@code BoundedContext}.
         *
         * @return new instance of Stand
         */
        @Internal
        public Stand build() {
            boolean multitenant = this.multitenant == null
                                  ? false
                                  : this.multitenant;

            if (callbackExecutor == null) {
                callbackExecutor = MoreExecutors.directExecutor();
            }

            subscriptionRegistry = MultitenantSubscriptionRegistry.newInstance(multitenant);

            typeRegistry = InMemoryTypeRegistry.newInstance();

            topicValidator = new TopicValidator(typeRegistry);
            queryValidator = new QueryValidator(typeRegistry);
            subscriptionValidator = new SubscriptionValidator(subscriptionRegistry);

            Stand result = new Stand(this);
            return result;
        }
    }
}
