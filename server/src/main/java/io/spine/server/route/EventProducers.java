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

package io.spine.server.route;

import com.google.common.collect.ImmutableSet;
import io.spine.base.EventMessage;
import io.spine.core.EventContext;
import io.spine.core.Events;

import java.util.Set;

/**
 * Provides default {@link EventRoute}s for obtaining a producer ID from an event.
 */
final class EventProducers {

    /** Prevents instantiation of this utility class. */
    private EventProducers() {
    }

    /**
     * Obtains an event producer ID from the context of the event.
     */
    static final class FromContext<I> implements EventRoute<I, EventMessage> {

        private static final long serialVersionUID = 0L;

        @Override
        public Set<I> apply(EventMessage message, EventContext context) {
            @SuppressWarnings("unchecked") // The route creator is responsible for the type check.
            I id = (I) Events.getProducer(context);
            return ImmutableSet.of(id);
        }

        @Override
        public String toString() {
            return "EventProducers.fromContext()";
        }
    }

    /**
     * The route that obtains a producer ID from the first field of the event message.
     */
    static final class FromFirstMessageField<I> implements EventRoute<I, EventMessage> {

        private static final long serialVersionUID = 0L;

        private final FromEventMessage<I> func = FromEventMessage.fieldAt(0);

        @Override
        public Set<I> apply(EventMessage message, EventContext context) {
            I id = func.apply(message, context);
            return ImmutableSet.of(id);
        }

        @Override
        public String toString() {
            return "EventProducers.fromFirstMessageField()";
        }
    }

    /**
     * Obtains an event producer ID from a field of an event message.
     */
    static final class FromEventMessage<I> extends FieldAtIndex<I, EventMessage, EventContext> {

        private static final long serialVersionUID = 0L;

        private FromEventMessage(int idIndex) {
            super(idIndex);
        }

        /**
         * Creates a new instance.
         *
         * @param index a zero-based index of an ID field in this type of messages
         */
        static<I> FromEventMessage<I> fieldAt(int index) {
            return new FromEventMessage<>(index);
        }
    }
}
