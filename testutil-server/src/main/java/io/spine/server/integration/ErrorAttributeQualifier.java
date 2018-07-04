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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Value;
import io.spine.base.Error;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
public class ErrorAttributeQualifier extends ErrorQualifier {

    private final String name;

    private ErrorAttributeQualifier(String name) {
        super();
        this.name = name;
    }

    @Override
    protected String description() {
        return format("Error contains an attribute \"%s\"", name);
    }

    @Override
    public boolean test(Error error) {
        Map<String, Value> attributes = error.getAttributes();
        return attributes.containsKey(name);
    }

    public ErrorQualifier value(Value value) {
        checkNotNull(value);
        return new ErrorQualifier() {
            @Override
            protected String description() {
                return format("Error contains an attribute \"%s\" with following value: %s",
                              name, value);
            }

            @Override
            public boolean test(Error error) {
                Map<String, Value> attributes = error.getAttributes();
                return value.equals(attributes.get(name));
            }
        };
    }

    public static ErrorAttributeQualifier withAttribute(String name) {
        return new ErrorAttributeQualifier(name);
    }
}
