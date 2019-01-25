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

package io.spine.server.inbox;

import io.spine.core.EventEnvelope;
import io.spine.server.event.DuplicateEventException;

import java.util.Optional;

/**
 * The part of {@link Inbox} responsible for processing incoming {@link io.spine.core.EventEnvelope
 * events}.
 *
 * @param <I>
 *         the type of identifier or inbox target entities
 */
class InboxOfEvents<I> extends InboxPart<I, EventEnvelope> {

    InboxOfEvents(EventEnvelope envelope, Inbox.Builder<I> builder, I entityId) {
        super(envelope, builder, builder.getEventEndpoints(), entityId);
    }

    @Override
    protected void setRecordPayload(EventEnvelope envelope, InboxMessageVBuilder builder) {
        builder.setEvent(envelope.getOuterObject());
    }

    @Override
    protected Optional<DuplicateEventException> checkDuplicates(InboxContentRecord contents) {
        EventEnvelope envelope = getEnvelope();
        boolean hasDuplicate = contents.getMessageList()
                                       .stream()
                                       .filter(InboxMessage::hasEvent)
                                       .anyMatch(m -> envelope.getId()
                                                              .equals(m.getEvent()
                                                                       .getId()));
        if (hasDuplicate) {
            DuplicateEventException exception =
                    new DuplicateEventException(envelope.getOuterObject());
            return Optional.of(exception);
        }
        return Optional.empty();
    }
}
