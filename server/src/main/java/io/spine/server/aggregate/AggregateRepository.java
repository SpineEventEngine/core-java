/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.common.base.Optional;
import io.spine.annotation.SPI;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.commandbus.CommandErrorHandler;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.Repository;
import io.spine.server.event.DelegatingEventDispatcher;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcherDelegate;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.model.Model;
import io.spine.server.rejection.DelegatingRejectionDispatcher;
import io.spine.server.rejection.RejectionDispatcherDelegate;
import io.spine.server.route.CommandRouting;
import io.spine.server.route.EventProducers;
import io.spine.server.route.EventRouting;
import io.spine.server.route.RejectionProducers;
import io.spine.server.route.RejectionRouting;
import io.spine.server.stand.Stand;
import io.spine.server.storage.Storage;
import io.spine.server.storage.StorageFactory;

import javax.annotation.CheckReturnValue;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The repository which manages instances of {@code Aggregate}s.
 *
 * <p>This class is made {@code abstract} for preserving type information of aggregate ID and
 * aggregate classes used by implementations.
 *
 * <p>A repository class may look like this:
 * <pre>
 * {@code
 *  public class OrderRepository extends AggregateRepository<OrderId, OrderAggregate> {
 *      public OrderRepository() {
 *          super();
 *      }
 *  }
 * }
 * </pre>
 *
 * @param <I> the type of the aggregate IDs
 * @param <A> the type of the aggregates managed by this repository
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public abstract class AggregateRepository<I, A extends Aggregate<I, ?, ?>>
        extends Repository<I, A>
        implements CommandDispatcher<I>,
                   EventDispatcherDelegate<I>,
                   RejectionDispatcherDelegate<I> {

    /** The default number of events to be stored before a next snapshot is made. */
    static final int DEFAULT_SNAPSHOT_TRIGGER = 100;

    /** The routing schema for commands handled by the aggregates. */
    private final CommandRouting<I> commandRouting = CommandRouting.newInstance();

    /** The routing schema for events to which aggregates react. */
    private final EventRouting<I> eventRouting =
            EventRouting.withDefault(EventProducers.<I>fromContext());

    /** The routing schema for rejections to which aggregates react. */
    private final RejectionRouting<I> rejectionRouting =
            RejectionRouting.withDefault(RejectionProducers.<I>fromContext());

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
     * <p>{@linkplain io.spine.server.commandbus.CommandBus#register(
     * io.spine.server.bus.MessageDispatcher) Registers} itself with the {@code CommandBus} of the
     * parent {@code BoundedContext}.
     */
    @Override
    public void onRegistered() {
        super.onRegistered();
        final BoundedContext boundedContext = getBoundedContext();

        final Set<CommandClass> commandClasses = getMessageClasses();

        final DelegatingEventDispatcher<I> eventDispatcher;
        eventDispatcher = DelegatingEventDispatcher.of(this);
        final Set<EventClass> eventClasses = eventDispatcher.getMessageClasses();

        final ExternalMessageDispatcher<I> extEventDispatcher;
        extEventDispatcher = eventDispatcher.getExternalDispatcher();
        final Set<ExternalMessageClass> extEventClasses = extEventDispatcher.getMessageClasses();

        final DelegatingRejectionDispatcher<I> rejectionDispatcher;
        rejectionDispatcher = DelegatingRejectionDispatcher.of(this);
        final Set<RejectionClass> rejectionClasses = rejectionDispatcher.getMessageClasses();

        final ExternalMessageDispatcher<I> extRejectionDispatcher;
        extRejectionDispatcher = rejectionDispatcher.getExternalDispatcher();
        final Set<ExternalMessageClass> extRejectionClasses =
                extRejectionDispatcher.getMessageClasses();

        if (commandClasses.isEmpty() && eventClasses.isEmpty() && rejectionClasses.isEmpty()
                && extEventClasses.isEmpty() && extRejectionClasses.isEmpty()) {
            throw newIllegalStateException(
                    "Aggregates of the repository %s neither handle commands" +
                            " nor react on events or rejections.", this);
        }

        registerInCommandBus(boundedContext, commandClasses);
        registerInEventBus(boundedContext, eventDispatcher, eventClasses);
        registerInRejectionBus(boundedContext, rejectionDispatcher, rejectionClasses);

        registerExtMessageDispatcher(boundedContext, extEventDispatcher, extEventClasses);
        registerExtMessageDispatcher(boundedContext, extRejectionDispatcher, extRejectionClasses);

        this.commandErrorHandler = CommandErrorHandler.with(boundedContext.getRejectionBus());
    }

    private void registerExtMessageDispatcher(BoundedContext boundedContext,
                                              ExternalMessageDispatcher<I> extEventDispatcher,
                                              Set<ExternalMessageClass> extEventClasses) {
        if (!extEventClasses.isEmpty()) {
            boundedContext.getIntegrationBus()
                          .register(extEventDispatcher);
        }
    }

    private void registerInRejectionBus(BoundedContext boundedContext,
                                        DelegatingRejectionDispatcher<I> rejectionDispatcher,
                                        Set<RejectionClass> rejectionClasses) {
        if (!rejectionClasses.isEmpty()) {
            boundedContext.getRejectionBus()
                          .register(rejectionDispatcher);
        }
    }

    private void registerInEventBus(BoundedContext boundedContext,
                                    DelegatingEventDispatcher<I> eventDispatcher,
                                    Set<EventClass> eventClasses) {
        if (!eventClasses.isEmpty()) {
            boundedContext.getEventBus()
                          .register(eventDispatcher);
        }
    }

    private void registerInCommandBus(BoundedContext boundedContext,
                                      Set<CommandClass> commandClasses) {
        if (!commandClasses.isEmpty()) {
            boundedContext.getCommandBus()
                          .register(this);
        }
    }

    @Override
    public A create(I id) {
        return aggregateClass().createEntity(id);
    }

    /** Obtains class information of aggregates managed by this repository. */
    private AggregateClass<A> aggregateClass() {
        return (AggregateClass<A>)entityClass();
    }

    @Override
    @SuppressWarnings("unchecked") // The cast is ensured by generic parameters of the repository.
    protected AggregateClass<A> getModelClass(Class<A> cls) {
        return (AggregateClass<A>) Model.getInstance()
                                        .asAggregateClass(cls);
    }

    /**
     * Stores the passed aggregate and commits its uncommitted events.
     *
     * @param aggregate an instance to store
     */
    @Override
    protected void store(A aggregate) {
        final I id = aggregate.getId();
        final int snapshotTrigger = getSnapshotTrigger();
        final AggregateStorage<I> storage = aggregateStorage();
        int eventCount = storage.readEventCountAfterLastSnapshot(id);
        final Iterable<Event> uncommittedEvents = aggregate.getUncommittedEvents();
        for (Event event : uncommittedEvents) {
            storage.writeEvent(id, event);
            ++eventCount;
            if (eventCount >= snapshotTrigger) {
                final Snapshot snapshot = aggregate.toSnapshot();
                storage.writeSnapshot(id, snapshot);
                eventCount = 0;
            }
        }
        aggregate.commitEvents();
        storage.writeEventCountAfterLastSnapshot(id, eventCount);

        if (aggregate.lifecycleFlagsChanged()) {
            storage.writeLifecycleFlags(aggregate.getId(), aggregate.getLifecycleFlags());
        }
    }

    /**
     * Creates aggregate storage for the repository.
     *
     * @param factory the factory to create the storage
     * @return new storage
     */
    @Override
    protected Storage<I, ?, ?> createStorage(StorageFactory factory) {
        final Storage<I, ?, ?> result = factory.createAggregateStorage(getEntityClass());
        return result;
    }

    /**
     * {@inheritDoc}
     */
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
    public I dispatch(final CommandEnvelope envelope) {
        checkNotNull(envelope);
        return AggregateCommandEndpoint.handle(this, envelope);
    }

    /**
     * Logs the passed exception in the log associated with the class of the repository.
     *
     * <p>The exception is logged only if the root cause of it is not a
     * {@linkplain io.spine.base.ThrowableMessage rejection} thrown by a command handling method.
     *
     * @param envelope  the command which caused the error
     * @param exception the error occurred during processing of the command
     */
    @Override
    public void onError(CommandEnvelope envelope, RuntimeException exception) {
        commandErrorHandler.handleError(envelope, exception);
    }

    @Override
    public Set<EventClass> getEventClasses() {
        return aggregateClass().getEventReactions();
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // We return immutable impl.
    public Set<EventClass> getExternalEventClasses() {
        return aggregateClass().getExternalEventReactions();
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
        return AggregateEventEndpoint.handle(this, envelope);
    }

    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);
        logError("Error reacting on event (class: %s id: %s).", envelope, exception);
    }

    @Override
    public Set<RejectionClass> getRejectionClasses() {
        return aggregateClass().getRejectionReactions();
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // We return immutable impl.
    public Set<RejectionClass> getExternalRejectionClasses() {
        return aggregateClass().getExternalRejectionReactions();
    }

    @Override
    public Set<I> dispatchRejection(RejectionEnvelope envelope) {
        checkNotNull(envelope);
        return AggregateRejectionEndpoint.handle(this, envelope);
    }

    @Override
    public void onError(RejectionEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);
        logError("Error reacting on rejection (class %s, id: %s)", envelope, exception);
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
     * Obtains rejection routing instance used by this repository.
     */
    protected final RejectionRouting<I> getRejectionRouting() {
        return rejectionRouting;
    }

    /**
     * Posts passed events to {@link EventBus}.
     */
    private void postEvents(Iterable<Event> events) {
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
    @CheckReturnValue
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

    @VisibleForTesting
    public AggregateStorage<I> aggregateStorage() {
        @SuppressWarnings("unchecked") // We check the type on initialization.
        final AggregateStorage<I> result = (AggregateStorage<I>) getStorage();
        return result;
    }

    /**
     * Loads or creates an aggregate by the passed ID.
     *
     * @param  id the ID of the aggregate
     * @return loaded or created aggregate instance
     */
    A loadOrCreate(I id) {
        final Optional<A> optional = load(id);

        if (optional.isPresent()) {
            return optional.get();
        }

        final A result = create(id);
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
     * @return the loaded instance or {@code Optional.absent()} if there is no {@code Aggregate}
     *         with the ID
     */
    private Optional<A> load(I id) {
        final Optional<AggregateStateRecord> eventsFromStorage = fetchHistory(id);
        if (eventsFromStorage.isPresent()) {
            final A result = play(id, eventsFromStorage.get());
            return Optional.of(result);
        }
        return Optional.absent();
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
     *         {@code Optional.absent()} if there is no record with the ID
     */
    protected Optional<AggregateStateRecord> fetchHistory(I id) {
        final AggregateReadRequest<I> request = new AggregateReadRequest<>(id, snapshotTrigger);
        final Optional<AggregateStateRecord> eventsFromStorage = aggregateStorage().read(request);
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
        final A result = create(id);
        final AggregateTransaction tx = AggregateTransaction.start(result);
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
        final List<Event> events = aggregate.getUncommittedEvents();
        store(aggregate);
        updateStand(tenantId, aggregate);
        postEvents(events);
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
     * @return the loaded object or {@link Optional#absent()} if there are no events for the aggregate
     * @throws IllegalStateException
     *         if the storage of the repository is not {@linkplain #initStorage(StorageFactory)
     *         initialized} prior to this call
     * @see AggregateStateRecord
     */
    @Override
    public Optional<A> find(I id) throws IllegalStateException {
        final Optional<A> result = load(id);
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
    protected AggregateEndpointDelivery<I, A, EventEnvelope> getEventEndpointDelivery() {
        return AggregateEventDelivery.directDelivery(this);
    }

    /**
     * Defines a strategy of rejection delivery applied to the instances managed by this repository.
     *
     * <p>By default uses direct delivery.
     *
     * <p>Descendants may override this method to redefine the strategy. In particular,
     * it is possible to postpone dispatching of a certain rejection to a particular aggregate
     * instance at runtime.
     *
     * @return delivery strategy for rejections
     */
    @SPI
    protected AggregateEndpointDelivery<I, A, RejectionEnvelope> getRejectionEndpointDelivery() {
        return AggregateRejectionDelivery.directDelivery(this);
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
    protected AggregateEndpointDelivery<I, A, CommandEnvelope> getCommandEndpointDelivery() {
        return AggregateCommandDelivery.directDelivery(this);
    }
}
