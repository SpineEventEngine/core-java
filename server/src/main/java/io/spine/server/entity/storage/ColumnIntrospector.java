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
import io.spine.code.proto.ColumnOption;
import io.spine.code.proto.FieldDeclaration;
import io.spine.server.entity.model.EntityClass;
import io.spine.type.MessageType;

import java.lang.reflect.Method;

import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;
import static java.util.Arrays.stream;

final class ColumnIntrospector {

    private static final String ENTITY_WITH_COLUMNS_NAME_PATTERN = "%sWithColumns";

    private final EntityClass<?> entityClass;

    private ColumnIntrospector(EntityClass<?> aClass) {
        entityClass = aClass;
    }

    static ImmutableMap<String, Column> columnsOf(EntityClass<?> entityClass) {
        ColumnIntrospector introspector = new ColumnIntrospector(entityClass);
        ImmutableMap<String, Column> columns = introspector.columns();
        return columns;
    }

    private ImmutableMap<String, Column> columns() {
        ImmutableMap.Builder<String, Column> columns = ImmutableMap.builder();
        MessageType stateType = entityClass.stateMessageType();
        ImmutableList<FieldDeclaration> columnFields = ColumnOption.columnsOf(stateType);
        columnFields.forEach(field -> addToMap(field, columns, entityClass));
        ImmutableMap<String, Column> result = columns.build();
        return result;
    }

    private static void addToMap(FieldDeclaration field,
                                 ImmutableMap.Builder<String, Column> columns,
                                 EntityClass<?> entityClass) {
        String columnName = field.name()
                                 .value();
        Class<?> theEntityClass = entityClass.value();
        Class<?>[] interfaces = theEntityClass.getInterfaces();
        Class<? extends Message> stateClass = entityClass.stateClass();
        String interfaceName =
                format(ENTITY_WITH_COLUMNS_NAME_PATTERN, stateClass.getSimpleName());

        @SuppressWarnings("LocalVariableNamingConvention")
        boolean implementsEntityWithColumns = stream(interfaces)
                .anyMatch(clazz -> interfaceName.equals(clazz.getSimpleName()));

        String getterName = getterName(field);
        try {
            Method method;
            if (implementsEntityWithColumns) {
                method = theEntityClass.getMethod(getterName);
             } else {
                method = stateClass.getMethod(getterName);
            }
            Class<?> columnType = method.getReturnType();
            Column.Getter columnGetter = entity -> method.invoke(entity);
            Column column = new Column(columnName, columnType, columnGetter);
            columns.put(columnName, column);
        } catch (NoSuchMethodException e) {
            throw newIllegalStateException(e,
                                           "Getter with name %s not found in entity class %s.",
                                           getterName, entityClass.typeName());
        }
    }

    private static String getterName(FieldDeclaration field) {
        String fieldNameCamelCase = field.name()
                                         .toCamelCase();
        return format("get%s", fieldNameCamelCase);
    }
}
