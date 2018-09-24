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
import com.google.common.collect.Streams;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.protobuf.AnyPacker;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.aggregate.model.EventApplier;
import io.spine.server.command.CommandHandlingEntity;
import io.spine.server.command.model.CommandHandlerMethod;
import io.spine.server.entity.EventPlayer;
import io.spine.server.event.EventReactor;
import io.spine.server.event.model.EventReactorMethod;
import io.spine.server.model.EventsResult;
import io.spine.server.model.MayProduceNothing;
import io.spine.server.model.ReactorMethodResult;
import io.spine.validate.ValidatingBuilder;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;
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
 * <h1>Creating an aggregate class</h1>
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
 * <h1>Performance considerations</h1>
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
        implements EventPlayer, EventReactor, MayProduceNothing {

    /**
     * Events generated in the process of handling commands that were not yet committed.
     *
     * @see #commitEvents()
     */
    private UncommittedEvents uncommittedEvents = UncommittedEvents.ofNone();

    /** A guard for ensuring idempotency of messages dispatched by this aggregate. */
    private IdempotencyGuard idempotencyGuard;

    /**
     * Creates a new instance.
     *
     * @apiNote Constructors of derived classes are likely to have package-private access level
     * because of the following reasons:
     * <ol>
     *     <li>These constructors are not public API of an application.
     *     Commands and aggregate IDs are.
     *     <li>These constructors need to be accessible from tests in the same package.
     * </ol>
     *
     * <p>If you do have tests that create aggregates via constructors, consider annotating them
     * with {@code @VisibleForTesting}. Otherwise, aggregate constructors (that are invoked by
     * {@link io.spine.server.aggregate.AggregateRepository AggregateRepository}
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

    @Internal
    @Override
    protected AggregateClass<?> getModelClass() {
        return asAggregateClass(getClass());
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
    protected List<Event> dispatchCommand(CommandEnvelope command) {
        idempotencyGuard.check(command);
        CommandHandlerMethod method = thisClass().getHandler(command.getMessageClass());
        EventsResult result = method.invoke(this, command);
        return result.produceEvents(command);
    }

    /**
     * Dispatches the event on which the aggregate reacts.
     *
     * <p>Reacting on a event may result in emitting event messages.
     * All the {@linkplain Empty empty} messages are filtered out from the result.
     *
     * @param  event the envelope with the event to dispatch
     * @return a list of event messages that the aggregate produces in reaction to the event or
     *         an empty list if the aggregate state does not change in reaction to the event
     */
    List<Event> reactOn(EventEnvelope event) {
        idempotencyGuard.check(event);
        EventReactorMethod method =
                thisClass().getReactor(event.getMessageClass(), event.getOriginClass());
        ReactorMethodResult result =
                method.invoke(this, event);
        return result.produceEvents(event);
    }

    /**
     * Invokes applier method for the passed event message.
     *
     * @param event the event to apply
     */
    void invokeApplier(EventEnvelope event) {
        EventApplier method = thisClass().getApplier(event.getMessageClass());
        method.invoke(this, event);
    }

    @Override
    public void play(Iterable<Event> events) {
        EventPlayer.forTransactionOf(this)
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
     * Applies events to this {@code Aggregate}.
     *
     * <p>Before applying the events, changes their versions as follows:
     * <ol>
     *     <li>The first event in the list gets the current version of the aggregate incremented
     *         by one.
     *     <li>All the next events get the following versions.
     * </ol>
     *
     * <p>For example, if the current version number of the aggregate is {@code 42}, and
     * the {@code events} list is of size 3, the applied events will have versions {@code 43},
     * {@code 44}, and {@code 45}.
     *
     * @param events the events to apply
     * @return the exact list of {@code events} but with adjusted versions
     */
    List<Event> apply(List<Event> events) {
        ImmutableList<Event> versionedEvents = prepareEvents(events);
        play(versionedEvents);
        uncommittedEvents = uncommittedEvents.append(versionedEvents);
        return versionedEvents;
    }

    /**
     * Prepares the given events to be applied to this aggregate.
     *
     * @param originalEvents
     *         the events to be applied
     * @return events ready to be applied to this aggregate
     * @see #apply(List)
     */
    private ImmutableList<Event> prepareEvents(Collection<Event> originalEvents) {
        Version currentVersion = getVersion();

        Stream<Version> versions = Stream.iterate(currentVersion, Versions::increment)
                                         .skip(1) // Skip current version
                                         .limit(originalEvents.size());
        Stream<Event> events = originalEvents.stream();
        ImmutableList<Event> eventsToApply = Streams.zip(events, versions,
                                                         Events::substituteVersion)
                                                    .collect(toImmutableList());
        return eventsToApply;
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
        S stateToRestore = unpack(snapshot.getState());
        Version versionFromSnapshot = snapshot.getVersion();
        setInitialState(stateToRestore, versionFromSnapshot);
    }

    /**
     * Returns all uncommitted events.
     *
     * @return immutable view of all uncommitted events
     */
    UncommittedEvents getUncommittedEvents() {
        return uncommittedEvents;
    }

    /**
     * {@linkplain #remember Remembers} the uncommitted events as
     * the {@link io.spine.server.entity.RecentHistory RecentHistory} and clears them.
     */
    void commitEvents() {
        List<Event> recentEvents = uncommittedEvents.list();
        remember(recentEvents);
        uncommittedEvents = UncommittedEvents.ofNone();
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
    Snapshot toSnapshot() {
        Any state = AnyPacker.pack(getState());
        Snapshot.Builder builder = Snapshot
                .newBuilder()
                .setState(state)
                .setVersion(getVersion())
                .setTimestamp(getCurrentTime());
        return builder.build();
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
