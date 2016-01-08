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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.EventRecord;
import org.spine3.base.Response;
import org.spine3.base.UserId;
import org.spine3.client.*;
import org.spine3.client.grpc.ClientServiceGrpc;
import org.spine3.protobuf.Messages;
import org.spine3.sample.order.OrderId;
import org.spine3.util.Identifiers;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.spine3.protobuf.Messages.toText;
import static org.spine3.sample.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT;
import static org.spine3.sample.Requests.*;
import static org.spine3.util.Users.newUserId;

/**
 * Sample of a client implementation.
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 * @author Alexander Litus
 */
public class Client {

    private static final String LOCALHOST = "localhost";
    private static final String CLIENT_SERVICE_HOST = LOCALHOST;

    private static final String RPC_FAILED = "RPC failed";
    private static final int SHUTDOWN_TIMEOUT_SEC = 5;

    private final ManagedChannel channel;
    private final ClientServiceGrpc.ClientServiceBlockingClient client;
    private final Connection connection;

    /**
     * Construct the client connecting to server at {@code host:port}.
     */
    public Client() {
        channel = ManagedChannelBuilder
                .forAddress(CLIENT_SERVICE_HOST, DEFAULT_CLIENT_SERVICE_PORT)
                .usePlaintext(true)
                .build();
        client = ClientServiceGrpc.newBlockingStub(channel);


        final ClientRequest request = ClientRequest.newBuilder()
                .setId(ClientId.newBuilder()
                    .setValue(Identifiers.newUuid()))

                .setDevice(DeviceType.SERVICE)

                .setVersion(CodeVersion.newBuilder()
                        .setMajor(0)
                        .setMinor(2)
                        .setPatchLevel(0))

                .setOs(OsInfo.getDefaultInstance())
                    //TODO:2015-12-16:alexander.yevsyukov: Create utility method for builing OS info from Java API.
                .build();

        connection = client.connect(request);
    }

    /**
     * Sends requests to the server.
     */
    public static void main(String[] args) throws InterruptedException {
        // Access a service running on the local machine
        final Client client = new Client();

        final List<CommandRequest> requests = generateRequests();
        try {
            for (CommandRequest request : requests) {
                log().info("Sending a request: " + request.getCommand().getTypeUrl() + "...");
                final Response result = client.send(request);
                log().info("Result: " + toText(result));
            }

            client.readEvents();

        } finally {
            client.shutdown();
        }
    }

    /**
     * Creates several test requests.
     */
    public static List<CommandRequest> generateRequests() {
        final List<CommandRequest> result = newArrayList();

        for (int i = 0; i < 10; i++) {
            final OrderId orderId = OrderId.newBuilder().setValue(String.valueOf(i)).build();
            final UserId userId = newUserId("user_" + i);

            final CommandRequest createOrder = createOrder(userId, orderId);
            result.add(createOrder);
            final CommandRequest addOrderLine = addOrderLine(userId, orderId);
            result.add(addOrderLine);
            final CommandRequest payForOrder = payForOrder(userId, orderId);
            result.add(payForOrder);
        }
        return result;
    }

    /**
     * Shutdown the connection channel.
     * @throws InterruptedException if waiting is interrupted.
     */
    private void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(SHUTDOWN_TIMEOUT_SEC, SECONDS);
    }

    /**
     * Sends a request to the server.
     */
    private Response send(CommandRequest request) {
        Response result = null;
        try {
            result = client.post(request);
        } catch (RuntimeException e) {
            log().warn(RPC_FAILED, e);
        }
        return result;
    }

    private void readEvents() {
        final Iterator<EventRecord> events = client.getEvents(connection);

        while (events.hasNext()) {
            final EventRecord record = events.next();
            final String eventText = Messages.toText(record.getEvent());
            log().info(eventText);
        }

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
