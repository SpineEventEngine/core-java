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
import io.spine.base.Environment;
import io.spine.base.Identifier;
import io.spine.server.EnvironmentDependantValue.Configurator;
import io.spine.server.commandbus.CommandScheduler;
import io.spine.server.commandbus.ExecutorCommandScheduler;
import io.spine.server.delivery.Delivery;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.storage.system.SystemAwareStorageFactory;
import io.spine.server.trace.TracerFactory;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * The server conditions and configuration under which the application operates.
 */
public final class ServerEnvironment implements AutoCloseable {

    private static final ServerEnvironment INSTANCE = new ServerEnvironment();

    /**
     * The deployment detector is instantiated with a system {@link DeploymentDetector} and
     * can be reassigned the value using {@link #configureDeployment(Supplier)}.
     *
     * <p>Value from this supplier are used to {@linkplain #deploymentType()
     * get the deployment type}.
     */
    private Supplier<DeploymentType> deploymentDetector = DeploymentDetector.newInstance();

    /**
     * The identifier of the server instance running in scope of this application.
     *
     * <p>It is currently impossible to set the node identifier directly. This is a subject
     * to change in the future framework versions.
     */
    private final NodeId nodeId;

    /**
     * The strategy of delivering the messages received by entity repositories
     * to the entity instances.
     *
     * <p>If not {@linkplain #configureDelivery(Delivery) configured by the end-user},
     * initialized with the {@linkplain Delivery#local() local} delivery by default.
     */
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    // No synchronization in `reset()` as it is test-only.
    private @MonotonicNonNull Delivery delivery;

    /**
     * Storage factories for both the production and the testing environments.
     */
    private final EnvironmentDependantValue<StorageFactory> storages =
            EnvironmentDependantValue
                    .<StorageFactory>newBuilder()
                    .wrappingTests(SystemAwareStorageFactory::wrap)
                    .wrappingProduction(SystemAwareStorageFactory::wrap)
                    .build();

    /**
     * The factory of {@code Tracer}s used in this environment.
     */
    private @Nullable TracerFactory tracerFactory;

    /**
     * The production and testing factories for channel-based transport.
     */
    private final EnvironmentDependantValue<TransportFactory> transportFactories =
            EnvironmentDependantValue
                    .<TransportFactory>newBuilder()
                    .build();

    /**
     * Provides schedulers used by all {@code CommandBus} instances of this environment.
     */
    private Supplier<CommandScheduler> commandScheduler;

    private ServerEnvironment() {
        nodeId = NodeId.newBuilder()
                       .setValue(Identifier.newUuid())
                       .vBuild();
        commandScheduler = ExecutorCommandScheduler::new;
    }

    /**
     * Returns a singleton instance.
     */
    public static ServerEnvironment instance() {
        return INSTANCE;
    }

    /**
     * The type of the environment application is deployed to.
     */
    public DeploymentType deploymentType() {
        return deploymentDetector.get();
    }

    /**
     * Updates the delivery for this environment.
     *
     * <p>This method is most typically used upon an application start. It's very uncommon and
     * even dangerous to update the delivery mechanism later when the message delivery
     * process may have been already used by various {@code BoundedContext}s.
     */
    public synchronized void configureDelivery(Delivery delivery) {
        checkNotNull(delivery);
        this.delivery = delivery;
    }

    /**
     * Returns the delivery mechanism specific to this environment.
     *
     * <p>Unless {@linkplain #configureDelivery(Delivery) updated manually}, returns
     * a {@linkplain Delivery#local() local implementation} of {@code Delivery}.
     */
    public synchronized Delivery delivery() {
        if (delivery == null) {
            delivery = Delivery.local();
        }
        return delivery;
    }

    /**
     * Obtains command scheduling mechanism used by {@code CommandBus} in this environment.
     */
    public CommandScheduler newCommandScheduler() {
        return commandScheduler.get();
    }

    /**
     * Assigns command scheduling mechanism used at this environment by all
     * {@code CommandBus} instances.
     *
     * <p>If not configured, {@link ExecutorCommandScheduler} is used.
     */
    public void scheduleCommandsUsing(Supplier<CommandScheduler> commandScheduler) {
        checkNotNull(commandScheduler);
        this.commandScheduler = commandScheduler;
    }

    /**
     * Obtains the identifier of the server node, on which this code is running at the moment.
     *
     * <p>At the moment, the node identifier is always UUID-generated. In future versions of the
     * framework it is expected to become configurable.
     */
    public NodeId nodeId() {
        return nodeId;
    }

