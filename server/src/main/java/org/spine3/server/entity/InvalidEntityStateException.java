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

import com.google.protobuf.Message;
import org.spine3.base.Error;
import org.spine3.base.ValidationError;
import org.spine3.validate.ConstraintViolation;
import org.spine3.validate.ConstraintViolations;

import static java.lang.String.format;
import static org.spine3.server.entity.EntityStateValidationError.INVALID_ENTITY_STATE;

/**
 * Signals that an entity state does not pass {@linkplain AbstractEntity#validate(Message)
 * validation}.
 *
 * @author Dmytro Grankin
 */
public class InvalidEntityStateException extends EntityStateException {

    private static final long serialVersionUID = 0L;

    private static final String MSG_VALIDATION_ERROR =
            "Entity state does match the validation constraints. ";

    private InvalidEntityStateException(String messageText, Message entityState, Error error) {
        super(messageText, entityState, error);
    }

    /**
     * Creates an exception instance for an entity state, which has fields that
     * violate validation constraint(s).
     *
     * @param entityState the invalid entity state
     * @param violations  the constraint violations for the entity state
     */
    public static InvalidEntityStateException onConstraintViolations(
            Message entityState, Iterable<ConstraintViolation> violations) {
        final String errorDetails = MSG_VALIDATION_ERROR + ConstraintViolations.toText(violations);
        final Error error = invalidEventMessageError(entityState, violations, errorDetails);
        final String text = format(
                "%sEntity state class: %s. See Error.getValidationError() for details.",
                MSG_VALIDATION_ERROR, EntityStateClass.of(entityState));
        return new InvalidEntityStateException(text, entityState, error);
    }

    /**
     * Creates an instance of {@code Error} for an entity state,
     * which has fields that violate validation constraint(s).
     */
    private static Error invalidEventMessageError(Message entityState,
                                                  Iterable<ConstraintViolation> violations,
                                                  String errorText) {
        final ValidationError validationError = ValidationError.newBuilder()
                                                               .addAllConstraintViolation(
                                                                       violations)
                                                               .build();
        final Error.Builder error = Error.newBuilder()
                                         .setType(EntityStateValidationError.getDescriptor()
                                                                            .getFullName())
                                         .setCode(INVALID_ENTITY_STATE.getNumber())
                                         .setValidationError(validationError)
                                         .setMessage(errorText)
                                         .putAllAttributes(entityStateTypeAttribute(entityState));
        return error.build();
    }
}
