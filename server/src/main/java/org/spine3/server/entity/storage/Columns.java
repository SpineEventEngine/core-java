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

package org.spine3.server.entity.storage;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.EntityWithLifecycle;

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
import static com.google.common.collect.Multimaps.synchronizedListMultimap;
import static java.lang.String.format;

/**
 * A utility for generating the {@link Column Columns} {@linkplain Map}.
 *
 * <p>All the methods of the passed {@link Entity} that fit
 * <a href="http://download.oracle.com/otndocs/jcp/7224-javabeans-1.01-fr-spec-oth-JSpec/">
 * the Java Bean</a> getter spec are considered {@link Column Columns}.
 *
 * <p>Note that the returned type of a {@link Column} getter must either be primitive or
 * serializable, otherwise a runtime exception is thrown when trying to get an instance of
 * {@link Column}.
 *
 * <p>When passing an instance of an already known {@link Entity} type, the getters are retrieved
 * from a cache and are not updated.
 *
 * <p>There are several excluded methods, which are never taken into account and are
 * <b>not</b> considered {@link Column Columns}:
 * <ul>
 *     <li>{@link Object#getClass()}
 *     <li>{@link Entity#getId()}
 *     <li>{@link Entity#getState()}
 *     <li>{@link Entity#getDefaultState()}
 *     <li>{@link org.spine3.server.entity.EntityWithLifecycle#getLifecycleFlags()
 *     EntityWithLifecycle.getLifecycleFlags()}
 *     <li>{@link org.spine3.server.aggregate.Aggregate#getBuilder() Aggregate.getBuilder()}
 * </ul>
 *
 * <p>Note: if creating a getter method with a name which intersects with one of these method
 * names, this getter method will also <b>not</b> be considered a {@link Column Column}.
 *
 * @author Dmytro Dashenkov
 * @see Column
 */
class Columns {

    private static final String SPINE_PACKAGE = "org.spine3.";
    private static final String NON_PUBLIC_CLASS_WARNING =
            "Passed entity class %s is not public. Storage fields won't be extracted.";
    private static final String NON_PUBLIC_INTERNAL_CLASS_WARNING =
            "Passed entity class %s is probably a Spine internal non-public entity. " +
                    "Storage fields won't be extracted.";

    /**
     * A one to many container of the {@link Class} to {@link Column} relations.
     *
     * <p>This container is mutable and thread safe.
     */
    private static final Multimap<Class<? extends Entity>, Column> knownEntityProperties =
            synchronizedListMultimap(
                    LinkedListMultimap.<Class<? extends Entity>, Column>create());

    private Columns() {
        // Prevent initialization of a utility class
    }

    /**
     * Generates the {@link Column Columns} for the given {@linkplain Entity}.
     *
     * <p>If there were no {@linkplain Entity entities} stored in the scope of current class
     * <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-12.html">initialization</a>,
     * a call to this method will create a cache of the passed {@linkplain Entity entity's} getters
     * and use it in all the successive calls.
     *
     * @param entity an {@link Entity} to get the {@link Column Columns} from
     * @param <E>    the type of the {@link Entity}
     * @return a {@link Map} of the {@link Column Column} names to their
     * {@linkplain Column.MemoizedValue memoized values}.
     * @see Column.MemoizedValue
     */
    static <E extends Entity<?, ?>> Map<String, Column.MemoizedValue> from(E entity) {
        checkNotNull(entity);
        final Class<? extends Entity> entityType = entity.getClass();
        final int modifiers = entityType.getModifiers();
        if (!Modifier.isPublic(modifiers)) {
            logNonPublicClass(entityType);
            return Collections.emptyMap();
        }
        ensureRegistered(entityType);

        final Map<String, Column.MemoizedValue> fields = extractColumns(entityType, entity);
        return fields;
    }

    /**
     * Retrieves a {@link Column} instance of the given name and from the given Entity class.
     *
     * <p>If the given Entity class has not yet been added to the Column cache, it's added upon this
     * operation.
     *
     * <p>If no column is found, an {@link IllegalArgumentException} is thrown.
     *
     * @param entityClass the class containing the {@link Column} definition
     * @param columnName  the name of the {@link Column}
     * @return an instance of {@link Column} with the given name
     * @throws IllegalArgumentException if the {@link Column} is not found
     */
    static Column findColumn(Class<? extends Entity> entityClass, String columnName) {
        checkNotNull(entityClass);
        checkNotNull(columnName);
        ensureRegistered(entityClass);

        final Collection<Column> cachedColumns = knownEntityProperties.get(entityClass);
        for (Column column : cachedColumns) {
            if (column.getName()
                      .equals(columnName)) {
                return column;
            }
        }

        throw new IllegalArgumentException(
                format("Could not find a Column description for %s.%s.",
                       entityClass.getCanonicalName(),
                       columnName));
    }

    /**
     * Generates the {@link Column Column} values considering the passed
     * {@linkplain Entity entity type} indexed.
     *
     * @param entityType indexed type of the {@link Entity}
     * @param entity     the object which to take the values from
     * @return a {@link Map} of the {@link Column Columns}
     */
    private static Map<String, Column.MemoizedValue> extractColumns(
            Class<? extends Entity> entityType,
            Entity entity) {
        final Collection<Column> storageFieldProperties =
                knownEntityProperties.get(entityType);
        final Map<String, Column.MemoizedValue> values =
                new HashMap<>(storageFieldProperties.size());

        for (Column column : storageFieldProperties) {
            final String name = column.getName();
            final Column.MemoizedValue value = column.memoizeFor(entity);
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
     * Caches the {@linkplain Entity entity type} for further {@link Column Columns} retrieving.
     */
    private static void addToCache(Class<? extends Entity> entityType) {
        final BeanInfo entityDescriptor;
        try {
            entityDescriptor = Introspector.getBeanInfo(entityType);
        } catch (IntrospectionException e) {
            throw new IllegalStateException(e);
        }

        for (PropertyDescriptor property : entityDescriptor.getPropertyDescriptors()) {
            final Method getter = property.getReadMethod();
            if (!ExcludedMethod.contain(getter.getName())) {
                final Column storageField = Column.from(getter);
                knownEntityProperties.put(entityType, storageField);
            }
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

    private enum ExcludedMethod {

        /**
         * @see Object#getClass()
         */
        GET_CLASS("getClass"),

        /**
         * The {@link Entity} identifier is stored separately and is used more widely then just
         * for queries.
         *
         * @see Entity#getId()
         */
        GET_ID("getId"),

        /**
         * The {@link Entity#getState() Entity state} is stored as a binary by default.
         *
         * <p>This provides faster write and read operations, but slows down the queries.
         *
         * @see Entity#getState()
         */
        GET_STATE("getState"),

        /**
         * The default state is not a field of the {@link Entity}, but a method which provides
         * the ancillary information to the repository etc.
         *
         * @see Entity#getDefaultState()
         */
        GET_DEFAULT_STATE("getDefaultState"),

        /**
         * The {@link org.spine3.server.entity.LifecycleFlags lifecycle flags} are fields that
         * should be stored separately as well.
         *
         * <p>When querying multiple records, the lifecycle flags help to filter them.
         *
         * @see EntityWithLifecycle#getLifecycleFlags()
         */
        GET_LIFECYCLE_FLAGS("getLifecycleFlags");

        private final String methodName;

        ExcludedMethod(String methodName) {
            this.methodName = methodName;
        }

        private static boolean contain(String methodName) {
            for (ExcludedMethod method : ExcludedMethod.values()) {
                if (method.methodName.equals(methodName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
