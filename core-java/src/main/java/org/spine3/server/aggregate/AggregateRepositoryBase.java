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
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import org.spine3.CommandClass;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.server.RepositoryBase;
import org.spine3.server.internal.CommandHandlerMethod;
import org.spine3.server.storage.AggregateEvents;
import org.spine3.server.storage.AggregateStorage;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.propagate;

/**
 * Abstract base for aggregate root repositories.
 *
 * @param <A> the type of the aggregated root
 * @param <I> the type of the aggregated root id
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods") // we can not have instances of AbstractRepository.
public abstract class AggregateRepositoryBase<I extends Message,
                                              A extends Aggregate<I, ?>>
        extends RepositoryBase<I, A> implements AggregateRepository<I, A> {

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
     * The number of events to store between snapshots.
     */
    private int snapshotTrigger = DEFAULT_SNAPSHOT_TRIGGER;

    /**
     * The counter of event stored since last snapshot.
     */
    private int countSinceLastSnapshot;

    protected AggregateRepositoryBase() {
        super();
    }

    @SuppressWarnings("RefusedBequest") // We override to check our type of storage.
    protected void checkStorageClass(Object storage) {
        @SuppressWarnings({"unused", "unchecked"})
        AggregateStorage<I> ignored = (AggregateStorage<I>)storage;
    }

    public int getSnapshotTrigger() {
        return this.snapshotTrigger;
    }

    @SuppressWarnings("unused")
    public void setSnapshotTrigger(int snapshotTrigger) {
        checkArgument(snapshotTrigger > 0);
        this.snapshotTrigger = snapshotTrigger;
    }

    private AggregateStorage<I> aggregateStorage() {
        @SuppressWarnings("unchecked") // We check the type on initialization.
        final AggregateStorage<I> result = (AggregateStorage<I>) getStorage();
        return result;
    }

    @Override
    public Multimap<Method, Class<? extends Message>> getHandlers() {
        Class<? extends Aggregate> aggregateClass = TypeInfo.getEntityClass(getClass());
        Set<Class<? extends Message>> aggregateCommands = Aggregate.getCommandMessageClasses(aggregateClass);
        Method dispatch = dispatchAsMethod();
        final ImmutableMultimap.Builder<Method, Class<? extends Message>> builder = ImmutableMultimap.<Method, Class<? extends Message>>builder()
                .putAll(dispatch, aggregateCommands);
        return builder.build();
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

        Class<? extends Aggregate> aggregateClass = TypeInfo.getEntityClass(getClass());
        Set<CommandClass> aggregateCommands = Aggregate.getCommandClasses(aggregateClass);

        CommandHandlerMethod handler = dispatchAsHandler();
        for (CommandClass commandClass : aggregateCommands) {
            result.put(commandClass, handler);
        }
        return result;
    }

    /**
     * Returns the reference to the method {@link #dispatch(Message, CommandContext)} of this repository.
     */
    private Method dispatchAsMethod() {
        try {
            return getClass().getMethod(DISPATCH_METHOD_NAME, Message.class, CommandContext.class);
        } catch (NoSuchMethodException e) {
            throw propagate(e);
        }
    }

    /**
     * Returns the reference to the method {@link #dispatch(Message, CommandContext)} of this repository.
     */
    private CommandHandlerMethod dispatchAsHandler() {
        Method method = dispatchAsMethod();
        final CommandHandlerMethod result = new CommandHandlerMethod(this, method);
        return result;
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
    public A load(I id) throws IllegalStateException {
        final AggregateEvents aggregateEvents = aggregateStorage().load(id);

        try {
            Snapshot snapshot = aggregateEvents.hasSnapshot()
                        ? aggregateEvents.getSnapshot()
                        : null;
            if (snapshot != null) {
                List<EventRecord> trail = aggregateEvents.getEventRecordList();
                A result = create(id);
                result.restore(snapshot);
                result.play(trail);
                return result;
            } else {
                List<EventRecord> events = aggregateEvents.getEventRecordList();
                A result = create(id);
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
    public void store(A aggregateRoot) {
        final List<EventRecord> uncommittedEvents = aggregateRoot.getUncommittedEvents();
        final int snapshotTrigger = getSnapshotTrigger();
        for (EventRecord event : uncommittedEvents) {
            storeEvent(event);

            if (countSinceLastSnapshot > snapshotTrigger) {
                createAndStoreSnapshot(aggregateRoot);
                countSinceLastSnapshot = 0;
            }
        }

        aggregateRoot.commitEvents();
    }

    private void createAndStoreSnapshot(A aggregateRoot) {
        Snapshot snapshot = aggregateRoot.toSnapshot();
        final I aggregateRootId = aggregateRoot.getId();
        aggregateStorage().store(aggregateRootId, snapshot);
    }

    private void storeEvent(EventRecord event) {
        aggregateStorage().store(event);
        ++countSinceLastSnapshot;
    }

    @Override
    public List<EventRecord> dispatch(Message command, CommandContext context) throws InvocationTargetException {
        I aggregateId = getAggregateId(command);
        A aggregateRoot = load(aggregateId);

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
