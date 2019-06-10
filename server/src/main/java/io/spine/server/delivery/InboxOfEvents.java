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

package io.spine.server.delivery;

import io.spine.server.event.DuplicateEventException;
import io.spine.server.type.EventEnvelope;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * The part of {@link Inbox} responsible for processing incoming
 * {@link io.spine.server.type.EventEnvelope events}.
 *
 * @param <I>
 *         the type of identifier or inbox target entities
 */
class InboxOfEvents<I> extends InboxPart<I, EventEnvelope> {

    InboxOfEvents(Inbox.Builder<I> builder) {
        super(builder, builder.getEventEndpoints());
    }

    @Override
    protected void setRecordPayload(EventEnvelope envelope, InboxMessage.Builder builder) {
        builder.setEvent(envelope.outerObject());
    }

    @Override
    protected InboxMessageId inboxMsgIdFrom(EventEnvelope envelope) {
        String rawValue = envelope.id()
                                  .getValue();
        InboxMessageId result = InboxMessageId.newBuilder()
                                              .setValue(rawValue)
                                              .build();
        return result;
    }

    @Override
    protected EventEnvelope asEnvelope(InboxMessage message) {
        return EventEnvelope.of(message.getEvent());
    }

    @Override
    protected Dispatcher dispatcherWith(Collection<InboxMessage> deduplicationSource) {
        return new EventDispatcher(deduplicationSource);
    }

    /**
     * A strategy of event delivery from this {@code Inbox} to the event targets.
     */
    class EventDispatcher extends Dispatcher {

        private EventDispatcher(Collection<InboxMessage> deduplicationSource) {
            super(deduplicationSource);
        }

        @Override
        protected Predicate<? super InboxMessage> filterByType() {
            return (Predicate<InboxMessage>) InboxMessage::hasEvent;
        }


        @Override
        protected RuntimeException onDuplicateFound(InboxMessage duplicate) {
            return new DuplicateEventException(duplicate.getEvent());
        }
    }
}
