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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import io.grpc.stub.StreamObserver;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.logging.Logging;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.protobuf.TextFormat.shortDebugString;
import static io.spine.client.DelegatingConsumer.toRealConsumer;

/**
 * Delivers an event to all consumers subscribed to events of this type.
 *
 * @see #onNext(Event)
 */
final class MultiEventDelivery implements StreamObserver<Event>, Logging {

    private final
    ImmutableMultimap<Class<? extends EventMessage>, EventConsumer<? extends EventMessage>> map;
    private final ErrorHandler streamingErrorHandler;

    /**
     * Creates new instance.
     *
     * @param consumers
     *         event consumers
     * @param streamingErrorHandler
     *         the handler for errors which is used when a {@linkplain #onError(Throwable)
     *         streaming error occurs}. When this handler is invoked no further delivery of
     *         events is expected. If {@code null} is passed, default implementation would
     *         log the error.
     */
    MultiEventDelivery(MultiEventConsumers consumers,
                       @Nullable ErrorHandler streamingErrorHandler) {
        this.map = ImmutableMultimap.copyOf(consumers.map());
        this.streamingErrorHandler = nullToDefault(streamingErrorHandler);
    }

    private ErrorHandler nullToDefault(@Nullable ErrorHandler handler) {
        if (handler != null) {
            return handler;
        }
        return throwable -> _error().withCause(throwable).log("Error receiving event.");
    }

    @Override
    public void onNext(Event e) {
        deliver(e);
    }

    /**
     * Delivers the event to all the subscribed consumers.
     *
     * <p>If one of the consumers would cause an error when handling the event, the error will
     * be logged, and the event will be passed to remaining consumers.
     */
    private void deliver(Event event) {
        consumersOf(event.type()).forEach(c -> deliver(event, c));
    }

    private ImmutableCollection<EventConsumer<? extends EventMessage>>
    consumersOf(Class<? extends EventMessage> type) {
        return map.get(type);
    }

    /**
     * Delivers the event to the consumer.
     *
     * <p>If the consumer causes an error it will be logged and not rethrown.
     */
    private void deliver(Event event, EventConsumer<? extends EventMessage> c) {
        EventMessage msg = event.enclosedMessage();
        EventContext ctx = event.getContext();
        try {
            @SuppressWarnings("unchecked")
            // Safe as we match the type when adding consumers.
            EventConsumer<EventMessage> consumer = (EventConsumer<EventMessage>) c;
            consumer.accept(msg, ctx);
        } catch (Throwable throwable) {
            logError(c, event, throwable);
        }
    }

    private void logError(EventConsumer<?> consumer, Event event, Throwable throwable) {
        String eventDiags = shortDebugString(event);
        Object consumerToReport = toRealConsumer(consumer);
        _error().withCause(throwable)
                .log("The consumer `%s` could not handle the event `%s`.",
                     consumerToReport, eventDiags);
    }

    /** Passes the {@code Throwable} to the configured error handler. */
    @Override
    public void onError(Throwable t) {
        streamingErrorHandler.accept(t);
    }

    /** Does nothing. */
    @Override
    public void onCompleted() {
        // Do nothing.
    }
}
