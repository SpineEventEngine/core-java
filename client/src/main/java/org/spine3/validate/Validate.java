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

package org.spine3.validate;

import com.google.protobuf.Message;
import org.spine3.base.CommandId;
import org.spine3.base.EventId;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.spine3.base.Identifiers.EMPTY_ID;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.util.Exceptions.newIllegalArgumentException;
import static org.spine3.util.Exceptions.newIllegalStateException;

/**
 * This class provides general validation routines.
 *
 * @author Alexander Yevsyukov
 */
public class Validate {

    private static final String MUST_BE_A_POSITIVE_VALUE = "%s must be a positive value";

    private Validate() {
        // Prevent instantiation of this utility class.
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
        checkNotNull(errorMessageTemplate);
        checkNotNull(errorMessageArgs);
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
        checkNotDefault(object,
                        "The message is in the default state: %s",
                        TypeName.of(object));
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
        checkNotNull(errorMessageTemplate);
        checkNotNull(errorMessageArgs);
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
            final String typeName = TypeName.of(object).value();
            throw newIllegalStateException("The message is not in the default state: %s", typeName);
        }
        return object;
    }

    /**
     * Ensures the truth of an expression involving one parameter to the calling method.
     *
     * @param expression         a boolean expression with the parameter we check
     * @param errorMessageFormat the format of the error message, which has {@code %s} placeholder for
     *                           the parameter name
     * @param parameterName      the name of the parameter
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkParameter(boolean expression,
                                      String errorMessageFormat,
                                      String parameterName) {
        checkNotNull(errorMessageFormat);
        checkNotNull(parameterName);
        if (!expression) {
            throw newIllegalArgumentException(errorMessageFormat, parameterName);
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
        checkNotNull(stringToCheck);
        checkNotNull(fieldName);
        checkParameter(!stringToCheck.isEmpty(),
                       "Field %s must not be an empty string.", fieldName
        );
        final String trimmed = stringToCheck.trim();
        checkParameter(trimmed.length() > 0,
                       "Field %s must not be a blank string.", fieldName
        );
        return stringToCheck;
    }

    /**
     * Ensures that the passed value is positive.
     *
     * @param value the value to check
     * @throws IllegalArgumentException if requirement is not met
     */
    public static void checkPositive(long value) {
        if (value <= 0) {
            throw newIllegalArgumentException("value (%d) must be positive", value);
        }
    }

    /**
     * Ensures that the passed value is positive.
     *
     * @param value        the value to check
     * @param argumentName the name of the checked value to be used in the error message
     * @throws IllegalArgumentException if requirement is not met
     */
    public static void checkPositive(long value, String argumentName) {
        checkNotNull(argumentName);
        checkParameter(value > 0L, MUST_BE_A_POSITIVE_VALUE, argumentName);
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
        checkNotNull(paramName);
        if (!isBetween(value, lowBound, highBound)) {
            throw newIllegalArgumentException("%s (%d) should be in bounds [%d, %d] inclusive",
                                              paramName, value, lowBound, highBound);
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

    /**
     * Ensures that the passed name is not empty or blank.
     *
     * @param name the name to check
     * @return the passed value
     * @throws IllegalArgumentException if the ID string value is empty or blank
     */
    @SuppressWarnings("DuplicateStringLiteralInspection") // is OK for this popular field name value.
    public static String checkNameNotEmptyOrBlank(String name) {
        return checkNotEmptyOrBlank(name, "name");
    }
}
