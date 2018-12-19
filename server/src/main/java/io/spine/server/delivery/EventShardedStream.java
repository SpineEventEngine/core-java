/*
 * Copyright 2018, TeamDev. All rights reserved.
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
package io.spine.server.delivery;

import com.google.protobuf.Any;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;

import static io.spine.protobuf.AnyPacker.unpack;

/**
 * The stream of events sent to a specific shard.
 *
 * @param <I> the type of the identifiers of the event targets.
 */
public class EventShardedStream<I> extends ShardedStream<I, Event, EventEnvelope> {

    private EventShardedStream(Builder<I> builder) {
        super(builder);
    }

    public static <I> Builder<I> newBuilder() {
        return new Builder<>();
    }

    @Override
    protected ShardedMessageConverter<I, Event, EventEnvelope> newConverter() {
        return new Converter<>();
    }

    /**
     * The converter of {@link EventEnvelope} into {@link ShardedMessage} instances
     * and vice versa.
     *
     * @param <I> the type of the identifiers of the event targets.
     */
    private static class Converter<I> extends ShardedMessageConverter<I, Event, EventEnvelope> {

        @Override
        protected EventEnvelope toEnvelope(Any packedEvent) {
            Event event = unpack(packedEvent, Event.class);
            EventEnvelope result = EventEnvelope.of(event);
            return result;
        }
    }

    /**
     * The builder for the {@code EventShardedStream} instances.
     *
     * @param <I> the type of the identifiers of the event targets.
     */
    public static class Builder<I>
            extends AbstractBuilder<I, EventEnvelope, Builder<I>, EventShardedStream<I>> {
        @Override
        protected EventShardedStream<I> createStream() {
            return new EventShardedStream<>(this);
        }
    }
}
