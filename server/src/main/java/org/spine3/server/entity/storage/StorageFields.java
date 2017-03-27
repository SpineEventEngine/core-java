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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmytro Dashenkov
 */
public class StorageFields {

    private static final String GETTER_REGEX = "((get)|(is))[A-Z]\\w*";
    private static final Pattern GETTER_PATTERN = Pattern.compile(GETTER_REGEX);

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

    // TODO:2017-03-22:dmytro.dashenkov: Check if this register should be synchronized.
    private static final Multimap<Class<? extends Entity>, Column<?>> knownEntityProperties =
            LinkedListMultimap.create();

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
        final Method[] publicMethods = entityType.getMethods();

        for (Method candidate : publicMethods) {
            final String methodName = candidate.getName();
            final boolean argumentsMatch = candidate.getParameterTypes().length == 0;
            final boolean isNotExclusion = !EXCLUDED_METHODS.contains(methodName);
            final int modifiers = candidate.getModifiers();
            final boolean instanceMethod = !Modifier.isStatic(modifiers);
            final boolean isNotVoid = !candidate.getReturnType()
                                                .equals(Void.TYPE)
                                      && !candidate.getReturnType()
                                                   .equals(Void.class);
            if (argumentsMatch
                && isNotExclusion
                && instanceMethod
                && isNotVoid) {
                // Regex operations are not fast enough to check all the methods.
                // That's why we check the Method object fields first
                final boolean nameMatches = GETTER_PATTERN.matcher(methodName)
                                                          .matches();
                if (nameMatches) {
                    final Column<?> storageField = Column.from(candidate);
                    knownEntityProperties.put(entityType, storageField);
                }
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
