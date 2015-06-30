/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3;

import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import org.spine3.AggregateRoot;
import org.spine3.CommandHandler;
import org.spine3.Repository;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.engine.MessageSubscriber;
import org.spine3.lang.CommandHandlerAlreadyRegisteredException;
import org.spine3.lang.UnsupportedCommandException;
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

    private final Map<Class<? extends Message>, MessageSubscriber> subscribersByType = Maps.newConcurrentMap();

    /**
     * Register the passed command handler in the dispatcher.
     *
     * @param handler a command handler object
     */
    public void register(CommandHandler handler) {
        checkNotNull(handler);

        Map<Class<? extends Message>, MessageSubscriber> subscribers = getSubscribers(handler);
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

        Map<Class<? extends Message>, MessageSubscriber> subscribers = getSubscribers(repository);
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

        Map<Class<? extends Message>, MessageSubscriber> subscribers = getSubscribers(aggregateRoot);
        checkSubscribers(subscribers);

        putAll(subscribers);
    }

    private static Map<Class<? extends Message>, MessageSubscriber> getSubscribers(CommandHandler handler) {
        //TODO:2015-06-15:mikhail.melnik: check that method is called "handle()"
        //or maybe we do not need handler() in the API as we use usual class scanning
        Map<Class<? extends Message>, MessageSubscriber> result = Methods.scanForCommandSubscribers(handler);
        return result;
    }

    private static Map<Class<? extends Message>, MessageSubscriber> getSubscribers(Repository repository) {
        /*
           The order inside this method is important!

           At first we add all subscribers from the aggregate root
           and after that register directly Repository's subscribers.
         */
        Map<Class<? extends Message>, MessageSubscriber> result = getSubscribersFromRepositoryRoot(repository);

        Map<Class<? extends Message>, MessageSubscriber> subscribers = Methods.scanForCommandSubscribers(repository);
        result.putAll(subscribers);

        return result;
    }

    private static Map<Class<? extends Message>, MessageSubscriber> getSubscribersFromRepositoryRoot(Repository repository) {
        Map<Class<? extends Message>, MessageSubscriber> result = Maps.newHashMap();

        Class<? extends AggregateRoot> rootClass = Methods.getRepositoryAggregateRootClass(repository);
        Set<Class<? extends Message>> commandClasses = Methods.getCommandClasses(rootClass);

        MessageSubscriber subscriber = MessageSubscriber.fromRepository(repository);
        for (Class<? extends Message> commandClass : commandClasses) {
            result.put(commandClass, subscriber);
        }
        return result;
    }

    private static Map<Class<? extends Message>, MessageSubscriber> getSubscribers(AggregateRoot aggregateRoot) {
        Map<Class<? extends Message>, MessageSubscriber> result = Methods.scanForCommandSubscribers(aggregateRoot);
        return result;
    }

    private void checkSubscribers(Map<Class<? extends Message>, MessageSubscriber> subscribers) {
        for (Map.Entry<Class<? extends Message>, MessageSubscriber> entry : subscribers.entrySet()) {
            Class<? extends Message> commandClass = entry.getKey();

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
    public List<EventRecord> dispatch(Message command, CommandContext context)
            throws InvocationTargetException {

        checkNotNull(command);
        checkNotNull(context);

        Class<? extends Message> commandClass = command.getClass();
        if (!subscriberRegistered(commandClass)) {
            throw new UnsupportedCommandException(command);
        }

        MessageSubscriber subscriber = getSubscriber(commandClass);
        List<EventRecord> result = subscriber.handle(command, context);
        return result;
    }

    /**
     * Directs a command to the corresponding aggregate handler.
     *
     * @param command the command to be processed
     * @param context the context of the command
     * @return a list of the events that were produced as the result of handling the command
     * @throws InvocationTargetException if an exception occurs during command handling
     */
    public List<? extends Message> dispatchToAggregate(Message command, CommandContext context)
            throws InvocationTargetException {

        checkNotNull(command);
        checkNotNull(context);

        MessageSubscriber subscriber = getSubscriber(command.getClass());

        Object handlingResult = subscriber.handle(command, context);

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

    private void putAll(Map<Class<? extends Message>, MessageSubscriber> subscribers) {
        subscribersByType.putAll(subscribers);
    }

    private MessageSubscriber getSubscriber(Class<? extends Message> commandClass) {
        return subscribersByType.get(commandClass);
    }

    private boolean subscriberRegistered(Class<? extends Message> commandClass) {
        return subscribersByType.containsKey(commandClass);
    }

}
