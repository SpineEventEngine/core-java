/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.model.declare;

import com.google.common.base.Joiner;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isProtected;
import static java.lang.reflect.Modifier.isPublic;

/**
 * @author Alex Tymchenko
 */
public class AccessModifier implements Predicate<Method> {

    public static final AccessModifier PUBLIC =
            new AccessModifier(Modifier::isPublic, "public");

    public static final AccessModifier PROTECTED =
            new AccessModifier(Modifier::isProtected, "protected");


    public static final AccessModifier PACKAGE_PRIVATE =
            new AccessModifier(
                    methodModifier -> !(isPublic(methodModifier)
                            || isProtected(methodModifier)
                            || isPrivate(methodModifier)), "package-private");

    public static final AccessModifier PRIVATE =
            new AccessModifier(Modifier::isProtected, "private");


    private final Predicate<Integer> checkingMethod;

    private final String name;

    public AccessModifier(Predicate<Integer> checkingMethod, String name) {
        this.checkingMethod = checkingMethod;
        this.name = name;
    }

    static Object asString(Iterable<AccessModifier> modifiers) {
        return Joiner.on(", ")
                     .join(modifiers);
    }

    @Override
    public boolean test(Method method) {
        return checkingMethod.test(method.getModifiers());
    }

    @Override
    public String toString() {
        return name;
    }
}
