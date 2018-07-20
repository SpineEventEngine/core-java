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

package io.spine.server.entity;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.Value;
import io.spine.base.Error;
import io.spine.protobuf.AnyPacker;
import io.spine.type.TypeName;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ConstraintViolations.ExceptionFactory;

import java.util.Map;

import static io.spine.server.entity.EntityStateValidationError.INVALID_ENTITY_STATE;

/**
 * Signals that an entity state does not pass {@linkplain AbstractEntity#validate(Message)
 * validation}.
 *
 * @author Dmytro Grankin
 */
public final class InvalidEntityStateException extends RuntimeException {

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

    private InvalidEntityStateException(String messageText, Message entityState, Error error) {
        super(messageText);
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
    public static InvalidEntityStateException onConstraintViolations(
            Message entityState, Iterable<ConstraintViolation> violations) {
        ConstraintViolationExceptionFactory helper = new ConstraintViolationExceptionFactory(
                entityState, violations);
        return helper.newException();
    }

    /**
     * Returns a related event message.
     */
    public Message getEntityState() {
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
    public Error getError() {
        return error;
    }

    /**
     * A helper utility aimed to create an {@code InvalidEntityStateException} to report the
     * entity state which field values violate validation constraint(s).
     */
    private static class ConstraintViolationExceptionFactory
            extends ExceptionFactory<InvalidEntityStateException,
            Message,
            EntityStateClass,
            EntityStateValidationError> {

        /**
         * The name of the attribute of the entity state type reported in an error.
         *
         * @see #getMessageTypeAttribute(Message)
         * @see Error
         */
        private static final String ATTR_ENTITY_STATE_TYPE_NAME = "entityStateType";

        private final EntityStateClass entityStateClass;

        private ConstraintViolationExceptionFactory(Message entityState,
                                                    Iterable<ConstraintViolation> violations) {
            super(entityState, violations);
            this.entityStateClass = EntityStateClass.of(entityState);
        }

        @Override
        protected EntityStateClass getMessageClass() {
            return entityStateClass;
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
         * @param entityState the entity state to get the type from
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
        protected InvalidEntityStateException createException(
                String exceptionMsg, Message entityState, Error error) {
            return new InvalidEntityStateException(exceptionMsg, entityState, error);
        }
    }
}
