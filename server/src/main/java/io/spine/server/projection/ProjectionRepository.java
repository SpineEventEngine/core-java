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

package io.spine.server.projection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.base.EntityState;
import io.spine.base.Identifier;
import io.spine.client.ActorRequestFactory;
import io.spine.core.ActorContext;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.BatchDeliveryListener;
import io.spine.server.delivery.CatchUp;
import io.spine.server.delivery.CatchUpId;
import io.spine.server.delivery.CatchUpProcess;
import io.spine.server.delivery.CatchUpProcessBuilder;
import io.spine.server.delivery.CatchUpSignal;
import io.spine.server.delivery.Delivery;
import io.spine.server.delivery.Inbox;
import io.spine.server.delivery.InboxLabel;
import io.spine.server.delivery.event.CatchUpRequested;
import io.spine.server.entity.EventDispatchingRepository;
import io.spine.server.entity.RepositoryCache;
import io.spine.server.entity.model.StateClass;
import io.spine.server.event.EventFactory;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.event.model.SubscriberMethod;
import io.spine.server.projection.model.ProjectionClass;
import io.spine.server.route.EventRouting;
import io.spine.server.route.StateUpdateRouting;
import io.spine.server.stand.Stand;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.TenantFunction;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeName;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Sets.intersection;
import static com.google.common.collect.Sets.union;
import static io.spine.option.EntityOption.Kind.PROJECTION;
import static io.spine.server.projection.model.ProjectionClass.asProjectionClass;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Abstract base for repositories managing {@link Projection}s.
 *
 * @param <I>
 *         the type of IDs of projections
 * @param <P>
 *         the type of projections
 * @param <S>
 *         the type of projection state messages
 */
