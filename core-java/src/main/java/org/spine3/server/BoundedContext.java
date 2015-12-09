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

import com.google.common.annotations.VisibleForTesting;
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
import org.spine3.server.internal.CommandHandlingObject;
import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.util.Events;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
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
public final class BoundedContext implements AutoCloseable {

    private final String name;

    private final StorageFactory storageFactory;
    private final CommandDispatcher commandDispatcher;
    private final EventBus eventBus;
    private final CommandStore commandStore;
    private final EventStore eventStore;

    private final List<Repository<?, ?>> repositories = Lists.newLinkedList();

    private BoundedContext(Builder builder) {
        this.name = builder.name;
        this.storageFactory = builder.storageFactory;
        this.commandDispatcher = builder.commandDispatcher;
        this.eventBus = builder.eventBus;
        this.commandStore = builder.commandStore;
        this.eventStore = builder.eventStore;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Closes the BoundedContext performing all necessary clean-ups.
     * <p>
     * This method shuts down all registered repositories. Each registered repository is:
     * <ul>
     * <li>un-registered from {@link CommandDispatcher}</li>
     * <li>un-registered from {@link EventBus}</li>
     * <li>detached from storage</li>
     * </ul>
     */
    @Override
    public void close() {
        shutDownRepositories();
        log().info(nameForLogging() + " closed.");
    }

    private String nameForLogging() {
        return getClass().getSimpleName() + ' ' + getName();
    }

    /**
     * @return the name of this {@code BoundedContext}
     */
    public String getName() {
        return name;
    }

    private void shutDownRepositories() {
        for (Repository<?, ?> repository : repositories) {
            unregister(repository);
        }
        repositories.clear();
    }

    /**
     * Registers the passed repository with the BoundedContext.
     *
     * <p>The context creates and assigns a storage depending on the type of the passed repository.
     *
     * <p>For instances of {@link AggregateRepository} an instance of {@link AggregateStorage} is created
     * and assigned.
     *
     * <p>For other types of repositories an instance of {@link org.spine3.server.storage.EntityStorage} is
     * created and assigned.
     *
     * @param repository the repository to register
     * @param <I>        the type of IDs used in the repository
     * @param <E>        the type of entities or aggregates
     */
    public <I, E extends Entity<I, ?>> void register(Repository<I, E> repository) {
        assignStorage(repository);

        repositories.add(repository);

        if (repository instanceof CommandHandlingObject) {
            getCommandDispatcher().register((CommandHandlingObject) repository);
        }

        getEventBus().register(repository);
    }

    private <I, E extends Entity<I, ?>> void assignStorage(Repository<I, E> repository) {
        final Object storage;
        final Class<? extends Repository> repositoryClass = repository.getClass();
        if (repository instanceof AggregateRepository) {
            final Class<? extends Aggregate<I, ?>> aggregateClass = Repository.TypeInfo.getEntityClass(repositoryClass);

            storage = storageFactory.createAggregateStorage(aggregateClass);
        } else {
            final Class<? extends Entity<I, Message>> entityClass = Repository.TypeInfo.getEntityClass(repositoryClass);

            storage = storageFactory.createEntityStorage(entityClass);
        }
        repository.assignStorage(storage);
    }

    private void unregister(Repository<?, ?> repository) {
        if (repository instanceof CommandHandlingObject) {
            getCommandDispatcher().unregister((CommandHandlingObject)repository);
        }

        getEventBus().unregister(repository);
        repository.assignStorage(null);
    }

    /**
     * Processes the incoming command request.
     *
     * <p>This method is the entry point of a command in to a backend of an application.
     *
     * @param request incoming command request to handle
     * @return the result of command handling
     */
    @CheckReturnValue
    public CommandResult process(CommandRequest request) {
        checkNotNull(request);

        store(request);

        final CommandResult result = dispatch(request);
        storeAndPostEvents(result.getEventRecordList());

        return result;
    }

    @VisibleForTesting
    EventStore getEventStore() {
        return eventStore;
    }

    @VisibleForTesting
    CommandStore getCommandStore() {
        return commandStore;
    }

    private void store(CommandRequest request) {
        getCommandStore().store(request);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private CommandResult dispatch(CommandRequest request) {
        final CommandDispatcher dispatcher = getCommandDispatcher();
        try {
            final Message command = Messages.fromAny(request.getCommand());
            final CommandContext context = request.getContext();

            final List<EventRecord> eventRecords = dispatcher.dispatch(command, context);

            final CommandResult result = Events.toCommandResult(eventRecords, Collections.<Any>emptyList());
            return result;
        } catch (InvocationTargetException | RuntimeException e) {
            throw propagate(e);
        }
    }

    private void storeAndPostEvents(Iterable<EventRecord> records) {
        final EventStore eventStore = getEventStore();
        for (EventRecord record : records) {
            eventStore.store(record);
            post(record);
        }
    }

    @SuppressWarnings("TypeMayBeWeakened") // We do not intend to post EventRecordBuilder instances into the bus.
    private void post(EventRecord eventRecord) {
        final EventBus eventBus = getEventBus();
        final Message event = Events.getEvent(eventRecord);
        final EventContext context = eventRecord.getContext();

        eventBus.post(event, context);
    }


    /**
     * Convenience method for obtaining instance of {@link CommandDispatcher}.
     *
     * @return instance of {@code CommandDispatcher} used in the application
     */
    @CheckReturnValue
    public CommandDispatcher getCommandDispatcher() {
        return this.commandDispatcher;
    }

    /**
     * Convenience method for obtaining instance of {@link EventBus}.
     *
     * @return instance of {@code EventBus} used in the application
     */
    @CheckReturnValue
    public EventBus getEventBus() {
        return this.eventBus;
    }

    /**
     * A builder for producing {@code BoundedContext} instances.
     *
     * <p>An application can have more than one bounded context. To distinguish
     * them use {@link #setName(String)}. If no name is given the default name will be assigned.
     */
    public static class Builder {

        public static final String DEFAULT_NAME = "Main";
        private String name;
        private StorageFactory storageFactory;
        private CommandDispatcher commandDispatcher;
        private EventBus eventBus;
        @Nullable
        private CommandStore commandStore;
        @Nullable
        private EventStore eventStore;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public String getName() {
            return name;
        }

        public Builder setStorageFactory(StorageFactory storageFactory) {
            this.storageFactory = checkNotNull(storageFactory);
            return this;
        }

        public StorageFactory getStorageFactory() {
            return storageFactory;
        }

        public Builder setCommandDispatcher(CommandDispatcher commandDispatcher) {
            this.commandDispatcher = checkNotNull(commandDispatcher);
            return this;
        }

        public CommandDispatcher getCommandDispatcher() {
            return commandDispatcher;
        }

        public Builder setEventBus(EventBus eventBus) {
            this.eventBus = checkNotNull(eventBus);
            return this;
        }

        public EventBus getEventBus() {
            return eventBus;
        }

        public Builder setCommandStore(@Nullable CommandStore commandStore) {
            this.commandStore = commandStore;
            return this;
        }

        @Nullable
        public CommandStore getCommandStore() {
            return commandStore;
        }

        public Builder setEventStore(@Nullable EventStore eventStore) {
            this.eventStore = eventStore;
            return this;
        }

        @Nullable
        public EventStore getEventStore() {
            return eventStore;
        }

        public BoundedContext build() {
            if (this.name == null) {
                this.name = DEFAULT_NAME;
            }

            checkNotNull(storageFactory, "storageFactory");
            checkNotNull(commandDispatcher, "commandDispatcher");
            checkNotNull(eventBus, "eventBus");

            if (commandStore == null) {
                commandStore = new CommandStore(storageFactory.createCommandStorage());
            }

            if (eventStore == null) {
                eventStore = new EventStore(storageFactory.createEventStorage());
            }

            final BoundedContext result = new BoundedContext(this);

            log().info(result.nameForLogging() + " created.");
            return result;
        }
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(BoundedContext.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
