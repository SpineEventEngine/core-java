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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.client.ColumnFilter.Operator;
import io.spine.protobuf.AnyPacker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

import static io.spine.base.Time.getCurrentTime;
import static io.spine.client.ColumnFilter.Operator.EQUAL;
import static io.spine.client.ColumnFilter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.ColumnFilter.Operator.GREATER_THAN;
import static io.spine.client.ColumnFilter.Operator.LESS_OR_EQUAL;
import static io.spine.client.ColumnFilter.Operator.LESS_THAN;
import static io.spine.client.ColumnFilters.all;
import static io.spine.client.ColumnFilters.either;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.ColumnFilters.ge;
import static io.spine.client.ColumnFilters.gt;
import static io.spine.client.ColumnFilters.le;
import static io.spine.client.ColumnFilters.lt;
import static io.spine.client.CompositeColumnFilter.CompositeOperator;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.EITHER;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Verify.assertContainsAll;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("ColumnFilters utility should")
class ColumnFiltersTest {

    private static final String COLUMN_NAME = "preciseColumn";
    private static final Timestamp COLUMN_VALUE = getCurrentTime();

    @SuppressWarnings("DuplicateStringLiteralInspection") // Display name for utility c-tor test.
    @Test
    @DisplayName("have private parameterless constructor")
    void haveUtilityCtor() {
        assertHasPrivateParameterlessCtor(ColumnFilters.class);
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // Display name for null test.
    @Test
    @DisplayName("not accept nulls for non-Nullable public method arguments")
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(Timestamp.class, Timestamp.getDefaultInstance())
                .setDefault(ColumnFilter.class, ColumnFilter.getDefaultInstance())
                .testAllPublicStaticMethods(ColumnFilters.class);
    }

    @SuppressWarnings("InnerClassMayBeStatic") // JUnit 5 nested test classes cannot to be static.
    @Nested
    @DisplayName("when creating column filter")
    class FilterCreationTest {

        @Test
        @DisplayName("successfully create `equals` filter")
        void createEquals() {
            checkCreatesInstance(eq(COLUMN_NAME, COLUMN_VALUE), EQUAL);
        }

        @Test
        @DisplayName("successfully create `greater than` filter")
        void createGreaterThan() {
            checkCreatesInstance(gt(COLUMN_NAME, COLUMN_VALUE), GREATER_THAN);
        }

        @Test
        @DisplayName("successfully create `greater than or equals` filter")
        void createGreaterOrEqual() {
            checkCreatesInstance(ge(COLUMN_NAME, COLUMN_VALUE), GREATER_OR_EQUAL);
        }

        @Test
        @DisplayName("successfully create `less than` filter")
        void createLessThan() {
            checkCreatesInstance(lt(COLUMN_NAME, COLUMN_VALUE), LESS_THAN);
        }

        @Test
        @DisplayName("successfully create `less than or equals` filter")
        void createLessOrEqual() {
            checkCreatesInstance(le(COLUMN_NAME, COLUMN_VALUE), LESS_OR_EQUAL);
        }

        private void checkCreatesInstance(ColumnFilter filter,
                                          Operator operator) {
            assertEquals(COLUMN_NAME, filter.getColumnName());
            assertEquals(pack(COLUMN_VALUE), filter.getValue());
            assertEquals(operator, filter.getOperator());
        }
    }

    @SuppressWarnings("InnerClassMayBeStatic") // JUnit 5 nested test classes cannot to be static.
    @Nested
    @DisplayName("when creating composite column filter")
    class CompositeFilterCreationTest {

        @Test
        @DisplayName("successfully create `all` grouping")
        void createAllGrouping() {
            final ColumnFilter[] filters = {
                    le(COLUMN_NAME, COLUMN_VALUE),
                    ge(COLUMN_NAME, COLUMN_VALUE)
            };
            checkCreatesInstance(all(filters[0], filters[1]), ALL, filters);
        }

        @Test
        @DisplayName("successfully create `either` grouping")
        void createEitherGrouping() {
            final ColumnFilter[] filters = {
                    lt(COLUMN_NAME, COLUMN_VALUE),
                    gt(COLUMN_NAME, COLUMN_VALUE)
            };
            checkCreatesInstance(either(filters[0], filters[1]), EITHER, filters);
        }

        private void checkCreatesInstance(CompositeColumnFilter filter,
                                          CompositeOperator operator,
                                          ColumnFilter[] groupedFilters) {
            assertEquals(operator, filter.getOperator());
            assertContainsAll(filter.getFilterList(), groupedFilters);
        }
    }

    @SuppressWarnings("InnerClassMayBeStatic") // JUnit 5 nested test classes cannot to be static.
    @Nested
    @DisplayName("when creating ordering filter")
    class OrderingFilterCreationTest {

        @Test
        @DisplayName("successfully create ordering filter for numbers")
        void createOrderFilterForNumber() {
            final double number = 3.14;
            final ColumnFilter filter = le("doubleColumn", number);
            assertNotNull(filter);
            assertEquals(LESS_OR_EQUAL, filter.getOperator());
            final DoubleValue value = AnyPacker.unpack(filter.getValue());
            assertEquals(number, value.getValue(), 0.0);
        }

        @Test
        @DisplayName("successfully create ordering filter for strings")
        void createOrderFilterForString() {
            final String string = "abc";
            final ColumnFilter filter = gt("stringColumn", string);
            assertNotNull(filter);
            assertEquals(GREATER_THAN, filter.getOperator());
            final StringValue value = AnyPacker.unpack(filter.getValue());
            assertEquals(string, value.getValue());
        }

        @Test
        @DisplayName("fail to create ordering filter for enumerated types")
        void notCreateOrderFilterForEnum() {
            assertThrows(IllegalArgumentException.class, () -> ge("enumColumn", EQUAL));
        }

        @Test
        @DisplayName("fail to create ordering filter for non primitive number types")
        void notCreateOrderFilterForNonPrimitiveNumber() {
            final AtomicInteger number = new AtomicInteger(42);
            assertThrows(IllegalArgumentException.class, () -> ge("atomicColumn", number));
        }

        @Test
        @DisplayName("fail to create ordering filter for not supported types")
        void notCreateOrderFilterForNotSupportedType() {
            final Comparable<?> value = Calendar.getInstance(); // Comparable but not supported
            assertThrows(IllegalArgumentException.class, () -> le("invalidColumn", value));
        }
    }
}
