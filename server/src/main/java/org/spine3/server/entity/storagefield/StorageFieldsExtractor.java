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

package org.spine3.server.entity.storagefield;

import org.spine3.server.entity.Entity;
import org.spine3.server.entity.StorageFields;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmytro Dashenkov.
 */
public class StorageFieldsExtractor {

    private static final String GETTER_REGEX = "((get)|(is))[A-Z]\\w*";
    private static final Pattern GETTER_PATTERN = Pattern.compile(GETTER_REGEX);

    private static final Map<
            Class<? extends Entity<?, ?>>,
            StorageFieldsGenerator<? extends Entity<?, ?>>> fieldGenerators
            = new ConcurrentHashMap<>();

    private StorageFieldsExtractor() {
    }

    @SuppressWarnings("unchecked") // Reflection - type param conflict
    public static <E extends Entity<?, ?>> StorageFields extract(E entity) {
        checkNotNull(entity);
        final Class<E> entityClass = (Class<E>) entity.getClass();

        StorageFieldsGenerator<E> fieldsGenerator =
                (StorageFieldsGenerator<E>) fieldGenerators.get(entityClass);

        if (fieldsGenerator == null) {
            fieldsGenerator = newGenerator(entityClass);
        }
        final StorageFields fields = fieldsGenerator.apply(entity);
        return fields;
    }

    private static <E extends Entity<?, ?>> StorageFieldsGenerator<E> newGenerator(
            Class<E> entityClass) {
        final Collection<EntityFieldGetter<E>> getters = new LinkedList<>();
        final Method[] methods = entityClass.getDeclaredMethods();
        for (Method method : methods) {
            final String methodName = method.getName();
            final boolean returnTypeMatches = !method.getReturnType()
                                                     .equals(Void.TYPE);
            final boolean accessMatches = method.isAccessible();
            if (accessMatches && returnTypeMatches) {
                // Regex operations are not fast enough to check all the methods.
                // Instead, we check the method's access level and return type, and then the name.
                final boolean nameMatches = GETTER_PATTERN.matcher(methodName)
                                                          .matches();
                if (nameMatches) {
                    final EntityFieldGetter<E> getter
                            = new EntityFieldGetter<>(methodName, entityClass);
                    getters.add(getter);
                }
            }
        }
        final StorageFieldsGenerator<E> generator = new StorageFieldsGenerator<>(getters);
        fieldGenerators.put(entityClass, generator);
        return generator;
    }
}
