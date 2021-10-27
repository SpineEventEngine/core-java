/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
package io.spine.server;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.grpc.stub.StreamObserver;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Subscriptions;
import io.spine.client.Target;
import io.spine.client.Topic;
import io.spine.client.grpc.SubscriptionServiceGrpc;
import io.spine.core.Response;
import io.spine.logging.Logging;
import io.spine.server.stand.Stand;
import io.spine.server.stand.SubscriptionCallback;
import io.spine.type.TypeUrl;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.flogger.LazyArgs.lazy;
import static io.spine.grpc.StreamObservers.forwardErrorsOnly;
import static io.spine.server.stand.SubscriptionCallback.forwardingTo;

/**
 * The {@code SubscriptionService} provides an asynchronous way to fetch read-side state
 * from the server.
 *
 * <p>For synchronous read-side updates please see {@link QueryService}.
 */
public final class SubscriptionService
        extends SubscriptionServiceGrpc.SubscriptionServiceImplBase
        implements Logging {

    private static final Joiner LIST_JOINER = Joiner.on(", ");

    private final ImmutableMap<TypeUrl, BoundedContext> typeToContextMap;

    private SubscriptionService(ImmutableMap<TypeUrl, BoundedContext> map) {
        super();
        this.typeToContextMap = checkNotNull(map);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /** Composes the service with a single Bounded Context. **/
    public static SubscriptionService fromSingle(BoundedContext context) {
        SubscriptionService result = newBuilder()
                .add(context)
                .build();

        return result;
    }

    @Override
    public void subscribe(Topic topic, StreamObserver<Subscription> responseObserver) {
        _debug().log("Creating the subscription to the topic: `%s`.", topic);
        try {
            subscribeTo(topic, responseObserver);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error().withCause(e)
                    .log("Error processing subscription request.");
            responseObserver.onError(e);
        }
    }

    private void subscribeTo(Topic topic, StreamObserver<Subscription> responseObserver) {
        Target target = topic.getTarget();
        Optional<BoundedContext> foundContext = findContextOf(target);
        if (foundContext.isPresent()) {
            Stand stand = foundContext.get().stand();
            stand.subscribe(topic, responseObserver);
        } else {
            Set<BoundedContext> contexts = ImmutableSet.copyOf(typeToContextMap.values());
            _warn().log("Unable to find a Bounded Context for type `%s`." +
                                " Creating a subscription in contexts: %s.",
                        topic.getTarget().type(),
                        LIST_JOINER.join(contexts));
            Subscription subscription = Subscriptions.from(topic);
            for (BoundedContext context : contexts) {
                Stand stand = context.stand();
                stand.subscribe(subscription);
            }
            responseObserver.onNext(subscription);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void activate(Subscription subscription, StreamObserver<SubscriptionUpdate> observer) {
        _debug().log("Activating the subscription: `%s`.", subscription);
        try {
            SubscriptionCallback callback = forwardingTo(observer);
            StreamObserver<Response> responseObserver = forwardErrorsOnly(observer);
            Optional<BoundedContext> foundContext = findContextOf(subscription);
            if (foundContext.isPresent()) {
                Stand targetStand = foundContext.get().stand();
                targetStand.activate(subscription, callback, responseObserver);
            } else {
                for (BoundedContext context : typeToContextMap.values()) {
                    Stand stand = context.stand();
                    stand.activate(subscription, callback, responseObserver);
                }
            }
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error().withCause(e)
                    .log("Error activating the subscription.");
            observer.onError(e);
        }
    }

    @Override
    public void cancel(Subscription subscription, StreamObserver<Response> responseObserver) {
        _debug().log("Incoming cancel request for the subscription topic: `%s`.", subscription);

        Optional<BoundedContext> selected = findContextOf(subscription);
        if (!selected.isPresent()) {
            _warn().log("Trying to cancel a subscription `%s` which could not be found.",
                        lazy(subscription::toShortString));
            responseObserver.onCompleted();
            return;
        }
        try {
            BoundedContext context = selected.get();
            Stand stand = context.stand();
            stand.cancel(subscription, responseObserver);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error().withCause(e)
                    .log("Error processing cancel subscription request.");
            responseObserver.onError(e);
        }
    }

    private Optional<BoundedContext> findContextOf(Subscription subscription) {
        Target target = subscription.getTopic()
                                    .getTarget();
        Optional<BoundedContext> result = findContextOf(target);
        return result;
    }

    private Optional<BoundedContext> findContextOf(Target target) {
        TypeUrl type = target.type();
        BoundedContext selected = typeToContextMap.get(type);
        Optional<BoundedContext> result = Optional.ofNullable(selected);
        return result;
    }

    /**
     * The builder for the {@link SubscriptionService}.
     */
    public static class Builder {
        private final Set<BoundedContext> contexts = Sets.newHashSet();

        /** Adds the context to be handled by the subscription service. */
        @CanIgnoreReturnValue
        public Builder add(BoundedContext context) {
            // Save it to a temporary set so that it is easy to remove it if needed.
            contexts.add(context);
            return this;
        }

        /** Removes the context from being handled by the subscription service. */
        @CanIgnoreReturnValue
        public Builder remove(BoundedContext context) {
            contexts.remove(context);
            return this;
        }

        /** Obtains the context added to the subscription service by the time of the call. */
        public ImmutableList<BoundedContext> contexts() {
            return ImmutableList.copyOf(contexts);
        }

        /**
         * Builds the {@link SubscriptionService}.
         *
         * @throws IllegalStateException if no Bounded Contexts were added.
         */
        public SubscriptionService build() throws IllegalStateException {
            if (contexts.isEmpty()) {
                throw new IllegalStateException(
                        "Subscription service must have at least one Bounded Context.");
            }
            ImmutableMap<TypeUrl, BoundedContext> map = createMap();
            SubscriptionService result = new SubscriptionService(map);
            return result;
        }

        private ImmutableMap<TypeUrl, BoundedContext> createMap() {
            ImmutableMap.Builder<TypeUrl, BoundedContext> builder = ImmutableMap.builder();
            for (BoundedContext context : contexts) {
                putIntoMap(context, builder);
            }
            return builder.build();
        }

        private static void putIntoMap(BoundedContext context,
                                       ImmutableMap.Builder<TypeUrl, BoundedContext> mapBuilder) {
            Stand stand = context.stand();
            Consumer<TypeUrl> putIntoMap = typeUrl -> mapBuilder.put(typeUrl, context);
            stand.exposedTypes()
                 .forEach(putIntoMap);
            stand.exposedEventTypes()
                 .forEach(putIntoMap);
        }
    }
}
