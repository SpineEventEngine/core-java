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

package org.spine3.examples.aggregate;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Command;
import org.spine3.base.Event;
import org.spine3.base.Response;
import org.spine3.base.UserId;
import org.spine3.client.grpc.ClientServiceGrpc;
import org.spine3.client.grpc.Topic;
import org.spine3.protobuf.Messages;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.spine3.client.UserUtil.newUserId;
import static org.spine3.examples.aggregate.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT;
import static org.spine3.examples.aggregate.Requests.*;
import static org.spine3.protobuf.Messages.toText;

/**
 * Sample of a client implementation.
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 * @author Alexander Litus
 */
public class Client {
    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static final String SERVICE_HOST = "localhost";

    private static final String RPC_FAILED = "RPC failed";
    private static final int SHUTDOWN_TIMEOUT_SEC = 5;

    private final Topic topic = Topic.getDefaultInstance();
    private final ManagedChannel channel;
    private final ClientServiceGrpc.ClientServiceBlockingClient blockingClient;
    private final ClientServiceGrpc.ClientServiceStub nonBlockingClient;

    private final StreamObserver<Event> observer = new StreamObserver<Event>() {
        @Override
        public void onNext(Event event) {
            final String eventText = Messages.toText(event.getMessage());
            log().info(eventText);
        }

        @Override
        public void onError(Throwable throwable) {
            log().error("Streaming error occurred", throwable);
        }

        @Override
        public void onCompleted() {
            log().info("Stream completed.");
        }
    };

    /**
     * Construct the client connecting to server at {@code host:port}.
     */
    public Client() {
        channel = ManagedChannelBuilder
                .forAddress(SERVICE_HOST, DEFAULT_CLIENT_SERVICE_PORT)
                .usePlaintext(true)
                .build();
        blockingClient = ClientServiceGrpc.newBlockingStub(channel);

        nonBlockingClient = ClientServiceGrpc.newStub(channel);
    }

    private void subscribe() {
        nonBlockingClient.subscribe(topic, observer);
    }

    /**
     * Sends requests to the server.
     */
    public static void main(String[] args) throws InterruptedException {
        final Client client = new Client();
        client.subscribe();

        final List<Command> requests = generateRequests();
        try {
            for (Command request : requests) {
                log().info("Sending a request: " + request.getMessage().getTypeUrl() + "...");
                final Response result = client.post(request);
                log().info("Result: " + toText(result));
            }

        } finally {
            client.shutdown();
        }
    }

    /**
     * Creates several test requests.
     */
    public static List<Command> generateRequests() {
        final List<Command> result = newLinkedList();

        for (int i = 0; i < 10; i++) {
            final OrderId orderId = OrderId.newBuilder().setValue(String.valueOf(i)).build();
            final UserId userId = newUserId("user_" + i);

            final Command createOrder = createOrder(userId, orderId);
            result.add(createOrder);
            final Command addOrderLine = addOrderLine(userId, orderId);
            result.add(addOrderLine);
            final Command payForOrder = payForOrder(userId, orderId);
            result.add(payForOrder);
        }
        return result;
    }

    /**
     * Shutdown the connection channel.
     * @throws InterruptedException if waiting is interrupted.
     */
    private void shutdown() throws InterruptedException {
        blockingClient.unsubscribe(topic);
        channel.shutdown().awaitTermination(SHUTDOWN_TIMEOUT_SEC, SECONDS);
    }

    /**
     * Sends a request to the server.
     */
    private Response post(Command request) {
        Response result = null;
        try {
            result = blockingClient.post(request);
        } catch (RuntimeException e) {
            log().warn(RPC_FAILED, e);
        }
        return result;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(Client.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
