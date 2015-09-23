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

package org.spine3.sample;

import io.grpc.ChannelImpl;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.CommandRequest;
import org.spine3.base.CommandResult;
import org.spine3.base.CommandServiceGrpc;
import org.spine3.base.UserId;
import org.spine3.protobuf.Messages;
import org.spine3.sample.order.OrderId;
import org.spine3.util.Users;

import java.util.concurrent.TimeUnit;

//TODO:2015-09-23:alexander.yevsyukov: Eliminate code duplication with BaseSample.

/**
 * Sample gRPC client implementation. Can be used with {@link org.spine3.sample.server.DataStoreSampleServer} and
 * {@link org.spine3.sample.server.FileSystemSampleServer}.
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 */
public class SampleClient {

    private static final int PORT = 50051;
    private final ChannelImpl channel;
    private final CommandServiceGrpc.CommandServiceBlockingStub blockingStub;

    private static final String RPC_FAILED = "RPC failed";
    private static final String USER = "User ";
    private static final String RESULT = "Result: ";

    /**
     * Construct the client connecting to server at {@code host:port}.
     */
    public SampleClient(String host, int port) {
        channel = NettyChannelBuilder
                .forAddress(host, port)
                .negotiationType(NegotiationType.PLAINTEXT)
                .build();
        blockingStub = CommandServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    private void createOrder(UserId userId, OrderId orderId) {
        try {
            log().info(USER + userId + " tries to create new order " + orderId + " ...");
            CommandRequest request = Requests.createOrder(userId, orderId);

            CommandResult result = blockingStub.handle(request);
            log().info(RESULT + Messages.toText(result));
        } catch (RuntimeException e) {
            log().warn(RPC_FAILED, e);
        }
    }

    private void addOrderLine(UserId userId, OrderId orderId) {
        try {
            log().info(USER + userId + " tries to add new order line in order " + orderId + " ...");
            CommandRequest request = Requests.addOrderLine(userId, orderId);

            CommandResult result = blockingStub.handle(request);
            log().info(RESULT + Messages.toText(result));
        } catch (RuntimeException e) {
            log().warn(RPC_FAILED, e);
        }
    }

    private void payOrder(UserId userId, OrderId orderId) {
        try {
            log().info(USER + userId + " tries to pay money for order " + orderId + " ...");
            CommandRequest request = Requests.payForOrder(userId, orderId);

            CommandResult result = blockingStub.handle(request);
            log().info(RESULT + Messages.toText(result));
        } catch (RuntimeException e) {
            log().warn(RPC_FAILED, e);
        }
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting.
     */
    public static void main(String[] args) throws InterruptedException {

        /* Access a service running on the local machine on port 50051 */
        SampleClient client = new SampleClient("localhost", PORT);

        try {

            for (int i = 0; i < 10; i++) {
                OrderId orderId = OrderId.newBuilder().setValue(String.valueOf(i)).build();
                UserId userId = Users.createId("user" + i);

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
        private final Logger value = LoggerFactory.getLogger(SampleClient.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
