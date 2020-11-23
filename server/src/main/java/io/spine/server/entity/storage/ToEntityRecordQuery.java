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
import io.spine.query.Direction;
import io.spine.query.EntityQuery;
import io.spine.query.LogicalOperator;
import io.spine.query.QueryPredicate;
import io.spine.query.RecordColumn;
import io.spine.query.RecordCriterion;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;
import io.spine.query.SortBy;
import io.spine.query.Subject;
import io.spine.query.SubjectParameter;
import io.spine.server.entity.EntityRecord;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.query.Direction.ASC;
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
        copySorting(source);
        copyLimit(source);
        copyMask(source);
    }

    private void copyMask(EntityQuery<I, S, ?> source) {
        FieldMask mask = source.mask();
        if (mask != null) {
            this.withMask(mask);
        }
    }

    private void copyLimit(EntityQuery<I, S, ?> source) {
        Integer limit = source.limit();
        if (limit != null) {
            this.limit(limit);
        }
    }

    private void copySorting(EntityQuery<I, S, ?> source) {
        ImmutableList<SortBy<?, S>> sorting = source.sorting();
        for (SortBy<?, S> sortByOrigin : sorting) {
            RecordColumn<EntityRecord, ?> thisColumn =
                    AsEntityRecordColumn.apply(sortByOrigin.column());
            Direction direction = sortByOrigin.direction();
            if(ASC == direction) {
                this.sortAscendingBy(thisColumn);
            } else {
                this.sortDescendingBy(thisColumn);
            }
        }
    }

    private void copyPredicates(Subject<I, S> subject) {
        ImmutableList<QueryPredicate<S>> predicates = subject.predicates();
        for (QueryPredicate<S> sourcePredicate : predicates) {
            ImmutableList<SubjectParameter<?, ?, ?>> params = sourcePredicate.allParams();
            LogicalOperator operator = sourcePredicate.operator();
            if (operator == LogicalOperator.AND) {
                copyParameters(this, params);
            } else {
                this.either((builder) -> copyParameters(builder, params));
            }

        }
    }

    @CanIgnoreReturnValue
    private RecordQueryBuilder<I, EntityRecord>
    copyParameters(RecordQueryBuilder<I, EntityRecord> builder,
                   Iterable<SubjectParameter<?, ?, ?>> params) {
        for (SubjectParameter<?, ?, ?> parameter : params) {
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
