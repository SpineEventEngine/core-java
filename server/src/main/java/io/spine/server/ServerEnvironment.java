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
import io.spine.annotation.Internal;
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
 *
 * <h3>Configuration</h3>
 *
 * <p>Some parts of the {@code ServerEnvironment} can be customized based on the {@code
 * EnvironmentType}. To do so, one of the overloads of the {@code use} method can be called.
 * Two environment types exist out of the box: {@link Tests} and {@link Production}. For example:
 *
 * <pre>
 *     ServerEnvironment.use(productionStorageFactory, Production.class)
 *                      .use(testingStorageFactory, Tests.class)
 *                      .use(memoizingTracerFactory, Production.class)
 * </pre>
 *
 * <p>A custom environment type may also be used:
 * <pre>
 *     final class StagingEnvironment extends EnvironmentType {
 *         ...
 *     }
 *
 *     ServerEnvironment.use(inMemoryStorageFactory, Staging.class);
 *
 *     // Or, if `Staging` expects dependencies to its constructor, it can turn into this:
 *     ServerEnvironment.use(inMemoryStorageFactory, new StagingEnvironment(dependencies));
 * </pre>
 *
 * <p>If {@code Staging} is {@link Environment#type() enabled}, the specified value is going to be
 * returned on {@link #storageFactory()}.
 *
 * <h3>Relation to Bounded Contexts</h3>
 *
 * <p>A {@code ServerEnvironment} instance is a singleton. It means that all Bounded Contexts
 * which are deployed within the same JVM share the configuration of the server environment.
 * Namely, under the same environment type, all Bounded Contexts will share the same storage
 * factory, transport factory, delivery and tracing tools.
 *
 * <p>If one decides that some of the Bounded Contexts of an application requires custom
 * settings, they should arrange a separate deployable artifact and distinct configuration
 * of the respective {@code ServerEnvironment} for those Bounded Contexts. In this way,
 * the Contexts would reside in their own JVMs and not overlap on interacting with this singleton.
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
        use(delivery, this.delivery, environment().type());
    }

    /**
     * Returns the delivery mechanism specific to this environment.
     *
     * <p>Unless {@linkplain #use(Delivery, EnvironmentType) updated manually}, returns
     * a {@linkplain Delivery#local() local implementation} of {@code Delivery}.
     */
    public synchronized Delivery delivery() {
        Class<? extends EnvironmentType> currentEnv = environment().type();
        Optional<Delivery> currentDelivery = this.delivery.optionalValue(currentEnv);
        if (currentDelivery.isPresent()) {
            return currentDelivery.get();
        }
        Delivery localDelivery = Delivery.local();
        this.delivery.use(localDelivery, currentEnv);
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
     * Assigns the specified {@code TransportFactory} for the specified application environment.
     *
     * @return this instance of {@code ServerEnvironment}
     */
    @CanIgnoreReturnValue
    public ServerEnvironment use(StorageFactory factory, Class<? extends EnvironmentType> type) {
        checkNotNull(factory);
        SystemAwareStorageFactory wrapped = wrap(factory);
        use(wrapped, storageFactory, type);
        return this;
    }

    /**
     * Assigns the specified {@code Delivery} for the selected environment.
     *
     * @return this instance of {@code ServerEnvironment}
     */
    @CanIgnoreReturnValue
    public ServerEnvironment use(Delivery delivery, EnvironmentType envType) {
        use(delivery, this.delivery, envType);
        return this;
    }

    /**
     * Assigns the specified {@code Delivery} for the selected environment.
     *
     * @return this instance of {@code ServerEnvironment}
     */
    @CanIgnoreReturnValue
    public ServerEnvironment use(Delivery delivery, Class<? extends EnvironmentType> envType) {
        use(delivery, this.delivery, envType);
        return this;
    }

    /**
     * Assigns {@code TracerFactory} for the specified application environment.
     *
     * @return this instance of {@code ServerEnvironment}
     */
    @CanIgnoreReturnValue
    public ServerEnvironment use(TracerFactory factory, EnvironmentType envType) {
        use(factory, tracerFactory, envType);
        return this;
    }

    /**
     * Assigns {@code TracerFactory} for the specified application environment.
     *
     * @return this instance of {@code ServerEnvironment}
     */
    @CanIgnoreReturnValue
    public ServerEnvironment use(TracerFactory factory, Class<? extends EnvironmentType> envType) {
        use(factory, tracerFactory, envType);
        return this;
    }

    /**
     * Configures the specified transport factory for the selected type of environment.
     *
     * @return this instance of {@code ServerEnvironment}
     */
    @CanIgnoreReturnValue
    public ServerEnvironment use(TransportFactory factory, EnvironmentType envType) {
        use(factory, transportFactory, envType);
        return this;
    }

    /**
     * Configures the specified transport factory for the selected type of environment.
     *
     * @return this instance of {@code ServerEnvironment}
     */
    @CanIgnoreReturnValue
    public ServerEnvironment use(TransportFactory factory, Class<? extends EnvironmentType> type) {
        use(factory, transportFactory, type);
        return this;
    }

    /**
     * Assigns the specified {@code StorageFactory} for tests.
     *
     * @deprecated use {@link #use(StorageFactory, Class)}, specifying {@code Tests.class)}
     */
    @Deprecated
    public void configureStorageForTests(StorageFactory factory) {
        checkNotNull(factory);
        use(factory, Tests.class);
    }

    /**
     * Assigns the specified {@code StorageFactory} for the {@link Production} application
     * environment.
     *
     * @deprecated use {@link #use(StorageFactory, Class)}, specifying the
     *         {@code Production.class} or any other environment type
     */
    @Deprecated
    public void configureStorage(StorageFactory factory) {
        checkNotNull(factory);
        use(factory, Production.class);
    }

    /**
     * Obtains the {@link TracerFactory} associated with the current environment, if it was set.
     */
    public Optional<TracerFactory> tracing() {
        Class<? extends EnvironmentType> currentType = environment().type();
        return this.tracerFactory.optionalValue(currentType);
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
     *         if the value {@code StorageFactory} was not {@linkplain #use(StorageFactory, Class)}
     *         configured} prior to the call
     */
    public StorageFactory storageFactory() {
        Class<? extends EnvironmentType> type = environment().type();
        StorageFactory result = storageFactory
                .optionalValue(type)
                .orElseThrow(() -> {
                    String className = type.getSimpleName();
                    return newIllegalStateException(
                            "The storage factory for environment `%s` was not " +
                                    "configured. Please call `use(storage, %s);`.",
                            className, className);
                });
        return result;
    }

    /**
     * Returns a storage factory for the current environment, or an empty {@code Optional} if it
     * was not configured.
     */
    @Internal
    public Optional<StorageFactory> optionalStorageFactory() {
        Class<? extends EnvironmentType> type = environment().type();
        return storageFactory.optionalValue(type);
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
        Class<? extends EnvironmentType> type = environment().type();
        TransportFactory result = transportFactory
                .optionalValue(type)
                .orElseThrow(() -> {
                    String environmentName = type.getSimpleName();
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
        Class<? extends EnvironmentType> currentEnv = environment().type();
        this.delivery.use(Delivery.local(), currentEnv);
        resetDeploymentType();
    }

    /**
     * Releases resources associated with this instance.
     */
    @Override
    public void close() throws Exception {
        tracerFactory.apply(AutoCloseable::close);
        transportFactory.apply(AutoCloseable::close);
        storageFactory.apply(AutoCloseable::close);
    }

    private static <V> void use(V value, EnvSetting<V> setting, EnvironmentType type) {
        checkNotNull(value);
        checkNotNull(type);
        checkNotNull(setting);
        environment().register(type);
        setting.use(value, type.getClass());
    }

    private static <V> void use(V value,
                                EnvSetting<V> setting,
                                Class<? extends EnvironmentType> type) {
        checkNotNull(value);
        checkNotNull(type);
        checkNotNull(setting);
        setting.use(value, type);
    }
}
