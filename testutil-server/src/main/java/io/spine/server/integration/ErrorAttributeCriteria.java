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
 * An specific {@link ErrorCriteria error criteria} that checks the errors
 * {@link Error#getAttributes() attributes}.
 *
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
public class ErrorAttributeCriteria extends ErrorCriteria {

    private final String name;

    ErrorAttributeCriteria(String name) {
        super();
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public String description() {
        return format("Error contains an attribute \"%s\"", name);
    }

    /** {@inheritDoc} */
    @Override
    public boolean test(Error error) {
        Map<String, Value> attributes = error.getAttributesMap();
        return attributes.containsKey(name);
    }

    /**
     * Verifies the value of the attribute under current name.
     *
     * @param value a value that is expected by an attribute name
     * @return a new {@link ErrorCriteria error criteria} instance
     */
    public ErrorCriteria value(Value value) {
        checkNotNull(value);
        return new ErrorCriteria() {
            @Override
            public String description() {
                return format("Error contains an attribute \"%s\" with following value: %s",
                              name, value);
            }

            @Override
            public boolean test(Error error) {
                Map<String, Value> attributes = error.getAttributesMap();
                return value.equals(attributes.get(name));
            }
        };
    }
}
