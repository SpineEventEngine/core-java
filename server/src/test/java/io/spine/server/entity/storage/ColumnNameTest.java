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
import com.google.protobuf.Descriptors.FieldDescriptor;
import io.spine.code.proto.FieldDeclaration;
import io.spine.server.storage.LifecycleFlagField;
import io.spine.test.entity.TaskView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("`ColumnName` should")
class ColumnNameTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(ColumnName.class);
    }

    @Test
    @DisplayName("be constructed from string value")
    void initFromString() {
        String columnName = "the-column-name";
        ColumnName name = ColumnName.of(columnName);

        assertThat(name.value()).isEqualTo(columnName);
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Called to throw exception.
    @Test
    @DisplayName("not be constructed from empty string")
    void notInitFromEmpty() {
        assertThrows(IllegalArgumentException.class, () -> ColumnName.of(""));
    }

    @Test
    @DisplayName("be constructed from `StorageField`")
    void initFromStorageField() {
        LifecycleFlagField storageField = LifecycleFlagField.archived;
        ColumnName name = ColumnName.of(storageField);

        assertThat(name.value()).isEqualTo(storageField.name());
    }

    @Test
    @DisplayName("be constructed from `FieldDeclaration`")
    void initFromFieldDeclaration() {
        FieldDescriptor field = TaskView.getDescriptor()
                                        .getFields()
                                        .get(0);
        FieldDeclaration fieldDeclaration = new FieldDeclaration(field);
        ColumnName columnName = ColumnName.of(fieldDeclaration);

        assertThat(columnName.value()).isEqualTo(field.getName());
    }

    @Test
    @DisplayName("be extracted from getter name")
    void initFromGetter() throws NoSuchMethodException {
        Method getter = TaskView.class.getMethod("getEstimateInDays");
        ColumnName columnName = ColumnName.from(getter);
        assertThat(columnName.value()).isEqualTo("estimate_in_days");
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Called to throw exception.
    @Test
    @DisplayName("throw `IAE` when the given method is not a getter")
    void notInitFromNonGetter() throws NoSuchMethodException {
        Method method = TaskView.class.getMethod("hasId");
        assertThrows(IllegalArgumentException.class, () -> ColumnName.from(method));
    }
}
