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
import io.spine.server.entity.EntityFactory;
import io.spine.server.model.ModelError;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * Default implementation of entity factory which creates entities by invoking constructor
 * which accepts entity ID.
 */
public class DefaultEntityFactory<E extends Entity> implements EntityFactory<E> {

    private static final long serialVersionUID = 0L;
    private final Class<?> idClass;
    private final Class<E> entityClass;

    /**
     * The constructor for entities of this class.
     *
     * <p>If the entity class has multiple constructors, this one points to the one which
     * accepts one parameter.
     *
     * @see #constructor()
     */
    @LazyInit
    @SuppressWarnings("Immutable") // effectively
    private transient volatile @MonotonicNonNull Constructor<E> constructor;

    /**
     * The type of the first constructor parameter, if any.
     * {@code null} if the constructor does not accept parameters.
     */
    @LazyInit
    @SuppressWarnings("Immutable") // effectively
    private transient volatile @MonotonicNonNull Class<?> firstParameterType;

    protected DefaultEntityFactory(Class<?> idClass, Class<E> entityClass) {
        this.idClass = checkNotNull(idClass);
        this.entityClass = checkNotNull(entityClass);
    }

    @Override
    public E create(Object constructionArgument) {
        Constructor<E> ctor = constructor();
        //TODO:2019-03-10:alexander.yevsyukov: Have d-tour if we have parameterless ctor.
        checkArgumentMatches(constructionArgument);
        try {
            E result = ctor.newInstance(constructionArgument);
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Class<E> entityClass() {
        return entityClass;
    }

    @Override
    @SuppressWarnings("SynchronizeOnThis") // Double-check idiom for lazy init.
    public Constructor<E> constructor() {
        Constructor<E> result = constructor;
        if (result == null) {
            synchronized (this) {
                result = constructor;
                if (result == null) {
                    constructor = findConstructor();
                    result = constructor;
                    Class<?>[] parameterTypes = result.getParameterTypes();
                    firstParameterType =
                            (parameterTypes.length > 0)
                            ? parameterTypes[0]
                            : null;
                }
            }
        }
        return result;
    }

    /**
     * Obtains the constructor for the passed entity class.
     *
     * <p>The entity class must have a constructor with the single parameter of type defined by
     * generic type {@code <I>}.
     *
     * @throws IllegalStateException if the entity class does not have the required constructor
     */
    protected Constructor<E> findConstructor() {
        Constructor<E> result;
        try {
            result = entityClass.getDeclaredConstructor(idClass);
            result.setAccessible(true);
        } catch (NoSuchMethodException ignored) {
            throw noSuchConstructor(entityClass.getName(), idClass.getName());
        }
        return result;
    }

    private void checkArgumentMatches(Object argument) {
        checkState(firstParameterType != null,
                   "The entity class `%s` does not have a constructor which accepts one parameter.",
                   entityClass.getName());
        Class<?> actualArgumentType = argument.getClass();
        String errorMessage =
                "Constructor argument type mismatch: expected `%s`, but was `%s`." +
                " Check for message routing mistakes.";
        checkArgument(firstParameterType.isAssignableFrom(actualArgumentType),
                      errorMessage,
                      actualArgumentType.getName(),
                      firstParameterType.getName());
    }

    private static ModelError noSuchConstructor(String entityClass, String idClass) {
        String errMsg = format(
                "%s class must declare a constructor with a single %s ID parameter.",
                entityClass, idClass
        );
        return new ModelError(new NoSuchMethodException(errMsg));
    }

    @Override
    public int hashCode() {
        return Objects.hash(idClass, entityClass);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DefaultEntityFactory)) {
            return false;
        }
        final DefaultEntityFactory other = (DefaultEntityFactory) obj;
        return Objects.equals(this.idClass, other.idClass)
                && Objects.equals(this.entityClass, other.entityClass);
    }
}
