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

package io.spine.server.integration;

import com.google.protobuf.Value;
import io.spine.base.Error;

import java.util.Map;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Mykhailo Drachuk
 */
public abstract class ErrorQualifier implements Predicate<Error> {

    protected abstract String description();

    public ErrorQualifier and(ErrorQualifier other) {
        checkNotNull(other);
        ErrorQualifier current = this;
        return new ErrorQualifier() {
            @Override
            protected String description() {
                return String.format("%s. %s", current.description(), other.description());
            }

            @Override
            public boolean test(Error error) {
                return current.test(error) && other.test(error);
            }
        };
    }

    public static ErrorQualifier withType(String type) {
        checkNotNull(type);
        return new ErrorQualifier() {
            @Override
            protected String description() {
                return format("Error type is %s", type);
            }

            @Override
            public boolean test(Error error) {
                return type.equals(error.getType());
            }
        };
    }

    public static ErrorQualifier withCode(int code) {
        return new ErrorQualifier() {
            @Override
            protected String description() {
                return format("Error code is \"%s\"", code);
            }

            @Override
            public boolean test(Error error) {
                return code == error.getCode();
            }
        };
    }

    public static ErrorQualifier withMessage(String message) {
        return new ErrorQualifier() {
            @Override
            protected String description() {
                return format("Error contains following message: %s", message);
            }

            @Override
            public boolean test(Error error) {
                return message.equals(error.getMessage());
            }
        };
    }

    public static ErrorQualifier withoutAttribute(String name) {
        return new ErrorQualifier() {
            @Override
            protected String description() {
                return format("Error does not contain an attribute \"%s\"", name);
            }

            @Override
            public boolean test(Error error) {
                Map<String, Value> attributes = error.getAttributes();
                return !attributes.containsKey(name);
            }
        };
    }

    //TODO:2018-07-04:mdrachuk: check validation errors? (constraint violation?)

}
