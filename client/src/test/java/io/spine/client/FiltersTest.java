/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.protobuf.Any;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.EventMessageField;
import io.spine.base.Field;
import io.spine.base.FieldPath;
import io.spine.client.Filter.Operator;
import io.spine.core.EventContext;
import io.spine.core.EventContextField;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.query.ColumnName;
import io.spine.query.EntityColumn;
import io.spine.query.EntityStateField;
import io.spine.test.client.ClProjectCreated;
import io.spine.test.client.TestEntity;
import io.spine.test.client.TestEntityOwner;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Time.currentTime;
import static io.spine.client.CompositeFilter.CompositeOperator;
import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeFilter.CompositeOperator.EITHER;
import static io.spine.client.Filter.Operator.EQUAL;
import static io.spine.client.Filter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.Filter.Operator.GREATER_THAN;
import static io.spine.client.Filter.Operator.LESS_OR_EQUAL;
import static io.spine.client.Filter.Operator.LESS_THAN;
import static io.spine.client.Filters.eq;
import static io.spine.client.Filters.ge;
import static io.spine.client.Filters.gt;
import static io.spine.client.Filters.le;
import static io.spine.client.Filters.lt;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.TypeConverter.toAny;
import static io.spine.test.client.TestEntityOwner.Role.ADMIN;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`Filters` utility should")
class FiltersTest extends UtilityClassTest<Filters> {

    private static final String FIELD = "owner.when_last_visited";
    private static final Timestamp REQUESTED_VALUE = currentTime();

    private static final String ENUM_FIELD = "owner.role";
    private static final TestEntityOwner.Role ENUM_VALUE = ADMIN;

