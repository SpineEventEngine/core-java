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
import org.spine3.base.CommandId;
import org.spine3.base.Errors;
import org.spine3.base.Event;
import org.spine3.base.FailureThrowable;
import org.spine3.base.Stringifiers;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.storage.AggregateEvents;
import org.spine3.server.aggregate.storage.Predicates;
import org.spine3.server.aggregate.storage.Snapshot;
import org.spine3.server.command.CommandDispatcher;
import org.spine3.server.command.CommandStatusService;
import org.spine3.server.entity.Repository;
import org.spine3.server.entity.idfunc.GetTargetIdFromCommand;
import org.spine3.server.entity.status.EntityStatus;
import org.spine3.server.event.EventBus;
import org.spine3.server.stand.StandFunnel;
import org.spine3.server.storage.Storage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.type.CommandClass;

import javax.annotation.CheckReturnValue;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Commands.getMessage;
import static org.spine3.server.entity.Entity.STATE_CLASS_GENERIC_INDEX;
import static org.spine3.server.reflect.Classes.getGenericParameterType;
import static org.spine3.validate.Validate.isNotDefault;

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
    public static final int DEFAULT_SNAPSHOT_TRIGGER = 100;

    private final GetTargetIdFromCommand<I, Message> getIdFunction = GetTargetIdFromCommand.newInstance();
    private final CommandStatusService commandStatusService;
    private final EventBus eventBus;
    private final StandFunnel standFunnel;

    /** The number of events to store between snapshots. */
    private int snapshotTrigger = DEFAULT_SNAPSHOT_TRIGGER;

    /**
     * Creates a new repository instance.
     *
     * @param boundedContext the bounded context to which this repository belongs
     */
    protected AggregateRepository(BoundedContext boundedContext) {
        super(boundedContext);
        this.commandStatusService = boundedContext.getCommandBus()
                                                  .getCommandStatusService();
        this.eventBus = boundedContext.getEventBus();
        this.standFunnel = boundedContext.getStandFunnel();
    }

    /**
     * Returns the class of aggregates managed by this repository.
     *
     * <p>This is convenience method, which redirects to {@link #getEntityClass()}.
     *
     * @return the class of the aggregates
     */
    public Class<? extends Aggregate<I, ?, ?>> getAggregateClass() {
        return getEntityClass();
    }

    public Class<? extends Message> getAggregateStateClass() {
        final Class<? extends Aggregate<I, ?, ?>> aggregateClass = getAggregateClass();
        final Class<? extends Message> stateClass = getGenericParameterType(aggregateClass, STATE_CLASS_GENERIC_INDEX);
        return stateClass;
    }

    @Override
    public Set<CommandClass> getCommandClasses() {
        final Set<CommandClass> result = CommandClass.setOf(Aggregate.getCommandClasses(getAggregateClass()));
        return result;
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
     */
    @SuppressWarnings("OverlyBroadCatchBlock")      // the exception handling is the same for all exception types.
    @Override
    @CheckReturnValue
    public void dispatch(Command request) throws IllegalStateException {
        final Message command = getMessage(checkNotNull(request));
        final CommandContext context = request.getContext();
        final CommandId commandId = context.getCommandId();
        final I aggregateId = getAggregateId(command);
        A aggregate = loadAndDispatch(aggregateId, commandId, command, context);

        final List<Event> events = aggregate.getUncommittedEvents();
        try {
            store(aggregate);
            standFunnel.post(aggregate);
        } catch (Exception e) {
            commandStatusService.setToError(commandId, e);
        }
        postEvents(events);
        commandStatusService.setOk(commandId);
    }

    private I getAggregateId(Message command) {
        final I id = getIdFunction.apply(command, CommandContext.getDefaultInstance());
        return id;
    }

    /** Posts passed events to {@link EventBus}. */
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
    public int getSnapshotTrigger() {
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
     * Loads or creates an aggregate by the passed ID.
     *
     * @param id the ID of the aggregate
     * @return loaded or created aggregate instance
     */
    @VisibleForTesting
    A loadOrCreate(I id) {
        @SuppressWarnings("OptionalGetWithoutIsPresent") // The storage always returns events.
        final AggregateEvents aggregateEvents = aggregateStorage().read(id).get();
        final Snapshot snapshot = aggregateEvents.getSnapshot();
        final A result = create(id);
        final List<Event> events = aggregateEvents.getEventList();
        if (isNotDefault(snapshot)) {
            result.restore(snapshot);
        }
        result.play(events);
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
                storage.write(id, snapshot);
                eventCount = 0;
            }
        }
        aggregate.commitEvents();
        storage.writeEventCountAfterLastSnapshot(id, eventCount);
    }

    /**
     * Loads the aggregate by the passed ID.
     *
     * <p>If the aggregate is not available in the repository this method returns
     * a newly created aggregate.
     *
     * <p>If the aggregate is “invisible” to regular queries, {@code Optional.absent()}
     * is returned
     *
     * @param id the ID of the aggregate to load
     * @return the loaded object
     * @throws IllegalStateException if the repository wasn't configured prior to calling this method
     * @see AggregateEvents
     */
    @Override
    public Optional<A> load(I id) throws IllegalStateException {
        final Optional<EntityStatus> status = aggregateStorage().readStatus(id);
        if (status.isPresent()) {
            if (!Predicates.isVisible().apply(status.get())) {
                return Optional.absent();
            }
        }

        A result = loadOrCreate(id);
        return Optional.of(result);
    }

    @Override
    protected Storage createStorage(StorageFactory factory) {
        final Storage result = factory.createAggregateStorage(getAggregateClass());
        return result;
    }

    /**
     * Loads an aggregate and dispatches the command to it.
     *
     * <p>During the command dispatching and event applying, the original list of events may
     * have been changed by other actors in the system.
     *
     * <p>To ensure the resulting {@code Aggregate} state is consistent with the numerous
     * concurrent actor changes, the event count from the last snapshot should remain the same
     * during the {@link AggregateRepository#load(Object)}
     * and {@link Aggregate#dispatch(Message, CommandContext)}.
     *
     * <p>In case the new events are detected, {@code Aggregate} loading and {@code Command}
     * dispatching is repeated from scratch.
     */
    private A loadAndDispatch(I aggregateId, CommandId commandId, Message command, CommandContext context) {
        final AggregateStorage<I> aggregateStorage = aggregateStorage();
        A aggregate;

        Integer eventCountBeforeSave = null;
        int eventCountBeforeDispatch = 0;
        do {
            if (eventCountBeforeSave != null) {
                final int newEventCount = eventCountBeforeSave - eventCountBeforeDispatch;
                logConcurrentModification(aggregateId, command, newEventCount);
            }

            checkCurrentStatus(aggregateId);

            eventCountBeforeDispatch = aggregateStorage.readEventCountAfterLastSnapshot(aggregateId);

            aggregate = loadOrCreate(aggregateId);

            try {
                aggregate.dispatch(command, context);
            } catch (RuntimeException e) {
                updateCommandStatus(commandId, e);
            }

            eventCountBeforeSave = aggregateStorage.readEventCountAfterLastSnapshot(aggregateId);
        } while (eventCountBeforeDispatch != eventCountBeforeSave);

        return aggregate;
    }

    /**
     * Ensures that the aggregate with the passed ID is visible to queries.
     *
     * @param aggregateId the ID of the aggregate to check
     * @throws IllegalStateException if the aggregate is “invisible”
     */
    private void checkCurrentStatus(I aggregateId) {
        final Optional<EntityStatus> status = aggregateStorage().readStatus(aggregateId);

        if (status.isPresent()) {
            final EntityStatus currentStatus = status.get();
            if (currentStatus.getArchived()) {
                throw new IllegalStateException(String.format("The aggregate (ID: %s) is archived.", aggregateId));
            }
            if (currentStatus.getDeleted()) {
                throw new IllegalStateException(String.format("The aggregate (ID: %s) is deleted.", aggregateId));
            }
        }
    }

    private void logConcurrentModification(I aggregateId, Message command, int newEventCount) {
        final String idStr = Stringifiers.idToString(aggregateId);
        final Class<? extends Aggregate<I, ?, ?>> aggregateClass = getAggregateClass();
        log().warn("Detected the concurrent modification of {} ID: {}. " +
                           "New events detected while dispatching the command {} " +
                           "The number of new events is {}. " +
                           "Restarting the command dispatching.",
                   aggregateClass, idStr, command, newEventCount);
    }

    @SuppressWarnings("ChainOfInstanceofChecks") // OK for this rare case of handing an exception.
    private void updateCommandStatus(CommandId commandId, RuntimeException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof Exception) {
            final Exception exception = (Exception) cause;
            commandStatusService.setToError(commandId, exception);
        } else if (cause instanceof FailureThrowable) {
            final FailureThrowable failure = (FailureThrowable) cause;
            commandStatusService.setToFailure(commandId, failure);
        } else {
            commandStatusService.setToError(commandId, Errors.fromThrowable(cause));
        }
    }

    @Override
    protected boolean markArchived(I id) {
        return aggregateStorage().markArchived(id);
    }

    @Override
    protected boolean markDeleted(I id) {
        return aggregateStorage().markDeleted(id);
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(AggregateRepository.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
