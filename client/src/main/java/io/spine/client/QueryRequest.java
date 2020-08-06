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
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.FieldMask;
import io.spine.base.EntityState;
import io.spine.query.Column;
import io.spine.query.ComparisonOperator;
import io.spine.query.EntityQuery;
import io.spine.query.QueryPredicate;
import io.spine.query.Subject;
import io.spine.query.SubjectParameter;
import org.checkerframework.checker.nullness.qual.Nullable;

import static io.spine.client.Filters.eq;
import static io.spine.client.Filters.ge;
import static io.spine.client.Filters.gt;
import static io.spine.client.Filters.le;
import static io.spine.client.Filters.lt;
import static io.spine.client.OrderBy.Direction.ASCENDING;
import static io.spine.client.OrderBy.Direction.DESCENDING;
import static io.spine.query.Direction.ASC;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Fetches the results of the specified {@link EntityQuery}.
 *
 * @param <S>
 *         the type of the queried entity states
 */
final class QueryRequest<S extends EntityState<?>> extends ClientRequest {

    /** The type of entities returned by the request. */
    private final Class<S> entityStateType;

    /**
     * The builder of a Proto-based {@code Query} used to transmit the definition
     * of the entity query over the wire onto the server side.
     */
    private final QueryBuilder builder;

    /**
     * The query to run.
     */
    private final EntityQuery<?, S, ?> entityQuery;

    /**
     * Creates an instance of the request based on the passed parent {@code ClientRequest},
     * for the given {@code EntityQuery}.
     */
    QueryRequest(ClientRequest parent, EntityQuery<?, S, ?> query) {
        super(parent);
        this.entityQuery = query;
        this.entityStateType = query.subject()
                                    .recordType();
        ActorRequestFactory factory = client().requestOf(user());
        this.builder = factory.query()
                              .select(entityStateType);
    }

    /**
     * Executes and obtains results of the query.
     */
    ImmutableList<S> run() {
        Query query = toProtoQuery(entityQuery);
        ImmutableList<S> result = client().read(query, entityStateType);
        return result;
    }

    private Query toProtoQuery(EntityQuery<?, S, ?> query) {
        Subject<?, S> subject = query.subject();
        addIds(subject);
        addPredicates(subject);
        addOrdering(query);
        addLimit(query);
        addFieldMask(query);
        return builder.build();
    }

    private void addFieldMask(EntityQuery<?, S, ?> query) {
        FieldMask originMask = query.mask();
        if (!originMask.equals(FieldMask.getDefaultInstance())) {
            builder.withMask(originMask.getPathsList());
        }
    }

    private void addLimit(EntityQuery<?, S, ?> query) {
        Integer originLimit = query.limit();
        if (originLimit != null) {
            builder.limit(originLimit);
        }
    }

    private void addOrdering(EntityQuery<?, S, ?> query) {
        for (io.spine.query.OrderBy<?, S> orderBy : query.ordering()) {
            String columnName = orderBy.column()
                                       .name()
                                       .value();
            OrderBy.Direction direction = orderBy.direction() == ASC ? ASCENDING : DESCENDING;
            builder.orderBy(columnName, direction);
        }
    }

    private void addPredicates(Subject<?, S> subject) {
        ImmutableList<QueryPredicate<S>> predicates = subject.predicates();
        for (QueryPredicate<S> predicate : predicates) {
            addParameters(predicate);
        }
    }

    private void addParameters(QueryPredicate<S> predicate) {
        ImmutableList<SubjectParameter<S, ?, ?>> parameters = predicate.parameters();
        for (SubjectParameter<S, ?, ?> parameter : parameters) {
            Column<S, ?> column = parameter.column();
            ComparisonOperator operator = parameter.operator();
            Object value = parameter.value();

            @Nullable Filter filter;
            switch (operator) {
                case EQUALS:
                    filter = eq(column, value); break;
                case GREATER_THAN:
                    filter = gt(column, value); break;
                case GREATER_OR_EQUALS:
                    filter = ge(column, value); break;
                case LESS_THAN:
                    filter = lt(column, value); break;
                case LESS_OR_EQUALS:
                    filter = le(column, value); break;
                case NOT_EQUALS:
                default:
                    throw newIllegalStateException("Unsupported comparison operator `%s`",
                                                   operator);
            }
            builder.where(filter);
        }
    }

    private void addIds(Subject<?, S> subject) {
        ImmutableSet<?> ids = subject.id()
                                     .values();
        if (!ids.isEmpty()) {
            builder.byId(ids);
        }
    }
}
