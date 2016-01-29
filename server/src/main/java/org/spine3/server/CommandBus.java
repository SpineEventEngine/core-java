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
package org.spine3.server;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.*;
import org.spine3.internal.MessageHandlerMethod;
import org.spine3.server.command.CommandStore;
import org.spine3.server.command.CommandValidation;
import org.spine3.server.command.error.CommandHandlerAlreadyRegisteredException;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.server.internal.CommandHandlerMethod;
import org.spine3.server.reflect.Classes;
import org.spine3.type.CommandClass;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Commands.getMessage;
import static org.spine3.server.command.CommandValidation.unsupportedCommand;

/**
 * Dispatches the incoming commands to the corresponding handler.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 */
public class CommandBus implements AutoCloseable {

    private final DispatcherRegistry dispatcherRegistry = new DispatcherRegistry();
    private final HandlerRegistry handlerRegistry = new HandlerRegistry();

    private final CommandStore commandStore;

    @CheckReturnValue
    public static CommandBus create(CommandStore store) {
        return new CommandBus(checkNotNull(store));
    }

    protected CommandBus(CommandStore commandStore) {
        this.commandStore = commandStore;
    }

    /**
     * Registers the passed command dispatcher.
     *
     * @param dispatcher the dispatcher
     * @throws IllegalArgumentException if {@link CommandDispatcher#getCommandClasses()} returns empty set
     */
    /* package */ void register(CommandDispatcher dispatcher) {
        checkNoHandlersRegisteredForCommandsOf(dispatcher);
        dispatcherRegistry.register(dispatcher);
    }

    /**
     * Unregisters dispatching for command classes by the passed dispatcher.
     *
     * <p>If the passed dispatcher deals with commands for which another dispatcher already registered
     * the dispatch entries for such commands will not be unregistered, and warning will be logged.
     *
     * @param dispatcher the dispatcher to unregister
     */
    /* package */ void unregister(CommandDispatcher dispatcher) {
        dispatcherRegistry.unregister(dispatcher);
    }

    /**
     * Registers the passed command handler.
     *
     * @param handler a {@code non-null} handler object
     * @throws IllegalArgumentException if the handler does not have command handling methods
     */
    /* package */ void register(CommandHandler handler) {
        checkNoDispatchersRegisteredForCommandsOf(handler);
        handlerRegistry.register(handler);
    }

    /**
     * Unregisters the command handler from the command bus.
     *
     * @param handler the handler to unregister
     */
    /* package */ void unregister(CommandHandler handler) {
        handlerRegistry.unregister(handler);
    }

    /**
     * Verifies if the command can be posted to this {@code CommandBus}.
     *
     * <p>The command can be posted if it has either dispatcher or handler registered with
     * the command bus.
     *
     * <p>The method intended to be used with the instances of command messages extracted from {@link Command}.
     *
     * <p>If {@code Command} instance is passed, its message is extracted and validated.
     *
     * @param command the command message to check
     * @return the result of {@link Responses#ok()} if the command is supported,
     *         {@link CommandValidation#unsupportedCommand(Message)} otherwise
     */
    public Response validate(Message command) {
        checkNotNull(command);
        final Message message = (command instanceof Command) ?
                getMessage((Command) command) :
                command;

        if (dispatcherRegistry.hasDispatcherFor(message)) {
            return Responses.ok();
        }

        if (handlerRegistry.hasHandlerFor(message)) {
            return Responses.ok();
        }

        //TODO:2015-12-16:alexander.yevsyukov: Implement command validation for completeness of commands.
        // Presumably, it would be CommandValidator<Class<? extends Message> which would be exposed by
        // corresponding Aggregates or ProcessManagers, and then contributed to validator registry.

        return unsupportedCommand(message);
    }

