/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import io.spine.test.Verify;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static io.spine.test.Verify.assertEmpty;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
public class ColumnTypeRegistryShould {

    private static <T> EntityColumn mockProperty(Class<T> cls) {
        EntityColumn column = mock(EntityColumn.class);
        when(column.getType()).thenReturn(cls);
        when(column.getPersistedType()).thenReturn(cls);
        return column;
    }


    @Test
    @DisplayName("have builder")
    void haveBuilder() {
        ColumnTypeRegistry.Builder builder = ColumnTypeRegistry.newBuilder();
        assertNotNull(builder);
    }

    @Test
    @DisplayName("have default empty singleton instance")
    void haveDefaultEmptySingletonInstance() {
        ColumnTypeRegistry emptyInstance = ColumnTypeRegistry.newBuilder()
                                                             .build();
        assertEmpty(emptyInstance.getColumnTypeMap());
    }

    @SuppressWarnings("MethodWithMultipleLoops") // OK for a test
    @Test
    @DisplayName("store column types")
    void storeColumnTypes() {
        Collection<Class> classes = Arrays.asList(String.class,
                                                  Integer.class,
                                                  Date.class);
        ColumnTypeRegistry.Builder<?> registryBuilder =
                ColumnTypeRegistry.newBuilder();
        for (Class<?> cls : classes) {
            ColumnType type = new AnyType();
            registryBuilder.put(cls, type);
        }

        ColumnTypeRegistry<?> registry = registryBuilder.build();

        for (Class<?> cls : classes) {
            ColumnType type = registry.get(mockProperty(cls));
            Verify.assertInstanceOf(AnyType.class, type);
        }
    }

    @Test
    @DisplayName("find closest superclass column type")
    void findClosestSuperclassColumnType() {
        ColumnTypeRegistry<?> registry =
                ColumnTypeRegistry.newBuilder()
                                  .put(GeneratedMessageV3.class, new GeneratedMessageType())
                                  .put(AbstractMessage.class, new AbstractMessageType())
                                  .build();
        EntityColumn column = mockProperty(Any.class);
        ColumnType type = registry.get(column);
        assertNotNull(type);
        assertThat(type, instanceOf(GeneratedMessageType.class));
    }

    @Test
    @DisplayName("map primitives autoboxed")
    void mapPrimitivesAutoboxed() {
        ColumnTypeRegistry<?> registry =
                ColumnTypeRegistry.newBuilder()
                                  .put(Integer.class, new IntegerType())
                                  .build();
        ColumnType integerColumnType = registry.get(mockProperty(Integer.class));
        assertNotNull(integerColumnType);
        ColumnType intColumnType = registry.get(mockProperty(int.class));
        assertNotNull(intColumnType);

        assertEquals(integerColumnType, intColumnType);
    }

    private static class AnyType extends SimpleColumnType {

        @Override
        public void setColumnValue(Object storageRecord, Object value, Object columnIdentifier) {
            // NOP
        }

        @Override
        public void setNull(Object storageRecord, Object columnIdentifier) {
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
        public void setColumnValue(StringBuilder storageRecord,
                                   String value,
                                   String columnIdentifier) {
            storageRecord.append(value);
        }

        @Override
        public void setNull(StringBuilder storageRecord, String columnIdentifier) {
            storageRecord.append(' ');
        }
    }

    private static class GeneratedMessageType
            implements ColumnType<GeneratedMessageV3, String, StringBuilder, String> {

        @Override
        public String convertColumnValue(GeneratedMessageV3 fieldValue) {
            return fieldValue.toString();
        }

        @Override
        public void setColumnValue(StringBuilder storageRecord,
                                   String value,
                                   String columnIdentifier) {
            storageRecord.append(value);
        }

        @Override
        public void setNull(StringBuilder storageRecord, String columnIdentifier) {
            storageRecord.append(' ');
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

        @Override
        public void setNull(StringBuilder storageRecord, String columnIdentifier) {
            storageRecord.append(' ');
        }
    }
}
