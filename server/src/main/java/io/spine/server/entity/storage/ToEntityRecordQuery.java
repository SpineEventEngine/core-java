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
import io.spine.base.EntityState;
import io.spine.query.Column;
import io.spine.query.ComparisonOperator;
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
 * @author Alex Tymchenko
 */
public final class ToEntityRecordQuery<I, S extends EntityState<I>>
        extends RecordQueryBuilder<I, EntityRecord> {

    private ToEntityRecordQuery(EntityQuery<I, S, ?> source) {
        super(EntityRecord.class);

        Subject<I, S> subject = source.subject();
        this.setIdParameter(subject.id());

        copyPredicates(subject);

        // ordering
        ImmutableList<OrderBy<?, S>> ordering = source.ordering();
        for (OrderBy<?, S> orderByOrigin : ordering) {
            RecordColumn<EntityRecord, ?> thisColumn =
                    new AsEntityRecordColumn(orderByOrigin.column());
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
            LogicalOperator operator = sourcePredicate.operator();
            if (operator == LogicalOperator.AND) {
                copyParameters(parameters, this);
            } else {
                this.either((builder) -> copyParameters(parameters, builder));
            }
        }
    }

    @CanIgnoreReturnValue
    private RecordQueryBuilder<I, EntityRecord>
    copyParameters(ImmutableList<SubjectParameter<S, ?, ?>> parameters,
                   RecordQueryBuilder<I, EntityRecord> builder) {
        for (SubjectParameter<S, ?, ?> parameter : parameters) {
            Column<S, ?> sourceColumn = parameter.column();
            AsEntityRecordColumn column = new AsEntityRecordColumn(sourceColumn);

            RecordCriterion<I, EntityRecord, Object> where = builder.where(column);
            Object paramValue = parameter.value();
            ComparisonOperator paramOperator = parameter.operator();
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
        return builder;
    }

    public static <I, S extends EntityState<I>> RecordQuery<I, EntityRecord>
    transform(EntityQuery<I, S, ?> query) {
        checkNotNull(query);
        return new ToEntityRecordQuery<>(query).build();
    }

}
