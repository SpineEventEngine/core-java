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

package org.spine3.server.entity;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.base.Events;

import java.util.Set;

/**
 * Utility class that provides default implementations for {@link IdSetEventFunction}s.
 *
 * @author Alexander Yevsyukov
 */
public class DefaultIdSetEventFunction {

    private DefaultIdSetEventFunction() {
    }

    /**
     * Creates the {@code IdSetEventFunction} that obtains event producer ID from
     * an {@code EventContext} and returns it as a sole element of the {@code ImmutableSet}.
     *
     * @param <I> the type of the IDs managed by the repository
     * @return new function instance
     */
    public static <I> IdSetEventFunction<I, Message> producerFromContext() {
        return new ProducerFromContext<>();
    }

    /**
     * Creates the {@code IdSetEventFunction} that obtains event producer ID from
     * an {@code EventContext} and returns it as a sole element of the {@code ImmutableSet}.
     *
     * @param <I> the type of the IDs managed by the repository
     * @return new function instance
     */
    public static <I> IdSetEventFunction<I, Message> producerFromFirstMessageField() {
        return new ProducerFromFirstEventMessageField<>();
    }

    /**
     * The {@code IdSetFunction} that obtains an event producer ID from the context of the event.
     *
     * @param <I> the type of entity IDs
     */
    private static class ProducerFromContext<I> implements IdSetEventFunction<I, Message> {
        @Override
        public Set<I> apply(Message message, EventContext context) {
            final I id = Events.getProducer(context);
            return ImmutableSet.of(id);
        }
    }

    /**
     * The {@code IdSetFunction} that obtains an event producer ID from the context of the event.
     *
     * @param <I> the type of entity IDs
     */
    private static class ProducerFromFirstEventMessageField<I> implements IdSetEventFunction<I, Message> {

        private final GetProducerIdFromEvent<I, Message> func = GetProducerIdFromEvent.fromFieldIndex(0);

        @Override
        public Set<I> apply(Message message, EventContext context) {
            final I id = func.apply(message, context);
            return ImmutableSet.of(id);
        }
    }
}
