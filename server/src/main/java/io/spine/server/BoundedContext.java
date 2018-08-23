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

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Experimental;
import io.spine.annotation.Internal;
import io.spine.core.Ack;
import io.spine.core.BoundedContextName;
import io.spine.core.BoundedContextNames;
import io.spine.core.Event;
import io.spine.logging.Logging;
import io.spine.option.EntityOption.Visibility;
import io.spine.server.aggregate.ImportBus;
import io.spine.server.command.CommandErrorHandler;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.commandbus.CommandDispatcherDelegate;
import io.spine.server.commandbus.DelegatingCommandDispatcher;
import io.spine.server.entity.Entity;
import io.spine.server.entity.Repository;
import io.spine.server.entity.VisibilityGuard;
import io.spine.server.event.DelegatingEventDispatcher;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;
import io.spine.server.event.EventDispatcherDelegate;
import io.spine.server.event.EventFactory;
import io.spine.server.integration.ExternalDispatcherFactory;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.IntegrationBus;
import io.spine.server.integration.IntegrationEvent;
import io.spine.server.integration.grpc.IntegrationEventSubscriberGrpc;
import io.spine.server.stand.Stand;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.TenantIndex;
import io.spine.system.server.SystemBoundedContext;
import io.spine.system.server.SystemGateway;
import io.spine.type.TypeName;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Suppliers.memoize;
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
 *     Martin Fowler on BoundedContext</a>
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public abstract class BoundedContext
        extends IntegrationEventSubscriberGrpc.IntegrationEventSubscriberImplBase
        implements AutoCloseable, Logging {

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
    private final ImportBus importBus;
    private final Stand stand;

    /** Controls access to entities of all registered repositories. */
    private final VisibilityGuard guard = VisibilityGuard.newInstance();

    /** Memoized version of the {@code StorageFactory} supplier passed to the constructor. */
    private final Supplier<StorageFactory> storageFactory;

    private final TenantIndex tenantIndex;

    /**
     * Creates new instance.
     *
     * @throws IllegalStateException
     *         if called from a derived class, which is not a part of the framework
     * @apiNote This constructor is for internal use of the framework.
     *          Application developers should not create classes derived from {@code BoundedContext}
     */
    @Internal
    protected BoundedContext(BoundedContextBuilder builder) {
        super();
        checkInheritance();

        this.name = builder.getName();
        this.multitenant = builder.isMultitenant();
        this.storageFactory = memoize(() -> builder.buildStorageFactorySupplier()
                                                   .get());
        this.eventBus = builder.buildEventBus();
        this.stand = builder.buildStand();
        this.tenantIndex = builder.buildTenantIndex();

        this.commandBus = buildCommandBus(builder, eventBus);
        this.integrationBus = buildIntegrationBus(builder, eventBus, name);
        this.importBus = buildImportBus(tenantIndex);
    }

    /**
     * Prevents 3rd party code from creating classes extending from {@code BoundedContext}.
     */
    @SuppressWarnings("ClassReferencesSubclass")
    private void checkInheritance() {
        Class<? extends BoundedContext> thisClass = getClass();
        checkState(
                DomainBoundedContext.class.equals(thisClass) ||
                        SystemBoundedContext.class.equals(thisClass),
                "The class `BoundedContext` is not designed for " +
                        "inheritance by the framework users."
        );
    }

    private static CommandBus buildCommandBus(BoundedContextBuilder builder, EventBus eventBus) {
        Optional<CommandBus.Builder> busBuilder = builder.getCommandBus();
        checkState(busBuilder.isPresent());
        CommandBus result = busBuilder.get()
                                      .injectEventBus(eventBus)
                                      .build();
        return result;
    }

    /**
     * Creates a new instance of {@link IntegrationBus} with the given parameters.
     *
     * @param builder    the {@link BoundedContextBuilder} to obtain
     *                   the {@link IntegrationBus.Builder} from
     * @param eventBus   the initialized {@link EventBus}
     * @param name       the name of the constructed bounded context
     * @return new instance of {@link IntegrationBus}
     */
    private static IntegrationBus buildIntegrationBus(BoundedContextBuilder builder,
                                                      EventBus eventBus,
                                                      BoundedContextName name) {
        Optional<IntegrationBus.Builder> busBuilder = builder.getIntegrationBus();
        checkArgument(busBuilder.isPresent());
        IntegrationBus result =
                busBuilder.get()
                          .setBoundedContextName(name)
                          .setEventBus(eventBus)
                          .build();
        return result;
    }

    private static ImportBus buildImportBus(TenantIndex tenantIndex) {
        ImportBus.Builder result = ImportBus
                .newBuilder()
                .injectTenantIndex(tenantIndex);
        return result.build();
    }

    /**
     * Creates a new builder for {@code BoundedContext}.
     *
     * @return new builder instance
     */
    public static BoundedContextBuilder newBuilder() {
        return new BoundedContextBuilder();
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
        repository.setBoundedContext(this);
        guard.register(repository);
        repository.onRegistered();
    }

    /**
     * Registers the passed command dispatcher with the {@code CommandBus} of
     * this {@code BoundedContext}.
     */
    public void registerCommandDispatcher(CommandDispatcher<?> dispatcher) {
        checkNotNull(dispatcher);
        if (dispatcher.dispatchesCommands()) {
            getCommandBus().register(dispatcher);
        }
    }

    /**
     * Registers the passed command dispatcher with the {@code CommandBus} of
     * this {@code BoundedContext}.
     */
    public void registerCommandDispatcher(CommandDispatcherDelegate<?> dispatcher) {
        checkNotNull(dispatcher);
        if (dispatcher.dispatchesCommands()) {
            registerCommandDispatcher(DelegatingCommandDispatcher.of(dispatcher));
        }
    }

    private void registerWithIntegrationBus(ExternalDispatcherFactory<?> dispatcher) {
        ExternalMessageDispatcher<?> externalDispatcher =
                dispatcher.createExternalDispatcher()
                          .orElseThrow(notExternalDispatcherFrom(dispatcher));

        getIntegrationBus().register(externalDispatcher);
    }

    /**
     * Registers the passed event dispatcher with the {@code EventBus} of
     * this {@code BoundedContext}, if it dispatches domestic events.
     * If the passed instance dispatches external events, registers it with
     * the {@code IntegrationBus}.
     */
    public void registerEventDispatcher(EventDispatcher<?> dispatcher) {
        checkNotNull(dispatcher);
        if (dispatcher.dispatchesEvents()) {
            getEventBus().register(dispatcher);
        }

        if (dispatcher.dispatchesExternalEvents()) {
            registerWithIntegrationBus(dispatcher);
        }
    }

    /**
     * Registers the passed event dispatcher with the {@code EventBus} of
     * this {@code BoundedContext}, if it dispatchers domestic events.
     * If the passed instance dispatches external events, registers it with
     * the {@code IntegrationBus}.
     */
    public void registerEventDispatcher(EventDispatcherDelegate<?> dispatcher) {
        checkNotNull(dispatcher);
        DelegatingEventDispatcher<?> delegatingDispatcher =
                DelegatingEventDispatcher.of(dispatcher);

        if (dispatcher.dispatchesEvents()) {
            getEventBus().register(delegatingDispatcher);
        }

        if (dispatcher.dispatchesExternalEvents()) {
            registerWithIntegrationBus(delegatingDispatcher);
        }
    }

    /**
     * Registers the passed dispatcher of import operations with the {@link ImportBus}.
     */
    public void registerImportDispatcher(EventDispatcher<?> dispatcher) {
        checkNotNull(dispatcher);
        if (dispatcher.dispatchesEvents()) {
            importBus.register(dispatcher);
        }
    }

    /**
     * Supplies {@code IllegalStateException} for the cases when dispatchers or dispatcher
     * delegates do not provide an external message dispatcher.
     */
    private static
    Supplier<IllegalStateException> notExternalDispatcherFrom(Object dispatcher) {
        return () -> newIllegalStateException("No external dispatcher provided by %s", dispatcher);
    }

    /**
     * Creates a {@code CommandErrorHandler} for objects that handle commands.
     */
    public CommandErrorHandler createCommandErrorHandler() {
        SystemGateway systemGateway = getSystemGateway();
        CommandErrorHandler result = CommandErrorHandler.with(systemGateway);
        return result;
    }
    /**
     * Sends an integration event to this {@code BoundedContext}.
     */
    @Experimental
    @Override
    public void notify(IntegrationEvent integrationEvent, StreamObserver<Ack> observer) {
        Event event = EventFactory.toEvent(integrationEvent);
        eventBus.post(event, observer);
    }

    /**
     * Obtains a set of entity type names by their visibility.
     */
    public Set<TypeName> getEntityTypes(Visibility visibility) {
        Set<TypeName> result = guard.getEntityTypes(visibility);
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
        Optional<Repository> repository = guard.getRepository(entityStateClass);
        return repository;
    }

    /** Obtains instance of {@link CommandBus} of this {@code BoundedContext}. */
    public CommandBus getCommandBus() {
        return this.commandBus;
    }

    /** Obtains instance of {@link EventBus} of this {@code BoundedContext}. */
    public EventBus getEventBus() {
        return this.eventBus;
    }

    /** Obtains instance of {@link IntegrationBus} of this {@code BoundedContext}. */
    public IntegrationBus getIntegrationBus() {
        return this.integrationBus;
    }

    /** Obtains instance of {@link Stand} of this {@code BoundedContext}. */
    public Stand getStand() {
        return stand;
    }

    /**
     * Obtains an ID of the bounded context.
     *
     * <p>The ID allows to identify a bounded context if a multi-context application.
     * If the ID was not defined, during the building process, the context would get
     * {@link BoundedContextNames#assumingTests()} name.
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
     * {@linkplain TenantIndex#singleTenant() null-object}
     * implementation.
     */
    @Internal
    public TenantIndex getTenantIndex() {
        return tenantIndex;
    }

    /** Obtains instance of {@link SystemGateway} of this {@code BoundedContext}. */
    @Internal
    public abstract SystemGateway getSystemGateway();

    /**
     * Closes the {@code BoundedContext} performing all necessary clean-ups.
     *
     * <p>This method performs the following:
     * <ol>
     *     <li>Closes associated {@link StorageFactory}.
     *     <li>Closes {@link CommandBus}.
     *     <li>Closes {@link EventBus}.
     *     <li>Closes {@link IntegrationBus}.
     *     <li>Closes {@link io.spine.server.event.EventStore EventStore}.
     *     <li>Closes {@link Stand}.
     *     <li>Closes {@link ImportBus}.
     *     <li>{@linkplain io.spine.server.entity.Repository#close()} Closes} all registered
     *     repositories.
     * </ol>
     *
     * @throws Exception caused by closing one of the components
     */
    @Override
    public void close() throws Exception {
        storageFactory.get()
                      .close();
        commandBus.close();
        eventBus.close();
        integrationBus.close();
        stand.close();
        importBus.close();

        shutDownRepositories();

        log().info(closed(nameForLogging()));
    }

    String nameForLogging() {
        return BoundedContext.class.getSimpleName() + ' ' + getName().getValue();
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
}
