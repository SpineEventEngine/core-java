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
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.option.EntityOption.Kind.AGGREGATE;
import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;
import static io.spine.server.tenant.TenantAwareRunner.with;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.Math.max;

/**
 * The repository which manages instances of {@code Aggregate}s.
 *
 * @param <I> the type of the aggregate IDs
 * @param <A> the type of the aggregates managed by this repository
 * @apiNote
 * This class is made {@code abstract} for preserving type information of aggregate ID and
 * aggregate classes used by implementations.
 */
@SuppressWarnings("ClassWithTooManyMethods")
public abstract class AggregateRepository<I, A extends Aggregate<I, ?, ?>>
        extends Repository<I, A>
        implements CommandDispatcher<I>,
                   EventDispatcherDelegate<I> {

    /** The default number of events to be stored before a next snapshot is made. */
    static final int DEFAULT_SNAPSHOT_TRIGGER = 100;

    /** The routing schema for commands handled by the aggregates. */
    private final CommandRouting<I> commandRouting = CommandRouting.newInstance();

    /** The routing schema for events to which aggregates react. */
    private final EventRouting<I> eventRouting =
            EventRouting.withDefault(EventRoute.byProducerId());

    /**
     * The routing for event import, which by default obtains the target aggregate ID as the
     * {@linkplain io.spine.core.EventContext#getProducerId() producer ID} of the event.
     */
    private final EventRouting<I> eventImportRoute =
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
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@code AggregateRepository} also registers itself with:
     *
     * <ul>
     *     <li>{@link io.spine.server.commandbus.CommandBus CommandBus},
     *     <li>{@link io.spine.server.event.EventBus EventBus},
     *     <li>{@link io.spine.server.aggregate.ImportBus ImportBus} of
     *         the parent {@code BoundedContext} for dispatching messages to its aggregates;
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
    protected final AggregateClass<A> aggregateClass() {
        return (AggregateClass<A>) entityClass();
    }

    @Override
    protected AggregateClass<A> getModelClass(Class<A> cls) {
        return asAggregateClass(cls);
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
     * @param cmd the command to dispatch
     */
    @Override
    public I dispatch(CommandEnvelope cmd) {
        checkNotNull(cmd);
        I target = with(cmd.getTenantId())
                .evaluate(() -> doDispatch(cmd));
        return target;
    }

    private I doDispatch(CommandEnvelope cmd) {
        I target = route(cmd);
        lifecycleOf(target).onDispatchCommand(cmd.getCommand());
        dispatchTo(target, cmd);
        return target;
    }

    private I route(CommandEnvelope cmd) {
        CommandRouting<I> routing = getCommandRouting();
        I target = routing.apply(cmd.getMessage(), cmd.getCommandContext());
        onCommandTargetSet(target, cmd.getId());
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
    public Set<EventClass> getEventClasses() {
        return aggregateClass().getEventClasses();
    }

    @Override
    public Set<EventClass> getExternalEventClasses() {
        return aggregateClass().getExternalEventClasses();
    }

    /**
     * Obtains classes of events that can be imported by aggregates of this repository.
     */
    public Set<EventClass> getImportableEventClasses() {
        return aggregateClass().getImportableEventClasses();
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
        Set<I> targets = with(event.getTenantId())
                .evaluate(() -> doDispatch(event));
        return targets;
    }

    private Set<I> doDispatch(EventEnvelope event) {
        Set<I> targets = route(event);
        targets.forEach(id -> dispatchTo(id, event));
        return targets;
    }

    private Set<I> route(EventEnvelope event) {
        EventRouting<I> routing = getEventRouting();
        Set<I> targets = routing.apply(event.getMessage(), event.getEventContext());
        return targets;
    }

    private void dispatchTo(I id, EventEnvelope event) {
        AggregateEventEndpoint<I, A> endpoint = new AggregateEventReactionEndpoint<>(this, event);
        endpoint.dispatchTo(id);
    }

    /**
     * Imports the passed event into one of the aggregates.
     */
    I importEvent(EventEnvelope event) {
        checkNotNull(event);
        I target = with(event.getTenantId()).evaluate(() -> doImport(event));
        return target;
    }

    private I doImport(EventEnvelope event) {
        I target = routeImport(event);
        EventImportEndpoint<I, A> endpoint = new EventImportEndpoint<>(this, event);
        endpoint.dispatchTo(target);
        return target;
    }

    private I routeImport(EventEnvelope event) {
        Set<I> ids = getEventImportRouting().apply(event.getMessage(), event.getEventContext());
        int numberOfTargets = ids.size();
        checkState(
                numberOfTargets > 0,
                "Could not get aggregate ID from the event context: `%s`. Event class: `%s`.",
                event.getEventContext(),
                event.getMessageClass()
        );
        checkState(
                numberOfTargets == 1,
                "Expected one aggregate ID, but got %s (`%s`). Event class: `%s`, context: `%s`.",
                String.valueOf(numberOfTargets),
                ids,
                event.getMessageClass(),
                event.getEventContext()
        );
        I id = ids.stream()
                  .findFirst()
                  .get();
        return id;
    }

    @Override
    public void onError(EventEnvelope event, RuntimeException exception) {
        checkNotNull(event);
        checkNotNull(exception);
        logError("Error reacting on event (class: `%s` id: `%s`) in aggregate of type `%s.`",
                 event, exception);
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
    void postEvents(Collection<Event> events) {
        Iterable<Event> filteredEvents = eventFilter().filter(events);
        EventBus bus = getBoundedContext().getEventBus();
        bus.post(filteredEvents);
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
     * the last snapshot} and the {@linkplain #getSnapshotTrigger() snapshot trigger}, plus one.
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

    void onDispatchEvent(I id, Event event) {
        lifecycleOf(id).onDispatchEventToReactor(event);
    }

    private void onCommandTargetSet(I id, CommandId commandId) {
        lifecycleOf(id).onTargetAssignedToCommand(commandId);
    }

    void onEventImported(I id, Event event) {
        lifecycleOf(id).onEventImported(event);
    }
}
