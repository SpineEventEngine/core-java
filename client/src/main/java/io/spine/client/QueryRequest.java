/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.protobuf.Message;

import java.util.List;
import java.util.function.Function;

/**
 * Allows to create a post a query for messages of the given type.
 *
 * <p>None of the parameters set by the builder methods are required. Call {@link #query()} to
 * retrieve the results of the query.
 *
 * <p>Usage example:
 * <pre>{@code
 * Query query = client.onBehalfOf(currentUser)
 *          .select(Customer.class)
 *          .byId(westCoastCustomerIds())
 *          .withMask("name", "address", "email")
 *          .where(eq("type", "permanent"),
 *                 eq("discountPercent", 10),
 *                 eq("companySize", Company.Size.SMALL))
 *          .orderBy("name", ASCENDING)
 *          .limit(20)
 *          .build();
 * }</pre>
 *
 * <p>Filtering by field values (via {@link #where(Filter...)} and
 * {@link #where(CompositeFilter...)} methods) can be composed using the {@link Filters}
 * utility class.
 *
 * @param <M>
 *         the type of the queried messages
 * @see Filters
 */
public final class QueryRequest<M extends Message>
        extends FilteringRequest<M, Query, QueryBuilder, QueryRequest<M>> {

    QueryRequest(ClientRequest parent, Class<M> type) {
        super(parent, type);
    }

    /**
     * Sets the sorting order by the target column and order direction.
     *
     * @param column
     *         the column to sort by
     * @param direction
     *         sorting direction
     */
    public QueryRequest<M> orderBy(String column, OrderBy.Direction direction) {
        builder().orderBy(column, direction);
        return this;
    }

    /**
     * Limits the number of results returned by the query.
     *
     * @param count
     *         the number of results to be returned
     */
    public QueryRequest<M> limit(int count) {
        builder().limit(count);
        return this;
    }

    /**
     * Obtains the results of the query.
     */
    public List<M> query() {
        Query query = builder().build();
        List<M> result = client().query(query);
        return result;
    }

    @Override
    Function<ActorRequestFactory, QueryBuilder> builderFn() {
        return (f) -> f.query().select(messageType());
    }

    @Override
    QueryRequest<M> self() {
        return this;
    }
}
