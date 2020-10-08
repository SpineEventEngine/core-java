/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Primitives;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.base.EventMessageField;
import io.spine.base.Field;
import io.spine.base.FieldPath;
import io.spine.client.CompositeFilter.CompositeOperator;
import io.spine.code.proto.FieldName;
import io.spine.core.Event;
import io.spine.core.EventContextField;
import io.spine.core.Version;
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.query.EntityStateField;

import java.util.Collection;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Lists.asList;
import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeFilter.CompositeOperator.EITHER;
import static io.spine.client.Filter.Operator;
import static io.spine.client.Filter.Operator.EQUAL;
import static io.spine.client.Filter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.Filter.Operator.GREATER_THAN;
import static io.spine.client.Filter.Operator.LESS_OR_EQUAL;
import static io.spine.client.Filter.Operator.LESS_THAN;
import static io.spine.protobuf.TypeConverter.toAny;
import static java.util.Arrays.stream;

/**
 * A factory of {@link Filter} instances.
 *
 * <p>Public methods of this class represent the recommended way to create a {@link Filter}.
 *
 * <a name="types"></a>
 * <h3>Supported Types</h3>
 *
 * <p>The filters allow to put criteria on fields with comparison operations. The criteria either
 * define the particular values for the fields or specify some ordering.
 *
 * <p>The {@linkplain #eq equality comparison} supports any data type for the compared objects.
 *
 * <p>The ordering comparison ({@link #gt &gt;}, {@link #lt &lt;}, {@link #ge &gt;=},
 * {@link #le &lt;=}) supports only the following types:
 * <ul>
 *     <li>{@link Timestamp com.google.protobuf.Timestamp};
 *     <li>{@link Version io.spine.core.Version};
 *     <li>Java primitive number types;
 *     <li>{@code String}.
 * </ul>
 *
 * @see QueryBuilder for the application
 * @see QueryFilter
 * @see EntityStateFilter
 * @see EventFilter
 */
@SuppressWarnings("ClassWithTooManyMethods") // A lot of method overloads for filter creation.
public final class Filters {

    /** Prevents this utility class instantiation. */
    private Filters() {
    }

