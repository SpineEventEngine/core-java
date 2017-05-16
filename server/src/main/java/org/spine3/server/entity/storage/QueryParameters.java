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
import org.spine3.annotation.SPI;
import org.spine3.client.QueryOperator;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.copyOf;
import static org.spine3.client.QueryOperator.EQUAL;

/**
 * The parameters of an {@link EntityQuery}.
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

    /**
     * Retrieves the Query parameters which are compared with the given operator.
     *
     * @param operator the {@linkplain QueryOperator operator} of the parameter comparison
     * @return a {@link Map} of the {@linkplain Column Entity Column meta information} to their
     * values
     */
    public ImmutableMap<Column<?>, Object> getParams(QueryOperator operator) {
        Map<Column<?>, Object> params = parameters.get(operator);
        params = params == null
                ? Collections.<Column<?>, Object>emptyMap()
                : params;
        return copyOf(params);
    }

    /**
     * Iterates over the Query parameters.
     *
     * <p>The iteration is performed synchronously to guarantee no unexpected behaviour when
     * performing closure-based information within the {@code consumer}.
     *
     * <p>The {@link ParameterConsumer#consume(QueryOperator, Column, Object)} is called exactly
     * once per each Query parameter available in current instance.
     *
     * @param consumer the {@link ParameterConsumer} processing each parameter separately
     * @see ParameterConsumer
     */
    public void forEach(ParameterConsumer consumer) {
        for (Map.Entry<QueryOperator, Map<Column<?>, Object>> param : parameters.entrySet()) {
            final QueryOperator operator = param.getKey();
            applyConsumer(consumer, operator, param.getValue());
        }
    }

    private static void applyConsumer(ParameterConsumer consumer,
                                      QueryOperator operator,
                                      Map<Column<?>, Object> paramValues) {
        for (Map.Entry<Column<?>, Object> parameterValue : paramValues.entrySet()) {
            consumer.consume(operator, parameterValue.getKey(), parameterValue.getValue());
        }
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

        private final Map<QueryOperator, Map<Column<?>, Object>> parameters
                = new EnumMap<>(QueryOperator.class);

        private Builder() {
            // Prevent direct initialization
        }

        /**
         * Put the Query parameter represented by the arguments into the resulting instance of
         * {@code QueryParameters}.
         *
         * @param operator the parameter comparison operator
         * @param column   the parameter target {@link Column}
         * @param value    the parameter value
         * @return self for method chaining
         */
        public Builder put(QueryOperator operator, Column<?> column, @Nullable Object value) {
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

        public QueryParameters build() {
            return new QueryParameters(parameters);
        }
    }

    /**
     * A functional interface consuming the information about a single Query parameter.
     *
     * <p>Since there is no special data type representing a single Query parameter, there is no
     * convenient way to iterate over all of the parameters outside of {@code QueryParameters}. To
     * provide the client of class possibility to iterate over the parameters not writing two nested
     * loops, we declare a special {@link QueryParameters#forEach(ParameterConsumer)} method which
     * accepts an instance of {@code ParameterConsumer}.
     */
    @SPI
    public interface ParameterConsumer {

        /**
         * Performs actions upon a parameter represented by the method arguments.
         *
         * <p>All the usages of this method are specified to be either synchronised. Also,
         * no instances of {@code ParameterConsumer} are persisted in any context, which in
         * conjunction with synchronization means that any closure based information exchange is
         * totally safe.
         *
         * @param operator the operator of the parameter
         * @param column   the parameter target {@link Column}
         * @param value    the right operand in the comparison represented by the given parameter,
         *                 i.e. the parameter value
         */
        void consume(QueryOperator operator, Column<?> column, @Nullable Object value);
    }
}
