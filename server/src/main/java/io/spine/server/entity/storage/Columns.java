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

import io.spine.server.entity.Entity;
import io.spine.server.entity.storage.EntityColumn.MemoizedValue;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.storage.ColumnValueExtractor.create;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;
import static java.lang.String.format;

/**
 * A utility class for working with {@linkplain EntityColumn entity columns}.
 *
 * <p>The methods of all {@link Entity entities} that fit
 * <a href="http://download.oracle.com/otndocs/jcp/7224-javabeans-1.01-fr-spec-oth-JSpec/">
 * the Java Bean</a> getter spec and annotated with {@link Column} are considered
 * {@linkplain EntityColumn columns}.
 *
 * <p>Additionally, the {@code Boolean} getters starting with {@code is-} are allowed (contrary to
 * the Java Bean spec), to enable situations like
 * <pre>
 *     {@code
 *             \@Nullable
 *             \@Column
 *             public Boolean isMale() {
 *                 return null;
 *             }
 *     }
 * </pre>
 *
 * <p>Inherited columns are taken into account too, but building entity hierarchies is strongly
 * discouraged.
 *
 * <p>Note that the returned type of an {@link EntityColumn} getter must either be primitive or
 * serializable, otherwise a runtime exception is thrown when trying to get an instance of
 * {@link EntityColumn}.
 *
 * @see EntityColumn
 */
final class Columns {

    /**
     * Prevents instantiation of this utility class.
     */
    private Columns() {
    }

    /**
     * Retrieves {@linkplain EntityColumn columns} for the given {@link Entity} class.
     *
     * <p>Performs checks for the entity column definitions correctness along the way.
     *
     * <p>If the check for correctness fails, throws {@link IllegalStateException}.
     *
     * @param entityClass the class containing the {@link EntityColumn} definition
     * @return a {@code Collection} of {@link EntityColumn} corresponded to entity class
     * @throws IllegalStateException if entity column definitions are incorrect
     */
    static Collection<EntityColumn> getAllColumns(Class<? extends Entity<?, ?>> entityClass) {
        checkNotNull(entityClass);

        ColumnReader columnReader = ColumnReader.forClass(entityClass);
        Collection<EntityColumn> entityColumns = columnReader.readColumns();
        return entityColumns;
    }

    /**
     * Retrieves an {@link EntityColumn} instance of the given name and from the given entity class.
     *
     * <p>If no column is found, an {@link IllegalArgumentException} is thrown.
     *
     * @param entityClass the class containing the {@link EntityColumn} definition
     * @param columnName  the entity column {@linkplain EntityColumn#getName() name}
     * @return an instance of {@link EntityColumn} with the given name
     * @throws IllegalArgumentException if the {@link EntityColumn} is not found
     */
    static EntityColumn findColumn(Class<? extends Entity<?, ?>> entityClass, String columnName) {
        checkNotNull(entityClass);
        checkColumnName(columnName);

        ColumnReader columnReader = ColumnReader.forClass(entityClass);
        Collection<EntityColumn> entityColumns = columnReader.readColumns();
        for (EntityColumn column : entityColumns) {
            if (columnName.equals(column.getName())) {
                return column;
            }
        }

        throw couldNotFindColumn(entityClass, columnName);
    }

    static void checkColumnName(String columnName) {
        checkNotEmptyOrBlank(columnName, "entity column name");
    }

    static IllegalArgumentException couldNotFindColumn(Class<? extends Entity> entityClass,
                                                       String columnName) {
        checkNotNull(entityClass);
        checkNotNull(columnName);
        throw new IllegalArgumentException(
                format("Could not find an `EntityColumn` description for `%s.%s`.",
                        entityClass.getCanonicalName(),
                        columnName));
    }

    /**
     * Extracts the {@linkplain EntityColumn column} values from the given {@linkplain Entity}, 
     * using given {@linkplain EntityColumn entity columns}.
     *
     * <p>By using predefined {@linkplain EntityColumn entity columns} the process of
     * {@linkplain ColumnReader#readColumns() obtaining columns} from the given {@link Entity} class
     * can be skipped.
     *
     * <p>This method will return {@linkplain Collections#emptyMap() empty map} for 
     * {@link Entity} classes that are non-public or cannot be subjected to column extraction 
     * for some other reason.
     *
     * @param  entity        
     *         an {@link Entity} to get the {@linkplain EntityColumn column} values from
     * @param  entityColumns 
     *         {@linkplain EntityColumn entity columns} which values should be extracted
     * @param  <E>           
     *         the type of the {@link Entity}
     * @return a {@code Map} of the column {@linkplain EntityColumn#getStoredName()
     *         names for storing} to their {@linkplain MemoizedValue memoized values}
     * @see MemoizedValue
     */
    static <E extends Entity<?, ?>> Map<String, MemoizedValue>
    extractColumnValues(E entity, Collection<EntityColumn> entityColumns) {
        checkNotNull(entity);
        checkNotNull(entityColumns);

        ColumnValueExtractor columnValueExtractor = create(entity, entityColumns);
        return columnValueExtractor.extractColumnValues();
    }
}
