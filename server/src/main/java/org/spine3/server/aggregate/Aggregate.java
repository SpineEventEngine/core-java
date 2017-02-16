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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.aggregate.error.MissingEventApplierException;
import org.spine3.server.aggregate.storage.Snapshot;
import org.spine3.server.command.CommandHandlingEntity;
import org.spine3.server.entity.status.EntityStatus;
import org.spine3.server.reflect.MethodRegistry;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.spine3.base.Events.createEvent;
import static org.spine3.base.Events.getMessage;
import static org.spine3.protobuf.Timestamps.getCurrentTime;
import static org.spine3.util.Exceptions.wrappedCause;

/**
 * Abstract base for aggregates.
 *
 * <p>An aggregate is the main building block of a business model.
 * Aggregates guarantee consistency of data modifications in response to
 * commands they receive.
 *
 * <p>An aggregate modifies its state in response to a command and produces
 * one or more events. These events are used later to restore the state of the
 * aggregate.
 *
 * <h2>Creating an aggregate class</h2>
 *
 * <p>In order to create a new aggregate class you need to:
 * <ol>
 *     <li>Select a type for identifiers of the aggregate.
 *      If you select to use a typed identifier (which is recommended),
 *      you need to define a protobuf message for the ID type.
 *     <li>Define the structure of the aggregate state as a Protobuf message.
 *     <li>Generate Java code for ID and state types.
 *     <li>Create new Java class derived from {@code Aggregate} passing ID and
 *     state types as generic parameters.
 * </ol>
 *
 * <h2>Adding command handler methods</h2>
 *
 * <p>Command handling methods of an {@code Aggregate} are defined in
 * the same way as described in {@link CommandHandlingEntity}.
 *
 * <p>Event(s) returned by command handling methods are posted to
 * the {@link org.spine3.server.event.EventBus} automatically by {@link AggregateRepository}.
 *
 * <h2>Adding event applier methods</h2>
 *
 * <p>Aggregate data is stored as a sequence of events it produces.
 * The state of the aggregate is restored by re-playing the history of
 * events and invoking corresponding <em>event applier methods</em>.
 *
 * <p>An event applier is a method that changes the state of the aggregate
 * in response to an event. An event applier takes a single parameter of the
 * event message it handles and returns {@code void}.
 *
 * <p>The modification of the state is done via a builder instance obtained
 * from {@link #getBuilder()}.
 *
 * <p>An {@code Aggregate} class must have applier methods for
 * <em>all</em> types of the events that it produces.
 *
 * <h2>Performance considerations for aggregate state</h2>
 *
 * <p>In order to improve performance of loading aggregates an
 * {@link AggregateRepository} periodically stores aggregate snapshots.
 * See {@link AggregateRepository#setSnapshotTrigger(int)} for details.
 *
 * @param <I> the type for IDs of this class of aggregates
 * @param <S> the type of the state held by the aggregate
 * @param <B> the type of the aggregate state builder
 *
 * @author Alexander Yevsyukov
 * @author Alexander Litus
 * @author Mikhail Melnik
 */
