/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import org.spine3.CommandClass;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.server.RepositoryBase;
import org.spine3.server.RepositoryEventStore;
import org.spine3.server.Snapshot;
import org.spine3.server.SnapshotStorage;
import org.spine3.server.internal.CommandHandlerMethod;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;

/**
 * Abstract base for aggregate root repositories.
 *
 * @param <R> the type of the aggregated root
 * @param <I> the type of the aggregated root id
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods") // we can not have instances of AbstractRepository.
public abstract class AggregateRootRepositoryBase<I extends Message,
                                                  R extends AggregateRoot<I, ?>>
        extends RepositoryBase<I, R> implements AggregateRootRepository<I, R> {

    /**
     * Default number of events to be stored before a next snapshot is made.
     */
    public static final int DEFAULT_SNAPSHOT_TRIGGER = 100;

    /**
     * The name of the method used for dispatching commands to aggregate roots.
     *
     * <p>This constant is used for obtaining {@code Method} instance via reflection.
     *
     * @see #dispatch(Message, CommandContext)
     */
    private static final String DISPATCH_METHOD_NAME = "dispatch";

    /**
     * The store for events and snapshots.
     */
    private final RepositoryEventStore eventStorage;

    /**
     * The storage for snapshots.
     *
     * <p>Snapshots are created each time the number of events exceeds the number
     * configured in {@link #snapshotTrigger}.
     *
     * @see #countSinceLastSnapshot
     */
    private final SnapshotStorage snapshotStorage;

    /**
     * The number of events to store between snapshots.
     */
    private int snapshotTrigger = DEFAULT_SNAPSHOT_TRIGGER;

    /**
     * The counter of event stored since last snapshot.
     */
    private int countSinceLastSnapshot;

    protected AggregateRootRepositoryBase(RepositoryEventStore eventStorage, SnapshotStorage snapshotStorage) {
        super();
        this.eventStorage = eventStorage;
        this.snapshotStorage = snapshotStorage;
    }

    public int getSnapshotTrigger() {
        return snapshotTrigger;
    }

    public void setSnapshotTrigger(int snapshotTrigger) {
        this.snapshotTrigger = snapshotTrigger;
    }

    /**
     * Creates a map which contains command handlers of the repository itself and
     * handlers dispatching commands to aggregates handled by this repository.
     *
     * @return immutable map of command handler methods
     */
    public Map<CommandClass, CommandHandlerMethod> getCommandHandlers() {
        ImmutableMap.Builder<CommandClass, CommandHandlerMethod> handlers = ImmutableMap.builder();
        handlers.putAll(getDispatchingHandlers());
        handlers.putAll(CommandHandlerMethod.scan(this));
        return handlers.build();
    }

    /**
     * Creates a map of handlers that call {@link #dispatch(Message, CommandContext)}
     * method for all commands of the aggregate root class.
     */
    private Map<CommandClass, CommandHandlerMethod> getDispatchingHandlers() {
        Map<CommandClass, CommandHandlerMethod> result = Maps.newHashMap();

        Class<? extends AggregateRoot> aggregateRootClass = TypeInfo.getEntityClass(getClass());
        Set<CommandClass> aggregateCommands = AggregateRoot.getCommandClasses(aggregateRootClass);

        CommandHandlerMethod handler = dispatchAsHandler();
        for (CommandClass commandClass : aggregateCommands) {
            result.put(commandClass, handler);
        }
        return result;
    }

    /**
     * Returns the reference to the method {@link #dispatch(Message, CommandContext)} of this repository.
     */
    private CommandHandlerMethod dispatchAsHandler() {
        try {
            Method method = getClass().getMethod(DISPATCH_METHOD_NAME, Message.class, CommandContext.class);
            final CommandHandlerMethod result = new CommandHandlerMethod(this, method);
            return result;
        } catch (NoSuchMethodException e) {
            throw propagate(e);
        }
    }

    /**
     * Loads the an aggregate by given id.
     *
     * @param id id of the aggregate to load
     * @return the loaded object
     * @throws IllegalStateException if the repository wasn't configured prior to calling this method
     */
    @Nonnull
    @Override
    public R load(I id) throws IllegalStateException {
        try {
            Snapshot snapshot = snapshotStorage.load(id);
            if (snapshot != null) {
                List<EventRecord> trail = eventStorage.getEvents(id, snapshot.getVersion());
                R result = create(id);
                result.restore(snapshot);
                result.play(trail);
                return result;
            } else {
                List<EventRecord> events = eventStorage.getAllEvents(id);
                R result = create(id);
                result.play(events);
                return result;
            }
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
    public void store(R aggregateRoot) {
        final List<EventRecord> uncommittedEvents = aggregateRoot.getUncommittedEvents();
        for (EventRecord event : uncommittedEvents) {
            storeEvent(event);

            if (countSinceLastSnapshot > snapshotTrigger) {
                createAndStoreSnapshot(aggregateRoot);
                countSinceLastSnapshot = 0;
            }
        }

        aggregateRoot.commitEvents();
    }

    private void createAndStoreSnapshot(R aggregateRoot) {
        Snapshot snapshot = aggregateRoot.toSnapshot();
        final I aggregateRootId = aggregateRoot.getId();
        snapshotStorage.store(aggregateRootId, snapshot);
    }

    private void storeEvent(EventRecord event) {
        eventStorage.store(event);
        ++countSinceLastSnapshot;
    }

    @Override
    public List<EventRecord> dispatch(Message command, CommandContext context) throws InvocationTargetException {
        I aggregateId = getAggregateId(command);
        R aggregateRoot = load(aggregateId);

        aggregateRoot.dispatch(command, context);

        final List<EventRecord> eventRecords = aggregateRoot.getUncommittedEvents();

        store(aggregateRoot);
        return eventRecords;
    }

    @SuppressWarnings("unchecked")
    // We cast to this type because assume that all commands for our aggregate refer to ID of the same type <I>.
    // If this assumption fails, we would get ClassCastException.
    // A better way would be to check all the aggregate commands for the presence of the ID field and
    // correctness of the type on compile-time.
    private I getAggregateId(Message command) {
        return (I) AggregateCommand.getAggregateId(command).value();
    }

}
