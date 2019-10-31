/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.client;

import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.spine.client.grpc.CommandServiceGrpc;
import io.spine.client.grpc.CommandServiceGrpc.CommandServiceBlockingStub;
import io.spine.client.grpc.QueryServiceGrpc;
import io.spine.client.grpc.QueryServiceGrpc.QueryServiceBlockingStub;
import io.spine.client.grpc.SubscriptionServiceGrpc;
import io.spine.client.grpc.SubscriptionServiceGrpc.SubscriptionServiceBlockingStub;
import io.spine.client.grpc.SubscriptionServiceGrpc.SubscriptionServiceStub;
import io.spine.core.Command;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.protobuf.AnyPacker;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.Messages.isDefault;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static io.spine.util.Preconditions2.checkNotDefaultArg;
import static io.spine.util.Preconditions2.checkNotEmptyOrBlank;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

/**
 * The gRPC-based gateway for backend services such as {@code CommandService} or
 * {@code QueryService}.
 */
public class Client implements AutoCloseable {

    /** The number of seconds to wait when {@linkplain #close() closing} the client. */
    private static final int TIMEOUT = 10;

    private final ManagedChannel channel;
    private final @Nullable TenantId tenant;
    private final QueryServiceBlockingStub queryService;
    private final CommandServiceBlockingStub commandService;
    private final SubscriptionServiceStub subscriptionService;
    private final SubscriptionServiceBlockingStub blockingSubscriptionService;
    private final UserId guestUser;

    /**
     * Creates a builder for a client connected to the specified address.
     *
     * <p>The returned builder will create {@code ManagedChannel} with the default configuration.
     * For a channel with custom configuration please use {@link #usingChannel(ManagedChannel)}.
     *
     * @see #usingChannel(ManagedChannel)
     */
    public static Builder connectTo(String host, int port) {
        checkNotEmptyOrBlank(host);
        return new Builder(host, port);
    }

    /**
     * Creates a builder for a client which will use the passed channel for the communication
     * with the backend services.
     *
     * <p>Use this method when a channel with custom configuration is needed for your client
     * application.
     *
     * @see #connectTo(String, int)
     * @see ManagedChannel
     */
    public static Builder usingChannel(ManagedChannel channel) {
        checkNotNull(channel);
        return new Builder(channel);
    }

    /**
     * Creates a new client which uses the passed channel for communications
     * with the backend services.
     */
    private Client(Builder builder) {
        this.channel = checkNotNull(builder.channel);
        this.tenant = builder.tenant;
        this.commandService = CommandServiceGrpc.newBlockingStub(channel);
        this.queryService = QueryServiceGrpc.newBlockingStub(channel);
        this.subscriptionService = SubscriptionServiceGrpc.newStub(channel);
        this.blockingSubscriptionService = SubscriptionServiceGrpc.newBlockingStub(channel);
        this.guestUser = builder.guestUser;
    }

    /**
     * Obtains the tenant of this client connection in a multitenant application,
     * and empty {@code Optional} in a single-tenant one.
     */
    public Optional<TenantId> tenant() {
        return Optional.ofNullable(tenant);
    }

    /**
     * Obtains the ID of the user for performing requests on behalf of the user
     * who is not logged in yet.
     */
    private UserId guestUser() {
        return guestUser;
    }

    /**
     * Closes the client by shutting down the gRPC connection.
     */
    @Override
    public void close() {
        try {
            channel.shutdown()
                   .awaitTermination(TIMEOUT, SECONDS);
        } catch (InterruptedException e) {
            throw illegalStateWithCauseOf(e);
        }
    }

    /**
     * Creates a new request factory for the requests to be sent on behalf of the passed user.
     */
    ActorRequestFactory requestOf(UserId user) {
        return ActorRequestFactory
                .newBuilder()
                .setTenantId(tenant)
                .setActor(user)
                .build();
    }

