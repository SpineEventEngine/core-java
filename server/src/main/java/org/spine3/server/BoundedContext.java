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

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandDispatcher;
import org.spine3.server.command.CommandStore;
import org.spine3.server.entity.AbstractVersionableEntity;
import org.spine3.server.entity.Repository;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventDispatcher;
import org.spine3.server.failure.FailureBus;
import org.spine3.server.integration.IntegrationEvent;
import org.spine3.server.integration.IntegrationEventContext;
import org.spine3.server.integration.grpc.IntegrationEventSubscriberGrpc;
import org.spine3.server.stand.Stand;
import org.spine3.server.stand.StandFunnel;
import org.spine3.server.stand.StandStorage;
import org.spine3.server.stand.StandUpdateDelivery;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.StorageFactorySwitch;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.util.Logging.closed;
import static org.spine3.validate.Validate.checkNameNotEmptyOrBlank;

/**
 * A facade for configuration and entry point for handling commands.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 */
public final class BoundedContext extends IntegrationEventSubscriberGrpc.IntegrationEventSubscriberImplBase
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

    private final CommandBus commandBus;
    private final EventBus eventBus;
    private final FailureBus failureBus;
    private final Stand stand;
    private final StandFunnel standFunnel;

    /** All the repositories registered with this bounded context */
    private final List<Repository<?, ?>> repositories = Lists.newLinkedList();

    /**
     * The map from a type of aggregate state to an aggregate repository instance that
     * manages such aggregates.
     */
    private final Map<Class<? extends Message>, AggregateRepository<?, ?>> aggregateRepositories = Maps.newHashMap();

    /**
     * Memoized version of the {@code StorageFactory} supplier passed to the constructor.
     */
    private final Supplier<StorageFactory> storageFactory;

    private BoundedContext(Builder builder) {
        super();
        this.name = builder.name;
        this.multitenant = builder.multitenant;
        this.storageFactory = Suppliers.memoize(builder.storageFactorySupplier);
        this.commandBus = builder.commandBus;
        this.eventBus = builder.eventBus;
        this.failureBus = builder.failureBus;
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
     * <li>Closes {@link org.spine3.server.event.EventStore EventStore}.
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
        storageFactory.get().close();
        commandBus.close();
        eventBus.close();
        failureBus.close();
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
    @SuppressWarnings("ChainOfInstanceofChecks") // OK here since ways of registering are way too different
    public <I, E extends AbstractVersionableEntity<I, ?>> void register(Repository<I, E> repository) {
        checkStorageAssigned(repository);
        repositories.add(repository);
        if (repository instanceof CommandDispatcher) {
            commandBus.register((CommandDispatcher) repository);
        }
        if (repository instanceof EventDispatcher) {
            eventBus.register((EventDispatcher) repository);
        }
        if (repository instanceof AggregateRepository) {
            registerAggregateRepository((AggregateRepository)repository);
        }
        stand.registerTypeSupplier(repository);
    }

    private void checkStorageAssigned(Repository repository) {
        if (!repository.storageAssigned()) {
            repository.initStorage(storageFactory.get());
        }
    }

    private void registerAggregateRepository(AggregateRepository<?, ?> repository) {
        final Class<? extends Message> stateClass = repository.getAggregateStateClass();
        final AggregateRepository<?, ?> alreadyRegistered = aggregateRepositories.get(stateClass);
        if (alreadyRegistered != null) {
            final String errMsg = String.format("Repository for aggregates with the state %s already registered: %s",
                                                stateClass, alreadyRegistered);
            throw new IllegalStateException(errMsg);
        }
        aggregateRepositories.put(stateClass, repository);
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

    /** Obtains instance of {@link FailureBus} of this {@code BoundedContext}. */
    @CheckReturnValue
    public FailureBus getFailureBus() {return this.failureBus;}

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
     * Obtains an {@code AggregateRepository} which manages aggregates with the passed state.
     *
     * @param aggregateStateClass the class of the aggregate state
     * @return repository instance or empty {@code Optional} if not found
     */
    public Optional<? extends AggregateRepository<?, ?>> getAggregateRepository(
            Class<? extends Message> aggregateStateClass) {
        final AggregateRepository<?, ?> result = aggregateRepositories.get(aggregateStateClass);
        return Optional.fromNullable(result);
    }

    /**
     * A builder for producing {@code BoundedContext} instances.
     *
     * <p>An application can have more than one bounded context. To distinguish
     * them use {@link #setName(String)}. If no name is given the default name will be assigned.
     */
    public static class Builder {

        private String name = DEFAULT_NAME;
        private Supplier<StorageFactory> storageFactorySupplier;
        private CommandStore commandStore;
        private CommandBus commandBus;
        private EventBus eventBus;
        private FailureBus failureBus;
        private boolean multitenant;
        private Stand stand;
        private StandUpdateDelivery standUpdateDelivery;
        private StandFunnel standFunnel;

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
            this.name = checkNameNotEmptyOrBlank(name);
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

        /**
         * Sets the supplier for {@code StorageFactory}.
         *
         * <p>If the supplier was not set or {@code null} was passed,
         * {@link StorageFactorySwitch} will be used during the construction of
         * a {@code BoundedContext} instance.
         */
        public Builder setStorageFactorySupplier(@Nullable Supplier<StorageFactory> supplier) {
            this.storageFactorySupplier = supplier;
            return this;
        }

        public Optional<Supplier<StorageFactory>> storageFactorySupplier() {
            return Optional.fromNullable(storageFactorySupplier);
        }

        public Builder setCommandStore(CommandStore commandStore) {
            this.commandStore = checkNotNull(commandStore);
            return this;
        }

        public Optional<CommandStore> commandStore() {
            return Optional.fromNullable(commandStore);
        }

        public Builder setCommandBus(CommandBus commandBus) {
            this.commandBus = checkNotNull(commandBus);
            return this;
        }

        public Optional<CommandBus> commandBus() {
            return Optional.fromNullable(commandBus);
        }


        public Builder setEventBus(EventBus eventBus) {
            this.eventBus = checkNotNull(eventBus);
            return this;
        }

        public Optional<EventBus> eventBus() {
            return Optional.fromNullable(eventBus);
        }

        public Builder setFailureBus(FailureBus failureBus) {
            this.failureBus = checkNotNull(failureBus);
            return this;
        }

        public Optional<FailureBus> failureBus() {
            return Optional.fromNullable(failureBus);
        }

        public Builder setStand(Stand stand) {
            this.stand = checkNotNull(stand);
            return this;
        }

        public Optional<Stand> stand() {
            return Optional.fromNullable(stand);
        }

        public Optional<StandUpdateDelivery> standUpdateDelivery() {
            return Optional.fromNullable(standUpdateDelivery);
        }

        public Builder setStandUpdateDelivery(StandUpdateDelivery standUpdateDelivery) {
            this.standUpdateDelivery = standUpdateDelivery;
            return this;
        }

        public BoundedContext build() {
            if (storageFactorySupplier == null) {
                storageFactorySupplier = StorageFactorySwitch.getInstance();
            }

            final StorageFactory storageFactory = storageFactorySupplier.get();

            if (storageFactory == null) {
                throw new IllegalStateException("Supplier of StorageFactory (" + storageFactorySupplier +
                                                        ") returned null instance");
            }

            /* If some of the properties were not set, create them using set StorageFactory. */
            if (commandStore == null) {
                commandStore = createCommandStore(storageFactory);
            }
            if (commandBus == null) {
                commandBus = createCommandBus(storageFactory);
            }
            if (eventBus == null) {
                eventBus = createEventBus(storageFactory);
            }
            if(failureBus == null) {
                failureBus = createFailureBus();
            }

            if (stand == null) {
                stand = createStand(storageFactory);
            }

            standFunnel = createStandFunnel(standUpdateDelivery);

            commandBus.setMultitenant(this.multitenant);

            final BoundedContext result = new BoundedContext(this);

            log().info(result.nameForLogging() + " created.");
            return result;
        }

        private StandFunnel createStandFunnel(@Nullable StandUpdateDelivery standUpdateDelivery) {
            final StandFunnel.Builder builder = StandFunnel.newBuilder()
                                                           .setStand(stand);
            if (standUpdateDelivery != null) {
                builder.setDelivery(standUpdateDelivery);
            }
            return builder.build();
        }

        private static CommandStore createCommandStore(StorageFactory storageFactory) {
            final CommandStore result = new CommandStore(storageFactory.createCommandStorage());
            return result;
        }

        private CommandBus createCommandBus(StorageFactory storageFactory) {
            if (commandStore == null) {
                this.commandStore = createCommandStore(storageFactory);
            }
            final CommandBus commandBus = CommandBus.newBuilder()
                                                    .setCommandStore(commandStore)
                                                    .build();
            return commandBus;
        }

        private static EventBus createEventBus(StorageFactory storageFactory) {
            final EventBus result = EventBus.newBuilder()
                                            .setStorageFactory(storageFactory)
                                            .build();
            return result;
        }

        private static FailureBus createFailureBus() {
            final FailureBus result = FailureBus.newBuilder()
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
