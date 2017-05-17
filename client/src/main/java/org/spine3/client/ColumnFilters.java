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

import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.client.ColumnFilter.Operator;
import static org.spine3.client.ColumnFilter.Operator.EQUAL;
import static org.spine3.client.ColumnFilter.Operator.GREATER_OR_EQUAL;
import static org.spine3.client.ColumnFilter.Operator.GREATER_THAN;
import static org.spine3.client.ColumnFilter.Operator.LESS_OR_EQUAL;
import static org.spine3.client.ColumnFilter.Operator.LESS_THAN;
import static org.spine3.protobuf.TypeConverter.toAny;

/**
 * A factory of {@link ColumnFilter} instances.
 *
 * <p>The public methods of this class represent the recommended way to create
 * a {@link ColumnFilter}.
 *
 * @see QueryBuilder for the application
 */
public final class ColumnFilters {

    private ColumnFilters() {
        // Prevent this utility class initialization.
    }

    /**
     * Creates new equality {@link ColumnFilter}.
     *
     * @param columnName the name of the Entity Column to query by, expressed in a single field
     *                   name with no type info
     * @param value      the requested value of the Entity Column
     * @return new instance of QueryParameter
     */
    public static ColumnFilter eq(String columnName, Object value) {
        checkNotNull(columnName);
        checkNotNull(value);
        return createFilter(columnName, value, EQUAL);
    }

    /**
     * Creates new "greater than" {@link ColumnFilter}.
     *
     * @param columnName the name of the Entity Column to query by, expressed in a single field
     *                   name with no type info
     * @param value      the requested value of the Entity Column
     * @return new instance of QueryParameter
     */
    public static ColumnFilter gt(String columnName, Timestamp value) {
        checkNotNull(columnName);
        checkNotNull(value);
        return createFilter(columnName, value, GREATER_THAN);
    }

    /**
     * Creates new "less than" {@link ColumnFilter}.
     *
     * @param columnName the name of the Entity Column to query by, expressed in a single field
     *                   name with no type info
     * @param value      the requested value of the Entity Column
     * @return new instance of QueryParameter
     */
    public static ColumnFilter lt(String columnName, Timestamp value) {
        checkNotNull(columnName);
        checkNotNull(value);
        return createFilter(columnName, value, LESS_THAN);
    }

    /**
     * Creates new "greater or equal" {@link ColumnFilter}.
     *
     * @param columnName the name of the Entity Column to query by, expressed in a single field
     *                   name with no type info
     * @param value      the requested value of the Entity Column
     * @return new instance of QueryParameter
     */
    public static ColumnFilter ge(String columnName, Timestamp value) {
        checkNotNull(columnName);
        checkNotNull(value);
        return createFilter(columnName, value, GREATER_OR_EQUAL);
    }

    /**
     * Creates new "less or equal" {@link ColumnFilter}.
     *
     * @param columnName the name of the Entity Column to query by, expressed in a single field
     *                   name with no type info
     * @param value      the requested value of the Entity Column
     * @return new instance of QueryParameter
     */
    public static ColumnFilter le(String columnName, Timestamp value) {
        checkNotNull(columnName);
        checkNotNull(value);
        return createFilter(columnName, value, LESS_OR_EQUAL);
    }

    private static ColumnFilter createFilter(String columnName, Object value, Operator operator) {
        final Any wrappedValue = toAny(value);
        final ColumnFilter filter = ColumnFilter.newBuilder()
                                                .setColumnName(columnName)
                                                .setValue(wrappedValue)
                                                .setOperator(operator)
                                                .build();
        return filter;
    }
}
