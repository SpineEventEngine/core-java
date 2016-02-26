/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.server.BoundedContext;
import org.spine3.server.CommandDispatcher;
import org.spine3.server.command.GetTargetIdFromCommand;
import org.spine3.server.entity.Repository;
import org.spine3.server.storage.AggregateEvents;
import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.type.CommandClass;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static org.spine3.base.Commands.getMessage;

/**
 * {@code AggregateRepository} manages instances of {@link Aggregate} of the type
 * specified as the generic parameter.
 *
 * <p>This class is made abstract for preserving type information of aggregate ID and
 * aggregate classes used by implementations. A simple repository class looks like this:
 * <pre>
 * public class OrderRepository extends AggregateRepository&lt;OrderId, OrderAggregate&gt; {
 *     public OrderRepository(BoundedContext boundedContext) {
 *         super(boundedContext);
 *     }
 * }
 * </pre>
 *
 * @param <I> the type of the aggregate IDs
 * @param <A> the type of the aggregates managed by this repository
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public abstract class AggregateRepository<I, A extends Aggregate<I, ?>>
                          extends Repository<I, A>
                          implements CommandDispatcher {

    /**
     * Default number of events to be stored before a next snapshot is made.
     */
    public static final int DEFAULT_SNAPSHOT_TRIGGER = 100;

    /**
     * The number of events to store between snapshots.
     */
    private int snapshotTrigger = DEFAULT_SNAPSHOT_TRIGGER;

    private final GetTargetIdFromCommand<I, Message> getIdFunction = GetTargetIdFromCommand.newInstance();

    /**
     * {@inheritDoc}
     */
    public AggregateRepository(BoundedContext boundedContext) {
        super(boundedContext);
    }

    @Override
    protected AutoCloseable createStorage(StorageFactory factory) {
        final AutoCloseable result = factory.createAggregateStorage(getAggregateClass());
        return result;
    }

    /**
     * Returns the class of aggregates managed by this repository.
     *
     * <p>This is convenience method, which redirects to {@link #getEntityClass()}.
     *
     * @return the class of the aggregates
     */
    public Class<? extends Aggregate<I, ?>> getAggregateClass() {
        return getEntityClass();
    }

    @Override
    public Set<CommandClass> getCommandClasses() {
        final Set<CommandClass> result = CommandClass.setOf(Aggregate.getCommandClasses(getAggregateClass()));
        return result;
    }

    /**
     * Returns the number of events until a next snapshot is made.
     *
     * @return a positive integer value
     * @see #DEFAULT_SNAPSHOT_TRIGGER
     */
    @CheckReturnValue
    public int getSnapshotTrigger() {
        return this.snapshotTrigger;
    }

    /**
     * Changes the number of events between making aggregate snapshots to the passed value.
     *
     * @param snapshotTrigger a positive number of the snapshot trigger
     */
    @SuppressWarnings("unused")
    public void setSnapshotTrigger(int snapshotTrigger) {
        checkArgument(snapshotTrigger > 0);
        this.snapshotTrigger = snapshotTrigger;
    }

    protected AggregateStorage<I> aggregateStorage() {
        @SuppressWarnings("unchecked") // We check the type on initialization.
        final AggregateStorage<I> result = (AggregateStorage<I>) getStorage();
        return checkStorage(result);
    }

    /**
     * Loads the aggregate by given id.
     *
     * @param id id of the aggregate to load
     * @return the loaded object
     * @throws IllegalStateException if the repository wasn't configured prior to calling this method
     */
    @Nonnull
    @Override
    public A load(I id) throws IllegalStateException {
        final AggregateEvents aggregateEvents = aggregateStorage().read(id);

        try {
            final Snapshot snapshot = aggregateEvents.hasSnapshot()
                    ? aggregateEvents.getSnapshot()
                    : null;
            final A result = create(id);
            final List<Event> events = aggregateEvents.getEventList();

            if (snapshot != null) {
                result.restore(snapshot);
            }

            result.play(events);

            return result;
        } catch (Throwable e) {
            throw propagate(e);
        }
    }

    /**
     * Stores the passed aggregate and commits its uncommitted events.
     *
     * @param aggregate an instance to store
     */
    @Override
    public void store(A aggregate) {
        final Iterable<Event> uncommittedEvents = aggregate.getStateChangingUncommittedEvents();

        //TODO:2016-01-22:alexander.yevsyukov: The below code is not correct.
        // Now we're storing snapshot in a sequence of uncommitted
        // events, which isn't going to be the case. We need to read the number of events since the last
        // snapshot of the aggregate instead.

        final I id = aggregate.getId();
        final int snapshotTrigger = getSnapshotTrigger();
        int eventCount = 0;
        for (Event event : uncommittedEvents) {
            aggregateStorage().writeEvent(id, event);
            ++eventCount;

            if (eventCount > snapshotTrigger) {
                createAndStoreSnapshot(id, aggregate);
                eventCount = 0;
            }
        }
        aggregate.commitEvents();
    }

    private void createAndStoreSnapshot(I id, A aggregate) {
        final Snapshot snapshot = aggregate.toSnapshot();
        aggregateStorage().write(id, snapshot);
    }

    /**
     * Processes the command by dispatching it to a method of an aggregate.
     *
     * <p>The aggregate ID is obtained from the passed command.
     *
     * <p>The repository loads the aggregate by this ID, or creates a new aggregate
     * if there is no aggregate with such ID.
     *
     * @param request the request to dispatch
     * @throws IllegalStateException if storage for the repository was not initialized
     * @throws InvocationTargetException if an exception occurs during command dispatching
     */
    @Override
    @CheckReturnValue
    public List<Event> dispatch(Command request) throws IllegalStateException, InvocationTargetException {
        final Message command = getMessage(checkNotNull(request));
        final CommandContext context = request.getContext();
        final I aggregateId = getAggregateId(command);
        final A aggregate = load(aggregateId);

        try {
            aggregate.dispatch(command, context);
        } catch (Throwable throwable) {
            //TODO:2016-01-25:alexander.yevsyukov: Store error status into the Command Store.
            //TODO:2016-01-25:alexander.yevsyukov: How do we tell the client that the command caused the
            // error or (which is more important) business failure?
            return null;
        }

        final List<Event> events = aggregate.getUncommittedEvents();

        //noinspection OverlyBroadCatchBlock
        try {
            store(aggregate);
        } catch (Exception e) {
            //TODO:2016-01-25:alexander.yevsyukov: Store error into the Command Store.
            return null;
        }

        //TODO:2016-01-25:alexander.yevsyukov: Post events to EventBus.

        return events;
    }

    private I getAggregateId(Message command) {
        final I id = getIdFunction.getId(command, CommandContext.getDefaultInstance());
        return id;
    }
}
