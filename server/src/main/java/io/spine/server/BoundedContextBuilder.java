/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.annotation.Internal;
import io.spine.core.BoundedContextName;
import io.spine.logging.Logging;
import io.spine.server.aggregate.AggregateRootDirectory;
import io.spine.server.aggregate.InMemoryRootDirectory;
import io.spine.server.bus.BusFilter;
import io.spine.server.bus.Listener;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.enrich.Enricher;
import io.spine.server.entity.Entity;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;
import io.spine.server.event.EventEnricher;
import io.spine.server.integration.IntegrationBroker;
import io.spine.server.stand.Stand;
import io.spine.server.tenant.TenantIndex;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.NoOpSystemClient;
import io.spine.system.server.SystemClient;
import io.spine.system.server.SystemContext;
import io.spine.system.server.SystemSettings;
import io.spine.system.server.SystemWriteSide;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.BoundedContextNames.assumingTestsValue;
import static io.spine.server.ContextSpec.multitenant;
import static io.spine.server.ContextSpec.singleTenant;

/**
 * A builder for producing {@code BoundedContext} instances.
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
// OK for this central piece.
public final class BoundedContextBuilder implements Logging {

    private final ContextSpec spec;

    private final CommandBus.Builder commandBus = CommandBus.newBuilder();

    /**
     * Command dispatchers to be registered with the context {@link CommandBus} after the Bounded
     * Context creation.
     */
    private final Collection<CommandDispatcher> commandDispatchers = new ArrayList<>();

    private final EventBus.Builder eventBus = EventBus.newBuilder();

    /**
     * Event dispatchers to be registered with the context {@link EventBus} and/or
     * {@link IntegrationBroker} after the Bounded Context creation.
     */
    private final Collection<EventDispatcher> eventDispatchers = new ArrayList<>();

    //TODO:2020-06-17:alex.tymchenko: rename the variable.
    private final SystemSettings systemFeatures;

    private Stand stand;
    private Supplier<AggregateRootDirectory> rootDirectory;
    private TenantIndex tenantIndex;

    /** Repositories to be registered with the Bounded Context being built after its creation. */
    private final Collection<Repository<?, ?>> repositories = new ArrayList<>();

    /**
     * Creates a new builder with the given spec.
     *
     * @param spec
     *         the context spec for the built context
     * @see BoundedContext#singleTenant
     * @see BoundedContext#multitenant
     */
    BoundedContextBuilder(ContextSpec spec) {
        this(spec, SystemSettings.defaults());
    }

    /**
     * Creates a new builder with the given spec and system features.
     *
     * @param spec
     *         the context spec for the built context
     * @param systemFeatures
     *         system feature flags; can be changed later via {@link #systemFeatures()}
     * @see BoundedContext#singleTenant
     * @see BoundedContext#multitenant
     */
    private BoundedContextBuilder(ContextSpec spec, SystemSettings systemFeatures) {
        this.spec = checkNotNull(spec);
        this.systemFeatures = checkNotNull(systemFeatures);
    }

    /**
     * Creates a new builder for a test-only bounded context.
     */
    @Internal
    @VisibleForTesting
    public static BoundedContextBuilder assumingTests(boolean multitenant) {
        ContextSpec spec = multitenant
                           ? multitenant(assumingTestsValue())
                           : singleTenant(assumingTestsValue());
        return new BoundedContextBuilder(spec);
    }

    /**
     * Creates a new builder for a single tenant test-only Bounded Context.
     */
    @Internal
    @VisibleForTesting
    public static BoundedContextBuilder assumingTests() {
        return assumingTests(false);
    }

    /**
     * Obtains the context spec.
     */
    public ContextSpec spec() {
        return spec;
    }

    /**
     * Returns the name of the resulting context.
     */
    public BoundedContextName name() {
        return spec.name();
    }

    public boolean isMultitenant() {
        return spec.isMultitenant();
    }

    /**
     * Obtains {@code TenantIndex} implementation associated with the Bounded Context.
     */
    public Optional<? extends TenantIndex> tenantIndex() {
        return Optional.ofNullable(tenantIndex);
    }

    /**
     * Obtains the instance of the {@code Stand} for the context to be built.
     *
     * <p>This method must be invoked only from the
     * {@link BoundedContext#BoundedContext(BoundedContextBuilder) constructor} of
     * the {@code BoundedContext}.
     */
    Stand stand() {
        return checkNotNull(stand);
    }

    /**
     * Obtains the instance of the {@code TenantIndex} for the context to be built.
     *
     * <p>This method must be invoked only from the
     * {@link BoundedContext#BoundedContext(BoundedContextBuilder) constructor} of
     * the {@code BoundedContext}.
     */
    TenantIndex buildTenantIndex() {
        TenantIndex result = isMultitenant()
                             ? checkNotNull(tenantIndex)
                             : TenantIndex.singleTenant();
        return result;
    }

    /**
     * Sets a custom {@link Enricher} for events posted to
     * the {@code EventBus} of the context being built.
     *
     * <p>If the {@code Enricher} is not set, the enrichments
     * will <strong>NOT</strong> be supported in this context.
     *
     * @param enricher
     *         the {@code Enricher} for events or {@code null} if enrichment is not supported
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder enrichEventsUsing(EventEnricher enricher) {
        eventBus.injectEnricher(enricher);
        return this;
    }

    /**
     * Obtains {@code EventEnricher} assigned to the context to be built, or
     * empty {@code Optional} if no enricher was assigned prior to this call.
     */
    public Optional<EventEnricher> eventEnricher() {
        return eventBus.enricher();
    }

    @CanIgnoreReturnValue
    public BoundedContextBuilder setTenantIndex(TenantIndex tenantIndex) {
        if (isMultitenant()) {
            checkNotNull(tenantIndex,
                         "`%s` cannot be null in a multitenant `BoundedContext`.",
                         TenantIndex.class.getSimpleName());
        }
        this.tenantIndex = tenantIndex;
        return this;
    }

    /**
     * Convenience method for handling the cases of passing a repository, which is also a message
     * dispatcher to {@code addXxxDispatcher()} and {@code removeXxxDispatcher()} methods.
     *
     * <p>Such a call can be made by a developer (presumably by mistake) because some repositories
     * <em>are</em> command- or event- dispatchers. Event though the methods
     * {@link #add(Repository)} and {@link #remove(Repository)} is the correct way of handling
     * removing repositories, we do not want to play hard in this case, and simply divert the flow
     * to process the passed {@code Repository} via correct counterpart methods.
     *
     * @param dispatcher
     *         the instance to handle
     * @param repositoryConsumer
     *         the operation to perform if the passed instance is a {@code Repository}
     * @param dispatcherConsumer
     *         the operation to perform if the passed instance is a not a {@code Repository}
     * @param <D>
     *         the type of the dispatchers
     * @return this builder
     */
    private <D extends MessageDispatcher<?, ?>>
    BoundedContextBuilder ifRepository(D dispatcher,
                                       Consumer<Repository<?, ?>> repositoryConsumer,
                                       Consumer<D> dispatcherConsumer) {
        if (dispatcher instanceof Repository) {
            repositoryConsumer.accept((Repository<?, ?>) dispatcher);
        } else {
            dispatcherConsumer.accept(dispatcher);
        }
        return this;
    }

    /**
     * Adds the passed command dispatcher to the dispatcher registration list which will be
     * processed after the Bounded Context is created.
     *
     * @apiNote This method is also capable of registering {@linkplain Repository repositories}
     *         that implement {@code CommandDispatcher}, but the {@link #add(Repository)} method
     *         should be preferred for this purpose.
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder addCommandDispatcher(CommandDispatcher commandDispatcher) {
        checkNotNull(commandDispatcher);
        return ifRepository(commandDispatcher, this::add, commandDispatchers::add);
    }

    /**
     * Adds a filter for commands.
     *
     * <p>The order of appending the filters to the builder is the order of the filters in
     * the {@code CommandBus}.
     */
    public BoundedContextBuilder addCommandFilter(BusFilter<CommandEnvelope> filter) {
        checkNotNull(filter);
        commandBus.appendFilter(filter);
        return this;
    }

    /**
     * Adds a listener for commands posted to the {@code CommandBus} of the context being built.
     */
    public BoundedContextBuilder addCommandListener(Listener<CommandEnvelope> listener) {
        checkNotNull(listener);
        commandBus.addListener(listener);
        return this;
    }

    /**
     * Adds the passed event dispatcher to the dispatcher registration list which will be processed
     * after the Bounded Context is created.
     *
     * @apiNote This method is also capable of registering {@linkplain Repository repositories}
     *         that implement {@code EventDispatcher}, but the {@link #add(Repository)} method
     *         should be preferred for this purpose.
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder addEventDispatcher(EventDispatcher eventDispatcher) {
        checkNotNull(eventDispatcher);
        return ifRepository(eventDispatcher, this::add, eventDispatchers::add);
    }

    /**
     * Adds a filter for events.
     *
     * <p>The order of appending the filters to the builder is the order of the filters in
     * the {@code EventBus}.
     *
     * @param filter
     *         the filter to add
     */
    public BoundedContextBuilder addEventFilter(BusFilter<EventEnvelope> filter) {
        checkNotNull(filter);
        eventBus.appendFilter(filter);
        return this;
    }

    /**
     * Adds a listener of the events posted to the {@code EventBus} of the context being built.
     */
    public BoundedContextBuilder addEventListener(Listener<EventEnvelope> listener) {
        checkNotNull(listener);
        eventBus.addListener(listener);
        return this;
    }

    /**
     * Adds the {@linkplain DefaultRepository default repository} for the passed entity class to
     * the repository registration list.
     */
    @CanIgnoreReturnValue
    public <I, E extends Entity<I, ?>> BoundedContextBuilder add(Class<E> entityClass) {
        checkNotNull(entityClass);
        return add(DefaultRepository.of(entityClass));
    }

    /**
     * Adds the passed repository to the registration list which will be processed after
     * the Bounded Context is created.
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder add(Repository<?, ?> repository) {
        checkNotNull(repository);
        repositories.add(repository);
        return this;
    }

    /**
     * Removes the passed command dispatcher from the corresponding registration list.
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder removeCommandDispatcher(CommandDispatcher commandDispatcher) {
        checkNotNull(commandDispatcher);
        return ifRepository(commandDispatcher, this::remove, commandDispatchers::remove);
    }

    /**
     * Removes the passed event dispatcher from the corresponding registration list.
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder removeEventDispatcher(EventDispatcher eventDispatcher) {
        checkNotNull(eventDispatcher);
        return ifRepository(eventDispatcher, this::remove, eventDispatchers::remove);
    }

    /**
     * Removes the repository from the registration list by the passed entity class.
     */
    @CanIgnoreReturnValue
    public <I, E extends Entity<I, ?>> BoundedContextBuilder remove(Class<E> entityClass) {
        checkNotNull(entityClass);
        repositories.removeIf(repository -> repository.entityClass()
                                                      .equals(entityClass));
        return this;
    }

    /**
     * Removes the passed repository from the registration list.
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder remove(Repository<?, ?> repository) {
        checkNotNull(repository);
        repositories.remove(repository);
        return this;
    }

    /**
     * Verifies if the passed command dispatcher was previously added into the registration list
     * of the Bounded Context this builder is going to build.
     */
    @VisibleForTesting
    boolean hasCommandDispatcher(CommandDispatcher commandDispatcher) {
        checkNotNull(commandDispatcher);
        if (commandDispatcher instanceof Repository) {
            return hasRepository((Repository<?, ?>) commandDispatcher);
        }
        boolean result = commandDispatchers.contains(commandDispatcher);
        return result;
    }

    /**
     * Verifies if the passed event dispatcher was previously added into the registration list
     * of the Bounded Context this builder is going to build.
     */
    @VisibleForTesting
    boolean hasEventDispatcher(EventDispatcher eventDispatcher) {
        checkNotNull(eventDispatcher);
        if (eventDispatcher instanceof Repository) {
            return hasRepository((Repository<?, ?>) eventDispatcher);
        }
        boolean result = eventDispatchers.contains(eventDispatcher);
        return result;
    }

    /**
     * Verifies if the repository with a passed entity class was previously added into the
     * registration list of the Bounded Context this builder is going to build.
     */
    @VisibleForTesting
    <I, E extends Entity<I, ?>> boolean hasRepository(Class<E> entityClass) {
        checkNotNull(entityClass);
        boolean result =
                repositories.stream()
                            .anyMatch(repository -> repository.entityClass()
                                                              .equals(entityClass));
        return result;
    }

    /**
     * Verifies if the passed repository was previously added into the registration list
     * of the Bounded Context this builder is going to build.
     */
    @VisibleForTesting
    boolean hasRepository(Repository<?, ?> repository) {
        checkNotNull(repository);
        boolean result = repositories.contains(repository);
        return result;
    }

    /**
     * Obtains the list of repositories added to the builder by the time of the call.
     *
     * <p>Adding repositories to the builder after this method returns will not update the
     * returned list.
     */
    public ImmutableList<Repository<?, ?>> repositories() {
        return ImmutableList.copyOf(repositories);
    }

    /**
     * Obtains the list of command dispatchers added to the builder by the time of the call.
     *
     * <p>Adding dispatchers to the builder after this method returns will not update the
     * returned list.
     */
    public ImmutableList<CommandDispatcher> commandDispatchers() {
        return ImmutableList.copyOf(commandDispatchers);
    }

    /**
     * Obtains the list of event dispatchers added to the builder by the time of the call.
     *
     * <p>Adding dispatchers to the builder after this method returns will not update the
     * returned list.
     */
    public ImmutableList<EventDispatcher> eventDispatchers() {
        return ImmutableList.copyOf(eventDispatchers);
    }

    /**
     * Obtains the {@link AggregateRootDirectory} to be used in the built context.
     *
     * <p>If no custom implementation is specified, an in-mem implementation is used.
     */
    AggregateRootDirectory aggregateRootDirectory() {
        if (rootDirectory == null) {
            rootDirectory = InMemoryRootDirectory::new;
        }
        return rootDirectory.get();
    }

    /**
     * Sets the supplier of {@link AggregateRootDirectory}-s to use in the built context.
     *
     * <p>By default, an in-mem implementation is used.
     *
     * @param directory
     *         the supplier of aggregate root directories
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder
    setAggregateRootDirectory(Supplier<AggregateRootDirectory> directory) {
        this.rootDirectory = checkNotNull(directory);
        return this;
    }

    /**
     * Obtains the system context feature configuration.
     *
     * <p>Users may enable or disable some features of the system context.
     *
     * @see SystemSettings
     */
    public SystemSettings systemFeatures() {
        return systemFeatures;
    }

    /**
     * Creates a new instance of {@code BoundedContext} with the set configurations.
     *
     * <p>The resulting domain-specific Bounded Context has as internal System Bounded Context.
     * The entities of the System domain describe the entities of the resulting Bounded Context.
     *
     * <p>The System Bounded Context shares some configuration with the Domain Bounded Context,
     * such as:
     * <ul>
     *     <li>{@linkplain #tenantIndex()} tenancy;
     *     <li>{@linkplain ServerEnvironment#transportFactory()} transport facilities.
     * </ul>
     *
     * <p>All the other configuration is NOT shared.
     *
     * <p>The name of the System Bounded Context is derived from the name of the Domain Bounded
     * Context.
     *
     * @return new {@code BoundedContext}
     */
    public BoundedContext build() {
        SystemContext system = buildSystem();
        BoundedContext result = buildDomain(system);
        _debug().log("%s created.", result.nameForLogging());

        registerRepositories(result);
        registerDispatchers(result);
        ServerEnvironment.instance()
                         .delivery()
                         .registerDispatchersIn(result);
        return result;
    }

    EventBus buildEventBus(BoundedContext context) {
        checkNotNull(context);
        eventBus.injectContext(context);
        eventBus.addListener(stand.eventListener());
        return eventBus.build();
    }

    CommandBus buildCommandBus() {
        return commandBus.build();
    }

    private void registerRepositories(BoundedContext result) {
        for (Repository<?, ?> repository : repositories) {
            result.register(repository);
            _debug().log("`%s` registered.", repository);
        }
    }

    private void registerDispatchers(BoundedContext result) {
        commandDispatchers.forEach(result::registerCommandDispatcher);
        eventDispatchers.forEach(result::registerEventDispatcher);
    }

    private BoundedContext buildDomain(SystemContext system) {
        SystemClient systemClient = system.createClient();
        Function<BoundedContextBuilder, DomainContext> instanceFactory =
                builder -> DomainContext.newInstance(builder, systemClient);
        BoundedContext result = buildPartial(instanceFactory, systemClient, system.stand());
        return result;
    }

    private SystemContext buildSystem() {
        BoundedContextBuilder system = new BoundedContextBuilder(systemSpec(), systemFeatures);
        Optional<? extends TenantIndex> tenantIndex = tenantIndex();
        tenantIndex.ifPresent(system::setTenantIndex);
        SystemContext result =
                system.buildPartial(SystemContext::newInstance, NoOpSystemClient.INSTANCE);
        return result;
    }

    private ContextSpec systemSpec() {
        ContextSpec systemSpec = this.spec.toSystem();
        if (!systemFeatures.includePersistentEvents()) {
            systemSpec = systemSpec.notStoringEvents();
        }
        return systemSpec;
    }

    private <B extends BoundedContext>
    B buildPartial(Function<BoundedContextBuilder, B> instanceFactory, SystemClient client) {
        return buildPartial(instanceFactory, client, null);
    }

    private <B extends BoundedContext>
    B buildPartial(Function<BoundedContextBuilder, B> instanceFactory,
                   SystemClient client,
                   @Nullable Stand systemStand) {
        initTenantIndex();
        initCommandBus(client.writeSide());
        this.stand = createStand(systemStand);
        B result = instanceFactory.apply(this);
        return result;
    }

    private void initTenantIndex() {
        if (tenantIndex == null) {
            tenantIndex = isMultitenant()
                          ? TenantIndex.createDefault()
                          : TenantIndex.singleTenant();
        }
    }

    private void initCommandBus(SystemWriteSide systemWriteSide) {
        commandBus.setMultitenant(isMultitenant());
        commandBus.injectSystem(systemWriteSide)
                  .injectTenantIndex(tenantIndex);
    }

    //TODO:2020-06-17:alex.tymchenko: `withSubscriptionRegistryFrom`?
    private Stand createStand(@Nullable Stand systemStand) {
        Stand.Builder result = Stand
                .newBuilder()
                .setMultitenant(isMultitenant());
        if (systemStand != null) {
            result.withSubscriptionRegistryFrom(systemStand);
        }
        return result.build();
    }

    /**
     * Creates a copy of this context builder for the purpose of testing.
     */
    @Internal
    @VisibleForTesting
    public BoundedContextBuilder testingCopy() {
        String name = name().getValue();
        EventEnricher enricher =
                eventEnricher().orElseGet(() -> EventEnricher.newBuilder()
                                                             .build());
        BoundedContextBuilder copy =
                isMultitenant()
                ? BoundedContext.multitenant(name)
                : BoundedContext.singleTenant(name);
        copy.enrichEventsUsing(enricher);
        repositories().forEach(copy::add);
        commandDispatchers().forEach(copy::addCommandDispatcher);
        eventDispatchers().forEach(copy::addEventDispatcher);
        return copy;
    }
}
