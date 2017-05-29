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

package org.spine3.server.entity.storage;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Timestamp;
import org.spine3.client.ColumnFilter.Operator;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.spine3.time.Timestamps2.isLaterThan;

/**
 * A factory of {@link OperatorComparator} instances.
 *
 * @author Dmytro Dashenkov
 */
final class OperatorComparators {

    private static final ImmutableMap<Operator, OperatorComparator> COMPARATORS =
            ImmutableMap.<Operator, OperatorComparator>builder()
                        .put(Operator.EQUAL, EqualComparator.INSTANCE)
                        .put(Operator.GREATER_THAN, GreaterThanComparator.INSTANCE)
                        .put(Operator.LESS_THAN, LessThanComparator.INSTANCE)
                        .put(Operator.GREATER_OR_EQUAL, GreaterOrEqualComparator.INSTANCE)
                        .put(Operator.LESS_OR_EQUAL, LessOrEqualComparator.INSTANCE)
                        .build();

    private OperatorComparators() {
        // Prevent utility class instantiation.
    }

    /**
     * Generates an instance of {@link OperatorComparator} based on the given {@link Operator}.
     *
     * @param operator the rule of comparison
     * @return an instance of {@link OperatorComparator}
     * @throws IllegalArgumentException if the passed operator is one of the error value enum
     *                                  constants
     */
    static OperatorComparator of(Operator operator) throws IllegalArgumentException {
        checkNotNull(operator);
        final OperatorComparator comparator = COMPARATORS.get(operator);
        checkArgument(comparator != null, "Unsupported operator %s.", operator);
        return comparator;
    }

    private enum EqualComparator implements OperatorComparator {

        INSTANCE;

        @Override
        public boolean compare(@Nullable Object left, @Nullable Object right) {
            return Objects.equals(left, right);
        }
    }

    private enum GreaterThanComparator implements OperatorComparator {

        INSTANCE;

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
            throw new UnsupportedOperationException(
                    format("Operation \'%s\' is not supported for type %s.",
                           this,
                           left.getClass()
                               .getCanonicalName()
                    ));
        }
    }

    private enum LessThanComparator implements OperatorComparator {

        INSTANCE;

        @Override
        public boolean compare(@Nullable Object left, @Nullable Object right) {
            return GreaterThanComparator.INSTANCE.compare(right, left);
        }
    }

    private enum GreaterOrEqualComparator implements OperatorComparator {

        INSTANCE;

        @Override
        public boolean compare(@Nullable Object left, @Nullable Object right) {
            return GreaterThanComparator.INSTANCE.compare(left, right)
                    || EqualComparator.INSTANCE.compare(left, right);
        }
    }

    private enum LessOrEqualComparator implements OperatorComparator {

        INSTANCE;

        @Override
        public boolean compare(@Nullable Object left, @Nullable Object right) {
            return LessThanComparator.INSTANCE.compare(left, right)
                    || EqualComparator.INSTANCE.compare(left, right);
        }
    }
}
