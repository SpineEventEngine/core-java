/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.FieldMask;
import io.spine.base.EntityState;
import io.spine.core.UserId;
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.query.ComparisonOperator;
import io.spine.query.EntityQuery;
import io.spine.query.LogicalOperator;
import io.spine.query.QueryPredicate;
import io.spine.query.Subject;
import io.spine.query.SubjectParameter;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.Filter.Operator.EQUAL;
import static io.spine.client.Filter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.Filter.Operator.GREATER_THAN;
import static io.spine.client.Filter.Operator.LESS_OR_EQUAL;
import static io.spine.client.Filter.Operator.LESS_THAN;
import static io.spine.client.Filters.all;
import static io.spine.client.Filters.createFilter;
import static io.spine.client.Filters.either;
import static io.spine.client.OrderBy.Direction.ASCENDING;
import static io.spine.client.OrderBy.Direction.DESCENDING;
import static io.spine.query.Direction.ASC;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Transforms {@link io.spine.query.EntityQuery} instances to the Protobuf-based Query objects.
 *
 * <p>Such a transformation is required in order to transfer the {@code EntityQuery}
 * instances over the wire.
 *
 * @see io.spine.query.EntityQueryBuilder#build(Function)
 */
public final class EntityQueryToProto implements Function<EntityQuery<?, ?, ?>, Query> {

    /**
     * A factory of the {@code Query} instances which is used in a transformation process.
     */
    private final QueryFactory factory;

    private EntityQueryToProto(QueryFactory factory) {
        this.factory = factory;
    }

    /**
     * Creates an instance of a transformer in a context of the passed {@code ClientRequest}.
     *
     * <p>The instances of {@link Query} created by the transformer will rely on the properties
     * of the client associated with the request.
     *
     * @param request
     *         the request in scope of which the conversion is done
     * @return new instance of the query transformer
     */
    public static EntityQueryToProto transformWith(ClientRequest request) {
        checkNotNull(request);
        Client client = request.client();
        UserId user = request.user();
        ActorRequestFactory factory = client.requestOf(user);
        return transformWith(factory.query());
    }

    /**
     * Creates an instance of a transformer which would use the passed query factory.
     *
     * @param factory
     *         the query factory
     * @return new instance of the query transformer
     */
    public static EntityQueryToProto transformWith(QueryFactory factory) {
        checkNotNull(factory);
        return new EntityQueryToProto(factory);
    }

    @Override
    public Query apply(EntityQuery<?, ?, ?> query) {
        Class<? extends EntityState<?>> entityStateType = query.subject()
                                                               .recordType();
        QueryBuilder builder = factory.select(entityStateType);
        Query result = toProtoQuery(builder, query);
        return result;
    }

    private static Query toProtoQuery(QueryBuilder builder, EntityQuery<?, ?, ?> query) {
        Subject<?, ?> subject = query.subject();
        addIds(builder, subject);
        addPredicates(builder, subject.predicates());
        addSorting(builder, query);
        addLimit(builder, query);
        addFieldMask(builder, query);
        return builder.build();
    }

    private static void addFieldMask(QueryBuilder builder, EntityQuery<?, ?, ?> query) {
        FieldMask originMask = query.mask();
        if (!originMask.equals(FieldMask.getDefaultInstance())) {
            builder.withMask(originMask.getPathsList());
        }
    }

    private static void addLimit(QueryBuilder builder, EntityQuery<?, ?, ?> query) {
        Integer originLimit = query.limit();
        if (originLimit != null) {
            builder.limit(originLimit);
        }
    }

    private static void addSorting(QueryBuilder builder, EntityQuery<?, ?, ?> query) {
        for (io.spine.query.SortBy<?, ?> sortBy : query.sorting()) {
            String columnName = sortBy.column()
                                      .name()
                                      .value();
            OrderBy.Direction direction = sortBy.direction() == ASC ? ASCENDING : DESCENDING;
            builder.orderBy(columnName, direction);
        }
    }

    private static void
    addPredicates(QueryBuilder builder, ImmutableList<? extends QueryPredicate<?>> predicates) {
        ImmutableSet.Builder<CompositeFilter> filters = ImmutableSet.builder();
        for (QueryPredicate<?> predicate : predicates) {
            LogicalOperator logicalOp = predicate.operator();
            ImmutableList<SubjectParameter<?, ?, ?>> params = predicate.allParams();
            CompositeFilter aFilter = toFilter(params, logicalOp);
            filters.add(aFilter);
        }
        builder.where(filters.build());
    }

    private static CompositeFilter
    toFilter(ImmutableList<SubjectParameter<?, ?, ?>> params, LogicalOperator logicalOp) {
        ImmutableList.Builder<Filter> filters = ImmutableList.builder();
        for (SubjectParameter<?, ?, ?> parameter : params) {
            Filter filter = asProtoFilter(parameter);
            filters.add(filter);
        }
        ImmutableList<Filter> filterList = filters.build();
        CompositeFilter compositeFilter =
                logicalOp == LogicalOperator.AND ? all(filterList)
                                                 : either(filterList);
        return compositeFilter;
    }

    private static Filter asProtoFilter(SubjectParameter<?, ?, ?> parameter) {
        Object value = parameter.value();
        ComparisonOperator comparison = parameter.operator();
        Column<?, ?> column = parameter.column();
        ColumnName colName = column.name();

        Filter result;
        switch (comparison) {
            case EQUALS:
                result = createFilter(colName, value, EQUAL); break;
            case GREATER_THAN:
                result = createFilter(colName, value, GREATER_THAN); break;
            case GREATER_OR_EQUALS:
                result = createFilter(colName, value, GREATER_OR_EQUAL); break;
            case LESS_THAN:
                result = createFilter(colName, value, LESS_THAN); break;
            case LESS_OR_EQUALS:
                result = createFilter(colName, value, LESS_OR_EQUAL); break;
            default:
                throw newIllegalStateException("Unsupported comparison operator `%s`",
                                               comparison);
        }
        return result;
    }

    private static void addIds(QueryBuilder builder, Subject<?, ?> subject) {
        ImmutableSet<?> ids = subject.id()
                                     .values();
        if (!ids.isEmpty()) {
            builder.byId(ids);
        }
    }
}
