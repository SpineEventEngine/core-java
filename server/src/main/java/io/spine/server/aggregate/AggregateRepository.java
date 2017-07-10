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
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.Repository;
import io.spine.server.entity.idfunc.GetTargetIdFromCommand;
import io.spine.server.entity.idfunc.IdCommandFunction;
import io.spine.server.event.EventBus;
import io.spine.server.stand.Stand;
import io.spine.server.storage.Storage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.CommandOperation;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static io.spine.server.aggregate.AggregateCommandEndpoint.createFor;
import static io.spine.server.entity.AbstractEntity.createEntity;
import static io.spine.server.entity.AbstractEntity.getConstructor;
import static io.spine.server.entity.EntityWithLifecycle.Predicates.isEntityVisible;

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

    /** The number of events to store between snapshots. */
    private int snapshotTrigger = DEFAULT_SNAPSHOT_TRIGGER;

    /** The set of command classes dispatched to aggregates by this repository. */
    @Nullable
    private Set<CommandClass> messageClasses;

    /**
     * Creates a new instance.
     */
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
        boundedContext.getCommandBus()
                      .register(this);
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

                store(aggregate);
                getStand().post(aggregate, command.getContext());

                postEvents(events);
            }
        };
        op.execute();
    }


    /**
     * Posts passed events to {@link EventBus}.
     */
    private void postEvents(Iterable<Event> events) {
        getEventBus().post(events);
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
            checkAggregateStateRecord(aggregateStateRecord);
            final AggregateTransaction tx = AggregateTransaction.start(result);
            result.play(aggregateStateRecord);
            tx.commit();
        }

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

    /**
     * Ensures that the {@link AggregateStateRecord} is valid.
     *
     * <p>{@link AggregateStateRecord} is considered valid when one of the following is true:
     * <ul>
     *     <li>{@linkplain AggregateStateRecord#getSnapshot() snapshot} is not default;
     *     <li>{@linkplain AggregateStateRecord#getEventList() event list} is not empty.
     * </ul>
     *
     * @param aggregateStateRecord the record to check
     * @throws IllegalStateException if the {@link AggregateStateRecord} is not valid
     */
    private static void checkAggregateStateRecord(AggregateStateRecord aggregateStateRecord) {
        final boolean snapshotIsNotSet =
                aggregateStateRecord.getSnapshot().equals(Snapshot.getDefaultInstance());

        if (snapshotIsNotSet && aggregateStateRecord.getEventList()
                                                    .isEmpty()) {
            throw new IllegalStateException("AggregateStateRecord instance should have either "
                                                    + "snapshot or non-empty event list.");
        }
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
