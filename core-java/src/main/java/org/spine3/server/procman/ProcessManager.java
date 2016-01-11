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

package org.spine3.server.procman;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.Internal;
import org.spine3.base.*;
import org.spine3.internal.EventHandlerMethod;
import org.spine3.server.Entity;
import org.spine3.server.internal.CommandHandlerMethod;
import org.spine3.server.internal.CommandHandlingObject;
import org.spine3.util.Classes;
import org.spine3.util.EventRecords;
import org.spine3.util.Events;
import org.spine3.util.MethodMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.internal.EventHandlerMethod.IS_EVENT_HANDLER;
import static org.spine3.internal.EventHandlerMethod.checkModifiers;
import static org.spine3.server.procman.PmCommandHandler.IS_PM_COMMAND_HANDLER;

/**
 * An independent component that reacts to domain events in a cross-aggregate, eventually consistent manner.
 *
 * <p>A central processing unit used to maintain the state of the business process and determine
 * the next processing step based on intermediate results.
 *
 * <p>Event/command handlers are invoked by the {@link ProcessManagerRepository}
 * that manages instances of a process manager class.
 *
 * <p>For more information on Process Managers, please see:
 * <ul>
 *     <li><a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/ProcessManager.html">Process Manager Pattern</a></li>
 *     <li>
 *         <a href="http://kellabyte.com/2012/05/30/clarifying-the-saga-pattern/">Clarifying the Saga pattern</a>
 *         (and the difference between Process Manager and Saga)
 *     </li>
 *     <li><a href="https://dzone.com/articles/are-sagas-and-workflows-same-t">Are Sagas and Workflows the same...</a></li>
 *     <li><a href="https://msdn.microsoft.com/en-us/library/jj591569.aspx">CQRS Journey Guide: A Saga on Sagas</a></li>
 * </ul>
 *
 * @param <I> the type of the process manager IDs
 * @param <M> the type of the process manager state
 * @author Alexander Litus
 */
public abstract class ProcessManager<I, M extends Message> extends Entity<I, M> implements CommandHandlingObject {

    /**
     * Keeps initialization state of the process manager.
     */
    private volatile boolean initialized = false;

    /**
     * The map of command handler methods for this process manager.
     *
     * @see Registry
     */
    private MethodMap commandHandlers;

    /**
     * The map of event handlers for this process manager.
     *
     * @see Registry
     */
    private MethodMap eventHandlers;

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the supported types
     * @see EntityId
     */
    protected ProcessManager(I id) {
        super(id);
    }

    /**
     * Performs initialization of the instance and registers this class of process managers
     * in the {@link Registry} if it is not registered yet.
     */
    private void init() {
        if (!this.initialized) {
            final Registry registry = Registry.getInstance();
            final Class<? extends ProcessManager> pmClass = getClass();
            if (!registry.contains(pmClass)) {
                registry.register(pmClass);
            }
            commandHandlers = registry.getCommandHandlers(pmClass);
            eventHandlers = registry.getEventHandlers(pmClass);
            this.initialized = true;
        }
    }

    /**
     * Dispatches a command to the command handler method of the process manager.
     *
     * @param command the command to be executed on the process manager
     * @param context of the command
     * @throws InvocationTargetException if an exception occurs during command dispatching
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")
    protected List<EventRecord> dispatchCommand(Message command, CommandContext context) throws InvocationTargetException {
        checkNotNull(command, "command");
        checkNotNull(context, "context");

        init();
        final Class<? extends Message> commandClass = command.getClass();
        final Method method = commandHandlers.get(commandClass);
        if (method == null) {
            throw missingCommandHandler(commandClass);
        }
        final CommandHandlerMethod commandHandler = new PmCommandHandler(this, method);
        final List<? extends Message> events = commandHandler.invoke(command, context);
        final List<EventRecord> eventRecords = toEventRecords(events, context.getCommandId());
        return eventRecords;
    }

    private List<EventRecord> toEventRecords(final List<? extends Message> events, final CommandId commandId) {
        return Lists.transform(events, new Function<Message, EventRecord>() {
            @Nullable // return null because an exception won't be propagated in this case
            @Override
            public EventRecord apply(@Nullable Message event) {
                if (event == null) {
                    return EventRecord.getDefaultInstance();
                }
                final EventContext eventContext = createEventContext(commandId, event, getState(), whenModified(), getVersion());
                final EventRecord result = EventRecords.createEventRecord(event, eventContext);
                return result;
            }
        });
    }

    /**
     * Dispatches an event to the event handler method of the process manager.
     *
     * @param event the event to be handled by the process manager
     * @param context of the event
     * @throws InvocationTargetException if an exception occurs during event dispatching
     */
    protected void dispatchEvent(Message event, EventContext context) throws InvocationTargetException {
        //noinspection DuplicateStringLiteralInspection
        checkNotNull(context, "context");
        checkNotNull(event, "event");

        init();
        final Class<? extends Message> eventClass = event.getClass();
        final Method method = eventHandlers.get(eventClass);
        if (method == null) {
            throw missingEventHandler(eventClass);
        }
        final EventHandlerMethod handler = new EventHandlerMethod(this, method);
        handler.invoke(event, context);
    }

