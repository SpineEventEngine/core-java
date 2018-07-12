/*
 * Copyright 2018, TeamDev. All rights reserved.
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
package io.spine.server;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Experimental;
import io.spine.annotation.Internal;
import io.spine.core.Ack;
import io.spine.core.BoundedContextName;
import io.spine.core.BoundedContextNames;
import io.spine.core.Event;
import io.spine.option.EntityOption.Visibility;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.entity.Entity;
import io.spine.server.entity.Repository;
import io.spine.server.entity.VisibilityGuard;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventFactory;
import io.spine.server.integration.IntegrationBus;
import io.spine.server.integration.IntegrationEvent;
import io.spine.server.integration.grpc.IntegrationEventSubscriberGrpc;
import io.spine.server.model.Model;
import io.spine.server.rejection.RejectionBus;
import io.spine.server.stand.Stand;
import io.spine.server.stand.StandStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.StorageFactorySwitch;
import io.spine.server.tenant.TenantIndex;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.system.server.SystemGateway;
import io.spine.type.TypeName;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.core.BoundedContextNames.newName;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A logical and structural boundary of a model.
 *
 * <p>Logically, a bounded context represents a sub-system, built to be described with the same
 * ubiquitous language. Any term within a single bounded context has a single meaning and
 * may or may not map to another term in the language of another bounded context.
 *
 * <p>The ubiquitous language of a bounded context is represented by the entity state, event,
 * and command types, entity types, etc. An entity and its adjacent types belong to the bounded
 * context, which the entity {@link Repository} is
 * {@linkplain BoundedContext#register(Repository) registered} in.
 *
 * <p>Structurally, a bounded context brings together all the infrastructure required for
 * the components of a model to cooperate.
 *
 * <p>An instance of {@code BoundedContext} acts as a major point of configuration for all
 * the model elements which belong to it.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 * @author Dmitry Ganzha
 * @author Dmytro Dashenkov
 * @see <a href="https://martinfowler.com/bliki/BoundedContext.html">
 * Blog post on bounded contexts</a>
 */
