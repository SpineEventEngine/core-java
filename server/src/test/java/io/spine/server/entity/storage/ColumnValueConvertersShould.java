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
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;

import static io.spine.server.entity.storage.ColumnValueConverters.of;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;

/**
 * @author Dmytro Kuzmin
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Many literals for method names.
public class ColumnValueConvertersShould {

    @Test
    @DisplayName("have private utility ctor")
    void havePrivateUtilityCtor() {
        assertHasPrivateParameterlessCtor(ColumnValueConverters.class);
    }

    @Test
    @DisplayName("not accept nulls")
    void notAcceptNulls() {
        new NullPointerTester().testAllPublicStaticMethods(ColumnValueConverters.class);
    }

    @Test
    @DisplayName("create identity instance for non enum type getter")
    void createIdentityInstanceForNonEnumTypeGetter() {
        final ColumnValueConverter converter = ofGetter("getLong");
        assertEquals(IdentityConverter.class, converter.getClass());
    }

    @Test
    @DisplayName("create ordinal converter for ordinal enum getter")
    void createOrdinalConverterForOrdinalEnumGetter() {
        final ColumnValueConverter converter = ofGetter("getEnumOrdinal");
        assertEquals(OrdinalEnumConverter.class, converter.getClass());
    }

    @Test
    @DisplayName("create string converter for string enum getter")
    void createStringConverterForStringEnumGetter() {
        final ColumnValueConverter converter = ofGetter("getEnumString");
        assertEquals(StringEnumConverter.class, converter.getClass());
    }

    @Test
    @DisplayName("create ordinal converter for not annotated enum getter")
    void createOrdinalConverterForNotAnnotatedEnumGetter() {
        final ColumnValueConverter converter = ofGetter("getEnumNotAnnotated");
        assertEquals(OrdinalEnumConverter.class, converter.getClass());
    }

    private static ColumnValueConverter ofGetter(String name) {
        try {
            final Method getter = TestEntity.class.getDeclaredMethod(name);
            final ColumnValueConverter converter = of(getter);
            return converter;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
