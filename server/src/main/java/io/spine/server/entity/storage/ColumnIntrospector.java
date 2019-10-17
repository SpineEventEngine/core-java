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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import io.spine.base.EntityWithColumns;
import io.spine.code.proto.ColumnOption;
import io.spine.code.proto.FieldDeclaration;
import io.spine.server.entity.model.EntityClass;
import io.spine.type.MessageType;

import java.lang.reflect.Method;

import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;

final class ColumnIntrospector {

    private static final String GET_PREFIX = "get";

    private final EntityClass<?> entityClass;

    ColumnIntrospector(EntityClass<?> aClass) {
        entityClass = aClass;
    }

    ImmutableMap<ColumnName, Column> systemColumns() {
        ImmutableMap.Builder<ColumnName, Column> columns = ImmutableMap.builder();
        Class<?> entityClazz = entityClass.value();
        Method[] methods = entityClazz.getMethods();
        for (Method method : methods) {
            boolean isSystemColumn = method.isAnnotationPresent(SystemColumn.class);
            if (isSystemColumn) {
                Column column = Column.create(method, false);
                columns.put(column.name(), column);
            }
        }
        ImmutableMap<ColumnName, Column> result = columns.build();
        return result;
    }

    ImmutableMap<ColumnName, Column> protoColumns() {
        ImmutableMap.Builder<ColumnName, Column> columns = ImmutableMap.builder();
        MessageType stateType = entityClass.stateType();
        ImmutableList<FieldDeclaration> columnFields = ColumnOption.columnsOf(stateType);
        columnFields.forEach(field -> addToMap(field, columns));
        ImmutableMap<ColumnName, Column> result = columns.build();
        return result;
    }

    private void
    addToMap(FieldDeclaration field, ImmutableMap.Builder<ColumnName, Column> columns) {
        Class<?> entityClazz = entityClass.value();
        Class<? extends Message> stateClass = entityClass.stateClass();

        @SuppressWarnings("LocalVariableNamingConvention")
        boolean implementsEntityWithColumns =
                EntityWithColumns.class.isAssignableFrom(entityClazz);
        String getterName = getterName(field);
        try {
            Method method = implementsEntityWithColumns
                            ? entityClazz.getMethod(getterName)
                            : stateClass.getMethod(getterName);
            Column column = Column.create(method, !implementsEntityWithColumns);
            columns.put(column.name(), column);
        } catch (NoSuchMethodException e) {
            throw newIllegalStateException(
                    e,
                    "Expected to find a getter with name %s in entity class %s according to the " +
                            "declaration of column %s.",
                    getterName, entityClass.typeName(), field.name());
        }
    }

    private static String getterName(FieldDeclaration field) {
        String fieldNameCamelCase = field.name()
                                         .toCamelCase();
        return format("%s%s", GET_PREFIX, fieldNameCamelCase);
    }
}
