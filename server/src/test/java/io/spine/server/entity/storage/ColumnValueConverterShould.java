/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import io.spine.server.entity.storage.given.ColumnTestEnv.TaskStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.rules.ExpectedException;

import java.io.Serializable;

import static io.spine.server.entity.storage.given.ColumnTestEnv.TaskStatus.SUCCESS;
import static org.junit.Assert.assertEquals;

/**
 * @author Dmytro Kuzmin
 */
public class ColumnValueConverterShould {

    @Rule
    public ExpectedException thrown = ExpectedException.none(); 
    
    @Test
    @DisplayName("not accept nulls")
    void notAcceptNulls() {
        ColumnValueConverter converter = new IdentityConverter(TaskStatus.class);
        new NullPointerTester().testAllPublicInstanceMethods(converter);
        converter = new OrdinalEnumConverter(TaskStatus.class);
        new NullPointerTester().testAllPublicInstanceMethods(converter);
        converter = new StringEnumConverter(TaskStatus.class);
        new NullPointerTester().testAllPublicInstanceMethods(converter);
    }

    @Test
    @DisplayName("perform identity conversion")
    void performIdentityConversion() {
        String value = "stringValue";
        ColumnValueConverter converter = new IdentityConverter(value.getClass());
        Serializable convertedObject = converter.convert(value);
        assertEquals(value, convertedObject);
    }

    @Test
    @DisplayName("convert enum to ordinal value")
    void convertEnumToOrdinalValue() {
        TaskStatus value = SUCCESS;
        ColumnValueConverter converter = new OrdinalEnumConverter(value.getClass());
        Serializable convertedValue = converter.convert(value);
        assertEquals(value.ordinal(), convertedValue);
    }

    @Test
    @DisplayName("convert enum to string value")
    void convertEnumToStringValue() {
        TaskStatus value = SUCCESS;
        ColumnValueConverter converter = new StringEnumConverter(value.getClass());
        Serializable convertedValue = converter.convert(value);
        assertEquals(value.name(), convertedValue);
    }

    @Test
    @DisplayName("not support wrong value type conversion")
    void notSupportWrongValueTypeConversion() {
        String value = "unsupportedValue";
        ColumnValueConverter converter = new OrdinalEnumConverter(TaskStatus.class);
        thrown.expect(IllegalArgumentException.class);
        converter.convert(value);
    }
}
