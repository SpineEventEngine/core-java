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

import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import org.spine3.CommandClass;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.error.UnsupportedCommandException;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

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
        Map<CommandClass, MessageSubscriber> subscribers = getSubscribers(checkNotNull(handler));
        register(subscribers);
    }

    /**
     * Registers the passed hander of many commands in the dispatcher.
     *
     * @param handler a {@code non-null} handler
     */
    public void register(ManyCommandHandler handler) {
        checkNotNull(handler);
        Map<CommandClass, MessageSubscriber> subscribers = handler.getSubscribers();
        register(subscribers);
    }

    /**
     * Registers the passed subscribers with the dispatcher.
     *
     * @param subscribers map from command classes to corresponding subscribers
     */
    public void register(Map<CommandClass, MessageSubscriber> subscribers) {
        checkSubscribers(subscribers);
        putAll(subscribers);
    }

    private static Map<CommandClass, MessageSubscriber> getSubscribers(CommandHandler handler) {
        Map<CommandClass, MessageSubscriber> subscribers = ServerMethods.scanForCommandHandlers(handler);
        return subscribers;
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
    public List<EventRecord> dispatch(Message command, CommandContext context)
            throws InvocationTargetException {

        checkNotNull(command);
        checkNotNull(context);

        CommandClass commandClass = CommandClass.of(command);
        if (!subscriberRegistered(commandClass)) {
            throw new UnsupportedCommandException(command);
        }

        MessageSubscriber subscriber = getSubscriber(commandClass);
        List<EventRecord> result = subscriber.handle(command, context);
        return result;
    }

    private void putAll(Map<CommandClass, MessageSubscriber> subscribers) {
        subscribersByType.putAll(subscribers);
    }

    public MessageSubscriber getSubscriber(CommandClass cls) {
        return subscribersByType.get(cls);
    }

    public boolean subscriberRegistered(CommandClass cls) {
        return subscribersByType.containsKey(cls);
    }

}
