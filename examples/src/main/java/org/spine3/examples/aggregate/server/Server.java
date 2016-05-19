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
package org.spine3.examples.aggregate.server;

import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.client.grpc.ClientServiceGrpc;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;

import java.io.IOException;

import static org.spine3.examples.aggregate.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT;

/**
 * Sample gRPC server implementation.
 *
 * @author Mikhail Melnik
 * @author Alexander Litus
 */
public class Server {

    private final ServerApp application;

    private final io.grpc.Server grpcServer;

    /**
     * @param storageFactory the {@link StorageFactory} used to create and set up storages.
     */
    public Server(StorageFactory storageFactory) {
        this.application = new ServerApp(storageFactory);
        this.grpcServer = createGrpcServer(this.application.getBoundedContext(), DEFAULT_CLIENT_SERVICE_PORT);
    }

    private static io.grpc.Server createGrpcServer(ClientServiceGrpc.ClientService boundedContext, int port) {
        final ServerServiceDefinition service = ClientServiceGrpc.bindService(boundedContext);
        final ServerBuilder builder = ServerBuilder.forPort(port);
        builder.addService(service);
        return builder.build();
    }

    /**
     * The entry point of the server application.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();

        final Server server = new Server(storageFactory);
        server.start();
        server.awaitTermination();
    }

    /**
     * Starts the server.
     * @throws IOException if unable to bind.
     */
    public void start() throws IOException {
        application.setUp();
        grpcServer.start();
        addShutdownHook(this);
        log().info("Server started, listening to commands on the port " + DEFAULT_CLIENT_SERVICE_PORT);
    }

    /**
     * Stops the server.
     */
    public void stop() throws Exception {
        try {
            application.close();
        } catch (IOException e) {
            log().error("Error closing application", e);
        }
        grpcServer.shutdown();
    }

    /**
     * Waits for the server to become terminated.
     */
    public void awaitTermination() throws InterruptedException {
        grpcServer.awaitTermination();
    }

    private static void addShutdownHook(final Server server) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            @SuppressWarnings("UseOfSystemOutOrSystemErr")
            @Override
            public void run() {
                System.err.println("Shutting down " + getClass().getName() + "  since JVM is shutting down...");
                try {
                    server.stop();
                } catch (Exception e) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace(System.err);
                }
                System.err.println("Server shut down.");
            }
        }));
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(Server.class);
    }
}
