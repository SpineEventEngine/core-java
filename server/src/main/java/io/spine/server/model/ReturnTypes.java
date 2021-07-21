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
@SuppressWarnings("UnstableApiUsage") // `TypeToken` is marked as "beta".
@Immutable
public final class ReturnTypes {

    /** A return type of a {@code void} handler method. */
    private static final ReturnTypes VOID = new ReturnTypes(void.class);

    private final ImmutableList<TypeToken<?>> whitelist;
    private final ImmutableList<TypeToken<?>> blacklist;

    private ReturnTypes(ImmutableList<TypeToken<?>> whitelist,
                        ImmutableList<TypeToken<?>> blacklist) {
        this.whitelist = whitelist;
        this.blacklist = blacklist;
    }

    /**
     * Creates an instance for the passed type.
     */
    private ReturnTypes(Class<?> cls) {
        this(ImmutableList.of(TypeToken.of(cls)), ImmutableList.of());
    }

    /**
     * Creates an instance containing the passed types.
     */
    public ReturnTypes(TypeToken<?>... whitelist) {
        this(ImmutableList.copyOf(whitelist), ImmutableList.of());
    }

    /**
     * Obtains the instance representing the {@code void} return type.
     */
    public static ReturnTypes onlyVoid() {
        return VOID;
    }

    /**
     * Creates a new {@code ReturnTypes} but with the given black list of types.
     *
     * @param types types not to allow as the return types
     * @return new {@code ReturnTypes} with a new black list
     */
    public ReturnTypes butNot(TypeToken<?>... types) {
        checkNotNull(types);
        return new ReturnTypes(whitelist, ImmutableList.copyOf(types));
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
        if (!mayReturnIgnored && ignored(actualReturnType)) {
            return false;
        }
        boolean conformsWhitelist = whitelist.stream()
                                             .anyMatch(t -> conforms(t, actualReturnType));
        if (!conformsWhitelist) {
            return false;
        }
        boolean conformsBlacklist = blacklist.stream()
                                             .noneMatch(t -> conforms(t, actualReturnType));
        return conformsBlacklist;
    }

    private static boolean conforms(TypeToken<?> expectedType,
                                    TypeToken<?> actualType) {
        return TypeMatcher.matches(expectedType, actualType);
    }

    private static boolean ignored(TypeToken<?> actualReturnType) {
        return TypeMatcher.messagesFitting(actualReturnType)
                          .stream()
                          .anyMatch(MethodResult::isIgnored);
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
        return whitelist.equals(types1.whitelist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(whitelist);
    }
}
