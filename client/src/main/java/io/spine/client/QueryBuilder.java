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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A builder for the {@link Query} instances.
 *
 * <p>The API of this class is inspired by the SQL syntax.
 *
 * <p>Calling any of the methods is optional. Call {@link #build()} to retrieve the resulting
 * instance of {@link Query}.
 *
 * <p>Calling any of the builder methods overrides the previous call of the given method or
 * any of its overloads. For example, calling sequentially
 * <pre>
 *     {@code
 *     builder.withMask(mask1)
 *            .withMask(mask2)
 *            // optionally some other invocations
 *            .withMask(mask3)
 *            .build();
 *     }
 * </pre>
 * is equivalent to calling
 * <pre>
 *     {@code
 *     builder.withMask(mask3)
 *            .build();
 *     }
 * </pre>
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
 * @author Mykhailo Drachuk
 * @see QueryFactory#select(Class)
 * @see ColumnFilters for the query parameters
 */
public final class QueryBuilder extends TargetBuilder<Query, QueryBuilder> {

    private final QueryFactory queryFactory;

    private @Nullable Set<String> fieldMask;

    QueryBuilder(Class<? extends Message> targetType, QueryFactory queryFactory) {
        super(targetType);
        this.queryFactory = checkNotNull(queryFactory);
    }

    /**
     * Sets the entity fields to retrieve.
     *
     * <p>The names of the fields must be formatted according to the {@link FieldMask}
     * specification.
     *
     * <p>If there are no fields (i.e. an empty {@link Iterable} is passed), all the fields will
     * be retrieved.
     *
     * @param fieldNames
     *         the fields to query
     * @return self for method chaining
     */
    public QueryBuilder withMask(Iterable<String> fieldNames) {
        checkNotNull(fieldNames);
        this.fieldMask = ImmutableSet.copyOf(fieldNames);
        return this;
    }

    /**
     * Sets the entity fields to retrieve.
     *
     * <p>The names of the fields must be formatted according to the {@link FieldMask}
     * specification.
     *
     * <p>If there are no fields (i.e. an empty array is passed), all the fields will
     * be retrieved.
     *
     * @param fieldNames
     *         the fields to query
     * @return self for method chaining
     */
    public QueryBuilder withMask(String... fieldNames) {
        this.fieldMask = ImmutableSet.<String>builder()
                .add(fieldNames)
                .build();
        return this;
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

    private @Nullable FieldMask composeMask() {
        if (fieldMask == null || fieldMask.isEmpty()) {
            return null;
        }
        FieldMask mask = FieldMask.newBuilder()
                                  .addAllPaths(fieldMask)
                                  .build();
        return mask;
    }

    @Override
    protected QueryBuilder self() {
        return this;
    }

    @Override
    public String toString() {
        return queryStringForFields(fieldMask);
    }
}
