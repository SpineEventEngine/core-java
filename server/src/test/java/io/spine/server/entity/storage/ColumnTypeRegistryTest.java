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
import com.google.protobuf.GeneratedMessageV3;
import io.spine.server.entity.storage.given.ColumnTypeRegistryTestEnv.AbstractMessageType;
import io.spine.server.entity.storage.given.ColumnTypeRegistryTestEnv.AnyType;
import io.spine.server.entity.storage.given.ColumnTypeRegistryTestEnv.DoubleType;
import io.spine.server.entity.storage.given.ColumnTypeRegistryTestEnv.GeneratedMessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.entity.storage.given.SimpleColumn.doubleColumn;
import static io.spine.server.entity.storage.given.SimpleColumn.doublePrimitiveColumn;
import static io.spine.server.entity.storage.given.SimpleColumn.stringColumn;
import static io.spine.server.entity.storage.given.SimpleColumn.timestampColumn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("ColumnTypeRegistry should")
class ColumnTypeRegistryTest {

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
        EntityColumn[] columns = {stringColumn(), doubleColumn(), timestampColumn()};

        ColumnTypeRegistry.Builder<?> registryBuilder =
                ColumnTypeRegistry.newBuilder();
        for (EntityColumn column : columns) {
            ColumnType type = new AnyType();
            registryBuilder.put(column.type(), type);
        }

        ColumnTypeRegistry<?> registry = registryBuilder.build();

        for (EntityColumn column : columns) {
            ColumnType type = registry.get(column);
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
        EntityColumn timestampColumn = timestampColumn();
        ColumnType type = registry.get(timestampColumn);
        assertNotNull(type);
        assertThat(type).isInstanceOf(GeneratedMessageType.class);
    }

    @Test
    @DisplayName("map primitives autoboxed")
    void mapPrimitivesAutoboxed() {
        ColumnTypeRegistry<?> registry =
                ColumnTypeRegistry.newBuilder()
                                  .put(Double.class, new DoubleType())
                                  .build();
        ColumnType doubleType = registry.get(doubleColumn());
        assertNotNull(doubleType);
        ColumnType doublePrimitiveType = registry.get(doublePrimitiveColumn());
        assertNotNull(doublePrimitiveType);

        assertEquals(doubleType, doublePrimitiveType);
    }
}
