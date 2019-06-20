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
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.core.BoundedContextName;
import io.spine.core.BoundedContextNames;
import io.spine.logging.Logging;
import io.spine.server.aggregate.AggregateRootDirectory;
import io.spine.server.aggregate.InMemoryRootDirectory;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;
import io.spine.server.integration.IntegrationBus;
import io.spine.server.stand.Stand;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.StorageFactorySwitch;
import io.spine.server.tenant.TenantIndex;
import io.spine.server.trace.TracerFactory;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.system.server.NoOpSystemClient;
import io.spine.system.server.SystemClient;
import io.spine.system.server.SystemContext;
import io.spine.system.server.SystemReadSide;
import io.spine.system.server.SystemWriteSide;
import io.spine.system.server.TraceEventObserver;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.core.BoundedContextNames.newName;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A builder for producing {@code BoundedContext} instances.
 *
 * <p>An application can have more than one Bounded Context. To distinguish
 * them, use {@link #setName(String)}. If no ID is given the default, an ID will be assigned.
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"}) // OK for this central piece.
public final class BoundedContextBuilder implements Logging {

    @SuppressWarnings("TestOnlyProblems")
        // Be default, assume test environment to simplify test data preparation.
    private BoundedContextName name = BoundedContextNames.assumingTests();
    private boolean multitenant;
    private TenantIndex tenantIndex;
    private @Nullable Supplier<StorageFactory> storageFactorySupplier;
    private @Nullable Supplier<TracerFactory> tracerFactorySupplier;

    private CommandBus.Builder commandBus;
    private EventBus.Builder eventBus;
    private Stand.Builder stand;
    private IntegrationBus.Builder integrationBus;
    private TransportFactory transportFactory;
    private Supplier<AggregateRootDirectory> rootDirectory;

    /** Repositories to be registered with the Bounded Context being built after its creation. */
    private final List<Repository<?, ?>> repositories = new ArrayList<>();

    /**
     * Prevents direct instantiation.
     *
     * @see BoundedContext#newBuilder()
     */
    BoundedContextBuilder() {
    }

    /**
     * Sets the value of the name for a new Bounded Context.
     *
     * <p>It is the responsibility of an application developer to provide meaningful and unique
     * names for Bounded Contexts. The framework does not check for duplication of names.
     *
     * <p>If the name is not defined in the builder, the context will get
     * {@link BoundedContextNames#assumingTests()} name.
     *
     * @param name an identifier string for a new Bounded Context.
     *             Cannot be null, empty, or blank
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder setName(String name) {
        return setName(newName(name));
    }

    /**
     * Sets the name for a new Bounded Context.
     *
     * <p>It is the responsibility of an application developer to provide meaningful and unique
     * names for Bounded Contexts. The framework does not check for duplication of names.
     *
     * <p>If the name is not defined in the builder, the context will get
     * {@link BoundedContextNames#assumingTests()} name.
     *
     * @param name an identifier string for a new Bounded Context.
     *             Cannot be null, empty, or blank
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder setName(BoundedContextName name) {
        BoundedContextNames.checkValid(name);
        this.name = name;
        return this;
    }

    /**
     * Returns the previously set name or {@link BoundedContextNames#assumingTests()}
     * if the name was not explicitly set.
     */
    public BoundedContextName name() {
        return name;
    }

    @CanIgnoreReturnValue
    public BoundedContextBuilder setMultitenant(boolean value) {
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
    @CanIgnoreReturnValue
    public
    BoundedContextBuilder setStorageFactorySupplier(@Nullable Supplier<StorageFactory> supplier) {
        this.storageFactorySupplier = supplier;
        return this;
    }

    public Optional<Supplier<StorageFactory>> storageFactorySupplier() {
        return Optional.ofNullable(storageFactorySupplier);
    }

    Supplier<StorageFactory> buildStorageFactorySupplier() {
        checkState(storageFactorySupplier != null);
        return storageFactorySupplier;
    }

    @CanIgnoreReturnValue
    public
    BoundedContextBuilder setTracerFactorySupplier(@Nullable Supplier<TracerFactory> supplier) {
        this.tracerFactorySupplier = supplier;
        return this;
    }

    public Optional<Supplier<TracerFactory>> tracerFactorySupplier() {
        return Optional.ofNullable(tracerFactorySupplier);
    }

    Supplier<@Nullable TracerFactory> buildTracerFactorySupplier() {
        @SuppressWarnings("ReturnOfNull")
        Supplier<TracerFactory> defaultSupplier = () -> null;
        return tracerFactorySupplier().orElse(defaultSupplier);
    }

    @CanIgnoreReturnValue
    public BoundedContextBuilder setCommandBus(CommandBus.Builder commandBus) {
        this.commandBus = checkNotNull(commandBus);
        return this;
    }

    Optional<IntegrationBus.Builder> integrationBus() {
        return Optional.ofNullable(integrationBus);
    }

    public Optional<CommandBus.Builder> commandBus() {
        return Optional.ofNullable(commandBus);
    }

    /**
     * Obtains {@code TenantIndex} implementation associated with the Bounded Context.
     */
    public Optional<? extends TenantIndex> tenantIndex() {
        return Optional.ofNullable(tenantIndex);
    }

    TenantIndex buildTenantIndex() {
        TenantIndex result = isMultitenant()
            ? checkNotNull(tenantIndex)
            : TenantIndex.singleTenant();
        return result;
    }

    @CanIgnoreReturnValue
    public BoundedContextBuilder setEventBus(EventBus.Builder eventBus) {
        this.eventBus = checkNotNull(eventBus);
        return this;
    }

    public Optional<EventBus.Builder> eventBus() {
        return Optional.ofNullable(eventBus);
    }

    EventBus buildEventBus() {
        return eventBus.build();
    }

    @CanIgnoreReturnValue
    public BoundedContextBuilder setStand(Stand.Builder stand) {
        this.stand = checkNotNull(stand);
        return this;
    }

    public Optional<Stand.Builder> stand() {
        return Optional.ofNullable(stand);
    }

    Stand buildStand() {
        return stand.build();
    }

    @CanIgnoreReturnValue
    public BoundedContextBuilder setTransportFactory(TransportFactory transportFactory) {
        this.transportFactory = checkNotNull(transportFactory);
        return this;
    }

    public Optional<TransportFactory> transportFactory() {
        return Optional.ofNullable(transportFactory);
    }

    @CanIgnoreReturnValue
    public BoundedContextBuilder setTenantIndex(TenantIndex tenantIndex) {
        if (this.multitenant) {
            checkNotNull(tenantIndex,
                         "TenantRepository cannot be null in multi-tenant BoundedContext.");
        }
        this.tenantIndex = tenantIndex;
        return this;
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
     * Removes the passed repository from the registration list.
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder remove(Repository<?, ?> repository) {
        checkNotNull(repository);
        repositories.remove(repository);
        return this;
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
     * Creates a new instance of {@code BoundedContext} with the set configurations.
     *
     * <p>The resulting domain-specific Bounded Context has as internal System Bounded Context.
     * The entities of the System domain describe the entities of the resulting Bounded Context.
     *
     * <p>The System Bounded Context shares some configuration with the Domain Bounded Context,
     * such as:
     * <ul>
     *     <li>{@linkplain #tenantIndex()} tenancy;
     *     <li>{@linkplain #storageFactory()} storage facilities;
     *     <li>{@linkplain #transportFactory()} transport facilities.
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
        TransportFactory transport = transportFactory()
                .orElseGet(InMemoryTransportFactory::newInstance);
        SystemContext system = buildSystem(transport);
        BoundedContext result = buildDomain(system, transport);
        log().debug("{} created.", result.nameForLogging());

        registerRepositories(result);
        registerTracing(result, system);
        return result;
    }

    private static void registerTracing(BoundedContext domain, BoundedContext system) {
        domain.tracing().ifPresent(tracing -> {
            EventDispatcher<?> observer = new TraceEventObserver(domain.name(), tracing);
            system.registerEventDispatcher(observer);
        });
    }

    private void registerRepositories(BoundedContext result) {
        for (Repository<?, ?> repository : repositories) {
            result.register(repository);
            log().debug("{} registered.", repository);
        }
    }

    private BoundedContext buildDomain(SystemContext system, TransportFactory transport) {
        SystemClient systemClient = system.createClient();
        Function<BoundedContextBuilder, DomainContext> instanceFactory =
                builder -> DomainContext.newInstance(builder, systemClient);
        BoundedContext result = buildPartial(instanceFactory, systemClient, transport);
        return result;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") // Builder methods.
    private SystemContext buildSystem(TransportFactory transport) {
        BoundedContextName name = BoundedContextNames.system(this.name);
        BoundedContextBuilder system = BoundedContext
                .newBuilder()
                .setMultitenant(multitenant)
                .setName(name);
        Optional<? extends Supplier<StorageFactory>> storage = storageFactorySupplier();
        storage.ifPresent(system::setStorageFactorySupplier);
        Optional<? extends TenantIndex> tenantIndex = tenantIndex();
        tenantIndex.ifPresent(system::setTenantIndex);

        SystemContext result = system.buildPartial(SystemContext::newInstance,
                                                   NoOpSystemClient.INSTANCE,
                                                   transport);
        return result;
    }

    private <B extends BoundedContext>
    B buildPartial(Function<BoundedContextBuilder, B> instanceFactory,
                   SystemClient client,
                   TransportFactory transport) {
        StorageFactory storageFactory = storageFactory();

        initTenantIndex(storageFactory);
        initCommandBus(client.writeSide());
        initEventBus(storageFactory);
        initStand(client.readSide());
        initIntegrationBus(transport);

        B result = instanceFactory.apply(this);
        return result;
    }

    private StorageFactory storageFactory() {
        if (storageFactorySupplier == null) {
            storageFactorySupplier = StorageFactorySwitch.newInstance(name, multitenant);
        }
        StorageFactory storageFactory = storageFactorySupplier.get();

        if (storageFactory == null) {
            throw newIllegalStateException(
                    "Supplier of StorageFactory (%s) returned null instance",
                    storageFactorySupplier
            );
        }
        return storageFactory;
    }

    private void initTenantIndex(StorageFactory factory) {
        if (tenantIndex == null) {
            tenantIndex = multitenant
                          ? TenantIndex.createDefault(factory)
                          : TenantIndex.singleTenant();
        }
    }

    private void initCommandBus(SystemWriteSide systemWriteSide) {
        if (commandBus == null) {
            commandBus = CommandBus.newBuilder()
                                   .setMultitenant(this.multitenant);
        } else {
            Boolean commandBusMultitenancy = commandBus.isMultitenant();
            if (commandBusMultitenancy != null) {
                checkSameValue("CommandBus must match multitenancy of BoundedContext. " +
                                       "Status in BoundedContext.Builder: %s CommandBus: %s",
                               commandBusMultitenancy);
            } else {
                commandBus.setMultitenant(this.multitenant);
            }
        }
        commandBus.injectSystem(systemWriteSide)
                  .injectTenantIndex(tenantIndex);
    }

    private void initEventBus(StorageFactory storageFactory) {
        if (eventBus == null) {
            eventBus = EventBus.newBuilder()
                               .setStorageFactory(storageFactory);
        } else {
            boolean eventStoreConfigured = eventBus.getEventStore()
                                                   .isPresent();
            if (!eventStoreConfigured) {
                eventBus.setStorageFactory(storageFactory);
            }
        }
    }

    private void initStand(SystemReadSide systemReadSide) {
        if (stand == null) {
            stand = createStand();
        } else {
            Boolean standMultitenant = stand.isMultitenant();
            // Check that both either multi-tenant or single-tenant.
            if (standMultitenant == null) {
                stand.setMultitenant(multitenant);
            } else {
                checkSameValue("Stand must match multitenancy of BoundedContext. " +
                                       "Status in BoundedContext.Builder: %s Stand: %s",
                               standMultitenant);
            }
        }
        stand.setSystemReadSide(systemReadSide);
    }

    private void initIntegrationBus(TransportFactory factory) {
        integrationBus = IntegrationBus.newBuilder();
        integrationBus.setTransportFactory(factory);
    }

    /**
     * Ensures that the value of the passed flag is equal to the value of
     * the {@link BoundedContextBuilder#multitenant}.
     *
     * @throws IllegalStateException if the flags values do not match
     */
    private void checkSameValue(String errMsgFmt, boolean partMultitenancy) {
        checkState(this.multitenant == partMultitenancy,
                   errMsgFmt,
                   String.valueOf(this.multitenant),
                   String.valueOf(partMultitenancy));
    }

    private Stand.Builder createStand() {
        Stand.Builder result = Stand.newBuilder()
                                    .setMultitenant(multitenant);
        return result;
    }
}