    /**
     * Directs a command request to the corresponding handler.
     *
     * @param request the command request to be processed
     * @return a list of the events as the result of handling the command
     * @throws UnsupportedCommandException if there is no handler or dispatcher registered for
     *  the class of the passed command
     */
    public List<Event> post(Command request) {

        //TODO:2016-01-24:alexander.yevsyukov: Do not return value.

        checkNotNull(request);

        store(request);


        final CommandClass commandClass = CommandClass.of(request);

        if (dispatcherRegistered(commandClass)) {
            return dispatch(request);
        }

        if (handlerRegistered(commandClass)) {
            final Message command = getMessage(request);
            final CommandContext context = request.getContext();
            return invokeHandler(command, context);
        }

        //TODO:2016-01-24:alexander.yevsyukov: Unify exceptions with messages sent in Response.
        throw new UnsupportedCommandException(getMessage(request));
    }

    private List<Event> dispatch(Command request) {
        final CommandClass commandClass = CommandClass.of(request);
        final CommandDispatcher dispatcher = getDispatcher(commandClass);
        List<Event> result = Collections.emptyList();
        try {
            result = dispatcher.dispatch(request);
        } catch (Exception e) {
            //TODO:2016-01-24:alexander.yevsyukov: Update command status here?
            log().error("", e);
        }
        return result;
    }


    /* package */ final List<Event> invokeHandler(Message command, CommandContext context) {
        final CommandClass commandClass = CommandClass.of(command);
        final CommandHandlerMethod method = getHandler(commandClass);
        List<Event> result = Collections.emptyList();
        try {
            result = method.invoke(command, context);
        } catch (InvocationTargetException e) {
            //TODO:2016-01-24:alexander.yevsyukov: Update command status here?
            log().error("", e);
        }
        return result;
    }

    private void store(Command request) {
        commandStore.store(request);
    }

    private boolean dispatcherRegistered(CommandClass cls) {
        final boolean result = dispatcherRegistry.hasDispatcherFor(cls);
        return result;
    }

    private boolean handlerRegistered(CommandClass cls) {
        final boolean result = handlerRegistry.handlerRegistered(cls);
        return result;
    }

    private CommandDispatcher getDispatcher(CommandClass commandClass) {
        return dispatcherRegistry.getDispatcher(commandClass);
    }

    private CommandHandlerMethod getHandler(CommandClass cls) {
        return handlerRegistry.getHandler(cls);
    }

    @Override
    public void close() throws Exception {
        dispatcherRegistry.unregisterAll();
        handlerRegistry.unregisterAll();
        commandStore.close();
    }

    /**
     * The registry of objects dispatching command request to where they are processed.
     *
     * <p>There can be only one dispatcher per command class.
     */
    private static class DispatcherRegistry {

        private final Map<CommandClass, CommandDispatcher> dispatchers = Maps.newHashMap();

        /* package */ void register(CommandDispatcher dispatcher) {
            checkNotNull(dispatcher);
            final Set<CommandClass> commandClasses = dispatcher.getCommandClasses();
            checkNotEmpty(dispatcher, commandClasses);
            checkNotAlreadyRegistered(dispatcher, commandClasses);

            for (CommandClass commandClass : commandClasses) {
                dispatchers.put(commandClass, dispatcher);
            }
        }

        /**
         * Ensures that the dispatcher forwards at least one command.
         *
         * <p>We pass the {@code dispatcher} and the set as arguments instead of getting the set
         * from the dispatcher because this operation is expensive and
         *
         * @throws IllegalArgumentException if the dispatcher returns empty set of command classes
         * @throws NullPointerException if the dispatcher returns null set
         */
        private static void checkNotEmpty(CommandDispatcher dispatcher, Set<CommandClass> commandClasses) {
            checkArgument(!commandClasses.isEmpty(),
                    "No command classes are forwarded by this dispatcher: %s", dispatcher);
        }

        /**
         * Ensures that all of the commands of the passed dispatcher are not
         * already registered for dispatched in this command bus.
         *
         * @throws IllegalArgumentException if at least one command class already has registered dispatcher
         */
        private void checkNotAlreadyRegistered(CommandDispatcher dispatcher, Set<CommandClass> commandClasses) {
            final Set<CommandClass> alreadyRegistered = Sets.newHashSet();
            // Gather command classes from this dispatcher that are registered.
            for (CommandClass commandClass : commandClasses) {
                if (getDispatcher(commandClass) != null) {
                    alreadyRegistered.add(commandClass);
                }
            }
            CommandBus.checkNotAlreadyRegistered(alreadyRegistered, dispatcher,
                    "Cannot register dispatcher %s for the command class %s which already has registered dispatcher.",
                    "Cannot register dispatcher %s for command classes (%s) which already have registered dispatcher(s).");
        }

