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
package org.spine3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.*;
import org.spine3.error.MissingEventApplierException;
import org.spine3.util.Events;
import org.spine3.protobuf.Messages;
import org.spine3.protobuf.Timestamps;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Abstract base for aggregate roots.
 *
 * @param <I> the type for ID of the aggregate root
 * @param <S> the type of the state held by the root
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"ClassWithTooManyMethods", "AbstractClassNeverImplemented"})
public abstract class AggregateRoot<I extends Message, S extends Message> {

    private CommandDispatcher dispatcher;
    private EventApplier applier;

    private final I id;
    private final Any idAsAny;

    private volatile boolean initialized = false;

    private S state;
    private int version = 0;
    private Timestamp whenLastModified = Timestamps.now();

    private final List<EventRecord> eventRecords = Lists.newLinkedList();

    protected AggregateRoot(I id) {
        this.id = id;
        this.idAsAny = Messages.toAny(id);
    }

    //TODO:2015-07-28:alexander.yevsyukov: Migrate API to use Event and Command instead of Message

    /**
     * Dispatches commands, generates events and apply them to the aggregate root.
     *
     * @param command the command to be executed on aggregate root
     * @param context of the command
     * @throws InvocationTargetException is thrown if an exception occurs during command dispatching
     */
    public void dispatch(Message command, CommandContext context) throws InvocationTargetException {
        init();
        List<? extends Message> events = generateEvents(command, context);

        CommandId commandId = context.getCommandId();

        apply(events, commandId);
    }

    /**
     * Plays passed events on the aggregate.
     *
     * @param records the list of the event records
     * @throws InvocationTargetException the exception is thrown if command dispatching fails inside
     */
    public void play(Iterable<EventRecord> records) throws InvocationTargetException {
        init();

        for (EventRecord record : records) {
            final Message event = Messages.fromAny(record.getEvent());
            apply(event);
        }
    }

    private void apply(Iterable<? extends Message> events, CommandId commandId) throws InvocationTargetException {
        for (Message event : events) {
            apply(event);

            int currentVersion = incrementVersion();
            EventContext eventContext = createEventContext(commandId, currentVersion, getState());
            EventRecord eventRecord = createEventRecord(event, eventContext);

            putUncommitted(eventRecord);
        }
    }

    /**
     * Applies an event to the aggregate root.
     * <p>
     * If the event is {@link Snapshot} its state is copied. Otherwise, the event
     * is dispatched to corresponding applier method.
     *
     * @param event the event to apply
     * @throws MissingEventApplierException if there is no applier method defined for this type of event
     * @throws InvocationTargetException    if an exception occurs during event applying
     */
    private void apply(Message event) throws InvocationTargetException {
        if (event instanceof Snapshot) {
            restore((Snapshot) event);
            return;
        }

        applier.apply(Event.of(event));
    }

    /**
     * Restores state from the passed snapshot.
     *
     * @param snapshot the snapshot with the state to restore
     */
    @SuppressWarnings("TypeMayBeWeakened") // Have this type to make API more obvious.
    public void restore(Snapshot snapshot) {
        setVersion(snapshot.getVersion());

        S stateToRestore = Messages.fromAny(snapshot.getState());
        setState(stateToRestore);

        setWhenLastModified(snapshot.getWhenLastModified());
    }

    private static EventRecord createEventRecord(Message event, EventContext context) {
        EventRecord result = EventRecord.newBuilder()
                .setEvent(Messages.toAny(event))
                .setContext(context).build();
        return result;
    }

    @CheckReturnValue
    public I getId() {
        return id;
    }

    @CheckReturnValue
    public S getState() {
        // The EventDispatcher.apply() method waits till the events are processed.
        // So once apply() finishes, it's safe to return the state.
        return state;
    }

    /**
     * Validates the passed state.
     * <p>
     * Does nothing by default. Aggregate roots may override this method to
     * specify logic of validating initial or intermediate state of the root.
     *
     * @param state a state object to replace the current state
     * @throws IllegalStateException if the state is not valid
     */
    @SuppressWarnings({"NoopMethodInAbstractClass", "UnusedParameters"})
    // Have this no-op method to prevent enforcing implementation in all sub-classes.
    protected void validate(S state) throws IllegalStateException {
        // Do nothing by default.
    }

    protected void setState(S state) {
        validate(state);
        this.state = state;
    }

    protected void setVersion(int version) {
        this.version = version;
    }

    protected void setWhenLastModified(Timestamp whenLastModified) {
        this.whenLastModified = whenLastModified;
    }

    /**
     * @return current version number of the aggregate.
     */
    @CheckReturnValue
    public int getVersion() {
        return version;
    }

    protected void init() {
        if (!initialized) {
            dispatcher = new CommandDispatcher();
            applier = new EventApplier();

            dispatcher.register(this);
            applier.register(this);

            if (state == null) {
                state = getDefaultState();
            }

            initialized = true;
        }
    }

    @CheckReturnValue
    protected boolean isInitialized() {
        return initialized;
    }

    @CheckReturnValue
    protected List<EventRecord> getUncommittedEvents() {
        return ImmutableList.copyOf(eventRecords);
    }

    @CheckReturnValue
    protected Timestamp whenLastModified() {
        return this.whenLastModified;
    }

    @CheckReturnValue
    public List<EventRecord> commitEvents() {
        List<EventRecord> result = ImmutableList.copyOf(eventRecords);
        eventRecords.clear();
        return result;
    }

    @CheckReturnValue
    protected abstract S getDefaultState();

    /**
     * Creates a context for an event.
     * <p>
     * Override this method if you want to add custom attributes to the created context.
     *
     * @param commandId      the ID of the command, which caused the event
     * @param currentVersion the version of the aggregate root after the event was applied
     * @param currentState   the state of the aggregated root after the event was applied
     * @return new instance of the {@code EventContext}
     */
    protected EventContext createEventContext(CommandId commandId, int currentVersion, S currentState) {

        EventId eventId = Events.generateId(commandId);

        Any state = Messages.toAny(currentState);

        EventContext result = EventContext.newBuilder()
                .setEventId(eventId)
                .setVersion(currentVersion)
                .setAggregateId(idAsAny)
                .setAggregateState(state)
                .build();

        return result;
    }

    private void putUncommitted(EventRecord record) {
        eventRecords.add(record);
    }

    private List<? extends Message> generateEvents(Message command, CommandContext context)
            throws InvocationTargetException {

        List<? extends Message> result = dispatcher.dispatchToAggregate(Command.of(command), context);
        return result;
    }

    private int incrementVersion() {
        ++version;
        whenLastModified = Timestamps.now();

        return version;
    }
}
