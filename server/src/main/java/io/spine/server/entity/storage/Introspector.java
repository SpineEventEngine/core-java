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
import io.spine.server.entity.Entity;
import io.spine.server.entity.model.EntityClass;
import io.spine.type.MessageType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import static io.spine.code.proto.ColumnOption.columnsOf;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;
import static java.util.function.Function.identity;

final class Introspector {

    private static final String GET_PREFIX = "get";

    private final EntityClass<?> entityClass;

    Introspector(EntityClass<?> entityClass) {
        this.entityClass = entityClass;
    }

    ImmutableMap<ColumnName, Column> systemColumns() {
        ImmutableMap.Builder<ColumnName, Column> columns = ImmutableMap.builder();
        Class<?> entityClazz = entityClass.value();
        Method[] methods = entityClazz.getMethods();
        for (Method method : methods) {
            boolean isSystemColumn = method.isAnnotationPresent(SystemColumn.class);
            if (isSystemColumn) {
                ColumnName columnName = ColumnName.from(method);
                Class<?> columnType = method.getReturnType();
                Column.Getter columnGetter = entity -> setAccessibleAndInvoke(method, entity);
                Column column = new Column(columnName, columnType, columnGetter);
                columns.put(column.name(), column);
            }
        }
        ImmutableMap<ColumnName, Column> result = columns.build();
        return result;
    }

    ImmutableMap<ColumnName, Column> protoColumns() {
        ImmutableMap.Builder<ColumnName, Column> columns = ImmutableMap.builder();
        MessageType stateType = entityClass.stateType();
        columnsOf(stateType)
                .forEach(field -> addToMap(field, columns));
        ImmutableMap<ColumnName, Column> result = columns.build();
        return result;
    }

    private void
    addToMap(FieldDeclaration field, ImmutableMap.Builder<ColumnName, Column> columns) {

        @SuppressWarnings("LocalVariableNamingConvention")
        boolean implementsEntityWithColumns =
                EntityWithColumns.class.isAssignableFrom(entityClass.value());
        ColumnName columnName = ColumnName.of(field);
        if (implementsEntityWithColumns) {
            columns.put(columnName, columnOfEntity(field));
        } else {
            columns.put(columnName, columnOfEntityState(field));
        }
    }

    private Column columnOfEntity(FieldDeclaration field) {
        return createColumn(field, entityClass.value(), identity());
    }

    private Column columnOfEntityState(FieldDeclaration field) {
        return createColumn(field, entityClass.stateClass(), Entity::state);
    }

    @SuppressWarnings("MethodParameterNamingConvention")
    private Column createColumn(FieldDeclaration field,
                                Class<?> classWithGetter,
                                Function<Entity<?, ?>, ?> getterTargetFromEntity) {
        ColumnName columnName = ColumnName.of(field);
        Method getter = getterOf(field, classWithGetter);
        Class<?> columnType = getter.getReturnType();
        Column.Getter columnGetter =
                entity -> setAccessibleAndInvoke(getter, getterTargetFromEntity.apply(entity));
        return new Column(columnName, columnType, columnGetter);
    }

    private Method getterOf(FieldDeclaration field, Class<?> clazz) {
        String getterName = getterName(field);
        try {
            Method result = clazz.getMethod(getterName);
            return result;
        } catch (NoSuchMethodException e) {
            throw getterNotFound(field, getterName, e);
        }
    }

    private IllegalStateException getterNotFound(FieldDeclaration field,
                                                 String getterName,
                                                 NoSuchMethodException e) {
        throw newIllegalStateException(
                e,
                "Expected to find a getter with name %s in entity class %s according to the " +
                        "declaration of column %s.",
                getterName, entityClass.typeName(), field.name());
    }

    private static String getterName(FieldDeclaration field) {
        String fieldNameCamelCase = field.name()
                                         .toCamelCase();
        return format("%s%s", GET_PREFIX, fieldNameCamelCase);
    }

    private static Object setAccessibleAndInvoke(Method method, Object target)
            throws InvocationTargetException, IllegalAccessException {
        method.setAccessible(true);
        Object result = method.invoke(target);
        method.setAccessible(false);
        return result;
    }
}
