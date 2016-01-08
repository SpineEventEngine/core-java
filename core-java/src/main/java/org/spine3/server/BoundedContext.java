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
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.*;
import org.spine3.client.ClientRequest;
import org.spine3.client.CommandRequest;
import org.spine3.client.Connection;
import org.spine3.client.grpc.ClientServiceGrpc;
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
import java.io.IOException;
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
public class BoundedContext implements ClientServiceGrpc.ClientService, AutoCloseable {

    /**
     * The name of the bounded context, which is used to distinguish the context in an application with
     * several bounded contexts.
     */
    private final String name;

    /**
     * If `true` the bounded context serves many organizations.
     */
    private final boolean multitenant;

    private final StorageFactory storageFactory;
    private final CommandDispatcher commandDispatcher;
    private final EventBus eventBus;
    private final CommandStore commandStore;
    private final EventStore eventStore;

    private final List<Repository<?, ?>> repositories = Lists.newLinkedList();

    private BoundedContext(Builder builder) {
        this.name = builder.name;
        this.multitenant = builder.multitenant;
        this.storageFactory = builder.storageFactory;
        this.commandDispatcher = builder.commandDispatcher;
        this.eventBus = builder.eventBus;
        this.commandStore = builder.commandStore;
        this.eventStore = builder.eventStore;
    }

    /**
     * Creates a new builder for {@code BoundedContext}.
     *
     * @return new builder instance
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Closes the BoundedContext performing all necessary clean-ups.
     *
     * <p>This method performs the following:
     * <ol>
     * <li>Closes associated {@link StorageFactory}.</li>
     * <li>Closes {@link CommandDispatcher}.</li>
     * <li>Closes {@link EventBus}.</li>
     * <li>Closes {@link CommandStore}.</li>
     * <li>Closes {@link EventStore}.</li>
     * <li>Shuts down all registered repositories. Each registered repository is:
     *      <ul>
     *      <li>un-registered from {@link CommandDispatcher}</li>
     *      <li>un-registered from {@link EventBus}</li>
     *      <li>detached from its storage</li>
     *      </ul>
     * </li>
     * </ol>
     * @throws IOException caused by closing one of the components
     */
    @Override
    public void close() throws IOException {
        storageFactory.close();
        commandDispatcher.close();
        eventBus.close();
        commandStore.close();
        eventStore.close();

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

    /**
     * @return {@code true} if the bounded context serves many organizations
     */
    @CheckReturnValue
    public boolean isMultitenant() {
        return multitenant;
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
            getCommandDispatcher().unregister((CommandHandlingObject) repository);
        }

        getEventBus().unregister(repository);
        repository.assignStorage(null);
    }

    @Override
    public void connect(ClientRequest request, StreamObserver<Connection> responseObserver) {
        //TODO:2015-12-21:alexander.yevsyukov: Implement
    }

    @Override
    public void post(CommandRequest request, StreamObserver<Response> responseObserver) {
        final Message command = Messages.fromAny(request.getCommand());
        final CommandContext commandContext = request.getContext();

        Response reply = null;

        // Ensure `namespace` is defined in a multitenant app.
        if (isMultitenant() && !commandContext.hasNamespace()) {
            reply = CommandValidation.unknownNamespace(command, request.getContext());
        }

        if (reply == null) {
            reply = validate(command);
        }

        responseObserver.onNext(reply);
        responseObserver.onCompleted();

        if (Responses.isOk(reply)) {
            handle(request);
        }
    }

    @Override
    public void getEvents(Connection request, StreamObserver<EventRecord> responseObserver) {
        //TODO:2015-12-21:alexander.yevsyukov: Implement
    }

    /**
     * Validates the incoming command.
     *
     * @param command the command to validate
     * @return {@link Response} with {@code ok} value if the command is valid, or
     *          with {@link org.spine3.base.Error} value otherwise
     */
    protected Response validate(Message command) {
        final CommandDispatcher dispatcher = getCommandDispatcher();
        final Response result = dispatcher.validate(command);
        return result;
    }

    private void handle(CommandRequest request) {
        //TODO:2015-12-16:alexander.yevsyukov: Deal with async. execution of the request.
        process(request);

        //TODO:2015-12-16:alexander.yevsyukov: Return results to the client through ClientService
    }

    //TODO:2016-01-08:alexander.yevsyukov: Hide this method in favor of a call from client via gRPC.
    /**
     * Processes the incoming command request.
     *
     * <p>This method is the entry point of a command in to a backend of an application.
     *
     * @param request incoming command request to handle
     * @return the result of command handling
     */
    public CommandResult process(CommandRequest request) {
        checkNotNull(request);

        store(request);

        final CommandResult result = dispatch(request);
        final List<EventRecord> eventRecords = result.getEventRecordList();

        storeEvents(eventRecords);
        postEvents(eventRecords);

        //TODO:2015-12-16:alexander.yevsyukov: Notify clients.

        return result;
    }

    @VisibleForTesting
    protected EventStore getEventStore() {
        return eventStore;
    }

    @VisibleForTesting
    protected CommandStore getCommandStore() {
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

            final CommandResult result = toCommandResult(eventRecords, Collections.<Any>emptyList());
            return result;
        } catch (InvocationTargetException | RuntimeException e) {
            throw propagate(e);
        }
    }

    private static CommandResult toCommandResult(Iterable<EventRecord> eventRecords, Iterable<Any> errors) {
        return CommandResult.newBuilder()
                .addAllEventRecord(eventRecords)
                .addAllError(errors)
                .build();
    }

    /**
     * Stores passed events in {@link EventStore}.
     */
    private void storeEvents(Iterable<EventRecord> records) {
        final EventStore eventStore = getEventStore();
        for (EventRecord record : records) {
            eventStore.append(record);
        }
    }

    /**
     * Posts passed events to {@link EventBus}.
     */
    private void postEvents(Iterable<EventRecord> records) {
        final EventBus eventBus = getEventBus();
        for (EventRecord record : records) {
            final Message event = Events.getEvent(record);
            final EventContext context = record.getContext();

            eventBus.post(event, context);
        }
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
        /**
         * The default name of {@code BoundedContext}.
         */
        public static final String DEFAULT_NAME = "Main";

        private String name;
        private StorageFactory storageFactory;
        private CommandStore commandStore;
        private EventStore eventStore;
        private CommandDispatcher commandDispatcher;
        private EventBus eventBus;
        private boolean multitenant;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public String getName() {
            return name;
        }

        public Builder setMultitenant(boolean value) {
            this.multitenant = value;
            return this;
        }

        public boolean isMultitenant() {
            return this.multitenant;
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
                eventStore = EventStore.create(
                        MoreExecutors.directExecutor(),
                        storageFactory.createEventStorage());
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
