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
import org.spine3.sample.Sample;
import org.spine3.server.Engine;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;

import java.io.IOException;

/**
 * Sample gRPC server implementation.
 *
 * @author Mikhail Melnik
 */
@SuppressWarnings("UtilityClass")
public class SampleServer {

    /**
     * The port on which the server runs.
     */
    public static final int SERVER_PORT = 50051;

    private static final ServerImpl SERVER = buildServer(SERVER_PORT);

    private static final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();

    /**
     * To run the sample on a FileSystemStorageFactory, replace the above initialization with the following:
     *
     * StorageFactory storageFactory = org.spine3.server.storage.filesystem.FileSystemStorageFactory.newInstance(MySampleClass.class);
     *
     *
     * To run the sample on a LocalDatastoreStorageFactory, replace the above initialization with the following:
     *
     * StorageFactory storageFactory = org.spine3.server.storage.datastore.LocalDatastoreStorageFactory.instance();
     */

    public static void main(String[] args) throws IOException {

        Sample.setUp(storageFactory);

        SERVER.start();

        log().info("Server started, listening on " + SERVER_PORT);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @SuppressWarnings("UseOfSystemOutOrSystemErr")
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                SampleServer.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private static void stop() {
        Sample.tearDown(storageFactory);
        SERVER.shutdown();
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

    private static ServerImpl buildServer(int serverPort) {
        final NettyServerBuilder builder = NettyServerBuilder.forPort(serverPort);
        final ServerServiceDefinition service = CommandServiceGrpc.bindService(new CommandServiceImpl());
        builder.addService(service);
        return builder.build();
    }

    private SampleServer() {
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(SampleServer.class);
    }
}
