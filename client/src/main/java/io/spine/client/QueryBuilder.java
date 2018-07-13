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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.base.Identifier;

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Sets.newHashSet;
import static io.spine.client.ColumnFilters.all;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

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
 * @see QueryFactory#select(Class)
 * @see ColumnFilters for the query parameters
 */
public final class QueryBuilder {

    private final QueryFactory queryFactory;
    private final Class<? extends Message> targetType;

    /*
        All the optional fields are initialized only when and if set.
        The empty collections make effectively no influence, but null values allow us to create
        the query `Target` more efficiently.
     */

    @Nullable
    private Set<?> ids;

    @Nullable
    private Set<CompositeColumnFilter> columns;

    @Nullable
    private Set<String> fieldMask;

    QueryBuilder(Class<? extends Message> targetType, QueryFactory queryFactory) {
        this.targetType = checkNotNull(targetType);
        this.queryFactory = checkNotNull(queryFactory);
    }

    /**
     * Sets the ID predicate to the {@link Query}.
     *
     * <p>Though it's not prohibited at compile-time, please make sure to pass instances of the
     * same type to the argument of this method. Moreover, the instances must be of the type of
     * the query target type identifier.
     *
     * <p>This method or any of its overloads do not check these
     * constrains an assume they are followed by the caller.
     *
     * <p>If there are no IDs (i.e. and empty {@link Iterable} is passed), the query retrieves all
     * the records regardless their IDs.
     *
     * @param ids the values of the IDs to look up
     * @return self for method chaining
     */
    public QueryBuilder byId(Iterable<?> ids) {
        checkNotNull(ids);
        this.ids = ImmutableSet.copyOf(ids);
        return this;
    }

    /**
     * Sets the ID predicate to the {@link Query}.
     *
     * @param ids the values of the IDs to look up
     * @return self for method chaining
     * @see #byId(Iterable)
     */
    public QueryBuilder byId(Message... ids) {
        this.ids = ImmutableSet.copyOf(ids);
        return this;
    }

    /**
     * Sets the ID predicate to the {@link Query}.
     *
     * @param ids the values of the IDs to look up
     * @return self for method chaining
     * @see #byId(Iterable)
     */
    public QueryBuilder byId(String... ids) {
        this.ids = ImmutableSet.copyOf(ids);
        return this;
    }

    /**
     * Sets the ID predicate to the {@link Query}.
     *
     * @param ids the values of the IDs to look up
     * @return self for method chaining
     * @see #byId(Iterable)
     */
    public QueryBuilder byId(Integer... ids) {
        this.ids = ImmutableSet.copyOf(ids);
        return this;
    }

    /**
     * Sets the ID predicate to the {@link Query}.
     *
     * @param ids the values of the IDs to look up
     * @return self for method chaining
     * @see #byId(Iterable)
     */
    public QueryBuilder byId(Long... ids) {
        this.ids = ImmutableSet.copyOf(ids);
        return this;
    }

    /**
     * Sets the Entity Column predicate to the {@link Query}.
     *
     * <p>If there are no {@link ColumnFilter}s (i.e. the passed array is empty), all
     * the records will be retrieved regardless the Entity Columns values.
     *
     * <p>The multiple parameters passed into this method are considered to be joined in
     * a conjunction ({@link CompositeColumnFilter.CompositeOperator#ALL ALL} operator), i.e.
     * a record matches this query only if it matches all of these parameters.
     *
     * @param predicate the {@link ColumnFilter}s to filter the requested entities by
     * @return self for method chaining
     * @see ColumnFilters for a convinient way to create {@link ColumnFilter} instances
     * @see #where(CompositeColumnFilter...)
     */
    public QueryBuilder where(ColumnFilter... predicate) {
        CompositeColumnFilter aggregatingFilter = all(asList(predicate));
        columns = singleton(aggregatingFilter);
        return this;
    }

