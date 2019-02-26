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
import io.spine.server.entity.storage.given.column.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static io.spine.server.entity.storage.given.column.TaskStatus.SUCCESS;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ColumnValueConverter should")
class ColumnValueConverterTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        ColumnValueConverter converter = new IdentityConverter(TaskStatus.class);
        new NullPointerTester().testAllPublicInstanceMethods(converter);
        converter = new OrdinalEnumConverter(TaskStatus.class);
        new NullPointerTester().testAllPublicInstanceMethods(converter);
        converter = new StringEnumConverter(TaskStatus.class);
        new NullPointerTester().testAllPublicInstanceMethods(converter);
    }

    @Test
    @DisplayName("perform identity conversion")
    void convertIdentically() {
        String value = "stringValue";
        ColumnValueConverter converter = new IdentityConverter(value.getClass());
        Serializable convertedObject = converter.convert(value);
        assertEquals(value, convertedObject);
    }

    @Test
    @DisplayName("convert enum to ordinal value")
    void convertEnumToOrdinal() {
        TaskStatus value = SUCCESS;
        ColumnValueConverter converter = new OrdinalEnumConverter(value.getDeclaringClass());
        Serializable convertedValue = converter.convert(value);
        assertEquals(value.ordinal(), convertedValue);
    }

    @Test
    @DisplayName("convert enum to string value")
    void convertEnumToString() {
        TaskStatus value = SUCCESS;
        ColumnValueConverter converter = new StringEnumConverter(value.getDeclaringClass());
        Serializable convertedValue = converter.convert(value);
        assertEquals(value.name(), convertedValue);
    }

    @Test
    @DisplayName("not convert value of wrong type")
    void notConvertWrongType() {
        String value = "unsupportedValue";
        ColumnValueConverter converter = new OrdinalEnumConverter(TaskStatus.class);
        assertThrows(IllegalArgumentException.class, () -> converter.convert(value));
    }
}
