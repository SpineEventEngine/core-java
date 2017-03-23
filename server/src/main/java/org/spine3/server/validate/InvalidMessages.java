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
package org.spine3.server.validate;

import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import com.google.protobuf.Value;
import org.spine3.base.Error;
import org.spine3.annotations.Internal;
import org.spine3.base.ValidationError;
import org.spine3.type.MessageClass;
import org.spine3.validate.ConstraintViolation;

import java.util.Map;

import static org.spine3.validate.ConstraintViolations.toText;

/**
 * A utility class for working with invalid {@code Message}s, such as
 * {@linkplain org.spine3.base.Command commands} and {@linkplain org.spine3.base.Event events}.
 *
 * @author Alex Tymchenko
 */
@Internal
public class InvalidMessages {

    private InvalidMessages() {
        // Prevent this utility class from instantiation.
    }

    /**
     * A helper class for building exceptions used to report invalid {@code Message}s,
     * which have fields that violate validation constraint(s).
     *
     * @param <E> type of {@code Exception} to build
     * @param <M> type of the {@code Message}
     * @param <C> type of the {@linkplain MessageClass MessageClass} of {@code |M|}.
     * @param <R> type of an error code to use for error reporting; must be a Protobuf enum value
     */
    public abstract static class ConstraintViolationHelper<E extends Exception,
                                                           M extends Message,
                                                           C extends MessageClass,
                                                           R extends ProtocolMessageEnum> {

        private final Iterable<ConstraintViolation> constraintViolations;
        private final M message;

        /**
         * Creates an {@code ConstraintViolationHelper} instance for a given message and
         * constraint violations,
         *
         * @param message              an invalid event message
         * @param constraintViolations constraint violations for the event message
         */
        protected ConstraintViolationHelper(M message,
                                   Iterable<ConstraintViolation> constraintViolations) {
            this.constraintViolations = constraintViolations;
            this.message = message;
        }

        /**
         * Obtains a {@code MessageClass} for an invalid {@code Message}.
         */
        protected abstract C getMessageClass();

        /**
         * Obtains an error code to use for error reporting.
         */
        protected abstract R getErrorCode();

        protected abstract String getErrorText();


        protected abstract Map<String, Value> getMessageTypeAttribute(Message message);

        protected abstract E createException(String exceptionMsg, M message, Error error);

        private String formatExceptionMessage() {
            return String.format("%s. Message class: %s. " +
                                 "See Error.getValidationError() for details.",
                          getErrorText(), getMessageClass());
        }

        private Error createError() {
            final ValidationError validationError =
                    ValidationError.newBuilder()
                                   .addAllConstraintViolation(constraintViolations)
                                   .build();
            final R errorCode = getErrorCode();
            final String typeName = errorCode.getDescriptorForType()
                                             .getFullName();
            final String errorTextTemplate = getErrorText();
            final String errorText = String.format("%s %s",
                                                   errorTextTemplate,
                                                   toText(constraintViolations));

            final Error.Builder error = Error.newBuilder()
                                             .setType(typeName)
                                             .setCode(errorCode.getNumber())
                                             .setValidationError(validationError)
                                             .setMessage(errorText)
                                             .putAllAttributes(getMessageTypeAttribute(message));
            return error.build();
        }

        /**
         * Creates an exception instance for an invalid message, which has fields that
         * violate validation constraint(s).
         */
        public E buildException() {
            return createException(formatExceptionMessage(), message, createError());
        }
    }
}
