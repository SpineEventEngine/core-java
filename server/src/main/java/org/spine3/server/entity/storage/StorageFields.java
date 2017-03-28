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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.annotations.Internal;
import org.spine3.server.entity.Entity;

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
 * A utility for generating the Storage fields {@linkplain Map}.
 *
 * <p>All the methods of the passed {@link Entity} that fit
 * <a href="http://download.oracle.com/otndocs/jcp/7224-javabeans-1.01-fr-spec-oth-JSpec/">the Java Bean</a>
 *  getter spec are considered Storage Fields and indexed respectively.
 *
 * <p>When passing an instance of already known {@link Entity} type, the methods are retrieved from
 * a cache and are not updated.
 *
 * <p>There are several excluded methods, which are never taken into account and are
 * <b>not</b> considered Storage Fields:
 * <ul>
 *     <li>{@link Object#getClass()}
 *     <li>{@link Entity#getId()}
 *     <li>{@link Entity#getState()}
 *     <li>{@link Entity#getDefaultState()}
 *     <li>{@link org.spine3.server.entity.EntityWithLifecycle#getLifecycleFlags() EntityWithLifecycle#getLifecycleFlags()}
 *     <li>{@link org.spine3.server.aggregate.Aggregate#getBuilder() Aggregate#getBuilder()}
 * </ul>
 *
 * <p>Note: if creating a getter method with a name which intersects with one of these method
 * names, your getter method will also <b>not</b> be considered a Storage Field.
 *
 * @author Dmytro Dashenkov
 * @see Column
 */
public class StorageFields {

    private static final String NON_PUBLIC_CLASS_WARNING =
            "Passed entity class %s is not public. Storage fields won't be extracted.";

    /**
     * An immutable {@link java.util.Set set} of excluded methods which fit the getter pattern but
     * are not Storage Fields.
     */
    private static final ImmutableSet<String> EXCLUDED_METHODS =
            ImmutableSet.<String>builder()
                        .add("getId")
                        .add("getState")
                        .add("getDefaultState")
                        .add("getLifecycleFlags")
                        .add("getBuilder")
                        .add("getClass")
                        .build();

    /**
     * A one to many container of the {@link Class} to {@link Column} relations.
     *
     * <p>This container is mutable and thread safe.
     */
    private static final Multimap<Class<? extends Entity>, Column<?>> knownEntityProperties =
            synchronizedListMultimap(
                    LinkedListMultimap.<Class<? extends Entity>, Column<?>>create());

    private StorageFields() {
        // Prevent initialization of a utility class
    }

    /**
     * @return an {@link Collections#emptyMap()} with the type of the Storage Fields map
     */
    @Internal
    public static Map<String, Column.MemoizedValue<?>> empty() {
        return Collections.emptyMap();
    }

    /**
     * Generates the Storage Fields for the given {@linkplain Entity}.
     *
     * <p>If there were no {@linkplain Entity entities} stored in the scope of current class
     * <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-12.html">initialization</a>,
     * a call to this method will create an index of the passed {@linkplain Entity entity's} getters
     * and use it in all the successive calls.
     *
     * @param entity an {@link Entity} to get the Storage Fields from
     * @param <E> the type of the {@link Entity}
     * @return a {@link Map} of the Storage Field names to their
     * {@linkplain Column.MemoizedValue memoized values}.
     * @see Column.MemoizedValue
     */
    public static <E extends Entity<?, ?>> Map<String, Column.MemoizedValue<?>> from(E entity) {
        checkNotNull(entity);
        final Class<? extends Entity> entityType = entity.getClass();
        final int modifiers = entityType.getModifiers();
        if (!Modifier.isPublic(modifiers)) {
            log().warn(
                    format(NON_PUBLIC_CLASS_WARNING,
                           entityType.getCanonicalName()));
            return Collections.emptyMap();
        }

        ensureRegistered(entityType);

        final Map<String, Column.MemoizedValue<?>> fields = getStorageFields(entityType, entity);
        return fields;
    }

    /**
     * Generates the Storage Filed values considering the passed {@linkplain Entity entity type}
     * indexed.
     *
     * @param entityType indexed type of the {@link Entity}
     * @param entity the object which to take the values from
     * @return a {@link Map} of the Storage Fields
     */
    private static Map<String, Column.MemoizedValue<?>> getStorageFields(
            Class<? extends Entity> entityType,
            Entity entity) {
        final Collection<Column<?>> storageFieldProperties =
                knownEntityProperties.get(entityType);
        final Map<String, Column.MemoizedValue<?>> values =
                new HashMap<>(storageFieldProperties.size());

        for (Column<?> column : storageFieldProperties) {
            final String name = column.getName();
            final Column.MemoizedValue<?> value = column.memoizeFor(entity);
            values.put(name, value);
        }
        return values;
    }

    private static void ensureRegistered(Class<? extends Entity> entityType) {
        if (knownEntityProperties.containsKey(entityType)) {
            return;
        }
        addToIndexes(entityType);
    }

    /**
     * Indexes the {@linkplain Entity entity type} for further Storage Fields retrieving.
     */
    private static void addToIndexes(Class<? extends Entity> entityType) {
        final BeanInfo entityDescriptor;
        try {
            entityDescriptor = Introspector.getBeanInfo(entityType);
        } catch (IntrospectionException e) {
            throw new IllegalStateException(e);
        }

        for (PropertyDescriptor property : entityDescriptor.getPropertyDescriptors()) {
            final Method getter = property.getReadMethod();
            if (!EXCLUDED_METHODS.contains(getter.getName())) {
                final Column<?> storageField = Column.from(getter);
                knownEntityProperties.put(entityType, storageField);
            }
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(StorageFields.class);
    }
}