        /* package */ void unregister(CommandDispatcher dispatcher) {
            checkNotNull(dispatcher);
            final Set<CommandClass> commandClasses = dispatcher.getCommandClasses();
            checkNotEmpty(dispatcher, commandClasses);

            for (CommandClass commandClass : commandClasses) {
                final CommandDispatcher registeredDispatcher = dispatchers.get(commandClass);
                if (dispatcher.equals(registeredDispatcher)) {
                    dispatchers.remove(commandClass);
                } else {
                    warnUnregisterNotPossible(commandClass, registeredDispatcher, dispatcher);
                }
            }
        }

        private static void warnUnregisterNotPossible(CommandClass commandClass,
                                                      CommandDispatcher registeredDispatcher,
                                                      CommandDispatcher dispatcher) {
            log().warn(
                    "Another dispatcher (%s) found when trying to unregister dispatcher %s for the command class %s." +
                            "Dispatcher for the command class %s will not be unregistered.",
                    registeredDispatcher, dispatcher, commandClass);
        }

        /* package */ boolean hasDispatcherFor(Message command) {
            final boolean result = hasDispatcherFor(CommandClass.of(command));
            return result;
        }

        /* package */ boolean hasDispatcherFor(CommandClass commandClass) {
            final CommandDispatcher dispatcher = getDispatcher(commandClass);
            return dispatcher != null;
        }

        /* package */ void unregisterAll() {
            dispatchers.clear();
        }

        /* package */ CommandDispatcher getDispatcher(CommandClass commandClass) {
            return dispatchers.get(commandClass);
        }
    }

    /**
     * The registry of command handlers by command class.
     *
     * <p>There can be only one handler per command class.
     */
    private static class HandlerRegistry {

        private final Map<CommandClass, CommandHandlerMethod> handlersByClass = Maps.newConcurrentMap();

        /* package */ void register(CommandHandler object) {
            checkNotNull(object);

            final Map<CommandClass, CommandHandlerMethod> handlers = CommandHandlerMethod.scan(object);

            if (handlers.isEmpty()) {
                throw new IllegalArgumentException("No command handler methods found in :" + object);
            }

            registerMap(handlers);
        }

        /* package */ void unregister(CommandHandler object) {
            checkNotNull(object);

            final Map<CommandClass, CommandHandlerMethod> subscribers = CommandHandlerMethod.scan(object);
            unregisterMap(subscribers);
        }

        /**
         * Registers the passed handlers with the dispatcher.
         *
         * @param handlers map from command classes to corresponding handlers
         */
        private void registerMap(Map<CommandClass, CommandHandlerMethod> handlers) {
            checkDuplicates(handlers);
            putAll(handlers);
        }

        private void unregisterMap(Map<CommandClass, CommandHandlerMethod> handlers) {
            for (Map.Entry<CommandClass, CommandHandlerMethod> entry : handlers.entrySet()) {
                final CommandClass commandClass = entry.getKey();
                if (handlerRegistered(commandClass)) {
                    final CommandHandlerMethod registered = getHandler(commandClass);
                    final CommandHandlerMethod passed = entry.getValue();
                    if (registered.equals(passed)) {
                        removeFor(commandClass);
                    }
                }
            }
        }

        private void removeFor(CommandClass commandClass) {
            handlersByClass.remove(commandClass);
        }

        private void checkDuplicates(Map<CommandClass, CommandHandlerMethod> handlers) {
            for (Map.Entry<CommandClass, CommandHandlerMethod> entry : handlers.entrySet()) {
                final CommandClass commandClass = entry.getKey();

                if (handlerRegistered(commandClass)) {
                    final MessageHandlerMethod alreadyRegistered = getHandler(commandClass);
                    throw new CommandHandlerAlreadyRegisteredException(commandClass,
                            alreadyRegistered.getFullName(),
                            entry.getValue().getFullName());
                }
            }
        }

