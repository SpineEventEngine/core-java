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
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;
import static io.spine.base.Identifier.pack;
import static io.spine.client.ColumnFilters.all;
import static io.spine.client.Targets.composeTarget;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

/**
 * An abstract base for builders that create {@link com.google.protobuf.Message} instances
 * including a {@link io.spine.client.Target Target}.
 *
 * @author Mykhailo Drachuk
 */
abstract class TargetBuilder<T extends Message, B extends TargetBuilder> {

    private final Class<? extends Message> targetType;

    /*
        All the optional fields are initialized only when and if set.
        The empty collections make effectively no influence, but null values allow us to create
        the query `Target` more efficiently.
     */

    private @Nullable Set<?> ids;
    private @Nullable Set<CompositeColumnFilter> columns;

    TargetBuilder(Class<? extends Message> targetType) {
        this.targetType = checkNotNull(targetType);
    }

    /**
     * Creates a new {@link io.spine.client.Target Target} based on the current builder
     * configuration.
     *
     * @return a new {@link io.spine.client.Query Query} or {@link io.spine.client.Topic Topic}
     *         target
     */
    Target buildTarget() {
        Set<Any> ids = composeIdPredicate();
        return composeTarget(targetType, ids, columns);
    }

    private @Nullable Set<Any> composeIdPredicate() {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        Function<Object, Any> transformFn = new Function<Object, Any>() {
            @Override
            public @Nullable Any apply(@Nullable Object o) {
                checkNotNull(o);
                return pack(o);
            }
        };
        Collection<Any> entityIds = ids.stream()
                                       .map(transformFn)
                                       .collect(Collectors.toList());
        Set<Any> result = newHashSet(entityIds);
        return result;
    }

    /**
     * Sets the ID predicate to the {@link io.spine.client.Query}.
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
     * @param ids
     *         the values of the IDs to look up
     * @return self for method chaining
     */
    public B byId(Iterable<?> ids) {
        checkNotNull(ids);
        this.ids = ImmutableSet.copyOf(ids);
        return self();
    }

    /**
     * Sets the ID predicate to the {@link io.spine.client.Query}.
     *
     * @param ids
     *         the values of the IDs to look up
     * @return self for method chaining
     * @see #byId(Iterable)
     */
    public B byId(Message... ids) {
        this.ids = ImmutableSet.copyOf(ids);
        return self();
    }

    /**
     * Sets the ID predicate to the {@link io.spine.client.Query}.
     *
     * @param ids
     *         the values of the IDs to look up
     * @return self for method chaining
     * @see #byId(Iterable)
     */
    public B byId(String... ids) {
        this.ids = ImmutableSet.copyOf(ids);
        return self();
    }

    /**
     * Sets the ID predicate to the {@link io.spine.client.Query}.
     *
     * @param ids
     *         the values of the IDs to look up
     * @return self for method chaining
     * @see #byId(Iterable)
     */
    public B byId(Integer... ids) {
        this.ids = ImmutableSet.copyOf(ids);
        return self();
    }

    /**
     * Sets the ID predicate to the {@link io.spine.client.Query}.
     *
     * @param ids
     *         the values of the IDs to look up
     * @return self for method chaining
     * @see #byId(Iterable)
     */
    public B byId(Long... ids) {
        this.ids = ImmutableSet.copyOf(ids);
        return self();
    }

    /**
     * Sets the Entity Column predicate to the {@link io.spine.client.Query}.
     *
     * <p>If there are no {@link io.spine.client.ColumnFilter}s (i.e. the passed array is empty),
     * all
     * the records will be retrieved regardless the Entity Columns values.
     *
     * <p>The multiple parameters passed into this method are considered to be joined in
     * a conjunction ({@link io.spine.client.CompositeColumnFilter.CompositeOperator#ALL ALL}
     * operator), i.e.
     * a record matches this query only if it matches all of these parameters.
     *
     * @param predicate
     *         the {@link io.spine.client.ColumnFilter}s to filter the requested entities by
     * @return self for method chaining
     * @see io.spine.client.ColumnFilters for a convinient way to create {@link
     *         io.spine.client.ColumnFilter} instances
     * @see #where(io.spine.client.CompositeColumnFilter...)
     */
    public B where(ColumnFilter... predicate) {
        CompositeColumnFilter aggregatingFilter = all(asList(predicate));
        columns = singleton(aggregatingFilter);
        return self();
    }

    /**
     * Sets the Entity Column predicate to the {@link io.spine.client.Query}.
     *
     * <p>If there are no {@link io.spine.client.ColumnFilter}s (i.e. the passed array is empty),
     * all
     * the records will be retrieved regardless the Entity Columns values.
     *
     * <p>The input values represent groups of {@linkplain io.spine.client.ColumnFilter column
     * filters} joined with
     * a {@linkplain io.spine.client.CompositeColumnFilter.CompositeOperator composite operator}.
     *
     * <p>The input filter groups are effectively joined between each other by
     * {@link io.spine.client.CompositeColumnFilter.CompositeOperator#ALL ALL} operator, i.e. a
     * record matches
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
     * <p>In the example above, the {@code Customer} records match the built query if they
     * represent
     * companies that have their company size between 50 and 1000 employees and either have been
     * established less than two years ago, or originate from Germany.
     *
     * <p>Note that the filters which belong to different {@link io.spine.client.ColumnFilters#all
     * all(...)} groups
     * may be represented as a single {@link io.spine.client.ColumnFilters#all all(...)} group. For
     * example, two
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
     * <p>The {@code Option 1} is recommended in this case, since the filters are grouped
     * logically,
     * though both builders produce effectively the same {@link io.spine.client.Query} instances.
     * Note, that
     * those instances may not be equal in terms of {@link Object#equals(Object)} method.
     *
     * @param predicate
     *         a number of {@link io.spine.client.CompositeColumnFilter} instances forming the query
     *         predicate
     * @return self for method chaining
     * @see io.spine.client.ColumnFilters for a convinient way to create {@link
     *         io.spine.client.CompositeColumnFilter} instances
     */
    public B where(CompositeColumnFilter... predicate) {
        columns = ImmutableSet.copyOf(predicate);
        return self();
    }

    public abstract T build();

    @Override
    public String toString() {
        return queryStringForFields(emptySet());
    }

    @SuppressWarnings("MethodWithMoreThanThreeNegations")
        // OK for this method as it's used primarily for debugging
    String queryStringForFields(@Nullable Set<String> fields) {
        String valueSeparator = "; ";
        StringBuilder sb = new StringBuilder();

        sb.append(TargetBuilder.class.getSimpleName())
          .append('(')
          .append("SELECT ")
          .append(fields == null || fields.isEmpty() ? '*' : fields)
          .append(" FROM ")
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

    /**
     * A typed instance of current Builder.
     *
     * @return {@code this} with the required compile-time type
     */
    protected abstract B self();
}
