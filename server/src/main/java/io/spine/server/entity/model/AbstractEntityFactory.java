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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract base for entity factories.
 */
public abstract class AbstractEntityFactory<E extends Entity> implements EntityFactory<E>  {

    private static final long serialVersionUID = 0L;

    /** The class of entities this factory creates. */
    private final Class<E> entityClass;

    /** The class of entity identifiers. */
    private final Class<?> idClass;

    /**
     * The type of the first constructor parameter, if any.
     * {@code null} if the constructor does not accept parameters.
     */
    @LazyInit
    @SuppressWarnings("Immutable") // effectively
    private transient volatile @MonotonicNonNull Class<?> firstParameterType;

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

    protected AbstractEntityFactory(Class<E> entityClass) {
        checkNotNull(entityClass);
        this.idClass = EntityClass.idClass(entityClass);
        this.entityClass = entityClass;
    }

    protected final Class<E> entityClass() {
        return entityClass;
    }

    protected final Class<?> idClass() {
        return idClass;
    }

    @Override
    @SuppressWarnings("SynchronizeOnThis") // Double-check idiom for lazy init.
    public final Constructor<E> constructor() {
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
     * Obtains the type of the first constructor parameter, or {@code null} if the entity
     * has the default constructor.
     */
    protected final @Nullable Class<?> firstParameterType() {
        return firstParameterType;
    }

    /**
     * Obtains the constructor for the entity class.
     *
     * @throws ModelError if the entity class does not have the required constructor
     */
    protected abstract Constructor<E> findConstructor();

    @Override
    public int hashCode() {
        return Objects.hash(idClass, entityClass);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AbstractEntityFactory)) {
            return false;
        }
        final AbstractEntityFactory other = (AbstractEntityFactory) obj;
        return Objects.equals(this.idClass, other.idClass)
                && Objects.equals(this.entityClass, other.entityClass);
    }
}
