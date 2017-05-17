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

import com.google.protobuf.Timestamp;
import org.spine3.client.ColumnFilter.Operator;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.spine3.time.Timestamps2.isLaterThan;

/**
 * @author Dmytro Dashenkov
 */
public final class QueryOperators {

    private QueryOperators() {
        // Prevent this static class from being initialized.
    }

    public static <T> boolean compare(@Nullable T left, Operator operator, @Nullable T right) {
        checkNotNull(operator);
        final MetaOperator op = getMetaInstance(operator);
        final boolean result = op.compare(left, right);
        return result;
    }

    public static String toString(Operator operator) {
        checkNotNull(operator);
        final MetaOperator op = getMetaInstance(operator);
        return op.toString();
    }

    private static MetaOperator getMetaInstance(Operator operator) {
        final String name = operator.name();
        try {
            final MetaOperator metaOperator = MetaOperator.valueOf(name);
            return metaOperator;
        } catch (@SuppressWarnings("ProhibitedExceptionCaught") // Thrown by Enum on missing value
                NullPointerException npe) {
            throw new IllegalArgumentException(
                    format("Cannot recognize query operator %s.", name),
                    npe
            );
        }
    }

    private enum MetaOperator {
        EQUAL("=") {
            @Override
            <T> boolean compare(@Nullable T left, @Nullable T right) {
                return Objects.equals(left, right);
            }
        },
        GREATER_THAN(">") {
            @Override
            <T> boolean compare(@Nullable T left, @Nullable T right) {
                // null values are handled within the instanceof check
                if (left instanceof Timestamp && right instanceof Timestamp) {
                    final Timestamp tsLeft = (Timestamp) left;
                    final Timestamp tsRight = (Timestamp) right;
                    return isLaterThan(tsLeft, tsRight);
                }
                return false;
            }
        },
        LESS_THAN("<") {
            @Override
            <T> boolean compare(@Nullable T left, @Nullable T right) {
                return GREATER_THAN.compare(right, left);
            }
        },
        GREATER_OR_EQUAL(">=") {
            @Override
            <T> boolean compare(@Nullable T left, @Nullable T right) {
                return GREATER_THAN.compare(left, right)
                        || EQUAL.compare(left, right);
            }
        },
        LESS_OR_EQUAL("<=") {
            @Override
            <T> boolean compare(@Nullable T left, @Nullable T right) {
                return LESS_THAN.compare(left, right)
                        || EQUAL.compare(left, right);
            }
        };

        private final String label;

        MetaOperator(String label) {
            this.label = label;
        }

        abstract <T> boolean compare(@Nullable T left, @Nullable T right);

        @Override
        public String toString() {
            return label;
        }
    }
}
