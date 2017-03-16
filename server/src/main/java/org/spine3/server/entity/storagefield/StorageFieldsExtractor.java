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
import org.spine3.server.reflect.Getter;

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

    private static final Map<Class<? extends Entity<?, ?>>, StorageFieldsWriter>
            fieldGenerators = new ConcurrentHashMap<>();

    private StorageFieldsExtractor() {
    }

    @SuppressWarnings("unchecked") // Reflection - type param conflict
    public static <E extends Entity<?, ?>> StorageFields extract(E entity) {
        checkNotNull(entity);
        final Class<E> entityClass = (Class<E>) entity.getClass();

        StorageFieldsWriter fieldsGenerator = fieldGenerators.get(entityClass);

        if (fieldsGenerator == null) {
            fieldsGenerator = newGenerator(entityClass);
        }
        final StorageFields fields = fieldsGenerator.generate(entity);
        return fields;
    }

    private static StorageFieldsWriter newGenerator(
            Class<? extends Entity<?, ?>> entityClass) {
        final Collection<Getter> getters = collectGetters(entityClass);
        final StorageFieldsWriter generator = new StorageFieldsWriter(getters);
        fieldGenerators.put(entityClass, generator);
        return generator;
    }

    private static <E extends Entity<?, ?>> Collection<Getter> collectGetters(
            Class<E> entityClass) {
        final Collection<Getter> getters = new LinkedList<>();

        final Method[] publicMethods = entityClass.getMethods();
        for (Method method : publicMethods) {
            final String methodName = method.getName();
            final boolean returnTypeMatches = !void.class.equals(method.getReturnType());
            final boolean argumentsMatch = method.getParameterTypes().length == 0;
            if (returnTypeMatches && argumentsMatch) {
                // Regex operations are not fast enough to check all the methods.
                // That's wht we check the Method object fields first
                final boolean nameMatches = GETTER_PATTERN.matcher(methodName)
                                                          .matches();
                if (nameMatches) {
                    final Getter getter = new Getter(methodName, method);
                    getters.add(getter);
                }
            }
        }

        return getters;
    }
}
