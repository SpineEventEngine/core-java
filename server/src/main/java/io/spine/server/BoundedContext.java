/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.BoundedContextName;
import io.spine.core.BoundedContextNames;
import io.spine.logging.Logging;
import io.spine.option.EntityOption.Visibility;
import io.spine.server.aggregate.AggregateRootDirectory;
import io.spine.server.aggregate.ImportBus;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.AbstractCommander;
import io.spine.server.command.CommandErrorHandler;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.commandbus.CommandDispatcherDelegate;
import io.spine.server.commandbus.DelegatingCommandDispatcher;
import io.spine.server.entity.Entity;
import io.spine.server.entity.Repository;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.event.AbstractEventReactor;
import io.spine.server.event.DelegatingEventDispatcher;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;
import io.spine.server.event.EventDispatcherDelegate;
import io.spine.server.event.store.DefaultEventStore;
import io.spine.server.integration.ExternalDispatcherFactory;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.IntegrationBus;
import io.spine.server.stand.Stand;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.TenantIndex;
import io.spine.server.trace.TracerFactory;
import io.spine.system.server.SystemClient;
import io.spine.system.server.SystemContext;
import io.spine.system.server.SystemReadSide;
import io.spine.system.server.SystemWriteSide;
import io.spine.type.TypeName;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A logical and structural boundary of a model.
 *
 * <p>Logically, a Bounded Context represents a sub-system built to be described with the same
 * Ubiquitous Language. Any term within a single bounded context has a single meaning and may or
 * may not map to another term in the language of another Bounded Context.
 *
 * <p>The Ubiquitous Language of a Bounded Context is represented by such concepts as the entity
 * state, event, and command types, entity types, and others. An entity and its adjacent types
 * belong to the Bounded Context which the entity {@link Repository} is
 * {@linkplain BoundedContext#register(Repository) registered} in.
 *
 * <p>Structurally, a Bounded Context brings together all the infrastructure required for
 * the components of a model to cooperate.
 *
 * <p>An instance of {@code BoundedContext} acts as a major point of configuration for all
 * the model elements which belong to it.
 *
 * @see <a href="https://martinfowler.com/bliki/BoundedContext.html">
 *         Martin Fowler on Bounded Contexts</a>
 */
@SuppressWarnings({"OverlyCoupledClass", "ClassWithTooManyMethods"})
public abstract class BoundedContext implements AutoCloseable, Logging {

    private final ContextSpec spec;
    private final CommandBus commandBus;
    private final EventBus eventBus;
    private final IntegrationBus integrationBus;
    private final ImportBus importBus;
    private final Stand stand;

    /** Controls access to entities of all registered repositories. */
    private final VisibilityGuard guard = VisibilityGuard.newInstance();
    private final AggregateRootDirectory aggregateRootDirectory;

    private final TenantIndex tenantIndex;

    /**
     * Creates new instance.
     *
     * @throws IllegalStateException
     *         if called from a derived class, which is not a part of the framework
     * @apiNote
     * This constructor is for internal use of the framework. Application developers should not
     * create classes derived from {@code BoundedContext}.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction") // to inject dependencies.
    @Internal
    protected BoundedContext(BoundedContextBuilder builder) {
        super();
        checkInheritance();
        this.spec = builder.spec();
        this.eventBus = builder.buildEventBus(this);
        this.stand = builder.buildStand();
        this.tenantIndex = builder.buildTenantIndex();
        this.commandBus = builder.buildCommandBus(eventBus);
        this.integrationBus = builder.buildIntegrationBus(this);
        this.importBus = buildImportBus(tenantIndex);
        this.aggregateRootDirectory = builder.aggregateRootDirectory();
    }

    /**
     * Performs post-creation initialization of the instance.
     *
     * <p>This method must be called shortly after the constructor so that the instance can
     * perform dependency injections steps that cannot be performed in the constructor.
     */
    protected final void init() {
        eventBus.init(this);
        tenantIndex.registerWith(this);
    }
    
    /**
     * Prevents 3rd party code from creating classes extending from {@code BoundedContext}.
     */
    @SuppressWarnings("ClassReferencesSubclass")
    private void checkInheritance() {
        Class<? extends BoundedContext> thisClass = getClass();
        checkState(
                DomainContext.class.equals(thisClass) ||
                        SystemContext.class.equals(thisClass),
                "The class `BoundedContext` is not designed for " +
                        "inheritance by the framework users."
        );
    }

    private static ImportBus buildImportBus(TenantIndex tenantIndex) {
        ImportBus.Builder result = ImportBus
                .newBuilder()
                .injectTenantIndex(tenantIndex);
        return result.build();
    }

    /**
     * Creates a new builder for a single tenant {@code BoundedContext}.
     *
     * @param name
     *         the name of the built context
     * @return new builder instance
     */
    public static BoundedContextBuilder singleTenant(String name) {
        checkNotNull(name);
        ContextSpec spec = ContextSpec.singleTenant(name);
        return new BoundedContextBuilder(spec);
    }

    /**
     * Creates a new builder for a multitenant {@code BoundedContext}.
     *
     * @param name
     *         the name of the built context
     * @return new builder instance
     */
    public static BoundedContextBuilder multitenant(String name) {
        checkNotNull(name);
        ContextSpec spec = ContextSpec.multitenant(name);
        return new BoundedContextBuilder(spec);
    }

    /**
     * Registers the passed repository with this {@code BoundedContext}.
     *
     * <p>If the repository does not have a storage assigned, it will be initialized
     * using the {@code StorageFactory} associated with this {@code BoundedContext}.
     *
     * @param repository
     *         the repository to register
     * @param <I>
     *         the type of IDs used in the repository
     * @param <E>
     *         the type of entities
     */
    @Internal
    public <I, E extends Entity<I, ?>> void register(Repository<I, E> repository) {
        checkNotNull(repository);
        repository.injectContext(this);
        guard.register(repository);
        repository.onRegistered();
        registerEventDispatcher(stand());
    }

    /**
     * Creates and registers the {@linkplain DefaultRepository default repository} for the passed
     * class of entities.
     *
     * @param <I>
     *         the type of entity identifiers
     * @param <E>
     *         the type of entities
     * @param entityClass
     *         the class of entities which will be served by a {@link DefaultRepository}
     * @see #register(Repository)
     */
    <I, E extends Entity<I, ?>> void register(Class<E> entityClass) {
        checkNotNull(entityClass);
        register(DefaultRepository.of(entityClass));
    }

    /**
     * Registers the passed command dispatcher with the {@code CommandBus} of
     * this {@code BoundedContext}.
     */
    public void registerCommandDispatcher(CommandDispatcher<?> dispatcher) {
        checkNotNull(dispatcher);
        if (dispatcher.dispatchesCommands()) {
            commandBus().register(dispatcher);
            if (dispatcher instanceof AbstractCommandHandler) {
                ((AbstractCommandHandler) dispatcher).injectEventBus(eventBus());
            }
        }
        if (dispatcher instanceof AbstractCommander) {
            AbstractCommander commander = (AbstractCommander) dispatcher;
            commander.injectCommandBus(commandBus());
            DelegatingEventDispatcher<?> proxy = DelegatingEventDispatcher.of(commander);
            registerEventDispatcher(proxy);
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
                          .orElseThrow(missingExternalDispatcherFrom(dispatcher));

        integrationBus().register(externalDispatcher);
    }

    /**
     * Registers the passed event dispatcher with the buses of this {@code BoundedContext}.
     *
     * <p>If the passed instance dispatches domestic events, registers it with the {@code EventBus}.
     * If the passed instance dispatches external events, registers it with
     * the {@code IntegrationBus}.
     *
     * @see #registerEventDispatcher(EventDispatcherDelegate)
     */
    public void registerEventDispatcher(EventDispatcher<?> dispatcher) {
        checkNotNull(dispatcher);
        if (dispatcher.dispatchesEvents()) {
            EventBus eventBus = eventBus();
            eventBus.register(dispatcher);
            SystemReadSide systemReadSide = systemClient().readSide();
            systemReadSide.register(dispatcher);
            if (dispatcher instanceof AbstractEventReactor) {
                ((AbstractEventReactor) dispatcher).injectEventBus(eventBus);
            }
        }
        if (dispatcher.dispatchesExternalEvents()) {
            registerWithIntegrationBus(dispatcher);
        }
    }

    /**
     * Registers the passed delegate of an {@link EventDispatcher} with the buses of this
     * {@code BoundedContext}.
     *
     * @see #registerEventDispatcher(EventDispatcher)
     */
    public void registerEventDispatcher(EventDispatcherDelegate<?> dispatcher) {
        checkNotNull(dispatcher);
        DelegatingEventDispatcher<?> delegate = DelegatingEventDispatcher.of(dispatcher);
        registerEventDispatcher(delegate);
    }

    /**
     * Supplies {@code IllegalStateException} for the cases when dispatchers or dispatcher
     * delegates do not provide an external message dispatcher.
     */
    private static
    Supplier<IllegalStateException> missingExternalDispatcherFrom(Object dispatcher) {
        return () -> newIllegalStateException(
                "No external dispatcher provided by `%s`.", dispatcher);
    }

    /**
     * Creates a {@code CommandErrorHandler} for objects that handle commands.
     */
    public CommandErrorHandler createCommandErrorHandler() {
        SystemWriteSide systemWriteSide = systemClient().writeSide();
        CommandErrorHandler result = CommandErrorHandler.with(systemWriteSide, this::eventBus);
        return result;
    }

    /**
     * Obtains a set of entity type names by their visibility.
     */
    public Set<TypeName> stateTypes(Visibility visibility) {
        Set<TypeName> result = guard.entityStateTypes(visibility);
        return result;
    }

    /**
     * Obtains the set of all entity type names.
     */
    public Set<TypeName> stateTypes() {
        Set<TypeName> result = guard.allEntityTypes();
        return result;
    }

    /**
     * Finds a repository by the state class of entities.
     *
     * <p>This method assumes that a repository for the given entity state class <b>is</b>
     * registered in this context. If there is no such repository, throws
     * an {@link IllegalStateException}.
     *
     * <p>If a repository is registered, the method returns it or {@link Optional#empty()} if
     * the requested entity is {@linkplain Visibility#NONE not visible}.
     *
     * @param entityStateClass
     *         the class of the state of the entity managed by the resulting repository
     * @return the requested repository or {@link Optional#empty()} if the repository manages
     *         a {@linkplain Visibility#NONE non-visible} entity
     * @throws IllegalStateException
     *         if the requested repository is not registered
     * @see VisibilityGuard
     */
    @Internal
    public Optional<Repository> findRepository(Class<? extends Message> entityStateClass) {
        // See if there is a repository for this state at all.
        if (!guard.hasRepository(entityStateClass)) {
            throw newIllegalStateException("No repository found for the entity state class `%s`.",
                                           entityStateClass.getName());
        }
        Optional<Repository> repository = guard.repositoryFor(entityStateClass);
        return repository;
    }

    /**
     * Verifies if this Bounded Context contains entities of the passed class.
     *
     * <p>This method does not take into account visibility of entity states.
     *
     * @see #findRepository(Class)
     */
    @VisibleForTesting
    public boolean hasEntitiesOfType(Class<? extends Entity<?, ?>> entityClass) {
        EntityClass<? extends Entity<?, ?>> cls = EntityClass.asEntityClass(entityClass);
        boolean result = guard.hasRepository(cls.stateClass());
        return result;
    }

    /**
     * Verifies if this Bounded Context has entities with the state of the passed class.
     *
     * <p>This method does not take into account visibility of entity states.
     *
     * @see #findRepository(Class)
     */
    @VisibleForTesting
    public boolean hasEntitiesWithState(Class<? extends Message> stateClass) {
        boolean result = guard.hasRepository(stateClass);
        return result;
    }

    /** Obtains instance of {@link CommandBus} of this {@code BoundedContext}. */
    public CommandBus commandBus() {
        return this.commandBus;
    }

    /** Obtains instance of {@link EventBus} of this {@code BoundedContext}. */
    public EventBus eventBus() {
        return eventBus;
    }

    /** Obtains instance of {@link IntegrationBus} of this {@code BoundedContext}. */
    public IntegrationBus integrationBus() {
        return this.integrationBus;
    }

    /** Obtains instance of {@link ImportBus} of this {@code BoundedContext}. */
    public ImportBus importBus() {
        return this.importBus;
    }

    /** Obtains instance of {@link Stand} of this {@code BoundedContext}. */
    public Stand stand() {
        return stand;
    }

    /**
     * Obtains an ID of the Bounded Context.
     *
     * <p>The ID allows to identify a Bounded Context if a multi-context application.
     * If the ID was not defined during the building process, the Bounded Context gets
     * {@link BoundedContextNames#assumingTests()} name.
     *
     * @return the ID of this {@code BoundedContext}
     */
    public BoundedContextName name() {
        return spec.name();
    }

    /**
     * Obtains specification of this context.
     */
    public ContextSpec spec() {
        return spec;
    }

    /**
     * Returns {@code true} if the Bounded Context is designed to serve more than one tenant of
     * the application, {@code false} otherwise.
     */
    public boolean isMultitenant() {
        return spec.isMultitenant();
    }

    /**
     * Obtains a tenant index of this Bounded Context.
     *
     * <p>If the Bounded Context is single-tenant returns
     * {@linkplain TenantIndex#singleTenant() null-object}
     * implementation.
     */
    @Internal
    public TenantIndex tenantIndex() {
        return tenantIndex;
    }

    /**
     * Obtains instance of {@link SystemClient} of this {@code BoundedContext}.
     */
    @Internal
    public abstract SystemClient systemClient();

    @Internal
    public AggregateRootDirectory aggregateRootDirectory() {
        return aggregateRootDirectory;
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
     *     <li>Closes {@link DefaultEventStore EventStore}.
     *     <li>Closes {@link Stand}.
     *     <li>Closes {@link ImportBus}.
     *     <li>Closes {@link TracerFactory} if it is present.
     *     <li>Closes all registered {@linkplain Repository repositories}.
     * </ol>
     *
     * @throws Exception
     *         caused by closing one of the components
     */
    @Override
    public void close() throws Exception {
        commandBus.close();
        eventBus.close();
        integrationBus.close();
        stand.close();
        importBus.close();
        shutDownRepositories();

        log().debug(closed(nameForLogging()));
    }

    /**
     * Tells if the context is closed.
     *
     * <p>This is a test-only method which is needed for the tests that forcibly close a context,
     * so that cleanup methods do not call it again.
     */
    @VisibleForTesting
    public boolean isClosed() {
        return guard.isClosed();
    }

    final String nameForLogging() {
        return BoundedContext.class.getSimpleName() + ' ' + name().getValue();
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

    /**
     * Returns the name of this Bounded Context.
     */
    @Override
    public String toString() {
        return spec.name()
                   .getValue();
    }
}
