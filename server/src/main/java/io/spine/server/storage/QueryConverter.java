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

package io.spine.server.storage;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.FieldPath;
import io.spine.base.Identifier;
import io.spine.client.CompositeFilter;
import io.spine.client.Filter;
import io.spine.client.IdFilter;
import io.spine.client.OrderBy;
import io.spine.client.ResponseFormat;
import io.spine.client.TargetFilters;
import io.spine.protobuf.TypeConverter;
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.query.RecordColumn;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;

/**
 * Converts the queries defined in Protobuf into the language of {@code io.spine.query} package.
 */
@Internal
public final class QueryConverter {

    private QueryConverter() {
    }

    /**
     * Converts the Protobuf-based filters and the response format into a {@code RecordQuery}.
     *
     * @param <I>
     *         the type of the record identifiers
     * @param <R>
     *         the type of the records which are queried
     * @param filters
     *         the original Protobuf-based filters to convert
     * @param format
     *         the original response format to convert
     * @param spec
     *         the specification of the columns stored for the particular record type
     * @return a new record query with the same semantic as the original filters and response format
     */
    public static <I, R extends Message> RecordQuery<I, R>
    convert(TargetFilters filters, ResponseFormat format, RecordSpec<I, R, ?> spec) {
        checkNotNull(spec);
        checkNotNull(filters);
        checkNotNull(format);

        Class<I> idType = spec.idType();
        Class<R> recordType = spec.recordType();
        RecordQueryBuilder<I, R> builder = RecordQuery.newBuilder(idType, recordType);

        identifiers(builder, filters.getIdFilter());
        filters(builder, spec, filters);
        fieldMask(builder, format);
        orderByAndLimit(builder, spec, format);

        return builder.build();
    }

    /**
     * Creates a new {@code RecordQuery} based on the given response format and
     * the storage specification of the queried record.
     *
     * <p>The result contains no filters on any record field.
     *
     * @param spec
     *         the specification of the columns stored for the particular record type
     * @param format
     *         the original response format
     * @param <I>
     *         the type of the record identifiers
     * @param <R>
     *         the type of the records which are queried
     * @return a new record query
     */
    public static <I, R extends Message> RecordQuery<I, R>
    newQuery(RecordSpec<I, R, ?> spec, ResponseFormat format) {
        checkNotNull(spec);
        checkNotNull(format);

        Class<I> idType = spec.idType();
        Class<R> recordType = spec.recordType();
        RecordQueryBuilder<I, R> builder = RecordQuery.newBuilder(idType, recordType);
        fieldMask(builder, format);
        orderByAndLimit(builder, spec, format);
        return builder.build();
    }

    private static <I, R extends Message> void
    filters(RecordQueryBuilder<I, R> builder, RecordSpec<I, R, ?> spec, TargetFilters filters) {
        if (filters.getFilterCount() > 0) {
            List<CompositeFilter> compositeFilters = filters.getFilterList();
            for (CompositeFilter composite : compositeFilters) {
                CompositeFilter.CompositeOperator compositeOperator = composite.getOperator();

                switch (compositeOperator) {
                    case ALL:
                        convert(composite, spec, builder);
                        break;
                    case EITHER:
                        builder.either((either) -> convert(composite, spec, either));
                        break;
                    case UNRECOGNIZED:
                    case CCF_CO_UNDEFINED:
                    default:
                        throw newIllegalArgumentException(
                                "Unsupported composite operator `%s` encountered.",
                                compositeOperator);
                }
            }
        }
    }

    /**
     * Converts the passed composite filter into a set of conditions and adds them to the passed
     * builder according to the record specification.
     */
    @CanIgnoreReturnValue
    private static <I, R extends Message> RecordQueryBuilder<I, R>
    convert(CompositeFilter filter, RecordSpec<I, R, ?> spec, RecordQueryBuilder<I, R> builder) {
        if (filter.getFilterCount() > 0) {
            List<Filter> childFilters = filter.getFilterList();
            for (Filter childFilter : childFilters) {
                convertSingle(childFilter, builder, spec);
            }
        }
        return builder;
    }

