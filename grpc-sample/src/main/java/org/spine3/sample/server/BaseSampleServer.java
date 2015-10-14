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
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.spine3.base.CommandRequest;
import org.spine3.base.CommandResult;
import org.spine3.base.CommandServiceGrpc;
import org.spine3.server.Engine;
import org.spine3.server.storage.StorageFactory;

import java.io.IOException;

import static org.spine3.sample.BaseSample.setupEnvironment;

/**
 * Sample gRPC server implementation.
 *
 * @author Mikhail Melnik
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class BaseSampleServer {

    public static final int SERVER_PORT = 50051;

    private ServerImpl server;
    private final StorageFactory storageFactory;
    private final Logger logger;

    protected BaseSampleServer(StorageFactory storageFactory, Logger logger) {
        this.storageFactory = storageFactory;
        this.logger = logger;
    }

    protected void start() throws IOException {

        setupEnvironment(storageFactory);

        server = NettyServerBuilder.forPort(SERVER_PORT)
                .addService(CommandServiceGrpc.bindService(new CommandServiceImpl()))
                .build()
                .start();

        logger.info("Server started, listening on " + SERVER_PORT);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                BaseSampleServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    protected void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private static class CommandServiceImpl implements CommandServiceGrpc.CommandService {

        @Override
        public void handle(CommandRequest req, StreamObserver<CommandResult> responseObserver) {
            CommandResult reply = Engine.getInstance().process(req);

            responseObserver.onValue(reply);
            responseObserver.onCompleted();
        }

        @Override
        public StreamObserver<CommandRequest> handleStream(StreamObserver<CommandResult> responseObserver) {
            //TODO:2015-06-25:mikhail.melnik: implement
            return null;
        }
    }
}
