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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.StorageFields;
import org.spine3.server.reflect.Getter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.server.entity.storage.EntityFields.toStorageFieldType;

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
 * @author Dmytro Dashenkov
 */
public class StorageFieldsExtractor {

    /**
     * A regular expression matching strings which correspond to a JavaBean specification for
     * getters: start with either {@code get} or {@code is} prefix followed by an upper case English
     * letter and by a number of other "word" symbols.
     */
    private static final String GETTER_REGEX = "((get)|(is))[A-Z]\\w*";
    private static final Pattern GETTER_PATTERN = Pattern.compile(GETTER_REGEX);

    private static final String NON_PUBLIC_CLASS_WARNING = "Passed entity class %s is not public. Storage fields won't be extracted.";

    private static final ImmutableSet<String> EXCLUDED_METHODS =
            ImmutableSet.<String>builder()
                        .add("getId")
                        .add("getState")
                        .add("getLifecycleFlags")
                        .add("getDefaultState")
                        .add("getBuilder")
                        .build();
    private static final Map<Class<? extends Entity<?, ?>>, EntityFields>
            fieldGenerators = new ConcurrentHashMap<>();

    private StorageFieldsExtractor() {
    }

    /**
     * Extracts {@link StorageFields} from the given {@link Entity}.
     *
     * <p>The fields cannot be extracted if the entity type definition is not {@code public}.
     *
     * @param entity the {@link Entity} to get the field values from
     * @param <E>    the type of the {@link Entity}
     * @return an object enclosing the storage fields or
     * {@linkplain Optional#absent() Optional.absent()} if the entity class is not {@code public}
     */
    @SuppressWarnings("unchecked") // Reflection - type param conflict
    public static <E extends Entity<?, ?>> Optional<StorageFields> extract(E entity) {
        checkNotNull(entity);
        final Class<? extends E> entityClass = (Class<E>) entity.getClass();

        EntityFields fieldsGenerator = fieldGenerators.get(entityClass);

        if (fieldsGenerator == null) {
            if (!isClassPublic(entityClass)) {
                log().warn(NON_PUBLIC_CLASS_WARNING);
                return Optional.absent();
            }
            fieldsGenerator = newGenerator(entityClass);
        }
        final StorageFields fields = fieldsGenerator.toStorageFields(entity);
        return Optional.of(fields);
    }

    private static EntityFields newGenerator(
            Class<? extends Entity<?, ?>> entityClass) {
        final Collection<Getter> getters = collectGetters(entityClass);
        final EntityFields generator = new EntityFields(getters);
        fieldGenerators.put(entityClass, generator);
        return generator;
    }

    private static <E extends Entity<?, ?>> Collection<Getter> collectGetters(
            Class<E> entityClass) {
        final Collection<Getter> getters = new LinkedList<>();

        final Method[] publicMethods = entityClass.getMethods();
        for (Method method : publicMethods) {
            final String methodName = method.getName();
            final Class returnType = method.getReturnType();
            final boolean returnTypeMatches = toStorageFieldType(returnType).isPresent();
            final boolean argumentsMatch = method.getParameterTypes().length == 0;
            final boolean isNotExclusion = !EXCLUDED_METHODS.contains(methodName);
            final int modifiers = method.getModifiers();
            final boolean instanceMethod = !Modifier.isStatic(modifiers);
            if (returnTypeMatches
                && argumentsMatch
                && isNotExclusion
                && instanceMethod) {
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

    private static boolean isClassPublic(Class<? extends Entity<?, ?>> cls) {
        final int modifiers = cls.getModifiers();
        final boolean isPublic = Modifier.isPublic(modifiers);
        return isPublic;
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(StorageFieldsExtractor.class);
    }
}
