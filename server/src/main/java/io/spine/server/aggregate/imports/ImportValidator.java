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

package io.spine.server.aggregate.imports;

import com.google.protobuf.Message;
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
 * Checks if a message of the event to import is
 * {@linkplain MessageValidator#validate(Message) valid}
 *
 * @author Alexander Yevsyukov
 */
final class ImportValidator implements EnvelopeValidator<EventEnvelope> {

    private final MessageValidator messageValidator = MessageValidator.newInstance();

    @Override
    public Optional<MessageInvalid> validate(EventEnvelope envelope) {
        checkNotNull(envelope);
        MessageInvalid result = null;
        Message eventMessage = envelope.getMessage();
        List<ConstraintViolation> violations = messageValidator.validate(eventMessage);
        if (!violations.isEmpty()) {
            result = onConstraintViolations(eventMessage, violations);
        }
        return ofNullable(result);
    }
}
