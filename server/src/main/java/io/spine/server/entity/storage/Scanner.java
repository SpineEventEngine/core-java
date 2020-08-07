/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.code.proto.ColumnOption.columnsOf;
import static io.spine.reflect.Invokables.asHandle;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * An extractor of entity columns.
 */
final class Scanner {

    /**
     * The target entity class.
     */
    private final EntityClass<?> entityClass;

    /**
     * Is {@code true} when proto-based columns of the entity are {@linkplain InterfaceBasedColumn
     * interface-based}.
     *
     * <p>With the current implementation all proto-based columns are either interface-based or
     * {@linkplain SimpleColumn entity-state-based} and two column implementation methods cannot be
     * mixed.
     */
    private final boolean columnsInterfaceBased;

    Scanner(EntityClass<?> entityClass) {
        this.entityClass = entityClass;
        this.columnsInterfaceBased = EntityWithColumns.class.isAssignableFrom(entityClass.value());
    }

    /**
     * Obtains the {@linkplain SystemColumn system} columns of the class.
     */
    ImmutableMap<ColumnName, SysColumn> systemColumns() {
        ImmutableMap.Builder<ColumnName, SysColumn> columns = ImmutableMap.builder();
        Class<?> entityClazz = entityClass.value();
        addSystemColumns(entityClazz, columns);
        return columns.build();
    }

    private static void addSystemColumns(Class<?> entityClazz,
                                         ImmutableMap.Builder<ColumnName, SysColumn> columns) {
        Method[] methods = entityClazz.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(SystemColumn.class)) {
                addSystemColumn(method, columns);
            }
        }
    }

    private static void addSystemColumn(Method method,
                                        ImmutableMap.Builder<ColumnName, SysColumn> columns) {
        ColumnData data = ColumnData.of(method);
        MethodHandle handle = asHandle(method);
        SysColumn.Getter getter = entity -> invoke(handle, entity);
        SysColumn column = new SysColumn(data.name, data.type, getter);
        columns.put(column.name(), column);
    }

    /**
     * Obtains the {@linkplain SimpleColumn entity-state-based} columns of the class.
     *
     * <p>Currently is mutually exclusive with {@link #interfaceBasedColumns()}, i.e. one of the
     * methods will hold an empty map for any given entity.
     */
    ImmutableMap<ColumnName, SimpleColumn> simpleColumns() {
        if (columnsInterfaceBased) {
            return ImmutableMap.of();
        }
        ImmutableMap.Builder<ColumnName, SimpleColumn> columns = ImmutableMap.builder();
        for (FieldDeclaration field : columnsOf(entityClass.stateType())) {
            addSimpleColumn(field, columns);
        }
        return columns.build();
    }

    private void addSimpleColumn(FieldDeclaration field,
                                 ImmutableMap.Builder<ColumnName, SimpleColumn> columns) {
        ColumnData data = ColumnData.of(field, entityClass);
        MethodHandle handle = asHandle(data.getter);
        SimpleColumn.Getter getter = state -> invoke(handle, state);
        SimpleColumn column = new SimpleColumn(data.name, data.type, getter, field);
        columns.put(column.name(), column);
    }

    /**
     * Obtains the {@linkplain InterfaceBasedColumn interface-based} columns of the class.
     *
     * <p>Currently is mutually exclusive with {@link #simpleColumns()}, i.e. one of the
     * methods will hold an empty map for any given entity.
     */
    ImmutableMap<ColumnName, InterfaceBasedColumn> interfaceBasedColumns() {
        if (!columnsInterfaceBased) {
            return ImmutableMap.of();
        }
        ImmutableMap.Builder<ColumnName, InterfaceBasedColumn> columns = ImmutableMap.builder();
        for (FieldDeclaration field : columnsOf(entityClass.stateType())) {
            addImplementedColumn(field, columns);
        }
        return columns.build();
    }

    private void addImplementedColumn(FieldDeclaration field,
                                      ImmutableMap.Builder<ColumnName, InterfaceBasedColumn> columns) {
        ColumnData data = ColumnData.of(field, entityClass);

        MethodHandle stateGetterHandle = asHandle(data.getter);
        GetterFromState getterFromState = state -> invoke(stateGetterHandle, state);

        Method getter = getterOf(field, entityClass.value());
        MethodHandle getterHandle = asHandle(getter);
        GetterFromEntity getterFromEntity = entity -> invoke(getterHandle, entity);

        InterfaceBasedColumn column = new InterfaceBasedColumn(data.name,
                                                               data.type,
                                                               getterFromEntity,
                                                               getterFromState,
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

    private static Object invoke(MethodHandle method, Object receiver) {
        try {
            return method.invoke(receiver);
        } catch (Throwable throwable) {
            throw illegalStateWithCauseOf(throwable);
        }
    }

    /**
     * The basic column data needed to create an entity {@link Column} instance.
     */
    private static class ColumnData {

        /**
         * The name of the column.
         */
        private final ColumnName name;

        /**
         * The Java type of the column.
         */
        private final Class<?> type;

        /**
         * The method with which the column is extracted from the entity.
         *
         * <p>The {@linkplain ColumnDeclaredInProto entity-state-based} columns are extracted from
         * the entity state while the {@linkplain SystemColumn system} columns are obtained from
         * the entity itself.
         */
        private final Method getter;

        private ColumnData(ColumnName name, Class<?> type, Method getter) {
            this.name = name;
            this.type = type;
            this.getter = getter;
        }

        /**
         * Extracts the column data from the proto field declaration.
         *
         * <p>The resulting data can be used for constructing a {@link ColumnDeclaredInProto}
         * instance.
         */
        private static ColumnData of(FieldDeclaration protoColumn, EntityClass<?> entityClass) {
            ColumnName columnName = ColumnName.of(protoColumn);
            Method getter = getterOf(protoColumn, entityClass.stateClass());
            Class<?> columnType = getter.getReturnType();
            return new ColumnData(columnName, columnType, getter);
        }

        /**
         * Extracts the column data from the method annotated with {@link SystemColumn}.
         */
        private static ColumnData of(Method systemColumn) {
            SystemColumn annotation = checkNotNull(systemColumn.getAnnotation(SystemColumn.class));
            ColumnName name = ColumnName.of(annotation.name());
            Class<?> type = systemColumn.getReturnType();
            return new ColumnData(name, type, systemColumn);
        }
    }
}
