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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.aggregate.error.MissingEventApplierException;
import org.spine3.server.command.CommandHandler;
import org.spine3.server.entity.Entity;
import org.spine3.server.event.EventBus;
import org.spine3.server.reflect.CommandHandlerMethod;
import org.spine3.server.reflect.MethodRegistry;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Events.createEvent;
import static org.spine3.base.Events.generateId;
import static org.spine3.base.Events.getMessage;
import static org.spine3.base.Identifiers.idToAny;
import static org.spine3.protobuf.Timestamps.getCurrentTime;
import static org.spine3.server.reflect.Classes.getHandledMessageClasses;
import static org.spine3.util.Exceptions.wrappedCause;
import static org.spine3.validate.Validate.checkPositive;

/**
 * Abstract base for aggregates.
 *
 * <p>Aggregate is the main building block of a business model. Aggregates guarantee consistency of data modifications
 * in response to commands they receive. Aggregate is the most common case of {@link CommandHandler}.
 *
 * <p>An aggregate modifies its state in response to a command and produces one or more events.
 * These events are used later to restore the state of the aggregate.
 *
 * <h2>Creating aggregate class</h2>
 *
 * <p>In order to create a new aggregate class you need to:
 * <ol>
 *     <li>Select a type for identifiers of the aggregates. If you select to use a typed identifier
 *     (which is recommended), you need to define a protobuf message for the ID type.
 *     <li>Define aggregate state structure as a protobuf message.
 *     <li>Generate Java code for ID and state types.
 *     <li>Create new aggregate class derived from {@code Aggregate} passing ID and state types.
 * </ol>
 *
 * <h2>Adding command handler methods</h2>
 *
 * <p>Command handling methods of an aggregate are defined in the same way as described in {@link CommandHandler}.
 *
 * <p>Event(s) returned by command handler methods are posted to the {@link EventBus} automatically.
 *
 * <h2>Adding event applier methods</h2>
 *
 * <p>Aggregate data is stored as sequence of events it produces. The state of the aggregate
 * is restored by re-playing the history of events and invoking corresponding <em>event applier methods</em>.
 *
 * <p>An event applier is a method that changes the state of the aggregate in response to an event. The aggregate
 * must have applier methods for <em>all</em> event types it produces. An event applier takes a single parameter
 * of the event message it handles and returns {@code void}.
 *
 * <p>The modification of the state is done via a builder instance obtained from {@link #getBuilder()}.
 *
 * @param <I> the type for IDs of this class of aggregates
 * @param <S> the type of the state held by the aggregate
 * @param <B> the type of the aggregate state builder
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 */
public abstract class Aggregate<I, S extends Message, B extends Message.Builder> extends Entity<I, S> {

    /**
     * The builder for the aggregate state.
     *
     * <p>This field is non-null only when the aggregate changes its state during command handling or playing events.
     *
     * @see #createBuilder()
     * @see #getBuilder()
     * @see #updateState()
     */
    @Nullable
    private volatile B builder;

    /** Cached value of the ID in the form of {@code Any} instance. */
    private final Any idAsAny;

    /**
     * Events generated in the process of handling commands that were not yet committed.
     *
     * @see #commitEvents()
     */
    private final List<Event> uncommittedEvents = Lists.newLinkedList();

    /**
     * Creates a new aggregate instance.
     *
     * <p>Constructors of aggregates should have package access level because of the following reasons:
     * <ol>
     *     <li>Aggregate constructors are not public API of an application. Commands and aggregate IDs are.
     *     <li>These constructors need to be accessible from tests in the same package.
     * </ol>
     *
     * <p>Because of the last reason consider annotating constructors with {@code @VisibleForTesting}.
     * The package access is needed only for tests. Otherwise aggregate constructors (that are invoked
     * by {@link AggregateRepository} via Reflection) may be left {@code private}.
     *
     * @param id the ID for the new aggregate
     * @throws IllegalArgumentException if the ID is not of one of the supported types
     */
    public Aggregate(I id) {
        super(id);
        this.idAsAny = idToAny(id);
    }

