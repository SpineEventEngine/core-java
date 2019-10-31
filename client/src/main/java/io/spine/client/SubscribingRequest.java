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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.base.MessageContext;

import java.util.function.Consumer;

/**
 * Abstract base for client requests that subscribe to messages.
 *
 * @param <M>
 *         the type of the subscribed messages
 * @param <C>
 *         the type of the context of messages or {@link io.spine.core.EmptyContext} if
 *         messages do not have a context
 * @param <W>
 *         the type of the message that wraps a message and its context
 *         (e.g. {@link io.spine.core.Event}); if subscribed message type does not have a context,
 *         this parameter is likely to be the same as {@code M}
 * @param <B>
 *         the type of this requests for return type covariance
 */

public abstract class
SubscribingRequest<M extends Message,
                   C extends MessageContext,
                   W extends Message,
                   B extends SubscribingRequest<M, C, W, B>>
        extends FilteringRequest<M, Topic, TopicBuilder, SubscribingRequest<M, C, W, B>> {

    SubscribingRequest(ClientRequest parent, Class<M> type) {
        super(parent, type);
    }

    abstract Consumers.Builder<M, C, W, ?> consumers();

    abstract MessageConsumer<M, C> toMessageConsumer(Consumer<M> consumer);

    @CanIgnoreReturnValue
    public SubscribingRequest<M, C, W, B> observe(Consumer<M> consumer) {
        consumers().add(toMessageConsumer(consumer));
        return self();
    }

    /**
     * Assigns a handler for the error reported to
     * {@link StreamObserver#onError(Throwable)} of
     * the {@link StreamObserver} responsible for delivering messages
     * to the consumers.
     *
     * <p>Once this handler is called, no more messages will be delivered to consumers.
     *
     * @see #onConsumingError(ConsumerErrorHandler)
     */
    @CanIgnoreReturnValue
    public SubscribingRequest<M, C, W, B> onStreamingError(ErrorHandler handler) {
        consumers().onStreamingError(handler);
        return self();
    }

    /**
     * Assigns a handler for an error that may occur in the code of one of the consumers.
     *
     * <p>After this handler called, remaining consumers will get the message as usually.
     *
     * @see #onStreamingError(ErrorHandler)
     */
    @CanIgnoreReturnValue
    SubscribingRequest<M, C, W, B> onConsumingError(ConsumerErrorHandler<M> handler) {
        consumers().onConsumingError(handler);
        return self();
    }

    /**
     * Creates and posts the subscription request to the server.
     */
    public Subscription subscribe() {
        Topic topic = builder().build();
        StreamObserver<W> observer = createObserver();
        return subscribe(topic, observer);
    }

    /**
     * Subscribes to receive all messages of the specified type.
     */
    public Subscription all() {
        Topic topic = factory().topic().allOf(messageType());
        StreamObserver<W> observer = createObserver();
        return subscribe(topic, observer);
    }

    private StreamObserver<W> createObserver() {
        return consumers().build().toObserver();
    }

    private Subscription subscribe(Topic topic, StreamObserver<W> observer) {
        Subscription subscription = client().subscribeTo(topic, observer);
        return subscription;
    }
}
