/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.sample.server;

import io.grpc.ServerImpl;
import io.grpc.stub.StreamObserver;
import io.grpc.transport.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.CommandStore;
import org.spine3.Engine;
import org.spine3.EventBus;
import org.spine3.EventStore;
import org.spine3.base.CommandRequest;
import org.spine3.base.CommandResult;
import org.spine3.base.CommandServiceGrpc;
import org.spine3.engine.Media;
import org.spine3.sample.order.OrderRootRepository;
import org.spine3.sample.store.filesystem.FileSystemMedia;

/**
 * Spine sample gRPC server implementation.
 *
 * @author Mikhail Melnik
 */
public class SpineSampleServer {

    private static final String STORAGE_PATH = "./storage";

    public static void registerEventSubscribers() {
        EventBus.instance().register(new SampleSubscriber());
    }

    public static void prepareEngine() {
        Media media = new FileSystemMedia(STORAGE_PATH);

        OrderRootRepository orderRootRepository = getOrderRootRepository(media);

        Engine engine = getEngine(media);
        engine.register(orderRootRepository);
    }

    public static OrderRootRepository getOrderRootRepository(Media media) {
        OrderRootRepository repository = new OrderRootRepository();
        repository.configure(media);
        return repository;
    }

    public static Engine getEngine(Media media) {
        CommandStore commandStore = new CommandStore(media);
        EventStore eventStore = new EventStore(media);
        Engine.configure(commandStore, eventStore);
        return Engine.getInstance();
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
