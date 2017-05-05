/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.client;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.spine3.base.Identifiers;
import org.spine3.json.Json;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Sets.newHashSet;

import static org.spine3.protobuf.AnyPacker.unpack;

/**
 * A builder for the {@link Query} instances.
 *
 * <p>The API of this class is inspired by the SQL syntax.
 *
 * <p>Calling any of the methods is optional. Call {@link #build() build()} to retrieve
 * the instance of {@link Query}.
 *
 * <p>Calling of any of the builder methods overrides the previous call of the given method of
 * any of its overloads.
 *
 * <p>Usage example:
 * <pre>
 *     {@code
 *     final Query query = factory().query()
 *                                  .select(Customer.class)
 *                                  .byId(getWestCostCustomersIds())
 *                                  .withMask("name", "address", "email")
 *                                  .where({@link QueryParameter#eq eq}("type", "permanent"),
 *                                         eq("discountPercent", 10),
 *                                         eq("companySize", Company.Size.SMALL))
 *                                  .build();
 *     }
 * </pre>
 *
 * @see QueryFactory#select(Class) for the intialization
 */
public final class QueryBuilder {

    private final QueryFactory queryFactory;
    private final Class<? extends Message> targetType;

    // All the optional fields are initialized only when and if set
    // The empty collections make effectively no influence, but null values allow us to create
    // the query `Target` more efficiently

    @Nullable
    private Set<?> ids;

    @Nullable
    private Map<String, Any> columns;

    @Nullable
    private Set<String> fieldMask;

    QueryBuilder(Class<? extends Message> targetType, QueryFactory queryFactory) {
        this.targetType = checkNotNull(targetType);
        this.queryFactory = checkNotNull(queryFactory);
    }

    /**
     * Sets the ID predicate to the {@linkplain Query}.
     *
     * <p>Though it's not prohibited at compile-time, please make sure to pass instances of the
     * same type to the argument of this method. Moreover, the instances must be of the type of
     * the query target type identifier. This method or any of its overload do not check these
     * constrains an assume they are followed by the caller.
     *
     * <p>If there are no IDs (i.e. and empty {@link Iterable} is passed), the query will
     * retrieve all the records regardless their IDs.
     *
     * @param ids the values of the IDs to look up
     * @return self for method chaining
     */
    public QueryBuilder byId(Iterable<?> ids) {
        this.ids = ImmutableSet.builder()
                               .add(ids)
                               .build();
        return this;
    }

    /**
     * Sets the ID predicate to the {@linkplain Query}.
     *
     * @param ids the values of the IDs to look up
     * @return self for method chaining
     * @see #byId(Iterable)
     */
    public QueryBuilder byId(Message... ids) {
        this.ids = ImmutableSet.<Message>builder()
                               .add(ids)
                               .build();
        return this;
    }

    /**
     * Sets the ID predicate to the {@linkplain Query}.
     *
     * @param ids the values of the IDs to look up
     * @return self for method chaining
     * @see #byId(Iterable)
     */
    public QueryBuilder byId(String... ids) {
        this.ids = ImmutableSet.<String>builder()
                               .add(ids)
                               .build();
        return this;
    }

    /**
     * Sets the ID predicate to the {@linkplain Query}.
     *
     * @param ids the values of the IDs to look up
     * @return self for method chaining
     * @see #byId(Iterable)
     */
    public QueryBuilder byId(Integer... ids) {
        this.ids = ImmutableSet.<Integer>builder()
                               .add(ids)
                               .build();
        return this;
    }

    /**
     * Sets the ID predicate to the {@linkplain Query}.
     *
     * @param ids the values of the IDs to look up
     * @return self for method chaining
     * @see #byId(Iterable)
     */
    public QueryBuilder byId(Long... ids) {
        this.ids = ImmutableSet.<Long>builder()
                               .add(ids)
                               .build();
        return this;
    }

    /**
     * Sets the Entity Column predicate to the {@linkplain Query}.
     *
     * <p>If there are no {@link QueryParameter}s (i.e. the passed array is empty), all
     * the records will be retrieved regardless the Entity Columns values.
     *
     * <p>The multiple parameters passed into this method are considered to be joined in
     * a conjunction ({@code AND} operator), i.e. a record matches this query only if it matches
     * all of these parameters.
     *
     * <p>The disjunctive filters currently are not supported.
     *
     * @param predicate the {@link QueryParameter}s to filter the requested entities by
     * @return self for method chaining
     * @see QueryParameter
     */
    public QueryBuilder where(QueryParameter... predicate) {
        final ImmutableMap.Builder<String, Any> mapBuilder = ImmutableMap.builder();
        for (QueryParameter param : predicate) {
            mapBuilder.put(param.getColumnName(), param.getValue());
        }
        columns = mapBuilder.build();
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
        final FieldMask mask = composeMask();
        // Implying AnyPacker.pack to be idempotent
        final Set<Any> entityIds = composeIdPredicate();

        final Query result = queryFactory.composeQuery(targetType, entityIds, columns, mask);
        return result;
    }

    @Nullable
    private FieldMask composeMask() {
        if (fieldMask == null || fieldMask.isEmpty()) {
            return null;
        }
        final FieldMask mask = FieldMask.newBuilder()
                                        .addAllPaths(fieldMask)
                                        .build();
        return mask;
    }

    @Nullable
    private Set<Any> composeIdPredicate() {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        final Collection<Any> entityIds = transform(ids, new Function<Object, Any>() {
            @Nullable
            @Override
            public Any apply(@Nullable Object o) {
                checkNotNull(o);
                final Any id = Identifiers.idToAny(o);
                return id;
            }
        });
        final Set<Any> result = newHashSet(entityIds);
        return result;
    }

    @SuppressWarnings("MethodWithMoreThanThreeNegations")
        // OK for this method as it's used primarily for debugging
    @Override
    public String toString() {
        final String valueSeparator = "; ";
        final StringBuilder sb = new StringBuilder();
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
            for (Map.Entry<String, Any> column : columns.entrySet()) {
                sb.append(column.getKey())
                  .append('=')
                  .append(Json.toCompactJson(unpack(column.getValue())))
                  .append(valueSeparator);
            }
        }
        sb.append(");");
        return sb.toString();
    }
}
