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

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.*;
import org.spine3.eventbus.EventBus;
import org.spine3.protobuf.Messages;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.util.Events;

import javax.annotation.CheckReturnValue;
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

    private StorageFactory storageFactory;

    private CommandStore commandStore;
    private EventStore eventStore;

    private final List<Repository<?, ?>> repositories = Lists.newLinkedList();

    private Engine() {
        // Disallow creation of instances from outside.
    }

    /**
     * Obtains instance of the engine.
     *
     * @return {@code Engine} instance
     * @throws IllegalStateException if the engine wasn't started before calling this method
     * @see #start(StorageFactory)
     */
    @CheckReturnValue
    public static Engine getInstance() {
        final Engine engine = instance();
        engine.checkStarted();
        return engine;
    }

    private void doStart(StorageFactory storageFactory) {
        this.storageFactory = storageFactory;
        this.commandStore = new CommandStore(storageFactory.createCommandStorage());
        this.eventStore = new EventStore(storageFactory.createEventStorage());
    }

    private void checkNotStarted() {
        if (isStarted()) {
            throw new IllegalStateException("Engine already started. Call stop() before re-start.");
        }
    }

    private void checkStarted() {
        if (!isStarted()) {
            throw new IllegalStateException("Engine is not started. Call Engine.start(StorageFactory).");
        }
    }

    /**
     * Starts the engine with the passed storage factory instance.
     * <p>
     * There can be only one started instance of {@code Engine} per application. Calling this method
     * without invoking {@link #stop()} will cause {@code IllegalStateException}
     *
     * @param storageFactory the factory to be used for creating application data storages
     * @throws IllegalStateException if the method is called more than once without calling {@link #stop()} in between
     */
    public static void start(StorageFactory storageFactory) {
        log().info("Starting on storage factory: " + storageFactory.getClass());
        final Engine engine = instance();
        engine.checkNotStarted();
        engine.doStart(storageFactory);
    }

    /**
     * @return {@code true} if the engine is started, {@code false} otherwise
     */
    @CheckReturnValue
    public boolean isStarted() {
        return storageFactory != null;
    }

    /**
     * Registers the passed repository with the Engine.
     * <p>
     * The Engine creates and assigns a storage depending on the type of the passed repository.
     * <p>
     * For regular repositories an instance of {@link org.spine3.server.storage.EntityStorage} is
     * created and assigned.
     * <p>
     * For instances of {@link AggregateRepository} an instance of {@link AggregateStorage} is created
     * and assigned.
     *
     * @param repository the repository to register
     * @param <I>        the type of IDs used in the repository
     * @param <E>        the type of entities or aggregates
     */
    public <I, E extends Entity<I, ?>> void register(Repository<I, E> repository) {
        if (repository instanceof AggregateRepository) {
            final Class<? extends Aggregate<I, ?>> aggregateClass = Repository.TypeInfo.getEntityClass(repository.getClass());

            final AggregateStorage<I> aggregateStorage = storageFactory.createAggregateStorage(aggregateClass);
            repository.assignStorage(aggregateStorage);
        } else {
            final Class<? extends Entity<I, Message>> entityClass = Repository.TypeInfo.getEntityClass(repository.getClass());

            final EntityStorage entityStorage = storageFactory.createEntityStorage(entityClass);
            repository.assignStorage(entityStorage);
        }

        repositories.add(repository);

        getCommandDispatcher().register(repository);
        getEventBus().register(repository);
    }

    /**
     * Stops the engine.
     * <p>
     * This method shuts down all registered repositories. Each registered repository is:
     * <ul>
     * <li>un-registered from {@link CommandDispatcher}</li>
     * <li>un-registered from {@link EventBus}</li>
     * <li>detached from storage</li>
     * </ul>
     */
    public static void stop() {
        final Engine engine = instance();

        engine.doStop();

        log().info("Engine stopped.");
    }

    private void shutDownRepositories() {
        final CommandDispatcher dispatcher = getCommandDispatcher();
        final EventBus eventBus = getEventBus();
        for (Repository<?, ?> repository : repositories) {
            dispatcher.unregister(repository);
            eventBus.unregister(repository);
            repository.assignStorage(null);
        }
        repositories.clear();
    }

    private  void doStop() {
        shutDownRepositories();
        storageFactory = null;
        commandStore = null;
        eventStore = null;
    }

    /**
     * Processed the incoming command requests.
     * <p>
     * This method is the entry point of a command in to a backend of an application.
     * <p>
     * The engine must be started.
     *
     * @param request incoming command request to handle
     * @return the result of command handling
     * @see #start(StorageFactory)
     */
    @CheckReturnValue
    public CommandResult process(CommandRequest request) {
        checkNotNull(request);
        checkStarted();

        store(request);

        final CommandResult result = dispatch(request);
        storeAndPost(result.getEventRecordList());

        return result;
    }

    private void store(CommandRequest request) {
        commandStore.store(request);
    }

    private static CommandResult dispatch(CommandRequestOrBuilder request) {
        final CommandDispatcher dispatcher = CommandDispatcher.getInstance();
        try {
            final Message command = Messages.fromAny(request.getCommand());
            final CommandContext context = request.getContext();

            final List<EventRecord> eventRecords = dispatcher.dispatch(command, context);

            final CommandResult result = Events.toCommandResult(eventRecords, Collections.<Any>emptyList());
            return result;
        } catch (InvocationTargetException | RuntimeException e) {
            //TODO:2015-06-15:mikhail.melnik: handle errors
            throw propagate(e);
        }
    }

    private void storeAndPost(Iterable<EventRecord> records) {
        final EventBus eventBus = EventBus.getInstance();
        for (EventRecord record : records) {
            eventStore.store(record);
            eventBus.post(record);
        }
    }

    /**
     * Convenience method for obtaining instance of {@link CommandDispatcher}.
     *
     * @return instance of {@code CommandDispatcher} used in the application
     * @see CommandDispatcher#getInstance()
     */
    @CheckReturnValue
    public CommandDispatcher getCommandDispatcher() {
        return CommandDispatcher.getInstance();
    }

    /**
     * Convenience method for obtaining instance of {@link EventBus}.
     *
     * @return instance of {@code EventBus} used in the application
     * @see EventBus#getInstance()
     */
    @CheckReturnValue
    public EventBus getEventBus() {
        return EventBus.getInstance();
    }

    private static Engine instance() {
        return Singleton.INSTANCE.value;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Engine value = new Engine();
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(Engine.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
