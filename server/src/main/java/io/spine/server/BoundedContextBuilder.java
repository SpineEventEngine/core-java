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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import io.spine.core.BoundedContextName;
import io.spine.core.BoundedContextNames;
import io.spine.logging.Logging;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.EventBus;
import io.spine.server.integration.IntegrationBus;
import io.spine.server.stand.Stand;
import io.spine.server.stand.StandStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.StorageFactorySwitch;
import io.spine.server.tenant.TenantIndex;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.system.server.NoOpSystemGateway;
import io.spine.system.server.SystemBoundedContext;
import io.spine.system.server.SystemGateway;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.core.BoundedContextNames.newName;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A builder for producing {@code BoundedContext} instances.
 *
 * <p>An application can have more than one bounded context. To distinguish
 * them use {@link #setName(String)}. If no ID is given the default ID will be assigned.
 *
 * @author Dmytro Dashenkov
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"}) // OK for this central piece.
@CanIgnoreReturnValue
public final class BoundedContextBuilder implements Logging {

    private BoundedContextName name = BoundedContextNames.assumingTests();
    private boolean multitenant;
    private TenantIndex tenantIndex;
    private TransportFactory transportFactory;
    private @Nullable Supplier<StorageFactory> storageFactorySupplier;

    private CommandBus.Builder commandBus;
    private EventBus.Builder eventBus;
    private Stand.Builder stand;
    private IntegrationBus.Builder integrationBus;

    /**
     * Prevents direct instantiation.
     *
     * @see BoundedContext#newBuilder()
     */
    BoundedContextBuilder() {
    }

    /**
     * Sets the value of the name for a new bounded context.
     *
     * <p>It is the responsibility of an application developer to provide meaningful and unique
     * names for bounded contexts. The framework does not check for duplication of names.
     *
     * <p>If the name is not defined in the builder, the context will get
     * {@link BoundedContextNames#assumingTests()} name.
     *
     * @param name an identifier string for a new bounded context.
     *             Cannot be null, empty, or blank
     */
    public BoundedContextBuilder setName(String name) {
        return setName(newName(name));
    }

    /**
     * Sets the name for a new bounded context.
     *
     * <p>It is the responsibility of an application developer to provide meaningful and unique
     * names for bounded contexts. The framework does not check for duplication of names.
     *
     * <p>If the name is not defined in the builder, the context will get
     * {@link BoundedContextNames#assumingTests()} name.
     *
     * @param name an identifier string for a new bounded context.
     *             Cannot be null, empty, or blank
     */
    public BoundedContextBuilder setName(BoundedContextName name) {
        BoundedContextNames.checkValid(name);
        this.name = name;
        return this;
    }

    /**
     * Returns the previously set name or {@link BoundedContextNames#assumingTests()}
     * if the name was not explicitly set.
     */
    public BoundedContextName getName() {
        return name;
    }

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
    public
    BoundedContextBuilder setStorageFactorySupplier(@Nullable Supplier<StorageFactory> supplier) {
        this.storageFactorySupplier = supplier;
        return this;
    }

    public Optional<Supplier<StorageFactory>> getStorageFactorySupplier() {
        return Optional.ofNullable(storageFactorySupplier);
    }

    Supplier<StorageFactory> buildStorageFactorySupplier() {
        checkState(storageFactorySupplier != null);
        return storageFactorySupplier;
    }

    public BoundedContextBuilder setCommandBus(CommandBus.Builder commandBus) {
        this.commandBus = checkNotNull(commandBus);
        return this;
    }

    public Optional<CommandBus.Builder> getCommandBus() {
        return Optional.ofNullable(commandBus);
    }

    public Optional<? extends TenantIndex> getTenantIndex() {
        return Optional.ofNullable(tenantIndex);
    }

    TenantIndex buildTenantIndex() {
        TenantIndex result = isMultitenant()
            ? checkNotNull(tenantIndex)
            : TenantIndex.singleTenant();
        return result;
    }

    public BoundedContextBuilder setEventBus(EventBus.Builder eventBus) {
        this.eventBus = checkNotNull(eventBus);
        return this;
    }

    public Optional<EventBus.Builder> getEventBus() {
        return Optional.ofNullable(eventBus);
    }

    EventBus buildEventBus() {
        return eventBus.build();
    }

    public BoundedContextBuilder setStand(Stand.Builder stand) {
        this.stand = checkNotNull(stand);
        return this;
    }

    public Optional<Stand.Builder> getStand() {
        return Optional.ofNullable(stand);
    }

    Stand buildStand() {
        return stand.build();
    }

    public BoundedContextBuilder setIntegrationBus(IntegrationBus.Builder integrationBus) {
        this.integrationBus = checkNotNull(integrationBus);
        return this;
    }

    public Optional<IntegrationBus.Builder> getIntegrationBus() {
        return Optional.ofNullable(integrationBus);
    }

    public BoundedContextBuilder setTenantIndex(TenantIndex tenantIndex) {
        if (this.multitenant) {
            checkNotNull(tenantIndex,
                         "TenantRepository cannot be null in multi-tenant BoundedContext.");
        }
        this.tenantIndex = tenantIndex;
        return this;
    }

    public BoundedContextBuilder setTransportFactory(TransportFactory transportFactory) {
        this.transportFactory = checkNotNull(transportFactory);
        return this;
    }

    /**
     * Creates a new instance of {@code BoundedContext} with the set configurations.
     *
     * <p>The resulting domain-specific bounded context has as internal System bounded context.
     * The entities of the System domain describe the entities of the resulting bounded context.
     *
     * <p>The System bounded contexts shares some configuration with the domain bounded context,
     * such as:
     * <ul>
     *     <li>{@linkplain #getTenantIndex()} tenancy;
     *     <li>{@linkplain #getStorageFactory()} storage facilities;
     *     <li>{@linkplain #getTransportFactory()} transport facilities.
     * </ul>
     *
     * <p>All the other configuration is NOT shared.
     *
     * <p>The name of the System bounded context is derived from the name of the domain bounded
     * context.
     *
     * @return new {@code BoundedContext}
     */
    @CheckReturnValue
    public BoundedContext build() {
        SystemBoundedContext system = buildSystem();
        BoundedContext result = buildDefault(system);
        log().info(result.nameForLogging() + " created.");
        return result;
    }

    private BoundedContext buildDefault(SystemBoundedContext system) {
        BiFunction<BoundedContextBuilder, SystemGateway, DomainBoundedContext>
                instanceFactory = DomainBoundedContext::newInstance;
        SystemGateway systemGateway = SystemGateway.newInstance(system);
        BoundedContext result = buildPartial(instanceFactory, systemGateway);
        return result;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") // Builder methods.
    private SystemBoundedContext buildSystem() {
        BoundedContextName name = BoundedContextNames.system(this.name);
        BoundedContextBuilder system = BoundedContext
                .newBuilder()
                .setMultitenant(multitenant)
                .setName(name)
                .setTransportFactory(getTransportFactory());
        Optional<? extends Supplier<StorageFactory>> storage = getStorageFactorySupplier();
        storage.ifPresent(system::setStorageFactorySupplier);
        Optional<? extends TenantIndex> tenantIndex = getTenantIndex();
        tenantIndex.ifPresent(system::setTenantIndex);

        BiFunction<BoundedContextBuilder, SystemGateway, SystemBoundedContext> instanceFactory =
                (builder, systemGateway) -> SystemBoundedContext.newInstance(builder);
        NoOpSystemGateway systemGateway = NoOpSystemGateway.INSTANCE;
        SystemBoundedContext result = system.buildPartial(instanceFactory, systemGateway);
        return result;
    }

    private <B extends BoundedContext> B
    buildPartial(BiFunction<BoundedContextBuilder, SystemGateway, B> instanceFactory,
                 SystemGateway systemGateway) {
        StorageFactory storageFactory = getStorageFactory();

        TransportFactory transportFactory = getTransportFactory();

        initTenantIndex(storageFactory);
        initCommandBus(systemGateway);
        initEventBus(storageFactory);
        initStand(storageFactory);
        initIntegrationBus(transportFactory);

        B result = instanceFactory.apply(this, systemGateway);
        return result;
    }

    private StorageFactory getStorageFactory() {
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

    private TransportFactory getTransportFactory() {
        if (transportFactory == null) {
            transportFactory = InMemoryTransportFactory.newInstance();
        }
        return transportFactory;
    }

    private void initTenantIndex(StorageFactory factory) {
        if (tenantIndex == null) {
            tenantIndex = multitenant
                          ? TenantIndex.createDefault(factory)
                          : TenantIndex.singleTenant();
        }
    }

    private void initCommandBus(SystemGateway systemGateway) {
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
        commandBus.injectSystemGateway(systemGateway)
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

    private void initStand(StorageFactory factory) {
        if (stand == null) {
            stand = createStand(factory);
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

    private Stand.Builder createStand(StorageFactory factory) {
        StandStorage standStorage = factory.createStandStorage();
        Stand.Builder result = Stand.newBuilder()
                                    .setMultitenant(multitenant)
                                    .setStorage(standStorage);
        return result;
    }
}