    /**
     * Sets the default {@linkplain DeploymentType deployment type}
     * {@linkplain Supplier supplier} which utilizes system properties.
     */
    private void resetDeploymentType() {
        Supplier<DeploymentType> supplier = DeploymentDetector.newInstance();
        configureDeployment(supplier);
    }

    /**
     * Makes the {@link #deploymentType()} return the values from the provided supplier.
     *
     * <p>When supplying your own deployment type in tests, remember to
     * {@linkplain #reset() reset it} during tear down.
     */
    @VisibleForTesting
    public void configureDeployment(Supplier<DeploymentType> supplier) {
        checkNotNull(supplier);
        deploymentDetector = supplier;
    }

    /**
     * Returns the configuration object to change the storage factory.
     *
     * <p>Callers may configure the testing factory via
     * {@code useStorageFactory(testFactory).forTests()} and the production factory via
     * {@code useStorageFactory(productionFactory).forProduction()}.
     *
     * @return the storage factory configuration object
     */
    public Configurator useStorageFactory(StorageFactory storage) {
        return this.storages.configure(storage);
    }

    /**
     * Assigns {@code TracerFactory} to this server environment.
     */
    public void configureTracing(TracerFactory tracerFactory) {
        this.tracerFactory = checkNotNull(tracerFactory);
    }

    /**
     * Obtains {@link TracerFactory} associated with this server environment.
     */
    public Optional<TracerFactory> tracing() {
        return Optional.ofNullable(tracerFactory);
    }

    /**
     * Obtains the storage factory for the current environment.
     *
     * <p>For tests, if the value was not set, defaults to a new {@code InMemoryStorageFactory}.
     * <p>For production, if the value was not set, throws a {@code NullPointerException}.
     *
     * @return {@code StorageFactory} instance for the storage for the current environment
     * @throws NullPointerException
     *         if the production {@code StorageFactory} was not
     *         {@linkplain #useStorageFactory(StorageFactory)} configured} prior to the call
     */
    public StorageFactory storageFactory() {
        if (environment().isTests()) {
            if (!storages.tests()
                         .isPresent()) {
                storages.configure(InMemoryStorageFactory.newInstance())
                        .forTests();
            }
            return storages.tests()
                           .get();

        }

        StorageFactory result = storages
                .production()
                .orElseThrow(() -> new NullPointerException(format(
                        "Production `%s` is not configured." +
                                " Please call `configureStorage()`.",
                        StorageFactory.class.getSimpleName())));

        return result;
    }

    private static Environment environment() {
        return Environment.instance();
    }

    /**
     * Returns the configuration object to change the transport factory.
     *
     * <p>Callers may configure the testing factory via
     * {@code configureTransport().tests(testFactory)} and the production factory via
     * {@code configureTransport().production(productionFactory)}.
     *
     * @return the transport factory configuration object
     */
    public Configurator useTransportFactory(TransportFactory transportFactory) {
        return transportFactories.configure(transportFactory);
    }

    /**
     * Obtains the transport factory for the current environment.
     * <p>If the factory is not assigned in the Production mode, throws
     * {@code NullPointerException} with the instruction to call
     * {@link #useTransportFactory(TransportFactory)}.
     *
     * <p>If the factory is not assigned in the Tests mode, assigns the instance of
     * {@link InMemoryTransportFactory} and returns it.
     */
    public TransportFactory transportFactory() {
        if (environment().isTests()) {
            if (!transportFactories.tests()
                                   .isPresent()) {
                this.transportFactories.configure(InMemoryTransportFactory.newInstance())
                                       .forTests();
            }
            return transportFactories.tests()
                                     .get();
        }

        TransportFactory result = transportFactories
                .production()
                .orElseThrow(() -> new NullPointerException(format(
                        "`%s` is not assigned. Please call `configureTransport()`.",
                        TransportFactory.class.getName())));

        return result;
    }

    /**
     * This is test-only method required for cleaning of the server environment instance in tests.
     */
    @VisibleForTesting
    public void reset() {
        this.transportFactories.nullifyBoth();
        this.tracerFactory = null;
        this.storages.nullifyBoth();
        this.delivery = Delivery.local();
        resetDeploymentType();
    }

    /**
     * Releases resources associated with this instance.
     */
    @Override
    public void close() throws Exception {
        if (tracerFactory != null) {
            tracerFactory.close();
        }
        if (transportFactories.production()
                              .isPresent()) {
            transportFactories.production()
                              .get()
                              .close();
        }
        if (storages.production()
                    .isPresent()) {
            storages.production()
                    .get()
                    .close();
        }
    }
}
