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
import org.spine3.base.EventRecord;
import org.spine3.eventbus.EventBus;
import org.spine3.sample.EventLogger;
import org.spine3.sample.order.OrderRepository;
import org.spine3.server.CommandDispatcher;
import org.spine3.server.Engine;
import org.spine3.server.MessageJournal;
import org.spine3.server.aggregate.AggregateEventStorage;
import org.spine3.server.aggregate.SnapshotStorage;
import org.spine3.server.storage.StorageFactory;

/**
 * Sample gRPC server implementation.
 *
 * @author Mikhail Melnik
 */
public abstract class BaseSampleServer {

    public void registerEventSubscribers() {
        EventBus.getInstance().register(new EventLogger());
    }

    public void prepareEngine() {

        final OrderRepository orderRootRepository = getOrderRootRepository();

        Engine.start(getStorageFactory());
        CommandDispatcher.getInstance().register(orderRootRepository);
    }

    private OrderRepository getOrderRootRepository() {

        final AggregateEventStorage eventStore = new AggregateEventStorage(
                provideEventStoreStorage()
        );

        final OrderRepository repository = new OrderRepository(eventStore, provideSnapshotStorage());
        return repository;
    }

    /* The port on which the server should run */
    private int port = 50051;
    private ServerImpl server;

    protected void start() throws Exception {

        prepareEngine();
        registerEventSubscribers();

        server = NettyServerBuilder.forPort(port)
                .addService(CommandServiceGrpc.bindService(new CommandServiceImpl()))
                .build()
                .start();

        getLog().info("Server started, listening on " + port);

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
            StreamObserver<CommandRequest> o = null;
            //TODO:2015-06-25:mikhail.melnik: implement
            return o;
        }
    }

    //TODO:2015-09-21:alexander.yevsyukov: We have such factory methods already in BaseSample. Why duplicate?

    protected abstract StorageFactory getStorageFactory();

    protected abstract Logger getLog();

    protected abstract MessageJournal<String, EventRecord> provideEventStoreStorage();

    protected abstract MessageJournal<String, CommandRequest> provideCommandStoreStorage();

    protected abstract SnapshotStorage provideSnapshotStorage();
}
