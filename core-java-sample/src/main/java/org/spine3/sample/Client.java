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
import org.spine3.base.CommandResult;
import org.spine3.client.CommandRequest;
import org.spine3.client.CommandServiceGrpc;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.spine3.protobuf.Messages.toText;
import static org.spine3.sample.server.Server.SERVER_PORT;

/**
 * Sample gRPC client implementation.
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 * @author Alexander Litus
 */
public class Client {

    private static final String LOCALHOST = "localhost";
    private static final String RPC_FAILED = "RPC failed";
    private static final int SHUTDOWN_TIMEOUT_SEC = 5;

    private final ManagedChannel channel;
    private final CommandServiceGrpc.CommandServiceBlockingStub blockingStub;

    //TODO:2015-12-15:alexander.yevsyukov: Why do we pass String instead of URL?
    //TODO:2015-12-15:alexander.yevsyukov: Isn't Client a part of client-side API framework customers would use?

    /**
     * Construct the client connecting to server at {@code host:port}.
     */
    public Client(String host, int port) {
        channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext(true)
                .build();
        blockingStub = CommandServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Sends requests to the server.
     */
    public static void main(String[] args) throws InterruptedException {
        // Access a service running on the local machine
        final Client client = new Client(LOCALHOST, SERVER_PORT);

        final List<CommandRequest> requests = Application.generateRequests();
        try {
            for (CommandRequest request : requests) {
                log().info("Sending a request: " + request.getCommand().getTypeUrl() + "...");
                final CommandResult result = client.send(request);
                log().info("Result: " + toText(result));
            }
        } finally {
            client.shutdown();
        }
    }

    /**
     * Shutdown the connection channel.
     * @throws InterruptedException if waiting is interrupted.
     */
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(SHUTDOWN_TIMEOUT_SEC, SECONDS);
    }

    /**
     * Sends a request to the server.
     */
    public CommandResult send(CommandRequest request) {

        CommandResult result = null;
        try {
            result = blockingStub.handle(request);
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
