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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandRequest;
import org.spine3.base.CommandResult;
import org.spine3.base.EventRecord;
import org.spine3.protobuf.Messages;
import org.spine3.util.Events;

import java.lang.reflect.InvocationTargetException;
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
     *                               with {@link CommandStore} and {@link RepositoryEventStore} instances
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
     */
    public static void configure(CommandStore commandStore, EventStore eventStore) {
        final Engine engine = instance();
        engine.commandStore = commandStore;
        engine.eventStore = eventStore;
    }

    public void register(CommandHandler handler) {
        dispatcher.register(handler);
    }

    public void register(ManyCommandHandler repository) {
        dispatcher.register(repository);
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
        storeAndPost(result.getEventRecordList());

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
        } catch (InvocationTargetException | RuntimeException e) {
            //TODO:2015-06-15:mikhail.melnik: handle errors
            CommandResult result = Events.toCommandResult(
                    Collections.<EventRecord>emptyList(),
                    Collections.<Any>singleton(Any.getDefaultInstance()));
            throw propagate(e);
//            return result;
        }
    }

    private void storeAndPost(Iterable<EventRecord> records) {
        for (EventRecord record : records) {
            eventStore.store(record);
            EventBus.instance().post(record);
        }
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
