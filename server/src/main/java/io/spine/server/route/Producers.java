/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.annotation.Internal;
import io.spine.core.EventContext;
import io.spine.core.Events;

import java.util.Set;

/**
 * Internal utility class that provides default {@link EventRoute}s
 * for obtaining a producer from an event.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public class Producers {

    private Producers() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates the {@code IdSetEventFunction} that obtains event producer ID from
     * an {@code EventContext} and returns it as a sole element of the {@code ImmutableSet}.
     *
     * @param <I> the type of the IDs managed by the repository
     * @return new function instance
     */
    public static <I> EventRoute<I, Message> fromContext() {
        return new FromContext<>();
    }

    /**
     * Creates the {@code IdSetEventFunction} that obtains event producer ID from
     * an {@code EventContext} and returns it as a sole element of the {@code ImmutableSet}.
     *
     * @param <I> the type of the IDs managed by the repository
     * @return new function instance
     */
    public static <I> EventRoute<I, Message> fromFirstMessageField() {
        return new FromFirstMessageField<>();
    }

    /**
     * Obtains an event producer ID from the context of the event.
     *
     * @param <I> the type of entity IDs
     */
    private static class FromContext<I> implements EventRoute<I, Message> {

        private static final long serialVersionUID = 0L;

        @Override
        public Set<I> apply(Message message, EventContext context) {
            final I id = Events.getProducer(context);
            return ImmutableSet.of(id);
        }

        @Override
        public String toString() {
            return "Producers.fromContext()";
        }
    }

    /**
     * The {@code IdSetFunction} that obtains an event producer ID from the context of the event.
     *
     * @param <I> the type of entity IDs
     */
    private static class FromFirstMessageField<I> implements EventRoute<I, Message> {

        private static final long serialVersionUID = 0L;

        private final FromEventMessage<I, Message> func = FromEventMessage.fieldAt(0);

        @Override
        public Set<I> apply(Message message, EventContext context) {
            final I id = func.apply(message, context);
            return ImmutableSet.of(id);
        }

        @Override
        public String toString() {
            return "Producers.fromFirstMessageField()";
        }
    }

    /**
     * Obtains an event producer ID based on an event {@link Message} and context.
     *
     * <p>An ID must be the first field in event messages (in Protobuf definition).
     * Its name must end with the
     * {@link Identifier#ID_PROPERTY_SUFFIX Identifier.ID_PROPERTY_SUFFIX}.
     *
     * @param <I> the type of target entity IDs
     * @param <M> the type of event messages to get IDs from
     * @author Alexander Litus
     */
    private static class FromEventMessage<I, M extends Message>
            extends FieldAtIndex<I, M, EventContext> {

        private static final long serialVersionUID = 0L;

        private FromEventMessage(int idIndex) {
            super(idIndex);
        }

        /**
         * Creates a new instance.
         *
         * @param index a zero-based index of an ID field in this type of messages
         */
        static<I, M extends Message> FromEventMessage<I, M> fieldAt(int index) {
            return new FromEventMessage<>(index);
        }
    }
}
