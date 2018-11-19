/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.testing.client.grpc;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.spine.base.CommandMessage;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.grpc.CommandServiceGrpc;
import io.spine.client.grpc.CommandServiceGrpc.CommandServiceBlockingStub;
import io.spine.client.grpc.QueryServiceGrpc;
import io.spine.client.grpc.QueryServiceGrpc.QueryServiceBlockingStub;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.UserId;
import io.spine.logging.Logging;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.type.TypeName;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * {@link TestClient} connects to the server using gRPC and allows sending commands and
 * querying its entities.
 */
public class TestClient implements Logging {

    private static final String RPC_FAILED = "The command could not be posted.";
    private final TestActorRequestFactory requestFactory;
    private final ManagedChannel channel;
    private final CommandServiceBlockingStub commandClient;
    private final QueryServiceBlockingStub queryClient;

    /**
     * Construct the client connecting to server at {@code host:port}.
     */
    public TestClient(UserId userId, String host, int port) {
        checkNotNull(userId);
        checkNotNull(host);
        this.requestFactory = TestActorRequestFactory.newInstance(userId);
        this.channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        this.commandClient = CommandServiceGrpc.newBlockingStub(channel);
        this.queryClient = QueryServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Creates a command for the passed message and sends it to the server.
     */
    @CanIgnoreReturnValue
    public Optional<Ack> post(CommandMessage domainCommand) {
        Command command = requestFactory.command()
                                        .create(domainCommand);
        TypeName commandType = TypeName.of(domainCommand);
        Ack result = null;
        try {
            _debug("Sending command: {} ...", commandType);
            result = commandClient.post(command);
            _debug("Ack: {}", result.toString());
        } catch (RuntimeException e) {
            _warn(RPC_FAILED, e);
        }
        return Optional.ofNullable(result);
    }

    /**
     * Queries all entities of provided type.
     *
     * @param messageType an entity type to query
     * @return query response with the state of entities obtained from the server
     */
    public QueryResponse queryAll(Class<? extends Message> messageType) {
        Query query = requestFactory.query()
                                    .all(messageType);
        QueryResponse result = queryClient.read(query);
        return result;
    }

    /**
     * Shutdown the client waiting 5 seconds for preexisting calls to continue.
     *
     * @throws InterruptedException if waiting is interrupted.
     */
    public void shutdown() throws InterruptedException {
        if (!channel.isShutdown()) {
            channel.shutdown()
                   .awaitTermination(5, SECONDS);
        }
    }

    /**
     * Verifies if the client is shutdown.
     */
    public boolean isShutdown() {
        return channel.isShutdown();
    }
}
