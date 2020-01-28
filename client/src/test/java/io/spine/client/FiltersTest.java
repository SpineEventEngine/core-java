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
import com.google.protobuf.DoubleValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.EntityColumn;
import io.spine.base.EntityStateField;
import io.spine.base.EventContextField;
import io.spine.base.EventMessageField;
import io.spine.base.Field;
import io.spine.base.FieldPath;
import io.spine.client.Filter.Operator;
import io.spine.core.EventContext;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.test.client.ClProjectCreated;
import io.spine.test.client.TestEntity;
import io.spine.test.client.TestEntityOwner;
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
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`Filters` utility should")
class FiltersTest {

    private static final String FIELD = "owner.when_last_visited";
    private static final Timestamp REQUESTED_VALUE = currentTime();

    private static final String ENUM_FIELD = "owner.role";
    private static final TestEntityOwner.Role ENUM_VALUE = ADMIN;

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(TargetFilters.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {

        new NullPointerTester()
                .setDefault(Filter.class, Filter.getDefaultInstance())
                .setDefault(EntityColumn.class, TestEntity.Columns.firstField())
                .setDefault(EntityStateField.class, TestEntity.Fields.id())
                .setDefault(EventMessageField.class, ClProjectCreated.Fields.id())
                .setDefault(EventContextField.class, EventContext.Fields.timestamp())
                .testAllPublicStaticMethods(Filters.class);
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
            Filter filter = gt("owner.when_last_visited", timestamp);

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
