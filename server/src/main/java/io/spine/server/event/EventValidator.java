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

package io.spine.server.event;

import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.MessageInvalid;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.MessageValidator;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.event.InvalidEventException.onConstraintViolations;
import static java.util.Optional.ofNullable;

/**
 * The {@link EventEnvelope} validator.
 *
 * <p>Checks if the message of the passed event is {@linkplain MessageValidator#validate(Message)
 * valid}.
 *
 * @author Dmytro Dashenkov
 */
final class EventValidator implements EnvelopeValidator<EventEnvelope> {

    private final MessageValidator messageValidator;

    EventValidator(MessageValidator messageValidator) {
        this.messageValidator = messageValidator;
    }

    @Override
    public Optional<MessageInvalid> validate(EventEnvelope envelope) {
        checkNotNull(envelope);

        final Event event = envelope.getOuterObject();
        MessageInvalid result = null;
        final List<ConstraintViolation> violations = messageValidator.validate(event);
        if (!violations.isEmpty()) {
            result = onConstraintViolations(event, violations);
        }
        return ofNullable(result);
    }
}
