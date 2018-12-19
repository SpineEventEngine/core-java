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
package io.spine.server.transport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.spine.client.ConnectionConstants;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

/**
 * Wrapping container for gRPC server.
 *
 * <p>Maintains and deploys several of gRPC services within a single server.
 *
 * <p>Uses {@link ServerServiceDefinition}s of each service.
 */
public class GrpcContainer {

    private static final String SERVER_NOT_STARTED_MSG =
            "gRPC server was not started or is shut down already.";

    private final int port;
    private final ImmutableSet<ServerServiceDefinition> services;

    private @Nullable Server grpcServer;

    public static Builder newBuilder() {
        return new Builder();
    }

    protected GrpcContainer(Builder builder) {
        this.port = builder.getPort();
        this.services = builder.getServices();
    }

    /**
     * Starts the service.
     *
     * @throws IOException if unable to bind
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
     * <p>Note, that the given gRPC service will become available to the clients,
     * once the gRPC container is started.
     *
     * <p>To find out, whether the service is already available for calls,
     * use {@link #isLive(BindableService)} method.
     *
     * @param service the gRPC service to check
     * @return {@code true}, if the given gRPC service for deployment; {@code false} otherwise
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
     * @param service the gRPC service
     * @return {@code true}, if the service is available for interaction within this container;
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
               .addShutdownHook(new Thread(getOnShutdownCallback()));
    }

    @VisibleForTesting
    Server createGrpcServer() {
        ServerBuilder builder = ServerBuilder.forPort(port);
        for (ServerServiceDefinition service : services) {
            builder.addService(service);
        }

        return builder.build();
    }

    // Use stderr here since the logger may have been reset by its JVM shutdown hook.
    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "CatchAndPrintStackTrace"})
    @VisibleForTesting
    Runnable getOnShutdownCallback() {
        return () -> {
            String serverClass = getClass().getName();
            try {
                if (!isShutdown()) {
                    System.err.println("Shutting down " +
                                       serverClass + " since JVM is shutting down...");
                    shutdown();
                    System.err.println(serverClass + " shut down.");
                }
            } catch (RuntimeException e) {
                e.printStackTrace(System.err);
            }
        };
    }

    public static class Builder {

        private int port = ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT;
        private final Set<ServerServiceDefinition> serviceDefinitions = Sets.newHashSet();

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public int getPort() {
            return this.port;
        }

        @CanIgnoreReturnValue
        public Builder addService(BindableService service) {
            serviceDefinitions.add(service.bindService());
            return this;
        }

        @CanIgnoreReturnValue
        public Builder removeService(ServerServiceDefinition service) {
            serviceDefinitions.remove(service);
            return this;
        }

        public ImmutableSet<ServerServiceDefinition> getServices() {
            return ImmutableSet.copyOf(serviceDefinitions);
        }

        public GrpcContainer build() {
            return new GrpcContainer(this);
        }
    }
}
