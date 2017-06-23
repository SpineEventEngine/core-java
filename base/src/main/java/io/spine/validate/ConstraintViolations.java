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

package io.spine.validate;

import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import com.google.protobuf.Value;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.type.MessageClass;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;

/**
 * Utility class for working with {@link ConstraintViolation}s.
 *
 * @author Alexander Yevsyukov
 */
public class ConstraintViolations {

    private ConstraintViolations() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Returns a formatted string using the format string and parameters from the violation.
     *
     * @param violation violation which contains the format string and
     *                  arguments referenced by the format specifiers in it
     * @return a formatted string
     * @see String#format(String, Object...)
     */
    public static String toText(ConstraintViolation violation) {
        checkNotNull(violation);

        final String format = violation.getMsgFormat();
        final List<String> params = violation.getParamList();
        final String parentViolationFormatted = format(format, params.toArray());

        final StringBuilder resultBuilder = new StringBuilder(parentViolationFormatted);
        if (violation.getViolationCount() > 0) {
            resultBuilder.append(toText(violation.getViolationList()));
        }
        return resultBuilder.toString();
    }

    /**
     * Returns a formatted string using the format string and parameters from each of
     * the violations passed.
     *
     * @param violations violations which contain the format string and
     *                   arguments referenced by the format specifiers in each of them
     * @return a formatted string
     * @see #toText(ConstraintViolation)
     */
    public static String toText(Iterable<ConstraintViolation> violations) {
        checkNotNull(violations);

        final StringBuilder resultBuilder = new StringBuilder("Violation list:");

        for (ConstraintViolation childViolation : violations) {
            final String childViolationFormatted = toText(childViolation);
            resultBuilder.append(lineSeparator())
                         .append(childViolationFormatted);
        }
        return resultBuilder.toString();
    }

    /**
     * Returns a formatted string using the specified format string and parameters
     * from the violation.
     *
     * @param format    a format string
     * @param violation violation which contains arguments referenced by the format
     *                  specifiers in the format string
     * @return a formatted string
     * @see String#format(String, Object...)
     */
    public static String toText(String format, ConstraintViolation violation) {
        checkNotNull(format);
        checkNotNull(violation);

        final List<String> params = violation.getParamList();
        final String parentViolationFormatted = format(format, params.toArray());

        final StringBuilder resultBuilder = new StringBuilder(parentViolationFormatted);
        if (violation.getViolationCount() > 0) {
            resultBuilder.append(toText(format, violation.getViolationList()));
        }

        return resultBuilder.toString();
    }

    /**
     * Returns a formatted string using the specified format string and parameters from
     * each of the violations passed.
     *
     * @param format     a format string
     * @param violations violations which contain the arguments referenced by the format
     *                   specifiers in the format string
     * @return a formatted string
     * @see String#format(String, Object...)
     */
    public static String toText(String format, Iterable<ConstraintViolation> violations) {
        checkNotNull(format);
        checkNotNull(violations);

        final StringBuilder resultBuilder = new StringBuilder("Violations:");

        for (ConstraintViolation childViolation : violations) {
            final String childViolationFormatted = toText(format, childViolation);
            resultBuilder.append(lineSeparator())
                         .append(childViolationFormatted);
        }
        return resultBuilder.toString();
    }

    /**
     * A helper class for building exceptions used to report invalid {@code Message}s,
     * which have fields that violate validation constraint(s).
     *
     * @param <E> type of {@code Exception} to build
     * @param <M> type of the {@code Message}
     * @param <C> type of the {@linkplain MessageClass} of {@code |M|}.
     * @param <R> type of an error code to use for error reporting; must be a Protobuf enum value
     */
    @Internal
    public abstract static class ExceptionFactory<E extends Exception,
            M extends Message,
            C extends MessageClass,
            R extends ProtocolMessageEnum> {

        private final Iterable<ConstraintViolation> constraintViolations;
        private final M message;

        /**
         * Creates an {@code ExceptionFactory} instance for a given message and
         * constraint violations.
         *
         * @param message              an invalid event message
         * @param constraintViolations constraint violations for the event message
         */
        protected ExceptionFactory(M message,
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

        /**
         * Obtains an error text to use for error reporting.
         *
         * <p>This text will also be used as a base for an exception message to generate.
         */
        protected abstract String getErrorText();

        /**
         * Obtains the {@code Message}-specific type attributes for error reporting.
         */
        protected abstract Map<String, Value> getMessageTypeAttribute(Message message);

        /**
         * Defines the way to create an instance of exception, basing on the source {@code Message},
         * exception text and a generated {@code Error}.
         */
        protected abstract E createException(String exceptionMsg, M message, Error error);

        private String formatExceptionMessage() {
            return format("%s. Message class: %s. " +
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
            final String errorText = format("%s %s",
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
        public E newException() {
            return createException(formatExceptionMessage(), message, createError());
        }
    }
}
