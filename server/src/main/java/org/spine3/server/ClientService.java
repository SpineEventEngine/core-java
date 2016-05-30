/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Command;
import org.spine3.base.Event;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.client.ConnectionConstants;
import org.spine3.client.grpc.ClientServiceGrpc;
import org.spine3.client.grpc.Topic;
import org.spine3.server.command.error.CommandException;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.server.type.CommandClass;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

/**
 * The {@code ClientService} allows client applications to post commands and
 * receive updates from the application backend.
 *
 * @author Alexander Yevsyukov
 */
public class ClientService implements org.spine3.client.grpc.ClientServiceGrpc.ClientService {

    private final ImmutableMap<CommandClass, BoundedContext> commandToBoundedContext;
    private final int port;
    @Nullable
    private io.grpc.Server grpcServer;

    public static Builder newBuilder() {
        return new Builder();
    }

    protected ClientService(Builder builder) {
        this.port = builder.getPort();
        this.commandToBoundedContext = builder.commandToBoundedContextMap().build();
    }

    /**
     * Starts the service.
     *
     * @throws IOException if unable to bind
     */
    public void start() throws IOException {
        checkState(grpcServer == null, "Already started");
        grpcServer = createGrpcServer(this, this.port);
        grpcServer.start();
    }

    /**
     * Waits for the service to become terminated.
     */
    public void awaitTermination() throws InterruptedException {
        checkState(grpcServer != null);
        grpcServer.awaitTermination();
    }

    /**
     * Makes the JVM shut down the service when it is shutting down itself.
     *
     * <p>Call this method when running the service in a separate JVM.
     */
    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            @SuppressWarnings("UseOfSystemOutOrSystemErr")
            @Override
            public void run() {
                final String serverClass = ClientService.this.getClass().getName();
                System.err.println("Shutting down " + serverClass + " since JVM is shutting down...");
                try {
                    if (!isShutdown()) {
                        shutdown();
                    }
                } catch (RuntimeException e) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace(System.err);
                }
                System.err.println(serverClass + " shut down.");
            }
        }));
    }

    @VisibleForTesting
    /* package */ boolean isShutdown() {
        return grpcServer == null;
    }

    /**
     * Shuts the service down after completing queued requests.
     */
    public void shutdown() {
        checkState(grpcServer != null);
        grpcServer.shutdown();
        grpcServer = null;
    }

    private static io.grpc.Server createGrpcServer(ClientServiceGrpc.ClientService clientService, int port) {
        final ServerServiceDefinition service = ClientServiceGrpc.bindService(clientService);
        final ServerBuilder builder = ServerBuilder.forPort(port)
                                                   .addService(service);
        return builder.build();
    }

    @Override
    public void post(Command request, StreamObserver<Response> responseObserver) {
        final CommandClass commandClass = CommandClass.of(request);
        final BoundedContext boundedContext = commandToBoundedContext.get(commandClass);
        if (boundedContext == null) {
            handleUnsupported(request, responseObserver);
            return;
        }
        boundedContext.getCommandBus().post(request, responseObserver);
    }

    private static void handleUnsupported(Command request, StreamObserver<Response> responseObserver) {
        final CommandException unsupported = new UnsupportedCommandException(request);
        log().error("Unsupported command posted to ClientService", unsupported);
        responseObserver.onError(Statuses.invalidArgumentWithCause(unsupported));
    }

    @Override
    public void subscribe(Topic request, StreamObserver<Event> responseObserver) {
        //TODO:2016-05-25:alexander.yevsyukov: Subscribe the client to the topic in the corresponding BoundedContext.
        // This API is likely to change to support Firebase-like registration where listening is
        // done by the client SDK implementation.
    }

    @Override
    public void unsubscribe(Topic request, StreamObserver<Response> responseObserver) {
        //TODO:2016-05-25:alexander.yevsyukov: Unsubscribe the client from the topic in the corresponding BoundedContext.
        responseObserver.onNext(Responses.ok());
        responseObserver.onCompleted();
    }

    public static class Builder {
        private final Set<BoundedContext> boundedContexts = Sets.newHashSet();
        private int port = ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT;

        public Builder addBoundedContext(BoundedContext boundedContext) {
            boundedContexts.add(boundedContext);
            return this;
        }

        public Builder removeBoundedContext(BoundedContext boundedContext) {
            boundedContexts.remove(boundedContext);
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public int getPort() {
            return this.port;
        }

        public ClientService build() {
            final ClientService result = new ClientService(this);
            return result;
        }

        private ImmutableMap.Builder<CommandClass, BoundedContext> commandToBoundedContextMap() {
            final ImmutableMap.Builder<CommandClass, BoundedContext> mapBuilder = ImmutableMap.builder();
            for (BoundedContext boundedContext : boundedContexts) {
                addBoundedContext(mapBuilder, boundedContext);
            }
            return mapBuilder;
        }

        private static void addBoundedContext(ImmutableMap.Builder<CommandClass, BoundedContext> mapBuilder,
                                              BoundedContext boundedContext) {
            for (CommandClass commandClass : boundedContext.getCommandBus().getSupportedCommandClasses()) {
                mapBuilder.put(commandClass, boundedContext);
            }
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ClientService.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
