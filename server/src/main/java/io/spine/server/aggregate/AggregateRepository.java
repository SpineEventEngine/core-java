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

package io.spine.server.aggregate;

import io.spine.annotation.SPI;
import io.spine.core.BoundedContextName;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.EventId;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.command.CaughtError;
import io.spine.server.command.CommandErrorHandler;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.delivery.Shardable;
import io.spine.server.delivery.ShardedStreamConsumer;
import io.spine.server.delivery.ShardingStrategy;
import io.spine.server.delivery.UniformAcrossTargets;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcherDelegate;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.route.CommandRouting;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.server.stand.Stand;
import io.spine.server.storage.Storage;
import io.spine.server.storage.StorageFactory;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.ImmutableList.of;
import static io.spine.option.EntityOption.Kind.AGGREGATE;
import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The repository which manages instances of {@code Aggregate}s.
 *
 * @param <I> the type of the aggregate IDs
 * @param <A> the type of the aggregates managed by this repository
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 * @apiNote
 * This class is made {@code abstract} for preserving type information of aggregate ID and
 * aggregate classes used by implementations.
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public abstract class AggregateRepository<I, A extends Aggregate<I, ?, ?>>
        extends Repository<I, A>
        implements CommandDispatcher<I>,
                   EventDispatcherDelegate<I>,
                   Shardable {

    /** The default number of events to be stored before a next snapshot is made. */
    static final int DEFAULT_SNAPSHOT_TRIGGER = 100;

    /** The routing schema for commands handled by the aggregates. */
    private final CommandRouting<I> commandRouting = CommandRouting.newInstance();

    /** The routing schema for events to which aggregates react. */
    private final EventRouting<I> eventRouting =
            EventRouting.withDefault(EventRoute.fromContext());

    /**
     * The routing for event import, which by default obtains the target aggregate ID as the
     * {@linkplain io.spine.core.EventContext#getProducerId() producer ID} of the event.
     */
    private final EventRouting<I> eventImportRoute =
            EventRouting.withDefault(EventRoute.fromContext());

    private final Supplier<AggregateCommandDelivery<I, A>> commandDeliverySupplier =
            memoize(this::createCommandDelivery);

    private final Supplier<AggregateEventDelivery<I, A>> eventDeliverySupplier =
            memoize(this::createEventDelivery);

    /**
     * The {@link CommandErrorHandler} tackling the dispatching errors.
     *
     * <p>This field is not {@code final} only because it is initialized in {@link #onRegistered()}
     * method.
     */
    private CommandErrorHandler commandErrorHandler;

    /** The number of events to store between snapshots. */
    private int snapshotTrigger = DEFAULT_SNAPSHOT_TRIGGER;

    /** Creates a new instance. */
    protected AggregateRepository() {
        super();
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@code AggregateRepository} also registers itself with:
     *
     * <ul>
     *     <li>{@link io.spine.server.commandbus.CommandBus CommandBus},
     *         {@link io.spine.server.event.EventBus EventBus}, and
     *         {@link io.spine.server.aggregate.ImportBus ImportBus} of
     *         the parent {@code BoundedContext} for dispatching messages to its aggregates;
     *     <li>{@link io.spine.server.delivery.Sharding#register(io.spine.server.delivery.Shardable)
     *     Sharding} for groupping of messages sent to its aggregates.
     * </ul>
     */
    @Override
    public void onRegistered() {
        checkNotVoid();

        super.onRegistered();

        BoundedContext boundedContext = getBoundedContext();
        boundedContext.registerCommandDispatcher(this);
        boundedContext.registerEventDispatcher(this);
        if (aggregateClass().importsEvents()) {
            boundedContext.getImportBus()
                          .register(EventImportDispatcher.of(this));
        }

        this.commandErrorHandler = boundedContext.createCommandErrorHandler();
        registerWithSharding();
    }

    /**
     * Ensures that this repository dispatches at least one kind of messages.
     */
    private void checkNotVoid() {
        boolean handlesCommands = dispatchesCommands();
        boolean reactsOnEvents = dispatchesEvents() || dispatchesExternalEvents();

        if (!handlesCommands && !reactsOnEvents) {
            throw newIllegalStateException(
                    "Aggregates of the repository %s neither handle commands" +
                            " nor react on events.", this);
        }
    }

    @Override
    public A create(I id) {
        A aggregate = aggregateClass().createEntity(id);
        return aggregate;
    }

    /** Obtains class information of aggregates managed by this repository. */
    AggregateClass<A> aggregateClass() {
        return (AggregateClass<A>) entityClass();
    }

    @Override
    protected AggregateClass<A> getModelClass(Class<A> cls) {
        return asAggregateClass(cls);
    }

    @Override
    public AggregateClass<A> getShardedModelClass() {
        return aggregateClass();
    }

    /**
     * Stores the passed aggregate and commits its uncommitted events.
     */
    @Override
    protected void store(A aggregate) {
        Write<I> operation = Write.operationFor(this, aggregate);
        operation.perform();
    }

    /**
     * Creates aggregate storage for the repository.
     *
     * @param factory the factory to create the storage
     * @return new storage
     */
    @Override
    protected Storage<I, ?, ?> createStorage(StorageFactory factory) {
        Storage<I, ?, ?> result = factory.createAggregateStorage(getEntityClass());
        return result;
    }

    @Override
    public Set<CommandClass> getMessageClasses() {
        return aggregateClass().getCommands();
    }

    /**
     * Dispatches the passed command to an aggregate.
     *
     * <p>The aggregate ID is obtained from the passed command.
     *
     * <p>The repository loads the aggregate by this ID, or creates a new aggregate
     * if there is no aggregate with such ID.
     *
     * @param envelope the envelope of the command to dispatch
     */
    @Override
    public I dispatch(CommandEnvelope envelope) {
        checkNotNull(envelope);
        AggregateCommandEndpoint<I, A> endpoint = new AggregateCommandEndpoint<>(this, envelope);
        I result = endpoint.handle();
        return result;
    }

    /**
     * Handles the given error.
     *
     * <p>If the given error is a rejection, posts the rejection event into
     * the {@link EventBus}. Otherwise, logs the error.
     *
     * @param envelope  the command which caused the error
     * @param exception the error occurred during processing of the command
     */
    @Override
    public void onError(CommandEnvelope envelope, RuntimeException exception) {
        CaughtError error = commandErrorHandler.handleError(envelope, exception);
        error.asRejection()
             .map(RejectionEnvelope::getOuterObject)
             .ifPresent(event -> postEvents(of(event)));
        error.rethrowOnce();
    }

    @Override
    public Set<EventClass> getEventClasses() {
        return aggregateClass().getEventClasses();
    }

    @Override
    public Set<EventClass> getExternalEventClasses() {
        return aggregateClass().getExternalEventClasses();
    }

    public Set<EventClass> getImportableEventClasses() {
        return aggregateClass().getImportableEventClasses();
    }

    /**
     * Dispatches event to one or more aggregates reacting on the event.
     *
     * @param envelope the event
     * @return identifiers of aggregates that reacted on the event
     */
    @Override
    public Set<I> dispatchEvent(EventEnvelope envelope) {
        checkNotNull(envelope);
        AggregateEventReactionEndpoint<I, A> endpoint =
                new AggregateEventReactionEndpoint<>(this, envelope);
        Set<I> result = endpoint.handle();
        return result;
    }

    boolean importsEvent(EventClass eventClass) {
        boolean result = aggregateClass().getImportableEventClasses()
                                         .contains(eventClass);
        return result;
    }

    /**
     * Imports the passed event into one of the aggregates.
     */
    I importEvent(EventEnvelope envelope) {
        checkNotNull(envelope);
        EventImportEndpoint<I, A> endpoint = new EventImportEndpoint<>(this, envelope);
        Set<I> singleSet = endpoint.handle();
        checkState(singleSet.size() == 1);
        I result = singleSet.stream()
                            .findFirst()
                            .get();
        return result;
    }

    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);
        logError("Error reacting on event (class: %s id: %s).", envelope, exception);
    }

    /**
     * Obtains command routing instance used by this repository.
     */
    protected final CommandRouting<I> getCommandRouting() {
        return commandRouting;
    }

    /**
     * Obtains event routing instance used by this repository.
     */
    protected final EventRouting<I> getEventRouting() {
        return eventRouting;
    }

    /**
     * Obtains the event import routing, which by default uses
     * {@linkplain io.spine.core.EventContext#getProducerId() producer ID} of the event
     * as the target aggregate ID.
     *
     * <p>This default routing requires that {@link Event Event} instances
     * {@linkplain ImportBus#post(com.google.protobuf.Message, io.grpc.stub.StreamObserver) posted}
     * for import must {@link io.spine.core.EventContext#getProducerId() contain} the ID of the
     * target aggregate. Not providing a valid aggregate ID would result in
     * {@code RuntimeException}.
     *
     * <p>Some aggregates may produce events with the aggregate ID as the first field of an event
     * message. To set the default routing for repositories of such aggregates, please use the
     * code below:
     *
     * <pre>{@code
     * getEventImportRouting().replaceDefault(EventRoute.fromFirstMessageField());
     * }</pre>
     *
     * Consider adding this code to the constructor of your {@code AggregateRepository} class.
     */
    protected final EventRouting<I> getEventImportRouting() {
        return eventImportRoute;
    }

    /**
     * Posts passed events to {@link EventBus}.
     */
    void postEvents(Iterable<Event> events) {
        getEventBus().post(events);
    }

    private void updateStand(TenantId tenantId, A aggregate) {
        getStand().post(tenantId, aggregate);
    }

    /**
     * Returns the number of events until a next {@code Snapshot} is made.
     *
     * @return a positive integer value
     * @see #DEFAULT_SNAPSHOT_TRIGGER
     */
    protected int getSnapshotTrigger() {
        return this.snapshotTrigger;
    }

    /**
     * Changes the number of events between making aggregate snapshots to the passed value.
     *
     * <p>The default value is defined in {@link #DEFAULT_SNAPSHOT_TRIGGER}.
     *
     * @param snapshotTrigger a positive number of the snapshot trigger
     */
    protected void setSnapshotTrigger(int snapshotTrigger) {
        checkArgument(snapshotTrigger > 0);
        this.snapshotTrigger = snapshotTrigger;
    }

    /**
     * Returns the storage assigned to this aggregate.
     *
     * @return storage instance
     * @throws IllegalStateException if the storage is null
     */
    protected AggregateStorage<I> aggregateStorage() {
        @SuppressWarnings("unchecked") // We check the type on initialization.
        AggregateStorage<I> result = (AggregateStorage<I>) getStorage();
        return result;
    }

    /**
     * Loads or creates an aggregate by the passed ID.
     *
     * @param  id the ID of the aggregate
     * @return loaded or created aggregate instance
     */
    A loadOrCreate(I id) {
        Optional<A> optional = load(id);

        if (optional.isPresent()) {
            return optional.get();
        }

        A result = create(id);
        return result;
    }

    /**
     * Loads an aggregate by the passed ID.
     *
     * <p>This method defines the basic flow of an {@code Aggregate} loading. First,
     * the {@linkplain AggregateStateRecord Aggregate history} is
     * {@linkplain #fetchHistory fetched} from the storage. Then the {@code Aggregate} is
     * {@linkplain #play restored} from its state history.
     *
     * @param id the ID of the aggregate
     * @return the loaded instance or {@code Optional.empty()} if there is no {@code Aggregate}
     *         with the ID
     */
    private Optional<A> load(I id) {
        Optional<AggregateStateRecord> eventsFromStorage = fetchHistory(id);
        if (eventsFromStorage.isPresent()) {
            A result = play(id, eventsFromStorage.get());
            return Optional.of(result);
        } else {
            lifecycleOf(id).onEntityCreated(AGGREGATE);
            return Optional.empty();
        }
    }

    /**
     * Fetches the history of the {@code Aggregate} with the given ID.
     *
     * <p>To read an {@link AggregateStateRecord} from an {@link AggregateStorage},
     * a {@linkplain #getSnapshotTrigger() snapshot trigger} is used as a
     * {@linkplain AggregateReadRequest#getBatchSize() batch size}.
     *
     * @param id the ID of the {@code Aggregate} to fetch
     * @return the {@link AggregateStateRecord} for the {@code Aggregate} or
     *         {@code Optional.empty()} if there is no record with the ID
     */
    protected Optional<AggregateStateRecord> fetchHistory(I id) {
        AggregateReadRequest<I> request = new AggregateReadRequest<>(id, snapshotTrigger);
        Optional<AggregateStateRecord> eventsFromStorage = aggregateStorage().read(request);
        return eventsFromStorage;
    }

    /**
     * Plays the given {@linkplain AggregateStateRecord Aggregate history} for an instance
     * of {@link Aggregate} with the given ID.
     *
     * @param id      the ID of the {@code Aggregate} to load
     * @param history the state record of the {@code Aggregate} to load
     * @return an instance of {@link Aggregate}
     */
    protected A play(I id, AggregateStateRecord history) {
        A result = create(id);
        AggregateTransaction tx = AggregateTransaction.start(result);
        result.play(history);
        tx.commit();
        return result;
    }

    /**
     * Invoked by an endpoint after a message was dispatched to the aggregate.
     *
     * @param tenantId  the tenant associated with the processed message
     * @param aggregate the updated aggregate
     */
    void onModifiedAggregate(TenantId tenantId, A aggregate) {
        store(aggregate);
        updateStand(tenantId, aggregate);
    }

    /**
     * Loads an aggregate by the passed ID.
     *
     * <p>An aggregate will be loaded despite its {@linkplain LifecycleFlags visibility}.
     * I.e. even if the aggregate is
     * {@linkplain io.spine.server.entity.EntityWithLifecycle#isArchived() archived}
     * or {@linkplain io.spine.server.entity.EntityWithLifecycle#isDeleted() deleted},
     * it is loaded and returned.
     *
     * @param  id the ID of the aggregate to load
     * @return the loaded object or {@link Optional#empty()} if there are no events for the aggregate
     * @throws IllegalStateException
     *         if the storage of the repository is not {@linkplain #initStorage(StorageFactory)
     *         initialized} prior to this call
     * @see AggregateStateRecord
     */
    @Override
    public Optional<A> find(I id) throws IllegalStateException {
        Optional<A> result = load(id);
        return result;
    }

    /** The EventBus to which we post events produced by aggregates. */
    private EventBus getEventBus() {
        return getBoundedContext().getEventBus();
    }

    /** The Stand instance for sending updated aggregate states. */
    private Stand getStand() {
        return getBoundedContext().getStand();
    }

    /**
     * Defines a strategy of event delivery applied to the instances managed by this repository.
     *
     * <p>By default uses direct delivery.
     *
     * <p>Descendants may override this method to redefine the strategy. In particular,
     * it is possible to postpone dispatching of a certain event to a particular aggregate
     * instance at runtime.
     *
     * @return delivery strategy for events applied to the instances managed by this repository
     */
    @SPI
    protected AggregateDelivery<I, A, EventEnvelope, ?, ?> getEventEndpointDelivery() {
        return eventDeliverySupplier.get();
    }

    /**
     * Defines a strategy of command delivery applied to the instances managed by this repository.
     *
     * <p>By default uses direct delivery.
     *
     * <p>Descendants may override this method to redefine the strategy. In particular,
     * it is possible to postpone dispatching of a certain command to a particular aggregate
     * instance at runtime.
     *
     * @return delivery strategy for rejections
     */
    @SPI
    protected AggregateDelivery<I, A, CommandEnvelope, ?, ?> getCommandEndpointDelivery() {
        return commandDeliverySupplier.get();
    }

    @Override
    protected EntityLifecycle lifecycleOf(I id) {
        return super.lifecycleOf(id);
    }

    void onDispatchEvent(I id, Event event) {
        lifecycleOf(id).onDispatchEventToReactor(event);
    }

    void onCommandTargetSet(I id, CommandId commandId) {
        lifecycleOf(id).onTargetAssignedToCommand(commandId);
    }

    void onImportTargetSet(I id, EventId eventId) {
        lifecycleOf(id).onImportTargetSet(eventId);
    }

    void onImportEvent(I id, Event event) {
        lifecycleOf(id).onImportEvent(event);
    }

    private AggregateEventDelivery<I, A> createEventDelivery() {
        return new AggregateEventDelivery<>(this);
    }

    private AggregateCommandDelivery<I, A> createCommandDelivery() {
        return new AggregateCommandDelivery<>(this);
    }

    @Override
    public ShardingStrategy getShardingStrategy() {
        return UniformAcrossTargets.singleShard();
    }

    @Override
    public Iterable<ShardedStreamConsumer<?, ?>> getMessageConsumers() {
        Iterable<ShardedStreamConsumer<?, ?>> result =
                of(
                        getCommandEndpointDelivery().getConsumer(),
                        getEventEndpointDelivery().getConsumer()
                );
        return result;
    }

    @Override
    public BoundedContextName getBoundedContextName() {
        BoundedContextName name = getBoundedContext().getName();
        return name;
    }

    @Override
    public void close() {
        unregisterWithSharding();
        super.close();
    }
}
