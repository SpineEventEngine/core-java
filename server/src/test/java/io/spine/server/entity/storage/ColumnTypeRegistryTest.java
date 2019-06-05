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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import io.spine.server.entity.storage.given.ColumnTypeRegistryTestEnv.AbstractMessageType;
import io.spine.server.entity.storage.given.ColumnTypeRegistryTestEnv.AnyType;
import io.spine.server.entity.storage.given.ColumnTypeRegistryTestEnv.GeneratedMessageType;
import io.spine.server.entity.storage.given.ColumnTypeRegistryTestEnv.IntegerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ColumnTypeRegistry should")
class ColumnTypeRegistryTest {

    private static <T> EntityColumn mockProperty(Class<T> cls) {
        EntityColumn column = mock(EntityColumn.class);
        when(column.type()).thenReturn(cls);
        when(column.persistedType()).thenReturn(cls);
        return column;
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("have builder")
    void haveBuilder() {
        ColumnTypeRegistry.Builder builder = ColumnTypeRegistry.newBuilder();
        assertNotNull(builder);
    }

    @Test
    @DisplayName("have default empty singleton instance")
    void haveEmptyInstance() {
        ColumnTypeRegistry emptyInstance = ColumnTypeRegistry.newBuilder()
                                                             .build();
        assertThat(emptyInstance.getColumnTypeMap()).isEmpty();
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "unchecked"}) // OK for a test.
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
            assertThat(type).isInstanceOf(AnyType.class);
        }
    }

    @Test
    @DisplayName("find closest superclass for column type")
    void findClosestSuperclass() {
        ColumnTypeRegistry<?> registry =
                ColumnTypeRegistry.newBuilder()
                                  .put(GeneratedMessageV3.class, new GeneratedMessageType())
                                  .put(AbstractMessage.class, new AbstractMessageType())
                                  .build();
        EntityColumn column = mockProperty(Any.class);
        ColumnType type = registry.get(column);
        assertNotNull(type);
        assertThat(type).isInstanceOf(GeneratedMessageType.class);
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
}
