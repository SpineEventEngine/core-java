/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.entity.storage;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.spine3.test.Verify.assertEmpty;
import static org.spine3.test.Verify.assertInstanceOf;

/**
 * @author Dmytro Dashenkov
 */
public class ColumnTypeRegistryShould {

    @Test
    public void have_builder() {
        final ColumnTypeRegistry.Builder builder = ColumnTypeRegistry.newBuilder();
        assertNotNull(builder);
    }

    @Test
    public void have_default_empty_singleton_instance() {
        final ColumnTypeRegistry emptyInstanceOne = ColumnTypeRegistry.empty();
        assertEmpty(emptyInstanceOne.getColumnTypeMap());

        final ColumnTypeRegistry emptyInstanceTwo = ColumnTypeRegistry.empty();
        assertEmpty(emptyInstanceTwo.getColumnTypeMap());

        assertSame(emptyInstanceOne, emptyInstanceTwo);
    }

    @SuppressWarnings("MethodWithMultipleLoops") // OK for a test
    @Test
    public void store_column_types() {
        final Collection<Class> classes = Arrays.<Class>asList(String.class,
                                                               Integer.class,
                                                               Date.class);
        final ColumnTypeRegistry.Builder<?> registryBuilder =
                ColumnTypeRegistry.<ColumnType>newBuilder();
        for (Class<?> cls : classes) {
            final ColumnType type = new AnyType();
            registryBuilder.put(cls, type);
        }

        final ColumnTypeRegistry<?> registry = registryBuilder.build();

        for (Class<?> cls : classes) {
            final ColumnType type = registry.get(mockProperty(cls));
            assertInstanceOf(AnyType.class, type);
        }
    }

    private static <T> Column<T> mockProperty(Class<T> cls) {
        @SuppressWarnings("unchecked")
        final Column<T> column = (Column<T>) mock(Column.class);
        when(column.getType()).thenReturn(cls);
        return column;
    }

    private static class AnyType extends SimpleColumnType {

        @Override
        public void setColumnValue(Object storageRecord, Object value, Object columnIdentifier) {
            // NOP
        }
    }
}
