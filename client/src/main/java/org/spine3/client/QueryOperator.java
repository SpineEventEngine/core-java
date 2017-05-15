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

package org.spine3.client;

import com.google.common.base.Objects;
import com.google.protobuf.Timestamp;
import org.spine3.annotation.Internal;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;
import static org.spine3.time.Timestamps2.isLaterThan;

/**
 * An enumeration of all supported value comparison operators applicable to the Entity Columns.
 */
public enum QueryOperator {

    /**
     * Equality operator ({@code =}).
     */
    EQUAL("=") {
        @Override
        public <T> boolean matches(@Nullable T left, @Nullable T right) {
            return Objects.equal(left, right);
        }
    },

    /**
     * Comparison operator stating that the stored value is greater then ({@code >})
     * the passed value.
     */
    @Internal
    GREATER_THEN(">") {
        @Override
        public <T> boolean matches(@Nullable T left, @Nullable T right) {
            if (left == null || right == null) {
                return false;
            }
            checkState(left instanceof Timestamp,
                       "Invalid comparison %s %s %s." ,
                       left.toString(), this, right);
            final Timestamp tsLeft = (Timestamp) left;
            final Timestamp tsRight = (Timestamp) right;
            return isLaterThan(tsLeft, tsRight);
        }
    };

    private final String label;

    QueryOperator(String repr) {
        this.label = repr;
    }

    public abstract <T> boolean matches(@Nullable T left, @Nullable T right);

    @Override
    public String toString() {
        return label;
    }
}
