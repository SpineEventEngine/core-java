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
import io.spine.server.trace.TracerFactory;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.storage.system.SystemAwareStorageFactory.wrap;
import static io.spine.util.Exceptions.newIllegalStateException;

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
     * Storage factory. Differs between {@linkplain EnvironmentType environment types}.
     */
    private final EnvSetting<StorageFactory> storageFactory = new EnvSetting<>();

    /**
     * Factory of {@code Tracer}s. Differs between {@linkplain EnvironmentType environment types}.
     */
    private final EnvSetting<TracerFactory> tracerFactory = new EnvSetting<>();

    /**
     * Factory for channel-based transport. Differs between {@linkplain EnvironmentType environment
     * types}.
     */
    private final EnvSetting<TransportFactory> transportFactory = new EnvSetting<>();

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
     * Assigns the specified {@code TransportFactory} for the specified application environment.
     *
     * @return this instance of {@code ServerEnvironment}
     */
    @CanIgnoreReturnValue
    public ServerEnvironment use(StorageFactory storage, EnvironmentType envType) {
        checkNotNull(envType);
        environment().register(envType);
        this.storageFactory.use(wrap(storage), envType);
        return this;
    }

    /**
     * Assigns {@code TracerFactory} for the specified application environment.
     *
     * @return this instance of {@code ServerEnvironment}
     */
    @CanIgnoreReturnValue
    public ServerEnvironment use(TracerFactory tracerFactory, EnvironmentType envType) {
        checkNotNull(envType);
        environment().register(envType);
        this.tracerFactory.use(tracerFactory, envType);
        return this;
    }

    /**
     * Configures the specified transport factor to be used under the specified environment type.
     *
     * @return this instance of {@code ServerEnvironment} this instance
     */
    @CanIgnoreReturnValue
    public ServerEnvironment use(TransportFactory transportFactory, EnvironmentType envType) {
        checkNotNull(envType);
        environment().register(envType);
        this.transportFactory.use(transportFactory, envType);
        return this;
    }

    /**
     * Obtains the {@link TracerFactory} associated with the current environment, if it was set.
     */
    public Optional<TracerFactory> tracing() {
        EnvironmentType currentType = environment().type();
        return this.tracerFactory.value(currentType);
    }

    /**
     * Obtains the storage factory for the current environment.
     *
     * <p>For tests, if the value was not set, defaults to a new {@code InMemoryStorageFactory}.
     * <p>For other environments, if the value was not set, throws a {@code IllegalStateException}.
     *
     * @return {@code StorageFactory} instance for the storage for the current environment
     * @throws IllegalStateException
     *         if the value {@code StorageFactory} was not
     *         {@linkplain #use(StorageFactory, EnvironmentType)} configured} prior to the call
     */
    public StorageFactory storageFactory() {
        if (environment().is(tests())) {
            return storageFactory.assignOrDefault(() -> wrap(InMemoryStorageFactory.newInstance()),
                                                  tests());
        }

        EnvironmentType type = environment().type();
        StorageFactory result = storageFactory
                .value(type)
                .orElseThrow(() -> {
                    String className = type.getClass()
                                           .getSimpleName();
                    return newIllegalStateException(
                            "The storage factory for environment `%s` was not" +
                                    "configured. Please call `use(storage, %s);`.",
                            className, className);
                });
        return result;
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
        if (environment().is(tests())) {
            return transportFactory.assignOrDefault(InMemoryTransportFactory::newInstance, tests());
        }

        EnvironmentType type = environment().type();

        TransportFactory result = transportFactory
                .value(type)
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
        this.delivery = Delivery.local();
        resetDeploymentType();
    }

    /**
     * Releases resources associated with this instance.
     */
    @Override
    public void close() throws Exception {
        tracerFactory.ifPresentForEnvironment(Production.type(), AutoCloseable::close);
        transportFactory.ifPresentForEnvironment(Production.type(), AutoCloseable::close);
        storageFactory.ifPresentForEnvironment(Production.type(), AutoCloseable::close);
    }

    private static Tests tests() {
        return Tests.type();
    }
}
