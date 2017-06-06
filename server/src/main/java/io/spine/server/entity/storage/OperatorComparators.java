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

package io.spine.server.entity.storage;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Timestamp;
import io.spine.client.ColumnFilter.Operator;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.time.Timestamps2.isLaterThan;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.lang.String.format;

/**
 * A factory of {@link ComparisonOperation} instances.
 *
 * @author Dmytro Dashenkov
 */
final class OperatorComparators {

    private static final ImmutableMap<Operator, ComparisonOperation> COMPARATORS =
            ImmutableMap.<Operator, ComparisonOperation>builder()
                        .put(Operator.EQUAL, Equal.operation())
                        .put(Operator.GREATER_THAN, GreaterThan.operation())
                        .put(Operator.LESS_THAN, LessThan.operation())
                        .put(Operator.GREATER_OR_EQUAL, GreaterOrEqual.operation())
                        .put(Operator.LESS_OR_EQUAL, LessOrEqual.operation())
                        .build();

    private OperatorComparators() {
        // Prevent utility class instantiation.
    }

    /**
     * Generates an instance of {@link ComparisonOperation} based on the given {@link Operator}.
     *
     * @param operator the rule of comparison
     * @return an instance of {@link ComparisonOperation}
     * @throws IllegalArgumentException if the passed operator is one of the error value enum
     *                                  constants
     */
    static ComparisonOperation of(Operator operator) throws IllegalArgumentException {
        checkNotNull(operator);
        final ComparisonOperation comparator = COMPARATORS.get(operator);
        checkArgument(comparator != null, "Unsupported operator %s.", operator);
        return comparator;
    }

    private enum Equal implements ComparisonOperation {

        INSTANCE;

        private static ComparisonOperation operation() {
            return INSTANCE;
        }

        @Override
        public boolean compare(@Nullable Object left, @Nullable Object right) {
            return Objects.equals(left, right);
        }
    }

    private enum GreaterThan implements ComparisonOperation {

        INSTANCE;

        private static ComparisonOperation operation() {
            return INSTANCE;
        }

        @SuppressWarnings("ChainOfInstanceofChecks") // Generic but limited operand types
        @Override
        public boolean compare(@Nullable Object left, @Nullable Object right) {
            if (left == null || right == null) {
                return false;
            }
            if (left.getClass() != right.getClass()) {
                throw new IllegalArgumentException(
                        format(
                                "Cannot compare an instance of %s to an instance of %s.",
                                left.getClass(),
                                right.getClass())
                );
            }
            if (left instanceof Timestamp) {
                final Timestamp tsLeft = (Timestamp) left;
                final Timestamp tsRight = (Timestamp) right;
                return isLaterThan(tsLeft, tsRight);
            }
            if (left instanceof Comparable<?>) {
                final Comparable cmpLeft = (Comparable<?>) left;
                final Comparable cmpRight = (Comparable<?>) right;
                @SuppressWarnings("unchecked") // Type is unknown but checked at runtime
                final int comparisonResult = cmpLeft.compareTo(cmpRight);
                return comparisonResult > 0;
            }
            throw newIllegalArgumentException("Operation \'%s\' is not supported for type %s.",
                                              this,
                                              left.getClass().getCanonicalName());
        }
    }

    private enum LessThan implements ComparisonOperation {

        INSTANCE;

        private static ComparisonOperation operation() {
            return INSTANCE;
        }

        @Override
        public boolean compare(@Nullable Object left, @Nullable Object right) {
            return GreaterThan.operation().compare(right, left);
        }
    }

    private enum GreaterOrEqual implements ComparisonOperation {

        INSTANCE;

        private static ComparisonOperation operation() {
            return INSTANCE;
        }

        @Override
        public boolean compare(@Nullable Object left, @Nullable Object right) {
            return GreaterThan.operation().compare(left, right)
                    || Equal.operation().compare(left, right);
        }
    }

    private enum LessOrEqual implements ComparisonOperation {

        INSTANCE;

        private static ComparisonOperation operation() {
            return INSTANCE;
        }

        @Override
        public boolean compare(@Nullable Object left, @Nullable Object right) {
            return LessThan.operation().compare(left, right)
                    || Equal.operation().compare(left, right);
        }
    }
}