    /**
     * Creates a new equality {@link Filter}.
     *
     * @param fieldPath
     *         the field path or the entity column name for entity filters
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter eq(String fieldPath, Object value) {
        checkNotNull(fieldPath);
        checkNotNull(value);
        return createFilter(fieldPath, value, EQUAL);
    }

    /**
     * Creates a new equality {@link Filter}.
     *
     * @param column
     *         the column to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter eq(Column<?, ?> column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        return createFilter(column.name(), value, EQUAL);
    }

    /**
     * Creates a new equality {@link Filter}.
     *
     * @param field
     *         the entity state field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter eq(EntityStateField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createFilter(field.getField(), value, EQUAL);
    }

    /**
     * Creates a new equality {@link Filter}.
     *
     * @param field
     *         the event message field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter eq(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createFilter(field.getField(), value, EQUAL);
    }

    /**
     * Creates a new equality {@link Filter}.
     *
     * <p>The field path in the filter will be automatically prepended with {@code "context."} to
     * enable filtering by {@linkplain io.spine.core.EventContext event context}.
     *
     * @param field
     *         the event context field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter eq(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createContextFilter(field.getField(), value, EQUAL);
    }

    /**
     * Creates a new "greater than" {@link Filter}.
     *
     * <p>For the supported types description, see <a href="#types">Comparison types section</a>.
     *
     * @param fieldPath
     *         the field path or the entity column name for entity filters
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter gt(String fieldPath, Object value) {
        checkNotNull(fieldPath);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return createFilter(fieldPath, value, GREATER_THAN);
    }

    /**
     * Creates a new "greater than" {@link Filter}.
     *
     * <p>For the supported types description, see <a href="#types">Comparison types section</a>.
     *
     * @param column
     *         the entity column to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter gt(Column<?, ?> column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        return createFilter(column.name(), value, GREATER_THAN);
    }

    /**
     * Creates a new "greater than" {@link Filter}.
     *
     * <p>For the supported types description, see <a href="#types">Comparison types section</a>.
     *
     * @param field
     *         the entity state field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter gt(EntityStateField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createFilter(field.getField(), value, GREATER_THAN);
    }

    /**
     * Creates a new "greater than" {@link Filter}.
     *
     * <p>For the supported types description, see <a href="#types">Comparison types section</a>.
     *
     * @param field
     *         the event message field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter gt(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createFilter(field.getField(), value, GREATER_THAN);
    }

    /**
     * Creates a new "greater than" {@link Filter}.
     *
     * <p>The field path in the filter will be automatically prepended with {@code "context."} to
     * enable filtering by {@linkplain io.spine.core.EventContext event context}.
     *
     * <p>For the supported types description, see <a href="#types">Comparison types section</a>.
     *
     * @param field
     *         the event context field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter gt(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createContextFilter(field.getField(), value, GREATER_THAN);
    }

    /**
     * Creates a new "less than" {@link Filter}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param fieldPath
     *         the field path or the entity column name for entity filters
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter lt(String fieldPath, Object value) {
        checkNotNull(fieldPath);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return createFilter(fieldPath, value, LESS_THAN);
    }

    /**
     * Creates a new "less than" {@link Filter}.
     *
     * <p>For the supported types description, see <a href="#types">Comparison types section</a>.
     *
     * @param column
     *         the entity column to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter lt(Column<?, ?> column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        return createFilter(column.name(), value, LESS_THAN);
    }

    /**
     * Creates a new "less than" {@link Filter}.
     *
     * <p>For the supported types description, see <a href="#types">Comparison types section</a>.
     *
     * @param field
     *         the entity state field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter lt(EntityStateField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createFilter(field.getField(), value, LESS_THAN);
    }

    /**
     * Creates a new "less than" {@link Filter}.
     *
     * <p>For the supported types description, see <a href="#types">Comparison types section</a>.
     *
     * @param field
     *         the event message field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter lt(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createFilter(field.getField(), value, LESS_THAN);
    }

    /**
     * Creates a new "less than" {@link Filter}.
     *
     * <p>The field path in the filter will be automatically prepended with {@code "context."} to
     * enable filtering by {@linkplain io.spine.core.EventContext event context}.
     *
     * <p>For the supported types description, see <a href="#types">Comparison types section</a>.
     *
     * @param field
     *         the event context field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter lt(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createContextFilter(field.getField(), value, LESS_THAN);
    }

    /**
     * Creates a new "greater or equal" {@link Filter}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param fieldPath
     *         the field path or the entity column name for entity filters
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter ge(String fieldPath, Object value) {
        checkNotNull(fieldPath);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return createFilter(fieldPath, value, GREATER_OR_EQUAL);
    }

    /**
     * Creates a new "greater or equal" {@link Filter}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param column
     *         the entity column to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter ge(Column<?, ?> column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        return createFilter(column.name(), value, GREATER_OR_EQUAL);
    }

    /**
     * Creates a new "greater or equal" {@link Filter}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param field
     *         the entity state field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter ge(EntityStateField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createFilter(field.getField(), value, GREATER_OR_EQUAL);
    }

    /**
     * Creates a new "greater or equal" {@link Filter}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param field
     *         the event message field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter ge(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createFilter(field.getField(), value, GREATER_OR_EQUAL);
    }

    /**
     * Creates a new "greater or equal" {@link Filter}.
     *
     * <p>The field path in the filter will be automatically prepended with {@code "context."} to
     * enable filtering by {@linkplain io.spine.core.EventContext event context}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param field
     *         the event context field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter ge(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createContextFilter(field.getField(), value, GREATER_OR_EQUAL);
    }

    /**
     * Creates a new "less or equal" {@link Filter}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param fieldPath
     *         the field path or the entity column name for entity filters
     * @param value
     *         the requested value
     * @return new instance of {@code Filter}
     */
    public static Filter le(String fieldPath, Object value) {
        checkNotNull(fieldPath);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return createFilter(fieldPath, value, LESS_OR_EQUAL);
    }

    /**
     * Creates a new "less or equal" {@link Filter}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param column
     *         the entity column to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter le(Column<?, ?> column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        return createFilter(column.name(), value, LESS_OR_EQUAL);
    }

    /**
     * Creates a new "less or equal" {@link Filter}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param field
     *         the entity state field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter le(EntityStateField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createFilter(field.getField(), value, LESS_OR_EQUAL);
    }

    /**
     * Creates a new "less or equal" {@link Filter}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param field
     *         the event message field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter le(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createFilter(field.getField(), value, LESS_OR_EQUAL);
    }

    /**
     * Creates a new "less or equal" {@link Filter}.
     *
     * <p>The field path in the filter will be automatically prepended with {@code "context."} to
     * enable filtering by {@linkplain io.spine.core.EventContext event context}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param field
     *         the event context field to filter by
     * @param value
     *         the requested value
     * @return a new instance of {@code Filter}
     */
    public static Filter le(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return createContextFilter(field.getField(), value, LESS_OR_EQUAL);
    }

    /**
     * Creates a new conjunction composite filter.
     *
     * <p>A record is considered matching this filter if and only if it matches all of the
     * aggregated filters.
     *
     * @param first
     *         the first {@link Filter}
     * @param rest
     *         the array of additional {@linkplain Filter filters}, possibly empty
     * @return new instance of {@link CompositeFilter}
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    // OK as the method is clearly distinguished by the first argument.
    public static CompositeFilter all(Filter first, Filter... rest) {
        checkNotNull(first);
        checkNotNull(rest);
        return composeFilters(asList(first, rest), ALL);
    }

    /**
     * Creates a new disjunction composite filter.
     *
     * <p>A record is considered matching this filter if it matches at least one of the aggregated
     * filters.
     *
     * @param first
     *         the first {@link Filter}
     * @param rest
     *         the array of additional {@linkplain Filter filters}, possibly empty
     * @return new instance of {@link CompositeFilter}
     */
    public static CompositeFilter either(Filter first, Filter... rest) {
        checkNotNull(first);
        checkNotNull(rest);
        return composeFilters(asList(first, rest), EITHER);
    }

