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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.*;
import org.spine3.protobuf.Messages;
import org.spine3.server.Entity;
import org.spine3.server.aggregate.error.MissingEventApplierException;
import org.spine3.server.internal.CommandHandlerMethod;
import org.spine3.util.Events;
import org.spine3.util.MethodMap;
import org.spine3.util.Methods;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.util.Identifiers.idToAny;

/**
 * Abstract base for aggregates.
 *
 * @param <I> the type for IDs of this class of aggregates. For supported types see {@link AggregateId}
 * @param <M> the type of the state held by the aggregate
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ClassWithTooManyMethods")
public abstract class Aggregate<I, M extends Message> extends Entity<I, M> {

    /**
     * Cached value of the ID in the form of Any instance.
     */
    private final Any idAsAny;

    /**
     * Keeps initialization state of the aggregate.
     */
    private volatile boolean initialized = false;

    /**
     * The map of command handling methods for this class.
     *
     * @see Registry
     */
    private MethodMap commandHandlers;

    /**
     * The map of event appliers for this class.
     *
     * @see Registry
     */
    private MethodMap eventAppliers;

    /**
     * Events generated in the process of handling commands that were not yet committed.
     *
     * @see #commitEvents()
     */
    private final List<EventRecord> uncommittedEvents = Lists.newLinkedList();

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the supported types
     */
    protected Aggregate(I id) {
        super(id);
        this.idAsAny = idToAny(id);
    }

    /**
     * Returns set of the command types handled by a given aggregate.
     *
     * @param clazz {@link Class} of the aggregate
     * @return command types handled by aggregate
     */
    @CheckReturnValue
    public static Set<Class<? extends Message>> getCommandClasses(Class<? extends Aggregate> clazz) {
        return getHandledMessageClasses(clazz, CommandHandlerMethod.isCommandHandlerPredicate);
    }

    /**
     * Returns event/command types handled by given {@code Aggregate} class.
     *
     * @return immutable set of message classes or an empty set
     */
    @CheckReturnValue
    static ImmutableSet<Class<? extends Message>> getHandledMessageClasses(Class<? extends Aggregate> clazz, Predicate<Method> methodPredicate) {

        Set<Class<? extends Message>> result = Sets.newHashSet();

        for (Method method : clazz.getDeclaredMethods()) {

            boolean methodMatches = methodPredicate.apply(method);

            if (methodMatches) {
                Class<? extends Message> firstParamType = Methods.getFirstParamType(method);
                result.add(firstParamType);
            }
        }
        return ImmutableSet.<Class<? extends Message>>builder().addAll(result).build();
    }

    /**
     * Performs initialization of the instance.
     */
    private void init() {
        if (!this.initialized) {
            final Registry registry = Registry.instance();
            final Class<? extends Aggregate> thisClass = getClass();

            // Register this aggregate root class if it wasn't.
            if (!registry.contains(thisClass)) {
                registry.register(thisClass);
            }

            commandHandlers = registry.getCommandHandlers(thisClass);
            eventAppliers = registry.getEventAppliers(thisClass);

            if (super.getState() == null) {
                setDefault();
            }

            this.initialized = true;
        }
    }

    private Object invokeHandler(Message command, CommandContext context) throws InvocationTargetException {
        final Class<? extends Message> commandClass = command.getClass();
        Method method = commandHandlers.get(commandClass);
        if (method == null) {
            throw missingCommandHandler(commandClass);
        }
        CommandHandlerMethod commandHandler = new CommandHandlerMethod(this, method);
        final Object result = commandHandler.invoke(command, context);
        return result;
    }

    private void invokeApplier(Message event) throws InvocationTargetException {
        final Class<? extends Message> eventClass = event.getClass();
        Method method = eventAppliers.get(eventClass);
        if (method == null) {
            throw missingEventApplier(eventClass);
        }

        EventApplier applier = new EventApplier(this, method);
        applier.invoke(event);
    }

    /**
     * Returns the current state of the aggregate root.
     *
     * @return a non-null state object or default state instance
     */
    @Nonnull
    @Override
    public M getState() {
        init();
        final M state = super.getState();
        // An aggregate root when initialized may not have a null state because:
        // 1. Its initialization sets the state to default.
        // 2. Modifications are performed via command handlers or event appliers,
        //     which involves prior initialization.
        assert state != null;
        return state;
    }

    /**
     * Returns a non-null timestamp of the last modification.
     *
     * @return a non-null instance, which is the timestamp of the last modification or
     * the timestamp of setting the default state of the aggregate
     */
    @Nonnull
    @Override
    public Timestamp whenModified() {
        init();
        final Timestamp lastModified = super.whenModified();
        // An aggregate when initialized may not have a null modification timestamp because:
        // 1. Its initialization sets the timestamp.
        // 2. Modifications are performed via command handlers or event appliers,
        //     which involves prior initialization.
        assert lastModified != null;
        return lastModified;
    }

    private Any getIdAsAny() {
        return idAsAny;
    }

    /**
     * Dispatches commands, generates events and applies them to the aggregate.
     *
     * @param command the command to be executed on aggregate
     * @param context of the command
     * @throws InvocationTargetException is thrown if an exception occurs during command dispatching
     */
    @VisibleForTesting  // otherwise this method would have had package access.
    protected final void dispatch(Message command, CommandContext context) throws InvocationTargetException {
        init();
        List<? extends Message> events = generateEvents(command, context);
        CommandId commandId = context.getCommandId();
        apply(events, commandId);
    }

    /**
     * Directs the passed command to the corresponding command handler method of the aggregate.
     *
     * @param command the command to be processed
     * @param context the context of the command
     * @return a list of the event messages that were produced as the result of handling the command
     * @throws InvocationTargetException if an exception occurs during command handling
     */
    private List<? extends Message> generateEvents(Message command, CommandContext context)
            throws InvocationTargetException {

        checkNotNull(command);
        checkNotNull(context);

        final Object handlingResult = invokeHandler(command, context);
        final Class<?> resultClass = handlingResult.getClass();

        //noinspection IfMayBeConditional
        if (List.class.isAssignableFrom(resultClass)) {
            // Cast to list of messages as it is one of the return types we expect by methods we can call.
            @SuppressWarnings("unchecked")
            final List<? extends Message> result = (List<? extends Message>) handlingResult;
            return result;
        } else {
            // Another type of result is single event (as Message).
            final List<Message> result = Collections.singletonList((Message) handlingResult);
            return result;
        }
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
            /**
             * Event applier should call {@link #incrementState(Message)}.
             * It will advance version and record time of the modification.
             *
             * <p>It may turn that the event does not modify the state of the aggregate.
             */
            apply(event);

            int currentVersion = getVersion();
            final M state = getState();
            EventContext eventContext = createEventContext(commandId, event, state, whenModified(), currentVersion);

            EventRecord eventRecord = Events.createEventRecord(event, eventContext);

            putUncommitted(eventRecord);
        }
    }

    /**
     * Applies an event to the aggregate.
     * <p/>
     * <p>If the event is {@link Snapshot} its state is copied. Otherwise, the event
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

        invokeApplier(event);
    }

    private void putUncommitted(EventRecord record) {
        uncommittedEvents.add(record);
    }

    /**
     * Restores state from the passed snapshot.
     *
     * @param snapshot the snapshot with the state to restore
     */
    public void restore(SnapshotOrBuilder snapshot) {
        M stateToRestore = Messages.fromAny(snapshot.getState());

        setState(stateToRestore, snapshot.getVersion(), snapshot.getWhenModified());
    }

    /**
     * @return immutable view of records for uncommitted events
     */
    @CheckReturnValue
    public List<EventRecord> getUncommittedEvents() {
        return ImmutableList.copyOf(uncommittedEvents);
    }

    /**
     * Returns and clears the events that were uncommitted before the call of this method.
     *
     * @return the list of event records
     */
    @CheckReturnValue
    public List<EventRecord> commitEvents() {
        List<EventRecord> result = ImmutableList.copyOf(uncommittedEvents);
        uncommittedEvents.clear();
        return result;
    }

    /**
     * Creates a context for an event.
     * <p/>
     * <p>The context may optionally have custom attributes added by
     * {@link #addEventContextAttributes(EventContext.Builder, CommandId, Message, Message, int)}.
     *
     * @param commandId      the ID of the command, which caused the event
     * @param event          the event for which to create the context
     * @param currentState   the state of the aggregated after the event was applied
     * @param whenModified   the moment of the aggregate modification for this event
     * @param currentVersion the version of the aggregate after the event was applied
     * @return new instance of the {@code EventContext}
     * @see #addEventContextAttributes(EventContext.Builder, CommandId, Message, Message, int)
     */
    protected EventContext createEventContext(CommandId commandId, Message event, M currentState, Timestamp whenModified, int currentVersion) {

        EventId eventId = Events.createId(commandId, whenModified);

        EventContext.Builder builder = EventContext.newBuilder()
                .setEventId(eventId)
                .setVersion(currentVersion)
                .setAggregateId(getIdAsAny());

        addEventContextAttributes(builder, commandId, event, currentState, currentVersion);

        return builder.build();
    }

    /**
     * Adds custom attributes to an event context builder during the creation of the event context.
     * <p/>
     * <p>Does nothing by default. Override this method if you want to add custom attributes to the created context.
     *
     * @param builder        a builder for the event context
     * @param commandId      the id of the command, which cased the event
     * @param event          the event message
     * @param currentState   the current state of the aggregate after the event was applied
     * @param currentVersion the version of the aggregate after the event was applied
     * @see #createEventContext(CommandId, Message, Message, Timestamp, int)
     */
    @SuppressWarnings({"NoopMethodInAbstractClass", "UnusedParameters"}) // Have no-op method to avoid overriding.
    protected void addEventContextAttributes(EventContext.Builder builder,
                                             CommandId commandId, Message event, M currentState, int currentVersion) {
        // Do nothing.
    }

    /**
     * Transforms the current state of the aggregate into the snapshot event.
     *
     * @return new snapshot
     */
    public Snapshot toSnapshot() {
        final Any state = Any.pack(getState());
        final int version = getVersion();
        final Timestamp whenModified = whenModified();
        Snapshot.Builder builder = Snapshot.newBuilder()
                .setState(state)
                .setWhenModified(whenModified)
                .setVersion(version)
                .setTimestamp(TimeUtil.getCurrentTime());

        return builder.build();
    }

    /**
     * The registry of method maps for all aggregate classes.
     * <p/>
     * <p>This registry is used for caching command handlers and event appliers.
     * Aggregates register their classes in {@link Aggregate#init()} method.
     */
    private static class Registry {

        private final MethodMap.Registry<Aggregate> commandHandlers = new MethodMap.Registry<>();

        private final MethodMap.Registry<Aggregate> eventAppliers = new MethodMap.Registry<>();

        void register(Class<? extends Aggregate> clazz) {
            commandHandlers.register(clazz, CommandHandlerMethod.isCommandHandlerPredicate);
            CommandHandlerMethod.checkModifiers(commandHandlers.get(clazz));

            eventAppliers.register(clazz, EventApplier.isEventApplierPredicate);
            EventApplier.checkModifiers(eventAppliers.get(clazz));
        }

        boolean contains(Class<? extends Aggregate> clazz) {
            boolean result = commandHandlers.contains(clazz);
            return result;
        }

        MethodMap getCommandHandlers(Class<? extends Aggregate> clazz) {
            MethodMap result = commandHandlers.get(clazz);
            return result;
        }

        MethodMap getEventAppliers(Class<? extends Aggregate> clazz) {
            MethodMap result = eventAppliers.get(clazz);
            return result;
        }

        static Registry instance() {
            return Singleton.INSTANCE.value;
        }

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final Registry value = new Registry();
        }
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
