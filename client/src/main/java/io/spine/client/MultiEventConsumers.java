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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.grpc.stub.StreamObserver;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.logging.Logging;
import org.checkerframework.checker.nullness.qual.Nullable;

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
    ImmutableMultimap<Class<? extends EventMessage>, EventConsumer<? extends EventMessage>> map;

    static Builder newBuilder() {
        return new Builder();
    }

    private MultiEventConsumers(Builder builder) {
        this.map = ImmutableMultimap.copyOf(builder.map);
    }

    /**
     * Obtains the set of consumed event types.
     */
    ImmutableSet<Class<? extends EventMessage>> eventTypes() {
        return map.keySet();
    }

    /** Obtains all the consumers grouped by type of consumed events. */
    ImmutableMultimap<Class<? extends EventMessage>, EventConsumer<? extends EventMessage>> map() {
        return map;
    }

    /**
     * Creates an observer that would deliver events to all the consumers.
     *
     * @param handler
     *         the handler for possible errors reported by the server.
     *         If null the error will be simply logged.
     */
    StreamObserver<Event> toObserver(@Nullable ErrorHandler handler) {
        return new MultiEventDelivery(this, handler);
    }

    /**
     * The builder for {@link MultiEventConsumers}.
     */
    static final class Builder {

        private final
        Multimap<Class<? extends EventMessage>, EventConsumer<? extends EventMessage>>
                map = HashMultimap.create();

        /**
         * Adds the consumer of the events message.
         */
        @CanIgnoreReturnValue
        public <E extends EventMessage>
        Builder observe(Class<E> eventType, Consumer<E> consumer) {
            checkNotNull(eventType);
            checkNotNull(consumer);
            map.put(eventType, EventConsumer.from(consumer));
            return this;
        }

        /**
         * Adds the consumer of the event message and its context.
         */
        @CanIgnoreReturnValue
        <E extends EventMessage>
        Builder observe(Class<E> eventType, EventConsumer<E> consumer) {
            checkNotNull(eventType);
            checkNotNull(consumer);
            map.put(eventType, consumer);
            return this;
        }

        /**
         * Creates the new instance.
         */
        MultiEventConsumers build() {
            return new MultiEventConsumers(this);
        }
    }
}
