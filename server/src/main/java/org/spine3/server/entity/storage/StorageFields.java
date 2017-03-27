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
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.storage.reflect.Column;

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

/**
 * @author Dmytro Dashenkov
 */
public class StorageFields {

    private static final String NON_PUBLIC_CLASS_WARNING =
            "Passed entity class %s is not public. Storage fields won't be extracted.";

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
    }

    public static Map<String, Column.MemoizedValue<?>> empty() {
        return Collections.emptyMap();
    }

    public static <E extends Entity<?, ?>> Map<String, Column.MemoizedValue<?>> from(E entity) {
        checkNotNull(entity);
        final Class<? extends Entity> entityType = entity.getClass();
        final int modifiers = entityType.getModifiers();
        if (!Modifier.isPublic(modifiers)) {
            log().warn(NON_PUBLIC_CLASS_WARNING);
            return Collections.emptyMap();
        }

        ensureRegistered(entityType);

        final Map<String, Column.MemoizedValue<?>> fields = getStorageFields(entityType, entity);
        return fields;
    }

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