    /**
     * Creates a builder for requests on behalf of the passed user.
     */
    public ClientRequest onBehalfOf(UserId user) {
        checkNotDefaultArg(user);
        return new ClientRequest(user, this);
    }

    /**
     * Creates a builder for posting guest requests.
     */
    public ClientRequest asGuest() {
        return onBehalfOf(guestUser());
    }

    /**
     * Subscribes the given {@link StreamObserver} to the given topic and activates
     * the subscription.
     *
     * @param topic
     *         the topic to subscribe to
     * @param observer
     *         the observer to subscribe
     * @param <M>
     *         the type of the result messages
     * @return the activated subscription
     * @see #cancel(Subscription)
     */
    <M extends Message> Subscription subscribeTo(Topic topic, StreamObserver<M> observer) {
        Subscription subscription = blockingSubscriptionService.subscribe(topic);
        subscriptionService.activate(subscription, new SubscriptionObserver<>(observer));
        return subscription;
    }

    /**
     * Requests cancellation of the passed subscription.
     *
     * @see #subscribeTo(Topic, StreamObserver)
     */
    public void cancel(Subscription s) {
        blockingSubscriptionService.cancel(s);
    }

    /**
     * Posts the command to the {@code CommandService}.
     */
    void postCommand(Command c) {
        commandService.post(c);
    }

    /**
     * Queries the read-side with the specified query.
     */
    <M extends Message> List<M> query(Query query) {
        @SuppressWarnings("unchecked")
        // The caller is responsible for matching the type with the query.
        List<M> result = queryService
                .read(query)
                .getMessageList()
                .stream()
                .map(EntityStateWithVersion::getState)
                .map(AnyPacker::unpack)
                .map(message -> (M) message)
                .collect(toList());
        return result;
    }

    /**
     * The builder for the client.
     */
    public static final class Builder {

        /**
         * The channel to be used in the client.
         *
         * <p>If not set directly, the channel will be created using the assigned host and port
         * values.
         */
        private ManagedChannel channel;

        /**
         * The address of the host which will be used for creating an instance
         * of {@code ManagedChannel}.
         *
         * <p>This field is {@code null} if the builder is created using already made
         * {@code ManagedChannel}.
         */
        private @MonotonicNonNull String host;
        private int port;

        /**
         * The ID of the tenant in a multi-tenant application.
         *
         * <p>Is {@code null} in single-tenant applications.
         */
        private @Nullable TenantId tenant;

        /** The ID of the user for performing requests on behalf of a non-logged in user. */
        private UserId guestUser = UserId
                .newBuilder()
                .setValue("guest")
                .build();

        private Builder(ManagedChannel channel) {
            this.channel = checkNotNull(channel);
        }

        private Builder(String host, int port) {
            this.host = host;
            this.port = port;
        }

        private static ManagedChannel createChannel(String host, int port) {
            ManagedChannel result = ManagedChannelBuilder
                    .forAddress(host, port)
                    .build();
            return result;
        }

        /**
         * Assigns the tenant for the client connection to be built.
         *
         * <p>This method should be called only in multi-tenant applications.
         *
         * @param tenant
         *          a non-null ID of the tenant
         */
        public Builder forTenant(TenantId tenant) {
            this.tenant = checkNotNull(tenant);
            return this;
        }

        /**
         * Assigns the ID of the user for performing requests on behalf of non-logged in user.
         *
         * <p>If the not set directly, the {@code UserId} withe value {@code "guest"} will be used.
         */
        public Builder withGuestId(UserId guestUser) {
           checkNotNull(guestUser);
           checkArgument(!isDefault(guestUser), "Guest user ID cannot be a default value.");
           this.guestUser = guestUser;
           return this;
        }

        /**
         * Creates a new instance of the client.
         */
        public Client build() {
            if (channel == null) {
                checkNotNull(host, "Either channel or host must be specified.");
                channel = createChannel(host, port);
            }
            return new Client(this);
        }
    }
}
