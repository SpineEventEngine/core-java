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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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
        final ColumnTypeRegistry emptyInstance = ColumnTypeRegistry.newBuilder()
                                                                   .build();
        assertEmpty(emptyInstance.getColumnTypeMap());
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

    @Test
    public void find_closest_superclass_column_type() {
        final ColumnTypeRegistry<?> registry =
                ColumnTypeRegistry.newBuilder()
                                  .put(GeneratedMessageV3.class, new GeneratedMessageType())
                                  .put(AbstractMessage.class, new AbstractMessageType())
                                  .build();
        final Column column = mockProperty(Any.class);
        final ColumnType type = registry.get(column);
        assertNotNull(type);
        assertThat(type, instanceOf(GeneratedMessageType.class));
    }

    @Test
    public void map_primitives_autoboxed() {
        final ColumnTypeRegistry<?> registry =
                ColumnTypeRegistry.newBuilder()
                                  .put(Integer.class, new IntegerType())
                                  .build();
        final ColumnType integerColumnType = registry.get(mockProperty(Integer.class));
        assertNotNull(integerColumnType);
        final ColumnType intColumnType = registry.get(mockProperty(int.class));
        assertNotNull(intColumnType);

        assertEquals(integerColumnType, intColumnType);
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

    private static class AbstractMessageType
            implements ColumnType<AbstractMessage, String, StringBuilder, String> {

        @Override
        public String convertColumnValue(AbstractMessage fieldValue) {
            return fieldValue.toString();
        }

        @Override
        public void setColumnValue(StringBuilder storageRecord, String value, String columnIdentifier) {
            storageRecord.append(value);
        }
    }

    private static class GeneratedMessageType
            implements ColumnType<GeneratedMessageV3, String, StringBuilder, String> {

        @Override
        public String convertColumnValue(GeneratedMessageV3 fieldValue) {
            return fieldValue.toString();
        }

        @Override
        public void setColumnValue(StringBuilder storageRecord, String value, String columnIdentifier) {
            storageRecord.append(value);
        }
    }

    private static class IntegerType
            implements ColumnType<Integer, String, StringBuilder, String> {

        @Override
        public String convertColumnValue(Integer fieldValue) {
            return String.valueOf(fieldValue);
        }

        @Override
        public void setColumnValue(StringBuilder storageRecord, String value,
                                   String columnIdentifier) {
            storageRecord.append(value);
        }
    }
}
