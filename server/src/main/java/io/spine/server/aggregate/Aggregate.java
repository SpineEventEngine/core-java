/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.MessageEnvelope;
import io.spine.core.RejectionEnvelope;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.protobuf.AnyPacker;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.aggregate.model.EventApplier;
import io.spine.server.command.CommandHandlingEntity;
import io.spine.server.command.dispatch.Dispatch;
import io.spine.server.command.dispatch.DispatchResult;
import io.spine.server.command.model.CommandHandlerMethod;
import io.spine.server.entity.EventPlayer;
import io.spine.server.entity.EventPlayers;
import io.spine.server.event.EventFactory;
import io.spine.server.event.model.EventReactorMethod;
import io.spine.server.model.Model;
import io.spine.server.rejection.model.RejectionReactorMethod;
import io.spine.validate.ValidatingBuilder;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.core.Events.getMessage;
import static io.spine.validate.Validate.isNotDefault;

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
 * the {@link io.spine.server.event.EventBus EventBus} automatically
 * by {@link AggregateRepository}.
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
@SuppressWarnings("OverlyCoupledClass") // OK for this central class.
public abstract class Aggregate<I,
                                S extends Message,
                                B extends ValidatingBuilder<S, ? extends Message.Builder>>
        extends CommandHandlingEntity<I, S, B>
        implements EventPlayer {

    /**
     * Events generated in the process of handling commands that were not yet committed.
     *
     * @see #commitEvents()
     */
    private final List<Event> uncommittedEvents = newLinkedList();

    /** A guard for ensuring idempotency of messages dispatched by this aggregate. */
    private IdempotencyGuard idempotencyGuard;

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
        setIdempotencyGuard();
    }

    /**
     * Creates and assigns the aggregate an {@link IdempotencyGuard idempotency guard}.
     */
    private void setIdempotencyGuard() {
        idempotencyGuard = new IdempotencyGuard(this);
    }

    /**
     * Obtains model class for this aggregate.
     */
    @Override
    protected AggregateClass<?> thisClass() {
        return (AggregateClass<?>)super.thisClass();
    }

    /**
     * Obtains the model class as {@link Model#asAggregateClass(Class) AggregateClass}.
     */
    @Override
    protected AggregateClass<?> getModelClass() {
        return Model.getInstance()
                    .asAggregateClass(getClass());
    }

    /**
     * {@inheritDoc}
     *
     * <p>In {@code Aggregate}, this method must be called only from within an event applier.
     *
     * @throws IllegalStateException if the method is called from outside an event applier
     */
    @Override
    protected B getBuilder() {
        return super.getBuilder();
    }

    /**
     * Obtains a method for the passed command and invokes it.
     *
     * <p>Dispatching the commands results in emitting event messages. All the
     * {@linkplain Empty empty} messages are filtered out from the result.
     *
     * @param  command the envelope with the command to dispatch
     * @return a list of event messages that the aggregate produces by handling the command
     */
    @Override
    protected List<? extends Message> dispatchCommand(CommandEnvelope command) {
        idempotencyGuard.check(command);
        CommandHandlerMethod method = thisClass().getHandler(command.getMessageClass());
        Dispatch<CommandEnvelope> dispatch = Dispatch.of(command)
                                                     .to(this, method);
        DispatchResult dispatchResult = dispatch.perform();
        return dispatchResult.asMessages();
    }

    /**
     * Dispatches the event on which the aggregate reacts.
     *
     * <p>Reacting on a event may result in emitting event messages. All the {@linkplain Empty empty}
     * messages are filtered out from the result.
     *
     * @param  event the envelope with the event to dispatch
     * @return a list of event messages that the aggregate produces in reaction to the event or
     *         an empty list if the aggregate state does not change in reaction to the event
     */
    List<? extends Message> reactOn(EventEnvelope event) {
        EventReactorMethod method = thisClass().getReactor(event.getMessageClass());
        Dispatch<EventEnvelope> dispatch = Dispatch.of(event)
                                                   .to(this, method);
        DispatchResult dispatchResult = dispatch.perform();
        return dispatchResult.asMessages();
    }

    /**
     * Dispatches the rejection to which the aggregate reacts.
     *
     * <p>Reacting on a rejection may result in emitting event messages. All the
     * {@linkplain Empty empty} messages are filtered out from the result.
     *
     * @param  rejection the envelope with the rejection
     * @return a list of event messages that the aggregate produces in reaction to
     *         the rejection, or an empty list if the aggregate state does not change in
     *         response to this rejection
     */
    List<? extends Message> reactOn(RejectionEnvelope rejection) {
        CommandClass commandClass = CommandClass.of(rejection.getCommandMessage());
        RejectionReactorMethod method = thisClass().getReactor(rejection.getMessageClass(),
                                                               commandClass);
        Dispatch<RejectionEnvelope> dispatch = Dispatch.of(rejection)
                                                       .to(this, method);
        DispatchResult dispatchResult = dispatch.perform();
        return dispatchResult.asMessages();
    }

    /**
     * Invokes applier method for the passed event message.
     *
     * @param eventMessage the event message to apply
     */
    void invokeApplier(Message eventMessage) {
        EventApplier method = thisClass().getApplier(EventClass.of(eventMessage));
        method.invoke(this, eventMessage);
    }

    @Override
    public void play(Iterable<Event> events) {
        EventPlayers.forTransactionOf(this)
                    .play(events);
    }

    /**
     * Applies passed events.
     *
     * <p>The events passed to this method is the aggregate data (which may include
     * a {@code Snapshot}) loaded by a repository and passed to the aggregate so that
     * it restores its state.
     *
     * @param  aggregateStateRecord the aggregate state with events to play
     * @throws IllegalStateException
     *         if applying events caused an exception, which is set as the {@code cause} for
     *         the thrown instance
     */
    void play(AggregateStateRecord aggregateStateRecord) {
        Snapshot snapshot = aggregateStateRecord.getSnapshot();
        if (isNotDefault(snapshot)) {
            restore(snapshot);
        }
        List<Event> events = aggregateStateRecord.getEventList();

        play(events);
        remember(events);
    }

    /**
     * Applies event messages.
     *
     * @param eventMessages the event messages or events to apply
     * @param origin        the envelope of a message which caused the events
     * @see #ensureEventMessage(Message)
     */
    void apply(Iterable<? extends Message> eventMessages, MessageEnvelope origin) {
        List<? extends Message> messages = newArrayList(eventMessages);
        EventFactory eventFactory = EventFactory.on(origin, getProducerId());

        List<Event> events = newArrayListWithCapacity(messages.size());

        Version projectedEventVersion = getVersion();

        for (Message eventOrMessage : messages) {
            /* Applying each message would increment the entity version.
               Therefore, we should simulate this behaviour. */
            projectedEventVersion = Versions.increment(projectedEventVersion);
            Message eventMessage = ensureEventMessage(eventOrMessage);

            Event event;
            if (eventOrMessage instanceof Event) {
                /* If we get instances of Event, it means we are dealing with an import command,
                   which contains these events in the body. So we deal with a command envelope.
                */
                CommandEnvelope ce = (CommandEnvelope)origin;
                event = importEvent((Event) eventOrMessage,
                                    ce.getCommandContext(),
                                    projectedEventVersion);
            } else {
                event = eventFactory.createEvent(eventMessage, projectedEventVersion);
            }
            events.add(event);
        }
        play(events);
        uncommittedEvents.addAll(events);
    }

    /**
     * Creates an event based on the event received in an import command.
     *
     * @param  event          the event to import
     * @param  commandContext the context of the import command
     * @param  version        the version of the aggregate to use for the event
     * @return an event with updated command context and entity version
     */
    private static Event importEvent(Event event, CommandContext commandContext, Version version) {
        EventContext eventContext = event.getContext()
                                         .toBuilder()
                                         .setCommandContext(commandContext)
                                         .setTimestamp(getCurrentTime())
                                         .setVersion(version)
                                         .build();
        Event result = event.toBuilder()
                            .setContext(eventContext)
                            .build();
        return result;
    }

    /**
     * Ensures that an event applier gets an instance of an event message,
     * not {@link Event}.
     *
     * <p>Instances of {@code Event} may be passed to an applier during
     * importing events or processing integration events. This may happen because
     * corresponding command handling method returned either {@code List<Event>}
     * or {@code Event}.
     *
     * @param  eventOrMsg an event message or {@code Event}
     * @return the passed instance or an event message extracted from the passed
     *         {@code Event} instance
     */
    private static Message ensureEventMessage(Message eventOrMsg) {
        Message eventMsg;
        if (eventOrMsg instanceof Event) {
            Event event = (Event) eventOrMsg;
            eventMsg = getMessage(event);
        } else {
            eventMsg = eventOrMsg;
        }
        return eventMsg;
    }

    /**
     * Restores the state and version from the passed snapshot.
     *
     * <p>If this method is called during a {@linkplain #play(AggregateStateRecord) replay}
     * (because the snapshot was encountered) the method uses the state
     * {@linkplain #getBuilder() builder}, which is used during the replay.
     *
     * <p>If not in replay, the method sets the state and version directly to the aggregate.
     *
     * @param snapshot the snapshot with the state to restore
     */
    void restore(Snapshot snapshot) {
        S stateToRestore = AnyPacker.unpack(snapshot.getState());
        Version versionFromSnapshot = snapshot.getVersion();
        setInitialState(stateToRestore, versionFromSnapshot);
    }

    /**
     * Returns all uncommitted events.
     *
     * @return immutable view of all uncommitted events
     */
    List<Event> getUncommittedEvents() {
        return ImmutableList.copyOf(uncommittedEvents);
    }

    /**
     * Obtains the number of uncommitted events.
     */
    @Internal
    @VisibleForTesting
    protected int uncommittedEventsCount() {
        return uncommittedEvents.size();
    }

    /**
     * Returns and clears all the events that were uncommitted before the call of this method.
     *
     * @return the list of events
     */
    List<Event> commitEvents() {
        List<Event> result = ImmutableList.copyOf(uncommittedEvents);
        uncommittedEvents.clear();
        remember(result);
        return result;
    }

    /**
     * Instructs to modify the state of an aggregate only within an event applier method.
     */
    @Override
    protected String getMissingTxMessage() {
        return "Modification of aggregate state or its lifecycle flags is not available this way." +
                " Make sure to modify those only from an event applier method.";
    }

    /**
     * Transforms the current state of the aggregate into the {@link Snapshot} instance.
     *
     * @return new snapshot
     */
    Snapshot toShapshot() {
        Any state = AnyPacker.pack(getState());
        Snapshot.Builder builder = Snapshot.newBuilder()
                                           .setState(state)
                                           .setVersion(getVersion())
                                           .setTimestamp(getCurrentTime());
        Snapshot snapshot = builder.build();
        return snapshot;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Opens the method for the repository.
     */
    @Override
    protected void clearRecentHistory() {
        super.clearRecentHistory();
    }

    /**
     * Creates an iterator of the aggregate event history with reverse traversal.
     *
     * <p>The records are returned sorted by timestamp in a descending order (from newer to older).
     *
     * <p>The iterator is empty if there's no history for the aggregate.
     *
     * @return new iterator instance
     */
    protected Iterator<Event> historyBackward() {
        return recentHistory().iterator();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose the method to the package.
     */
    @Override
    @VisibleForTesting
    protected int versionNumber() {
        return super.versionNumber();
    }
}
