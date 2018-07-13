/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.client;

import com.google.common.primitives.Primitives;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.client.CompositeColumnFilter.CompositeOperator;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.asList;
import static io.spine.client.ColumnFilter.Operator;
import static io.spine.client.ColumnFilter.Operator.EQUAL;
import static io.spine.client.ColumnFilter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.ColumnFilter.Operator.GREATER_THAN;
import static io.spine.client.ColumnFilter.Operator.LESS_OR_EQUAL;
import static io.spine.client.ColumnFilter.Operator.LESS_THAN;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.EITHER;
import static io.spine.protobuf.TypeConverter.toAny;

/**
 * A factory of {@link ColumnFilter} instances.
 *
 * <p>The public methods of this class represent the recommended way to create
 * a {@link ColumnFilter}.
 *
 * <a name="types"></a>
 * <h1>Comparision types</h1>
 *
 * <p>The Column filters support two generic kinds of comparison:
 * <ol>
 *     <li>Equality comparison;
 *     <li>Ordering comparison.
 * </ol>
 *
 * <p>The {@linkplain #eq equality comparison} supports any data type for the Entity Columns.
 *
 * <p>The ordering comparison ({@link #gt &gt;}, {@link #lt &lt;}, {@link #ge &gt;=},
 * {@link #le &lt;=}) support only following data types for the Entity Columns:
 * <ul>
 *     <li>{@link Timestamp com.google.protobuf.Timestamp};
 *     <li>Java primitive number types;
 *     <li>{@code String}.
 * </ul>
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
     * @return new instance of ColumnFilter
     */
    public static ColumnFilter eq(String columnName, Object value) {
        checkNotNull(columnName);
        checkNotNull(value);
        return createFilter(columnName, value, EQUAL);
    }

    /**
     * Creates new "greater than" {@link ColumnFilter}.
     *
     * <p>For the supported types description see <a href="#types">Comparision types section</a>.
     *
     * @param columnName the name of the Entity Column to query by, expressed in a single field
     *                   name with no type info
     * @param value      the requested value of the Entity Column
     * @return new instance of ColumnFilter
     */
    public static ColumnFilter gt(String columnName, Object value) {
        checkNotNull(columnName);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return createFilter(columnName, value, GREATER_THAN);
    }

    /**
     * Creates new "less than" {@link ColumnFilter}.
     *
     * <p>For the supported types description see <a href="#types">Comparision types section</a>.
     *
     * @param columnName the name of the Entity Column to query by, expressed in a single field
     *                   name with no type info
     * @param value      the requested value of the Entity Column
     * @return new instance of ColumnFilter
     */
    public static ColumnFilter lt(String columnName, Object value) {
        checkNotNull(columnName);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return createFilter(columnName, value, LESS_THAN);
    }

    /**
     * Creates new "greater or equal" {@link ColumnFilter}.
     *
     * <p>For the supported types description see <a href="#types">Comparision types section</a>.
     *
     * @param columnName the name of the Entity Column to query by, expressed in a single field
     *                   name with no type info
     * @param value      the requested value of the Entity Column
     * @return new instance of ColumnFilter
     */
    public static ColumnFilter ge(String columnName, Object value) {
        checkNotNull(columnName);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return createFilter(columnName, value, GREATER_OR_EQUAL);
    }

    /**
     * Creates new "less or equal" {@link ColumnFilter}.
     *
     * <p>For the supported types description see <a href="#types">Comparision types section</a>.
     *
     * @param columnName the name of the Entity Column to query by, expressed in a single field
     *                   name with no type info
     * @param value      the requested value of the Entity Column
     * @return new instance of ColumnFilter
     */
    public static ColumnFilter le(String columnName, Object value) {
        checkNotNull(columnName);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return createFilter(columnName, value, LESS_OR_EQUAL);
    }

    /**
     * Creates new conjunction composite filter.
     *
     * <p>A record is considered matching this filter if and only if it matches all of
     * the aggregated Column filters.
     *
     * @param first the first {@link ColumnFilter}
     * @param rest  the array of additional {@linkplain ColumnFilter filters}, possibly empty
     * @return new instance of {@link CompositeColumnFilter}
     */
    public static CompositeColumnFilter all(ColumnFilter first, ColumnFilter... rest) {
        return composeFilters(asList(first, rest), ALL);
    }

    /**
     * Creates new disjunction composite filter.
     *
     * <p>A record is considered matching this filter if it matches at least one of the composite
     * Column filters.
     *
     * @param first the first {@link ColumnFilter}
     * @param rest  the array of additional {@linkplain ColumnFilter filters}, possibly empty
     * @return new instance of {@link CompositeColumnFilter}
     */
    public static CompositeColumnFilter either(ColumnFilter first, ColumnFilter... rest) {
        return composeFilters(asList(first, rest), EITHER);
    }

    /**
     * Creates new conjunction composite filter.
     *
     * <p>A record is considered matching this filter if and only if it matches all of
     * the composite Column filters.
     *
     * <p>This method is used to create the default {@code ALL} filter if the user chooses to pass
     * instances of {@link ColumnFilter} directly to the {@link QueryBuilder}.
     *
     * @param filters the aggregated Column filters
     * @return new instance of {@link CompositeColumnFilter}
     * @see #all(ColumnFilter, ColumnFilter...) for the public API equivalent
     */
    static CompositeColumnFilter all(Collection<ColumnFilter> filters) {
        return composeFilters(filters, ALL);
    }

    private static ColumnFilter createFilter(String columnName, Object value, Operator operator) {
        Any wrappedValue = toAny(value);
        ColumnFilter filter = ColumnFilter.newBuilder()
                                                .setColumnName(columnName)
                                                .setValue(wrappedValue)
                                                .setOperator(operator)
                                                .build();
        return filter;
    }

    private static CompositeColumnFilter composeFilters(Collection<ColumnFilter> filters,
                                                        CompositeOperator operator) {
        CompositeColumnFilter result = CompositeColumnFilter.newBuilder()
                                                                  .addAllFilter(filters)
                                                                  .setOperator(operator)
                                                                  .build();
        return result;
    }

    private static void checkSupportedOrderingComparisonType(Class<?> cls) {
        Class<?> dataType = Primitives.wrap(cls);
        boolean supported = isSupportedNumber(dataType)
                || Timestamp.class.isAssignableFrom(dataType)
                || String.class.isAssignableFrom(dataType);
        checkArgument(supported,
                      "The type %s is not supported for the ordering comparison.",
                      dataType.getCanonicalName());
    }

    private static boolean isSupportedNumber(Class<?> wrapperClass) {
        boolean result = (Number.class.isAssignableFrom(wrapperClass)
                && Comparable.class.isAssignableFrom(wrapperClass));
        return result;
    }
}
