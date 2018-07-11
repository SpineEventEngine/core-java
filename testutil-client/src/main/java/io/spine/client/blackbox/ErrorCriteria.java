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

package io.spine.client.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Value;
import io.spine.base.Error;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * An abstract predicate testing {@link Error Spine errors} to
 * match some simple rules.
 *
 * <p>These criterias are consumed by acks verifier
 * {@link AcknowledgementsVerifier#ackedWithErrors(ErrorCriteria) ackedWithError method}.
 *
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
public abstract class ErrorCriteria {

    /**
     * A message describing the criteria.
     *
     * <p>Used to report an assertion error.
     */
    public abstract String description();

    /**
     * Checks if the error matches the criteria.
     *
     * @param error a Spine Error instance that is part of a {@link io.spine.core.Ack ack response}
     * @return {@code true} if the error matches the criteria, {@code false} otherwise
     */
    public abstract boolean matches(Error error);

    /**
     * Verifies that the {@link Error#getType() errors type} matches the provided one.
     *
     * @param type a type that is to be matched in error
     * @return new {@link ErrorCriteria error criteria} instance
     */
    public static ErrorCriteria withType(String type) {
        checkNotNull(type);
        return new ErrorCriteria() {
            @Override
            public String description() {
                return format("Error type is %s", type);
            }

            @Override
            public boolean matches(Error error) {
                return type.equals(error.getType());
            }
        };
    }

    /**
     * Verifies that the {@link Error#getCode() errors code} matches the provided one.
     *
     * @param code a code that is to be matched in error
     * @return new {@link ErrorCriteria error criteria} instance
     */
    public static ErrorCriteria withCode(int code) {
        return new ErrorCriteria() {
            @Override
            public String description() {
                return format("Error code is \"%s\"", code);
            }

            @Override
            public boolean matches(Error error) {
                return code == error.getCode();
            }
        };
    }

    /**
     * Verifies that the {@link Error#getMessage() errors message} matches the provided one.
     *
     * @param message a message that is to be matched in error
     * @return new {@link ErrorCriteria error criteria} instance
     */
    public static ErrorCriteria withMessage(String message) {
        return new ErrorCriteria() {
            @Override
            public String description() {
                return format("Error contains following message: %s", message);
            }

            @Override
            public boolean matches(Error error) {
                return message.equals(error.getMessage());
            }
        };
    }

    /**
     * A static factory method for creating an {@link ErrorAttributeCriteria error attribute
     * criteria}.
     *
     * <p>An error attribute verifier checks that the error contains an
     * {@link Error#getAttributes() attribute} with a provided name.
     *
     * @param name name of an attribute which looked for by this criteria
     * @return a new {@link ErrorAttributeCriteria error attribute criteria} instance
     */
    @SuppressWarnings("ClassReferencesSubclass")
    public static ErrorAttributeCriteria withAttribute(String name) {
        return new ErrorAttributeCriteria(name);
    }

    /**
     * Verifies that the error does not contain {@link Error#getAttributesMap() an attribute}
     * with a provided name.
     *
     * @param name a name of an attribute that must be absent in error
     * @return new {@link ErrorCriteria error criteria} instance
     */
    public static ErrorCriteria withoutAttribute(String name) {
        return new ErrorCriteria() {
            @Override
            public String description() {
                return format("Error does not contain an attribute \"%s\"", name);
            }

            @Override
            public boolean matches(Error error) {
                Map<String, Value> attributes = error.getAttributesMap();
                return !attributes.containsKey(name);
            }
        };
    }
}