    /**
     * Returns the set of the command classes handled by the passed aggregate class.
     *
     * @param clazz the class of the aggregate
     * @return immutable set of command classes
     */
    @CheckReturnValue
    static ImmutableSet<Class<? extends Message>> getCommandClasses(Class<? extends Aggregate> clazz) {
        return getHandledMessageClasses(clazz, CommandHandlerMethod.PREDICATE);
    }

    /**
     * Returns the set of the event classes that comprise the state of the passed aggregate class.
     *
     * @param clazz the class of the aggregate
     * @return immutable set of event classes
     */
    static ImmutableSet<Class<? extends Message>> getEventClasses(Class<? extends Aggregate> clazz) {
        return getHandledMessageClasses(clazz, EventApplierMethod.PREDICATE);
    }

    private Any getIdAsAny() {
        return idAsAny;
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
        @SuppressWarnings("unchecked") // It is safe to assume that correct builder type is passed to aggregate,
         // because otherwise it won't be possible to write the code of applier methods that make sense to the
         // aggregate.
        final S newState = (S) getBuilder().build();
        setState(newState, getVersion(), whenModified());

        this.builder = null;
    }

    /**
     * Dispatches the passed command to appropriate handler.
     *
     * <p>As the result of this method call, the aggregate generates event messages and applies them to the aggregate.
     *
     * <p>The returned list of event messages is used only for testing purposes. An {@link AggregateRepository} obtains
     * events generated by the aggregate via {@link #getUncommittedEvents()}.
     *
     * @param command the command message to be executed on the aggregate.
     *                If this parameter is passed as {@link Any} the enclosing message will be unwrapped.
     * @param context the context of the command
     * @return event messages generated by the aggregate
     * @throws RuntimeException if an exception occurred during command dispatching with this exception as the cause
     * @see #dispatchForTest(Message, CommandContext)
     */
    List<? extends Message> dispatch(Message command, CommandContext context) {
        checkNotNull(command);
        checkNotNull(context);

        final Message commandMessage = ensureCommandMessage(command);

        try {
            final List<? extends Message> events = invokeHandler(commandMessage, context);
            apply(events, context);
            return events;
        } catch (InvocationTargetException e) {
            throw wrappedCause(e);
        }
    }

    /**
     * Ensures that the passed instance of {@code Message} is not an {@code Any},
     * and unwraps the command message if {@code Any} is passed.
     */
    private static Message ensureCommandMessage(Message command) {
        Message commandMessage;
        if (command instanceof Any) {
            // We're likely getting the result of command.getMessage(), and the called did not bother to unwrap it.
            // Extract the wrapped message (instead of treating this as an error). There may be many occasions of
            // such a call especially from the testing code.
            final Any any = (Any) command;
            commandMessage = AnyPacker.unpack(any);
        } else {
            commandMessage = command;
        }
        return commandMessage;
    }

    /**
     * This method is provided <em>only</em> for the purpose of testing command handling
     * of an aggregate and must not be called from the production code.
     *
     * <p>The production code uses the method {@link #dispatch(Message, CommandContext)},
     * which is called automatically by {@link AggregateRepository}.
     */
    @VisibleForTesting
    public final List<? extends Message> dispatchForTest(Message command, CommandContext context) {
        return dispatch(command, context);
    }