public abstract class Aggregate<I, S extends Message, B extends Message.Builder>
                extends CommandHandlingEntity<I, S> {

    /**
     * The builder for the aggregate state.
     *
     * <p>This field is non-null only when the aggregate changes its state
     * during command handling or playing events.
     *
     * @see #createBuilder()
     * @see #getBuilder()
     * @see #updateState()
     */
    @Nullable
    private volatile B builder;

    /**
     * Events generated in the process of handling commands that were not yet committed.
     *
     * @see #commitEvents()
     */
    private final List<Event> uncommittedEvents = Lists.newLinkedList();

    /**
     * Creates a new instance.
     *
     * <p>Constructors of derived classes should have package access level
     * because of the following reasons:
     * <ol>
     *     <li>These constructors are not public API of an application.
     *     Commands and aggregate IDs are.
     *     <li>These constructors need to be accessible from tests in the same package.
     * </ol>
     *
     * <p>Because of the last reason consider annotating constructors with
     * {@code @VisibleForTesting}. The package access is needed only for tests.
     * Otherwise aggregate constructors (that are invoked by {@link AggregateRepository}
     * via Reflection) may be left {@code private}.
     *
     * @param id the ID for the new aggregate
     */
    protected Aggregate(I id) {
        super(id);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("RedundantMethodOverride") // Expose the method to this package.
    @Override
    protected EntityStatus getStatus() {
        return super.getStatus();
    }

    /**
     * This method starts the phase of updating the aggregate state.
     *
     * <p>The update phase is closed by the {@link #updateState()}.
     */
    private void createBuilder() {
        @SuppressWarnings("unchecked") // It is safe as we checked the type on the construction.
        final B builder = (B) getState().toBuilder();
        this.builder = builder;
    }

    /**
     * Obtains the instance of the state builder.
     *
     * <p>This method must be called only from within an event applier.
     *
     * @return the instance of the new state builder
     * @throws IllegalStateException if the method is called from outside an event applier
     */
    protected B getBuilder() {
        if (this.builder == null) {
            throw new IllegalStateException(
                    "Builder is not available. Make sure to call getBuilder() only from an event applier method.");
        }
        return builder;
    }

    /** Updates the aggregate state and closes the update phase of the aggregate. */
    private void updateState() {
        @SuppressWarnings("unchecked")
         /* It is safe to assume that correct builder type is passed to the aggregate,
            because otherwise it won't be possible to write the code of applier methods
            that make sense to the aggregate. */
        final S newState = (S) getBuilder().build();
        setState(newState, getVersion().getNumber(), whenModified());
        this.builder = null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>As the result of this method call, the aggregate generates
     * event messages and applies them to compute new state.
     */
    @Override
    protected List<? extends Message> invokeHandler(Message commandMessage, CommandContext context)
            throws InvocationTargetException {
        final List<? extends Message> eventMessages = super.invokeHandler(commandMessage, context);

        apply(eventMessages, context);

        return eventMessages;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose the method to the package.
     */
    @Override
    protected List<? extends Message> dispatchCommand(Message command, CommandContext context) {
        return super.dispatchCommand(command, context);
    }

    /**
     * Invokes applier method for the passed event message.
     *
     * @param eventMessage the event message to apply
     * @throws InvocationTargetException if an exception was thrown during the method invocation
     */
    private void invokeApplier(Message eventMessage) throws InvocationTargetException {
        final EventApplierMethod method = MethodRegistry.getInstance()
                                                        .get(getClass(),
                                                             eventMessage.getClass(),
                                                             EventApplierMethod.factory());
        if (method == null) {
            throw missingEventApplier(eventMessage.getClass());
        }
        method.invoke(this, eventMessage);
    }

    /**
     * Applies passed events.
     *
     * <p>The events passed to this method is the aggregate data loaded
     * by a repository and passed to the aggregate so that it restores its state.
     *
     * @param events the list of the events
     * @throws IllegalStateException if applying events caused an exception, which is set as
     *                               the {@code cause} for the thrown instance
     */
    void play(Iterable<Event> events) {
        createBuilder();
        try {
            for (Event event : events) {
                final Message message = getMessage(event);
                final EventContext context = event.getContext();
                try {
                    applyEventOrSnapshot(message);
                    setVersion(context.getVersion(), context.getTimestamp());
                } catch (InvocationTargetException e) {
                    throw wrappedCause(e);
                }
            }
        } finally {
            updateState();
        }
    }

    /**
     * Applies event messages.
     *
     * @param eventMessages the event message to apply
     * @param commandContext the context of the command, execution of which produces the passed events
     * @throws InvocationTargetException if an exception occurs during event applying
     */
    private void apply(Iterable<? extends Message> eventMessages, CommandContext commandContext)
            throws InvocationTargetException {
        createBuilder();
        try {
            for (Message message : eventMessages) {
                apply(message, commandContext);
            }
        } finally {
            updateState();
        }
    }

    private void apply(Message eventOrMsg, CommandContext commandContext)
            throws InvocationTargetException {
        final Message eventMsg;
        final EventContext eventContext;
        if (eventOrMsg instanceof Event) {
            /* We are receiving the event during import or integration.
               This happened because an aggregate's command handler returned either
               List<Event> or Event. */
            final Event event = (Event) eventOrMsg;
            eventMsg = getMessage(event);
            eventContext = event.getContext()
                                .toBuilder()
                                .setCommandContext(commandContext)
                                .setTimestamp(getCurrentTime())
                                .setVersion(getVersion().getNumber())
                                .build();
        } else {
            eventMsg = eventOrMsg;
            eventContext = createEventContext(eventMsg, commandContext);
        }
        applyEventOrSnapshot(eventMsg);
        incrementVersion();
        final Event event = createEvent(eventMsg, eventContext);
        uncommittedEvents.add(event);
    }

    /**
     * Applies an event to the aggregate.
     *
     * <p>If the event is {@link Snapshot} its state is copied. Otherwise, the event
     * is dispatched to corresponding applier method.
     *
     * @param eventOrSnapshot an event to apply or a snapshot to use to restore state
     * @throws MissingEventApplierException if there is no applier method defined for this type of event
     * @throws InvocationTargetException    if an exception occurred when calling event applier
     */
    private void applyEventOrSnapshot(Message eventOrSnapshot) throws InvocationTargetException {
        if (eventOrSnapshot instanceof Snapshot) {
            restore((Snapshot) eventOrSnapshot);
        } else {
            invokeApplier(eventOrSnapshot);
        }
    }

    /**
     * Restores state from the passed snapshot.
     *
     * @param snapshot the snapshot with the state to restore
     */
    void restore(Snapshot snapshot) {
        final S stateToRestore = AnyPacker.unpack(snapshot.getState());

        // See if we're in the state update cycle.
        final B builder = this.builder;

        // If the call to restore() is made during a reply (because the snapshot event was encountered)
        // use the currently initialized builder.
        if (builder != null) {
            builder.clear();
            builder.mergeFrom(stateToRestore);
            setVersion(snapshot.getVersion(), snapshot.getWhenModified());
        } else {
            setState(stateToRestore, snapshot.getVersion(), snapshot.getWhenModified());
        }
    }

    /**
     * Returns all uncommitted events.
     *
     * @return immutable view of all uncommitted events
     */
    @CheckReturnValue
    List<Event> getUncommittedEvents() {
        return ImmutableList.copyOf(uncommittedEvents);
    }

    /**
     * Returns and clears all the events that were uncommitted before the call of this method.
     *
     * @return the list of events
     */
    List<Event> commitEvents() {
        final List<Event> result = ImmutableList.copyOf(uncommittedEvents);
        uncommittedEvents.clear();
        return result;
    }

    /**
     * Transforms the current state of the aggregate into the {@link Snapshot} instance.
     *
     * @return new snapshot
     */
    @CheckReturnValue
    Snapshot toSnapshot() {
        final Any state = AnyPacker.pack(getState());
        final int version = getVersion().getNumber();
        final Timestamp whenModified = whenModified();
        final Snapshot.Builder builder = Snapshot.newBuilder()
                .setState(state)
                .setWhenModified(whenModified)
                .setVersion(version)
                .setTimestamp(getCurrentTime());
        return builder.build();
    }

    private IllegalStateException missingEventApplier(Class<? extends Message> eventClass) {
        return new IllegalStateException(
                String.format("Missing event applier for event class %s in aggregate class %s.",
                        eventClass.getName(), getClass().getName()));
    }
}
