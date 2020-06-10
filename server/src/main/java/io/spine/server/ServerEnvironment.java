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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.base.Environment;
import io.spine.base.EnvironmentType;
import io.spine.base.Identifier;
import io.spine.base.Production;
import io.spine.base.Tests;
import io.spine.server.commandbus.CommandScheduler;
import io.spine.server.commandbus.ExecutorCommandScheduler;
import io.spine.server.delivery.Delivery;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.storage.system.SystemAwareStorageFactory;
import io.spine.server.trace.TracerFactory;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;

import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.storage.system.SystemAwareStorageFactory.wrap;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The server conditions and configuration under which the application operates.
 */
@SuppressWarnings("ClassWithTooManyMethods" /* there are some deprecated methods to be eliminated later. */)
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
     * <p>If not {@linkplain #use(Delivery, EnvironmentType)}) configured by the end-user},
     * initialized with the {@linkplain Delivery#local() local} delivery by default.
     *
     * <p>Differs between {@linkplain EnvironmentType environment types}.
     */
    private final EnvSetting<Delivery> delivery = new EnvSetting<>();

    /**
     * Storage factory. Differs between {@linkplain EnvironmentType environment types}.
     *
     * <p>Defaults to an {@code InMemoryStorageFactory} for tests.
     */
    private final EnvSetting<StorageFactory> storageFactory =
            new EnvSetting<>(Tests.class, () -> wrap(InMemoryStorageFactory.newInstance()));

    /**
     * Factory of {@code Tracer}s. Differs between {@linkplain EnvironmentType environment types}.
     */
    private final EnvSetting<TracerFactory> tracerFactory = new EnvSetting<>();

    /**
     * Factory for channel-based transport. Differs between {@linkplain EnvironmentType environment
     * types}.
     *
     * <p>Defaults to an {@code InMemoryTransportFactory} for tests.
     */
    private final EnvSetting<TransportFactory> transportFactory =
            new EnvSetting<>(Tests.class, InMemoryTransportFactory::newInstance);

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
     * Updates the delivery for the current environment.
     *
     * <p>This method is most typically used upon an application start. It's very uncommon and
     * even dangerous to update the delivery mechanism later when the message delivery
     * process may have been already used by various {@code BoundedContext}s.
     *
     * @deprecated use {@link #use(Delivery, EnvironmentType)}
     */
    @Deprecated
    public synchronized void configureDelivery(Delivery delivery) {
        checkNotNull(delivery);
        use(delivery, this.delivery, new Production());
    }

    /**
     * Returns the delivery mechanism specific to this environment.
     *
     * <p>Unless {@linkplain #use(Delivery, EnvironmentType) updated manually}, returns
     * a {@linkplain Delivery#local() local implementation} of {@code Delivery}. Also configures
     * a new {@code InMemoryStorageFactory} for the current environment, as such:
     *
     * <pre>
     *
     *     ServerEnvironment serverEnvironment = ServerEnvironment.instance();
     *
     *     serverEnvironment.reset();
     *
     *     // This is a new local `Delivery`.
     *     Delivery delivery = serverEnvironment.delivery();
     *
     *     // A new storage factory was assigned.
     *     StorageFactory storageFactory = serverEnvironment.storageFactory();
     *     assertThat(storageFactory).isInstanceOf(InMemoryStorageFactory.class);
     * </pre>
     */
    public synchronized Delivery delivery() {
        EnvironmentType currentEnv = environment().type();
        Optional<Delivery> currentDelivery = this.delivery.optionalValue(currentEnv.getClass());
        if (currentDelivery.isPresent()) {
            return currentDelivery.get();
        }
        Delivery localDelivery = Delivery.local();
        this.delivery.use(localDelivery, currentEnv.getClass());
        return localDelivery;
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
     * Assigns the specified {@code TransportFactory} for the specified application environment.
     *
     * <p>You may use {@code new Tests()}, {@code new Production()} or an environment type
     * defined by you.
     *
     * @return this instance of {@code ServerEnvironment}
     */
    @CanIgnoreReturnValue
    public ServerEnvironment use(StorageFactory factory, EnvironmentType envType) {
        checkNotNull(factory);
        SystemAwareStorageFactory wrapped = wrap(factory);
        use(wrapped, storageFactory, envType);
        return this;
    }

    /**
     * Assigns the specified {@code Delivery} for the selected environment.
     *
     * <p>You may use {@code new Tests()}, {@code new Production()} or an environment type
     * defined by you.
     *
     * @return this instance of {@code ServerEnvironment}
     */
    public ServerEnvironment use(Delivery delivery, EnvironmentType envType) {
        use(delivery, this.delivery, envType);
        return this;
    }

    /**
     * Assigns {@code TracerFactory} for the specified application environment.
     *
     * <p>You may use {@code new Tests()}, {@code new Production()} or an environment type
     * defined by you.
     *
     * @return this instance of {@code ServerEnvironment}
     */
    @CanIgnoreReturnValue
    public ServerEnvironment use(TracerFactory factory, EnvironmentType envType) {
        use(factory, tracerFactory, envType);
        return this;
    }

    /**
     * Configures the specified transport factory for the selected type of environment.
     *
     * <p>You may use {@code new Tests()}, {@code new Production()} or an environment type
     * defined by you.
     *
     * @return this instance of {@code ServerEnvironment}
     */
    @CanIgnoreReturnValue
    public ServerEnvironment use(TransportFactory factory, EnvironmentType envType) {
        use(factory, transportFactory, envType);
        return this;
    }

    /**
     * Assigns the specified {@code StorageFactory} for tests.
     *
     * @deprecated use {@link #use(StorageFactory, EnvironmentType)}, specifying the
     *         {@code new Tests())}
     */
    @Deprecated
    public void configureStorageForTests(StorageFactory factory) {
        checkNotNull(factory);
        use(factory, new Tests());
    }

    /**
     * Assigns the specified {@code StorageFactory} for the {@link Production} application
     * environment.
     *
     * @deprecated use {@link #use(StorageFactory, EnvironmentType)}, specifying the
     *         {@code new Production()} or any other environment type
     */
    @Deprecated
    public void configureStorage(StorageFactory factory) {
        checkNotNull(factory);
        use(factory, new Production());
    }

    /**
     * Obtains the {@link TracerFactory} associated with the current environment, if it was set.
     */
    public Optional<TracerFactory> tracing() {
        EnvironmentType currentType = environment().type();
        return this.tracerFactory.optionalValue(currentType.getClass());
    }

    /**
     * Obtains the storage factory for the current environment.
     *
     * <p>For tests, if the value was not set, defaults to a new {@code InMemoryStorageFactory}.
     *
     * <p>For other environments, if the value was not set, throws a {@code IllegalStateException}.
     *
     * @return {@code StorageFactory} instance for the storage for the current environment
     * @throws IllegalStateException
     *         if the value {@code StorageFactory} was not
     *         {@linkplain #use(StorageFactory, EnvironmentType)} configured} prior to the call
     */
    public StorageFactory storageFactory() {
        EnvironmentType type = environment().type();
        StorageFactory result = storageFactory
                .optionalValue(type.getClass())
                .orElseThrow(() -> {
                    String className = type.getClass()
                                           .getSimpleName();
                    return newIllegalStateException(
                            "The storage factory for environment `%s` was not " +
                                    "configured. Please call `use(storage, %s);`.",
                            className, className);
                });
        return result;
    }

    public Optional<StorageFactory> optionalStorageFactory() {
        EnvironmentType type = environment().type();
        return storageFactory.optionalValue(type.getClass());
    }

    /**
     * Obtains the transport factory for the current environment.
     *
     * <p>In the {@linkplain Tests testing environment}, if the factory was not assigned, assigns
     * a new {@code InMemoryTransportFactory} and returns it.
     *
     * <p>For all other environment types, throws an {@code IllegalStateException}.
     *
     * <p>If the factory is not assigned in the Tests mode, assigns the instance of
     * {@link InMemoryTransportFactory} and returns it.
     */
    public TransportFactory transportFactory() {
        EnvironmentType type = environment().type();
        TransportFactory result = transportFactory
                .optionalValue(type.getClass())
                .orElseThrow(() -> {
                    String environmentName = type.getClass()
                                                 .getSimpleName();
                    return newIllegalStateException(
                            "Transport factory is not assigned for the current environment `%s`. " +
                                    "Please call `use(transportFactory, %s);`.",
                            environmentName, environmentName);
                });

        return result;
    }

    private static Environment environment() {
        return Environment.instance();
    }

    /**
     * This is test-only method required for cleaning of the server environment instance in tests.
     */
    @VisibleForTesting
    public void reset() {
        this.transportFactory.reset();
        this.tracerFactory.reset();
        this.storageFactory.reset();
        this.delivery.reset();
        EnvironmentType currentEnv = environment().type();
        this.delivery.use(Delivery.local(), currentEnv.getClass());
        resetDeploymentType();
    }

    /**
     * Releases resources associated with this instance.
     */
    @Override
    public void close() throws Exception {
        tracerFactory.ifPresentForEnvironment(Production.class, AutoCloseable::close);
        transportFactory.ifPresentForEnvironment(Production.class, AutoCloseable::close);
        storageFactory.ifPresentForEnvironment(Production.class, AutoCloseable::close);
    }

    private static <V> void use(V value, EnvSetting<V> setting, EnvironmentType type) {
        checkNotNull(value);
        checkNotNull(type);
        checkNotNull(setting);
        environment().register(type);
        setting.use(value, type.getClass());
    }
}
