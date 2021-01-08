/*
 * Copyright 2021, TeamDev. All rights reserved.
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
import io.spine.base.EntityColumn;
import io.spine.base.EntityState;

import java.util.function.Function;

import static io.spine.client.Filters.extractFilters;

/**
 * Allows to create a post a query for messages of the given type.
 *
 * <p>None of the parameters set by the builder methods are required. Call {@link #run()} to
 * retrieve the results of the query.
 *
 * <p>Usage example:
 * <pre>{@code
 * ImmutableList<Customer> customers = client.onBehalfOf(currentUser)
 *          .select(Customer.class)
 *          .byId(westCoastCustomerIds())
 *          .withMask("name", "address", "email")
 *          .where(eq(Customer.Column.type(), "permanent"),
 *                 eq(Customer.Column.discountPercent(), 10),
 *                 eq(Customer.Column.companySize(), Company.Size.SMALL))
 *          .orderBy(Customer.Column.name(), ASCENDING)
 *          .limit(20)
 *          .run();
 * }</pre>
 *
 * <p>Filtering by field values (via {@link #where(QueryFilter...)} and
 * {@link #where(CompositeQueryFilter...)} methods) can be composed using the {@link Filters}
 * utility class.
 *
 * @param <S>
 *         the type of the queried entity states
 * @see Filters
 */
public final class QueryRequest<S extends EntityState>
        extends FilteringRequest<S, Query, QueryBuilder, QueryRequest<S>> {

    QueryRequest(ClientRequest parent, Class<S> type) {
        super(parent, type);
    }

    /**
     * Configures the request to return results matching all the passed filters.
     */
    public QueryRequest<S> where(QueryFilter... filter) {
        builder().where(extractFilters(filter));
        return this;
    }

    /**
     * Configures the request to return results matching all the passed filters.
     */
    public QueryRequest<S> where(CompositeQueryFilter... filter) {
        builder().where(extractFilters(filter));
        return this;
    }

    /**
     * Sets the sorting order by the target column and order direction.
     *
     * @param column
     *         the column to sort by
     * @param direction
     *         sorting direction
     * @deprecated Please use the {@linkplain #orderBy(EntityColumn, OrderBy.Direction) alternative}
     *             which relies on strongly-typed columns instead.
     */
    @Deprecated
    public QueryRequest<S> orderBy(String column, OrderBy.Direction direction) {
        builder().orderBy(column, direction);
        return this;
    }

    /**
     * Sets the sorting order by the target column and order direction.
     *
     * @param column
     *         the column to sort by
     * @param direction
     *         sorting direction
     */
    public QueryRequest<S> orderBy(EntityColumn column, OrderBy.Direction direction) {
        String columnName = column.name()
                                  .value();
        builder().orderBy(columnName, direction);
        return this;
    }

    /**
     * Limits the number of results returned by the query.
     *
     * @param count
     *         the number of results to be returned
     */
    public QueryRequest<S> limit(int count) {
        builder().limit(count);
        return this;
    }

    /**
     * Obtains results of the query.
     */
    public ImmutableList<S> run() {
        Query query = builder().build();
        ImmutableList<S> result = client().read(query, messageType());
        return result;
    }

    @Override
    Function<ActorRequestFactory, QueryBuilder> builderFn() {
        return (f) -> f.query().select(messageType());
    }

    @Override
    QueryRequest<S> self() {
        return this;
    }
}
