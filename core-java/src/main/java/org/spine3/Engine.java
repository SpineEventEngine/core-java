/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandRequest;
import org.spine3.base.CommandResult;
import org.spine3.base.EventRecord;
import org.spine3.util.Events;
import org.spine3.util.Messages;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;

/**
 * This class is a facade for configuration and entry point for handling commands.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 */
public final class Engine {

    private final CommandDispatcher dispatcher = new CommandDispatcher();

    private CommandStore commandStore;
    private EventStore eventStore;

    /**
     * Returns a singleton instance of the engine.
     *
     * @return Engine instance
     * @throws IllegalStateException if the engine was not configured
     *                               with {@link CommandStore} and {@link EventStore} instances
     * @see #configure(CommandStore, EventStore)
     */
    public static Engine getInstance() {
        final Engine engine = instance();

        if (engine.commandStore == null || engine.eventStore == null) {
            throw new IllegalStateException(
                    "Engine is not configured. Call Engine.configure() before obtaining the instance.");
        }
        return engine;
    }

    /**
     * Configures the engine with the passed implementations of command and event stores.
     *
     * @param commandStore storage for the commands
     * @param eventStore   storage for the events
     */
    public static void configure(CommandStore commandStore, EventStore eventStore) {
        final Engine engine = instance();
        engine.commandStore = commandStore;
        engine.eventStore = eventStore;
    }

    public void register(Repository repository) {
        dispatcher.register(repository);
    }

    public void register(CommandHandler handler) {
        dispatcher.register(handler);
    }

    /**
     * Handles incoming command requests.
     *
     * @param request incoming command to handle
     * @return the result of command handling
     */
    public CommandResult handle(CommandRequest request) {
        checkNotNull(request);

        store(request);

        CommandResult result = dispatch(request);
        post(result.getEventRecordList());

        return result;
    }

    private void store(CommandRequest request) {
        commandStore.store(request);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private CommandResult dispatch(CommandRequest request) {
        try {
            Message command = Messages.fromAny(request.getCommand());
            CommandContext context = request.getContext();

            List<EventRecord> eventRecords = dispatcher.dispatch(command, context);

            CommandResult result = Events.toCommandResult(eventRecords, Collections.<Any>emptyList());
            return result;
        } catch (Exception e) {
            //TODO:2015-06-15:mikhail.melnik: handle errors
            CommandResult result = Events.toCommandResult(
                    Collections.<EventRecord>emptyList(),
                    Collections.<Any>singleton(Any.getDefaultInstance()));
            throw propagate(e);
//            return result;
        }
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private void post(List<EventRecord> records) {
        for (EventRecord record : records) {
            eventStore.store(record);
            EventBus.instance().post(record);
        }
    }

    public EventStore getEventStore() {
        return eventStore;
    }

    private static Engine instance() {
        return Singleton.INSTANCE.value;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Engine value = new Engine();
    }

    private Engine() {
        // Disallow creation of instances from outside.
    }

}
