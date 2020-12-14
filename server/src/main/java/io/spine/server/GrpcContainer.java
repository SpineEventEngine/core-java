/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessServerBuilder;
import io.spine.client.ConnectionConstants;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * Wrapping container for gRPC server.
 *
 * <p>Maintains and deploys several of gRPC services within a single server.
 *
 * <p>Uses {@link ServerServiceDefinition}s of each service.
 */
public final class GrpcContainer {

    private static final String SERVER_NOT_STARTED_MSG =
            "gRPC server was not started or is shut down already.";

    /**
     * The port at which the container is exposed.
     *
     * <p>Is {@code null} if the container is exposed in the in-process mode with a server name.
     */
    private final @Nullable Integer port;
    private final @Nullable String serverName;
    private final ImmutableSet<ServerServiceDefinition> services;

    private @Nullable Server grpcServer;

    @VisibleForTesting
    private @Nullable Server injectedServer;

    /**
     * Creates a new builder for the container.
     *
     * @deprecated please use {@link #atPort(int)} or {@link #inProcess(String)}
     */
    @Deprecated
    public static Builder newBuilder() {
        return new Builder(ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT, null);
    }

    /**
     * Initiates creating a container exposed at the given port.
     */
    public static Builder atPort(int port) {
        return new Builder(port, null);
    }

    /**
     * Initiates creating an in-process container exposed with the given server name.
     *
     * <p>The container in fully-featured, high performance, and is useful in testing.
     */
    public static Builder inProcess(String serverName) {
        return new Builder(null, serverName);
    }

    private GrpcContainer(Builder builder) {
        this.port = builder.port().orElse(null);
        this.serverName = builder.serverName().orElse(null);
        this.services = builder.services();
    }

    /**
     * Obtains the port at which the container is exposed, or empty {@code Optional} if this
     * is an in-process container.
     *
     * @see #serverName()
     */
    public Optional<Integer> port() {
        return Optional.ofNullable(port);
    }

    /**
     * Obtains the name of the in-process server, or empty {@code Optinal} if the container is
     * exposed at a port.
     *
     * @see #port()
     */
    public Optional<String> serverName() {
        return Optional.ofNullable(serverName);
    }

    /**
     * Starts the service.
     *
     * @throws IOException
     *         if unable to bind
     */
    public void start() throws IOException {
        checkState(grpcServer == null, "gRPC server is started already.");
        grpcServer = createGrpcServer();
        grpcServer.start();
    }

    /**
     * Returns {@code true} if the server is shut down or was not started at all,
     * {@code false} otherwise.
     *
     * @see GrpcContainer#shutdown()
     */
    public boolean isShutdown() {
        boolean isShutdown = grpcServer == null;
        return isShutdown;
    }

    /**
     * Initiates an orderly shutdown in which existing calls continue but new calls are rejected.
     */
    public void shutdown() {
        checkState(grpcServer != null, SERVER_NOT_STARTED_MSG);
        grpcServer.shutdown();
        this.grpcServer = null;
    }

    /**
     * Initiates a forceful shutdown in which preexisting and new calls are rejected.
     *
     * <p>The method returns when the service becomes terminated.
     * The most common usage scenario for this method is clean-up in unit tests
     * (e.g. {@literal @}{@code AfterEach} in JUnit5) that involve gRPC communications.
     */
    @VisibleForTesting
    public void shutdownNowAndWait() {
        checkState(grpcServer != null, SERVER_NOT_STARTED_MSG);
        grpcServer.shutdownNow();
        awaitTermination();
        this.grpcServer = null;
    }

