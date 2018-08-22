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

package io.spine.server.model;

import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.MessageEnvelope;
import io.spine.core.Version;
import io.spine.server.EventProducer;
import io.spine.server.event.EventFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Abstract base for method results that generate events.
 *
 * @author Alexander Yevsyukov
 */
public abstract class EventsResult extends MethodResult<Message> {

    private final EventProducer producer;

    /**
     * Creates a new results object.
     *
     * @param producer  the object on behalf of which to produce events
     * @param output    raw method output, cannot be {@code null}
     */
    protected EventsResult(EventProducer producer, Object output) {
        super(checkNotNull(output));
        this.producer = checkNotNull(producer);
    }

    /**
     * Transforms the messages of the result into a list of events.
     */
    public
    List<Event> produceEvents(MessageEnvelope origin) {
        List<? extends Message> messages = asMessages();
        List<Event> result =
                messages.stream()
                        .map(toEvent(origin))
                        .collect(toList());
        return result;
    }

    protected Function<Message, Event> toEvent(MessageEnvelope origin) {
        return new ToEvent(producer, origin);
    }

    /**
     * Converts an event message into an {@link Event}.
     */
    private static final class ToEvent implements Function<Message, Event> {

        private final EventFactory eventFactory;
        private final @Nullable Version version;

        private ToEvent(EventProducer producer,
                        MessageEnvelope origin) {
            this.eventFactory = EventFactory.on(origin, producer.getProducerId());
            this.version = producer.getVersion();
        }

        @Override
        public Event apply(Message message) {
            return eventFactory.createEvent(message, version);
        }
    }
}