    /**
     * Sets the Entity Column predicate to the {@link Query}.
     *
     * <p>If there are no {@link ColumnFilter}s (i.e. the passed array is empty), all
     * the records will be retrieved regardless the Entity Columns values.
     *
     * <p>The input values represent groups of {@linkplain ColumnFilter column filters} joined with
     * a {@linkplain CompositeColumnFilter.CompositeOperator composite operator}.
     *
     * <p>The input filter groups are effectively joined between each other by
     * {@link CompositeColumnFilter.CompositeOperator#ALL ALL} operator, i.e. a record matches
     * this query if it matches all the composite filters.
     *
     * <p>Example of usage:
     * <pre>
     *     {@code
     *     factory.select(Customer.class)
     *            // Possibly other parameters
     *            .where(all(ge("companySize", 50), le("companySize", 1000)),
     *                   either(gt("establishedTime", twoYearsAgo), eq("country", "Germany")))
     *            .build();
     *     }
     * </pre>
     *
     * <p>In the example above, the {@code Customer} records match the built query if they represent
     * companies that have their company size between 50 and 1000 employees and either have been
     * established less than two years ago, or originate from Germany.
     *
     * <p>Note that the filters which belong to different {@link ColumnFilters#all all(...)} groups
     * may be represented as a single {@link ColumnFilters#all all(...)} group. For example, two
     * following queries would be identical:
     * <pre>
     *     {@code
     *     // Option 1
     *     factory.select(Customer.class)
     *            .where(all(
     *                       eq("country", "Germany")),
     *                   all(
     *                       ge("companySize", 50),
     *                       le("companySize", 100)))
     *            .build();
     *
     *     // Option 2 (identical)
     *     factory.select(Customer.class)
     *            .where(all(
     *                       eq("country", "Germany"),
     *                       ge("companySize", 50),
     *                       le("companySize", 100)))
     *            .build();
     *     }
     * </pre>
     *
     * <p>The {@code Option 1} is recommended in this case, since the filters are grouped logically,
     * though both builders produce effectively the same {@link Query} instances. Note, that
     * those instances may not be equal in terms of {@link Object#equals(Object)} method.
     *
     * @param predicate a number of {@link CompositeColumnFilter} instances forming the query
     *                  predicate
     * @return self for method chaining
     * @see ColumnFilters for a convinient way to create {@link CompositeColumnFilter} instances
     */
    public QueryBuilder where(CompositeColumnFilter... predicate) {
        columns = ImmutableSet.copyOf(predicate);
        return this;
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
     * @param fieldNames the fields to query
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
     * @param fieldNames the fields to query
     * @return self for method chaining
     */
    public QueryBuilder withMask(String... fieldNames) {
        this.fieldMask = ImmutableSet.<String>builder()
                                     .add(fieldNames)
                                     .build();
        return this;
    }

    /**
     * Generates a new instance of {@link Query} regarding all the set parameters.
     *
     * @return the built {@link Query}
     */
    public Query build() {
        FieldMask mask = composeMask();
        Set<Any> entityIds = composeIdPredicate();

        Query result = queryFactory.composeQuery(targetType, entityIds, columns, mask);
        return result;
    }

    @Nullable
    private FieldMask composeMask() {
        if (fieldMask == null || fieldMask.isEmpty()) {
            return null;
        }
        FieldMask mask = FieldMask.newBuilder()
                                        .addAllPaths(fieldMask)
                                        .build();
        return mask;
    }

    @Nullable
    private Set<Any> composeIdPredicate() {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        Collection<Any> entityIds = transform(ids, new Function<Object, Any>() {
            @Nullable
            @Override
            public Any apply(@Nullable Object o) {
                checkNotNull(o);
                Any id = Identifier.pack(o);
                return id;
            }
        });
        Set<Any> result = newHashSet(entityIds);
        return result;
    }

    @SuppressWarnings("MethodWithMoreThanThreeNegations")
        // OK for this method as it's used primarily for debugging
    @Override
    public String toString() {
        String valueSeparator = "; ";
        StringBuilder sb = new StringBuilder();
        sb.append(QueryBuilder.class.getSimpleName())
          .append('(')
          .append("SELECT ");
        if (fieldMask == null || fieldMask.isEmpty()) {
            sb.append('*');
        } else {
            sb.append(fieldMask);
        }
        sb.append(" FROM ")
          .append(targetType.getSimpleName())
          .append(" WHERE (");
        if (ids != null && !ids.isEmpty()) {
            sb.append("id IN ")
              .append(ids)
              .append(valueSeparator);
        }
        if (columns != null && !columns.isEmpty()) {
            sb.append("AND columns: ")
              .append(columns);
        }
        sb.append(");");
        return sb.toString();
    }
}
