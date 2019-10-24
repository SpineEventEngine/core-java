/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.entity.storage;

import com.google.common.testing.NullPointerTester;
import io.spine.code.proto.FieldDeclaration;
import io.spine.server.entity.Entity;
import io.spine.server.entity.WithLifecycle;
import io.spine.server.entity.storage.given.TaskViewProjection;
import io.spine.test.entity.TaskView;
import io.spine.test.entity.TaskViewId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`Column` should")
class ColumnTest {

    private final Column column = column();

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicInstanceMethods(column());
    }

    @Test
    @DisplayName("obtain column name")
    void obtainName() {
        ColumnName name = column.name();
        assertThat(name.value()).isEqualTo(archived.name());
    }

    @Test
    @DisplayName("obtain column type")
    void obtainType() {
        Class<?> type = column.type();

        assertThat(type).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("obtain the proto field corresponding to the column")
    void obtainProtoField() {
        String columnName = "estimate_in_days";
        ColumnName name = ColumnName.of(columnName);
        Column column = Columns.of(TaskViewProjection.class)
                               .get(name);
        FieldDeclaration field = column.protoField();

        assertThat(field).isNotNull();
        assertThat(field.name()
                        .value()).isEqualTo(columnName);
    }

    @Test
    @DisplayName("obtain `null` proto field in case the column is a system column")
    void obtainNullProtoField() {
        FieldDeclaration field = column.protoField();
        assertThat(field).isNull();
    }

    @Test
    @DisplayName("tell if a column is a proto column")
    void checkIsProtoColumn() {
        Column column = Columns.of(TaskViewProjection.class)
                               .get(ColumnName.of("name"));
        assertThat(column.isProtoColumn()).isTrue();
    }

    @Test
    @DisplayName("extract column value from the entity")
    void extractValueFromEntity() {
        TaskViewProjection projection = new TaskViewProjection();
        Object value = column.valueIn(projection);

        assertThat(value).isEqualTo(projection.isArchived());
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Called to throw exception.
    @Test
    @DisplayName("throw `ISE` if an exception happens during value extraction")
    void throwOnExtractionError() {
        ColumnName name = ColumnName.of("some-column-name");
        Class<Boolean> type = boolean.class;
        Column.Getter getter = entity -> {
            throw new RuntimeException("ignore this error");
        };
        Column column = new Column(name, type, getter);
        Entity<TaskViewId, TaskView> entity = new TaskViewProjection();

        assertThrows(IllegalStateException.class, () -> column.valueIn(entity));
    }

    @Test
    @DisplayName("extract the interface-based column value")
    void extractInterfaceBasedValue() {
        Column column = Columns.of(TaskViewProjection.class)
                               .get(ColumnName.of("estimate_in_days"));
        TaskViewProjection projection = new TaskViewProjection();
        Object value = column.valueFromInterface(projection);

        assertThat(value).isEqualTo(projection.getEstimateInDays());
        assertThat(value).isNotEqualTo(projection.state()
                                                 .getEstimateInDays());
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Called to throw exception.
    @Test
    @DisplayName("throw `ISE` when trying to extract an interface-based value from non-interface-based column")
    void throwOnWrongColumnType() {
        Entity<TaskViewId, TaskView> projection = new TaskViewProjection();
        assertThrows(IllegalStateException.class, () -> column.valueFromInterface(projection));
    }

    private static Column column() {
        ColumnName name = ColumnName.of(archived);
        Class<Boolean> type = boolean.class;
        Column.Getter getter = WithLifecycle::isArchived;
        return new Column(name, type, getter);
    }
}
