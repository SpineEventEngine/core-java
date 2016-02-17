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

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;

/**
 * This class provides general validation routines.
 *
 * @author Alexanxer Yevsyukov
 */
public class Validate {

    private Validate() {}


    /**
     * Verifies if the passed message object is its default state.
     * @param object the message to inspect
     *
     * @return true if the message is in the default state, false otherwise
     */
    public static boolean isDefault(Message object) {
        return object.getDefaultInstanceForType().equals(object);
    }

    /**
     * Verifies if the passed message object is not its default state.
     * @param object the message to inspect
     *
     * @return true if the message is not in the default state, false otherwise
     */
    public static boolean isNotDefault(Message object) {
        return !isDefault(object);
    }

    /**
     * Ensures that the passed object is not in its default state.
     *
     * @param object the {@code Message} instance to check
     * @param errorMessage the message for the exception to be thrown;
     *                     will be converted to a string using {@link String#valueOf(Object)}
     * @throws IllegalStateException if the object is in its default state
     */
    public static <M extends Message> M checkNotDefault(M object, @Nullable Object errorMessage) {
        checkState(isNotDefault(object), errorMessage);
        return object;
    }

    /**
     * Ensures that the passed object is not in its default state.
     *
     * @param object the {@code Message} instance to check
     * @param errorMessageTemplate a template for the exception message should the check fail
     * @param errorMessageArgs the arguments to be substituted into the message template
     * @throws IllegalStateException if the object is in its default state
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    public static <M extends Message> M checkNotDefault(M object, String errorMessageTemplate, Object... errorMessageArgs) {
        checkState(isNotDefault(object), errorMessageTemplate, errorMessageArgs);
        return object;
    }

    /**
     * Ensures that the passed object is not in its default state.
     *
     * @param object the {@code Message} instance to check
     * @throws IllegalStateException if the object is in its default state
     */
    public static <M extends Message> M checkNotDefault(M object) {
        checkNotDefault(object, "The message is in the default state: %s", object);
        return object;
    }

    /**
     * Ensures that the passed object is in its default state.
     *
     * @param object the {@code Message} instance to check
     * @param errorMessage the message for the exception to be thrown;
     *                     will be converted to a string using {@link String#valueOf(Object)}
     * @throws IllegalStateException if the object is not in its default state
     */
    public static <M extends Message> M checkDefault(M object, @Nullable Object errorMessage) {
        checkState(isDefault(object), errorMessage);
        return object;
    }

    /**
     * Ensures that the passed object is in its default state.
     *
     * @param object the {@code Message} instance to check
     * @param errorMessageTemplate a template for the exception message should the check fail
     * @param errorMessageArgs the arguments to be substituted into the message template
     * @throws IllegalStateException if the object is not in its default state
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    public static <M extends Message> M checkDefault(M object, String errorMessageTemplate,
                                                     Object... errorMessageArgs) {
        checkState(isDefault(object), errorMessageTemplate, errorMessageArgs);
        return object;
    }

    /**
     * Ensures that the passed object is in its default state.
     *
     * @param object the {@code Message} instance to check
     * @throws IllegalStateException if the object is not in its default state
     */
    public static <M extends Message> M checkDefault(M object) {
        checkDefault(object, "The message is not in the default state: %s", object);
        return object;
    }
}
