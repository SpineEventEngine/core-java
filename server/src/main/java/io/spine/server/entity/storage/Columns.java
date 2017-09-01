/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityClass;
import io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Multimaps.synchronizedListMultimap;
import static io.spine.server.entity.storage.ColumnRecords.getAnnotatedVersion;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;
import static java.lang.String.format;

/**
 * A utility for generating the {@linkplain EntityColumn columns} {@linkplain Map}.
 *
 * <p>The methods of all {@link Entity entities} that fit
 * <a href="http://download.oracle.com/otndocs/jcp/7224-javabeans-1.01-fr-spec-oth-JSpec/">
 * the Java Bean</a> getter spec and annotated with {@link Column}
 * are considered {@linkplain EntityColumn columns}.
 * Inherited columns are taken into account either.
 *
 * <p>Note that the returned type of a {@link EntityColumn} getter must either be primitive or
 * serializable, otherwise a runtime exception is thrown when trying to get an instance of
 * {@link EntityColumn}.
 *
 * <p>When passing an instance of an already known {@link Entity} type, the getters are retrieved
 * from a cache and are not updated.
 *
 * @author Dmytro Dashenkov
 * @see EntityColumn
 */
@Internal
public class Columns {

    private static final String SPINE_PACKAGE = "io.spine.";
    private static final String NON_PUBLIC_CLASS_WARNING =
            "Passed entity class %s is not public. Storage fields won't be extracted.";
    private static final String NON_PUBLIC_INTERNAL_CLASS_WARNING =
            "Passed entity class %s is probably a Spine internal non-public entity. " +
                    "Storage fields won't be extracted.";

    /**
     * A one to many container of the {@link Class} to {@link EntityColumn} relations.
     *
     * <p>This container is mutable and thread safe.
     */
    private static final Multimap<Class<? extends Entity>, EntityColumn> knownEntityProperties =
            synchronizedListMultimap(
                    LinkedListMultimap.<Class<? extends Entity>, EntityColumn>create());

    private Columns() {
        // Prevent initialization of a utility class
    }

    /**
     * Ensures that the entity columns are valid for the specified entity class and caches them.
     *
     * @param entityClass the class to check entity columns
     */
    public static void checkColumnDefinitions(EntityClass<?> entityClass) {
        ensureRegistered(entityClass.value());
    }

    /**
     * Generates the {@linkplain EntityColumn columns} for the given {@linkplain Entity}.
     *
     * <p>If there were no {@linkplain Entity entities} stored in the scope of current class
     * <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-12.html">initialization</a>,
     * a call to this method will create a cache of the passed {@linkplain Entity entity's} getters
     * and use it in all the successive calls.
     *
     * @param entity an {@link Entity} to get the {@linkplain EntityColumn columns} from
     * @param <E>    the type of the {@link Entity}
     * @return a {@link Map} of the column {@linkplain EntityColumn#getStoredName() names for storing}
     *         to their {@linkplain MemoizedValue memoized values}.
     * @see MemoizedValue
     */
    static <E extends Entity<?, ?>> Map<String, MemoizedValue> from(E entity) {
        checkNotNull(entity);
        final Class<? extends Entity> entityType = entity.getClass();
        final int modifiers = entityType.getModifiers();
        if (!Modifier.isPublic(modifiers)) {
            logNonPublicClass(entityType);
            return Collections.emptyMap();
        }
        ensureRegistered(entityType);

        final Map<String, MemoizedValue> fields = extractColumns(entityType, entity);
        return fields;
    }

    /**
     * Retrieves a {@link EntityColumn} instance of the given name and from the given entity class.
     *
     * <p>If the given entity class has not yet been added to the column cache,
     * it will be added upon this operation.
     *
     * <p>If no column is found, an {@link IllegalArgumentException} is thrown.
     *
     * @param entityClass the class containing the {@link EntityColumn} definition
     * @param columnName  the name of the {@link EntityColumn}
     * @return an instance of {@link EntityColumn} with the given name
     * @throws IllegalArgumentException if the {@link EntityColumn} is not found
     */
    static EntityColumn findColumn(Class<? extends Entity> entityClass, String columnName) {
        checkNotNull(entityClass);
        checkNotEmptyOrBlank(columnName, "entity column name");
        ensureRegistered(entityClass);

        final Collection<EntityColumn> cachedColumns = getColumns(entityClass);
        for (EntityColumn column : cachedColumns) {
            if (column.getName()
                      .equals(columnName)) {
                return column;
            }
        }

        throw new IllegalArgumentException(
                format("Could not find a EntityColumn description for %s.%s.",
                       entityClass.getCanonicalName(),
                       columnName));
    }