    /**
     * Directs the passed command to the corresponding command handler method of the aggregate.
     *
     * @param commandMessage the command to be processed
     * @param context the context of the command
     * @return a list of the event messages that were produced as the result of handling the command
     * @throws InvocationTargetException if an exception occurs during command handling
     */
    private List<? extends Message> invokeHandler(Message commandMessage, CommandContext context)
            throws InvocationTargetException {
        final Class<? extends Message> commandClass = commandMessage.getClass();
        final CommandHandlerMethod method = MethodRegistry.getInstance()
                                                          .get(getClass(),
                                                               commandClass,
                                                               CommandHandlerMethod.factory());
        if (method == null) {
            throw missingCommandHandler(commandClass);
        }
        final List<? extends Message> result = method.invoke(this, commandMessage, context);
        return result;
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
     * Plays passed events on the aggregate.
     *
     * <p>The events passed to this method is the aggregates data loaded by a repository and passed
     * to the aggregate so that it restores its state.
     *
     * @param events the list of the aggregate events
     * @throws RuntimeException if applying events caused an exception. This exception is set as the {@code cause}
     *                          for the thrown {@code RuntimeException}
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
     * Applies event messages to the aggregate.
     *
     * @param events the event message to apply
     * @param commandContext the context of the command, execution of which produces the passed events
     * @throws InvocationTargetException if an exception occurs during event applying
     */
    private void apply(Iterable<? extends Message> events, CommandContext commandContext)
            throws InvocationTargetException {
        createBuilder();
        try {
            for (Message message : events) {
                apply(message, commandContext);
            }
        } finally {
            updateState();
        }
    }

    private void apply(Message eventOrMsg, CommandContext commandContext) throws InvocationTargetException {
        final Message eventMsg;
        final EventContext eventContext;
        final Timestamp currentTime = getCurrentTime();
        if (eventOrMsg instanceof Event) {
            // We are receiving the event during import or integration. This happened because
            // an aggregate's command handler returned either List<Event> or Event.
            final Event event = (Event) eventOrMsg;
            eventMsg = getMessage(event);
            eventContext = event.getContext()
                                .toBuilder()
                                .setCommandContext(commandContext)
                                .setTimestamp(currentTime)
                                .setVersion(getVersion())
                                .build();
        } else {
            eventMsg = eventOrMsg;
            eventContext = createEventContext(eventMsg, commandContext, currentTime);
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
     * Creates a context for an event message.
     *
     * <p>The context may optionally have custom attributes added by
     * {@link #extendEventContext(Message, EventContext.Builder, CommandContext)}.
     *
     *
     * @param event          the event for which to create the context
     * @param commandContext the context of the command, execution of which produced the event
     * @param whenModified   the time when an aggregate was modified
     * @return new instance of the {@code EventContext}
     * @see #extendEventContext(Message, EventContext.Builder, CommandContext)
     */
    @CheckReturnValue
    protected EventContext createEventContext(Message event, CommandContext commandContext, Timestamp whenModified) {
        checkPositive(whenModified, "Aggregate modification time");
        final EventId eventId = generateId();
        final EventContext.Builder builder = EventContext.newBuilder()
                .setEventId(eventId)
                .setTimestamp(whenModified)
                .setCommandContext(commandContext)
                .setProducerId(getIdAsAny())
                .setVersion(getVersion());
        extendEventContext(event, builder, commandContext);
        return builder.build();
    }

    /**
     * Adds custom attributes to an event context builder during the creation of the event context.
     *
     * <p>Does nothing by default. Override this method if you want to add custom attributes to the created context.
     *
     * @param event          the event message
     * @param builder        a builder for the event context
     * @see #createEventContext(Message, CommandContext, Timestamp)
     */
    @SuppressWarnings({"NoopMethodInAbstractClass", "UnusedParameters"}) // Have no-op method to avoid forced overriding.
    protected void extendEventContext(Message event, EventContext.Builder builder, CommandContext commandContext) {
        // Do nothing.
    }

    /**
     * Transforms the current state of the aggregate into the {@link Snapshot} instance.
     *
     * @return new snapshot
     */
    @CheckReturnValue
    Snapshot toSnapshot() {
        final Any state = AnyPacker.pack(getState());
        final int version = getVersion();
        final Timestamp whenModified = whenModified();
        final Snapshot.Builder builder = Snapshot.newBuilder()
                .setState(state)
                .setWhenModified(whenModified)
                .setVersion(version)
                .setTimestamp(getCurrentTime());
        return builder.build();
    }

    // Factory methods for exceptions
    //------------------------------------

    private IllegalStateException missingCommandHandler(Class<? extends Message> commandClass) {
        return new IllegalStateException(
                String.format("Missing handler for command class %s in aggregate class %s.",
                        commandClass.getName(), getClass().getName()));
    }

    private IllegalStateException missingEventApplier(Class<? extends Message> eventClass) {
        return new IllegalStateException(
                String.format("Missing event applier for event class %s in aggregate class %s.",
                        eventClass.getName(), getClass().getName()));
    }
}
