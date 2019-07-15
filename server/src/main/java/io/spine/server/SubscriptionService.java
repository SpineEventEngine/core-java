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
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.flogger.LazyArgs.lazy;
import static io.spine.client.Subscriptions.toShortString;
import static io.spine.grpc.StreamObservers.forwardErrorsOnly;
import static io.spine.util.Exceptions.newIllegalArgumentException;

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
        _debug().log("Creating the subscription to a topic: `%s`.", topic);

        try {
            Target target = topic.getTarget();
            Optional<BoundedContext> selected = selectBoundedContext(target);
            BoundedContext context = selected.orElseThrow(
                    () -> newIllegalArgumentException(
                            "Trying to subscribe to an unknown type: `%s`.", target.getType())
            );
            Stand stand = context.stand();

            stand.subscribe(topic, responseObserver);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error().withCause(e)
                    .log("Error processing subscription request.");
            responseObserver.onError(e);
        }
    }

    @Override
    public void activate(Subscription subscription, StreamObserver<SubscriptionUpdate> observer) {
        _debug().log("Activating the subscription: `%s`.", subscription);
        try {
            Optional<BoundedContext> selected = selectBoundedContext(subscription);
            BoundedContext context = selected.orElseThrow(
                    () -> newIllegalArgumentException(
                            "Target subscription `%s` could not be found for activation.",
                            toShortString(subscription))
            );
            Stand.NotifySubscriptionAction notifyAction = update -> {
                checkNotNull(update);
                observer.onNext(update);
            };
            Stand targetStand = context.stand();

            targetStand.activate(subscription, notifyAction, forwardErrorsOnly(observer));
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error().withCause(e)
                    .log("Error activating the subscription.");
            observer.onError(e);
        }
    }

    @Override
    public void cancel(Subscription subscription, StreamObserver<Response> responseObserver) {
        _debug().log("Incoming cancel request for the subscription topic: `%s`.", subscription);

        Optional<BoundedContext> selected = selectBoundedContext(subscription);
        if (!selected.isPresent()) {
            _warn().log("Trying to cancel a subscription `%s` which could not be found.",
                        lazy(() -> toShortString(subscription)));
            responseObserver.onCompleted();
            return;
        }
        try {
            BoundedContext context = selected.get();
            Stand stand = context.stand();
            stand.cancel(subscription, responseObserver);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error().withCause(e)
                    .log("Error processing cancel subscription request");
            responseObserver.onError(e);
        }
    }

    private Optional<BoundedContext> selectBoundedContext(Subscription subscription) {
        Target target = subscription.getTopic()
                                    .getTarget();
        Optional<BoundedContext> result = selectBoundedContext(target);
        return result;
    }

    private Optional<BoundedContext> selectBoundedContext(Target target) {
        TypeUrl type = TypeUrl.parse(target.getType());
        BoundedContext selected = typeToContextMap.get(type);
        Optional<BoundedContext> result = Optional.ofNullable(selected);
        return result;
    }

    public static class Builder {
        private final Set<BoundedContext> contexts = Sets.newHashSet();

        public Builder add(BoundedContext context) {
            // Save it to a temporary set so that it is easy to remove it if needed.
            contexts.add(context);
            return this;
        }

        public Builder remove(BoundedContext context) {
            contexts.remove(context);
            return this;
        }

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
