/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.model;

import com.google.common.base.Splitter;
import kotlin.Metadata;
import kotlin.jvm.internal.Reflection;
import kotlin.reflect.KCallable;
import kotlin.reflect.KClass;
import kotlin.reflect.KParameter;
import kotlin.reflect.KType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * A utility for working with Java class reflective representations.
 */
final class J2Kt {

    /**
     * Prevents the utility class instantiation.
     */
    private J2Kt() {
    }

    static Optional<KCallable<?>> findKotlinMethod(Method javaMethod) {
        Class<?> javaClass = javaMethod.getDeclaringClass();
        if (!isKotlin(javaClass)) {
            return Optional.empty();
        }
        KClass<?> kotlinClass = Reflection.getOrCreateKotlinClass(javaClass);
        Optional<KCallable<?>> result = kotlinClass.getMembers()
                                                   .stream()
                                                   .filter(new SameMethod(javaMethod))
                                                   .findFirst();
        return result;
    }

    /**
     * Checks if this class is compiled from Kotlin source.
     *
     * <p>Relies on the {@code kotlin.Metadata} annotation to be present on the class.
     *
     * @return {@code true} if the given class is annotated with the {@code kotlin.Metadata}
     *          annotation, {@code false} otherwise
     */
    private static boolean isKotlin(Class<?> cls) {
        return cls.isAnnotationPresent(Metadata.class);
    }

    /**
     * A predicate which compares a Kotlin {@code KCallable} to a Java {@code Method} and checks if
     * they represent the same method or not.
     */
    private static final class SameMethod implements Predicate<KCallable<?>> {

        private static final String KOTLIN_NAME_MODULE_SEPARATOR = "$";
        private static final Splitter nameSplitter = Splitter.on(KOTLIN_NAME_MODULE_SEPARATOR);

        private final String name;
        private final List<KType> javaParamTypes;

        private SameMethod(Method javaMethod) {
            this.name = deObscureName(javaMethod.getName());
            this.javaParamTypes = stream(javaMethod.getParameterTypes())
                    .map(Reflection::typeOf)
                    .collect(toList());
        }

        @Override
        public boolean test(KCallable<?> method) {
            return name.equals(method.getName()) && paramTypes(method).equals(javaParamTypes);
        }

        private static List<KType> paramTypes(KCallable<?> method) {
            return method.getParameters()
                         .stream()
                         .skip(1) // `this` instance as the first parameter.
                         .map(KParameter::getType)
                         .collect(toList());
        }

        private static String deObscureName(String name) {
            if (!name.contains(KOTLIN_NAME_MODULE_SEPARATOR)) {
                return name;
            }
            return nameSplitter.splitToList(name).get(0);
        }
    }
}
