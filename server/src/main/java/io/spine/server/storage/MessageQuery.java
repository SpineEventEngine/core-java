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

package io.spine.server.storage;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.server.entity.storage.CompositeQueryParameter;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.QueryParameters;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.storage.QueryParameters.FIELD_PARAMETERS;

/**
 * @author Alex Tymchenko
 */
public final class MessageQuery<I> {

    private static final QueryParameters EMPTY_PARAMS = QueryParameters.newBuilder()
                                                                       .build();
    private final ImmutableSet<I> ids;
    private final QueryParameters parameters;

    /**
     * Creates new instance of {@code EntityQuery}.
     *
     * @param ids
     *         the accepted ID values
     * @param parameters
     *         the values of the {@link Column}s stored in a mapping of the
     *         {@link Column}'s metadata to the (multiple) acceptable values;
     *         if there are no values, all the values are matched upon such a column
     * @return new instance of {@code EntityQuery}
     */
    public static <I> MessageQuery<I> of(Iterable<I> ids, QueryParameters parameters) {
        checkNotNull(ids);
        checkNotNull(parameters);
        return new MessageQuery<>(ids, parameters);
    }

    public static <I> MessageQuery<I> of(Iterable<I> ids) {
        checkNotNull(ids);
        return new MessageQuery<>(ids, EMPTY_PARAMS);
    }

    public static <I> MessageQuery<I> of(QueryParameters parameters) {
        checkNotNull(parameters);
        return new MessageQuery<>(ImmutableSet.of(), parameters);
    }

    public static <I> MessageQuery<I> all() {
        return new MessageQuery<>(ImmutableSet.of(), EMPTY_PARAMS);
    }

    private MessageQuery(Iterable<I> ids, QueryParameters parameters) {
        this.ids = ImmutableSet.copyOf(ids);
        this.parameters = parameters;
    }

    public static <I, V> MessageQuery<I> byColumn(MessageColumn<?, ?> column, V value) {
        QueryParameters queryParams = QueryParameters.eq(column, value);
        return of(queryParams);
    }

    /**
     * Obtains an immutable set of accepted ID values.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // Immutable structure
    public Set<I> getIds() {
        return ids;
    }

    /**
     * Obtains a {@link Map} of the {@link Column} metadata to the column required value.
     */
    public QueryParameters getParameters() {
        return parameters;
    }

    public MessageQuery<I> append(QueryParameters moreParams) {
        ImmutableList<CompositeQueryParameter> toAdd = ImmutableList.copyOf(moreParams.iterator());
        QueryParameters newParams = QueryParameters.newBuilder(this.parameters)
                                                   .addAll(toAdd)
                                                   .build();
        return new MessageQuery<>(ids, newParams);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EntityQuery<?> query = (EntityQuery<?>) o;
        return Objects.equal(getIds(), query.getIds()) &&
                Objects.equal(getParameters(), query.getParameters());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getIds(), getParameters());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("ID filter", ids)
                          .add(FIELD_PARAMETERS, parameters)
                          .toString();
    }
}
