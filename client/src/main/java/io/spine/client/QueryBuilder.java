/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.client.Order.Direction.OD_UNKNOWN;
import static io.spine.client.Order.Direction.UNRECOGNIZED;
import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * A builder for the {@link Query} instances.
 *
 * <p>None of the parameters set by builder methods are required. Call {@link #build()} to retrieve
 * the resulting {@link Query} instance.
 *
 * <p>Usage example:
 * <pre>
 *     {@code
 *     final Query query = factory().query()
 *                                  .select(Customer.class)
 *                                  .byId(getWestCoastCustomerIds())
 *                                  .withMask("name", "address", "email")
 *                                  .where(eq("type", "permanent"),
 *                                         eq("discountPercent", 10),
 *                                         eq("companySize", Company.Size.SMALL))
 *                                  .orderedBy("name", DESCENDING)
 *                                  .limit(20)
 *                                  .build();
 *     }
 * </pre>
 *
 * @author Dmytro Dashenkov
 * @see QueryFactory#select(Class) to start query building
 * @see io.spine.client.ColumnFilters for filter creation shortcuts
 * @see AbstractTargetBuilder for more details on this builders API
 */
public final class QueryBuilder extends AbstractTargetBuilder<Query, QueryBuilder> {

    private final QueryFactory queryFactory;

    private String orderingColumn;
    private Order.Direction direction;
    private long limit = 0;

    QueryBuilder(Class<? extends Message> targetType, QueryFactory queryFactory) {
        super(targetType);
        this.queryFactory = checkNotNull(queryFactory);
    }

    /**
     * Sets the sorting order to query, represented by the target column and order direction.
     *
     * @param column an entity column to sort by
     * @param direction a direction of the sorting
     * @return this builder instance
     */
    public QueryBuilder orderedBy(String column, Order.Direction direction) {
        checkNotNull(column);
        checkNotNull(direction);
        checkArgument(
                direction != OD_UNKNOWN && direction != UNRECOGNIZED,
                "Invalid ordering direction"
        );

        this.orderingColumn = column;
        this.direction = direction;
        return self();
    }

    /**
     * Limits the number of results returned by the query.
     *
     * @param count an amount of the results to be returned
     * @return this builder instance
     */
    public QueryBuilder limit(long count) {
        checkLimit(count);
        this.limit = count;
        return self();
    }

    private static void checkLimit(Number count) {
        checkArgument(count.longValue() > 0, "A Query limit must be more than 0.");
    }

    /**
     * Generates a new {@link Query} instance with current builder configuration.
     *
     * @return the built {@link Query}
     */
    @Override
    public Query build() {
        Optional<Order> order = order();
        Optional<Pagination> pagination = pagination();

        checkState(order.isPresent() || !pagination.isPresent(),
                   "Pagination cannot be set for unordered Queries");

        Target target = buildTarget();
        FieldMask mask = composeMask();

        if (pagination.isPresent()) {
            return queryFactory.composeQuery(target, order.get(), pagination.get(), mask);
        }
        if (order.isPresent()) {
            return queryFactory.composeQuery(target, order.get(), mask);
        }
        return queryFactory.composeQuery(target, mask);
    }

    private Optional<Pagination> pagination() {
        if (limit == 0) {
            return empty();
        }
        Pagination result = PaginationVBuilder.newBuilder()
                                              .setPageSize(limit)
                                              .build();
        return of(result);
    }

    private Optional<Order> order() {
        if (orderingColumn == null) {
            return empty();
        }
        Order result = OrderVBuilder.newBuilder()
                                    .setColumn(orderingColumn)
                                    .setDirection(direction)
                                    .build();
        return of(result);
    }

    @Override
    QueryBuilder self() {
        return this;
    }
}