    /**
     * Creates a new composite filter with a disjunction applied to the aggregated filters.
     *
     * <p>A record is considered matching this filter if it matches at least one of
     * the aggregated filters.
     *
     * <p>This method is used to create the default {@code EITHER} filter if the user
     * chooses to pass instances of {@link Filter} directly to the {@link QueryBuilder}.
     *
     * @param filters
     *         the aggregated filters
     * @return new instance of {@link CompositeFilter}
     * @see #either(Filter, Filter...) for the public API equivalent
     */
     static CompositeFilter either(Collection<Filter> filters) {
        checkNotNull(filters);
        checkArgument(!filters.isEmpty(),
                      "Composite filter must contain at least one plain filter in it.");
        return composeFilters(filters, EITHER);
    }

    /**
     * Creates a new conjunction composite filter.
     *
     * <p>A record is considered matching this filter if and only if it matches all of
     * the aggregated filters.
     *
     * <p>This method is used to create the default {@code ALL} filter if the user chooses to pass
     * instances of {@link Filter} directly to the {@link QueryBuilder}.
     *
     * @param filters
     *         the aggregated filters
     * @return new instance of {@link CompositeFilter}
     * @see #all(Filter, Filter...) for the public API equivalent
     */
    static CompositeFilter all(Collection<Filter> filters) {
        checkNotNull(filters);
        checkArgument(!filters.isEmpty(),
                      "Composite filter must contain at least one simple filter in it.");
        return composeFilters(filters, ALL);
    }

    static Filter createFilter(String fieldPath, Object value, Operator operator) {
        Field field = Field.parse(fieldPath);
        return createFilter(field, value, operator);
    }

    static Filter createFilter(ColumnName columnName, Object value, Operator operator) {
        String fieldPath = columnName.value();
        return createFilter(fieldPath, value, operator);
    }


    static Filter createFilter(FieldName fieldName, Object value, Operator operator) {
        FieldPath fieldPath = fieldName.asPath();
        return createFilter(fieldPath, value, operator);
    }

    static Filter createFilter(Field field, Object value, Operator operator) {
        FieldPath fieldPath = field.path();
        return createFilter(fieldPath, value, operator);
    }

    static Filter createFilter(FieldPath path, Object value, Operator operator) {
        Any wrappedValue = toAny(value);
        Filter filter = Filter
                .newBuilder()
                .setFieldPath(path)
                .setValue(wrappedValue)
                .setOperator(operator)
                .build();
        return filter;
    }

    static Filter createContextFilter(Field field, Object value, Operator operator) {
        FieldPath fieldPath = Event.Field.context()
                                         .getField()
                                         .nested(field)
                                         .path();
        return createFilter(fieldPath, value, operator);
    }

    static CompositeFilter composeFilters(Iterable<Filter> filters, CompositeOperator operator) {
        CompositeFilter result = CompositeFilter
                .newBuilder()
                .addAllFilter(filters)
                .setOperator(operator)
                .build();
        return result;
    }

    static Filter[] extractFilters(TypedFilter<?>[] filters) {
        return stream(filters)
                .map(TypedFilter::filter)
                .toArray(Filter[]::new);
    }

    static <M extends Message> ImmutableList<Filter>
    extractFilters(Collection<? extends TypedFilter<M>> filters) {
        return filters.stream()
                      .map(TypedFilter::filter)
                      .collect(toImmutableList());
    }

    static CompositeFilter[] extractFilters(TypedCompositeFilter<?>[] filters) {
        return stream(filters)
                .map(TypedCompositeFilter::filter)
                .toArray(CompositeFilter[]::new);
    }

    static void checkSupportedOrderingComparisonType(Class<?> cls) {
        Class<?> dataType = Primitives.wrap(cls);
        boolean supported = isSupportedNumber(dataType)
                || Timestamp.class.isAssignableFrom(dataType)
                || Version.class.isAssignableFrom(dataType)
                || String.class.isAssignableFrom(dataType);
        checkArgument(supported,
                      "The type `%s` is not supported for the ordering comparison.",
                      dataType.getCanonicalName());
    }

    private static boolean isSupportedNumber(Class<?> wrapperClass) {
        boolean result = (Number.class.isAssignableFrom(wrapperClass)
                && Comparable.class.isAssignableFrom(wrapperClass));
        return result;
    }

    /**
     * Creates a filter of events which can apply conditions from the passed
     * {@code CompositeFilter} to both event message and its context.
     *
     * <p>The filter is deemed addressing the {@linkplain io.spine.core.EventContext event context}
     * if the field path specified in it starts with the {@code "context."} prefix.
     */
    @Internal
    public static Predicate<Event> toEventFilter(CompositeFilter filterData) {
        checkNotNull(filterData);
        return new CompositeEventFilter(filterData);
    }
}
