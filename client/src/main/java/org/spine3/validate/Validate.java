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

package org.spine3.validate;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandId;
import org.spine3.base.EventId;
import org.spine3.protobuf.TypeName;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.spine3.base.Identifiers.EMPTY_ID;
import static org.spine3.base.Identifiers.idToString;

/**
 * This class provides general validation routines.
 *
 * @author Alexander Yevsyukov
 */
public class Validate {

    private static final String MUST_BE_A_POSITIVE_VALUE = "%s must be a positive value";
    private static final String MUST_BE_IN_BOUNDS = "%s should be in bounds of %d and %d values inclusive. Found: %d";

    private Validate() {
    }

    /**
     * Verifies if the passed message object is its default state and is not {@code null}.
     *
     * @param object the message to inspect
     * @return true if the message is in the default state, false otherwise
     */
    public static boolean isDefault(Message object) {
        checkNotNull(object);
        final boolean result = object.getDefaultInstanceForType()
                                     .equals(object);
        return result;
    }

    /**
     * Verifies if the passed message object is not its default state and is not {@code null}.
     *
     * @param object the message to inspect
     * @return true if the message is not in the default state, false otherwise
     */
    public static boolean isNotDefault(Message object) {
        checkNotNull(object);
        final boolean result = !isDefault(object);
        return result;
    }

    /**
     * Ensures that the passed object is not in its default state and is not {@code null}.
     *
     * @param object       the {@code Message} instance to check
     * @param errorMessage the message for the exception to be thrown;
     *                     will be converted to a string using {@link String#valueOf(Object)}
     * @throws IllegalStateException if the object is in its default state
     */
    public static <M extends Message> M checkNotDefault(M object, @Nullable Object errorMessage) {
        checkNotNull(object);
        checkState(isNotDefault(object), errorMessage);
        return object;
    }

    /**
     * Ensures that the passed object is not in its default state and is not {@code null}.
     *
     * @param object               the {@code Message} instance to check
     * @param errorMessageTemplate a template for the exception message should the check fail
     * @param errorMessageArgs     the arguments to be substituted into the message template
     * @throws IllegalStateException if the object is in its default state
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    public static <M extends Message> M checkNotDefault(M object,
                                                        String errorMessageTemplate,
                                                        Object... errorMessageArgs) {
        checkNotNull(object);
        checkState(isNotDefault(object), errorMessageTemplate, errorMessageArgs);
        return object;
    }

    /**
     * Ensures that the passed object is not in its default state and is not {@code null}.
     *
     * @param object the {@code Message} instance to check
     * @throws IllegalStateException if the object is in its default state
     */
    public static <M extends Message> M checkNotDefault(M object) {
        checkNotNull(object);
        checkNotDefault(object, "The message is in the default state: %s", TypeName.of(object));
        return object;
    }

    /**
     * Ensures that the passed object is in its default state and is not {@code null}.
     *
     * @param object       the {@code Message} instance to check
     * @param errorMessage the message for the exception to be thrown;
     *                     will be converted to a string using {@link String#valueOf(Object)}
     * @throws IllegalStateException if the object is not in its default state
     */
    public static <M extends Message> M checkDefault(M object, @Nullable Object errorMessage) {
        checkNotNull(object);
        checkState(isDefault(object), errorMessage);
        return object;
    }

    /**
     * Ensures that the passed object is in its default state and is not {@code null}.
     *
     * @param object               the {@code Message} instance to check
     * @param errorMessageTemplate a template for the exception message should the check fail
     * @param errorMessageArgs     the arguments to be substituted into the message template
     * @throws IllegalStateException if the object is not in its default state
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    public static <M extends Message> M checkDefault(M object,
                                                     String errorMessageTemplate,
                                                     Object... errorMessageArgs) {
        checkNotNull(object);
        checkState(isDefault(object), errorMessageTemplate, errorMessageArgs);
        return object;
    }

    /**
     * Ensures that the passed object is in its default state and is not {@code null}.
     *
     * @param object the {@code Message} instance to check
     * @throws IllegalStateException if the object is not in its default state
     */
    public static <M extends Message> M checkDefault(M object) {
        checkNotNull(object);
        if (!isDefault(object)) {
            final String typeName = TypeName.of(object);
            final String errorMessage = "The message is not in the default state: " + typeName;
            throw new IllegalStateException(errorMessage);
        }
        return object;
    }

