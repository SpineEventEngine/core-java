/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
package org.spine3.sample.server;

import io.grpc.ServerImpl;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.CommandRequest;
import org.spine3.base.CommandResult;
import org.spine3.base.CommandServiceGrpc;
import org.spine3.sample.Application;
import org.spine3.server.Engine;
import org.spine3.server.storage.StorageFactory;

import java.io.IOException;

/**
 * Sample gRPC server implementation.
 *
 * @author Mikhail Melnik
 * @author Alexander Litus
 */
public class Server {

    /**
     * The default port on which the server runs.
     */
    public static final int SERVER_PORT = 50051;

    private final ServerImpl serverImpl;
    private final Application application;

    /**
     * @param serverPort the port on which the server should run.
     * @param storageFactory the {@link StorageFactory} used to create and set up storages.
     */
    public Server(int serverPort, StorageFactory storageFactory) {

        this.serverImpl = buildServerImpl(serverPort);
        this.application = new Application(storageFactory);
    }

    /**
     * The entry point of the sample.
     * To change the storage implementation, change {@link Application#getStorageFactory()} method implementation.
     */
    public static void main(String[] args) throws IOException {

        final StorageFactory storageFactory = Application.getStorageFactory();

        final Server server = new Server(SERVER_PORT, storageFactory);

        server.start();

        log().info("Server started, listening on the port " + SERVER_PORT);

        addShutdownHook(server);
    }

    /**
     * Starts the server.
     * @throws IOException if unable to bind.
     */
    public void start() throws IOException {

        application.setUp();
        serverImpl.start();
    }

    /**
     * Stops the server.
     */
    public void stop() {

        application.tearDown();
        serverImpl.shutdown();
    }

    private static void addShutdownHook(final Server server) {

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            @SuppressWarnings("UseOfSystemOutOrSystemErr")
            @Override
            public void run() {
                System.err.println("Shutting down the gRPC server since JVM is shutting down...");
                server.stop();
                System.err.println("Server shut down.");
            }
        }));
    }

    private static class CommandServiceImpl implements CommandServiceGrpc.CommandService {

        @Override
        public void handle(CommandRequest req, StreamObserver<CommandResult> responseObserver) {
            final CommandResult reply = Engine.getInstance().process(req);

            responseObserver.onValue(reply);
            responseObserver.onCompleted();
        }

        @Override
        public StreamObserver<CommandRequest> handleStream(StreamObserver<CommandResult> responseObserver) {
            //TODO:2015-06-25:mikhail.melnik: implement
            return null;
        }
    }

    private static ServerImpl buildServerImpl(int serverPort) {
        final NettyServerBuilder builder = NettyServerBuilder.forPort(serverPort);
        final ServerServiceDefinition service = CommandServiceGrpc.bindService(new CommandServiceImpl());
        builder.addService(service);
        return builder.build();
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
