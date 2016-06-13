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
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Response;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandDispatcher;
import org.spine3.server.command.CommandStore;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.Repository;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventDispatcher;
import org.spine3.server.event.EventStore;
import org.spine3.server.integration.IntegrationEvent;
import org.spine3.server.integration.IntegrationEventContext;
import org.spine3.server.integration.grpc.IntegrationEventSubscriberGrpc.IntegrationEventSubscriber;
import org.spine3.server.storage.StorageFactory;
import org.spine3.validate.Validate;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.util.Logging.closed;

/**
 * A facade for configuration and entry point for handling commands.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 */
public class BoundedContext implements IntegrationEventSubscriber, AutoCloseable {
    /**
     * The default name for a {@code BoundedContext}.
     */
    public static final String DEFAULT_NAME = "Main";

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
     * <li>Closes associated {@link StorageFactory}.
     * <li>Closes {@link CommandBus}.
     * <li>Closes {@link EventBus}.
     * <li>Closes {@link CommandStore}.
     * <li>Closes {@link EventStore}.
     * <li>Shuts down all registered repositories. Each registered repository is:
     *      <ul>
     *      <li>un-registered from {@link CommandBus}
     *      <li>un-registered from {@link EventBus}
     *      <li>detached from its storage
     *      </ul>
     * </ol>
     * @throws Exception caused by closing one of the components
     */
    @Override
    public void close() throws Exception {
        storageFactory.close();
        commandBus.close();
        eventBus.close();

        shutDownRepositories();

        log().info(closed(nameForLogging()));
    }

    private void shutDownRepositories() throws Exception {
        for (Repository<?, ?> repository : repositories) {
            repository.close();
        }
        repositories.clear();
    }

    private String nameForLogging() {
        return getClass().getSimpleName() + ' ' + getName();
    }

    /**
     * Obtains a name of the bounded context.
     *
     * <p>The name allows to identify a bounded context if a multi-context application.
     * If the name was not defined, during the building process, the context would get {@link #DEFAULT_NAME}.
     *
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

    /**
     * Registers the passed repository with the {@code BoundedContext}.
     *
     * <p>If the repository does not have a storage assigned, it will be initialized
     * using the {@code StorageFactory} associated with this bounded context.
     *
     * @param repository the repository to register
     * @param <I>        the type of IDs used in the repository
     * @param <E>        the type of entities or aggregates
     * @see Repository#initStorage(StorageFactory)
     */
    @SuppressWarnings("ChainOfInstanceofChecks")
    public <I, E extends Entity<I, ?>> void register(Repository<I, E> repository) {
        checkStorageAssigned(repository);
        repositories.add(repository);
        if (repository instanceof CommandDispatcher) {
            commandBus.register((CommandDispatcher) repository);
        }
        if (repository instanceof EventDispatcher) {
            eventBus.register((EventDispatcher) repository);
        }
    }

    private void checkStorageAssigned(Repository repository) {
        if (!repository.storageAssigned()) {
            repository.initStorage(this.storageFactory);
        }
    }

    @Override
    public void notify(IntegrationEvent integrationEvent, StreamObserver<Response> responseObserver) {
        final Message eventMsg = fromAny(integrationEvent.getMessage());
        final boolean isValid = eventBus.validate(eventMsg, responseObserver);
        if (isValid) {
            final Event event = toEvent(integrationEvent);
            eventBus.post(event);
        }
    }