public abstract class BoundedContext
        extends IntegrationEventSubscriberGrpc.IntegrationEventSubscriberImplBase
        implements AutoCloseable {

    /**
     * The name of the bounded context, which is used to distinguish the context in an application
     * with several bounded contexts.
     */
    private final BoundedContextName name;

    /** If {@code true} the bounded context serves many tenants. */
    private final boolean multitenant;

    private final CommandBus commandBus;
    private final EventBus eventBus;
    private final IntegrationBus integrationBus;
    private final Stand stand;

    /** Controls access to entities of all repositories registered with this bounded context. */
    private final VisibilityGuard guard = VisibilityGuard.newInstance();

    /** Memoized version of the {@code StorageFactory} supplier passed to the constructor. */
    private final Supplier<StorageFactory> storageFactory;

    private final @Nullable TenantIndex tenantIndex;

    BoundedContext(Builder builder) {
        super();
        this.name = builder.name;
        this.multitenant = builder.multitenant;
        this.storageFactory = Suppliers.memoize(builder.storageFactorySupplier);
        this.commandBus = builder.commandBus.build();
        this.eventBus = builder.eventBus.build();
        this.stand = builder.stand.build();
        this.tenantIndex = builder.tenantIndex;

        /*
         * Additionally initialize the {@code IntegrationBus} with a ready-to-go instances
         * of {@code EventBus} and {@code RejectionBus}.
         */
        this.integrationBus = builder.integrationBus.setEventBus(this.eventBus)
                                                    .setRejectionBus(this.commandBus.rejectionBus())
                                                    .setBoundedContextName(this.name)
                                                    .build();
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
     *     <li>Closes associated {@link StorageFactory}.
     *     <li>Closes {@link CommandBus}.
     *     <li>Closes {@link EventBus}.
     *     <li>Closes {@link IntegrationBus}.
     *     <li>Closes {@link CommandStore}.
     *     <li>Closes {@link io.spine.server.event.EventStore EventStore}.
     *     <li>Closes {@link Stand}.
     *     <li>Shuts down all registered repositories. Each registered repository is:
     *      <ul>
     *          <li>un-registered from {@link CommandBus}
     *          <li>un-registered from {@link EventBus}
     *          <li>detached from its storage
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
        integrationBus.close();
        stand.close();

        shutDownRepositories();

        log().info(closed(nameForLogging()));
    }

    /**
     * Returns the passed name with added suffix {@code " closed."}.
     */
    private static String closed(String name) {
        return name + " closed.";
    }

    /**
     * Closes all repositories and clears {@link TenantIndex}.
     */
    private void shutDownRepositories() {
        guard.shutDownRepositories();

        if (tenantIndex != null) {
            tenantIndex.close();
        }
    }

    private String nameForLogging() {
        return BoundedContext.class.getSimpleName() + ' ' + getName().getValue();
    }

    /**
     * Obtains an ID of the bounded context.
     *
     * <p>The ID allows to identify a bounded context if a multi-context application.
     * If the ID was not defined, during the building process, the context would get
     * {@link BoundedContextNames#defaultName()} name.
     *
     * @return the ID of this {@code BoundedContext}
     */
    public BoundedContextName getName() {
        return name;
    }

    /**
     * Obtains {@link StorageFactory} associated with this {@code BoundedContext}.
     */
    public StorageFactory getStorageFactory() {
        return storageFactory.get();
    }

    /**
     * @return {@code true} if the bounded context serves many organizations
     */
    public boolean isMultitenant() {
        return multitenant;
    }

    /**
     * Obtains a tenant index of this Bounded Context.
     *
     * <p>If the Bounded Context is single-tenant returns
     * {@linkplain io.spine.server.tenant.TenantIndex.Factory#singleTenant() null-object}
     * implementation.
     */
    @Internal
    public TenantIndex getTenantIndex() {
        if (!isMultitenant()) {
            return TenantIndex.Factory.singleTenant();
        }
        return tenantIndex;
    }

    /**
     * Registers the passed repository with the {@code BoundedContext}.
     *
     * <p>If the repository does not have a storage assigned, it will be initialized
     * using the {@code StorageFactory} associated with this bounded context.
     *
     * <p>Checks whether there is a default state for entity type.
     *
     * @param repository the repository to register
     * @param <I>        the type of IDs used in the repository
     * @param <E>        the type of entities or aggregates
     * @see Repository#initStorage(StorageFactory)
     */
    public <I, E extends Entity<I, ?>> void register(Repository<I, E> repository) {
        checkNotNull(repository);
        final Message defaultState = Model.getInstance()
                                          .getDefaultState(repository.getEntityClass());
        checkNotNull(defaultState);

        repository.setBoundedContext(this);
        guard.register(repository);
        repository.onRegistered();
    }

    /**
     * Sends an integration event to this {@code BoundedContext}.
     */
    @Experimental
    @Override
    public void notify(IntegrationEvent integrationEvent, StreamObserver<Ack> observer) {
        final Event event = EventFactory.toEvent(integrationEvent);
        eventBus.post(event, observer);
    }

    /** Obtains instance of {@link CommandBus} of this {@code BoundedContext}. */
    public CommandBus getCommandBus() {
        return this.commandBus;
    }

    /** Obtains instance of {@link EventBus} of this {@code BoundedContext}. */
    public EventBus getEventBus() {
        return this.eventBus;
    }

    /** Obtains instance of {@link RejectionBus} of this {@code BoundedContext}. */
    public RejectionBus getRejectionBus() {
        return this.commandBus.rejectionBus();
    }

    /** Obtains instance of {@link IntegrationBus} of this {@code BoundedContext}. */
    public IntegrationBus getIntegrationBus() {
        return this.integrationBus;
    }

    /** Obtains instance of {@link SystemGateway} of this {@code BoundedContext}. */
    @Internal
    public abstract SystemGateway getSystemGateway();

    /** Obtains instance of {@link Stand} of this {@code BoundedContext}. */
    public Stand getStand() {
        return stand;
    }

    /**
     * Obtains a set of entity type names by their visibility.
     */
    public Set<TypeName> getEntityTypes(Visibility visibility) {
        final Set<TypeName> result = guard.getEntityTypes(visibility);
        return result;
    }

    /**
     * Finds a repository by the state class of entities.
     */
    @Internal
    public Optional<Repository> findRepository(Class<? extends Message> entityStateClass) {
        // See if there is a repository for this state at all.
        if (!guard.hasRepository(entityStateClass)) {
            throw newIllegalStateException("No repository found for the the entity state class %s",
                                           entityStateClass.getName());
        }
        final Optional<Repository> repository = guard.getRepository(entityStateClass);
        return repository;
    }

    /**
     * A builder for producing {@code BoundedContext} instances.
     *
     * <p>An application can have more than one bounded context. To distinguish
     * them use {@link #setName(String)}. If no ID is given the default ID will be assigned.
     */
    @SuppressWarnings("ClassWithTooManyMethods") // OK for this central piece.
    public static class Builder {

        private BoundedContextName name = BoundedContextNames.defaultName();
        private boolean multitenant;
        private TenantIndex tenantIndex;
        private Supplier<StorageFactory> storageFactorySupplier;
        private TransportFactory transportFactory;

        private CommandBus.Builder commandBus;
        private EventBus.Builder eventBus;
        private Stand.Builder stand;
        private IntegrationBus.Builder integrationBus;

        /**
         * Sets the value of the name for a new bounded context.
         *
         * <p>If the name is not defined in the builder, the context will get
         * {@link BoundedContextNames#defaultName()} name.
         *
         * <p>It is the responsibility of an application developer to provide meaningful and unique
         * names for bounded contexts. The framework does not check for duplication of names.
         *
         * @param name an identifier string for a new bounded context.
         *             Cannot be null, empty, or blank
         */
        public Builder setName(String name) {
            return setName(newName(name));
        }

        /**
         * Sets the name for a new bounded context.
         *
         * <p>If the name is not defined in the builder, the context will get
         * {@link BoundedContextNames#defaultName()} name.
         *
         * <p>It is the responsibility of an application developer to provide meaningful and unique
         * names for bounded contexts. The framework does not check for duplication of names.
         *
         * @param name an identifier string for a new bounded context.
         *             Cannot be null, empty, or blank
         */
        public Builder setName(BoundedContextName name) {
            BoundedContextNames.checkValid(name);
            this.name = name;
            return this;
        }

        /**
         * Returns the previously set name or {@link BoundedContextNames#defaultName()}
         * if the name was not explicitly set.
         */
        public BoundedContextName getName() {
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

        public Optional<Supplier<StorageFactory>> getStorageFactorySupplier() {
            return Optional.fromNullable(storageFactorySupplier);
        }

        public Builder setCommandBus(CommandBus.Builder commandBus) {
            this.commandBus = checkNotNull(commandBus);
            return this;
        }

        public Optional<CommandBus.Builder> getCommandBus() {
            return Optional.fromNullable(commandBus);
        }

        public Optional<? extends TenantIndex> getTenantIndex() {
            return Optional.fromNullable(tenantIndex);
        }

        public Builder setEventBus(EventBus.Builder eventBus) {
            this.eventBus = checkNotNull(eventBus);
            return this;
        }

        public Optional<EventBus.Builder> getEventBus() {
            return Optional.fromNullable(eventBus);
        }

        public Builder setStand(Stand.Builder stand) {
            this.stand = checkNotNull(stand);
            return this;
        }

        public Optional<Stand.Builder> getStand() {
            return Optional.fromNullable(stand);
        }

        public Builder setIntegrationBus(IntegrationBus.Builder integrationBus) {
            this.integrationBus = checkNotNull(integrationBus);
            return this;
        }

        public Optional<IntegrationBus.Builder> getIntegrationBus() {
            return Optional.fromNullable(integrationBus);
        }

        public Builder setTenantIndex(TenantIndex tenantIndex) {
            if (this.multitenant) {
                checkNotNull(tenantIndex,
                             "TenantRepository cannot be null in multi-tenant BoundedContext.");
            }
            this.tenantIndex = tenantIndex;
            return this;
        }

        public Builder setTransportFactory(TransportFactory transportFactory) {
            this.transportFactory = checkNotNull(transportFactory);
            return this;
        }

        public BoundedContext build() {
            SystemBoundedContext system = buildSystem();
            BoundedContext result = buildDefault(system);
            log().info(result.nameForLogging() + " created.");
            return result;
        }

        private BoundedContext buildDefault(SystemBoundedContext system) {
            BoundedContext result =
                    buildPartial(builder -> DomainBoundedContext.newInstance(builder, system));
            return result;
        }

        @SuppressWarnings("ResultOfMethodCallIgnored") // Builder methods.
        private SystemBoundedContext buildSystem() {
            BoundedContextName name = BoundedContextNames.system(this.name);
            Builder system = newBuilder()
                    .setMultitenant(multitenant)
                    .setName(name)
                    .setTransportFactory(getTransportFactory());
            Optional<? extends Supplier<StorageFactory>> storage = getStorageFactorySupplier();
            if (storage.isPresent()) {
                system.setStorageFactorySupplier(storage.get());
            }
            Optional<? extends TenantIndex> tenantIndex = getTenantIndex();
            if (tenantIndex.isPresent()) {
                system.setTenantIndex(tenantIndex.get());
            }
            SystemBoundedContext result = system.buildPartial(SystemBoundedContext::newInstance);
            return result;
        }

        private <B extends BoundedContext> B
        buildPartial(Function<Builder, B> instanceFactory) {
            StorageFactory storageFactory = getStorageFactory();

            TransportFactory transportFactory = getTransportFactory();

            initTenantIndex(storageFactory);
            initCommandBus(storageFactory);
            initEventBus(storageFactory);
            initStand(storageFactory);
            initIntegrationBus(transportFactory);

            B result = instanceFactory.apply(this);
            return result;
        }

        private StorageFactory getStorageFactory() {
            if (storageFactorySupplier == null) {
                storageFactorySupplier =
                        StorageFactorySwitch.newInstance(name, multitenant);
            }

            final StorageFactory storageFactory = storageFactorySupplier.get();

            if (storageFactory == null) {
                throw newIllegalStateException(
                        "Supplier of StorageFactory (%s) returned null instance",
                        storageFactorySupplier
                );
            }
            return storageFactory;
        }

        private TransportFactory getTransportFactory() {
            if (transportFactory == null) {
                transportFactory = InMemoryTransportFactory.newInstance();
            }
            return transportFactory;
        }

        private void initTenantIndex(StorageFactory factory) {
            if (tenantIndex == null) {
                tenantIndex = multitenant
                        ? TenantIndex.Factory.createDefault(factory)
                        : TenantIndex.Factory.singleTenant();
            }
        }

        private void initCommandBus(StorageFactory factory) {
            if (commandBus == null) {
                final CommandStore commandStore = createCommandStore(factory, tenantIndex);
                commandBus = CommandBus.newBuilder()
                                       .setMultitenant(this.multitenant)
                                       .setCommandStore(commandStore);
            } else {
                final Boolean commandBusMultitenancy = commandBus.isMultitenant();
                if (commandBusMultitenancy != null) {
                    checkSameValue("CommandBus must match multitenancy of BoundedContext. " +
                                            "Status in BoundedContext.Builder: %s CommandBus: %s",
                                   commandBusMultitenancy);
                } else {
                    commandBus.setMultitenant(this.multitenant);
                }

                if (commandBus.getCommandStore() == null) {
                    final CommandStore commandStore = createCommandStore(factory, tenantIndex);
                    commandBus.setCommandStore(commandStore);
                }
            }
        }

        private void initEventBus(StorageFactory storageFactory) {
            if (eventBus == null) {
                eventBus = EventBus.newBuilder()
                                   .setStorageFactory(storageFactory);
            } else {
                final boolean eventStoreConfigured = eventBus.getEventStore()
                                                             .isPresent();
                if (!eventStoreConfigured) {
                    eventBus.setStorageFactory(storageFactory);
                }
            }
        }

        private void initStand(StorageFactory factory) {
            if (stand == null) {
                stand = createStand(factory);
            } else {
                final Boolean standMultitenant = stand.isMultitenant();
                // Check that both either multi-tenant or single-tenant.
                if (standMultitenant == null) {
                    stand.setMultitenant(multitenant);
                } else {
                    checkSameValue("Stand must match multitenancy of BoundedContext. " +
                                            "Status in BoundedContext.Builder: %s Stand: %s",
                                   standMultitenant);
                }
            }
        }

        private void initIntegrationBus(TransportFactory transportFactory) {
            if (integrationBus == null) {
                integrationBus = IntegrationBus.newBuilder()
                                               .setTransportFactory(transportFactory);
            }
            if (!integrationBus.getTransportFactory()
                               .isPresent()) {
                integrationBus.setTransportFactory(transportFactory);
            }
        }

        /**
         * Ensures that the value of the passed flag is equal to the value of
         * the {@link Builder#multitenant}.
         *
         * @throws IllegalStateException if the flags values do not match
         */
        private void checkSameValue(String errMsgFmt, boolean partMultitenancy) {
            checkState(this.multitenant == partMultitenancy,
                       errMsgFmt,
                       String.valueOf(this.multitenant),
                       String.valueOf(partMultitenancy));
        }

        private static CommandStore createCommandStore(StorageFactory factory, TenantIndex index) {
            final CommandStore result = new CommandStore(factory, index);
            return result;
        }

        private Stand.Builder createStand(StorageFactory factory) {
            final StandStorage standStorage = factory.createStandStorage();
            final Stand.Builder result = Stand.newBuilder()
                                              .setMultitenant(multitenant)
                                              .setStorage(standStorage);
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