    /**
     * Creates a context for an event.
     *
     * <p>The context may optionally have custom attributes added by
     * {@link #addEventContextAttributes(EventContext.Builder, CommandId, Message, Message, int)}.
     *
     * @param commandId      the ID of the command, which caused the event
     * @param event          the event for which to create the context
     * @param currentState   the state of the process manager after the event was applied
     * @param whenModified   the moment of the aggregate modification for this event
     * @param currentVersion the version of the aggregate after the event was applied
     * @return new instance of the {@code EventContext}
     */
    @CheckReturnValue
    protected EventContext createEventContext(CommandId commandId, Message event, M currentState, Timestamp whenModified, int currentVersion) {
        final EventId eventId = Events.createId(commandId, whenModified);
        final EventContext.Builder builder = EventContext.newBuilder()
                .setEventId(eventId)
                .setVersion(currentVersion);

        addEventContextAttributes(builder, commandId, event, currentState, currentVersion);

        return builder.build();
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
     * @param currentVersion the version of the process manager after the event was applied
     * @see #createEventContext(CommandId, Message, Message, Timestamp, int)
     */
    @SuppressWarnings({"NoopMethodInAbstractClass", "UnusedParameters"}) // Have no-op method to avoid forced overriding.
    protected void addEventContextAttributes(EventContext.Builder builder,
                                             CommandId commandId, Message event, M currentState, int currentVersion) {
        // Do nothing.
    }

    /**
     * Returns the set of the command types handled by the process manager.
     *
     * @param pmClass the process manager class to inspect
     * @return immutable set of command classes or an empty set if no commands are handled
     */
    public static Set<Class<? extends Message>> getHandledCommandClasses(Class<? extends ProcessManager> pmClass) {
        return Classes.getHandledMessageClasses(pmClass, IS_PM_COMMAND_HANDLER);
    }

    /**
     * Returns the set of event classes handled by the process manager.
     *
     * @param pmClass the process manager class to inspect
     * @return immutable set of event classes or an empty set if no events are handled
     */
    public static ImmutableSet<Class<? extends Message>> getHandledEventClasses(Class<? extends ProcessManager> pmClass) {
        return Classes.getHandledMessageClasses(pmClass, IS_EVENT_HANDLER);
    }

    private IllegalStateException missingCommandHandler(Class<? extends Message> commandClass) {
        return new IllegalStateException(String.format("Missing handler for command class %s in process manager class %s.",
                        commandClass.getName(), getClass().getName()));
    }

    private IllegalStateException missingEventHandler(Class<? extends Message> eventClass) {
        return new IllegalStateException(String.format("Missing event handler for event class %s in the process manager class %s",
                eventClass, this.getClass()));
    }

    @Internal
    @Override
    public CommandHandlerMethod createMethod(Method method) {
        return new PmCommandHandler(this, method);
    }

    @Internal
    @Override
    public Predicate<Method> getHandlerMethodPredicate() {
        return PmCommandHandler.IS_PM_COMMAND_HANDLER;
    }

    /**
     * The registry of method maps for all process manager classes.
     *
     * <p>This registry is used for caching command/event handlers.
     * Process managers register their classes in {@link ProcessManager#init()} method.
     */
    private static class Registry {

        private final MethodMap.Registry<ProcessManager> commandHandlers = new MethodMap.Registry<>();
        private final MethodMap.Registry<ProcessManager> eventHandlers = new MethodMap.Registry<>();

        void register(Class<? extends ProcessManager> clazz) {
            commandHandlers.register(clazz, IS_PM_COMMAND_HANDLER);
            CommandHandlerMethod.checkModifiers(commandHandlers.get(clazz).values());

            eventHandlers.register(clazz, IS_EVENT_HANDLER);
            checkModifiers(eventHandlers.get(clazz).values());
        }

        @CheckReturnValue
        boolean contains(Class<? extends ProcessManager> clazz) {
            final boolean result = commandHandlers.contains(clazz) && eventHandlers.contains(clazz);
            return result;
        }

        @CheckReturnValue
        MethodMap getCommandHandlers(Class<? extends ProcessManager> clazz) {
            final MethodMap result = commandHandlers.get(clazz);
            return result;
        }

        @CheckReturnValue
        MethodMap getEventHandlers(Class<? extends ProcessManager> clazz) {
            final MethodMap result = eventHandlers.get(clazz);
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
}
