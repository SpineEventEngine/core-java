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
import io.spine.server.entity.storage.InterfaceBasedColumn.GetterFromEntity;
import io.spine.server.entity.storage.InterfaceBasedColumn.GetterFromState;

import java.lang.reflect.Method;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.code.proto.ColumnOption.columnsOf;
import static io.spine.reflect.Methods.setAccessibleAndInvoke;
import static io.spine.util.Exceptions.newIllegalStateException;

final class Introspector {

    private final EntityClass<?> entityClass;
    private final boolean columnsInterfaceBased;

    Introspector(EntityClass<?> entityClass) {
        this.entityClass = entityClass;
        this.columnsInterfaceBased =
                EntityWithColumns.class.isAssignableFrom(entityClass.value());
    }

    ImmutableMap<ColumnName, SpineColumn> systemColumns() {
        ImmutableMap.Builder<ColumnName, SpineColumn> columns = ImmutableMap.builder();
        Class<?> entityClazz = entityClass.value();
        Method[] methods = entityClazz.getMethods();
        Arrays.stream(methods)
              .filter(method -> method.isAnnotationPresent(SystemColumn.class))
              .forEach(method -> addSystemColumn(method, columns));
        return columns.build();
    }

    private static void addSystemColumn(Method method,
                                        ImmutableMap.Builder<ColumnName, SpineColumn> columns) {
        ColumnData data = ColumnData.of(method);
        SpineColumn.Getter getter = entity -> setAccessibleAndInvoke(data.getter, entity);
        SpineColumn column = new SpineColumn(data.name, data.type, getter);
        columns.put(column.name(), column);
    }

    ImmutableMap<ColumnName, SimpleColumn> simpleColumns() {
        if (columnsInterfaceBased) {
            return ImmutableMap.of();
        }
        ImmutableMap.Builder<ColumnName, SimpleColumn> columns = ImmutableMap.builder();
        columnsOf(entityClass.stateType())
                .forEach(field -> addSimpleColumn(field, columns));
        return columns.build();
    }

    private void addSimpleColumn(FieldDeclaration field,
                                 ImmutableMap.Builder<ColumnName, SimpleColumn> columns) {
        ColumnData data = ColumnData.of(field, entityClass);
        SimpleColumn.Getter getter = state -> setAccessibleAndInvoke(data.getter, state);
        SimpleColumn column = new SimpleColumn(data.name, data.type, getter, field);
        columns.put(column.name(), column);
    }

    ImmutableMap<ColumnName, InterfaceBasedColumn> interfaceBasedColumns() {
        if (!columnsInterfaceBased) {
            return ImmutableMap.of();
        }
        ImmutableMap.Builder<ColumnName, InterfaceBasedColumn> columns = ImmutableMap.builder();
        columnsOf(entityClass.stateType())
                .forEach(field -> addImplementedColumn(field, columns));
        return columns.build();
    }

    private void addImplementedColumn(FieldDeclaration field,
                                      ImmutableMap.Builder<ColumnName, InterfaceBasedColumn> columns) {
        ColumnData data = ColumnData.of(field, entityClass);
        Method getter = getterOf(field, entityClass.value());
        GetterFromState getterFromState =
                state -> setAccessibleAndInvoke(data.getter, state);
        GetterFromEntity getterFromEntity =
                entity -> setAccessibleAndInvoke(getter, entity);
        InterfaceBasedColumn column = new InterfaceBasedColumn(data.name,
                                                               data.type,
                                                               getterFromState,
                                                               getterFromEntity,
                                                               field);
        columns.put(column.name(), column);
    }

    private static Method getterOf(FieldDeclaration field, Class<?> clazz) {
        String getterName = field.javaGetterName();
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

    private static class ColumnData {

        private final ColumnName name;
        private final Class<?> type;
        private final Method getter;

        private ColumnData(ColumnName name, Class<?> type, Method getter) {
            this.name = name;
            this.type = type;
            this.getter = getter;
        }

        private static ColumnData of(FieldDeclaration protoColumn, EntityClass<?> entityClass) {
            ColumnName columnName = ColumnName.of(protoColumn);
            Method getter = getterOf(protoColumn, entityClass.stateClass());
            Class<?> columnType = getter.getReturnType();
            return new ColumnData(columnName, columnType, getter);
        }

        private static ColumnData of(Method systemColumn) {
            SystemColumn annotation = checkNotNull(systemColumn.getAnnotation(SystemColumn.class));
            ColumnName name = ColumnName.of(annotation.name());
            Class<?> type = systemColumn.getReturnType();
            return new ColumnData(name, type, systemColumn);
        }
    }
}
