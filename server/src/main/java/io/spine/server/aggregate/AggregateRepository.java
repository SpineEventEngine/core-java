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

package io.spine.server.aggregate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets.SetView;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.command.CommandErrorHandler;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcherDelegate;
import io.spine.server.route.CommandRouting;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.server.storage.Storage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.Sets.union;
import static io.spine.option.EntityOption.Kind.AGGREGATE;
import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;
import static io.spine.server.tenant.TenantAwareRunner.with;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.Math.max;

/**
 * The repository which manages instances of {@code Aggregate}s.
 *
 * @param <I>
 *         the type of the aggregate IDs
 * @param <A>
 *         the type of the aggregates managed by this repository
 * @see Aggregate
 */
@SuppressWarnings("ClassWithTooManyMethods")
public abstract class AggregateRepository<I, A extends Aggregate<I, ?, ?>>
        extends Repository<I, A>
        implements CommandDispatcher<I>,
                   EventDispatcherDelegate<I> {

    /** The default number of events to be stored before a next snapshot is made. */
    static final int DEFAULT_SNAPSHOT_TRIGGER = 100;

    /** The routing schema for commands handled by the aggregates. */
    private final Supplier<CommandRouting<I>> commandRouting;

    /** The routing schema for events to which aggregates react. */
    private final EventRouting<I> eventRouting =
            EventRouting.withDefault(EventRoute.byProducerId());

    /**
     * The routing for event import, which by default obtains the target aggregate ID as the
     * {@linkplain io.spine.core.EventContext#getProducerId() producer ID} of the event.
     */
    private final EventRouting<I> eventImportRouting =
            EventRouting.withDefault(EventRoute.byProducerId());

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
        this.commandRouting = memoize(() -> CommandRouting.newInstance(idClass()));
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
    protected void init(BoundedContext context) {
        checkNotVoid();

        super.init(context);

        setupCommandRouting(commandRouting.get());
        setupEventRouting(eventRouting);
        setupImportRouting(eventImportRouting);

        context.registerCommandDispatcher(this);
        context.registerEventDispatcher(this);
        if (aggregateClass().importsEvents()) {
            context.importBus()
                   .register(EventImportDispatcher.of(this));
        }
        this.commandErrorHandler = context.createCommandErrorHandler();
    }

    /**
     * Ensures that this repository dispatches at least one kind of messages.
     */
    private void checkNotVoid() {
        boolean handlesCommands = dispatchesCommands();
        boolean reactsOnEvents = dispatchesEvents() || dispatchesExternalEvents();

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
     * routing.replaceDefault(EventRoute.fromFirstMessageField());
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
        Storage<I, ?, ?> result = factory.createAggregateStorage(entityClass());
        return result;
    }

    @Override
    public Set<CommandClass> messageClasses() {
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
     * @param cmd the command to dispatch
     */
    @Override
    public I dispatch(CommandEnvelope cmd) {
        checkNotNull(cmd);
        I target = with(cmd.tenantId())
                .evaluate(() -> doDispatch(cmd));
        return target;
    }

    private I doDispatch(CommandEnvelope cmd) {
        I target = route(cmd);
        lifecycleOf(target).onDispatchCommand(cmd.command());
        dispatchTo(target, cmd);
        return target;
    }

    private I route(CommandEnvelope cmd) {
        CommandRouting<I> routing = commandRouting();
        I target = routing.apply(cmd.message(), cmd.context());
        onCommandTargetSet(target, cmd.id());
        return target;
    }

    private void dispatchTo(I id, CommandEnvelope cmd) {
        AggregateCommandEndpoint<I, A> endpoint = new AggregateCommandEndpoint<>(this, cmd);
        endpoint.dispatchTo(id);
    }

    /**
     * Handles the given error.
     *
     * <p>If the given error is a rejection, posts the rejection event into
     * the {@link EventBus}. Otherwise, logs the error.
     *
     * @param cmd
     *         the command which caused the error
     * @param exception
     *         the error occurred during processing of the command
     */
    @Override
    public void onError(CommandEnvelope cmd, RuntimeException exception) {
        commandErrorHandler.handle(cmd, exception, event -> postEvents(ImmutableList.of(event)));
    }

    @Override
    public Set<EventClass> domesticEvents() {
        return aggregateClass().domesticEvents();
    }

    @Override
    public Set<EventClass> externalEvents() {
        return aggregateClass().externalEvents();
    }

    /**
     * Obtains classes of events that can be imported by aggregates of this repository.
     */
    public Set<EventClass> importableEvents() {
        return aggregateClass().importableEvents();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns events emitted by the aggregate class as well as importable events.
     *
     * <p>Although technically imported events are not "produced" in this repository, they end up
     * in the same {@code EventBus} and have the same behaviour as the ones emitted by the
     * aggregates.
     */
    @Override
    public ImmutableSet<EventClass> outgoingEvents() {
        SetView<EventClass> eventClasses =
                union(aggregateClass().outgoingEvents(), importableEvents());
        return ImmutableSet.copyOf(eventClasses);
    }

    /**
     * Dispatches event to one or more aggregates reacting on the event.
     *
     * @param event the event to dispatch
     * @return identifiers of aggregates that reacted on the event
     */
    @Override
    public Set<I> dispatchEvent(EventEnvelope event) {
        checkNotNull(event);
        Set<I> targets = with(event.tenantId())
                .evaluate(() -> doDispatch(event));
        return targets;
    }

    private Set<I> doDispatch(EventEnvelope event) {
        Set<I> targets = route(event);
        targets.forEach(id -> dispatchTo(id, event));
        return targets;
    }

    private Set<I> route(EventEnvelope event) {
        EventRouting<I> routing = eventRouting();
        Set<I> targets = routing.apply(event.message(), event.context());
        return targets;
    }

    private void dispatchTo(I id, EventEnvelope event) {
        AggregateEventEndpoint<I, A> endpoint = new AggregateEventReactionEndpoint<>(this, event);
        endpoint.dispatchTo(id);
    }

    /**
     * Imports the passed event into one of the aggregates.
     */
    final I importEvent(EventEnvelope event) {
        checkNotNull(event);
        I target = with(event.tenantId()).evaluate(() -> doImport(event));
        return target;
    }

    private I doImport(EventEnvelope event) {
        I target = routeImport(event);
        EventImportEndpoint<I, A> endpoint = new EventImportEndpoint<>(this, event);
        endpoint.dispatchTo(target);
        return target;
    }

    private I routeImport(EventEnvelope event) {
        Set<I> ids = eventImportRouting.apply(event.message(), event.context());
        int numberOfTargets = ids.size();
        checkState(
                numberOfTargets > 0,
                "Could not get aggregate ID from the event context: `%s`. Event class: `%s`.",
                event.context(),
                event.messageClass()
        );
        checkState(
                numberOfTargets == 1,
                "Expected one aggregate ID, but got %s (`%s`). Event class: `%s`, context: `%s`.",
                String.valueOf(numberOfTargets),
                ids,
                event.messageClass(),
                event.context()
        );
        I id = ids.stream()
                  .findFirst()
                  .orElseThrow(() -> newIllegalStateException(
                          "Unable to route import event: `%s`.", event)
                  );
        return id;
    }

    @Override
    public void onError(EventEnvelope event, RuntimeException exception) {
        checkNotNull(event);
        checkNotNull(exception);
        logError("Error reacting on event (class: `%s` id: `%s`) in aggregate with the state `%s`.",
                 event, exception);
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
     * Posts passed events to {@link EventBus}.
     */
    final void postEvents(Collection<Event> events) {
        Iterable<Event> filteredEvents = eventFilter().filter(events);
        EventBus bus = context().eventBus();
        bus.post(filteredEvents);
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
        AggregateStorage<I> result = (AggregateStorage<I>) storage();
        return result;
    }

    /**
     * Loads or creates an aggregate by the passed ID.
     *
     * @param  id the ID of the aggregate
     * @return loaded or created aggregate instance
     */
    final A loadOrCreate(I id) {
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
     * {@linkplain #play restored} from its state history.
     *
     * @param id the ID of the aggregate
     * @return the loaded instance or {@code Optional.empty()} if there is no {@code Aggregate}
     *         with the ID
     */
    private Optional<A> load(I id) {
        Optional<AggregateHistory> found = loadHistory(id);
        Optional<A> result = found.map(history -> play(id, history));
        return result;
    }

    /**
     * Loads the history of the {@code Aggregate} with the given ID.
     *
     * <p>The method loads only the recent history of the aggregate.
     * The maximum depth of the read operation is defined as the greater number between
     * the {@linkplain AggregateStorage#readEventCountAfterLastSnapshot(Object) event count after
     * the last snapshot} and the {@linkplain #snapshotTrigger() snapshot trigger}, plus one.
     * This way, we secure the read operation from:
     * <ol>
     *     <li>snapshot trigger decrease;
     *     <li>eventual consistency in the event count field.
     * </ol>
     *
     * <p>The extra one unit of depth is added in order to be sure to include the last snapshot
     * itself.
     *
     * @param id the ID of the {@code Aggregate} to fetch
     * @return the {@link AggregateHistory} for the {@code Aggregate} or
     *         {@code Optional.empty()} if there is no record with the ID
     */
    private Optional<AggregateHistory> loadHistory(I id) {
        AggregateStorage<I> storage = aggregateStorage();

        int eventsAfterLastSnapshot = storage.readEventCountAfterLastSnapshot(id);
        int batchSize = max(snapshotTrigger, eventsAfterLastSnapshot) + 1;

        AggregateReadRequest<I> request = new AggregateReadRequest<>(id, batchSize);
        Optional<AggregateHistory> result = storage.read(request);
        return result;
    }

    /**
     * Plays the given {@linkplain AggregateHistory Aggregate history} for an instance
     * of {@link Aggregate} with the given ID.
     *
     * @param id      the ID of the {@code Aggregate} to load
     * @param history the state record of the {@code Aggregate} to load
     * @return an instance of {@link Aggregate}
     */
    protected A play(I id, AggregateHistory history) {
        A result = create(id);
        AggregateTransaction tx = AggregateTransaction.start(result);
        result.play(history);
        tx.commit();
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
     * @param  id the ID of the aggregate to load
     * @return the aggregate instance, or {@link Optional#empty() empty()} if there is no
     *         aggregate with such ID
     */
    @Override
    public Optional<A> find(I id) throws IllegalStateException {
        return load(id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overridden to expose the method into current package.
     */
    @Override
    protected EntityLifecycle lifecycleOf(I id) {
        return super.lifecycleOf(id);
    }

    final void onDispatchEvent(I id, Event event) {
        lifecycleOf(id).onDispatchEventToReactor(event);
    }

    private void onCommandTargetSet(I id, CommandId commandId) {
        lifecycleOf(id).onTargetAssignedToCommand(commandId);
    }

    final void onEventImported(I id, Event event) {
        lifecycleOf(id).onEventImported(event);
    }
}
