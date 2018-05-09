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
import io.spine.server.entity.storage.given.ColumnTestEnv.TestEntity;
import org.junit.Test;

import java.lang.reflect.Method;

import static io.spine.server.entity.storage.PersistenceInfo.from;
import static io.spine.server.entity.storage.EnumType.ORDINAL;
import static io.spine.server.entity.storage.EnumType.STRING;
import static org.junit.Assert.assertEquals;

/**
 * @author Dmytro Kuzmin
 */
public class PersistenceInfoShould {

    @Test
    public void not_accept_nulls() {
        new NullPointerTester().testAllPublicStaticMethods(PersistenceInfo.class);
    }

    @Test
    public void create_identity_instance_for_non_enum_type_getter() {
        final PersistenceInfo info = forGetter("getLong");
        assertEquals(long.class, info.getPersistedType());
        assertEquals(IdentityConverter.class, info.getValueConverter()
                                                  .getClass());
    }

    @Test
    public void create_correct_instance_for_ordinal_enum_getter() {
        final PersistenceInfo info = forGetter("getEnumOrdinal");
        checkInfoIsOfEnumType(info, ORDINAL);
    }

    @Test
    public void create_correct_instance_for_string_enum_getter() {
        final PersistenceInfo info = forGetter("getEnumString");
        checkInfoIsOfEnumType(info, STRING);
    }

    @Test
    public void create_instance_of_ordinal_type_for_not_annotated_enum_getter() {
        final PersistenceInfo info = forGetter("getEnumNotAnnotated");
        checkInfoIsOfEnumType(info, ORDINAL);
    }

    private static PersistenceInfo forGetter(String name) {
        try {
            final Method result = TestEntity.class.getDeclaredMethod(name);
            return from(result);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkInfoIsOfEnumType(PersistenceInfo info, EnumType type) {
        final Class<?> expectedType = EnumPersistenceTypes.of(type);
        final Class<?> actualType = info.getPersistedType();
        assertEquals(expectedType, actualType);
        final EnumConverter expectedConverter = EnumConverters.forType(type);
        final ColumnValueConverter actualConverter = info.getValueConverter();
        assertEquals(expectedConverter.getClass(), actualConverter.getClass());
    }
}
