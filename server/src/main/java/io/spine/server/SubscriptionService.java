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
package io.spine.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.grpc.stub.StreamObserver;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Target;
import io.spine.client.Topic;
import io.spine.client.grpc.SubscriptionServiceGrpc;
import io.spine.core.Response;
import io.spine.logging.Logging;
import io.spine.server.stand.Stand;
import io.spine.type.TypeUrl;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.grpc.StreamObservers.forwardErrorsOnly;

/**
 * The {@code SubscriptionService} provides an asynchronous way to fetch read-side state
 * from the server.
 *
 * <p>For synchronous read-side updates please see {@link QueryService}.
 */
public class SubscriptionService
        extends SubscriptionServiceGrpc.SubscriptionServiceImplBase
        implements Logging {

    private final ImmutableMap<TypeUrl, BoundedContext> typeToContextMap;

    private SubscriptionService(Map<TypeUrl, BoundedContext> map) {
        super();
        this.typeToContextMap = ImmutableMap.copyOf(map);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public void subscribe(Topic topic, StreamObserver<Subscription> responseObserver) {
        _debug("Creating the subscription to a topic: {}", topic);

        try {
            Target target = topic.getTarget();
            BoundedContext boundedContext = selectBoundedContext(target);
            Stand stand = boundedContext.getStand();

            stand.subscribe(topic, responseObserver);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error(e, "Error processing subscription request");
            responseObserver.onError(e);
        }
    }

    @Override
    public void activate(Subscription subscription, StreamObserver<SubscriptionUpdate> observer) {
        _debug("Activating the subscription: {}", subscription);

        try {
            BoundedContext boundedContext = selectBoundedContext(subscription);
            Stand.NotifySubscriptionAction notifyAction = update -> {
                checkNotNull(update);
                observer.onNext(update);
            };
            Stand targetStand = boundedContext.getStand();

            targetStand.activate(subscription, notifyAction, forwardErrorsOnly(observer));
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error(e, "Error activating the subscription");
            observer.onError(e);
        }
    }

    @Override
    public void cancel(Subscription subscription, StreamObserver<Response> responseObserver) {
        _debug("Incoming cancel request for the subscription topic: {}", subscription);

        BoundedContext boundedContext = selectBoundedContext(subscription);
        try {
            Stand stand = boundedContext.getStand();
            stand.cancel(subscription, responseObserver);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error(e, "Error processing cancel subscription request");
            responseObserver.onError(e);
        }
    }

    private BoundedContext selectBoundedContext(Subscription subscription) {
        Target target = subscription.getTopic()
                                    .getTarget();
        BoundedContext context = selectBoundedContext(target);
        return context;
    }

    private BoundedContext selectBoundedContext(Target target) {
        TypeUrl type = TypeUrl.parse(target.getType());
        BoundedContext result = typeToContextMap.get(type);
        return result;
    }

    public static class Builder {
        private final Set<BoundedContext> boundedContexts = Sets.newHashSet();

        public Builder add(BoundedContext boundedContext) {
            // Save it to a temporary set so that it is easy to remove it if needed.
            boundedContexts.add(boundedContext);
            return this;
        }

        public Builder remove(BoundedContext boundedContext) {
            boundedContexts.remove(boundedContext);
            return this;
        }

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
                throw new IllegalStateException(
                        "Subscription service must have at least one bounded context.");
            }
            ImmutableMap<TypeUrl, BoundedContext> map = createMap();
            SubscriptionService result = new SubscriptionService(map);
            return result;
        }

        private ImmutableMap<TypeUrl, BoundedContext> createMap() {
            ImmutableMap.Builder<TypeUrl, BoundedContext> builder = ImmutableMap.builder();
            for (BoundedContext boundedContext : boundedContexts) {
                putIntoMap(boundedContext, builder);
            }
            return builder.build();
        }

        private static void putIntoMap(BoundedContext boundedContext,
                                       ImmutableMap.Builder<TypeUrl, BoundedContext> mapBuilder) {
            Stand stand = boundedContext.getStand();
            Consumer<TypeUrl> putIntoMap = typeUrl -> mapBuilder.put(typeUrl, boundedContext);
            stand.getExposedTypes()
                 .forEach(putIntoMap);
            stand.getExposedEventTypes()
                 .forEach(putIntoMap);
        }
    }
}