public abstract class ProjectionRepository<I, P extends Projection<I, S, ?>, S extends EntityState>
        extends EventDispatchingRepository<I, P, S> {

    private @MonotonicNonNull Inbox<I> inbox;

    private @MonotonicNonNull CatchUpProcess<I> catchUpProcess;

    private @MonotonicNonNull RepositoryCache<I, P> cache;

    /**
     * Initializes the repository.
     *
     * <p>Ensures there is at least one event subscriber method (external or domestic) declared
     * by the class of the projection. Throws an {@code IllegalStateException} otherwise.
     *
     * <p>If projections of this repository are {@linkplain io.spine.core.Subscribe subscribed} to
     * entity state updates, a routing for state updates is created and
     * {@linkplain #setupStateRouting(StateUpdateRouting) configured}.
     * If one of the states of entities cannot be routed during the created schema,
     * {@code IllegalStateException} will be thrown.
     *
     * @param context
     *         the {@code BoundedContext} of this repository
     * @throws IllegalStateException
     *         if the state routing does not cover one of the entity state types to which
     *         the entities are subscribed
     */
    @Override
    @OverridingMethodsMustInvokeSuper
    public void registerWith(BoundedContext context) throws IllegalStateException {
        super.registerWith(context);
        ensureDispatchesEvents();
        initCache(context.isMultitenant());
        Delivery delivery = ServerEnvironment.instance()
                                             .delivery();
        initInbox(delivery);
        initCatchUp(context, delivery);
    }

    private void initCatchUp(BoundedContext context, Delivery delivery) {
        CatchUpProcessBuilder<I> builder = delivery.newCatchUpProcess(this);
        builder.withDispatchOp(this::dispatchCatchingUp)
               .withIndex(() -> ImmutableSet.copyOf(recordStorage().index()))
               .withEventStore(() -> context.eventBus()
                                            .eventStore());
        catchUpProcess = builder.build();
        context.registerEventDispatcher(catchUpProcess);

    }

    private void initCache(boolean multitenant) {
        cache = new RepositoryCache<>(multitenant, this::doFindOrCreate, this::doStore);
    }

    /**
     * Initializes the {@code Inbox}.
     * @param delivery the delivery of the current server environment
     */
    private void initInbox(Delivery delivery) {
        inbox = delivery
                .<I>newInbox(entityStateType())
                .withBatchListener(new BatchDeliveryListener<I>() {
                    @Override
                    public void onStart(I id) {
                        cache.startCaching(id);
                    }

                    @Override
                    public void onEnd(I id) {
                        cache.stopCaching(id);
                    }
                })
                .addEventEndpoint(InboxLabel.UPDATE_SUBSCRIBER,
                                  e -> ProjectionEndpoint.of(this, e))
                .addEventEndpoint(InboxLabel.CATCH_UP,
                                  e -> CatchUpEndpoint.of(this, e))
                .build();
    }

    private Inbox<I> inbox() {
        return checkNotNull(inbox);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    protected void setupEventRouting(EventRouting<I> routing) {
        super.setupEventRouting(routing);
        if (projectionClass().subscribesToStates()) {
            StateUpdateRouting<I> stateRouting = createStateRouting();
            routing.routeStateUpdates(stateRouting);
        }
    }

    private void ensureDispatchesEvents() {
        boolean noEventSubscriptions = !dispatchesEvents();
        if (noEventSubscriptions) {
            throw newIllegalStateException(
                    "Projections of the repository `%s` have neither domestic nor external" +
                            " event subscriptions.", this);
        }
    }

    /**
     * Creates and configures the {@code StateUpdateRouting} used by this repository.
     *
     * <p>This method verifies that the created state routing serves all the state classes to
     * which projections of this repository are subscribed.
     *
     * @throws IllegalStateException
     *         if one of the subscribed state classes cannot be served by the created state routing
     */
    private StateUpdateRouting<I> createStateRouting() {
        StateUpdateRouting<I> routing = StateUpdateRouting.newInstance(idClass());
        setupStateRouting(routing);
        validate(routing);
        return routing;
    }

    private void validate(StateUpdateRouting<I> routing) throws IllegalStateException {
        ProjectionClass<P> cls = projectionClass();
        Set<StateClass> stateClasses = union(cls.domesticStates(), cls.externalStates());
        ImmutableList<StateClass> unsupported =
                stateClasses.stream()
                            .filter(c -> !routing.supports(c.value()))
                            .collect(toImmutableList());
        if (!unsupported.isEmpty()) {
            boolean moreThanOne = unsupported.size() > 1;
            String fmt =
                    "The repository `%s` does not provide routing for updates of the state " +
                            (moreThanOne ? "classes" : "class") +
                            " `%s` to which the class `%s` is subscribed.";
            throw newIllegalStateException(
                    fmt, this, (moreThanOne ? unsupported : unsupported.get(0)), cls
            );
        }
    }

    /**
     * A callback for derived repository classes to customize routing schema for delivering
     * updated state to subscribed entities, if the default schema does not satisfy
     * the routing needs.
     *
     * @param routing
     *         the routing to customize
     */
    @SuppressWarnings("NoopMethodInAbstractClass") // see Javadoc
    protected void setupStateRouting(StateUpdateRouting<I> routing) {
        // Do nothing by default.
    }

    @VisibleForTesting
    static Timestamp nullToDefault(@Nullable Timestamp timestamp) {
        return timestamp == null
               ? Timestamp.getDefaultInstance()
               : timestamp;
    }

    /** Obtains {@link EventStore} from which to get events during catch-up. */
    final EventStore eventStore() {
        return context()
                .eventBus()
                .eventStore();
    }

    /** Obtains class information of projection managed by this repository. */
    private ProjectionClass<P> projectionClass() {
        return (ProjectionClass<P>) entityModelClass();
    }

    @Internal
    @Override
    protected final ProjectionClass<P> toModelClass(Class<P> cls) {
        return asProjectionClass(cls);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public P create(I id) {
        P projection = super.create(id);
        lifecycleOf(id).onEntityCreated(PROJECTION);
        return projection;
    }

    /**
     * Obtains event filters for event classes handled by projections of this repository.
     */
    private Set<EventFilter> createEventFilters() {
        ImmutableSet.Builder<EventFilter> builder = ImmutableSet.builder();
        Set<EventClass> eventClasses = messageClasses();
        for (EventClass eventClass : eventClasses) {
            String typeName = TypeName.of(eventClass.value())
                                      .value();
            builder.add(EventFilter.newBuilder()
                                   .setEventType(typeName)
                                   .build());
        }
        return builder.build();
    }

    /**
     * Obtains the {@code Stand} from the {@code BoundedContext} of this repository.
     */
    protected final Stand stand() {
        return context().stand();
    }

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException
     *         if the storage is null
     */
    @Override
    protected final RecordStorage<I> recordStorage() {
        @SuppressWarnings("unchecked") // ensured by the type returned by `createdStorage()`.
        RecordStorage<I> recordStorage = ((ProjectionStorage<I>) storage()).recordStorage();
        return checkStorage(recordStorage);
    }

    @Override
    protected final ProjectionStorage<I> createStorage() {
        StorageFactory sf = defaultStorageFactory();
        Class<P> projectionClass = entityClass();
        ProjectionStorage<I> projectionStorage =
                sf.createProjectionStorage(context().spec(), projectionClass);
        return projectionStorage;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to perform finding using the cache.
     */
    @Override
    protected final P findOrCreate(I id) {
        return cache.load(id);
    }

    private P doFindOrCreate(I id) {
        return super.findOrCreate(id);
    }

    @Override
    public final void store(P entity) {
        cache.store(entity);
    }

    private void doStore(P entity) {
        super.store(entity);
    }

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException
     *         if the storage is null
     */
    protected ProjectionStorage<I> projectionStorage() {
        @SuppressWarnings("unchecked") /* OK as we control the creation in createStorage(). */
        ProjectionStorage<I> storage = (ProjectionStorage<I>) storage();
        return storage;
    }

    @Override
    public final ImmutableSet<EventClass> messageClasses() {
        return projectionClass().events();
    }

    @Override
    public final ImmutableSet<EventClass> domesticEventClasses() {
        return projectionClass().domesticEvents();
    }

    @Override
    public final ImmutableSet<EventClass> externalEventClasses() {
        return projectionClass().externalEvents();
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public final boolean canDispatch(EventEnvelope event) {
        Optional<SubscriberMethod> subscriber = projectionClass().subscriberOf(event);
        return subscriber.isPresent();
    }

    @Override
    protected final void dispatchTo(I id, Event event) {
        inbox().send(EventEnvelope.of(event))
               .toSubscriber(id);
    }

    @Internal
    public void writeLastHandledEventTime(Timestamp timestamp) {
        checkNotNull(timestamp);
        projectionStorage().writeLastHandledEventTime(timestamp);
    }

    @Internal
    public Timestamp readLastHandledEventTime() {
        Timestamp timestamp = projectionStorage().readLastHandledEventTime();
        return nullToDefault(timestamp);
    }

    /**
     * Repeats the dispatching of the events from the event log to the requested entities
     * since the specified time.
     *
     * <p>At the beginning of the process the state of each of the entities is set to the default.
     *
     * @param ids
     *         identifiers of the entities to catch-up
     * @param since
     *         point in the past, since which the catch-up should be performed
     */
    public void catchUp(Set<I> ids, Timestamp since) {

        catchUpProcess.startCatchUp(ids, since);

        CatchUp.Request.Builder requestBuilder = CatchUp.Request.newBuilder();
        if(!ids.isEmpty()) {
            for (I id : ids) {
                Any packed = Identifier.pack(id);
                requestBuilder.addTarget(packed);
            }
        }

        requestBuilder.setSinceWhen(since);
        Set<EventClass> classes = eventClasses();
        for (EventClass eventClass : classes) {
            //TODO:2019-11-29:alex.tymchenko: `TypeName` or `TypeUrl`?
            TypeName name = eventClass.typeName();
            requestBuilder.addEventType(name.value());
        }
        CatchUp.Request request = requestBuilder.vBuild();
        CatchUpId id = CatchUpId.newBuilder()
                                .setUuid(Identifier.newUuid())
                                .setProjectionType(entityStateType().value())
                                .vBuild();
        CatchUpRequested eventMessage = CatchUpRequested
                .newBuilder()
                .setId(id)
                .setRequest(request)
                .vBuild();
        UserId actor = UserId.newBuilder()
                             .setValue(getClass().getName())
                             .build();
        ActorRequestFactory requestFactory = requestFactory(actor, context().isMultitenant());
        Any producerId = AnyPacker.pack(actor);
        ActorContext actorContext = requestFactory.newActorContext();
        EventFactory eventFactory = EventFactory.forImport(actorContext, producerId);
        Event event = eventFactory.createEvent(eventMessage, null);
        context().eventBus()
                 .post(event);
    }

    public void catchUpAll(Timestamp since) {
        catchUp(new HashSet<>(), since);
    }

    private static ActorRequestFactory requestFactory(UserId actor, boolean multitenant) {
        TenantFunction<ActorRequestFactory> function =
                new TenantFunction<ActorRequestFactory>(multitenant) {
                    @Override
                    public ActorRequestFactory apply(TenantId id) {
                        return ActorRequestFactory.newBuilder()
                                                  .setActor(actor)
                                                  .setTenantId(id)
                                                  .build();
                    }
                };
        return function.execute();
    }

    Set<I> dispatchCatchingUp(Event event, @Nullable Set<I> restrictToIds) {
        EventEnvelope envelope = EventEnvelope.of(event);
        Set<I> catchUpTargets;
        if (envelope.message() instanceof CatchUpSignal) {
            checkNotNull(restrictToIds);
            catchUpTargets = restrictToIds;
        } else {
            Set<I> routedTargets = route(envelope);
            catchUpTargets = restrictToIds == null
                             ? routedTargets
                             : intersection(routedTargets,restrictToIds).immutableCopy();
        }
        Inbox<I> inbox = inbox();
        for (I target : catchUpTargets) {
            inbox.send(envelope)
                 .toCatchUp(target);
        }
        return catchUpTargets;
    }

    @VisibleForTesting
    final EventStreamQuery createStreamQuery() {
        Set<EventFilter> eventFilters = createEventFilters();

        // Gets the timestamp of the last event. This also ensures we have the storage.
        Timestamp timestamp = readLastHandledEventTime();
        EventStreamQuery.Builder builder = EventStreamQuery
                .newBuilder()
                .setAfter(timestamp)
                .addAllFilter(eventFilters);
        return builder.build();
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void close() {
        super.close();
        if (inbox != null) {
            inbox.unregister();
        }
    }
}
