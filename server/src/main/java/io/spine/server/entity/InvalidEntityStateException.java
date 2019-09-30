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

package io.spine.server.entity;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.Value;
import io.spine.base.Error;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.model.StateClass;
import io.spine.type.TypeName;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ExceptionFactory;
import io.spine.validate.ValidationException;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static io.spine.server.entity.EntityStateValidationError.INVALID_ENTITY_STATE;

/**
 * Signals that an entity state does not pass validation.
 */
public final class InvalidEntityStateException extends ValidationException {

    private static final long serialVersionUID = 0L;

    private static final String MSG_VALIDATION_ERROR =
            "Entity state does match the validation constraints";

    /**
     * The entity state or the message packed into {@link Any}.
     *
     * <p>We use {@link GeneratedMessageV3} (not {@code Message}) because
     * it is {@link java.io.Serializable Serializable}.
     */
    private final GeneratedMessageV3 entityState;

    /**
     * The error passed with the exception.
     */
    private final Error error;

    private InvalidEntityStateException(Message entityState, Error error) {
        super(error.getValidationError()
                   .getConstraintViolationList());
        this.entityState = entityState instanceof GeneratedMessageV3
                           ? (GeneratedMessageV3) entityState
                           : AnyPacker.pack(entityState);
        this.error = error;
    }

    /**
     * Creates an exception instance for an entity state, which has fields that
     * violate validation constraint(s).
     *
     * @param entityState the invalid entity state
     * @param violations  the constraint violations for the entity state
     */
    public static InvalidEntityStateException
    onConstraintViolations(Message entityState, Iterable<ConstraintViolation> violations) {
        Factory factory = new Factory(entityState, violations);
        return factory.newException();
    }

    /**
     * Returns a related event message.
     */
    Message entityState() {
        if (entityState instanceof Any) {
            Any any = (Any) entityState;
            Message unpacked = AnyPacker.unpack(any);
            return unpacked;
        }
        return entityState;
    }

    /**
     * Returns an error occurred.
     */
    public Error error() {
        return error;
    }

    /**
     * A helper utility aimed to create an {@code InvalidEntityStateException} to report the
     * entity state which field values violate validation constraint(s).
     */
    private static final class Factory
            extends ExceptionFactory<InvalidEntityStateException,
                                     Message,
                                     StateClass,
                                     EntityStateValidationError> {

        /**
         * The name of the attribute of the entity state type reported in an error.
         *
         * @see #getMessageTypeAttribute(Message)
         * @see Error
         */
        private static final String ATTR_ENTITY_STATE_TYPE_NAME = "entityStateType";

        private final StateClass stateClass;

        private Factory(Message entityState,
                        Iterable<ConstraintViolation> violations) {
            super(entityState, violations);
            this.stateClass = StateClass.of(entityState);
        }

        @Override
        protected StateClass getMessageClass() {
            return stateClass;
        }

        @Override
        protected EntityStateValidationError getErrorCode() {
            return INVALID_ENTITY_STATE;
        }

        @Override
        protected String getErrorText() {
            return MSG_VALIDATION_ERROR;
        }

        /**
         * Returns a map with an entity state type attribute.
         *
         * @param entityState
         *         the entity state to get the type from
         */
        @Override
        protected Map<String, Value> getMessageTypeAttribute(Message entityState) {
            String entityStateType = TypeName.of(entityState)
                                             .value();
            Value value = Value.newBuilder()
                               .setStringValue(entityStateType)
                               .build();
            return ImmutableMap.of(ATTR_ENTITY_STATE_TYPE_NAME, value);
        }

        @Override
        protected InvalidEntityStateException
        createException(String exceptionMsg, Message entityState, Error error) {
            List<ConstraintViolation> violations = error.getValidationError()
                                                        .getConstraintViolationList();
            checkArgument(!violations.isEmpty(), "No constraint violations provided.");
            return new InvalidEntityStateException(entityState, error);
        }
    }
}