        @CheckReturnValue
        private boolean handlerRegistered(CommandClass cls) {
            return handlersByClass.containsKey(cls);
        }

        private CommandHandlerMethod getHandler(CommandClass cls) {
            return handlersByClass.get(cls);
        }

        private void putAll(Map<CommandClass, CommandHandlerMethod> subscribers) {
            handlersByClass.putAll(subscribers);
        }

        private void unregisterAll() {
            for (CommandClass commandClass : handlersByClass.keySet()) {
                removeFor(commandClass);
            }
        }

        private boolean hasHandlerFor(Message command) {
            final CommandHandlerMethod method = getHandler(CommandClass.of(command));
            return method != null;
        }
    }


    /**
     * Ensures that no handlers already registered for the command classes of the passed dispatcher.
     *
     * @param dispatcher the dispatcher to check
     * @throws IllegalArgumentException if one or more classes have registered handlers
     */
    @SuppressWarnings("InstanceMethodNamingConvention") // prefer longer name here for clarity.
    private void checkNoHandlersRegisteredForCommandsOf(CommandDispatcher dispatcher) {
        final Set<CommandClass> alreadyRegistered = Sets.newHashSet();
        final Set<CommandClass> commandClasses = dispatcher.getCommandClasses();
        for (CommandClass commandClass : commandClasses) {
            if (handlerRegistered(commandClass)) {
                alreadyRegistered.add(commandClass);
            }
        }

        checkNotAlreadyRegistered(alreadyRegistered, dispatcher,
                "Cannot register dispatcher %s for command class %s which already has registered handler.",
                "Cannot register dispatcher %s for command classes (%s) which already have registered handlers.");
    }

    /**
     * Ensures that there are no dispatchers registered for the commands of the passed handler.
     *
     * @param handler the command handler to check
     * @throws IllegalArgumentException if one ore more command classes already have registered dispatchers
     */
    @SuppressWarnings("InstanceMethodNamingConvention") // prefer longer name here for clarity.
    private void checkNoDispatchersRegisteredForCommandsOf(CommandHandler handler) {
        final ImmutableSet<Class<? extends Message>> handledMessageClasses = Classes.getHandledMessageClasses(
                handler.getClass(), CommandHandler.METHOD_PREDICATE);

        final Set<CommandClass> alreadyRegistered = Sets.newHashSet();
        for (Class<? extends Message> handledMessageClass : handledMessageClasses) {
            final CommandClass commandClass = CommandClass.of(handledMessageClass);
            if (dispatcherRegistry.hasDispatcherFor(commandClass)) {
                alreadyRegistered.add(commandClass);
            }
        }

        checkNotAlreadyRegistered(alreadyRegistered, handler,
                "Cannot register handler %s for the command class %s which already has registered dispatcher.",
                "Cannot register handler %s for command classes (%s) which already have registered dispatcher(s).");
    }

    /**
     * Ensures that the passed set of classes is empty.
     *
     * <p>This is a convenience method for checking registration of handling dispatching.
     *
     * @param alreadyRegistered the set of already registered classes or an empty set
     * @param registeringObject the object which tries to register dispatching or handling
     * @param singularFormat the message format for the case if the {@code alreadyRegistered} set contains only one element
     * @param pluralFormat the message format if {@code alreadyRegistered} set has more than one element
     * @throws IllegalArgumentException if the set is not empty
     */
    private static void checkNotAlreadyRegistered(Set<? extends CommandClass> alreadyRegistered, Object registeringObject,
                                                  String singularFormat, String pluralFormat) {
        final String format = alreadyRegistered.size() > 1 ? pluralFormat : singularFormat;
        checkArgument(alreadyRegistered.isEmpty(), format, registeringObject, Joiner.on(", ").join(alreadyRegistered));
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(CommandBus.class);
    }

    /**
     * The logger instance used by {@code CommandBus}.
     */
    protected static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
