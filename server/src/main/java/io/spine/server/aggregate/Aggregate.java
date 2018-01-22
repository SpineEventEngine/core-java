/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.Internal;
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
import io.spine.server.command.CommandHandlerMethod;
import io.spine.server.command.CommandHandlingEntity;
import io.spine.server.event.EventFactory;
import io.spine.server.event.EventReactorMethod;
import io.spine.server.model.Model;
import io.spine.server.rejection.RejectionReactorMethod;
import io.spine.validate.ValidatingBuilder;

import javax.annotation.CheckReturnValue;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static io.spine.core.Events.getMessage;
import static io.spine.time.Time.getCurrentTime;
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
                extends CommandHandlingEntity<I, S, B> {

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
    @VisibleForTesting
    protected B getBuilder() {
        return super.getBuilder();
    }

    /**
     * Obtains a method for the passed command and invokes it.
     */
    @Override
    protected List<? extends Message> dispatchCommand(CommandEnvelope command) {
        final CommandHandlerMethod method = thisClass().getHandler(command.getMessageClass());
        final List<? extends Message> result =
                method.invoke(this, command.getMessage(), command.getCommandContext());
        return result;
    }

    /**
     * Dispatches the event on which the aggregate reacts.
     *
     * @param  event the envelope with the event to dispatch
     * @return a list with event messages that the aggregate produces in reaction to the event or
     *         an empty list if the aggregate state does not change in reaction to the event
     */
    List<? extends Message> reactOn(EventEnvelope event) {
        final EventReactorMethod method = thisClass().getReactor(event.getMessageClass());
        return method.invoke(this, event.getMessage(), event.getEventContext());
    }

    /**
     * Dispatches the rejection to which the aggregate reacts.
     *
     * @param  rejection the envelope with the rejection
     * @return a list with event messages that the aggregate produces in reaction to
     *         the rejection, or an empty list if the aggregate state does not change in
     *         response to this rejection
     */
    List<? extends Message> reactOn(RejectionEnvelope rejection) {
        final RejectionReactorMethod method = thisClass().getReactor(rejection.getMessageClass());
        return method.invoke(this, rejection.getMessage(), rejection.getRejectionContext());
    }

    /**
     * Invokes applier method for the passed event message.
     *
     * @param eventMessage the event message to apply
     */
    void invokeApplier(Message eventMessage) {
        final EventApplierMethod method = thisClass().getApplier(EventClass.of(eventMessage));
        method.invoke(this, eventMessage);
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
        final Snapshot snapshot = aggregateStateRecord.getSnapshot();
        if (isNotDefault(snapshot)) {
            restore(snapshot);
        }
        final List<Event> events = aggregateStateRecord.getEventList();

        play(events);
    }

    /**
     * Applies event messages.
     *
     * @param eventMessages the event messages or events to apply
     * @param origin        the envelope of a message which caused the events
     * @see #ensureEventMessage(Message)
     */
    void apply(Iterable<? extends Message> eventMessages, MessageEnvelope origin) {
        final List<? extends Message> messages = newArrayList(eventMessages);
        final EventFactory eventFactory =
                EventFactory.on(origin, getProducerId());

        final List<Event> events = newArrayListWithCapacity(messages.size());

        Version projectedEventVersion = getVersion();

        for (Message eventOrMessage : messages) {
            /* Applying each message would increment the entity version.
               Therefore, we should simulate this behaviour. */
            projectedEventVersion = Versions.increment(projectedEventVersion);
            final Message eventMessage = ensureEventMessage(eventOrMessage);

            final Event event;
            if (eventOrMessage instanceof Event) {
                /* If we get instances of Event, it means we are dealing with an import command,
                   which contains these events in the body. So we deal with a command envelope.
                */
                final CommandEnvelope ce = (CommandEnvelope)origin;
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
        final EventContext eventContext = event.getContext()
                                               .toBuilder()
                                               .setCommandContext(commandContext)
                                               .setTimestamp(getCurrentTime())
                                               .setVersion(version)
                                               .build();
        final Event result = event.toBuilder()
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
        final Message eventMsg;
        if (eventOrMsg instanceof Event) {
            final Event event = (Event) eventOrMsg;
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
        final S stateToRestore = AnyPacker.unpack(snapshot.getState());
        final Version versionFromSnapshot = snapshot.getVersion();
        setInitialState(stateToRestore, versionFromSnapshot);
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
        final List<Event> result = ImmutableList.copyOf(uncommittedEvents);
        uncommittedEvents.clear();
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
    @CheckReturnValue
    Snapshot toSnapshot() {
        final Any state = AnyPacker.pack(getState());
        final Snapshot.Builder builder = Snapshot.newBuilder()
                .setState(state)
                .setVersion(getVersion())
                .setTimestamp(getCurrentTime());
        return builder.build();
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
