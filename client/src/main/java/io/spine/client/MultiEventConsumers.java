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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.grpc.stub.StreamObserver;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.logging.Logging;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An association of event types to their consumers which also delivers events.
 *
 * <p>A consumer of an event can accept {@linkplain EventConsumer event message and its context}
 * or {@linkplain Consumer only event message}.
 */
final class MultiEventConsumers implements Logging {

    private final
    ImmutableMap<Class<? extends EventMessage>, EventConsumers<? extends EventMessage>> map;

    static Builder newBuilder() {
        return new Builder();
    }

    private MultiEventConsumers(Builder builder) {
        this.map = ImmutableMap.copyOf(builder.toMap());
    }

    /**
     * Obtains the set of consumed event types.
     */
    ImmutableSet<Class<? extends EventMessage>> eventTypes() {
        return map.keySet();
    }

    /** Obtains all the consumers grouped by type of consumed events. */
    ImmutableMap<Class<? extends EventMessage>, StreamObserver<Event>> toObservers() {
        Map<Class<? extends EventMessage>, StreamObserver<Event>> observers =
                Maps.transformValues(map, EventConsumers::toObserver);
        return ImmutableMap.copyOf(observers);
    }

    /**
     * Creates an observer that would deliver events to all the consumers.
     *
     * @param handler
     *         the handler for possible errors reported by the server.
     *         If null the error will be simply logged.
     */
    StreamObserver<Event> toObserver(@Nullable ErrorHandler handler) {
        return new DeliveringMultiEventObserver(this, handler);
    }

    /**
     * The builder for {@link MultiEventConsumers}.
     */
    static final class Builder {

        /** Maps a type of an event to the builder of {@code EventConsumers} of such events. */
        private final
        Map<Class<? extends EventMessage>, EventConsumers.Builder<? extends EventMessage>> map =
                new HashMap<>();

        /** The handler for streaming errors that may occur during gRPC calls. */
        private @Nullable ErrorHandler streamingErrorHandler;

        /** The common handler for errors of all consumed event types that may occur. */
        private @Nullable ConsumerErrorHandler<EventMessage> consumingErrorHandler;

        @CanIgnoreReturnValue
        <E extends EventMessage>
        Builder observe(Class<E> eventType, Consumer<E> consumer) {
            checkNotNull(eventType);
            checkNotNull(consumer);
            EventConsumer<E> ec = EventConsumer.from(consumer);
            return doPut(eventType, ec);
        }

        /**
         * Adds the consumer of the event message and its context.
         */
        @CanIgnoreReturnValue
        <E extends EventMessage>
        Builder observe(Class<E> eventType, EventConsumer<E> consumer) {
            checkNotNull(eventType);
            checkNotNull(consumer);
            return doPut(eventType, consumer);
        }

        private <E extends EventMessage>
        Builder doPut(Class<E> eventType, EventConsumer<E> ec) {
            if (map.containsKey(eventType)) {
                @SuppressWarnings("unchecked")
                // The cast is protected by generic params of this method.
                EventConsumers.Builder<E> builder = (EventConsumers.Builder<E>) map.get(eventType);
                builder.add(ec);
            } else {
                map.put(eventType, EventConsumers.<E>newBuilder().add(ec));
            }
            return this;
        }

        /**
         * Produces a map from an event type to consumers of those events.
         */
        private ImmutableMap<Class<? extends EventMessage>, EventConsumers<? extends EventMessage>>
        toMap() {
            ImmutableMap.Builder<Class<? extends EventMessage>,
                                 EventConsumers<? extends EventMessage>>
            builder = ImmutableMap.builder();
            for (Class<? extends EventMessage> eventType : map.keySet()) {
                EventConsumers.Builder<? extends EventMessage> consumers = map.get(eventType);
                if (streamingErrorHandler != null) {
                    consumers.onStreamingError(streamingErrorHandler);
                }
                if (consumingErrorHandler != null) {
                    consumers.onConsumingError(
                            new DelegatingEventConsumerHandler<>(consumingErrorHandler)
                    );
                }
                builder.put(eventType, consumers.build());
            }
            return builder.build();
        }

        /**
         * Assigns a handler for the error reported to
         * {@link StreamObserver#onError(Throwable)}.
         *
         * <p>Once this handler is called, no more messages will be delivered to consumers.
         *
         * @see #onConsumingError(ConsumerErrorHandler)
         */
        @CanIgnoreReturnValue
        Builder onStreamingError(ErrorHandler handler) {
            streamingErrorHandler = checkNotNull(handler);
            return this;
        }

        /**
         * Assigns a handler for an error that may occur in the code of one of the consumers.
         *
         * <p>After this handler called, remaining consumers will get the message as usually.
         *
         * @see #onStreamingError(ErrorHandler)
         */
        @CanIgnoreReturnValue
        Builder onConsumingError(ConsumerErrorHandler<EventMessage> handler) {
            consumingErrorHandler = checkNotNull(handler);
            return this;
        }
        /**
         * Creates the new instance.
         */
        MultiEventConsumers build() {
            return new MultiEventConsumers(this);
        }
    }

    /**
     * Adapts generified {@code ConsumerErrorHandler<E>} API to non-generified
     * so that a common error handler can be used for all the consumers.
     *
     * @param <E>
     *         the type of events observed by an instance of {@code EventConsumers}
     * @see Builder#toMap()
     */
    private static final class DelegatingEventConsumerHandler<E extends EventMessage>
            implements ConsumerErrorHandler<E> {

        private final ConsumerErrorHandler<EventMessage> delegate;

        private DelegatingEventConsumerHandler(ConsumerErrorHandler<EventMessage> delegate) {
            this.delegate = checkNotNull(delegate);
        }

        @Override
        public void accept(MessageConsumer<E, ?> consumer, Throwable throwable) {
            @SuppressWarnings("unchecked")
            // The cast is protected by generic params of `EventConsumers`.
                    MessageConsumer<EventMessage, EventContext> cast =
                    (MessageConsumer<EventMessage, EventContext>) consumer;
            delegate.accept(cast, throwable);
        }
    }
}
