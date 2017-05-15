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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import org.spine3.client.QueryOperator;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.copyOf;
import static org.spine3.client.QueryOperator.EQUAL;

/**
 * The parameters of an {@link EntityQuery}.
 *
 * <p>The values of the parameters are mapped to the operators they are declared with.
 * // TODO:2017-05-15:dmytro.dashenkov: Improve javadoc.
 *
 * @author Dmytro Dashenkov
 */
public final class QueryParameters {

    private final ImmutableMap<QueryOperator, Map<Column<?>, Object>> parameters;

    private QueryParameters(Map<QueryOperator, Map<Column<?>, Object>> parameters) {
        this.parameters = copyOf(parameters);
    }

    @VisibleForTesting // Should not be used in the production code
    static QueryParameters fromValues(Map<Column<?>, Object> values) {
        return new QueryParameters(ImmutableMap.of(EQUAL, values));
    }

    public ImmutableMap<Column<?>, Object> getParams(QueryOperator operator) {
        Map<Column<?>, Object> params = parameters.get(operator);
        params = params == null
                ? Collections.<Column<?>, Object>emptyMap()
                : params;
        return copyOf(params);
    }

    public Set<Map.Entry<QueryOperator, Map<Column<?>, Object>>> entrySet() {
        return parameters.entrySet();
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

    @VisibleForTesting // Should not be used in the production code
    static QueryParameters getDefaultInstance() {
        return newBuilder().build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<QueryOperator, Map<Column<?>, Object>> parameters
                = new EnumMap<>(QueryOperator.class);

        private Builder() {
            // Prevent direct initialization
        }

        public Builder put(QueryOperator operator, Column<?> column, Object value) {
            checkNotNull(operator);
            checkNotNull(column);

            Map<Column<?>, Object> params = parameters.get(operator);
            if (params == null) {
                params = new HashMap<>();
                params.put(column, value);
                parameters.put(operator, params);
            } else {
                params.put(column, value);
            }
            return this;
        }

        public Builder putAll(QueryOperator operator, Map<Column<?>, Object> values) {
            checkNotNull(operator);
            checkNotNull(values);

            Map<Column<?>, Object> params = parameters.get(operator);
            if (params == null) {
                params = new HashMap<>();
                params.putAll(values);
                parameters.put(operator, params);
            } else {
                params.putAll(values);
            }
            return this;
        }
        public QueryParameters build() {
            return new QueryParameters(parameters);
        }
    }
}
