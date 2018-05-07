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

package io.spine.server.entity.storage.enumeration;

import com.google.common.testing.NullPointerTester;
import io.spine.server.entity.storage.enumeration.given.EnumeratedTestEnv.TestClass;
import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.Method;

import static io.spine.server.entity.storage.enumeration.EnumType.ORDINAL;
import static io.spine.server.entity.storage.enumeration.EnumType.STRING;
import static io.spine.server.entity.storage.enumeration.PersistenceTypes.getPersistenceType;
import static io.spine.server.entity.storage.enumeration.EnumeratedValue.from;
import static io.spine.server.entity.storage.enumeration.given.EnumeratedTestEnv.TestEnum.ONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("DuplicateStringLiteralInspection") // Many string literals for method names
public class EnumeratedValueShould {

    @Test
    public void not_accept_nulls() {
        new NullPointerTester().testAllPublicStaticMethods(EnumeratedValue.class);
        final EnumeratedValue value = forGetter("getEnumOrdinal");
        new NullPointerTester().testAllPublicInstanceMethods(value);
    }

    @Test
    public void be_created_for_getter_of_enum_type() {
        final EnumeratedValue value = forGetter("getEnumOrdinal");
        assertFalse(value.isEmpty());
    }

    @Test
    public void create_empty_instance_for_non_enum_type_getter() {
        final EnumeratedValue value = forGetter("getInt");
        assertTrue(value.isEmpty());
    }

    @Test
    public void create_instance_of_correct_enum_type() {
        final EnumeratedValue valueOrdinal = forGetter("getEnumOrdinal");
        assertEquals(ORDINAL, valueOrdinal.getEnumType());

        final EnumeratedValue valueString = forGetter("getEnumString");
        assertEquals(STRING, valueString.getEnumType());
    }

    @Test
    public void create_instance_of_ordinal_type_for_not_annotated_enum_getter() {
        final EnumeratedValue valueOrdinal = forGetter("getEnumNotAnnotated");
        assertEquals(ORDINAL, valueOrdinal.getEnumType());
    }

    @Test
    public void return_stored_type() {
        final EnumeratedValue valueOrdinal = forGetter("getEnumOrdinal");
        final Class<?> ordinalType = getPersistenceType(ORDINAL);
        assertEquals(ordinalType, valueOrdinal.getPersistenceType());

        final EnumeratedValue valueString = forGetter("getEnumString");
        final Class<?> stringType = getPersistenceType(STRING);
        assertEquals(stringType, valueString.getPersistenceType());
    }

    @Test
    public void retrieve_value_for_storing_from_enum_value() {
        final EnumeratedValue valueOrdinal = forGetter("getEnumOrdinal");
        final Serializable retrievedValueOrdinal = valueOrdinal.getFor(ONE);
        assertEquals(ONE.ordinal(), retrievedValueOrdinal);

        final EnumeratedValue valueString = forGetter("getEnumString");
        final Serializable retrievedValueString = valueString.getFor(ONE);
        assertEquals(ONE.name(), retrievedValueString);
    }

    private static EnumeratedValue forGetter(String name) {
        try {
            final Method result = TestClass.class.getDeclaredMethod(name);
            return from(result);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
