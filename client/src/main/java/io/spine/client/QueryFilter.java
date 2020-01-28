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

import io.spine.base.EntityColumn;
import io.spine.base.EntityState;
import io.spine.client.Filter.Operator;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.Filter.Operator.EQUAL;
import static io.spine.client.Filter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.Filter.Operator.GREATER_THAN;
import static io.spine.client.Filter.Operator.LESS_OR_EQUAL;
import static io.spine.client.Filter.Operator.LESS_THAN;
import static io.spine.client.Filters.checkSupportedOrderingComparisonType;
import static io.spine.client.Filters.createFilter;

/**
 * A query filter which targets a {@linkplain EntityColumn column} of an entity.
 */
public final class QueryFilter extends TypedFilter<EntityState> {

    private static final long serialVersionUID = 0L;

    private QueryFilter(EntityColumn column, Object expected, Operator operator) {
        super(createFilter(column.name(), expected, operator));
    }

    /**
     * Creates a new equality filter.
     *
     * @param column
     *         the entity column from which the actual value is taken
     * @param value
     *         the expected value
     */
    public static QueryFilter eq(EntityColumn column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        return new QueryFilter(column, value, EQUAL);
    }

    /**
     * Creates a new "greater than" filter.
     *
     * <p>NOTE: not all value types are supported for ordering comparison. See {@link Filters} for
     * details.
     *
     * @param column
     *         the entity column from which the actual value is taken
     * @param value
     *         the expected value
     */
    public static QueryFilter gt(EntityColumn column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new QueryFilter(column, value, GREATER_THAN);
    }

    /**
     * Creates a new "less than" filter.
     *
     * <p>NOTE: not all value types are supported for ordering comparison. See {@link Filters} for
     * details.
     *
     * @param column
     *         the entity column from which the actual value is taken
     * @param value
     *         the expected value
     */
    public static QueryFilter lt(EntityColumn column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new QueryFilter(column, value, LESS_THAN);
    }

    /**
     * Creates a new "greater than or equals" filter.
     *
     * <p>NOTE: not all value types are supported for ordering comparison. See {@link Filters} for
     * details.
     *
     * @param column
     *         the entity column from which the actual value is taken
     * @param value
     *         the expected value
     */
    public static QueryFilter ge(EntityColumn column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new QueryFilter(column, value, GREATER_OR_EQUAL);
    }

    /**
     * Creates a new "less than or equals" filter.
     *
     * <p>NOTE: not all value types are supported for ordering comparison. See {@link Filters} for
     * details.
     *
     * @param column
     *         the entity column from which the actual value is taken
     * @param value
     *         the expected value
     */
    public static QueryFilter le(EntityColumn column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new QueryFilter(column, value, LESS_OR_EQUAL);
    }
}
