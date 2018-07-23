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
import com.google.protobuf.Value;
import io.spine.base.Error;
import io.spine.core.EventClass;
import io.spine.core.EventValidationError;
import io.spine.core.MessageInvalid;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ConstraintViolations.ExceptionFactory;

import java.util.Map;

/**
 * The exception for reporting invalid events.
 *
 * <p>An event is invalid if it's supported (there's a handler for the event), but it's
 * attributes are not populated according to framework conventions or validation constraints.
 *
 * @author Alexander Litus
 */
public class InvalidEventException extends EventException implements MessageInvalid {

    private static final long serialVersionUID = 0L;

    private static final String MSG_VALIDATION_ERROR = "Event message does not match " +
                                                       "the validation constraints.";

    private InvalidEventException(String messageText, Message eventMsg, Error error) {
        super(messageText, eventMsg, error);
    }

    /**
     * Creates an exception instance for a event message, which has fields that
     * violate validation constraint(s).
     *
     * @param eventMsg   an invalid event message
     * @param violations constraint violations for the event message
     */
    public static InvalidEventException onConstraintViolations(
            Message eventMsg, Iterable<ConstraintViolation> violations) {

        ConstraintViolationExceptionFactory helper = new ConstraintViolationExceptionFactory(
                eventMsg, violations);
        return helper.newException();
    }

    /**
     * A helper utility aimed to create an {@code InvalidEventException} to report the
     * event which field values violate validation constraint(s).
     */
    private static class ConstraintViolationExceptionFactory
                                extends ExceptionFactory<InvalidEventException,
                                                         Message,
                                                         EventClass,
                                                         EventValidationError> {

        private final EventClass eventClass;

        private ConstraintViolationExceptionFactory(Message eventMsg,
                                                    Iterable<ConstraintViolation> violations) {
            super(eventMsg, violations);
            this.eventClass = EventClass.of(eventMsg);
        }

        @Override
        protected EventClass getMessageClass() {
            return eventClass;
        }

        @Override
        protected EventValidationError getErrorCode() {
            return EventValidationError.INVALID_EVENT;
        }

        @Override
        protected String getErrorText() {
            return MSG_VALIDATION_ERROR;
        }

        @Override
        protected Map<String, Value> getMessageTypeAttribute(Message message) {
            return eventTypeAttribute(message);
        }

        @Override
        protected InvalidEventException createException(String exceptionMsg,
                                                        Message event,
                                                        Error error) {
            return new InvalidEventException(exceptionMsg, event, error);
        }
    }
}
