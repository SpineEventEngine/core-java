/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.protobuf.Any;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Topic;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.Response;
import io.spine.core.Responses;
import io.spine.core.TenantId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.EntityRecordChange;
import io.spine.server.entity.EntityRecordChangeVBuilder;
import io.spine.server.entity.EntityRecordVBuilder;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.Repository;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.tenant.EventOperation;
import io.spine.server.tenant.QueryOperation;
import io.spine.server.tenant.SubscriptionOperation;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.system.server.EntityStateChanged;
import io.spine.system.server.SystemReadSide;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.client.Queries.typeOf;
import static io.spine.grpc.StreamObservers.ack;
import static java.util.stream.Collectors.toSet;

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
 */
@SuppressWarnings("OverlyCoupledClass")
public class Stand extends AbstractEventSubscriber implements AutoCloseable {

    /**
     * Used to return an empty result collection for {@link Query}.
     */
    private static final QueryProcessor NOOP_PROCESSOR = new NoopQueryProcessor();

    /**
     * Manages the subscriptions for this instance of {@code Stand}.
     */
    private final SubscriptionRegistry subscriptionRegistry;

    private final SubscriptionCache activeSubscriptionsCache;

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

    private final AggregateQueryProcessor aggregateQueryProcessor;

    private Stand(Builder builder) {
        this.callbackExecutor = builder.getCallbackExecutor();
        this.multitenant = builder.multitenant != null
                           ? builder.multitenant
                           : false;
        this.subscriptionRegistry = builder.getSubscriptionRegistry();
        this.activeSubscriptionsCache = builder.getSubscriptionCache();
        this.typeRegistry = builder.getTypeRegistry();
        this.topicValidator = builder.getTopicValidator();
        this.queryValidator = builder.getQueryValidator();
        this.subscriptionValidator = builder.getSubscriptionValidator();
        this.aggregateQueryProcessor = new AggregateQueryProcessor(builder.getSystemReadSide());
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Posts the state of an entity to this stand.
     *
     * @implNote
     * The only purpose of this method is to deliver the new entity state to the subscribers
     * through the artificially created {@link io.spine.system.server.EntityStateChanged} event. It
     * doesn't do any proper lifecycle management, ignoring "archived"/"deleted" flags, applied
     * messages IDs, etc.
     *
     * @param entity
     *         the entity whose state needs to be posted
     * @param lifecycle
     *         the lifecycle of the entity
     * @deprecated Avoid posting entity state to the Stand directly and prefer relying on the
     *             proper entity lifecycle via event dispatch.
     */
    @Deprecated
    public void post(Entity entity, EntityLifecycle lifecycle) {
        Any id = Identifier.pack(entity.getId());
        Any state = AnyPacker.pack(entity.getState());
        EntityRecord record = EntityRecordVBuilder
                .newBuilder()
                .setEntityId(id)
                .setState(state)
                .build();
        EntityRecordChange change = EntityRecordChangeVBuilder
                .newBuilder()
                .setNewValue(record)
                .build();
        lifecycle.onStateChanged(change, ImmutableSet.of());
    }

    /**
     * {@inheritDoc}
     *
     * <p>See {@link #canDispatch(EventEnvelope)}.
     */
    @Override
    protected void handle(EventEnvelope envelope) {
        Collection<SubscriptionRecord> subscriptionRecords =
                activeSubscriptionsCache.get(envelope);
        checkState(!subscriptionRecords.isEmpty(),
                   "Dispatched an event with no active subscriptions on it");
        subscriptionRecords.forEach(
                record -> {
                    Runnable action = () -> record.update(envelope);
                    callbackExecutor.execute(action);
                }
        );
        activeSubscriptionsCache.removeRecords(envelope);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If {@code Stand} can dispatch an event, i.e. there is a matching subscription, the method
     * caches the matching subscription record to avoid searching for it again in
     * {@link #handle(EventEnvelope)}.
     */
    @Override
    public boolean canDispatch(EventEnvelope envelope) {
        TypeUrl typeUrl = TypeUrl.of(envelope.getMessage());
        TenantAwareOperation op = new EventOperation(envelope.getOuterObject()) {
            @Override
            public void run() {
                if (!subscriptionRegistry.hasType(typeUrl)) {
                    return;
                }
                Set<SubscriptionRecord> records = subscriptionRegistry.byType(typeUrl);
                records.stream()
                       .filter(SubscriptionRecord::isActive)
                       .filter(record -> record.matches(envelope))
                       .forEach(record -> activeSubscriptionsCache.put(envelope, record));
            }
        };
        op.execute();
        boolean canDispatch = !activeSubscriptionsCache.get(envelope)
                                                       .isEmpty();
        return canDispatch;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stand dispatches no message classes if there are no subscriptions present but as having
     * such dispatcher is illegal we say that it always dispatches {@link EntityStateChanged}
     * event.
     */
    @Override
    public Set<EventClass> getMessageClasses() {
        ImmutableSet<TypeUrl> types = subscriptionRegistry.typeSet();
        Set<EventClass> result = types
                .stream()
                .map(EventClass::from)
                .collect(toSet());
        EventClass entityStateChanged = EventClass.from(EntityStateChanged.class);
        result.add(entityStateChanged);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stand does not consume external events.
     */
    @Override
    public Set<EventClass> getExternalEventClasses() {
        return ImmutableSet.of();
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
     * {@code NotifySubscriptionAction} upon the changes in the entities, defined by
     * the {@code Target} attribute used for this subscription.
     *
     * @param subscription
     *         the subscription to activate
     * @param notifyAction
     *         an action which notifies the subscribers about an update
     * @see #subscribe(Topic, StreamObserver)
     */
    public void activate(Subscription subscription,
                         NotifySubscriptionAction notifyAction,
                         StreamObserver<Response> responseObserver) {
        checkNotNull(subscription);
        checkNotNull(notifyAction);

        subscriptionValidator.validate(subscription, responseObserver);

        SubscriptionOperation op = new SubscriptionOperation(subscription) {
            @Override
            public void run() {
                subscriptionRegistry.activate(subscription, notifyAction);
                ack(responseObserver);
            }
        };

        op.execute();
    }

    /**
     * Cancels the {@link Subscription}.
     *
     * <p>Typically invoked to cancel the previous
     * {@link #activate(Subscription, NotifySubscriptionAction, StreamObserver) activate()} call.
     *
     * <p>After this method is called, the subscribers stop receiving the updates,
     * related to the given {@code Subscription}.
     *
     * @param subscription
     *         a subscription to cancel
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

        TypeUrl type = typeOf(query);
        QueryProcessor queryProcessor = processorFor(type);

        QueryOperation op = new QueryOperation(query) {
            @Override
            public void run() {
                Collection<Any> readResult = queryProcessor.process(query());
                QueryResponse response = QueryResponse
                        .newBuilder()
                        .addAllMessages(readResult)
                        .setResponse(Responses.ok())
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
        op.execute();
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
     */
    public <I, E extends Entity<I, ?>>
           void registerTypeSupplier(Repository<I, E> repository) {
        typeRegistry.register(repository);
    }

    /**
     * Dumps all {@link TypeUrl}-to-{@link RecordBasedRepository} relations.
     */
    @Override
    public void close() throws Exception {
        typeRegistry.close();
    }

    /**
     * Delivers the given subscription update to the subscribers.
     *
     * @see #activate(Subscription, NotifySubscriptionAction, StreamObserver)
     * @see #cancel(Subscription, StreamObserver)
     */
    public interface NotifySubscriptionAction extends Consumer<SubscriptionUpdate> {
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
        Optional<? extends RecordBasedRepository<?, ?, ?>> foundRepository =
                typeRegistry.getRecordRepository(type);
        if (foundRepository.isPresent()) {
            RecordBasedRepository<?, ?, ?> repository = foundRepository.get();
            return new EntityQueryProcessor(repository);
        } else if (getExposedAggregateTypes().contains(type)) {
            return aggregateProcessor();
        } else {
            return NOOP_PROCESSOR;
        }
    }

    private QueryProcessor aggregateProcessor() {
        return aggregateQueryProcessor;
    }

    @CanIgnoreReturnValue
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
        private @Nullable Boolean multitenant;

        private Executor callbackExecutor;
        private SubscriptionRegistry subscriptionRegistry;
        private SubscriptionCache subscriptionCache;
        private TypeRegistry typeRegistry;
        private TopicValidator topicValidator;
        private QueryValidator queryValidator;
        private SubscriptionValidator subscriptionValidator;
        private SystemReadSide systemReadSide;

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
            this.callbackExecutor = checkNotNull(callbackExecutor);
            return this;
        }

        @Internal
        public Builder setMultitenant(@Nullable Boolean multitenant) {
            this.multitenant = multitenant;
            return this;
        }

        @Internal
        public Builder setSystemReadSide(SystemReadSide readSide) {
            this.systemReadSide = checkNotNull(readSide);
            return this;
        }

        @Internal
        public @Nullable Boolean isMultitenant() {
            return multitenant;
        }

        private SubscriptionRegistry getSubscriptionRegistry() {
            return subscriptionRegistry;
        }

        private SubscriptionCache getSubscriptionCache() {
            return subscriptionCache;
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

        private SystemReadSide getSystemReadSide() {
            return systemReadSide;
        }

        /**
         * Builds an instance of {@code Stand}.
         *
         * <p>This method is supposed to be called internally when building aggregating
         * {@code BoundedContext}.
         *
         * @return new instance of Stand
         */
        @CheckReturnValue
        @Internal
        public Stand build() {
            checkState(systemReadSide != null, "SystemWriteSide is not set.");

            boolean multitenant = this.multitenant == null
                                  ? false
                                  : this.multitenant;

            if (callbackExecutor == null) {
                callbackExecutor = MoreExecutors.directExecutor();
            }

            subscriptionRegistry = MultitenantSubscriptionRegistry.newInstance(multitenant);

            subscriptionCache = new SubscriptionCache();

            typeRegistry = InMemoryTypeRegistry.newInstance();

            topicValidator = new TopicValidator(typeRegistry);
            queryValidator = new QueryValidator(typeRegistry);
            subscriptionValidator = new SubscriptionValidator(subscriptionRegistry);

            Stand result = new Stand(this);
            return result;
        }
    }
}
