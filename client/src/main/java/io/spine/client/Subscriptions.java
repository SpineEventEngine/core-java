/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.spine.base.Identifier;
import io.spine.client.grpc.SubscriptionServiceGrpc;
import io.spine.client.grpc.SubscriptionServiceGrpc.SubscriptionServiceBlockingStub;
import io.spine.client.grpc.SubscriptionServiceGrpc.SubscriptionServiceStub;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Collections.synchronizedSet;

/**
 * Maintains the list of the <strong>active</strong> subscriptions created by the {@link Client}.
 *
 * <p>Subscriptions are created when a client application:
 * <ul>
 *     <li>{@linkplain ClientRequest#subscribeToEvent(Class) subscribes to events};
 *     <li>{@linkplain ClientRequest#subscribeTo(Class) subscribes to entity states};
 *     <li>{@linkplain CommandRequest#post() posts a command}.
 * </ul>
 *
 * <p>The subscriptions should be {@linkplain Subscriptions#cancel(Subscription) cancelled} after
 * an update is delivered to the client and no further updates are expected.
 *
 * <p>All remaining subscriptions are {@linkplain #cancelAll() cancelled} by the {@code Client}
 * when it {@linkplain Client#close() closes}.
 *
 * @see ClientRequest#subscribeTo(Class)
 * @see ClientRequest#subscribeToEvent(Class)
 * @see CommandRequest#post()
 */
public final class Subscriptions {

    /**
     * The format of all {@linkplain SubscriptionId Subscription identifiers}.
     */
    private static final String SUBSCRIPTION_ID_FORMAT = "s-%s";

    /**
     * The format for convenient subscription printing in logs and error messages.
     */
    static final String SUBSCRIPTION_PRINT_FORMAT = "(ID: %s, target: %s)";

    private final SubscriptionServiceStub subscriptionService;
    private final SubscriptionServiceBlockingStub blockingSubscriptionService;
    private final Set<Subscription> subscriptions;

    Subscriptions(ManagedChannel channel) {
        this.subscriptionService = SubscriptionServiceGrpc.newStub(channel);
        this.blockingSubscriptionService = SubscriptionServiceGrpc.newBlockingStub(channel);
        this.subscriptions = synchronizedSet(new HashSet<>());
    }

    /**
     * Generates a new subscription identifier.
     *
     * <p>The result is based upon UUID generation.
     *
     * @return new subscription identifier.
     */
    public static SubscriptionId generateId() {
        String formattedId = format(SUBSCRIPTION_ID_FORMAT, Identifier.newUuid());
        return newId(formattedId);
    }

    /**
     * Wraps a given {@code String} as a subscription identifier.
     *
     * <p>Should not be used in production. Use {@linkplain #generateId() automatic generation}
     * instead.
     *
     * @return new subscription identifier.
     */
    public static SubscriptionId newId(String value) {
        return SubscriptionId.newBuilder()
                             .setValue(value)
                             .build();
    }

    /**
     * Creates a new subscription with the given {@link Topic} and a random ID.
     *
     * @param topic
     *         subscription topic
     * @return new subscription
     */
    public static Subscription from(Topic topic) {
        checkNotNull(topic);

        SubscriptionId id = generateId();
        Subscription subscription = Subscription
                .newBuilder()
                .setId(id)
                .setTopic(topic)
                .vBuild();
        return subscription;
    }

    /**
     * Obtains a short printable form of subscription.
     *
     * <p>Standard {@link Subscription#toString()} includes all subscription data and thus its
     * output is too huge to use in short log messages and stack traces.
     *
     * @return a printable {@code String} with core subscription data
     * @deprecated please use {@link Subscription#toShortString()}
     */
    @Deprecated
    public static String toShortString(Subscription subscription) {
        return subscription.toShortString();
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
        add(subscription);
        return subscription;
    }

    /** Adds all the passed subscriptions. */
    void addAll(Collection<Subscription> newSubscriptions) {
        newSubscriptions.forEach(this::add);
    }

    /** Remembers the passed subscription for future use. */
    private void add(Subscription s) {
        subscriptions.add(checkNotNull(s));
    }

    /**
     * Cancels the passed subscription.
     *
     * @return {@code true} if the subscription was previously made
     */
    public boolean cancel(Subscription subscription) {
        checkNotNull(subscription);
        requestCancellation(subscription);
        return subscriptions.remove(subscription);
    }

    private void requestCancellation(Subscription subscription) {
        //TODO:2020-04-17:alexander.yevsyukov: Check response and report the error.
        blockingSubscriptionService.cancel(subscription);
    }

    /** Cancels all the subscriptions. */
    public void cancelAll() {
        Iterator<Subscription> iterator = subscriptions.iterator();
        while (iterator.hasNext()) {
            Subscription subscription = iterator.next();
            requestCancellation(subscription);
            iterator.remove();
        }
    }

    @VisibleForTesting
    boolean contains(Subscription s) {
        return subscriptions.contains(s);
    }

    /**
     * Verifies if there are any active subscriptions.
     */
    public boolean isEmpty() {
        return subscriptions.isEmpty();
    }
}
