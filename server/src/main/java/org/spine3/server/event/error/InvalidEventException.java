/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event.error;

import com.google.protobuf.Message;
import org.spine3.base.Error;
import org.spine3.base.EventValidationError;
import org.spine3.base.ValidationError;
import org.spine3.server.type.EventClass;
import org.spine3.validate.ConstraintViolation;

/**
 * The exception for reporting invalid events.
 *
 * <p>An event is invalid if it's supported (there's a handler for the event), but it's
 * attributes are not populated according to framework conventions or validation constraints.
 *
 * @author Alexander Litus
 */
public class InvalidEventException extends EventException {

    private static final long serialVersionUID = 0L;

    private static final String MSG_VALIDATION_ERROR = "Event message does match validation constrains.";

    private InvalidEventException(String messageText, Message eventMsg, Error error) {
        super(messageText, eventMsg, error);
    }

    /**
     * Creates an exception instance for a event message, which has fields that violate validation constraint(s).
     *
     * @param eventMsg an invalid event message
     * @param violations constraint violations for the event message
     */
    public static InvalidEventException onConstraintViolations(Message eventMsg,
                                                                 Iterable<ConstraintViolation> violations) {
        final Error error = invalidEventMessageError(eventMsg, violations, MSG_VALIDATION_ERROR);
        @SuppressWarnings("DuplicateStringLiteralInspection")
        final String text = MSG_VALIDATION_ERROR + " Message class: " + EventClass.of(eventMsg) +
                " See Error.getValidationError() for details.";
        //TODO:2016-06-09:alexander.yevsyukov: Add more diagnostics on the validation problems discovered.
        return new InvalidEventException(text, eventMsg, error);
    }

    /**
     * Creates an instance of {@code Error} for a event message, which has fields that violate
     * validation constraint(s).
     */
    private static Error invalidEventMessageError(Message eventMessage,
            Iterable<ConstraintViolation> violations,
            String errorText) {
        final ValidationError validationError = ValidationError.newBuilder()
                .addAllConstraintViolation(violations)
                .build();
        final Error.Builder error = Error.newBuilder()
                .setType(EventValidationError.getDescriptor().getFullName())
                .setCode(EventValidationError.INVALID_EVENT.getNumber())
                .setValidationError(validationError)
                .setMessage(errorText)
                .putAllAttributes(eventTypeAttribute(eventMessage));
        return error.build();
    }
}
