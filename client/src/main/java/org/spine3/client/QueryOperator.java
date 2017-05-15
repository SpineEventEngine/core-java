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

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.spine3.time.Timestamps2.isLaterThan;

/**
 * An enumeration of all supported value comparison operators applicable to the Entity Columns.
 *
 * <p>Usage of this enumeration when building a {@link Query} is unnecessary. It's effectively
 * {@linkplain org.spine3.annotation.Internal internal} except for the
 * {@linkplain org.spine3.annotation.SPI service provider interface implementors}.
 *
 * <p>Even though the type provides an interface for performing the in-memory comparisons by the
 * listed operators, the main purpose is to show the strategy of the comparison, not implement it.
 */
public enum QueryOperator {

    /**
     * Equality operator ({@code =}).
     */
    EQUAL("=") {
        /**
         * {@inheritDoc}
         *
         * <p>Equality operator supports {@code null} values and returns {@code true} if both
         * the operands are equal to {@code null}.
         */
        @Override
        <T> boolean compare(@Nullable T left, @Nullable T right) {
            return Objects.equal(left, right);
        }
    },

    /**
     * Comparison operator stating that the stored value is greater then ({@code >})
     * the passed value.
     */
    GREATER_THEN(">") {
        /**
         * {@inheritDoc}
         *
         * <p>The comparison works properly only is the operands are instances of {@link Timestamp}.
         * Otherwise an {@link IllegalArgumentException} will be thrown.
         *
         * <p>Returns {@code false} if at lease one of the operands is {@link null}.
         */
        @Override
        <T> boolean compare(@Nullable T left, @Nullable T right) {
            if (left == null || right == null) {
                return false;
            }
            checkArgument(left instanceof Timestamp,
                          "Invalid comparison %s %s %s.",
                          left.toString(), this, right);
            final Timestamp tsLeft = (Timestamp) left;
            final Timestamp tsRight = (Timestamp) right;
            return isLaterThan(tsRight, tsLeft);
        }
    };

    private final String label;

    /**
     * Compares two given objects and returns the result of the comparison.
     *
     * <p>The method provides the natural order of the tokens when writing down a comparison
     * expression e.g. {@code 42 > 8}.
     *
     * <p>The {@code null} handling policy is specific for each operator.
     *
     * @param left     the left operand in the comparison
     * @param operator the comparison strategy to use
     * @param right    the right operand in the comparison
     * @param <T>      the type of the operands
     * @return {@code true} is the operands match the given operator, {@code false} otherwise
     */
    public static <T> boolean compare(@Nullable T left, QueryOperator operator, @Nullable T right) {
        return operator.compare(left, right);
    }

    QueryOperator(String label) {
        this.label = label;
    }

    /**
     * Compares two given objects and returns the result of the comparison.
     *
     * @param left  the left operand in the comparison
     * @param right the right operand in the comparison
     * @param <T>   the type of the operands
     * @return {@code true} is the operands match the given operator, {@code false} otherwise
     */
    abstract <T> boolean compare(@Nullable T left, @Nullable T right);

    /**
     * {@inheritDoc}
     *
     * @return a short symbolic representation of the operator.
     */
    @Override
    public String toString() {
        return label;
    }
}
