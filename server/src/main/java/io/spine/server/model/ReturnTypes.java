/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;

import javax.annotation.concurrent.Immutable;
import java.lang.reflect.Method;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Specifies types of values a handler methods may return.
 */
@Immutable
public final class ReturnTypes {

    /** A return type of a {@code void} handler method. */
    private static final ReturnTypes VOID = new ReturnTypes(void.class);

    private final ImmutableList<TypeToken<?>> types;

    /**
     * Creates an instance for the passed type.
     */
    private ReturnTypes(Class<?> cls) {
        checkNotNull(cls);
        this.types = ImmutableList.of(TypeToken.of(cls));
    }

    /**
     * Creates an instance containing the passed types.
     */
    public ReturnTypes(TypeToken<?>... types) {
        checkNotNull(types);
        this.types = ImmutableList.copyOf(types);
    }

    /**
     * Verifies if the return type of the passed method matches this instance.
     *
     * @param method
     *         the method to check
     * @param mayReturnIgnored
     *         is {@code true} if the method can return {@link Nothing}
     */
    boolean matches(Method method, boolean mayReturnIgnored) {
        TypeToken<?> actualReturnType = Invokable.from(method).getReturnType();
        boolean conforms =
                types.stream()
                     .anyMatch(type -> conforms(type, actualReturnType, mayReturnIgnored));
        return conforms;
    }

    /**
     * Obtains the instance representing the {@code void} return type.
     */
    public static ReturnTypes onlyVoid() {
        return VOID;
    }

    @SuppressWarnings("UnstableApiUsage") // Using Guava's `TypeToken`.
    private static boolean conforms(TypeToken<?> returnType,
                                    TypeToken<?> actualReturnType,
                                    boolean mayReturnIgnored) {
        boolean typeMatches = TypeMatcher.matches(returnType, actualReturnType);
        boolean isNotIgnored =
                mayReturnIgnored
                        || TypeMatcher.messagesFitting(actualReturnType)
                                      .stream()
                                      .noneMatch(MethodResult::isIgnored);
        return typeMatches && isNotIgnored;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReturnTypes types1 = (ReturnTypes) o;
        return types.equals(types1.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(types);
    }
}