    /**
     * Retrieves {@linkplain EntityColumn columns} for the given {@code Entity} class.
     *
     * @param entityClass the class containing the {@link EntityColumn} definition
     * @return a {@link Collection} of {@link EntityColumn} corresponded to entity class
     */
    static Collection<EntityColumn> getColumns(Class<? extends Entity> entityClass) {
        checkNotNull(entityClass);
        ensureRegistered(entityClass);

        final Collection<EntityColumn> result = knownEntityProperties.get(entityClass);
        return result;
    }

    /**
     * Generates the {@link EntityColumn} values considering the passed
     * {@linkplain Entity entity type} indexed.
     *
     * @param entityType indexed type of the {@link Entity}
     * @param entity     the object which to take the values from
     * @return a {@link Map} of the {@linkplain EntityColumn columns}
     */
    private static Map<String, MemoizedValue> extractColumns(Class<? extends Entity> entityType,
                                                             Entity entity) {
        final Collection<EntityColumn> storageFieldProperties = knownEntityProperties.get(entityType);
        final Map<String, MemoizedValue> values = new HashMap<>(storageFieldProperties.size());

        for (EntityColumn column : storageFieldProperties) {
            final String name = column.getStoredName();
            final MemoizedValue value = column.memoizeFor(entity);
            values.put(name, value);
        }
        return values;
    }

    private static void ensureRegistered(Class<? extends Entity> entityType) {
        if (knownEntityProperties.containsKey(entityType)) {
            return;
        }
        addToCache(entityType);
    }

    /**
     * Caches the {@linkplain Entity entity type}
     * for further {@linkplain EntityColumn columns} retrieving.
     */
    private static void addToCache(Class<? extends Entity> entityType) {
        final BeanInfo entityDescriptor;
        try {
            entityDescriptor = Introspector.getBeanInfo(entityType);
        } catch (IntrospectionException e) {
            throw new IllegalStateException(e);
        }

        final Collection<EntityColumn> entityColumns = newLinkedList();
        for (PropertyDescriptor property : entityDescriptor.getPropertyDescriptors()) {
            final Method getter = property.getReadMethod();
            final boolean isEntityColumn = getAnnotatedVersion(getter).isPresent();
            if (isEntityColumn) {
                final EntityColumn column = EntityColumn.from(getter);
                entityColumns.add(column);
            }
        }
        checkRepeatedColumnNames(entityColumns, entityType);
        knownEntityProperties.putAll(entityType, entityColumns);
    }

    /**
     * Ensures that the specified columns have no repeated names.
     *
     * @param columns     the columns to check
     * @param entityClass the entity class for the columns
     */
    private static void checkRepeatedColumnNames(Iterable<EntityColumn> columns,
                                                 Class<? extends Entity> entityClass) {
        final Collection<String> checkedNames = newLinkedList();
        for (EntityColumn column : columns) {
            final String columnName = column.getStoredName();
            if (checkedNames.contains(columnName)) {
                final String msg = "The entity `%s` has columns with the same name for storing `%s`.";
                throw newIllegalStateException(msg, entityClass.getName(), columnName);
            }
            checkedNames.add(columnName);
        }
    }

    /**
     * Writes the non-public {@code Entity} class warning into the log unless the passed class
     * represents one of the Spine internal {@link Entity} implementations.
     */
    private static void logNonPublicClass(Class<? extends Entity> cls) {
        final String className = cls.getCanonicalName();
        final boolean internal = className.startsWith(SPINE_PACKAGE);
        if (internal) {
            log().trace(format(NON_PUBLIC_INTERNAL_CLASS_WARNING, className));
        } else {
            log().warn(format(NON_PUBLIC_CLASS_WARNING, className));
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(Columns.class);
    }
}
