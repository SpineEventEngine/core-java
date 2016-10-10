/*
 *
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
 *
 */
package org.spine3.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.client.Subscription;
import org.spine3.client.SubscriptionUpdate;
import org.spine3.client.Target;
import org.spine3.client.Topic;
import org.spine3.client.grpc.SubscriptionServiceGrpc;
import org.spine3.protobuf.KnownTypes;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.stand.Stand;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The {@code SubscriptionService} provides an asynchronous way to fetch read-side state from the server.
 *
 * <p> For synchronous read-side updates please see {@link QueryService}.
 *
 * @author Alex Tymchenko
 */
public class SubscriptionService extends SubscriptionServiceGrpc.SubscriptionServiceImplBase {
    private final ImmutableMap<TypeUrl, BoundedContext> typeToContextMap;

    private SubscriptionService(Builder builder) {
        this.typeToContextMap = builder.getBoundedContextMap();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @SuppressWarnings("RefusedBequest")     // as we override default implementation with `unimplemented` status.
    @Override
    public void subscribe(Topic topic, StreamObserver<Subscription> responseObserver) {
        log().debug("Creating the subscription to a topic: {}", topic);

        try {
            final Target target = topic.getTarget();
            final BoundedContext boundedContext = selectBoundedContext(target);
            final Stand stand = boundedContext.getStand();

            final Subscription subscription = stand.subscribe(target);

            responseObserver.onNext(subscription);
            responseObserver.onCompleted();
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            log().error("Error processing subscription request", e);
            responseObserver.onError(e);
        }
    }

    @SuppressWarnings("RefusedBequest")     // as we override default implementation with `unimplemented` status.
    @Override
    public void activate(final Subscription subscription, final StreamObserver<SubscriptionUpdate> responseObserver) {
        log().debug("Activating the subscription: {}", subscription);

        try {
            final BoundedContext boundedContext = selectBoundedContext(subscription);

            final Stand.StandUpdateCallback updateCallback = new Stand.StandUpdateCallback() {
                @Override
                public void onEntityStateUpdate(Any newEntityState) {
                    checkNotNull(subscription);
                    final SubscriptionUpdate update = SubscriptionUpdate.newBuilder()
                                                                        .setSubscription(subscription)
                                                                        .setResponse(Responses.ok())
                                                                        .addUpdates(newEntityState)
                                                                        .build();
                    responseObserver.onNext(update);
                }
            };
            final Stand targetStand = boundedContext.getStand();
            targetStand.activate(subscription, updateCallback);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            log().error("Error activating the subscription", e);
            responseObserver.onError(e);
        }
    }

    @SuppressWarnings("RefusedBequest")     // as we override default implementation with `unimplemented` status.
    @Override
    public void cancel(Subscription subscription, StreamObserver<Response> responseObserver) {
        log().debug("Incoming cancel request for the subscription topic: {}", subscription);

        final BoundedContext boundedContext = selectBoundedContext(subscription);
        try {
            final Stand stand = boundedContext.getStand();
            stand.cancel(subscription);
            responseObserver.onNext(Responses.ok());
            responseObserver.onCompleted();
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            log().error("Error processing cancel subscription request", e);
            responseObserver.onError(e);
        }
    }

    private BoundedContext selectBoundedContext(Subscription subscription) {
        final String typeAsString = subscription.getType();
        final TypeUrl type = KnownTypes.getTypeUrl(typeAsString);
        checkNotNull(type, "Unknown type of the subscription");
        final BoundedContext result = typeToContextMap.get(type);
        return result;
    }

    private BoundedContext selectBoundedContext(Target target) {
        final String typeAsString = target.getType();
        final TypeUrl type = KnownTypes.getTypeUrl(typeAsString);
        checkNotNull(type, "Unknown type of the target");
        final BoundedContext result = typeToContextMap.get(type);
        return result;
    }

    public static class Builder {
        private final Set<BoundedContext> boundedContexts = Sets.newHashSet();
        private ImmutableMap<TypeUrl, BoundedContext> typeToContextMap;

        public Builder addBoundedContext(BoundedContext boundedContext) {
            // Save it to a temporary set so that it is easy to remove it if needed.
            boundedContexts.add(boundedContext);
            return this;
        }

        public Builder removeBoundedContext(BoundedContext boundedContext) {
            boundedContexts.remove(boundedContext);
            return this;
        }

        @SuppressWarnings("ReturnOfCollectionOrArrayField") // the collection returned is immutable
        public ImmutableMap<TypeUrl, BoundedContext> getBoundedContextMap() {
            return typeToContextMap;
        }

        @SuppressWarnings("ReturnOfCollectionOrArrayField") // the collection returned is immutable
        public ImmutableList<BoundedContext> getBoundedContexts() {
            return ImmutableList.copyOf(boundedContexts);
        }

        /**
         * Builds the {@link SubscriptionService}.
         *
         * @throws IllegalStateException if no bounded contexts were added.
         */
        public SubscriptionService build() throws IllegalStateException {
            if (boundedContexts.isEmpty()) {
                throw new IllegalStateException("Subscription service must have at least one bounded context.");
            }
            this.typeToContextMap = createBoundedContextMap();
            final SubscriptionService result = new SubscriptionService(this);
            return result;
        }

        private ImmutableMap<TypeUrl, BoundedContext> createBoundedContextMap() {
            final ImmutableMap.Builder<TypeUrl, BoundedContext> builder = ImmutableMap.builder();
            for (BoundedContext boundedContext : boundedContexts) {
                addBoundedContext(builder, boundedContext);
            }
            return builder.build();
        }

        private static void addBoundedContext(ImmutableMap.Builder<TypeUrl, BoundedContext> mapBuilder,
                                              BoundedContext boundedContext) {

            final Stand stand = boundedContext.getStand();
            final ImmutableSet<TypeUrl> exposedTypes = stand.getExposedTypes();
            for (TypeUrl availableType : exposedTypes) {
                mapBuilder.put(availableType, boundedContext);
            }
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(SubscriptionService.class);
    }

}
