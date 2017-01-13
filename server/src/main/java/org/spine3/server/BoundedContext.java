/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.base.Response;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandDispatcher;
import org.spine3.server.command.CommandStore;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.Repository;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventDispatcher;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.enrich.EventEnricher;
import org.spine3.server.integration.IntegrationEvent;
import org.spine3.server.integration.IntegrationEventContext;
import org.spine3.server.integration.grpc.IntegrationEventSubscriberGrpc;
import org.spine3.server.stand.Stand;
import org.spine3.server.stand.StandFunnel;
import org.spine3.server.storage.StandStorage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.validate.Validate;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.util.Logging.closed;

/**
 * A facade for configuration and entry point for handling commands.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 */
public class BoundedContext extends IntegrationEventSubscriberGrpc.IntegrationEventSubscriberImplBase
        implements AutoCloseable {

    /** The default name for a {@code BoundedContext}. */
    public static final String DEFAULT_NAME = "Main";

    /**
     * The name of the bounded context, which is used to distinguish the context in an application with
     * several bounded contexts.
     */
    private final String name;

    /** If `true` the bounded context serves many organizations. */
    private final boolean multitenant;
    private final StorageFactory storageFactory;
    private final CommandBus commandBus;
    private final EventBus eventBus;
    private final Stand stand;
    private final StandFunnel standFunnel;

    private final List<Repository<?, ?>> repositories = Lists.newLinkedList();

    private BoundedContext(Builder builder) {
        super();
        this.name = builder.name;
        this.multitenant = builder.multitenant;
        this.storageFactory = builder.storageFactory;
        this.commandBus = builder.commandBus;
        this.eventBus = builder.eventBus;
        this.stand = builder.stand;
        this.standFunnel = builder.standFunnel;
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
     * <li>Closes {@link Stand}.
     * <li>Shuts down all registered repositories. Each registered repository is:
     *      <ul>
     *      <li>un-registered from {@link CommandBus}
     *      <li>un-registered from {@link EventBus}
     *      <li>detached from its storage
     *      </ul>
     * </ol>
     *
     * @throws Exception caused by closing one of the components
     */
    @Override
    public void close() throws Exception {
        storageFactory.close();
        commandBus.close();
        eventBus.close();
        stand.close();

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
        stand.registerTypeSupplier(repository);
    }

    private void checkStorageAssigned(Repository repository) {
        if (!repository.storageAssigned()) {
            repository.initStorage(this.storageFactory);
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod") /* We ignore method from super because the default
                                                        implementation sets unimplemented status. */
    @Override
    public void notify(IntegrationEvent integrationEvent, StreamObserver<Response> responseObserver) {
        final Message eventMsg = unpack(integrationEvent.getMessage());
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
                                                 .setProducerId(AnyPacker.pack(producerId))
                                                 .build();
        final Event result = Events.createEvent(integrationEvent.getMessage(), context);
        return result;
    }

    /** Obtains instance of {@link CommandBus} of this {@code BoundedContext}. */
    @CheckReturnValue
    public CommandBus getCommandBus() {
        return this.commandBus;
    }

    /** Obtains instance of {@link EventBus} of this {@code BoundedContext}. */
    @CheckReturnValue
    public EventBus getEventBus() {
        return this.eventBus;
    }

    /** Obtains instance of {@link StandFunnel} of this {@code BoundedContext}. */
    @CheckReturnValue
    public StandFunnel getStandFunnel() {
        return this.standFunnel;
    }

    /** Obtains instance of {@link Stand} of this {@code BoundedContext}. */
    @CheckReturnValue
    public Stand getStand() {
        return stand;
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
        private EventBus eventBus;
        private boolean multitenant;
        private Stand stand;
        private StandFunnel standFunnel;
        private Executor standFunnelExecutor;

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


        public Builder setEventBus(EventBus eventBus) {
            this.eventBus = checkNotNull(eventBus);
            return this;
        }

        @Nullable
        public EventBus getEventBus() {
            return eventBus;
        }

        public Builder setStand(Stand stand) {
            this.stand = checkNotNull(stand);
            return this;
        }

        @Nullable
        public Stand getStand() {
            return stand;
        }

        @Nullable
        public Executor getStandFunnelExecutor() {
            return standFunnelExecutor;
        }

        public Builder setStandFunnelExecutor(Executor standFunnelExecutor) {
            this.standFunnelExecutor = standFunnelExecutor;
            return this;
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
            if (eventBus == null) {
                eventBus = createEventBus();
            }

            if (stand == null) {
                stand = createStand(storageFactory);
            }

            standFunnel = createStandFunnel(standFunnelExecutor);

            commandBus.setMultitenant(this.multitenant);

            final BoundedContext result = new BoundedContext(this);

            log().info(result.nameForLogging() + " created.");
            return result;
        }

        private StandFunnel createStandFunnel(@Nullable Executor standFunnelExecutor) {
            StandFunnel standFunnel;
            if (standFunnelExecutor == null) {
                standFunnel = StandFunnel.newBuilder()
                                         .setStand(stand)
                                         .build();
            } else {
                standFunnel = StandFunnel.newBuilder()
                                         .setExecutor(standFunnelExecutor)
                                         .setStand(stand)
                                         .build();
            }
            return standFunnel;
        }

        private CommandStore createCommandStore() {
            final CommandStore result = new CommandStore(storageFactory.createCommandStorage());
            return result;
        }

        private CommandBus createCommandBus() {
            if (commandStore == null) {
                this.commandStore = createCommandStore();
            }
            final CommandBus commandBus = CommandBus.newBuilder()
                                                    .setCommandStore(commandStore)
                                                    .build();
            return commandBus;
        }

        private EventBus createEventBus() {
            final EventBus result = EventBus.newBuilder()
                                            .setStorageFactory(storageFactory)
                                            .build();
            return result;
        }

        private static Stand createStand(StorageFactory storageFactory) {
            final StandStorage standStorage = storageFactory.createStandStorage();
            final Stand result = Stand.newBuilder()
                                      .setStorage(standStorage)
                                      .build();
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
