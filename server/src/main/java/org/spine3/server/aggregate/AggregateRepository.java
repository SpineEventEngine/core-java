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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import org.spine3.Internal;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.server.BoundedContext;
import org.spine3.server.MultiHandler;
import org.spine3.server.Repository;
import org.spine3.server.internal.CommandHandlerMethod;
import org.spine3.server.internal.CommandHandlingObject;
import org.spine3.server.storage.AggregateEvents;
import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.StorageFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.propagate;

/**
 * {@code AggregateRepository} manages instances of {@link Aggregate} of the type
 * specified as the generic parameter.
 *
 * <p>This class is made abstract for preserving type information of aggregate ID and
 * aggregate classes used by implementations. A simple repository class looks like this:
 * <pre>
 * public class OrderRepository extends AggregateRepository<OrderId, OrderAggregate> {
 *     public OrderRepository(BoundedContext boundedContext) {
 *         super(boundedContext);
 *     }
 * }
 * </pre>
 *
 * @param <I> the type of the aggregated root id
 * @param <A> the type of the aggregated root
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public abstract class AggregateRepository<I, A extends Aggregate<I, ?>> extends Repository<I, A>
        implements MultiHandler, CommandHandlingObject {

    /**
     * Default number of events to be stored before a next snapshot is made.
     */
    public static final int DEFAULT_SNAPSHOT_TRIGGER = 100;

    /**
     * The name of the method used for dispatching commands to aggregate roots.
     * <p/>
     * <p>This constant is used for obtaining {@code Method} instance via reflection.
     *
     * @see #dispatch(Message, CommandContext)
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static final String DISPATCH_METHOD_NAME = "dispatch";

    /**
     * The number of events to store between snapshots.
     */
    private int snapshotTrigger = DEFAULT_SNAPSHOT_TRIGGER;

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
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @return a multimap from command handlers to command classes they handle.
     */
    @Override
    public Multimap<Method, Class<? extends Message>> getHandlers() {
        final Class<? extends Aggregate> aggregateClass = getEntityClass();
        final Set<Class<? extends Message>> aggregateCommands = Aggregate.getCommandClasses(aggregateClass);
        final Method dispatch = dispatchMethod();
        return ImmutableMultimap.<Method, Class<? extends Message>>builder()
                .putAll(dispatch, aggregateCommands)
                .build();
    }

    /**
     * Returns the reference to the method {@link #dispatch(Message, CommandContext)} of this repository.
     */
    private Method dispatchMethod() {
        try {
            return getClass().getMethod(DISPATCH_METHOD_NAME, Message.class, CommandContext.class);
        } catch (NoSuchMethodException e) {
            throw propagate(e);
        }
    }

    @Override
    @Internal
    public CommandHandlerMethod createMethod(Method method) {
        return new AggregateRepositoryDispatchMethod(this, method);
    }

    @Override
    @Internal
    public Predicate<Method> getHandlerMethodPredicate() {
        return AggregateCommandHandler.IS_AGGREGATE_COMMAND_HANDLER;
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
        final AggregateEvents aggregateEvents = aggregateStorage().load(id);

        try {
            final Snapshot snapshot = aggregateEvents.hasSnapshot()
                    ? aggregateEvents.getSnapshot()
                    : null;
            final A result = create(id);
            final List<EventRecord> events = aggregateEvents.getEventRecordList();

            if (snapshot != null) {
                result.restore(snapshot);
            }

            result.play(events);

            return result;
        } catch (InvocationTargetException e) {
            throw propagate(e);
        }
    }

    /**
     * Stores the passed aggregate root and commits its uncommitted events.
     *
     * @param aggregateRoot an instance to store
     */
    @Override
    public void store(A aggregateRoot) {
        final Iterable<EventRecord> uncommittedEvents = aggregateRoot.getStateChangingUncommittedEvents();
        final int snapshotTrigger = getSnapshotTrigger();
        int eventCount = 0;
        for (EventRecord event : uncommittedEvents) {
            storeEvent(event);
            ++eventCount;

            if (eventCount > snapshotTrigger) {
                createAndStoreSnapshot(aggregateRoot);
                eventCount = 0;
            }
        }

        aggregateRoot.commitEvents();
    }

    private void createAndStoreSnapshot(A aggregateRoot) {
        final Snapshot snapshot = aggregateRoot.toSnapshot();
        final I aggregateRootId = aggregateRoot.getId();
        aggregateStorage().store(aggregateRootId, snapshot);
    }

    private void storeEvent(EventRecord event) {
        aggregateStorage().store(event);
    }

    /**
     * Processes the command by dispatching it to a method of an aggregate.
     * <p/>
     * <p>For more details on writing aggregate commands please see
     * <a href="http://github.com/SpineEventEngine/core/wiki/Writing-Aggregate-Commands">“Writing Aggregate Commands”</a>.
     *
     * @param command the command to dispatch
     * @param context context info of the command
     * @return a list of the event records
     * @throws InvocationTargetException if an exception occurs during command dispatching
     * @see <a href="http://github.com/SpineEventEngine/core/wiki/Writing-Aggregate-Commands">Writing Aggregate Commands</a>
     */
    @CheckReturnValue
    @SuppressWarnings("unused") // because the method is used by Reflection.
    public List<EventRecord> dispatch(Message command, CommandContext context) throws InvocationTargetException {
        final I aggregateId = getAggregateId(command);
        final A aggregate = load(aggregateId);

        aggregate.dispatch(command, context);

        final List<EventRecord> eventRecords = aggregate.getUncommittedEvents();

        store(aggregate);
        return eventRecords;
    }

    @SuppressWarnings("unchecked")
    // We cast to this type because assume that all commands for our aggregate refer to ID of the same type <I>.
    // If this assumption fails, we would get ClassCastException.
    // To double check this we need to check all the aggregate commands for the presence of the ID field and
    // correctness of the type on compile time.
    private I getAggregateId(Message command) {
        return (I) AggregateId.fromCommand(command).value();
    }
}