    /** Waits for the service to become terminated. */
    public void awaitTermination() {
        checkState(grpcServer != null, SERVER_NOT_STARTED_MSG);
        try {
            grpcServer.awaitTermination();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Check if the given gRPC service is scheduled for the deployment in this container.
     *
     * <p>Note that the given gRPC service will become available to the clients,
     * once the gRPC container is started.
     *
     * <p>To find out whether the service is already available for calls,
     * use {@link #isLive(BindableService)} method.
     *
     * @param service
     *         the gRPC service to check
     * @return {@code true}, if the given gRPC service for deployment and {@code false} otherwise
     */
    public boolean isScheduledForDeployment(BindableService service) {
        String nameOfInterest = service.bindService()
                                       .getServiceDescriptor()
                                       .getName();
        boolean serviceIsPresent = false;
        for (ServerServiceDefinition serverServiceDefinition : services) {
            String scheduledServiceName = serverServiceDefinition.getServiceDescriptor()
                                                                 .getName();
            serviceIsPresent = serviceIsPresent || scheduledServiceName.equals(nameOfInterest);
        }
        return serviceIsPresent;
    }

    /**
     * Check if the given gRPC service has actually been deployed and is available for interaction
     * within this container.
     *
     * <p>Returns {@code true} if and only if
     * <p>  a. the service has been previously scheduled for the deployment,
     * <p>  b. the container has been started.
     *
     * @param service
     *         the gRPC service
     * @return {@code true}, if the service is available for interaction within this container and
     *         {@code false} otherwise
     */
    public boolean isLive(BindableService service) {
        boolean inShutdownState = isShutdown();
        boolean scheduledForDeployment = isScheduledForDeployment(service);
        boolean result = !inShutdownState && scheduledForDeployment;
        return result;
    }

    /**
     * Makes the JVM shut down the service when it is shutting down itself.
     *
     * <p>Call this method when running the service in a separate JVM.
     */
    public void addShutdownHook() {
        Runtime.getRuntime()
               .addShutdownHook(new Thread(shutdownCallback()));
    }

    private Server createGrpcServer() {
        if (injectedServer != null) {
            return injectedServer;
        }
        ServerBuilder builder = createServerBuilder();
        for (ServerServiceDefinition service : services) {
            builder.addService(service);
        }
        return builder.build();
    }

    private ServerBuilder createServerBuilder() {
        ServerBuilder result =
                serverName == null
                ? ServerBuilder.forPort(checkNotNull(port))
                : InProcessServerBuilder.forName(serverName)
                                        .directExecutor();
        return result;
    }

    /**
     * Injects a server to this container.
     *
     * <p>All calls to {@link #createGrpcServer()} will resolve to the given server instance.
     *
     * <p>A test-only method.
     */
    @VisibleForTesting
    public void injectServer(Server server) {
        this.injectedServer = server;
    }

    @VisibleForTesting
    Runnable shutdownCallback() {
        return new ShutdownCallback();
    }

    @VisibleForTesting
    @Nullable Server grpcServer() {
        return grpcServer;
    }

    /**
     * Shuts down the container printing the status to {@code System.err}.
     *
     * <p>Stderr is used since the logger may have been reset already by its JVM shutdown hook.
     */
    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "CatchAndPrintStackTrace"}) // see Javadoc
    final class ShutdownCallback implements Runnable {

        private final String containerClass = GrpcContainer.class.getName();

        @Override
        public void run() {
            try {
                if (!isShutdown()) {
                    println("Shutting down `%s` since JVM is shutting down...", containerClass);
                    shutdown();
                    println("`%s` shut down.", containerClass);
                }
            } catch (RuntimeException e) {
                e.printStackTrace(System.err);
            }
        }

        @FormatMethod
        private void println(@FormatString String msgFormat, Object... arg) {
            String msg = format(msgFormat, arg);
            System.err.println(msg);
        }
    }

    /**
     * The builder for {@code GrpcContainer} allows to define a port and services exposed
     * by the container.
     */
    public static final class Builder extends ConnectionBuilder {

        private final Set<ServerServiceDefinition> services = Sets.newHashSet();

        private Builder(@Nullable Integer port, @Nullable String serverName) {
            super(port, serverName);
        }

        /**
         * Does nothing.
         *
         * @deprecated please use {@link GrpcContainer#atPort(int)}.
         */
        @Deprecated
        public Builder setPort(@SuppressWarnings("unused") int ignored) {
            return this;
        }

        /**
         * Obtains the port to be used by the container.
         *
         * @deprecated please use {@link #port()}.
         */
        @Deprecated
        public int getPort() {
            return port().orElse(0);
        }

        @CanIgnoreReturnValue
        public Builder addService(BindableService service) {
            services.add(service.bindService());
            return this;
        }

        @CanIgnoreReturnValue
        public Builder removeService(ServerServiceDefinition service) {
            services.remove(service);
            return this;
        }

        /**
         * Obtains the services already added to the builder.
         * @deprecated please use {@link #services()}.
         */
        @Deprecated
        public ImmutableSet<ServerServiceDefinition> getServices() {
            return services();
        }

        /**
         * Obtains the services already added to the builder.
         */
        public ImmutableSet<ServerServiceDefinition> services() {
            return ImmutableSet.copyOf(services);
        }

        public GrpcContainer build() {
            return new GrpcContainer(this);
        }
    }
}
