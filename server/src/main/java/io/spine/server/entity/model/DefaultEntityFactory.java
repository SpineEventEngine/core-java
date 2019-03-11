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

package io.spine.server.entity.model;

import com.google.errorprone.annotations.concurrent.LazyInit;
import io.spine.server.entity.Entity;
import io.spine.server.model.ModelError;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Default implementation of entity factory which creates entities by invoking constructor
 * which accepts entity ID.
 */
final class DefaultEntityFactory<E extends Entity> extends AbstractEntityFactory<E> {

    private static final long serialVersionUID = 0L;

    /**
     * The method which the factory would use for setting an entity ID, if the entity class
     * does not have a constructor which accepts the ID.
     */
    private static final String SET_ID_METHOD_NAME = "setId";

    /**
     * Diagnostics message suffix for wrong type of an entity identifier.
     */
    private static final String ADVISE_CHECK_ROUTING = " Check for message routing mistakes.";

    /**
     * The method of setting entity identifier. Is not {@code null} if the entity class does
     * not declare a constructor which accepts the single parameter.g
     */
    @LazyInit
    private transient volatile @MonotonicNonNull Method setIdMethod;

    DefaultEntityFactory(Class<?> idClass, Class<E> entityClass) {
        super(idClass, entityClass);
    }

    @Override
    public E create(Object id) {
        checkArgumentMatches(id);
        E result = doCreate(id);
        return result;
    }

    private E doCreate(Object id) {
        Constructor<E> ctor = constructor();
        try {
            E result;
            if (!usesDefaultConstructor()) {
                result = ctor.newInstance(id);
            } else {
                result = ctor.newInstance();
                setIdMethod.invoke(result, id);
            }
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected Constructor<E> findConstructor() {
        Constructor<E> result;
        Constructor<?>[] constructors = entityClass().getDeclaredConstructors();
        result = idConstructorIn(constructors);

        if (result == null) {
            result = defaultConstructorIn(constructors);
        }

        if (result == null) {
            throw new ModelError(
                "The entity class `%s` must have either a constructor which accepts `%s` as a" +
                " single parameter or default constructor and `%s()` method.",
                entityClass(), idClass(), SET_ID_METHOD_NAME
            );
        }

        result.setAccessible(true);
        return result;
    }

    private @Nullable Constructor<E> idConstructorIn(Constructor<?>[] constructors) {
        @SuppressWarnings("unchecked") // ensured by the generic parameter of this class.
                Constructor<E> result = (Constructor<E>)
                Arrays.stream(constructors)
                      .filter(c -> {
                          Class<?>[] parameterTypes = c.getParameterTypes();
                          return parameterTypes.length == 1
                                  && idClass().equals(parameterTypes[0]);
                      })
                      .findAny()
                      .orElse(null);
        return result;
    }

    private @Nullable Constructor<E> defaultConstructorIn(Constructor<?>[] constructors) {
        Optional<Constructor<?>> defaultCtor =
                Arrays.stream(constructors)
                      .filter(c -> c.getParameterTypes().length == 0)
                      .findAny();

        if (defaultCtor.isPresent()) {
            @SuppressWarnings("unchecked") // ensured by the generic parameter of this class.
            Constructor<E> result = (Constructor<E>) defaultCtor.get();
            Class<?> idClass = idClass();
            Class<E> entityClass = entityClass();
            try {
                setIdMethod = entityClass.getDeclaredMethod(SET_ID_METHOD_NAME, idClass);
            } catch (NoSuchMethodException e) {
                throw (ModelError) new ModelError(
                        "The entity class `%s` does not have a constructor which accepts the ID" +
                                " class `%s` as s single parameter." +
                                " The entity class has the default constructor." +
                                " Please provide the `%s()` method.",
                        entityClass, idClass, SET_ID_METHOD_NAME
                ).initCause(e);
            }
            return result;
        }
        return null;
    }

    private void checkArgumentMatches(Object argument) {
        // Ensure we have the constructor, and if it parameterized, it's first parameter type.
        // If not, we must have the method for setting ID.
        checkNotNull(constructor());

        Class<?> actualArgumentType = argument.getClass();

        Class<?> idClass = idClass();
        if (usesDefaultConstructor()) {
            checkArgument(
                    idClass.isAssignableFrom(actualArgumentType),
                    "`%s()` argument type mismatch: expected `%s`, but was: `%s`.",
                    SET_ID_METHOD_NAME, idClass, actualArgumentType
            );
            return;
        }

        Class<?> firstParamType = firstParameterType();
        checkState(firstParamType != null,
                   "The entity class `%s` does not have a constructor which" +
                           " accepts ID parameter of type `%s`.",
                   entityClass().getName(), idClass.getName());
        String errorMessage =
                "Argument type mismatch: expected `%s`, but was `%s`." +
                        ADVISE_CHECK_ROUTING;
        checkArgument(firstParamType.isAssignableFrom(actualArgumentType),
                      errorMessage,
                      actualArgumentType.getName(),
                      firstParamType.getName());
    }

    private boolean usesDefaultConstructor() {
        return setIdMethod != null;
    }
}
