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
import io.grpc.stub.StreamObserver;
import io.grpc.transport.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.*;
import org.spine3.base.*;
import org.spine3.gae.datastore.DataStoreStorageProvider;
import org.spine3.sample.order.OrderRoot;
import org.spine3.sample.order.OrderRootRepository;
import org.spine3.util.ClassName;

/**
 * Spine sample gRPC server implementation.
 *
 * @author Mikhail Melnik
 */
public class SpineSampleServer {

    public static void registerEventSubscribers() {
        EventBus.instance().register(new SampleSubscriber());
    }

    public static void prepareEngine() {
        final GlobalEventStore globalEventStore = new GlobalEventStore(DataStoreStorageProvider.provideEventStoreStorage());
        final CommandStore commandStore = new CommandStore(DataStoreStorageProvider.provideCommandStoreStorage());

        final OrderRootRepository orderRootRepository = getOrderRootRepository();

        Engine.configure(commandStore, globalEventStore);
        final Engine engine = Engine.getInstance();
        engine.register(orderRootRepository);
    }

    public static OrderRootRepository getOrderRootRepository() {
        final ClassName aggregateRootClass = ClassName.of(OrderRoot.class);
        final EventStore eventStore = new EventStore(
                DataStoreStorageProvider.provideEventStoreStorage(),
                DataStoreStorageProvider.provideSnapshotStorage(aggregateRootClass));

        final OrderRootRepository repository = new OrderRootRepository();
        repository.configure(eventStore);
        return repository;
    }

    /* The port on which the server should run */
    private int port = 50051;
    private ServerImpl server;

    private void start() throws Exception {

        prepareEngine();
        registerEventSubscribers();

        server = NettyServerBuilder.forPort(port)
                .addService(CommandServiceGrpc.bindService(new CommandServiceImpl()))
                .build()
                .start();

        log().info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                SpineSampleServer.this.stop();
                System.err.println("*** server shut down");
            }
        });

    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws Exception {
        final SpineSampleServer server = new SpineSampleServer();
        server.start();
    }

    private static class CommandServiceImpl implements CommandServiceGrpc.CommandService {
        @Override
        public void handle(CommandRequest req, StreamObserver<CommandResult> responseObserver) {
            CommandResult reply = Engine.getInstance().handle(req);

            responseObserver.onValue(reply);
            responseObserver.onCompleted();
        }

        @Override
        public StreamObserver<CommandRequest> handleStream(StreamObserver<CommandResult> responseObserver) {
            StreamObserver<CommandRequest> o = null;
            //TODO:2015-06-25:mikhail.melnik: implement
            return o;
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(SpineSampleServer.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
