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

import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.error.CommandHandlerAlreadyRegisteredException;
import org.spine3.error.UnsupportedCommandException;
import org.spine3.server.AggregateRoot;
import org.spine3.util.Methods;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dispatches the incoming commands to the corresponding handler.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 */
public class CommandDispatcher {

    private final Map<CommandClass, MessageSubscriber> subscribersByType = Maps.newConcurrentMap();

    /**
     * Register the passed command handler in the dispatcher.
     *
     * @param handler a command handler object
     */
    public void register(CommandHandler handler) {
        checkNotNull(handler);

        Map<CommandClass, MessageSubscriber> subscribers = getSubscribers(handler);
        checkSubscribers(subscribers);

        putAll(subscribers);
    }

    /**
     * Registers the passed repository in the dispatcher.
     *
     * @param repository a repository object
     */
    public void register(Repository repository) {
        checkNotNull(repository);

        Map<CommandClass, MessageSubscriber> subscribers = getSubscribers(repository);
        checkSubscribers(subscribers);

        putAll(subscribers);
    }

    /**
     * Registers an aggregated root in the dispatcher.
     *
     * @param aggregateRoot the aggregate root object
     */
    public void register(AggregateRoot aggregateRoot) {
        checkNotNull(aggregateRoot);

        Map<CommandClass, MessageSubscriber> subscribers = getSubscribers(aggregateRoot);
        checkSubscribers(subscribers);

        putAll(subscribers);
    }

    private static Map<CommandClass, MessageSubscriber> getSubscribers(CommandHandler handler) {
        Map<CommandClass, MessageSubscriber> subscribers = Methods.scanForCommandHandlers(handler);
        return subscribers;
    }

    private static Map<CommandClass, MessageSubscriber> getSubscribers(Repository repository) {
        /*
           The order inside this method is important!

           At first we add all subscribers from the aggregate root
           and after that register directly Repository's subscribers.
         */
        Map<CommandClass, MessageSubscriber> subscribers = getSubscribersFromRepositoryRoot(repository);
        Map<CommandClass, MessageSubscriber> repoSubscribers = Methods.scanForCommandHandlers(repository);
        subscribers.putAll(repoSubscribers);

        return subscribers;
    }

    private static Map<CommandClass, MessageSubscriber> getSubscribersFromRepositoryRoot(Repository repository) {
        Map<CommandClass, MessageSubscriber> result = Maps.newHashMap();

        Class<? extends AggregateRoot> rootClass = Methods.getRepositoryAggregateRootClass(repository);
        Set<CommandClass> commandClasses = Methods.getCommandClasses(rootClass);

        MessageSubscriber subscriber = Repository.Converter.toMessageSubscriber(repository);
        for (CommandClass commandClass : commandClasses) {
            result.put(commandClass, subscriber);
        }
        return result;
    }

    private static Map<CommandClass, MessageSubscriber> getSubscribers(AggregateRoot aggregateRoot) {
        Map<CommandClass, MessageSubscriber> result = Methods.scanForCommandHandlers(aggregateRoot);
        return result;
    }

    private void checkSubscribers(Map<CommandClass, MessageSubscriber> subscribers) {
        for (Map.Entry<CommandClass, MessageSubscriber> entry : subscribers.entrySet()) {
            CommandClass commandClass = entry.getKey();

            if (subscriberRegistered(commandClass)) {
                final MessageSubscriber alreadyAddedHandler = getSubscriber(commandClass);
                throw new CommandHandlerAlreadyRegisteredException(commandClass, alreadyAddedHandler, entry.getValue());
            }
        }
    }

    /**
     * Directs a command to the corresponding handler.
     *
     * @param command the command to be processed
     * @param context the context of the command
     * @return a list of the event records as the result of handling the command
     * @throws InvocationTargetException if an exception occurs during command handling
     */
    public List<EventRecord> dispatch(Command command, CommandContext context)
            throws InvocationTargetException {

        checkNotNull(command);
        checkNotNull(context);

        CommandClass commandClass = command.getCommandClass();
        if (!subscriberRegistered(commandClass)) {
            throw new UnsupportedCommandException(command.value());
        }

        MessageSubscriber subscriber = getSubscriber(commandClass);
        List<EventRecord> result = subscriber.handle(command.value(), context);
        return result;
    }

    /**
     * Directs a command to the corresponding aggregate handler.
     *
     * @param command the command to be processed
     * @param context the context of the command
     * @return a list of the event messages that were produced as the result of handling the command
     * @throws InvocationTargetException if an exception occurs during command handling
     */
    public List<? extends Message> dispatchToAggregate(Command command, CommandContext context)
            throws InvocationTargetException {

        checkNotNull(command);
        checkNotNull(context);

        MessageSubscriber subscriber = getSubscriber(command.getCommandClass());

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

    private void putAll(Map<CommandClass, MessageSubscriber> subscribers) {
        subscribersByType.putAll(subscribers);
    }

    private MessageSubscriber getSubscriber(CommandClass cls) {
        return subscribersByType.get(cls);
    }

    private boolean subscriberRegistered(CommandClass cls) {
        return subscribersByType.containsKey(cls);
    }

}