    FiltersTest() {
        super(Filters.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Override
    protected void configure(NullPointerTester tester) {
        super.configure(tester);
        tester.setDefault(Filter.class, Filter.getDefaultInstance())
              .setDefault(EntityColumn.class, TestEntity.Column.firstField())
              .setDefault(EntityStateField.class, TestEntity.Field.owner())
              .setDefault(EventMessageField.class, ClProjectCreated.Field.name())
              .setDefault(EventContextField.class, EventContext.Field.pastMessage())
              .setDefault(ColumnName.class, ColumnName.of("filters_test"))
              .setDefault(Field.class, Field.named("filters_test"));
    }

    @Nested
    @DisplayName("create filter of type")
    class CreateFilterOfType {

        @Test
        @DisplayName("`equals`")
        void equals() {
            checkCreatesInstance(eq(FIELD, REQUESTED_VALUE), EQUAL);
        }

        @Test
        @DisplayName("`greater than`")
        void greaterThan() {
            checkCreatesInstance(gt(FIELD, REQUESTED_VALUE), GREATER_THAN);
        }

        @Test
        @DisplayName("`greater than or equals`")
        void greaterOrEqual() {
            checkCreatesInstance(ge(FIELD, REQUESTED_VALUE), GREATER_OR_EQUAL);
        }

        @Test
        @DisplayName("`less than`")
        void lessThan() {
            checkCreatesInstance(lt(FIELD, REQUESTED_VALUE), LESS_THAN);
        }

        @Test
        @DisplayName("`less than or equals`")
        void lessOrEqual() {
            checkCreatesInstance(le(FIELD, REQUESTED_VALUE), LESS_OR_EQUAL);
        }

        @Test
        @DisplayName("`equals` for enumerated types")
        void equalsForEnum() {
            Filter filter = eq(ENUM_FIELD, ENUM_VALUE);

            FieldPath fieldPath = filter.getFieldPath();
            String actual = Field.withPath(fieldPath)
                                 .toString();
            assertEquals(ENUM_FIELD, actual);
            assertEquals(toAny(ENUM_VALUE), filter.getValue());
            assertEquals(EQUAL, filter.getOperator());
        }

        private void checkCreatesInstance(Filter filter, Operator operator) {
            String actual = Field.withPath(filter.getFieldPath())
                                 .toString();
            assertEquals(FIELD, actual);
            assertEquals(pack(REQUESTED_VALUE), filter.getValue());
            assertEquals(operator, filter.getOperator());
        }
    }

    @Nested
    @DisplayName("create filter based on a typed")
    class CreateFilterByTyped {

        @Test
        @DisplayName("column")
        void column() {
            EntityColumn<TestEntity, String> column = TestEntity.Column.firstField();
            String value = "expected-filter-value";
            String expectedPath = column.name()
                                        .value();
            checkCreates(eq(column, value), expectedPath, value, EQUAL);
        }

        @Test
        @DisplayName("entity state field")
        void entityStateField() {
            EntityStateField field = TestEntity.Field.thirdField();
            int value = 142;
            String expectedPath = field.getField()
                                       .toString();
            checkCreates(gt(field, value), expectedPath, value, GREATER_THAN);
        }

        @Test
        @DisplayName("event message field")
        void eventMessageField() {
            EventMessageField field = ClProjectCreated.Field.name().value();
            String value = "expected-project-name";
            String expectedPath = field.getField()
                                       .toString();
            checkCreates(eq(field, value), expectedPath, value, EQUAL);
        }

        @Test
        @DisplayName("event context field")
        void eventContextField() {
            EventContextField field = EventContext.Field.external();
            boolean value = true;
            String expectedPath = format("context.%s", field.getField());
            checkCreates(eq(field, value), expectedPath, value, EQUAL);
        }

        private void checkCreates(Filter filter,
                                  String expectedPath,
                                  Object expectedValue,
                                  Operator expectedOperator) {
            String fieldPath = Field.withPath(filter.getFieldPath())
                                    .toString();
            assertThat(fieldPath).isEqualTo(expectedPath);

            Any value = filter.getValue();
            Any packedExpectedValue = toAny(expectedValue);
            assertThat(value).isEqualTo(packedExpectedValue);

            Operator operator = filter.getOperator();
            assertThat(operator).isEqualTo(expectedOperator);
        }
    }

    @Nested
    @DisplayName("create composite filter of type")
    class CreateCompositeFilterOfType {

        @Test
        @DisplayName("`all`")
        void all() {
            Filter[] filters = {
                    le(FIELD, REQUESTED_VALUE),
                    ge(FIELD, REQUESTED_VALUE)
            };
            checkCreatesInstance(Filters.all(filters[0], filters[1]), ALL, filters);
        }

        @Test
        @DisplayName("`either`")
        void either() {
            Filter[] filters = {
                    lt(FIELD, REQUESTED_VALUE),
                    gt(FIELD, REQUESTED_VALUE)
            };
            checkCreatesInstance(Filters.either(filters[0], filters[1]), EITHER, filters);
        }

        private void checkCreatesInstance(CompositeFilter filter,
                                          CompositeOperator operator,
                                          Filter[] groupedFilters) {
            assertEquals(operator, filter.getOperator());
            assertThat(filter.getFilterList())
                    .containsExactlyElementsIn(groupedFilters);
        }
    }

    @Nested
    @DisplayName("create ordering filter")
    class CreateOrderingFilter {

        @Test
        @DisplayName("for numbers")
        void forNumbers() {
            double number = 3.14;
            Filter filter = le("third_field", number);
            assertThat(filter.getOperator()).isEqualTo(LESS_OR_EQUAL);

            DoubleValue value = unpack(filter.getValue(), DoubleValue.class);
            assertThat(value.getValue()).isWithin(0.01).of(number);
        }

        @Test
        @DisplayName("for strings")
        void forStrings() {
            String theString = "abc";
            Filter filter = gt("first_field", theString);
            assertThat(filter.getOperator()).isEqualTo(GREATER_THAN);

            StringValue value = unpack(filter.getValue(), StringValue.class);
            assertThat(value.getValue()).isEqualTo(theString);
        }

        @Test
        @DisplayName("for timestamps")
        void forTimestamps() {
            Timestamp timestamp = currentTime();
            Filter filter = gt(FIELD, timestamp);

            assertThat(filter.getOperator()).isEqualTo(GREATER_THAN);
            Timestamp value = unpack(filter.getValue(), Timestamp.class);
            assertThat(value).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("for versions")
        void forVersions() {
            Version version = Versions.zero();
            Filter filter = ge("some_version_field", version);

            assertThat(filter).isNotNull();
            assertThat(filter.getOperator()).isEqualTo(GREATER_OR_EQUAL);
            Version value = unpack(filter.getValue(), Version.class);
            assertThat(value).isEqualTo(version);
        }
    }

    @Nested
    @DisplayName("fail to create ordering filter")
    class FailToCreateOrderingFilter {

        @Test
        @DisplayName("for enumerated types")
        void forEnums() {
            assertThrows(IllegalArgumentException.class,
                         () -> ge(ENUM_FIELD, ENUM_VALUE));
        }

        @Test
        @DisplayName("for non-primitive number types")
        void forNonPrimitiveNumbers() {
            AtomicInteger number = new AtomicInteger(42);
            assertThrows(IllegalArgumentException.class, () -> ge("atomicField", number));
        }

        @Test
        @DisplayName("for not supported types")
        void forUnsupportedTypes() {
            Comparable<?> value = Calendar.getInstance(); // Comparable but not supported
            assertThrows(IllegalArgumentException.class, () -> le("invalidField", value));
        }
    }
}
