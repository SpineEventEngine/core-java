/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.sample;

import io.grpc.ChannelImpl;
import io.grpc.transport.netty.NegotiationType;
import io.grpc.transport.netty.NettyChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.CommandRequest;
import org.spine3.base.CommandResult;
import org.spine3.base.CommandServiceGrpc;
import org.spine3.base.UserId;
import org.spine3.sample.order.OrderId;
import org.spine3.util.Messages;
import org.spine3.util.UserIds;

import java.util.concurrent.TimeUnit;

/**
 * Spine sample gRPC client implementation.
 *
 * @author Mikhail Melnik
 */
public class SpineSampleClient {

    private final ChannelImpl channel;
    private final CommandServiceGrpc.CommandServiceBlockingStub blockingStub;

    /**
     * Construct client connecting to HelloWorld server at {@code host:port}.
     */
    public SpineSampleClient(String host, int port) {
        channel = NettyChannelBuilder
                .forAddress(host, port)
                .negotiationType(NegotiationType.PLAINTEXT)
                .build();
        blockingStub = CommandServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTerminated(5, TimeUnit.SECONDS);
    }

    private void createOrder(UserId userId, OrderId orderId) {
        try {
            log().info("User " + userId + " tries to create new order " + orderId + " ...");
            CommandRequest request = SampleRequests.createOrder(userId, orderId);

            CommandResult result = blockingStub.handle(request);
            log().info("Result: " + Messages.toText(result));
        } catch (RuntimeException e) {
            log().warn("RPC failed", e);
            return;
        }
    }

    private void addOrderLine(UserId userId, OrderId orderId) {
        try {
            log().info("User " + userId + " tries to add new order line in order " + orderId + " ...");
            CommandRequest request = SampleRequests.addOrderLine(userId, orderId);

            CommandResult result = blockingStub.handle(request);
            log().info("Result: " + Messages.toText(result));
        } catch (RuntimeException e) {
            log().warn("RPC failed", e);
            return;
        }
    }

    private void payOrder(UserId userId, OrderId orderId) {
        try {
            log().info("User " + userId + " tries to pay money for order " + orderId + " ...");
            CommandRequest request = SampleRequests.payOrder(userId, orderId);

            CommandResult result = blockingStub.handle(request);
            log().info("Result: " + Messages.toText(result));
        } catch (RuntimeException e) {
            log().warn("RPC failed", e);
            return;
        }
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting.
     */
    public static void main(String[] args) throws Exception {

        /* Access a service running on the local machine on port 50051 */
        SpineSampleClient client = new SpineSampleClient("localhost", 50051);

        try {

            for (int i = 0; i < 10; i++) {
                OrderId orderId = OrderId.newBuilder().setValue(String.valueOf(i)).build();
                UserId userId = UserIds.create("user" + i);

                client.createOrder(userId, orderId);
                client.addOrderLine(userId, orderId);
                client.payOrder(userId, orderId);
            }

        } finally {
            client.shutdown();
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(SpineSampleClient.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
