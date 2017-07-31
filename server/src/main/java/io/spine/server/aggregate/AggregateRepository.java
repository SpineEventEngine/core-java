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
package io.spine.server.aggregate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.Repository;
import io.spine.server.event.DelegatingEventDispatcher;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcherDelegate;
import io.spine.server.route.CommandRouting;
import io.spine.server.route.EventRouting;
import io.spine.server.route.Producers;
import io.spine.server.stand.Stand;
import io.spine.server.storage.Storage;
import io.spine.server.storage.StorageFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Commands.causedByRejection;
import static io.spine.server.entity.AbstractEntity.createEntity;
import static io.spine.server.entity.AbstractEntity.getConstructor;
import static io.spine.server.entity.EntityWithLifecycle.Predicates.isEntityVisible;
import static io.spine.server.route.EventRouting.withDefault;

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
        implements CommandDispatcher<I>, EventDispatcherDelegate<I> {

    /** The default number of events to be stored before a next snapshot is made. */
    static final int DEFAULT_SNAPSHOT_TRIGGER = 100;

    /** The routing schema for commands handled by the aggregates. */
    private final CommandRouting<I> commandRouting = CommandRouting.newInstance();

    /** The routing schema for events to which aggregates react. */
    private final EventRouting<I> eventRouting = withDefault(Producers.<I>fromContext());

    /** The number of events to store between snapshots. */
    private int snapshotTrigger = DEFAULT_SNAPSHOT_TRIGGER;

    /** The constructor for creating entity instances. */
    private Constructor<A> entityConstructor;

    /** The set of command classes dispatched to aggregates by this repository. */
    @Nullable
    private Set<CommandClass> commandClasses;

    /** The set of event classes to which aggregates of this repository {@linkplain React react}. */
    @Nullable
    private Set<EventClass> reactedEventClasses;

    /** Creates a new instance. */
    protected AggregateRepository() {
        super();
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@linkplain io.spine.server.commandbus.CommandBus#register(
     *io.spine.server.bus.MessageDispatcher) Registers} itself with the {@code CommandBus} of the
     * parent {@code BoundedContext}.
     */
    @Override
    public void onRegistered() {
        super.onRegistered();
        final BoundedContext boundedContext = getBoundedContext();
        boundedContext.getCommandBus()
                      .register(this);

        // If there are any events on which aggregates react, register via delegating dispatcher.
        final DelegatingEventDispatcher<I> delegatingDispatcher;
        delegatingDispatcher = DelegatingEventDispatcher.of(this);
        if (!delegatingDispatcher.getMessageClasses()
                                 .isEmpty()) {
            boundedContext.getEventBus()
                          .register(delegatingDispatcher);
        }
    }

    @Override
    public A create(I id) {
        return createEntity(getEntityConstructor(), id);
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
    protected Storage<I, ?> createStorage(StorageFactory factory) {
        final Storage<I, ?> result = factory.createAggregateStorage(getAggregateClass());
        return result;
    }

    /**
     * Obtains the constructor.
     *
     * <p>The method returns cached value if called more than once.
     * During the first call, it {@linkplain #findEntityConstructor() finds} the constructor.
     */
    protected Constructor<A> getEntityConstructor() {
        if (this.entityConstructor == null) {
            this.entityConstructor = findEntityConstructor();
        }
        return this.entityConstructor;
    }

    /**
     * Obtains the constructor for creating entities.
     */
    @VisibleForTesting
    protected Constructor<A> findEntityConstructor() {
        final Constructor<A> result = getConstructor(getEntityClass(), getIdClass());
        this.entityConstructor = result;
        return result;
    }

    /**
     * Returns the class of aggregates managed by this repository.
     *
     * <p>This is convenience method, which redirects to {@link #getEntityClass()}.
     *
     * @return the class of the aggregates
     */
    Class<? extends Aggregate<I, ?, ?>> getAggregateClass() {
        return getEntityClass();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // We return immutable impl.
    @Override
    public Set<CommandClass> getMessageClasses() {
        if (commandClasses == null) {
            commandClasses = Aggregate.TypeInfo.getCommandClasses(getAggregateClass());
        }
        return commandClasses;
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
        try {
            return AggregateCommandEndpoint.handle(this, envelope);
        } catch (RuntimeException e) {
            if (!causedByRejection(e)) {
                onError(envelope, e);
            }
            throw e;
        }
    }

    /**
     * Logs the passed error in the log associated with the class of the repository.
     * 
     * @param envelope  the command which caused the error
     * @param exception the error occurred during processing of the command
     */
    @Override
    public void onError(CommandEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);
        logError("Error dispatching command (class: %s id: %s).", envelope, exception);
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // We return immutable impl.
    public Set<EventClass> getEventClasses() {
        if (reactedEventClasses == null) {
            reactedEventClasses = Aggregate.TypeInfo.getReactedEventClasses(getAggregateClass());
        }
        return reactedEventClasses;
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

    /**
     * Obtains command routing instance used by this repository.
     */
    protected final CommandRouting<I> getCommandRouting() {
        return commandRouting;
    }

    I getCommandTarget(Message commandMessage, CommandContext commandContext) {
        final I id = getCommandRouting().apply(commandMessage, commandContext);
        return id;
    }

    /**
     * Obtains event routing instance used by this repository.
     */
    protected final EventRouting<I> getEventRouting() {
        return eventRouting;
    }

    Set<I> getEventTargets(Message eventMessage, EventContext eventContext) {
        final Set<I> ids = getEventRouting().apply(eventMessage, eventContext);
        return ids;
    }

    /**
     * Posts passed events to {@link EventBus}.
     */
    void postEvents(Iterable<Event> events) {
        getEventBus().post(events);
    }

    void updateStand(TenantId tenantId, A aggregate) {
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
    @SuppressWarnings("unused")
    protected void setSnapshotTrigger(int snapshotTrigger) {
        checkArgument(snapshotTrigger > 0);
        this.snapshotTrigger = snapshotTrigger;
    }

    AggregateStorage<I> aggregateStorage() {
        @SuppressWarnings("unchecked") // We check the type on initialization.
        final AggregateStorage<I> result = (AggregateStorage<I>) getStorage();
        return result;
    }

    /**
     * Loads or creates an aggregate by the passed ID.
     *
     * @param id the ID of the aggregate
     * @return loaded or created aggregate instance
     */
    @VisibleForTesting
    A loadOrCreate(I id) {
        final Optional<AggregateStateRecord> eventsFromStorage = aggregateStorage().read(id);
        final A result = create(id);

        if (eventsFromStorage.isPresent()) {
            final AggregateStateRecord aggregateStateRecord = eventsFromStorage.get();
            final AggregateTransaction tx = AggregateTransaction.start(result);
            result.play(aggregateStateRecord);
            tx.commit();
        }

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
     * Loads the aggregate by the passed ID.
     *
     * <p>If the aggregate is not available in the repository this method
     * returns a newly created aggregate.
     *
     * <p>If the aggregate has at least one {@linkplain LifecycleFlags lifecycle flag} set
     * {@code Optional.absent()} is returned.
     *
     * @param id the ID of the aggregate to load
     * @return the loaded object
     * @throws IllegalStateException if the repository wasn't configured
     *                               prior to calling this method
     * @see AggregateStateRecord
     */
    @Override
    public Optional<A> find(I id) throws IllegalStateException {
        final Optional<LifecycleFlags> loadedFlags = aggregateStorage().readLifecycleFlags(id);
        if (loadedFlags.isPresent()) {
            final boolean isVisible = isEntityVisible().apply(loadedFlags.get());
            // If there is a flag that hides the aggregate, return nothing.
            if (!isVisible) {
                return Optional.absent();
            }
        }

        A result = loadOrCreate(id);
        return Optional.of(result);
    }

    /** The EventBus to which we post events produced by aggregates. */
    private EventBus getEventBus() {
        return getBoundedContext().getEventBus();
    }

    /** The Stand instance for sending updated aggregate states. */
    private Stand getStand() {
        return getBoundedContext().getStand();
    }
}
