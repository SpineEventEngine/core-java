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
package org.spine3.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.*;
import org.spine3.base.*;
import org.spine3.error.MissingEventApplierException;
import org.spine3.protobuf.Messages;
import org.spine3.util.Events;
import org.spine3.util.Methods;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;

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
    private EventApplierMap applier;

    private final I id;
    private final Any idAsAny;

    private volatile boolean initialized = false;

    private S state;
    private int version = 0;
    private Timestamp whenLastModified = getCurrentTime();

    private final List<EventRecord> eventRecords = Lists.newLinkedList();

    protected AggregateRoot(I id) {
        this.id = id;
        this.idAsAny = Messages.toAny(id);
    }

    /**
     * Directs a command to the corresponding aggregate handler.
     *
     * @param command the command to be processed
     * @param context the context of the command
     * @return a list of the event messages that were produced as the result of handling the command
     * @throws InvocationTargetException if an exception occurs during command handling
     */
    private List<? extends Message> dispatch(Command command, CommandContext context)
            throws InvocationTargetException {

        checkNotNull(command);
        checkNotNull(context);

        MessageSubscriber subscriber = dispatcher.getSubscriber(command.getCommandClass());

        Object handlingResult = subscriber.handle(command.value(), context);

        //noinspection IfMayBeConditional
        if (List.class.isAssignableFrom(handlingResult.getClass())) {
            // Cast to list of messages as it is one of the return types we expect by methods we can call.
            //noinspection unchecked
            return (List<? extends Message>) handlingResult;
        } else {
            // Another type of result is single event (as Message).
            return Collections.singletonList((Message) handlingResult);
        }
    }

    public Map<CommandClass, MessageSubscriber> getCommandHandlers() {
        Map<CommandClass, MessageSubscriber> result = Methods.scanForCommandHandlers(this);
        return result;
    }

    //TODO:2015-07-28:alexander.yevsyukov: Migrate API to use Event and Command instead of Message

    /**
     * Dispatches commands, generates events and apply them to the aggregate root.
     *
     * @param command the command to be executed on aggregate root
     * @param context of the command
     * @throws InvocationTargetException is thrown if an exception occurs during command dispatching
     */
    protected void dispatch(Message command, CommandContext context) throws InvocationTargetException {
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
            final S state = getState();
            EventContext eventContext = createEventContext(commandId, event, state, currentVersion);
            EventRecord eventRecord = createEventRecord(event, eventContext);

            putUncommitted(eventRecord);
        }
    }

    /**
     * Applies an event to the aggregate root.
     * <p/>
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
                .setContext(context)
                .build();
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
     * <p/>
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
            initCommandDispatcher();
            initEventApplier();

            if (state == null) {
                state = getDefaultState();
            }

            initialized = true;
        }
    }

    private void initCommandDispatcher() {
        dispatcher = new CommandDispatcher();
        Map<CommandClass, MessageSubscriber> subscribers = getCommandHandlers();
        dispatcher.register(subscribers);
    }

    private void initEventApplier() {
        applier = new EventApplierMap();
        applier.register(this);
    }

    @CheckReturnValue
    protected boolean isInitialized() {
        return initialized;
    }

    /**
     * @return immutable view of records for uncommitted events
     */
    @CheckReturnValue
    public List<EventRecord> getUncommittedEvents() {
        return ImmutableList.copyOf(eventRecords);
    }

    @CheckReturnValue
    public Timestamp whenLastModified() {
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
     * <p/>
     * The created context will hold the state of the root, if {@link #eventContextHasState()} returns {@code true}
     * (which is the default behaviour).
     * <p/>
     * The context may optionally have custom attributes are added by
     * {@link #addEventContextAttributes(EventContext.Builder, CommandId, Message, Message, int)}.
     *
     * @param commandId      the ID of the command, which caused the event
     * @param event          the event for which to create the context
     * @param currentState   the state of the aggregated root after the event was applied
     * @param currentVersion the version of the aggregate root after the event was applied
     * @return new instance of the {@code EventContext}
     * @see #eventContextHasState()
     * @see #addEventContextAttributes(EventContext.Builder, CommandId, Message, Message, int)
     */
    protected EventContext createEventContext(CommandId commandId, Message event, S currentState, int currentVersion) {

        EventId eventId = Events.generateId(commandId);

        Any state = Messages.toAny(currentState);

        EventContext.Builder builder = EventContext.newBuilder()
                .setEventId(eventId)
                .setVersion(currentVersion)
                .setAggregateId(idAsAny);

        if (eventContextHasState()) {
            builder.setAggregateState(state);
        }

        addEventContextAttributes(builder, commandId, event, currentState, currentVersion);

        return builder.build();
    }

    /**
     * This method controls inclusion of the aggregate root state into an event context.
     * <p/>
     * By default this method always return {@code true} making event contexts always include states.
     * Override this method to control the inclusion.
     *
     * @return always {@code true}
     * @see #createEventContext(CommandId, Message, Message, int)
     */
    protected boolean eventContextHasState() {
        return true;
    }

    /**
     * Adds custom attributes to an event context builder during the creation of the event context.
     * <p/>
     * Does nothing by default. Override this method if you want to add custom attributes to the created context.
     *
     * @param builder        a builder for the event context
     * @param commandId      the id of the command, which cased the event
     * @param event          the event message
     * @param currentState   the current state of the aggregate root after the event was applied
     * @param currentVersion the version of the aggregate root after the event was applied   @see #createEventContext(CommandId, Message, Message, int)
     */
    @SuppressWarnings({"NoopMethodInAbstractClass", "UnusedParameters"}) // Have no-op method to avoid overriding.
    protected void addEventContextAttributes(EventContext.Builder builder,
                                             CommandId commandId, Message event, S currentState, int currentVersion) {
        // Do nothing.
    }

    private void putUncommitted(EventRecord record) {
        eventRecords.add(record);
    }

    private List<? extends Message> generateEvents(Message command, CommandContext context)
            throws InvocationTargetException {

        List<? extends Message> result = dispatch(Command.of(command), context);
        return result;
    }

    private int incrementVersion() {
        ++version;
        whenLastModified = getCurrentTime();

        return version;
    }
}
