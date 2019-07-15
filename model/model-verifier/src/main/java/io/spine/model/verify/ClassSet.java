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

package io.spine.model.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Creates a set of classes by their names using the specified {@code ClassLoader}.
 */
final class ClassSet {

    private final ClassLoader classLoader;
    private final ImmutableSet<Class<?>> elements;
    private final ImmutableList<String> notFound;

    ClassSet(ClassLoader classLoader, Iterable<String> classNames) {
        this.classLoader = classLoader;
        ImmutableSet.Builder<Class<?>> elements = ImmutableSet.builder();
        ImmutableSet.Builder<String> notFound = ImmutableSet.builder();
        for (String className : classNames) {
            Class<?> cls;
            try {
                cls = createRawClass(className);
                elements.add(cls);
            } catch (ClassNotFoundException e) {
                // Append the class name already prepared for displaying.
                notFound.add(format("`%s`", className));
            }
        }
        this.elements = elements.build();
        List<String> sorted = new ArrayList<>(notFound.build());
        sorted.sort(Ordering.natural());
        this.notFound = ImmutableList.copyOf(sorted);
    }

    private Class<?> createRawClass(String fqn) throws ClassNotFoundException {
        return Class.forName(fqn, false, classLoader);
    }

    /**
     * Obtains classes of this set.
     */
    ImmutableSet<Class<?>> elements() {
        return elements;
    }

    /**
     * Obtains the sorted list of class names that are not found in the classpath.
     */
    ImmutableList<String> notFound() {
        return notFound;
    }
}
