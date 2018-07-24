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

import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.server.entity.storage.Methods.getAnnotatedVersion;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A class whose purpose is to obtain {@linkplain EntityColumn entity columns} from the given
 * {@link Entity} type.
 *
 * <p>Each {@code ColumnReader} instance is created for the specified {@link Entity} class.
 *
 * <p>Along with obtaining {@link Entity} class columns, the {@code ColumnReader} performs various
 * checks verifying that {@link EntityColumn} definitions in the processed {@link Entity} class are
 * correct. If column definitions are incorrect, the exception is thrown upon
 * {@linkplain EntityColumn entity columns} reading.
 *
 * @author Dmytro Kuzmin
 * @see Columns
 * @see EntityColumn
 */
class ColumnReader {

    private final Class<? extends Entity> entityClass;

    private ColumnReader(Class<? extends Entity> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * Creates an instance of {@link ColumnReader} for the given {@link Entity} class.
     *
     * <p>The reader can be further used to {@linkplain ColumnReader#readColumns() obtain}
     * {@linkplain EntityColumn entity columns} for the given class.
     *
     * @param entityClass {@link Entity} class for which to create the instance
     * @return new instance of {@code ColumnReader} for the specified class
     */
    static ColumnReader forClass(Class<? extends Entity> entityClass) {
        checkNotNull(entityClass);

        return new ColumnReader(entityClass);
    }

    /**
     * Retrieves {@linkplain EntityColumn columns} for the processed {@link Entity} class.
     *
     * <p>Performs checks for entity column definitions correctness along the way.
     *
     * <p>If the check for correctness fails, throws {@link IllegalStateException}.
     *
     * @return a {@code Collection} of {@link EntityColumn} corresponded to entity class
     * @throws IllegalStateException if entity column definitions are incorrect
     */
    Collection<EntityColumn> readColumns() {
        BeanInfo entityDescriptor;
        try {
            entityDescriptor = Introspector.getBeanInfo(entityClass);
        } catch (IntrospectionException e) {
            throw new IllegalStateException(e);
        }

        Collection<EntityColumn> entityColumns = newLinkedList();
        for (PropertyDescriptor property : entityDescriptor.getPropertyDescriptors()) {
            Method getter = property.getReadMethod();
            boolean isEntityColumn = getAnnotatedVersion(getter).isPresent();
            if (isEntityColumn) {
                EntityColumn column = EntityColumn.from(getter);
                entityColumns.add(column);
            }
        }

        checkRepeatedColumnNames(entityColumns);
        return entityColumns;
    }


    /**
     * Ensures that the specified columns have no repeated names.
     *
     * <p>If the check fails, throws {@link IllegalStateException}.
     *
     * @param columns     the columns to check
     * @throws IllegalStateException if columns contain repeated names
     */
    private void checkRepeatedColumnNames(Iterable<EntityColumn> columns) {
        Collection<String> checkedNames = newLinkedList();
        for (EntityColumn column : columns) {
            String columnName = column.getStoredName();
            if (checkedNames.contains(columnName)) {
                throw newIllegalStateException(
                        "The entity `%s` has columns with the same name for storing `%s`.",
                        entityClass.getName(),
                        columnName);
            }
            checkedNames.add(columnName);
        }
    }
}
