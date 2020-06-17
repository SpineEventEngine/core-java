/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.annotation.Internal;
import io.spine.base.EventMessage;
import io.spine.base.entity.EntityState;
import io.spine.client.ResponseFormat;
import io.spine.client.TargetFilters;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.delivery.BatchDeliveryListener;
import io.spine.server.delivery.Delivery;
import io.spine.server.delivery.Inbox;
import io.spine.server.delivery.InboxLabel;
import io.spine.server.dispatch.BatchDispatchOutcome;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.EventProducingRepository;
import io.spine.server.entity.QueryableRepository;
import io.spine.server.entity.Repository;
import io.spine.server.entity.RepositoryCache;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcherDelegate;
import io.spine.server.route.CommandRouting;
import io.spine.server.route.EventRouting;
import io.spine.server.route.Route;
import io.spine.server.storage.StorageFactory;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.SignalEnvelope;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Suppliers.memoize;
import static io.spine.option.EntityOption.Kind.AGGREGATE;
import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;
import static io.spine.server.tenant.TenantAwareRunner.with;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The repository which manages instances of {@code Aggregate}s.
 *
 * @param <I>
 *         the type of the aggregate IDs
 * @param <A>
 *         the type of the aggregates managed by this repository
 * @param <S>
 *         the type of the state of aggregate managed by this repository
 * @see Aggregate
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public abstract class AggregateRepository<I, A extends Aggregate<I, S, ?>, S extends EntityState<I>>
        extends Repository<I, A>
        implements CommandDispatcher, EventProducingRepository,
                   EventDispatcherDelegate, QueryableRepository {

    /** The default number of events to be stored before a next snapshot is made. */
    static final int DEFAULT_SNAPSHOT_TRIGGER = 100;

    /** The routing schema for commands handled by the aggregates. */
    private final Supplier<CommandRouting<I>> commandRouting;

    /** The routing schema for events to which aggregates react. */
    private final EventRouting<I> eventRouting;

    /**
     * The routing for event import, which by default obtains the target aggregate ID as the
     * {@linkplain io.spine.core.EventContext#getProducerId() producer ID} of the event.
     */
    private final EventRouting<I> eventImportRouting;

    /**
     * The {@link Inbox} for the messages, which are sent to the instances managed by this
     * repository.
     */
    private @MonotonicNonNull Inbox<I> inbox;

    private @MonotonicNonNull RepositoryCache<I, A> cache;

    /** The number of events to store between snapshots. */
    private int snapshotTrigger = DEFAULT_SNAPSHOT_TRIGGER;

    /** Creates a new instance. */
    protected AggregateRepository() {
        super();
        this.commandRouting = memoize(() -> CommandRouting.newInstance(idClass()));
        this.eventRouting = EventRouting.withDefaultByProducerId();
        this.eventImportRouting = EventRouting.withDefaultByProducerId();
    }

    /**
     * Initializes the repository during its registration with a {@code BoundedContext}.
     *
     * <p>Verifies that the class of aggregates of this repository subscribes to at least one
     * type of messages.
     *
     * <p>Registers itself with {@link io.spine.server.commandbus.CommandBus CommandBus},
     * {@link io.spine.server.event.EventBus EventBus}, and
     * {@link io.spine.server.aggregate.ImportBus ImportBus} of the parent {@code BoundedContext}
     * for dispatching messages to its aggregates.
     *
     * @param context
     *         the {@code BoundedContext} of this repository
     * @throws IllegalStateException
     *         if the aggregate class does not handle any messages
     */
    @Override
    @OverridingMethodsMustInvokeSuper
    public void registerWith(BoundedContext context) {
        checkNotVoid();

        super.registerWith(context);

        setupCommandRouting(commandRouting.get());
        setupEventRouting(eventRouting);
        setupImportRouting(eventImportRouting);

        context.registerCommandDispatcher(this);
        if (aggregateClass().importsEvents()) {
            context.importBus()
                   .register(EventImportDispatcher.of(this));
        }
        initCache(context.isMultitenant());
        initInbox();
        initMirror();
    }

    @Override
    public final EventBus eventBus() {
        return context().eventBus();
    }

    private void initCache(boolean multitenant) {
        cache = new RepositoryCache<>(multitenant, this::doLoadOrCreate, this::doStore);
    }

    /**
     * Initializes the {@code Inbox}.
     */
    private void initInbox() {
        Delivery delivery = ServerEnvironment.instance()
                                             .delivery();
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
                .addEventEndpoint(InboxLabel.REACT_UPON_EVENT,
                                  e -> new AggregateEventReactionEndpoint<>(this, e))
                .addEventEndpoint(InboxLabel.IMPORT_EVENT,
                                  e -> new EventImportEndpoint<>(this, e))
                .addCommandEndpoint(InboxLabel.HANDLE_COMMAND,
                                    c -> new AggregateCommandEndpoint<>(this, c))
                .build();
    }

    private Inbox<I> inbox() {
        return checkNotNull(inbox);
    }

    /**
     * Ensures that this repository dispatches at least one kind of messages.
     */
    private void checkNotVoid() {
        boolean handlesCommands = dispatchesCommands();
        boolean reactsOnEvents = dispatchesEvents();

        if (!handlesCommands && !reactsOnEvents) {
            throw newIllegalStateException(
                    "Aggregates of the repository `%s` neither handle commands" +
                            " nor react on events.", this);
        }
    }

    /**
     * A callback for derived classes to customize routing schema for commands.
     *
     * <p>Default routing returns the value of the first field of a command message.
     *
     * @param routing
     *         the routing schema to customize
     */
    @SuppressWarnings("NoopMethodInAbstractClass") // See Javadoc
    protected void setupCommandRouting(CommandRouting<I> routing) {
        // Do nothing.
    }

    /**
     * A callback for derived classes to customize routing schema for events.
     *
     * <p>Default routing returns the ID of the entity which
     * {@linkplain io.spine.core.EventContext#getProducerId() produced} the event.
     * This allows to “link” different kinds of entities by having the same class of IDs.
     * More complex scenarios (e.g. one-to-many relationships) may require custom routing schemas.
     *
     * @param routing
     *         the routing schema to customize
     */
    @SuppressWarnings("NoopMethodInAbstractClass") // see Javadoc
    protected void setupEventRouting(EventRouting<I> routing) {
        // Do nothing.
    }

    /**
     * A callback for derived classes to customize routing schema for importable events.
     *
     * <p>The default routing uses {@linkplain io.spine.core.EventContext#getProducerId()
     * producer ID} of the event as the ID of the target aggregate.
     *
     * <p>This default routing requires that {@link Event Event} instances
     * {@linkplain ImportBus#post(io.spine.core.Signal, io.grpc.stub.StreamObserver)}  posted}
     * for import must {@link io.spine.core.EventContext#getProducerId() contain} the ID of the
     * target aggregate. Not providing a valid aggregate ID would result in
     * {@code RuntimeException}.
     *
     * <p>Some aggregates may produce events with the aggregate ID as the first field of an event
     * message. To set the default routing for repositories of such aggregates, please use the
     * code below:
     *
     * <pre>{@code
     * routing.replaceDefault(EventRoute.byFirstMessageField(SomeId.class));
     * }</pre>
     *
     * @param routing
     *         the routing schema to customize.
     */
    @SuppressWarnings("NoopMethodInAbstractClass") // see Javadoc
    protected void setupImportRouting(EventRouting<I> routing) {
        // Do nothing.
    }

    @Override
    public A create(I id) {
        A aggregate = aggregateClass().create(id);
        return aggregate;
    }

    /** Obtains class information of aggregates managed by this repository. */
    protected final AggregateClass<A> aggregateClass() {
        return (AggregateClass<A>) entityModelClass();
    }

    @Override
    protected AggregateClass<A> toModelClass(Class<A> cls) {
        return asAggregateClass(cls);
    }

    /**
     * Stores the passed aggregate and commits its uncommitted events.
     */
    @Override
    protected final void store(A aggregate) {
        cache.store(aggregate);
    }

    @VisibleForTesting
    protected void doStore(A aggregate) {
        UncommittedHistory history = aggregate.uncommittedHistory();
        aggregateStorage().writeAll(aggregate, history.get());
        aggregate.commitEvents();
    }

    /**
     * Creates aggregate storage for the repository.
     *
     * @return new storage
     */
    @Override
    protected AggregateStorage<I, S> createStorage() {
        StorageFactory sf = defaultStorageFactory();
        AggregateStorage<I, S> result = sf.createAggregateStorage(context().spec(), entityClass());
        return result;
    }

    @Override
    public final ImmutableSet<CommandClass> messageClasses() {
        return aggregateClass().commands();
    }

    /**
     * Dispatches the passed command to an aggregate.
     *
     * <p>The aggregate ID is obtained from the passed command.
     *
     * <p>The repository loads the aggregate by this ID, or creates a new aggregate
     * if there is no aggregate with such ID.
     *
     * @param cmd
     *         the command to dispatch
     */
    @Override
    public final void dispatch(CommandEnvelope cmd) {
        checkNotNull(cmd);
        Optional<I> target = route(cmd);
        target.ifPresent(id -> inbox().send(cmd)
                                      .toHandler(id));
    }

    private Optional<I> route(CommandEnvelope cmd) {
        Optional<I> target = route(commandRouting(), cmd);
        target.ifPresent(id -> onCommandTargetSet(id, cmd));
        return target;
    }

    @Internal
    @Override
    protected final void onRoutingFailed(SignalEnvelope<?, ?, ?> envelope, Throwable cause) {
        super.onRoutingFailed(envelope, cause);
        postIfCommandRejected(envelope, cause);
    }

    @Override
    public ImmutableSet<EventClass> events() {
        return aggregateClass().events();
    }

    @Override
    public ImmutableSet<EventClass> domesticEvents() {
        return aggregateClass().domesticEvents();
    }

    @Override
    public ImmutableSet<EventClass> externalEvents() {
        return aggregateClass().externalEvents();
    }

    /**
     * Obtains classes of events that can be imported by aggregates of this repository.
     */
    public ImmutableSet<EventClass> importableEvents() {
        return aggregateClass().importableEvents();
    }

    @Override
    public ImmutableSet<EventClass> outgoingEvents() {
        return aggregateClass().outgoingEvents();
    }

    /**
     * Dispatches event to one or more aggregates reacting on the event.
     *
     * @param event
     *         the event to dispatch
     */
    @Override
    public void dispatchEvent(EventEnvelope event) {
        checkNotNull(event);
        Set<I> targets = route(event);
        targets.forEach((id) -> inbox().send(event)
                                       .toReactor(id));
    }

    private Set<I> route(EventEnvelope event) {
        return route(eventRouting(), event)
                .orElse(ImmutableSet.of());
    }

    /**
     * Imports the passed event into one of the aggregates.
     */
    final void importEvent(EventEnvelope event) {
        checkNotNull(event);
        Optional<I> target = routeImport(event);
        target.ifPresent(id -> inbox().send(event)
                                      .toImporter(id));
    }

    private Optional<I> routeImport(EventEnvelope event) {
        Optional<I> id = route(eventImportRouting(), event);
        return id;
    }

    @SuppressWarnings("UnnecessaryLambda")
    private Route<? extends EventMessage, EventContext, I> eventImportRouting() {
        return (message, context) -> {
            Set<I> ids = eventImportRouting.apply(message, context);
            int numberOfTargets = ids.size();
            String messageType = message.getClass()
                                        .getName();
            checkState(
                    numberOfTargets > 0,
                    "Could not get aggregate ID from the event context: `%s`. Event class: `%s`.",
                    context,
                    messageType
            );
            checkState(
                    numberOfTargets == 1,
                    "Expected one aggregate ID, but got %s (%s). Event class: `%s`, context: `%s`.",
                    String.valueOf(numberOfTargets),
                    ids,
                    messageType,
                    context
            );
            I id = ids.stream()
                      .findFirst()
                      .orElseThrow(() -> newIllegalStateException(
                              "Unable to route import event `%s`.", messageType)
                      );
            return id;
        };
    }

    /**
     * Obtains command routing instance used by this repository.
     */
    private CommandRouting<I> commandRouting() {
        return commandRouting.get();
    }

    /**
     * Obtains event routing instance used by this repository.
     */
    private EventRouting<I> eventRouting() {
        return eventRouting;
    }

    /**
     * Returns the number of events until a next {@code Snapshot} is made.
     *
     * @return a positive integer value
     * @see #DEFAULT_SNAPSHOT_TRIGGER
     */
    protected int snapshotTrigger() {
        return this.snapshotTrigger;
    }

    /**
     * Changes the number of events between making aggregate snapshots to the passed value.
     *
     * <p>The default value is defined in {@link #DEFAULT_SNAPSHOT_TRIGGER}.
     *
     * <p><b>NOTE</b>: repository read operations are optimized around the current snapshot
     * trigger. Setting the snapshot trigger to a new value may cause read operations to perform
     * sub-optimally, until a new snapshot is created. This doesn't apply to newly created
     * repositories.
     *
     * @param snapshotTrigger
     *         a positive number of the snapshot trigger
     */
    protected void setSnapshotTrigger(int snapshotTrigger) {
        checkArgument(snapshotTrigger > 0);
        this.snapshotTrigger = snapshotTrigger;
    }

    /**
     * Checks if the aggregate should be mirrored, and configures
     * the underlying storage accordingly.
     */
    private void initMirror() {
        if(shouldBeMirrored()) {
            aggregateStorage().enableMirror();
        }
    }

    /**
     * Returns {@code true} if the aggregates of this repository should be mirrored.
     *
     * <p>When the entity is mirrored, its latest state is stored in an {@link AggregateStorage},
     * allowing for efficient querying from outside.
     *
     * <p>All aggregates visible for querying or subscribing should be mirrored.
     */
    private boolean shouldBeMirrored() {
        boolean shouldBeMirrored = aggregateClass().visibility()
                                                   .isNotNone();
        return shouldBeMirrored;
    }

    /**
     * Returns the storage assigned to this aggregate.
     *
     * @return storage instance
     * @throws IllegalStateException
     *         if the storage is null
     */
    protected AggregateStorage<I, S> aggregateStorage() {
        @SuppressWarnings("unchecked") // We check the type on initialization.
        AggregateStorage<I, S> result = (AggregateStorage<I, S>) storage();
        return result;
    }

    /**
     * Loads or creates an aggregate by the passed ID.
     *
     * @param id
     *         the ID of the aggregate
     * @return loaded or created aggregate instance
     */
    final A loadOrCreate(I id) {
        return cache.load(id);
    }

    @VisibleForTesting
    protected A doLoadOrCreate(I id) {
        A result = load(id).orElseGet(() -> createNew(id));
        return result;
    }

    /** Creates a new entity with the passed ID. */
    private A createNew(I id) {
        A created = create(id);
        lifecycleOf(id).onEntityCreated(AGGREGATE);
        return created;
    }

    /**
     * Loads an aggregate by the passed ID.
     *
     * <p>This method defines the basic flow of an {@code Aggregate} loading. First,
     * the {@linkplain AggregateHistory Aggregate history} is
     * {@linkplain #loadHistory fetched} from the storage. Then the {@code Aggregate} is
     * {@linkplain #restore restored} from its state history.
     *
     * @param id
     *         the ID of the aggregate
     * @return the loaded instance or {@code Optional.empty()} if there is no {@code Aggregate}
     *         with the ID
     */
    private Optional<A> load(I id) {
        Optional<AggregateHistory> found = loadHistory(id);
        Optional<A> result = found.map(history -> restore(id, history));
        return result;
    }

    /**
     * Loads the history of the {@code Aggregate} with the given ID.
     *
     * <p>The method loads only the recent history of the aggregate.
     *
     * <p>The current {@link #snapshotTrigger} is used as a batch size of the read operation,
     * so the method can perform sub-optimally for some time
     * after the {@link #snapshotTrigger} change.
     *
     * @param id
     *         the ID of the {@code Aggregate} to fetch
     * @return the {@link AggregateHistory} for the {@code Aggregate} or
     *         {@code Optional.empty()} if there is no record with the ID
     */
    private Optional<AggregateHistory> loadHistory(I id) {
        AggregateStorage<I, S> storage = aggregateStorage();
        int batchSize = snapshotTrigger + 1;
        Optional<AggregateHistory> result = storage.read(id, batchSize);
        return result;
    }

    /**
     * Plays the given {@linkplain AggregateHistory Aggregate history} for an instance
     * of {@link Aggregate} with the given ID.
     *
     * @param id
     *         the ID of the {@code Aggregate} to load
     * @param history
     *         the state record of the {@code Aggregate} to load
     * @return an instance of {@link Aggregate}
     */
    protected A restore(I id, AggregateHistory history) {
        A result = create(id);
        AggregateTransaction<I, ?, ?> tx = AggregateTransaction.start(result);
        BatchDispatchOutcome outcome = result.replay(history);
        boolean success = outcome.getSuccessful();
        tx.commitIfActive();
        if (!success) {
            lifecycleOf(id).onCorruptedState(outcome);
            throw newIllegalStateException("Aggregate %s (ID: %s) cannot be loaded.%n",
                                           aggregateClass().value()
                                                           .getName(),
                                           result.idAsString());
        }
        return result;
    }

    /**
     * Loads an aggregate by the passed ID.
     *
     * <p>An aggregate will be loaded even if
     * {@link io.spine.server.entity.Entity#isArchived() archived}
     * or {@link io.spine.server.entity.Entity#isDeleted() deleted} lifecycle
     * attribute, or both of them, are set to {@code true}.
     *
     * @param id
     *         the ID of the aggregate to load
     * @return the aggregate instance, or {@link Optional#empty() empty()} if there is no
     *         aggregate with such ID
     */
    @Override
    public Optional<A> find(I id) throws IllegalStateException {
        return load(id);
    }

    private void onCommandTargetSet(I id, CommandEnvelope cmd) {
        EntityLifecycle lifecycle = lifecycleOf(id);
        CommandId commandId = cmd.id();
        with(cmd.tenantId()).run(() -> lifecycle.onTargetAssignedToCommand(commandId));
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void close() {
        super.close();
        if (inbox != null) {
            inbox.unregister();
        }
    }

    @Override
    @Internal
    public Iterator<EntityRecord> findRecords(TargetFilters filters, ResponseFormat format) {
        return aggregateStorage().readStates(filters, format);
    }
}
