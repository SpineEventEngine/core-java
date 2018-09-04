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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A builder for the {@link Query} instances.
 *
 * <p>None of the parameters set by builder methods are required. Call {@link #build()} to retrieve
 * the resulting instance of {@link Query}.
 *
 * <p>Usage example:
 * <pre>
 *     {@code
 *     final Query query = factory().query()
 *                                  .select(Customer.class)
 *                                  .byId(getWestCostCustomerIds())
 *                                  .withMask("name", "address", "email")
 *                                  .where(eq("type", "permanent"),
 *                                         eq("discountPercent", 10),
 *                                         eq("companySize", Company.Size.SMALL))
 *                                  .build();
 *     }
 * </pre>
 *
 * @author Dmytro Dashenkov
 * @see QueryFactory#select(Class) to start query building
 * @see io.spine.client.ColumnFilters for filter creation shortcuts
 * @see io.spine.client.TargetBuilder for more details on this builders API
 */
public final class QueryBuilder extends TargetBuilder<Query, QueryBuilder> {

    private final QueryFactory queryFactory;

    QueryBuilder(Class<? extends Message> targetType, QueryFactory queryFactory) {
        super(targetType);
        this.queryFactory = checkNotNull(queryFactory);
    }

    /**
     * Generates a new {@link Query} instance with current builder configuration.
     *
     * @return the built {@link Query}
     */
    @Override
    public Query build() {
        Target target = buildTarget();
        FieldMask mask = composeMask();
        Query query = queryFactory.composeQuery(target, mask);
        return query;
    }

    @Override
    QueryBuilder self() {
        return this;
    }
}
