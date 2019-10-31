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

import io.grpc.stub.StreamObserver;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.EventContext;

/**
 * A collection of event consumers that delivers messages to them.
 *
 * @param <E>
 *         the type of event messages
 */
final class EventConsumers<E extends EventMessage> extends Consumers<E, EventContext, Event> {

    static <E extends EventMessage> Builder<E> newBuilder() {
        return new Builder<>();
    }

    private EventConsumers(Builder<E> builder) {
        super(builder);
    }

    @Override
    StreamObserver<Event> toObserver() {
        return new EventObserver();
    }

    /**
     * The observer to be supplied to gRPC API.
     */
    private final class EventObserver extends DeliveringObserver {

        @SuppressWarnings("unchecked") // The correct type is provided by subscription impl.
        @Override
        E toMessage(Event event) {
            return (E) event.enclosedMessage();
        }

        @Override
        EventContext toContext(Event event) {
            return event.context();
        }
    }

    /**
     * The builder for consumers of events.
     *
     * @param <E>
     *         the type of event messages
     */
    static final class Builder<E extends EventMessage>
        extends Consumers.Builder<E, EventContext, Event, Builder<E>> {

        @Override
        Builder<E> self() {
            return this;
        }

        @Override
        Consumers<E, EventContext, Event> build() {
            return new EventConsumers<>(this);
        }
    }
}