    /**
     * Converts the a single filter into a condition and adds it to the passed builder
     * according to the record specification.
     */
    private static <I, R extends Message> void
    convertSingle(Filter filter, RecordQueryBuilder<I, R> builder, RecordSpec<I, R, ?> spec) {
        ColumnName name = columnNameOf(filter);
        Column<?, ?> column = findColumn(spec, name);
        AsRecordColumn<R> convertedColumn = new AsRecordColumn<>(column);

        Object value = TypeConverter.toObject(filter.getValue(), column.type());

        Filter.Operator operator = filter.getOperator();
        switch (operator) {
            case EQUAL:
                builder.where(convertedColumn).is(value);
                break;
            case GREATER_THAN:
                builder.where(convertedColumn).isGreaterThan(value);
                break;
            case GREATER_OR_EQUAL:
                builder.where(convertedColumn).isGreaterOrEqualTo(value);
                break;
            case LESS_THAN:
                builder.where(convertedColumn).isLessThan(value);
                break;
            case LESS_OR_EQUAL:
                builder.where(convertedColumn).isLessOrEqualTo(value);
                break;

            case UNRECOGNIZED:
            case CFO_UNDEFINED:
            default:
                throw newIllegalArgumentException("Unsupported filter operator `%s` encountered.",
                                                  operator);
        }
    }

    @SuppressWarnings("unchecked")
    private static <I, R extends Message>
    void identifiers(RecordQueryBuilder<I, R> builder, IdFilter idFilter) {
        if (idFilter.getIdCount() > 0) {
            List<I> ids = idFilter.getIdList()
                                  .stream()
                                  .map(Identifier::unpack)
                                  .map(id -> (I) id)
                                  .collect(toList());
            builder.id().in(ids);
        }
    }

    private static <I, R extends Message>
    void fieldMask(RecordQueryBuilder<I, R> builder, ResponseFormat format) {
        if (format.hasFieldMask()) {
            builder.withMask(format.getFieldMask());
        }
    }

    private static <I, R extends Message>
    void orderByAndLimit(RecordQueryBuilder<I, R> builder,
                         RecordSpec<I, R, ?> spec,
                         ResponseFormat format) {
        boolean hasOrderBy = false;
        if (format.getOrderByCount() > 0) {
            hasOrderBy = true;
            for (OrderBy protoOrderBy : format.getOrderByList()) {
                ColumnName columnName = ColumnName.of(protoOrderBy.getColumn());
                OrderBy.Direction protoDirection = protoOrderBy.getDirection();

                Column<?, ?> column = findColumn(spec, columnName);
                AsRecordColumn<R> convertedColumn = new AsRecordColumn<>(column);
                if (protoDirection == OrderBy.Direction.ASCENDING) {
                    builder.sortAscendingBy(convertedColumn);
                } else {
                    builder.sortDescendingBy(convertedColumn);
                }
            }
        }

        int limit = format.getLimit();
        if (limit > 0) {
            if (!hasOrderBy) {
                throw newIllegalArgumentException("Storage query must have " +
                                                          "at least one ordering directive " +
                                                          "if the limit is set.");
            }
            builder.limit(limit);
        }
    }

    private static <I, R extends Message> Column<?, ?>
    findColumn(RecordSpec<I, R, ?> spec, ColumnName columnName) {
        Optional<Column<?, ?>> maybeColumn = spec.findColumn(columnName);
        if (!maybeColumn.isPresent()) {
            throw newIllegalArgumentException(
                    "Cannot find the column `%s` for the type `%s`.", columnName,
                    spec.recordType());
        }
        Column<?, ?> column = maybeColumn.get();
        return column;
    }

    private static ColumnName columnNameOf(Filter filter) {
        FieldPath fieldPath = filter.getFieldPath();
        checkArgument(fieldPath.getFieldNameCount() == 1,
                      "Incorrect Column name in Entity Filter: `%s`.",
                      join(".", fieldPath.getFieldNameList()));
        String column = fieldPath.getFieldName(0);
        return ColumnName.of(column);
    }

    /**
     * A view on a {@link Column} as on a {@link RecordColumn}.
     *
     * <p>Serves for the column conversion when a simple cast is not possible due to the generic
     * type erasure.
     *
     * @param <R>
     *         the type of the message which column is being viewed
     */
    private static final class AsRecordColumn<R extends Message> extends RecordColumn<R, Object> {

        private static final long serialVersionUID = 0L;

        private AsRecordColumn(Column<?, ?> origin) {
            super(origin.name().value(), Object.class, new NoGetter<>());
        }
    }

    /**
     * Returns the getter which always throws an {@link IllegalStateException} upon invocation.
     *
     * @param <R>
     *         the type of the message which column getter it is
     */
    @Immutable
    private static final class NoGetter<R extends Message> implements Column.Getter<R, Object> {

        @Override
        public Object apply(R r) {
            throw newIllegalStateException(
                    "`AsRecordColumn`s serve for the column conversion only " +
                            "and do not provide a getter.");
        }
    }
}
