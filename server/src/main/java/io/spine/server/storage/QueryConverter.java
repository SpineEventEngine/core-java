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
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.query.Direction;
import io.spine.query.RecordColumn;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;

/**
 * Converts the queries defined in Protobuf into the language of {@link io.spine.query} package.
 */
@Internal
public final class QueryConverter {

    private QueryConverter() {
    }

    //TODO:2020-07-16:alex.tymchenko: document!
    public static <I, R extends Message> RecordQuery<I, R>
    convert(RecordSpec<I, R, ?> spec, TargetFilters filters, ResponseFormat format) {
        checkNotNull(spec);
        checkNotNull(filters);
        checkNotNull(format);

        Class<R> recordType = spec.recordType();
        RecordQueryBuilder<I, R> builder = RecordQuery.newBuilder(recordType);

        identifiers(builder, filters.getIdFilter());
        filters(builder, spec, filters);
        fieldMask(builder, format);
        orderByAndLimit(builder, spec, format);

        return builder.build();
    }

    public static <I, R extends Message> RecordQuery<I, R>
    convert(RecordSpec<I, R, ?> spec, ResponseFormat format) {
        checkNotNull(spec);
        checkNotNull(format);

        Class<R> recordType = spec.recordType();
        RecordQueryBuilder<I, R> builder = RecordQuery.newBuilder(recordType);

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
                        parts(builder, spec, composite); break;
                    case EITHER:
                        builder.either((either) -> parts(either, spec, composite)); break;
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

    private static <I, R extends Message> RecordQueryBuilder<I, R>
    parts(RecordQueryBuilder<I, R> builder, RecordSpec<I, R, ?> spec, CompositeFilter filter) {
        if (filter.getFilterCount() > 0) {
            List<Filter> childFilters = filter.getFilterList();
            for (Filter childFilter : childFilters) {
                singleFilterPart(builder, spec, childFilter);

            }
        }
        return builder;
    }

    private static <I, R extends Message> void
    singleFilterPart(RecordQueryBuilder<I, R> builder, RecordSpec<I, R, ?> spec, Filter filter) {
        ColumnName name = columnNameOf(filter);
        Column<?, ?> column = findColumn(spec, name);
        AsRecordColumn<R> convertedColumn = new AsRecordColumn<>(column);

        Message value = unpack(filter.getValue());
        Filter.Operator operator = filter.getOperator();
        switch (operator) {
            case EQUAL:
                builder.where(convertedColumn)
                       .is(value); break;
            case GREATER_THAN:
                builder.where(convertedColumn)
                       .isGreaterThan(value); break;
            case GREATER_OR_EQUAL:
                builder.where(convertedColumn)
                       .isGreaterOrEqualTo(value); break;
            case LESS_THAN:
                builder.where(convertedColumn)
                       .isLessThan(value); break;
            case LESS_OR_EQUAL:
                builder.where(convertedColumn)
                       .isLessOrEqualTo(value); break;

            case UNRECOGNIZED:
            case CFO_UNDEFINED:
            default:
                throw newIllegalArgumentException("Unsupported filter operator `%s` encountered.",
                                                  operator);
        }
    }

    @SuppressWarnings("unchecked")
    private static <I, R extends Message> void identifiers(RecordQueryBuilder<I, R> builder,
                                                           IdFilter idFilter) {
        if (idFilter.getIdCount() > 0) {
            List<I> ids = idFilter.getIdList()
                                  .stream()
                                  .map(Identifier::unpack)
                                  .map(id -> (I) id)
                                  .collect(toList());
            builder.id()
                   .with(ids);
        }
    }

    private static <I, R extends Message> void fieldMask(RecordQueryBuilder<I, R> builder,
                                                         ResponseFormat format) {
        if (format.hasFieldMask()) {
            builder.withMask(format.getFieldMask());
        }
    }

    private static <I, R extends Message> void
    orderByAndLimit(RecordQueryBuilder<I, R> builder, RecordSpec<I, R, ?> spec,
                    ResponseFormat format) {
        boolean hasOrderBy = false;
        if (format.getOrderByCount() > 0) {
            hasOrderBy = true;
            for (OrderBy protoOrderBy : format.getOrderByList()) {
                ColumnName columnName = ColumnName.of(protoOrderBy.getColumn());
                OrderBy.Direction protoDirection = protoOrderBy.getDirection();

                Column<?, ?> column = findColumn(spec, columnName);
                AsRecordColumn<R> convertedColumn = new AsRecordColumn<>(column);
                Direction direction = convertDirection(protoDirection);
                builder.orderBy(convertedColumn, direction);
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

    private static Direction convertDirection(OrderBy.Direction protoDirection) {
        Direction direction = protoDirection == OrderBy.Direction.ASCENDING
                              ? Direction.ASC
                              : Direction.DESC;
        return direction;
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
                      "Incorrect Column name in Entity Filter: %s",
                      join(".", fieldPath.getFieldNameList()));
        String column = fieldPath.getFieldName(0);
        return ColumnName.of(column);
    }

    private static final class AsRecordColumn<R extends Message> extends RecordColumn<R, Object> {

        private static final long serialVersionUID = 0L;

        private AsRecordColumn(Column<?, ?> origin) {
            super(origin.name()
                        .value(), Object.class, getter());
        }

        private static <R extends Message> Getter<R, Object> getter() {
            throw newIllegalStateException(
                    "`AsRecordColumn` serves for the column conversion only " +
                            "and does not supply a getter.");
        }
    }
}
