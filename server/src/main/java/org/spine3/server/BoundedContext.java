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

import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.client.grpc.ClientServiceGrpc;
import org.spine3.client.grpc.Topic;
import org.spine3.protobuf.Messages;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandStore;
import org.spine3.server.command.CommandValidation;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.Repository;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventStore;
import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.StorageFactory;

import javax.annotation.CheckReturnValue;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

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
    //TODO:2016-01-16:alexander.yevsyukov: Set all passed storages multitenant too.
    // Or require storageFactory be multitenant and create correspondingly configured storages.
    // There should be NamespaceManager, which keeps thread-local reference to the currently set namespace.
    // Implementations like namespace support of GCP would wrap over their APIs.

    private final StorageFactory storageFactory;
    private final CommandBus commandBus;
    private final EventBus eventBus;

    private final List<Repository<?, ?>> repositories = Lists.newLinkedList();

    private BoundedContext(Builder builder) {
        this.name = builder.name;
        this.multitenant = builder.multitenant;
        this.storageFactory = builder.storageFactory;
        this.commandBus = builder.commandBus;
        this.eventBus = builder.eventBus;
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
     * Closes the {@code BoundedContext} performing all necessary clean-ups.
     *
     * <p>This method performs the following:
     * <ol>
     * <li>Closes associated {@link StorageFactory}.</li>
     * <li>Closes {@link CommandBus}.</li>
     * <li>Closes {@link EventBus}.</li>
     * <li>Closes {@link CommandStore}.</li>
     * <li>Closes {@link EventStore}.</li>
     * <li>Shuts down all registered repositories. Each registered repository is:
     *      <ul>
     *      <li>un-registered from {@link CommandBus}</li>
     *      <li>un-registered from {@link EventBus}</li>
     *      <li>detached from its storage</li>
     *      </ul>
     * </li>
     * </ol>
     * @throws Exception caused by closing one of the components
     */
    @Override
    public void close() throws Exception {
        storageFactory.close();
        commandBus.close();
        eventBus.close();

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

    private void shutDownRepositories() throws Exception {
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
     * <p>For other types of repositories an instance of {@link EntityStorage} is
     * created and assigned.
     *
     * @param repository the repository to register
     * @param <I>        the type of IDs used in the repository
     * @param <E>        the type of entities or aggregates
     * @throws IllegalArgumentException if the passed repository has no storage assigned
     */
    @SuppressWarnings({"InstanceofIncompatibleInterface", "CastToIncompatibleInterface", "ChainOfInstanceofChecks"})
        // reason for suppressing: we intentionally cast the repository to CommandHandler because custom
        // repositories may implement CommandHandler interface for handling commands that related to
        // more than one entity in the repository. For example, DeleteAll command would mark as removed (or remove)
        // all entities in the repository.
    public <I, E extends Entity<I, ?>> void register(Repository<I, E> repository) {
        checkStorageAssigned(repository);
        repositories.add(repository);
        if (repository instanceof CommandDispatcher) {
            commandBus.register((CommandDispatcher) repository);
        }
        if (repository instanceof CommandHandler) {
            commandBus.register((CommandHandler) repository);
        }
        if (repository instanceof EventDispatcher) {
            getEventBus().register((EventDispatcher)repository);
        }
    }

    private static <I, E extends Entity<I, ?>> void checkStorageAssigned(Repository<I, E> repository) {
        if (!repository.storageAssigned()) {
            throw new IllegalArgumentException("The repository " + repository + " has no assigned storage. " +
                    "Please call Repository.initStorage() before registration with BoundedContext.");
        }
    }

    @SuppressWarnings({"ChainOfInstanceofChecks", "InstanceofIncompatibleInterface", "CastToIncompatibleInterface"})
        // See comments for register(Repository<?, ?> repository).
    private void unregister(Repository<?, ?> repository) throws Exception {
        if (repository instanceof CommandDispatcher) {
            commandBus.unregister((CommandDispatcher) repository);
        }

        if (repository instanceof CommandHandler) {
            commandBus.unregister((CommandHandler)repository);
        }

        if (repository instanceof EventDispatcher) {
            getEventBus().unregister((EventDispatcher) repository);
        }

        repository.close();
    }

    @Override
    public void post(Command request, StreamObserver<Response> responseObserver) {
        final Message message = Messages.fromAny(request.getMessage());
        final CommandContext commandContext = request.getContext();

        Response reply = null;

        // Ensure `namespace` context attribute is defined in a multitenant app.
        if (isMultitenant() && !commandContext.hasNamespace()) {
            reply = CommandValidation.unknownNamespace(message, request.getContext());
        }

        if (reply == null) {
            reply = validate(message);
        }

        responseObserver.onNext(reply);
        responseObserver.onCompleted();

        if (Responses.isOk(reply)) {
            post(request);
        }
    }

    private void post(Command request)  {
        commandBus.post(request);
    }

    /**
     * Validates the incoming command message.
     *
     * @param commandMessage the command message to validate
     * @return {@link Response} with {@code ok} value if the command is valid, or
     *          with {@link org.spine3.base.Error} value otherwise
     */
    protected Response validate(Message commandMessage) {
        final Response result = commandBus.validate(commandMessage);
        return result;
    }

    @Override
    public void subscribe(Topic request, StreamObserver<Event> responseObserver) {
        //TODO:2016-01-14:alexander.yevsyukov: Implement
    }

    @Override
    public void unsubscribe(Topic request, StreamObserver<Response> responseObserver) {
        //TODO:2016-01-14:alexander.yevsyukov: Implement
    }

    /**
     * Convenience method for obtaining instance of {@link CommandBus}.
     *
     * @return instance of {@code CommandDispatcher} used in the application
     */
    @CheckReturnValue
    public CommandBus getCommandBus() {
        return this.commandBus;
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
        private CommandBus commandBus;
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

        public Builder setCommandBus(CommandBus commandBus) {
            this.commandBus = checkNotNull(commandBus);
            return this;
        }

        public CommandBus getCommandBus() {
            return commandBus;
        }

        public Builder setEventBus(EventBus eventBus) {
            this.eventBus = checkNotNull(eventBus);
            return this;
        }

        public EventBus getEventBus() {
            return eventBus;
        }

        public BoundedContext build() {
            if (this.name == null) {
                this.name = DEFAULT_NAME;
            }

            checkNotNull(storageFactory, "storageFactory must be set");
            checkNotNull(commandBus, "commandDispatcher must be set");
            checkNotNull(eventBus, "eventBus must be set");

            final BoundedContext result = new BoundedContext(this);
            // Inject EventBus into CommandBus so that it can post events from command handlers.
            commandBus.setEventBus(eventBus);

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
