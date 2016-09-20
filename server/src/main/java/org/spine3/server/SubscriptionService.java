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

import com.google.common.base.Preconditions;
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
    public void subscribe(Topic topic, final StreamObserver<SubscriptionUpdate> responseObserver) {
        log().debug("Incoming subscription request to topic: {}", topic);
        final Target target = topic.getTarget();
        final BoundedContext boundedContext = selectBoundedContext(target);

        try {
            final SubscriptionContext context = new SubscriptionContext();
            final Stand.StandUpdateCallback updateCallback = new Stand.StandUpdateCallback() {
                @Override
                public void onEntityStateUpdate(Any newEntityState) {
                    final Subscription subscription = context.getSubscription();
                    Preconditions.checkNotNull(subscription);
                    final SubscriptionUpdate update = SubscriptionUpdate.newBuilder()
                                                                        .setSubscription(subscription)
                                                                        .setResponse(Responses.ok())
                                                                        .setUpdates(0, newEntityState)
                                                                        .build();
                    responseObserver.onNext(update);
                }
            };


            final Subscription subscription = boundedContext.getStand()
                                                            .subscribe(target, updateCallback);
            context.setSubscription(subscription);

        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            log().error("Error processing subscription request", e);
            responseObserver.onError(e);
            responseObserver.onCompleted();
        }
    }

    @SuppressWarnings("RefusedBequest")     // as we override default implementation with `unimplemented` status.
    @Override
    public void cancel(Subscription subscription, StreamObserver<Response> responseObserver) {
        log().debug("Incoming cancel request for the subscription topic: {}", subscription);

        final BoundedContext boundedContext = selectBoundedContext(subscription);
        try {
            boundedContext.getStand()
                          .cancel(subscription);
            responseObserver.onNext(Responses.ok());
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            log().error("Error processing cancel subscription request", e);
            responseObserver.onError(e);
            responseObserver.onCompleted();
        }
    }

    private BoundedContext selectBoundedContext(Subscription subscription) {
        final String typeAsString = subscription.getType();
        final TypeUrl type = KnownTypes.getTypeUrl(typeAsString);
        return typeToContextMap.get(type);
    }

    private BoundedContext selectBoundedContext(Target target) {
        final String typeAsString = target.getType();
        final TypeUrl type = KnownTypes.getTypeUrl(typeAsString);
        return typeToContextMap.get(type);
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

            final ImmutableSet<TypeUrl> availableTypes = boundedContext.getStand()
                                                                       .getAvailableTypes();

            for (TypeUrl availableType : availableTypes) {
                mapBuilder.put(availableType, boundedContext);
            }
        }
    }

    /**
     * The context for the subscription.
     *
     * <p>Used as a wrapper around the subscription being created during the {@link #subscribe(Topic, StreamObserver)}.
     */
    private static class SubscriptionContext {
        private Subscription subscription;

        public Subscription getSubscription() {
            return subscription;
        }

        public void setSubscription(Subscription subscription) {
            this.subscription = subscription;
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
