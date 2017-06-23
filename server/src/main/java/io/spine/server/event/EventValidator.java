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

package io.spine.server.event;

import com.google.common.base.Optional;
import io.spine.base.Event;
import io.spine.base.EventClass;
import io.spine.envelope.EventEnvelope;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.MessageValidator;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.transport.Statuses.invalidArgumentWithCause;

/**
 * @author Dmytro Dashenkov
 */
final class EventValidator implements EnvelopeValidator<EventEnvelope> {

    private final MessageValidator messageValidator;

    private final EventBus eventBus;

    EventValidator(MessageValidator messageValidator, EventBus eventBus) {
        this.messageValidator = messageValidator;
        this.eventBus = eventBus;
    }

    @Override
    public Optional<Throwable> validate(EventEnvelope envelope) {
        checkNotNull(envelope);

        final Event event = envelope.getOuterObject();
        final EventClass eventClass = EventClass.of(event);
        Throwable result = null;
        if (eventBus.isUnsupportedEvent(eventClass)) {
            final UnsupportedEventException unsupportedEvent = new UnsupportedEventException(event);
            result = invalidArgumentWithCause(unsupportedEvent, unsupportedEvent.getError());
        }
        final List<ConstraintViolation> violations = messageValidator.validate(event);
        if (!violations.isEmpty()) {
            final InvalidEventException invalidEvent =
                    InvalidEventException.onConstraintViolations(event, violations);
            result = invalidArgumentWithCause(invalidEvent, invalidEvent.getError());
        }
        return Optional.fromNullable(result);
    }
}
