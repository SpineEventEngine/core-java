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

package org.spine3.server.entity.storage;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import org.spine3.client.AggregatingColumnFilter;
import org.spine3.client.ColumnFilter;

import java.io.Serializable;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The parameters of an {@link EntityQuery}.
 *
 * @author Dmytro Dashenkov
 */
public final class QueryParameters implements Iterable<AggregatingColumnFilter>, Serializable {

    private static final long serialVersionUID = 526400152015141524L;

    private final ImmutableCollection<AggregatingColumnFilter> parameters;

    private QueryParameters(Builder builder) {
        this.parameters = builder.getParameters()
                                 .build();
    }

    /**
     * Returns the {@link ColumnFilter} targeted at the given {@link Column}, or
     * {@link Optional#absent()} is there is no such {@link ColumnFilter}.
     *
     * @param column the target {@link Column} of the result {@link ColumnFilter}
     * @return the corresponding {@link ColumnFilter
     */
    @Deprecated
    public ImmutableCollection<ColumnFilter> get(Column column) {
//        final ImmutableCollection<ColumnFilter> filters = parameters.get(column);
//        return filters;
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an iterator over the {@linkplain ColumnFilter column filters}.
     *
     * @return an {@link Iterator}.
     */
    @Override
    public Iterator<AggregatingColumnFilter> iterator() {
        return parameters.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryParameters that = (QueryParameters) o;
        return Objects.equal(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parameters);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * The builder for the {@code QueryParameters}.
     */
    public static class Builder {

        private final ImmutableCollection.Builder<AggregatingColumnFilter> parameters;

        private Builder() {
            parameters = ImmutableList.builder();
        }

        /**
         * Put the Query parameter represented by the arguments into the resulting instance of
         * {@code QueryParameters}.
         *
         * @return self for method chaining
         */
        public Builder put(Column column, ColumnFilter columnFilter) {
            checkNotNull(column);
            checkNotNull(columnFilter);

            parameters.add(column, columnFilter);

            return this;
        }

        private ImmutableMultimap.Builder<Column, ColumnFilter> getParameters() {
            return parameters;
        }

        /**
         * Creates a new instance of {@code QueryParameters} with the collected parameters.
         *
         * @return a new instance of {@code QueryParameters}
         */
        public QueryParameters build() {
            return new QueryParameters(this);
        }
    }
}
