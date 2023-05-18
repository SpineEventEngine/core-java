/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.server.MessageInvalid;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.type.EventEnvelope;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.Validate;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.event.InvalidEventException.onConstraintViolations;

/**
 * Checks if the message of the passed event is valid.
 */
final class EventValidator implements EnvelopeValidator<EventEnvelope> {

    @Override
    public Optional<MessageInvalid> validate(EventEnvelope event) {
        checkNotNull(event);
        Event outerObject = event.outerObject();
        List<ConstraintViolation> violations = Validate.violationsOf(outerObject);
        if (violations.isEmpty()) {
            return Optional.empty();
        }
        EventMessage message = event.message();
        MessageInvalid result = onConstraintViolations(message, violations);
        return Optional.of(result);
    }
}
