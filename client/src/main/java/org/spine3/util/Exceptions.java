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

package org.spine3.util;

import org.spine3.validate.ConversionError;
import org.spine3.validate.IllegalConversionArgumentException;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getRootCause;
import static java.lang.String.format;

/**
 * Utility class for working with exceptions for cases that are not
 * covered by {@link com.google.common.base.Throwables Throwables} class from Guava.
 *
 * @author Alexander Yevsyukov
 */
public class Exceptions {

    private Exceptions() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Always throws {@code UnsupportedOperationException} initialized with the passed string.
     *
     * <p>Use this method in combination with static import for brevity of code for
     * unsupported operations.
     * The return type is given to keep Java type system happy when called in methods with
     * return type as shown below:
     *
     * <pre>
     *   import static com.teamdev.commons.Exceptions.unsupported;
     *   ...
     *   T doSomething() {
     *      throw unsupported("Cannot do this");
     *   }
     * </pre>
     *
     * @param message a message for exception
     * @return nothing ever
     * @throws UnsupportedOperationException always
     */
    public static UnsupportedOperationException unsupported(String message) {
        checkNotNull(message);
        throw new UnsupportedOperationException(message);
    }

    /**
     * Always throws {@code UnsupportedOperationException}.
     *
     * <p>Use this method in combination with static import for brevity of code for
     * unsupported operations.
     * The return type is given to keep Java type system happy when called in methods with
     * return type as shown below:
     *
     * <pre>
     *   import static com.teamdev.commons.Exceptions.unsupported;
     *   ...
     *   T doSomething() {
     *      throw unsupported();
     *   }
     * </pre>
     *
     * @return nothing ever
     * @throws UnsupportedOperationException always
     */
    @SuppressWarnings("NewExceptionWithoutArguments") // No message necessary for this case.
    public static UnsupportedOperationException unsupported() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets a throwable's cause as the cause of a {@link IllegalStateException} and throws it.
     *
     * @param throwable to wrap
     * @return nothing ever, always throws an exception, the return type is for convenience
     * @throws IllegalStateException always
     */
    public static IllegalStateException wrappedCause(Throwable throwable) {
        checkNotNull(throwable);
        final Throwable cause = getRootCause(throwable);
        throw new IllegalStateException(cause);
    }

    private static String formatMessage(String format, Object[] args) {
        checkNotNull(format);
        checkNotNull(args);
        return format(Locale.ROOT, format, args);
    }

    /**
     * Throws {@code IllegalArgumentException} with the formatted string.
     *
     * @param format the format string
     * @param args formatting parameters
     * @return nothing ever, always throws an exception. The return type is given for convenience.
     * @throws IllegalArgumentException always
     */
    public static IllegalArgumentException newIllegalArgumentException(String format,
                                                                       Object... args) {
        final String errMsg = formatMessage(format, args);
        throw new IllegalArgumentException(errMsg);
    }

    /**
     * Throws {@code IllegalArgumentException} with the formatted string and the cause.
     *
     * @param cause the cause of the exception
     * @param format the format string
     * @param args formatting parameters
     * @return nothing ever, always throws an exception. The return type is given for convenience.
     * @throws IllegalArgumentException always
     */
    public static IllegalArgumentException newIllegalArgumentException(Throwable cause,
                                                                       String format,
                                                                       Object... args) {
        checkNotNull(cause);
        final String errMsg = formatMessage(format, args);
        throw new IllegalArgumentException(errMsg, cause);
    }

    /**
     * Throws {@code IllegalStateException} with the formatted string.
     *
     * @param format the format string
     * @param args formatting parameters
     * @return nothing ever, always throws an exception. The return type is given for convenience.
     * @throws IllegalStateException always
     */
    public static IllegalStateException newIllegalStateException(String format,
                                                                 Object... args) {
        final String errMsg = formatMessage(format, args);
        throw new IllegalStateException(errMsg);
    }

    /**
     * Throws {@code IllegalStateException} with the formatted string and the cause.
     *
     * @param cause the cause of the exception
     * @param format the format string
     * @param args formatting parameters
     * @return nothing ever, always throws an exception. The return type is given for convenience.
     * @throws IllegalStateException always
     */
    public static IllegalStateException newIllegalStateException(Throwable cause,
                                                                 String format,
                                                                 Object... args) {
        checkNotNull(cause);
        final String errMsg = formatMessage(format, args);
        throw new IllegalStateException(errMsg, cause);
    }

    /**
     * Creates {@link IllegalConversionArgumentException}
     * with specified exception message and throws it.
     *
     * @param exMessage a message for exception
     * @return always throws an exception, the return type is for convenience
     * @throws IllegalConversionArgumentException always
     */
    public static IllegalConversionArgumentException conversionArgumentException(String exMessage) {
        checkNotNull(exMessage);
        throw new IllegalConversionArgumentException(new ConversionError(exMessage));
    }
}
