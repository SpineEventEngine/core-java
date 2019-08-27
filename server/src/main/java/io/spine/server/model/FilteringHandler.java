/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.model;

import com.google.common.base.Objects;
import io.spine.base.FieldPath;

/**
 * A pair of a {@link HandlerMethod} and the field to filter its events by.
 *
 * @param <H>
 *         the type of handler method
 */
final class FilteringHandler<H extends HandlerMethod<?, ?, ?, ?>> {

    private final H handler;
    private final FieldPath filteredField;

    FilteringHandler(H handler, FieldPath field) {
        this.handler = handler;
        this.filteredField = field;
    }

    boolean fieldDiffersFrom(FieldPath path) {
        return !filteredField.equals(path);
    }

    H handler() {
        return handler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FilteringHandler<?> another = (FilteringHandler<?>) o;
        return Objects.equal(handler, another.handler) &&
                Objects.equal(filteredField, another.filteredField);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(handler, filteredField);
    }
}
