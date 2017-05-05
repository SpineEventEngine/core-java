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

import com.google.common.base.Objects;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.protobuf.ProtoJavaMapper;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.AnyPacker.unpack;

/**
 * A parameter of a {@link Query}.
 *
 * <p>This class may be considered a filter for the query. An instance contains the name of
 * the Entity Column to filter by and the value of the Column.
 *
 * <p>The supported types for querying are {@link Message} and Protobuf primitives.
 */
public final class QueryParameter {

    private final String columnName;
    private final Any value;

    private QueryParameter(String columnName, Any value) {
        this.columnName = columnName;
        this.value = value;
    }

    /**
     * Creates new equality {@code QueryParameter}.
     *
     * @param columnName the name of the Entity Column to query by, expressed in a single field
     *                   name with no type info
     * @param value      the requested value of the Entity Column
     * @return new instance of the QueryParameter
     */
    public static QueryParameter eq(String columnName, Object value) {
        checkNotNull(columnName);
        checkNotNull(value);
        final Any wrappedValue = ProtoJavaMapper.map(value);
        final QueryParameter parameter = new QueryParameter(columnName, wrappedValue);
        return parameter;
    }

    /**
     * @return the name of the Entity Column to query by
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * @return the value of the Entity Column to look for
     */
    public Any getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryParameter parameter = (QueryParameter) o;
        return Objects.equal(getColumnName(), parameter.getColumnName()) &&
                Objects.equal(getValue(), parameter.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getColumnName(), getValue());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('(')
          .append(columnName)
          .append(" = ")
          .append(unpack(value))
          .append(')');
        return sb.toString();
    }
}
