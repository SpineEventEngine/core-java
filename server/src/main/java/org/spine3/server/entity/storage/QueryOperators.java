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

import com.google.protobuf.Timestamp;
import org.spine3.client.ColumnFilter.Operator;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A utility class for working with the {@link Operator} enum.
 *
 * <p>The main facility of this class is executing the {@linkplain #compare comparison} operations
 * represented by an {@link Operator} enum value and two operands of a certain type.
 *
 * <a name="supported_types"/>
 *
 * <p>Not all the data types are supported for all the operations:
 * <ol>
 *     <li>{@link Operator#EQUAL EQUAL} supports all the types; the behaviour is equivalent to
 *         {@link Objects#equals(Object, Object)};
 *     <li>other operators currently support only {@link Timestamp} instances comparison; if any of
 *         the operands is {@code null}, the comparison returns {@code false}.
 * </ol>
 *
 * <p>Support for some other data types may be added in future.
 *
 * @author Dmytro Dashenkov
 */
final class QueryOperators {

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
    static <T> boolean compare(@Nullable T left, Operator operator, @Nullable T right)
            throws UnsupportedOperationException {
        checkNotNull(operator);
        final OperatorComparator comparator = OperatorComparators.of(operator);
        final boolean result = comparator.compare(left, right);
        return result;
    }
}
