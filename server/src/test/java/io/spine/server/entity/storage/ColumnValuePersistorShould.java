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
import io.spine.server.entity.storage.given.ColumnTestEnv;
import io.spine.server.entity.storage.given.ColumnTestEnv.TestEntity;
import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.Method;

import static io.spine.server.entity.storage.ColumnValuePersistor.from;
import static io.spine.server.entity.storage.EnumType.ORDINAL;
import static io.spine.server.entity.storage.EnumType.STRING;
import static io.spine.server.entity.storage.given.ColumnTestEnv.TaskStatus.SUCCESS;
import static org.junit.Assert.assertEquals;

/**
 * @author Dmytro Kuzmin
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Many literals for method names.
public class ColumnValuePersistorShould {

    @Test
    public void not_accept_nulls() {
        new NullPointerTester().testAllPublicStaticMethods(ColumnValuePersistor.class);
        final ColumnValuePersistor persistor = forGetter("getLong");
        new NullPointerTester().testAllPublicInstanceMethods(persistor);
    }

    @Test
    public void create_identity_instance_for_non_enum_type_getter() {
        final ColumnValuePersistor persistor = forGetter("getLong");
        assertEquals(long.class, persistor.getPersistedType());
    }

    @Test
    public void create_correct_instance_for_ordinal_enum_getter() {
        final ColumnValuePersistor persistor = forGetter("getEnumOrdinal");
        checkInfoIsOfEnumType(persistor, ORDINAL);
    }

    @Test
    public void create_correct_instance_for_string_enum_getter() {
        final ColumnValuePersistor persistor = forGetter("getEnumString");
        checkInfoIsOfEnumType(persistor, STRING);
    }

    @Test
    public void create_instance_of_ordinal_type_for_not_annotated_enum_getter() {
        final ColumnValuePersistor persistor = forGetter("getEnumNotAnnotated");
        checkInfoIsOfEnumType(persistor, ORDINAL);
    }

    @Test
    public void do_identity_conversion_for_non_enum_types() {
        final ColumnValuePersistor persistor = forGetter("getLong");
        final long value = 42L;
        final Serializable persistedValue = persistor.toPersistedValue(value);
        assertEquals(value, persistedValue);
    }

    @Test
    public void do_conversion_for_ordinal_enum_type() {
        final ColumnValuePersistor persistor = forGetter("getEnumOrdinal");
        final ColumnTestEnv.TaskStatus value = SUCCESS;
        final Serializable persistedValue = persistor.toPersistedValue(value);
        assertEquals(value.ordinal(), persistedValue);
    }

    @Test
    public void do_conversion_for_string_enum_type() {
        final ColumnValuePersistor persistor = forGetter("getEnumString");
        final ColumnTestEnv.TaskStatus value = SUCCESS;
        final Serializable persistedValue = persistor.toPersistedValue(value);
        assertEquals(value.name(), persistedValue);
    }

    private static ColumnValuePersistor forGetter(String name) {
        try {
            final Method result = TestEntity.class.getDeclaredMethod(name);
            final ColumnValuePersistor persistor = from(result);
            return persistor;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkInfoIsOfEnumType(ColumnValuePersistor info, EnumType type) {
        final Class<?> expectedType = EnumPersistenceTypes.of(type);
        final Class<?> actualType = info.getPersistedType();
        assertEquals(expectedType, actualType);
    }
}
