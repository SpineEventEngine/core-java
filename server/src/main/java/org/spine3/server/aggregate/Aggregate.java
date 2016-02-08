/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.*;
import org.spine3.protobuf.Messages;
import org.spine3.server.CommandHandler;
import org.spine3.server.Entity;
import org.spine3.server.EntityId;
import org.spine3.server.aggregate.error.MissingEventApplierException;
import org.spine3.server.internal.CommandHandlerMethod;
import org.spine3.server.reflect.Classes;
import org.spine3.server.reflect.MethodMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Collections2.filter;
import static org.spine3.server.Identifiers.idToAny;
import static org.spine3.server.internal.CommandHandlerMethod.checkModifiers;

/**
 * Abstract base for aggregates.
 *
 * @param <I> the type for IDs of this class of aggregates. For supported ID types see {@link EntityId}
 * @param <M> the type of the state held by the aggregate
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ClassWithTooManyMethods")
public abstract class Aggregate<I, M extends Message> extends Entity<I, M> implements CommandHandler {

    static final Predicate<Method> IS_AGGREGATE_COMMAND_HANDLER = new IsCommandHandler();
    static final Predicate<Method> IS_EVENT_APPLIER = new IsEventApplier();

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
    private final List<Event> uncommittedEvents = Lists.newLinkedList();

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the supported types
     * @see EntityId
     */
    public Aggregate(I id) {
        super(id);
        this.idAsAny = idToAny(id);
    }

    /**
     * Returns set of the command types handled by a given aggregate.
     *
     * @param clazz {@link Class} of the aggregate
     * @return immutable set of classes of commands
     */
    @CheckReturnValue
    public static ImmutableSet<Class<? extends Message>> getCommandClasses(Class<? extends Aggregate> clazz) {
        return Classes.getHandledMessageClasses(clazz, IS_AGGREGATE_COMMAND_HANDLER);
    }

    @Override
    public Predicate<Method> getHandlerMethodPredicate() {
        return IS_AGGREGATE_COMMAND_HANDLER;
    }

    /**
     * Performs initialization of the instance and registers this class of aggregates
     * in the {@link Registry} if it is not registered yet.
     */
    private void init() {
        if (this.initialized) {
            return;
        }

        final Registry registry = Registry.getInstance();
        final Class<? extends Aggregate> thisClass = getClass();

        // Register this aggregate root class if it wasn't.
        if (!registry.contains(thisClass)) {
            registry.register(thisClass);
        }

        commandHandlers = registry.getCommandHandlers(thisClass);
        eventAppliers = registry.getEventAppliers(thisClass);

        this.initialized = true;
    }

    private Any getIdAsAny() {
        return idAsAny;
    }

    /**
     * Dispatches commands, generates events and applies them to the aggregate.
     *
     * @param command the command message to be executed on the aggregate.
     *                If this parameter is passed as {@link Any} the enclosing message will be unwrapped.
     * @param context the context of the command
     * @throws RuntimeException if an exception occurred during command dispatching with this exception as the cause.
     */
    @VisibleForTesting  // otherwise this method would have had package access.
    protected final void dispatch(Message command, CommandContext context) {
        checkNotNull(command);
        checkNotNull(context);

        init();

        if (command instanceof Any) {
            // We're likely getting the result of command.getMessage().
            // Extract the wrapped message out of it.
            final Any any = (Any) command;
            //noinspection AssignmentToMethodParameter
            command = Messages.fromAny(any);
        }

        try {
            final List<? extends Message> events = invokeHandler(command, context);
            apply(events, context);
        } catch (InvocationTargetException e) {
            propagate(e.getCause());
        }
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
        final Method method = commandHandlers.get(commandClass);
        if (method == null) {
            throw missingCommandHandler(commandClass);
        }

        final CommandHandlerMethod commandHandler = new CommandHandlerMethod(this, method);
        final List<? extends Message> result = commandHandler.invoke(commandMessage, context);
        return result;
    }

    /**
     * Invokes applier method for the passed event message.
     *
     * @param eventMessage the event message to apply
     * @throws InvocationTargetException if an exception was thrown during the method invocation
     */
    private void invokeApplier(Message eventMessage) throws InvocationTargetException {
        final Class<? extends Message> eventClass = eventMessage.getClass();
        final Method method = eventAppliers.get(eventClass);
        if (method == null) {
            throw missingEventApplier(eventClass);
        }

        final EventApplier applier = new EventApplier(this, method);
        applier.invoke(eventMessage);
    }

    /**
     * Plays passed events on the aggregate.
     *
     * @param events the list of the events
     * @throws RuntimeException if applying events caused an exception. This exception is set as the {@code cause}
     *                          for the thrown {@code RuntimeException}
     */
    public void play(Iterable<Event> events) {
        init();

        for (Event event : events) {
            final Message message = Events.getMessage(event);
            try {
                apply(message);
            } catch (InvocationTargetException e) {
                propagate(e.getCause());
            }
        }
    }

    /**
     * Applies events to an aggregate unless they are state-neutral.
     *
     * @param messages the event message to apply
     * @param commandContext the context of the command, execution of which produces the passed events
     * @throws InvocationTargetException if an exception occurs during event applying
     * @see #getStateNeutralEventClasses()
     */
    private void apply(Iterable<? extends Message> messages, CommandContext commandContext) throws InvocationTargetException {
        //noinspection LocalVariableNamingConvention
        final Set<Class<? extends Message>> stateNeutralEventClasses = getStateNeutralEventClasses();

        for (Message message : messages) {
            final boolean isStateNeutral = stateNeutralEventClasses.contains(message.getClass());
            if (!isStateNeutral) {
                apply(message);
            }
            final int currentVersion = getVersion();
            final M state = getState();
            final EventContext eventContext = createEventContext(commandContext, state, whenModified(), currentVersion, message);
            final Event event = Events.createEvent(message, eventContext);
            putUncommitted(event);
        }
    }

    /**
     * Applies an event to the aggregate.
     *
     * <p>If the event is {@link Snapshot} its state is copied. Otherwise, the event
     * is dispatched to corresponding applier method.
     *
     * @param event the event to apply
     * @throws MissingEventApplierException if there is no applier method defined for this type of event
     * @throws InvocationTargetException    if an exception occurred when calling event applier
     */
    private void apply(Message event) throws InvocationTargetException {
        if (event instanceof Snapshot) {
            restore((Snapshot) event);
            return;
        }
        invokeApplier(event);
    }

    /**
     * Returns a set of classes of state-neutral events (an empty set by default).
     *
     * <p>An event is state-neutral if we do not modify the aggregate state when this event occurs.
     *
     * <p>Instead of creating empty applier methods for such events,
     * override this method returning immutable set of event classes. For example:
     *
     * <pre>
     * private static final ImmutableSet&lt;Class&lt;? extends Message&gt;&gt; STATE_NEUTRAL_EVENT_CLASSES =
     *         ImmutableSet.&lt;Class&lt;? extends Message&gt;&gt;of(StateNeutralEvent.class);
     *
     * &#64;Override
     * protected Set&lt;Class&lt;? extends Message&gt;&gt; getStateNeutralEventClasses() {
     *     return STATE_NEUTRAL_EVENT_CLASSES;
     * }
     * </pre>
     *
     * @return a set of classes of state-neutral events
     */
    protected Set<Class<? extends Message>> getStateNeutralEventClasses() {
        return Collections.emptySet();
    }

    /**
     * Restores state from the passed snapshot.
     *
     * @param snapshot the snapshot with the state to restore
     */
    public void restore(Snapshot snapshot) {
        final M stateToRestore = Messages.fromAny(snapshot.getState());

        setState(stateToRestore, snapshot.getVersion(), snapshot.getWhenModified());
    }

    private void putUncommitted(Event record) {
        uncommittedEvents.add(record);
    }

    /**
     * Returns all uncommitted events (including state-neutral).
     *
     * @return immutable view of records for all uncommitted events
     * @see #getStateNeutralEventClasses()
     */
    @CheckReturnValue
    public List<Event> getUncommittedEvents() {
        return ImmutableList.copyOf(uncommittedEvents);
    }

    /**
     * Returns uncommitted events (excluding state-neutral).
     *
     * @return an immutable view of records for applicable uncommitted events
     * @see #getStateNeutralEventClasses()
     */
    @SuppressWarnings("InstanceMethodNamingConvention") // Prefer longer name here for clarity.
    protected Collection<Event> getStateChangingUncommittedEvents() {
        final Predicate<Event> isStateChanging = isStateChangingEventRecord();
        final Collection<Event> result = filter(uncommittedEvents, isStateChanging);
        return result;
    }

    /**
     * Creates the predicate that filters {@code EventRecord}s which modify the state
     * of the aggregate.
     *
     * <p>The predicate uses passed event classes for the events that do not modify the
     * state of the aggregate. As such, they are called <strong>State Neutral.</strong>
     *
     * @return new predicate instance
     */
    private Predicate<Event> isStateChangingEventRecord() {
        final Collection<Class<? extends Message>> stateNeutralEventClasses = getStateNeutralEventClasses();
        return new Predicate<Event>() {
            @Override
            public boolean apply(
                    @SuppressWarnings("NullableProblems")
                    /* The @Nullable annotation is removed to avoid checking for null input,
                       which is not possible here. Having the null input doesn't allow to test
                       that code branch. */ Event event) {
                final Message message = Events.getMessage(event);
                final boolean isStateNeutral = stateNeutralEventClasses.contains(message.getClass());
                return !isStateNeutral;
            }
        };
    }

    /**
     * Returns and clears all the events that were uncommitted before the call of this method.
     *
     * @return the list of event records
     */
    public List<Event> commitEvents() {
        final List<Event> result = ImmutableList.copyOf(uncommittedEvents);
        uncommittedEvents.clear();
        return result;
    }

    /**
     * Creates a context for an event.
     *
     * <p>The context may optionally have custom attributes added by
     * {@link #addEventContextAttributes(EventContext.Builder, CommandId, Message, Message, int)}.
     *
     *
     * @param commandContext the context of the command, execution of which produced the event
     * @param currentState   the state of the aggregated after the event was applied
     * @param whenModified   the moment of the aggregate modification for this event
     * @param currentVersion the version of the aggregate after the event was applied
     * @param event          the event for which to create the context
     * @return new instance of the {@code EventContext}
     * @see #addEventContextAttributes(EventContext.Builder, CommandId, Message, Message, int)
     */
    @CheckReturnValue
    protected EventContext createEventContext(CommandContext commandContext, M currentState, Timestamp whenModified, int currentVersion, Message event) {
        final EventId eventId = Events.generateId();
        final EventContext.Builder result = EventContext.newBuilder()
                .setEventId(eventId)
                .setCommandContext(commandContext)
                .setTimestamp(whenModified)
                .setVersion(currentVersion)
                .setAggregateId(getIdAsAny());
        addEventContextAttributes(result, commandContext.getCommandId(), event, currentState, currentVersion);
        return result.build();
    }

    /**
     * Adds custom attributes to an event context builder during the creation of the event context.
     *
     * <p>Does nothing by default. Override this method if you want to add custom attributes to the created context.
     *
     * @param builder        a builder for the event context
     * @param commandId      the id of the command, which cased the event
     * @param event          the event message
     * @param currentState   the current state of the aggregate after the event was applied
     * @param currentVersion the version of the aggregate after the event was applied
     * @see #createEventContext(CommandContext, Message, Timestamp, int, Message)
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
    @CheckReturnValue
    public Snapshot toSnapshot() {
        final Any state = Any.pack(getState());
        final int version = getVersion();
        final Timestamp whenModified = whenModified();
        final Snapshot.Builder builder = Snapshot.newBuilder()
                .setState(state)
                .setWhenModified(whenModified)
                .setVersion(version)
                .setTimestamp(TimeUtil.getCurrentTime());

        return builder.build();
    }

    /**
     * The registry of method maps for all aggregate classes.
     *
     * <p>This registry is used for caching command handlers and event appliers.
     * Aggregates register their classes in {@link Aggregate#init()} method.
     */
    private static class Registry {

        private final MethodMap.Registry<Aggregate> commandHandlers = new MethodMap.Registry<>();

        private final MethodMap.Registry<Aggregate> eventAppliers = new MethodMap.Registry<>();

        void register(Class<? extends Aggregate> clazz) {
            commandHandlers.register(clazz, IS_AGGREGATE_COMMAND_HANDLER);
            checkModifiers(commandHandlers.get(clazz).values());

            eventAppliers.register(clazz, IS_EVENT_APPLIER);
            EventApplier.checkModifiers(eventAppliers.get(clazz));
        }

        @CheckReturnValue
        boolean contains(Class<? extends Aggregate> clazz) {
            final boolean result = commandHandlers.contains(clazz);
            return result;
        }

        @CheckReturnValue
        MethodMap getCommandHandlers(Class<? extends Aggregate> clazz) {
            final MethodMap result = commandHandlers.get(clazz);
            return result;
        }

        @CheckReturnValue
        MethodMap getEventAppliers(Class<? extends Aggregate> clazz) {
            final MethodMap result = eventAppliers.get(clazz);
            return result;
        }

        @CheckReturnValue
        static Registry getInstance() {
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

    /**
     * The predicate for filtering command handler methods of aggregates.
     */
    private static class IsCommandHandler implements Predicate<Method> {

        @Override
        public boolean apply(@Nullable Method method) {
            checkNotNull(method);
            return check(method);
        }

        /**
         * Checks if a method is a command handler of an aggregate.
         *
         * @param method a method to check
         * @return {@code true} if the method is a command handler, {@code false} otherwise
         */
        private static boolean check(Method method) {
            if (!CommandHandlerMethod.isAnnotatedCorrectly(method)){
                return false;
            }
            if (!CommandHandlerMethod.acceptsCorrectParams(method)) {
                return false;
            }
            final boolean returnsMessageOrList = returnsMessageOrList(method);
            return returnsMessageOrList;
        }

        private static boolean returnsMessageOrList(Method method) {
            final Class<?> returnType = method.getReturnType();

            if (Message.class.isAssignableFrom(returnType)) {
                return true;
            }
            //noinspection RedundantIfStatement
            if (List.class.isAssignableFrom(returnType)) {
                return true;
            }
            return false;
        }
    }

    /**
     * The predicate for filtering event applier methods.
     */
    private static class IsEventApplier implements Predicate<Method> {

        private static final int EVENT_PARAM_INDEX = 0;

        /**
         * Checks if a method is an event applier.
         *
         * @param method to check
         * @return {@code true} if the method is an event applier, {@code false} otherwise
         */
        public static boolean isEventApplier(Method method) {
            final boolean isAnnotated = method.isAnnotationPresent(Apply.class);
            if (!isAnnotated) {
                return false;
            }

            final Class<?>[] parameterTypes = method.getParameterTypes();
            final boolean hasOneParam = parameterTypes.length == 1;
            if (!hasOneParam) {
                return false;
            }

            final boolean firstParamIsMessage = Message.class.isAssignableFrom(parameterTypes[EVENT_PARAM_INDEX]);
            final boolean returnsNothing = Void.TYPE.equals(method.getReturnType());

            return firstParamIsMessage && returnsNothing;
        }

        @Override
        public boolean apply(@Nullable Method method) {
            checkNotNull(method);
            return isEventApplier(method);
        }
    }
}
