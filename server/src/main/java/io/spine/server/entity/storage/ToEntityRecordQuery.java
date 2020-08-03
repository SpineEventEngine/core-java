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

package io.spine.server.entity.storage;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.FieldMask;
import io.spine.annotation.Internal;
import io.spine.base.EntityState;
import io.spine.query.Column;
import io.spine.query.ComparisonOperator;
import io.spine.query.CustomSubjectParameter;
import io.spine.query.EntityQuery;
import io.spine.query.LogicalOperator;
import io.spine.query.OrderBy;
import io.spine.query.QueryPredicate;
import io.spine.query.RecordColumn;
import io.spine.query.RecordCriterion;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;
import io.spine.query.Subject;
import io.spine.query.SubjectParameter;
import io.spine.server.entity.EntityRecord;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Transforms the given {@link EntityQuery} into a {@link RecordQuery}
 * over the {@link EntityRecord}s.
 *
 * @param <I>
 *         the type of the identifiers of entities which are queried by the given query
 * @param <S>
 *         the type of the queried entity state
 */
@Internal
public final class ToEntityRecordQuery<I, S extends EntityState<I>>
        extends RecordQueryBuilder<I, EntityRecord> {

    /**
     * Transforms the entity query into a {@code RecordQuery}.
     *
     * @param query
     *         a query to transform
     * @param <I>
     *         the type of the identifiers of entities which are queried by the given query
     * @param <S>
     *         the type of the queried entity state
     * @return a new instance of {@code RecordQuery} targeting the {@link EntityRecord}s
     */
    public static <I, S extends EntityState<I>> RecordQuery<I, EntityRecord>
    transform(EntityQuery<I, S, ?> query) {
        checkNotNull(query);
        return new ToEntityRecordQuery<>(query).build();
    }

    private ToEntityRecordQuery(EntityQuery<I, S, ?> source) {
        super(source.subject().idType(), EntityRecord.class);

        Subject<I, S> subject = source.subject();
        this.setIdParameter(subject.id());

        copyPredicates(subject);

        // ordering
        ImmutableList<OrderBy<?, S>> ordering = source.ordering();
        for (OrderBy<?, S> orderByOrigin : ordering) {
            RecordColumn<EntityRecord, ?> thisColumn =
                    AsEntityRecordColumn.apply(orderByOrigin.column());
            this.orderBy(thisColumn, orderByOrigin.direction());
        }

        // limit
        Integer limit = source.limit();
        if (limit != null) {
            this.limit(limit);
        }

        // mask
        FieldMask mask = source.mask();
        if (mask != null) {
            this.withMask(mask);
        }
    }

    private void copyPredicates(Subject<I, S> subject) {
        ImmutableList<QueryPredicate<S>> predicates = subject.predicates();
        for (QueryPredicate<S> sourcePredicate : predicates) {
            ImmutableList<SubjectParameter<S, ?, ?>> parameters = sourcePredicate.parameters();
            ImmutableList<CustomSubjectParameter<?, ?>> customParams = sourcePredicate.customParameters();
            LogicalOperator operator = sourcePredicate.operator();
            if (operator == LogicalOperator.AND) {
                copyParameters(parameters, customParams, this);
            } else {
                this.either((builder) -> copyParameters(parameters, customParams, builder));
            }

        }
    }

    @SuppressWarnings("MethodWithMultipleLoops")    // Decreasing the number of methods.
    @CanIgnoreReturnValue
    private RecordQueryBuilder<I, EntityRecord>
    copyParameters(ImmutableList<SubjectParameter<S, ?, ?>> parameters,
                   ImmutableList<CustomSubjectParameter<?, ?>> customParams,
                   RecordQueryBuilder<I, EntityRecord> builder) {
        for (SubjectParameter<S, ?, ?> parameter : parameters) {
            addParameter(builder, parameter.column(), parameter.value(), parameter.operator());
        }
        for (CustomSubjectParameter<?, ?> parameter : customParams) {
            addParameter(builder, parameter.column(), parameter.value(), parameter.operator());
        }
        return builder;
    }

    private void addParameter(RecordQueryBuilder<I, EntityRecord> builder,
                              Column<?, ?> sourceColumn,
                              Object paramValue,
                              ComparisonOperator paramOperator) {
        RecordColumn<EntityRecord, Object> column = AsEntityRecordColumn.apply(sourceColumn);

        RecordCriterion<I, EntityRecord, Object> where = builder.where(column);
        switch (paramOperator) {
            case EQUALS:
                where.is(paramValue); break;
            case NOT_EQUALS:
                where.isNot(paramValue); break;
            case GREATER_OR_EQUALS:
                where.isGreaterOrEqualTo(paramValue); break;
            case GREATER_THAN:
                where.isGreaterThan(paramValue); break;
            case LESS_OR_EQUALS:
                where.isLessOrEqualTo(paramValue); break;
            case LESS_THAN:
                where.isLessThan(paramValue); break;
            default:
                throw newIllegalStateException("Unknown comparison operator `%s`.",
                                               paramOperator);
        }
    }
}
