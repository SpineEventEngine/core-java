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

package org.spine3.server.saga;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.internal.EventHandlerMethod;
import org.spine3.server.Entity;
import org.spine3.server.internal.CommandHandlerMethod;
import org.spine3.util.Classes;
import org.spine3.util.MethodMap;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;

/**
 * An independent component that reacts to domain events in a cross-aggregate, eventually consistent manner.
 *
 * <p>A state machine that is driven forward by incoming events (which may come from many aggregates).
 *
 * <p>Aka {@code pipeline} or {@code process manager}.
 *
 * <p>Event/command handlers are invoked by the {@link SagaRepository} that manages instances of a saga class.
 *
 * @param <I> the type of the saga IDs
 * @param <M> the type of the state of the saga
 * @see <a href="https://msdn.microsoft.com/en-us/library/jj591569.aspx">CQRS Journey Guide: A Saga on Sagas</a>
 * @see <a href="http://cqrs.nu/Faq/sagas">cqrs.nu/Faq/sagas</a>
 * @author Alexander Litus
 */
public abstract class Saga<I, M extends Message> extends Entity<I, M> {

    /**
     * Keeps initialization state of the saga.
     */
    private volatile boolean initialized = false;

    /**
     * The map of command handler methods for this saga.
     *
     * @see Registry
     */
    private MethodMap commandHandlers;

    /**
     * The map of event handlers for this saga.
     *
     * @see Registry
     */
    private MethodMap eventHandlers;

    protected Saga(I id) {
        super(id);
    }

    /**
     * Performs initialization of the instance and registers this class of sagas
     * in the {@link Registry} if it is not registered yet.
     */
    private void init() {
        if (!this.initialized) {
            final Registry registry = Registry.getInstance();
            final Class<? extends Saga> sagaClass = getClass();
            if (!registry.contains(sagaClass)) {
                registry.register(sagaClass);
            }
            commandHandlers = registry.getCommandHandlers(sagaClass);
            eventHandlers = registry.getEventHandlers(sagaClass);
            this.initialized = true;
        }
    }

    /**
     * Dispatches a command to the command handler method of the saga.
     *
     * @param command the command to be executed on the saga
     * @param context of the command
     * @throws InvocationTargetException if an exception occurs during command dispatching
     */
    protected List<? extends Message> dispatchCommand(Message command, CommandContext context) throws InvocationTargetException {
        checkNotNull(command, "command");
        checkNotNull(context, "command context");

        init();
        final Class<? extends Message> commandClass = command.getClass();
        final Method method = commandHandlers.get(commandClass);
        if (method == null) {
            throw missingCommandHandler(commandClass);
        }
        final CommandHandlerMethod commandHandler = new CommandHandlerMethod(this, method);
        final Object handlingResult = commandHandler.invoke(command, context);

        if (handlingResult == null) {
            return ImmutableList.<Message>builder().build();
        }

        // TODO:2015-11-27:alexander.litus: DRY
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
     * Dispatches an event to the event handler method of the saga.
     *
     * @param event the event to be handled by the saga
     * @param context of the event
     * @throws InvocationTargetException if an exception occurs during event dispatching
     */
    protected void dispatchEvent(Message event, EventContext context) throws InvocationTargetException {
        checkNotNull(event, "event");
        checkNotNull(context, "event context");

        init();
        final Class<? extends Message> eventClass = event.getClass();
        final Method method = eventHandlers.get(eventClass);
        if (method == null) {
            throw missingEventHandler(eventClass);
        }
        final EventHandlerMethod handler = new EventHandlerMethod(this, method);
        invokeHandler(handler, event, context);
    }

    private static void invokeHandler(EventHandlerMethod handler, Message event, EventContext context) {
        try {
            handler.invoke(event, context);
        } catch (InvocationTargetException e) {
            propagate(e);
        }
    }

    /**
     * Returns the set of the command types handled by the saga.
     *
     * @param sagaClass the saga class to inspect
     * @return immutable set of command classes or an empty set if no commands are handled
     */
    public static Set<Class<? extends Message>> getHandledCommandClasses(Class<? extends Saga> sagaClass) {
        return Classes.getHandledMessageClasses(sagaClass, CommandHandlerMethod.isCommandHandlerPredicate);
    }

    /**
     * Returns the set of event classes handled by the saga.
     *
     * @param sagaClass the saga class to inspect
     * @return immutable set of event classes or an empty set if no events are handled
     */
    public static ImmutableSet<Class<? extends Message>> getHandledEventClasses(Class<? extends Saga> sagaClass) {
        return Classes.getHandledMessageClasses(sagaClass, EventHandlerMethod.isEventHandlerPredicate);
    }

    private IllegalStateException missingCommandHandler(Class<? extends Message> commandClass) {
        return new IllegalStateException(String.format("Missing handler for command class %s in saga class %s.",
                        commandClass.getName(), getClass().getName()));
    }

    private IllegalStateException missingEventHandler(Class<? extends Message> eventClass) {
        return new IllegalStateException(String.format("Missing event handler for event class %s in the sage class %s",
                eventClass, this.getClass()));
    }

    /**
     * The registry of method maps for all saga classes.
     * <p>
     * This registry is used for caching command/event handlers.
     * Sagas register their classes in {@link Saga#init()} method.
     */
    private static class Registry {

        private final MethodMap.Registry<Saga> commandHandlers = new MethodMap.Registry<>();
        private final MethodMap.Registry<Saga> eventHandlers = new MethodMap.Registry<>();

        void register(Class<? extends Saga> clazz) {
            commandHandlers.register(clazz, CommandHandlerMethod.isCommandHandlerPredicate);
            CommandHandlerMethod.checkModifiers(commandHandlers.get(clazz).values());

            eventHandlers.register(clazz, EventHandlerMethod.isEventHandlerPredicate);
            EventHandlerMethod.checkModifiers(eventHandlers.get(clazz).values());
        }

        @CheckReturnValue
        boolean contains(Class<? extends Saga> clazz) {
            final boolean result = commandHandlers.contains(clazz) && eventHandlers.contains(clazz);
            return result;
        }

        @CheckReturnValue
        MethodMap getCommandHandlers(Class<? extends Saga> clazz) {
            final MethodMap result = commandHandlers.get(clazz);
            return result;
        }

        @CheckReturnValue
        MethodMap getEventHandlers(Class<? extends Saga> clazz) {
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
