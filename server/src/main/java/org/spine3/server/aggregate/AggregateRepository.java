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
package org.spine3.server.aggregate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.CommandDispatcher;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.LifecycleFlags;
import org.spine3.server.entity.Repository;
import org.spine3.server.entity.idfunc.GetTargetIdFromCommand;
import org.spine3.server.entity.idfunc.IdCommandFunction;
import org.spine3.server.event.EventBus;
import org.spine3.server.stand.StandFunnel;
import org.spine3.server.storage.Storage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.tenant.CommandOperation;
import org.spine3.type.CommandClass;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.server.aggregate.AggregateCommandEndpoint.createFor;
import static org.spine3.server.entity.AbstractEntity.createEntity;
import static org.spine3.server.entity.AbstractEntity.getConstructor;
import static org.spine3.server.entity.EntityWithLifecycle.Predicates.isEntityVisible;

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
 *      public OrderRepository(BoundedContext boundedContext) {
 *          super(boundedContext);
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
                implements CommandDispatcher {

    /** The default number of events to be stored before a next snapshot is made. */
    static final int DEFAULT_SNAPSHOT_TRIGGER = 100;

    private final IdCommandFunction<I, Message> defaultIdFunction =
            GetTargetIdFromCommand.newInstance();

    /** The constructor for creating entity instances. */
    private Constructor<A> entityConstructor;

    /** The EventBus to which we post events produced by aggregates. */
    private final EventBus eventBus;

    /** The funnel for sending updated aggregate states to Stand. */
    private final StandFunnel standFunnel;

    /** The number of events to store between snapshots. */
    private int snapshotTrigger = DEFAULT_SNAPSHOT_TRIGGER;

    /** The set of command classes dispatched to aggregates by this repository. */
    @Nullable
    private Set<CommandClass> messageClasses;

    /**
     * Creates a new repository instance.
     *
     * @param boundedContext the bounded context to which this repository belongs
     */
    protected AggregateRepository(BoundedContext boundedContext) {
        super();
        this.eventBus = boundedContext.getEventBus();
        this.standFunnel = boundedContext.getStandFunnel();
    }

    /**
     * Obtains the constructor.
     *
     * <p>The method returns cached value if called more than once.
     * During the first call, it {@linkplain #findEntityConstructor() finds}  the constructor.
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

    @Override
    public A create(I id) {
        return createEntity(getEntityConstructor(), id);
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
     * Obtains the class of the aggregate state.
     */
    public Class<? extends Message> getAggregateStateClass() {
        final Class<? extends Aggregate<I, ?, ?>> aggregateClass = getAggregateClass();
        final Class<? extends Message> stateClass = Entity.TypeInfo.getStateClass(aggregateClass);
        return stateClass;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // We return immutable impl.
    @Override
    public Set<CommandClass> getMessageClasses() {
        if (messageClasses == null) {
            messageClasses = Aggregate.TypeInfo.getCommandClasses(getAggregateClass());
        }
        return messageClasses;
    }

    /**
     * Returns the function which obtains an aggregate ID from a command.
     *
     * <p>The default implementation takes the first field from a command message.
     *
     * <p>If your repository needs another way of getting aggregate IDs, override
     * this method returning custom implementation of {@code IdCommandFunction}.
     *
     * @return default implementation of {@code IdCommandFunction}
     */
    protected IdCommandFunction<I, Message> getIdFunction() {
        return defaultIdFunction;
    }

    I getAggregateId(Message commandMessage, CommandContext commandContext) {
        final I id = getIdFunction().apply(commandMessage, commandContext);
        return id;
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
    public void dispatch(final CommandEnvelope envelope) {
        final Command command = envelope.getCommand();
        final CommandOperation op = new CommandOperation(command) {
            @Override
            public void run() {
                final AggregateCommandEndpoint<I, A> commandEndpoint = createFor(
                        AggregateRepository.this, envelope);
                commandEndpoint.execute();

                final Optional<A> processedAggregate = commandEndpoint.getAggregate();
                if (!processedAggregate.isPresent()) {
                    throw new IllegalStateException("No aggregate loaded for command: " + command);
                }

                final A aggregate = processedAggregate.get();
                final List<Event> events = aggregate.getUncommittedEvents();
                storeAndPostToStand(aggregate);
                postEvents(events);
            }
        };
        op.execute();
    }

    private void storeAndPostToStand(A aggregate) {
        store(aggregate);
        standFunnel.post(aggregate);
    }

    /**
     * Posts passed events to {@link EventBus}.
     */
    private void postEvents(Iterable<Event> events) {
        for (Event event : events) {
            eventBus.post(event);
        }
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
        return checkStorage(result);
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
        if (!eventsFromStorage.isPresent()) {
            throw unableToLoadEvents(id);
        }
        final AggregateStateRecord aggregateStateRecord = eventsFromStorage.get();
        final A result = create(id);
        result.play(aggregateStateRecord);
        return result;
    }

    /**
     * Stores the passed aggregate and commits its uncommitted events.
     *
     * @param aggregate an instance to store
     */
    @Override
    public void store(A aggregate) {
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

    @Override
    protected Storage<I, ?> createStorage(StorageFactory factory) {
        final Storage<I, ?> result = factory.createAggregateStorage(getAggregateClass());
        return result;
    }

    private static <I> IllegalStateException unableToLoadEvents(I id) {
        final String errMsg = format(
                "Unable to load events for the aggregate with ID: %s",
                idToString(id)
        );
        throw new IllegalStateException(errMsg);
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(AggregateRepository.class);
    }

    static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
