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
 * A utility for extracting the {@link StorageFields} from an {@link Entity}.
 *
 * <p>All the values are retrieved from the Entity getters.
 *
 * <p>A getter may have {@link javax.annotation.Nullable {@literal @}javax.annotation.Nullable}
 * annotation to show that the field may accept {@code null}s.
 * By default all the Storage Fields are non-null.
 *
 * <p>Supported field types are:
 * <ul>
 *     <li>{@link com.google.protobuf.Message Message}
 *     <li>{@link Integer}
 *     <li>{@link Long}
 *     <li>{@link String}
 *     <li>{@link Boolean}
 *     <li>{@link Float}
 *     <li>{@link Double}
 * </ul>
 *
 * <p>The java primitives are boxed into their wrapper classes.
 *
 * <p>To read the resulting {@link StorageFields} object use {@link StorageFieldTraverser}.
 *
 * @author Dmytro Dashenkov.
 */
public class StorageFieldsExtractor {

    /**
     * A regular expression matching strings which correspond to a JavaBean specification for
     * getters: start with either {@code get} or {@code is} prefix followed by an upper case English
     * letter and by a number of other "word" symbols.
     */
    private static final String GETTER_REGEX = "((get)|(is))[A-Z]\\w*";
    private static final Pattern GETTER_PATTERN = Pattern.compile(GETTER_REGEX);

    private static final Map<Class<? extends Entity<?, ?>>, StorageFieldsDecomposer>
            fieldGenerators = new ConcurrentHashMap<>();

    private StorageFieldsExtractor() {
    }

    /**
     * Extracts {@link StorageFields} from the given {@link Entity}.
     *
     * @param entity the {@link Entity} to get the field values from
     * @param <E>    the type of the {@link Entity}
     * @return an object enclosing the storage fields
     */
    @SuppressWarnings("unchecked") // Reflection - type param conflict
    public static <E extends Entity<?, ?>> StorageFields extract(E entity) {
        checkNotNull(entity);
        final Class<? extends E> entityClass = (Class<E>) entity.getClass();

        StorageFieldsDecomposer fieldsGenerator = fieldGenerators.get(entityClass);

        if (fieldsGenerator == null) {
            fieldsGenerator = newGenerator(entityClass);
        }
        final StorageFields fields = fieldsGenerator.parse(entity);
        return fields;
    }

    private static StorageFieldsDecomposer newGenerator(
            Class<? extends Entity<?, ?>> entityClass) {
        final Collection<Getter> getters = collectGetters(entityClass);
        final StorageFieldsDecomposer generator = new StorageFieldsDecomposer(getters);
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
                    final Getter getter = new Getter(method);
                    getters.add(getter);
                }
            }
        }

        return getters;
    }
}
