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

import com.google.common.collect.ImmutableSet;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.gson.internal.Primitives.wrap;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

/**
 * Utilities for working with methods.
 */
final class Methods {

    /**
     * One of the possible getter prefixes - {@code get-}.
     */
    private static final String GET_PREFIX = "get";

    /**
     * The other possible getter prefix - {@code is-}.
     *
     * <p>Allowed for {@code boolean} and {@link Boolean} entity columns.
     *
     * <p>Package-private access to enable the common usage of this prefix in the column
     * lookup code.
     * @see ColumnReader
     */
    static final String IS_PREFIX = "is";

    private static final String GETTER_PREFIX_REGEX = '(' + GET_PREFIX + ")|(" + IS_PREFIX + ')';
    private static final Pattern GETTER_PREFIX_PATTERN = Pattern.compile(GETTER_PREFIX_REGEX);
    private static final ImmutableSet<String> NULLABLE_ANNOTATION_SIMPLE_NAMES =
            ImmutableSet.of("CheckForNull", "Nullable", "NullableDecl", "NullableType");

    /** Prevents instantiation of this utility class. */
    private Methods() {
    }

    static Method retrieveAnnotatedVersion(Method getter) {
        Optional<Method> optionalMethod = getAnnotatedVersion(getter);
        if (!optionalMethod.isPresent()) {
            throw newIllegalStateException("Method `%s` is not an entity column getter.", getter);
        }
        return optionalMethod.get();
    }

    static Optional<String> nameFromAnnotation(Method getter) {
        String trimmedName = getter.getAnnotation(Column.class)
                                   .name()
                                   .trim();
        return trimmedName.isEmpty()
               ? Optional.empty()
               : Optional.of(trimmedName);
    }

    static String nameFromGetter(Method getter) {
        Matcher prefixMatcher = GETTER_PREFIX_PATTERN.matcher(getter.getName());
        String nameWithoutPrefix = prefixMatcher.replaceFirst("");
        String result =
                Character.toLowerCase(nameWithoutPrefix.charAt(0)) + nameWithoutPrefix.substring(1);
        return result;
    }

    static boolean mayReturnNull(Method getter) {
        AnnotatedType type = getter.getAnnotatedReturnType();
        boolean result = isNullable(type.getAnnotations());
        return result;
    }

    private static boolean isNullable(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            String simpleName = annotation.annotationType()
                                          .getSimpleName();
            if (NULLABLE_ANNOTATION_SIMPLE_NAMES.contains(simpleName)) {
                return true;
            }
        }
        return false;
    }

    static void checkGetter(Method method) {
        checkNotNull(method);
        boolean prefixFound = GETTER_PREFIX_PATTERN.matcher(method.getName())
                                                   .find();
        checkArgument(prefixFound,
                      "The method `%s` is not a getter:" +
                              " should have `get` or `is` prefix in the name.");
        int paramCount = method.getParameterTypes().length;
        checkArgument(paramCount == 0,
                      "The method `%s` is not a getter: accepts %s parameters instead of zero.",
                      method, paramCount);
        Class<?> returnType = method.getReturnType();
        checkArgument(!method.getName().startsWith(IS_PREFIX)
                              || boolean.class.isAssignableFrom(returnType)
                              || Boolean.class.isAssignableFrom(returnType),
                      "Getter with an `is` prefix should have `boolean` or `Boolean` return type.");
        checkArgument(getAnnotatedVersion(method).isPresent(),
                      "Entity column getter should be annotated with `%s`.",
                      Column.class.getName());
        int modifiers = method.getModifiers();
        checkArgument(isPublic(modifiers) && !isStatic(modifiers),
                      "Entity column getter should be public instance method.");
        Class<?> wrapped = wrap(returnType);
        checkArgument(Serializable.class.isAssignableFrom(wrapped),
                      "Cannot create column of non-serializable type %s by method %s.",
                      returnType,
                      method);
    }

    /**
     * Obtains the method version annotated with {@link Column} for the specified method.
     *
     * <p>Scans the specified method, the methods with the same signature
     * from the super classes and interfaces.
     *
     * @param method the method to find the annotated version
     * @return the annotated version of the specified method
     * @throws IllegalStateException if there is more than one annotated method is found
     *                               in the scanned classes
     */
    static Optional<Method> getAnnotatedVersion(Method method) {
        Set<Method> annotatedVersions = newHashSet();
        if (method.isAnnotationPresent(Column.class)) {
            annotatedVersions.add(method);
        }
        Class<?> declaringClass = method.getDeclaringClass();
        Iterable<Class<?>> ascendants = getSuperClassesAndInterfaces(declaringClass);
        for (Class<?> ascendant : ascendants) {
            Optional<Method> optionalMethod = getMethodBySignature(ascendant, method);
            if (optionalMethod.isPresent()) {
                Method ascendantMethod = optionalMethod.get();
                if (ascendantMethod.isAnnotationPresent(Column.class)) {
                    annotatedVersions.add(ascendantMethod);
                }
            }
        }

        checkState(annotatedVersions.size() <= 1,
                   "An entity column getter should be annotated only once. " +
                           "Found the annotated versions: %s.", annotatedVersions);

        if (annotatedVersions.isEmpty()) {
            return Optional.empty();
        }

        Method annotatedVersion = annotatedVersions.iterator()
                                                   .next();
        return Optional.of(annotatedVersion);
    }

    private static Iterable<Class<?>> getSuperClassesAndInterfaces(Class<?> cls) {
        Collection<Class<?>> interfaces = Arrays.asList(cls.getInterfaces());
        Collection<Class<?>> result = newHashSet(interfaces);
        Class<?> currentSuper = cls.getSuperclass();
        while (currentSuper != null) {
            result.add(currentSuper);
            currentSuper = currentSuper.getSuperclass();
        }
        return result;
    }

    /**
     * Obtains the method from the specified class with the same signature as the specified method.
     *
     * @param target the class to obtain the method with the signature
     * @param method the method to get the signature
     * @return the method with the same signature obtained from the specified class
     */
    private static Optional<Method> getMethodBySignature(Class<?> target, Method method) {
        checkArgument(!method.getDeclaringClass()
                             .equals(target));
        try {
            Method methodFromTarget = target.getMethod(method.getName(),
                                                       method.getParameterTypes());
            return Optional.of(methodFromTarget);
        } catch (NoSuchMethodException ignored) {
            return Optional.empty();
        }
    }
}
