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

package io.spine.server.storage;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.time.TimestampTemporal;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.Filter.Operator;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.lang.String.format;

/**
 * A boolean non-typed comparison operation on two given instances.
 *
 * @see io.spine.client.CompositeFilter.CompositeOperator for the comparison strategies
 */
@Internal
public enum OperatorEvaluator {

    EQUAL {
        @Override
        public boolean eval(@Nullable Object left, @Nullable Object right) {
            return Objects.equals(left, right);
        }
    },
    GREATER_THAN {
        @SuppressWarnings("ChainOfInstanceofChecks") // Generic but limited operand types
        @Override
        public boolean eval(@Nullable Object left, @Nullable Object right) {
            if (left == null || right == null) {
                return false;
            }
            if (left.getClass() != right.getClass()) {
                throw new IllegalArgumentException(
                        format("Cannot compare an instance of %s to an instance of %s.",
                               left.getClass(),
                               right.getClass())
                );
            }
            if (left instanceof Timestamp) {
                TimestampTemporal timeLeft = TimestampTemporal.from((Timestamp) left);
                TimestampTemporal timeRight = TimestampTemporal.from((Timestamp) right);
                return timeLeft.isLaterThan(timeRight);
            }
            if (left instanceof Comparable<?>) {
                Comparable cmpLeft = (Comparable<?>) left;
                Comparable cmpRight = (Comparable<?>) right;
                @SuppressWarnings("unchecked") // Type is unknown but checked at runtime
                int comparisonResult = cmpLeft.compareTo(cmpRight);
                return comparisonResult > 0;
            }
            throw newIllegalArgumentException("Operation \'%s\' is not supported for type %s.",
                                              this,
                                              left.getClass().getCanonicalName());
        }
    },
    LESS_THAN {
        @Override
        public boolean eval(@Nullable Object left, @Nullable Object right) {
            return GREATER_THAN.eval(right, left);
        }
    },
    GREATER_OR_EQUAL {
        @Override
        public boolean eval(@Nullable Object left, @Nullable Object right) {
            return GREATER_THAN.eval(left, right)
                    || EQUAL.eval(left, right);
        }
    },
    LESS_OR_EQUAL {
        @Override
        public boolean eval(@Nullable Object left, @Nullable Object right) {
            return LESS_THAN.eval(left, right)
                || EQUAL.eval(left, right);
        }
    };

    private static final ImmutableMap<Operator, OperatorEvaluator> EVALUATORS =
            ImmutableMap.<Operator, OperatorEvaluator>builder()
                    .put(Operator.EQUAL, EQUAL)
                    .put(Operator.GREATER_THAN, GREATER_THAN)
                    .put(Operator.LESS_THAN, LESS_THAN)
                    .put(Operator.GREATER_OR_EQUAL, GREATER_OR_EQUAL)
                    .put(Operator.LESS_OR_EQUAL, LESS_OR_EQUAL)
                    .build();

    /**
     * Evaluates the given expression.
     *
     * <p>For example, if operands where {@code 42} and {@code 9} (exactly in that order) and
     * the operator was {@link Operator#GREATER_THAN GREATER_THAN}, then this function could be
     * expressed as {@code 42 > 9}. The function returns the {@code boolean} result of
     * the evaluation.
     *
     * @param left
     *         the left operand
     * @param operator
     *         the comparison operator
     * @param right
     *         the right operand
     * @param <T>
     *         the type of the compared values
     * @return {@code true} if the operands match the operator, {@code false} otherwise
     * @throws UnsupportedOperationException
     *         if the operation is <a href="supported_types">not supported</a> for the given
     *         data types
     */
    public static <T> boolean eval(@Nullable T left, Operator operator, @Nullable T right)
            throws UnsupportedOperationException {
        checkNotNull(operator);
        OperatorEvaluator evaluator = EVALUATORS.get(operator);
        checkArgument(evaluator != null, operator);
        boolean result = evaluator.eval(left, right);
        return result;
    }

    /**
     * Evaluates the expression of joining the given operands with a certain operator.
     *
     * @see OperatorEvaluator#eval for the detailed behaiour
     * description
     * @return {@code true} if the expression evaluates into {@code true}, {@code false} otherwise
     */
    abstract boolean eval(@Nullable Object left, @Nullable Object right);
}
