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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Int32Value;
import io.spine.base.FieldPath;
import io.spine.base.entity.EntityColumn;
import io.spine.protobuf.AnyPacker;
import io.spine.test.client.TestEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.Filter.Operator.EQUAL;
import static io.spine.client.Filter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.Filter.Operator.GREATER_THAN;
import static io.spine.client.Filter.Operator.LESS_OR_EQUAL;
import static io.spine.client.Filter.Operator.LESS_THAN;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;

@DisplayName("`QueryFilter` should")
class QueryFilterTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(EntityColumn.class, TestEntity.Column.firstField())
                .testAllPublicStaticMethods(QueryFilter.class);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("an `equals` filter")
        void eqFilter() {
            checkCreates(QueryFilter::eq, EQUAL);
        }

        @Test
        @DisplayName("a `greater than` filter")
        void gtFilter() {
            checkCreates(QueryFilter::gt, GREATER_THAN);
        }

        @Test
        @DisplayName("a `less than` filter")
        void ltFilter() {
            checkCreates(QueryFilter::lt, LESS_THAN);
        }

        @Test
        @DisplayName("a `greater than or equals` filter")
        void geFilter() {
            checkCreates(QueryFilter::ge, GREATER_OR_EQUAL);
        }

        @Test
        @DisplayName("a `less than or equals` filter")
        void leFilter() {
            checkCreates(QueryFilter::le, LESS_OR_EQUAL);
        }

        private void checkCreates(BiFunction<EntityColumn, Object, QueryFilter> factoryMethod,
                                  Filter.Operator expectedOperator) {
            EntityColumn column = TestEntity.Column.thirdField();
            int value = 42;
            QueryFilter queryFilter = factoryMethod.apply(column, value);
            Filter filter = queryFilter.filter();

            FieldPath fieldPath = filter.getFieldPath();
            int nameCount = fieldPath.getFieldNameCount();
            assertThat(nameCount).isEqualTo(1);

            String fieldName = fieldPath.getFieldName(0);
            String columnName = column.name()
                                      .value();
            assertThat(fieldName).isEqualTo(columnName);

            assertThat(filter.getOperator()).isEqualTo(expectedOperator);

            Int32Value unpacked = AnyPacker.unpack(filter.getValue(), Int32Value.class);
            assertThat(unpacked.getValue()).isEqualTo(value);
        }
    }
}
