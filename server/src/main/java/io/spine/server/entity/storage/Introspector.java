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

import com.google.common.collect.ImmutableMap;
import io.spine.base.EntityWithColumns;
import io.spine.code.proto.FieldDeclaration;
import io.spine.server.entity.model.EntityClass;
import io.spine.type.MessageType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.code.proto.ColumnOption.columnsOf;
import static io.spine.util.Exceptions.newIllegalStateException;

final class Introspector {

    private final EntityClass<?> entityClass;
    private final boolean columnsInterfaceBased;

    Introspector(EntityClass<?> entityClass) {
        this.entityClass = entityClass;
        this.columnsInterfaceBased =
                EntityWithColumns.class.isAssignableFrom(entityClass.value());
    }

    ImmutableMap<ColumnName, Column> systemColumns() {
        ImmutableMap.Builder<ColumnName, Column> columns = ImmutableMap.builder();
        Class<?> entityClazz = entityClass.value();
        Method[] methods = entityClazz.getMethods();
        Arrays.stream(methods)
              .filter(method -> method.isAnnotationPresent(SystemColumn.class))
              .forEach(method -> addSystemColumn(method, columns));
        return columns.build();
    }

    private static void addSystemColumn(Method method,
                                        ImmutableMap.Builder<ColumnName, Column> columns) {
        ColumnData data = ColumnData.of(method);
        Column column = new Column(data.name, data.type, data.getter);
        columns.put(column.name(), column);
    }

    ImmutableMap<ColumnName, Column> protoColumns() {
        ImmutableMap.Builder<ColumnName, Column> columns = ImmutableMap.builder();
        MessageType stateType = entityClass.stateType();
        columnsOf(stateType)
                .forEach(field -> addProtoColumn(field, columns));
        ImmutableMap<ColumnName, Column> result = columns.build();
        return result;
    }

    private void
    addProtoColumn(FieldDeclaration field, ImmutableMap.Builder<ColumnName, Column> columns) {
        ColumnName columnName = ColumnName.of(field);
        if (columnsInterfaceBased) {
            columns.put(columnName, interfaceBasedColumn(field));
        } else {
            columns.put(columnName, column(field));
        }
    }

    private Column interfaceBasedColumn(FieldDeclaration field) {
        ColumnData data = ColumnData.of(field, entityClass);
        Method getterFromInterface = getterOf(field, entityClass.value());
        Column.Getter columnGetterFromInterface =
                entity -> setAccessibleAndInvoke(getterFromInterface, entity);
        return new Column(data.name, data.type, data.getter, columnGetterFromInterface, field);
    }

    private Column column(FieldDeclaration field) {
        ColumnData data = ColumnData.of(field, entityClass);
        return new Column(data.name, data.type, data.getter, field);
    }

    private static Method getterOf(FieldDeclaration field, Class<?> clazz) {
        String getterName = field.getterName();
        try {
            Method result = clazz.getMethod(getterName);
            return result;
        } catch (NoSuchMethodException e) {
            throw getterNotFound(field, getterName, clazz, e);
        }
    }

    private static IllegalStateException getterNotFound(FieldDeclaration field,
                                                        String getterName,
                                                        Class<?> clazz,
                                                        NoSuchMethodException e) {
        throw newIllegalStateException(
                e,
                "Expected to find a getter with name `%s` in class `%s` according to the " +
                        "declaration of column `%s`.",
                getterName, clazz.getCanonicalName(), field.name());
    }

    private static Object setAccessibleAndInvoke(Method method, Object target)
            throws InvocationTargetException, IllegalAccessException {
        method.setAccessible(true);
        Object result = method.invoke(target);
        method.setAccessible(false);
        return result;
    }

    private static class ColumnData {
        private final ColumnName name;
        private final Class<?> type;
        private final Column.Getter getter;

        private ColumnData(ColumnName name, Class<?> type, Column.Getter getter) {
            this.name = name;
            this.type = type;
            this.getter = getter;
        }

        private static ColumnData of(FieldDeclaration protoColumn, EntityClass<?> entityClass) {
            ColumnName columnName = ColumnName.of(protoColumn);
            Method getter = getterOf(protoColumn, entityClass.stateClass());
            Class<?> columnType = getter.getReturnType();
            Column.Getter columnGetter =
                    entity -> setAccessibleAndInvoke(getter, entity.state());
            return new ColumnData(columnName, columnType, columnGetter);
        }

        private static ColumnData of(Method systemColumn) {
            SystemColumn annotation = checkNotNull(systemColumn.getAnnotation(SystemColumn.class));
            ColumnName name = ColumnName.of(annotation.name());
            Class<?> type = systemColumn.getReturnType();
            Column.Getter getter = entity -> setAccessibleAndInvoke(systemColumn, entity);
            return new ColumnData(name, type, getter);
        }
    }
}
