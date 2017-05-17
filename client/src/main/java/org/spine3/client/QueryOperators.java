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
import org.spine3.annotation.Internal;
import org.spine3.client.ColumnFilter.Operator;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.spine3.time.Timestamps2.isLaterThan;

/**
 * A utility class for working with the {@link ColumnFilter.Operator} enum.
 *
 * <p>The main facility of this class is executing the {@linkplain #compare comparison} operations
 * represented by an {@link Operator} enum value and two operands of a certain type.
 *
 * <a name="supported_types"/>
 *
 * <p>Not all the data types are supported for all the operations:
 * <ol>
 *     <li>{@link Operator#EQUAL EQUAL} supports all the types; the behaviod is equivalent to
 *         {@link Objects#equals(Object, Object)};
 *     <li>other operators currently support only {@link Timestamp} instances comparison; if any of
 *         the operands is {@code null}, the comparison returns {@code false}.
 * </ol>
 *
 * <p>Support for some other data types may be added in future.
 *
 * @author Dmytro Dashenkov
 */
@Internal
public final class QueryOperators {

    private QueryOperators() {
        // Prevent this static class from being initialized.
    }

    /**
     * Compares the given operands with the given operator.
     *
     * <p>For example, if operands where {@code 42} and {@link 9} (exactly in that order) and
     * the operator was {@link Operator#GREATER_THAN GREATER_THAN}, then this function could be
     * expressed as {@code 42 > 8}. The function returns {@code true} if the expression is
     * mathematically correct.
     *
     * @param left     the left operand
     * @param operator the comparison operator
     * @param right    the right operand
     * @param <T>      the type of the compared values
     * @return {@code true} if the operands match the operator, {@code false} otherwise
     * @throws UnsupportedOperationException if the operation is
     *                                       <a href="supported_types">not supported</a> for
     *                                       the given data types
     */
    public static <T> boolean compare(@Nullable T left, Operator operator, @Nullable T right)
            throws UnsupportedOperationException {
        checkNotNull(operator);
        final MetaOperator op = getMetaInstance(operator);
        final boolean result = op.compare(left, right);
        return result;
    }

    /**
     * @return a symbolic representation of the given {@link Operator}
     */
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
                    npe);
        }
    }

    /**
     * A reflected {@link Operator} enum facilitated by custom methods.
     *
     * <p>Since {@link Operator} is defined in Protobuf, we cannot add custom methods to this type
     * in Java. The way abound is using this type, whose enum constants have the same name as
     * the {@link Operator} constants do.
     *
     * @see QueryOperators#getMetaInstance(Operator)
     */
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
                if (left == null || right == null) {
                    return false;
                }
                if (left instanceof Timestamp && right instanceof Timestamp) {
                    final Timestamp tsLeft = (Timestamp) left;
                    final Timestamp tsRight = (Timestamp) right;
                    return isLaterThan(tsLeft, tsRight);
                }
                throw new UnsupportedOperationException(
                        format("Operation \'%s\' is not supported for type %s.",
                               this,
                               left.getClass()
                                   .getCanonicalName()
                        ));
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

        /**
         * Compares the given operands by the rules of the operator.
         *
         * @see QueryOperators#compare(Object, Operator, Object) for the detailed behaiour
         * description
         */
        abstract <T> boolean compare(@Nullable T left, @Nullable T right);

        /**
         * @return a symbolic of the operator
         * @see QueryOperators#toString(Operator)
         */
        @Override
        public String toString() {
            return label;
        }
    }
}