    /**
     * Ensures the truth of an expression involving one parameter to the calling method.
     *
     * @param expression         a boolean expression with the parameter we check
     * @param parameterName      the name of the parameter
     * @param errorMessageFormat the format of the error message, which has {@code %s} placeholder for
     *                           the parameter name
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkParameter(boolean expression, String parameterName, String errorMessageFormat) {
        if (!expression) {
            final String errorMessage = String.format(errorMessageFormat, parameterName);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Ensures that the passed string is not {@code null}, empty or blank string.
     *
     * @param stringToCheck the string to check
     * @param fieldName     the name of the string field
     * @return the passed string
     * @throws IllegalArgumentException if the string is empty or blank
     */
    public static String checkNotEmptyOrBlank(String stringToCheck, String fieldName) {
        checkNotNull(stringToCheck, fieldName + " must not be null.");
        checkParameter(!stringToCheck.isEmpty(), fieldName, "%s must not be an empty string.");
        final String trimmed = stringToCheck.trim();
        checkParameter(trimmed.length() > 0, fieldName, "%s must not be a blank string.");
        return stringToCheck;
    }

    /**
     * Ensures that the passed timestamp:
     * <ul>
     * <li>is not {@code null};
     * <li>{@code seconds} > 0;
     * <li>{@code nanos} >= 0.
     * </ul>
     *
     * @param timestamp    the timestamp to check
     * @param argumentName the name of the checked value to be used in the error message
     * @return the passed timestamp
     * @throws IllegalArgumentException if any of the requirements are not met
     */
    public static Timestamp checkPositive(Timestamp timestamp, String argumentName) {
        checkNotNull(timestamp, argumentName);
        checkParameter(timestamp.getSeconds() > 0, argumentName, "%s must have a positive number of seconds.");
        checkParameter(timestamp.getNanos() >= 0, argumentName, "%s must not have a negative number of nanoseconds.");
        return timestamp;
    }

    /**
     * Ensures that the passed value is positive.
     *
     * @param value the value to check
     * @throws IllegalArgumentException if requirement is not met
     */
    public static void checkPositive(long value) {
        checkPositive(value, "");
    }

    /**
     * Ensures that the passed value is positive.
     *
     * @param value        the value to check
     * @param argumentName the name of the checked value to be used in the error message
     * @throws IllegalArgumentException if requirement is not met
     */
    public static void checkPositive(long value, String argumentName) {
        checkParameter(value > 0L, argumentName, MUST_BE_A_POSITIVE_VALUE);
    }

    /**
     * Ensures that the passed value is positive or zero.
     *
     * @param value the value to check
     * @throws IllegalArgumentException if requirement is not met
     */
    public static void checkPositiveOrZero(long value) {
        checkArgument(value >= 0);
    }

    /**
     * Ensures that target value is in between passed bounds.
     *
     * @param value     target value
     * @param paramName value name
     * @param lowBound  lower bound to check
     * @param highBound higher bound
     */
    public static void checkBounds(int value, String paramName, int lowBound, int highBound) {
        if (!isBetween(value, lowBound, highBound)) {
            final String errMsg = String.format(MUST_BE_IN_BOUNDS,
                                                paramName, lowBound, highBound, value);
            throw new IllegalArgumentException(errMsg);
        }
    }

    private static boolean isBetween(int value, int lowBound, int highBound) {
        return lowBound <= value && value <= highBound;
    }

    /**
     * Ensures that the passed ID is valid.
     *
     * @param id an ID to check
     * @throws IllegalArgumentException if the ID string value is empty or blank
     */
    public static EventId checkValid(EventId id) {
        checkNotNull(id);
        checkNotEmptyOrBlank(id.getUuid(), "event ID");
        return id;
    }

    /**
     * Ensures that the passed ID is valid.
     *
     * @param id an ID to check
     * @throws IllegalArgumentException if the ID string value is empty or blank
     */
    public static CommandId checkValid(CommandId id) {
        checkNotNull(id);
        final String idStr = idToString(id);
        checkArgument(!idStr.equals(EMPTY_ID), "Command ID must not be an empty string.");
        return id;
    }
}