    private static Event toEvent(IntegrationEvent integrationEvent) {
        final IntegrationEventContext sourceContext = integrationEvent.getContext();
        final StringValue producerId = newStringValue(sourceContext.getBoundedContextName());
        final EventContext context = EventContext.newBuilder()
                .setEventId(sourceContext.getEventId())
                .setTimestamp(sourceContext.getTimestamp())
                .setProducerId(toAny(producerId))
                .build();
        final Event.Builder result = Event.newBuilder()
                .setMessage(integrationEvent.getMessage())
                .setContext(context);
        return result.build();
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

        private String name = DEFAULT_NAME;
        private StorageFactory storageFactory;
        private CommandStore commandStore;
        private CommandBus commandBus;
        private EventStore eventStore;
        private Executor eventStoreStreamExecutor;
        private EventBus eventBus;
        private boolean multitenant;

        /**
         * Sets the name for a new bounded context.
         *
         * <p>If the name is not defined in the builder, the context will get {@link #DEFAULT_NAME}.
         *
         * <p>It is the responsibility of an application developer to provide meaningful and unique
         * names for bounded contexts. The framework does not check for duplication of names.
         *
         * @param name a name for a new bounded context. Cannot be null, empty, or blank
         */
        public Builder setName(String name) {
            this.name = Validate.checkNotEmptyOrBlank(name, "name");
            return this;
        }

        /**
         * Returns the previously set name or {@link #DEFAULT_NAME}
         * if the name was not explicitly set.
         */
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

        @Nullable
        public StorageFactory getStorageFactory() {
            return storageFactory;
        }

        public Builder setCommandStore(CommandStore commandStore) {
            this.commandStore = checkNotNull(commandStore);
            return this;
        }

        @Nullable
        public CommandStore getCommandStore() {
            return this.commandStore;
        }

        public Builder setCommandBus(CommandBus commandBus) {
            this.commandBus = checkNotNull(commandBus);
            return this;
        }

        @Nullable
        public CommandBus getCommandBus() {
            return commandBus;
        }

        /**
         * Specifies {@code EventStore} to be used when creating new {@code EventBus}.
         *
         * <p>This method can be called if {@link #setEventStoreStreamExecutor(Executor)}
         * was not called before.
         *
         * @see #setEventStoreStreamExecutor(Executor)
         */
        @SuppressWarnings("unused")
        public Builder setEventStore(EventStore eventStore) {
            checkState(eventStoreStreamExecutor == null, "eventStoreStreamExecutor already set.");
            this.eventStore = checkNotNull(eventStore);
            return this;
        }

        /**
         * Specifies an {@code Executor} for returning event stream from {@code EventStore}.
         *
         * <p>This {@code Executor} instance will be used for creating
         * new {@code EventStore} instance when building {@code BoundedContext}, <em>if</em>
         * {@code EventStore} was not explicitly set in the builder.
         *
         * <p>If an {@code Executor} is not set in the builder, {@link MoreExecutors#directExecutor()}
         * will be used.
         *
         * @see #setEventStore(EventStore)
         */
        @SuppressWarnings("MethodParameterNamingConvention")
        public Builder setEventStoreStreamExecutor(Executor eventStoreStreamExecutor) {
            checkState(eventStore == null, "EventStore is already configured.");
            this.eventStoreStreamExecutor = eventStoreStreamExecutor;
            return this;
        }

        @Nullable
        public Executor getEventStoreStreamExecutor() {
            return eventStoreStreamExecutor;
        }

        @Nullable
        public EventStore getEventStore() {
            return eventStore;
        }

        public Builder setEventBus(EventBus eventBus) {
            this.eventBus = checkNotNull(eventBus);
            return this;
        }

        @Nullable
        public EventBus getEventBus() {
            return eventBus;
        }

        public BoundedContext build() {
            checkNotNull(storageFactory, "storageFactory must be set");

            /* If some of the properties were not set, create them using set StorageFactory. */
            if (commandStore == null) {
                commandStore = createCommandStore();
            }
            if (commandBus == null) {
                commandBus = createCommandBus();
            }
            if (eventStore == null) {
                eventStore = createEventStore();
            }
            if (eventBus == null) {
                eventBus = createEventBus();
            }

            commandBus.setMultitenant(this.multitenant);

            final BoundedContext result = new BoundedContext(this);

            log().info(result.nameForLogging() + " created.");
            return result;
        }

        private CommandStore createCommandStore() {
            final CommandStore result = new CommandStore(storageFactory.createCommandStorage());
            return result;
        }

        private EventStore createEventStore() {
            if (eventStoreStreamExecutor == null) {
                this.eventStoreStreamExecutor = MoreExecutors.directExecutor();
            }

            final EventStore result = EventStore.newBuilder()
                                                .setStreamExecutor(eventStoreStreamExecutor)
                                                .setStorage(storageFactory.createEventStorage())
                                                .setLogger(EventStore.log())
                                                .build();
            return result;
        }

        private CommandBus createCommandBus() {
            if (commandStore == null) {
                this.commandStore = createCommandStore();
            }
            final CommandBus commandBus = CommandBus.newInstance(commandStore);
            return commandBus;
        }

        private EventBus createEventBus() {
            if (eventStore == null) {
                this.eventStore = createEventStore();
            }
            final EventBus eventBus = EventBus.newInstance(eventStore);
            return eventBus;
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
