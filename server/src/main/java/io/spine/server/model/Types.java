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

package io.spine.server.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.protobuf.Message;

import java.lang.reflect.TypeVariable;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A utility for working with the types used in method signatures.
 */
@SuppressWarnings("UnstableApiUsage")   // Using Guava's `TypeToken`.
final class Types {

    /** Prevents this utility class from instantiation. */
    private Types() {
    }

    /**
     * Tells whether the {@code expected} type is the same or can be reduced to {@code actual}.
     *
     * <p>E.g. {@literal Iterable<EventMessage>} can be reduced to {@code List<TaskAdded>}.
     *
     * //TODO:2019-09-26:alex.tymchenko: add better docs.
     */
    static boolean matches(TypeToken<?> expected, TypeToken<?> actual) {
        if (expected.equals(actual)) {
            return true;
        }

        Class<?> rawExpected = expected.getRawType();
        Class<?> rawActual = actual.getRawType();
        if (!rawExpected.isAssignableFrom(rawActual)) {
            return false;
        }

        TypeVariable<? extends Class<?>>[] expectedTypeParams = rawExpected.getTypeParameters();
        int expectedParamCount = expectedTypeParams.length;
        if (expectedParamCount == 0) {
            return true;
        }

        TypeVariable<? extends Class<?>>[] actualTypeParams = rawActual.getTypeParameters();

        if (expectedParamCount == 1) {
            TypeToken<?> expectedGenericType = expected.resolveType(expectedTypeParams[0]);
            return matchActualGenericsToOne(actual, expectedGenericType);
        } else if (expectedParamCount != actualTypeParams.length) {
            return false;
        } else {
            return matchOneToOne(expected, actual);
        }
    }

    /**
     * Resolves the generic parameter of the passed type into an actual value.
     *
     * <p>If the resolved parameter type is {@code Optional<T>}, resolves its parameter {@code T}
     * and returns the result. In case the {@code Optional} does not have the generic parameter,
     * returns the type of {@code Optional} itself.
     */
    private static TypeToken<?> resolve(TypeToken<?> type, TypeVariable<? extends Class<?>> param) {
        TypeToken<?> actualGenericType = type.resolveType(param);
        Class<?> rawType = actualGenericType.getRawType();
        TypeVariable<? extends Class<?>>[] parameters = rawType.getTypeParameters();
        if (Optional.class.isAssignableFrom(rawType) && parameters.length == 1) {
            actualGenericType = actualGenericType.resolveType(parameters[0]);
        }
        return actualGenericType;
    }

    /**
     * Tells whether the {@code type} is not the same nor a descendant as {@code expectedSuper}.
     */
    private static boolean differs(TypeToken<?> type, TypeToken<?> expectedSuper) {
        return !type.equals(expectedSuper) && !type.isSubtypeOf(expectedSuper);
    }

    private static boolean matchOneToOne(TypeToken<?> expected, TypeToken<?> actual) {
        TypeVariable<? extends Class<?>>[] expectedTypeParams = genericTypesOf(expected);
        TypeVariable<? extends Class<?>>[] actualTypeParams = genericTypesOf(actual);
        for (int paramIndex = 0; paramIndex < expectedTypeParams.length; paramIndex++) {
            TypeToken<?> expectedGeneric = expected.resolveType(expectedTypeParams[paramIndex]);
            TypeToken<?> actualGeneric = actual.resolveType(actualTypeParams[paramIndex]);
            if (differs(actualGeneric, expectedGeneric)) {
                return false;
            }
        }
        return true;
    }

    private static TypeVariable<? extends Class<?>>[] genericTypesOf(TypeToken<?> type) {
        return type.getRawType()
                   .getTypeParameters();
    }

    private static boolean
    matchActualGenericsToOne(TypeToken<?> whoseGenerics, TypeToken<?> expectedGenericType) {
        TypeVariable<? extends Class<?>>[] actualTypeParams = genericTypesOf(whoseGenerics);
        if (actualTypeParams.length > 0) {
            for (TypeVariable<? extends Class<?>> param : actualTypeParams) {
                TypeToken<?> actualGenericType = resolve(whoseGenerics, param);
                if (differs(actualGenericType, expectedGenericType)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns a set of {@link Message} types that are declared by the given type.
     *
     * <p>The types returned are the most narrow possible.
     *
     * <p>E.g. {@code Pair<TaskCreated, TaskAssigned>} returns {@literal Class<TaskCreated>} and
     * {@code Class<TaskAssigned>}.
     */
    static ImmutableSet<Class<? extends Message>> messagesFitting(TypeToken<?> type) {
        checkNotNull(type);
        Class<?> rawType = type.getRawType();
        TypeVariable<? extends Class<?>>[] parameters = rawType.getTypeParameters();
        if (0 == parameters.length) {
            return type.isSubtypeOf(Message.class)
                   ? ImmutableSet.of(asMessageType(rawType))
                   : ImmutableSet.of();
        }

        ImmutableSet.Builder<Class<? extends Message>> builder = ImmutableSet.builder();
        for (TypeVariable<? extends Class<?>> parameter : parameters) {
            TypeToken<?> genericType = resolve(type, parameter);
            if (genericType.isSubtypeOf(Message.class)) {
                builder.add(asMessageType(genericType.getRawType()));
            }
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")  // Previously checked {@code cls} is a {@code Message} subtype.
    private static Class<? extends Message> asMessageType(Class<?> cls) {
        return (Class<? extends Message>) cls;
    }
}
