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
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.server.entity.Entity;
import io.spine.server.model.ModelClass;
import io.spine.server.model.ModelError;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.Messages.defaultInstance;
import static java.lang.String.format;

/**
 * A class of entities.
 *
 * @param <E> the type of entities
 */
@SuppressWarnings("SynchronizeOnThis") // Double-check idiom for lazy init.
public class EntityClass<E extends Entity> extends ModelClass<E> {

    private static final long serialVersionUID = 0L;

    /** The class of entity IDs. */
    private final Class<?> idClass;

    /** The class of the entity state. */
    private final Class<? extends Message> stateClass;

    /** The default state of entities of this class. */
    @LazyInit
    private transient volatile @MonotonicNonNull Message defaultState;

    /** Type of the entity state. */
    private final TypeUrl entityStateType;

    /** The constructor for entities of this class. */
    @LazyInit
    @SuppressWarnings("Immutable") // effectively
    private transient volatile @MonotonicNonNull Constructor<E> entityConstructor;

    /** Creates new instance of the model class for the passed class of entities. */
    protected EntityClass(Class<E> cls) {
        super(cls);
        checkNotNull((Class<? extends Entity>) cls);
        this.idClass = idClass(cls);
        this.stateClass = stateClass(cls);
        this.entityStateType = TypeUrl.of(stateClass);
    }

    /**
     * Obtains an entity class for the passed raw class.
     */
    public static <E extends Entity> EntityClass<E> asEntityClass(Class<E> cls) {
        checkNotNull(cls);
        EntityClass<E> result = (EntityClass<E>)
                get(cls, EntityClass.class, () -> new EntityClass<>(cls));
        return result;
    }

    /**
     * Creates new entity.
     */
    public E createEntity(Object id) {
        checkNotNull(id);
        Constructor<E> ctor = constructor();
        checkArgumentMatches(ctor, id);
        try {
            E result = ctor.newInstance(id);
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void checkArgumentMatches(Constructor<?> ctor, Object argument) {
        Class<?> actualArgumentType = argument.getClass();
        Class<?> firstParameter = ctor.getParameterTypes()[0];
        String errorMessage =
                "Constructor argument type mismatch: expected `%s`, but was `%s`. " +
                        "Check for message routing mistakes.";
        checkArgument(firstParameter.isAssignableFrom(actualArgumentType),
                      errorMessage,
                      actualArgumentType.getName(),
                      firstParameter.getName());
    }

    private static ModelError noSuchConstructor(String entityClass, String idClass) {
        String errMsg = format(
                "%s class must declare a constructor with a single %s ID parameter.",
                entityClass, idClass
        );
        return new ModelError(new NoSuchMethodException(errMsg));
    }

    /**
     * Obtains the class of identifiers from the passed entity class.
     *
     * <p>Checks that this class of identifiers obtained from the passed entity class
     * is supported by the framework.
     *
     * <p>The type of entity identifiers ({@code <I>}) cannot be bound because
     * it can be {@code Long}, {@code String}, {@code Integer}, and class implementing
     * {@code Message}.
     *
     * <p>We perform the check to to detect possible programming error
     * in declarations of entity and repository classes <em>until</em> we have
     * compile-time model check.
     *
     * @throws ModelError if unsupported ID class passed
     */
    private static <I> Class<I> idClass(Class<? extends Entity> cls) {
        @SuppressWarnings("unchecked") // The type is preserved by the Entity type declaration.
        Class<I> idClass = (Class<I>) Entity.GenericParameter.ID.argumentIn(cls);
        try {
            Identifier.checkSupported(idClass);
        } catch (IllegalArgumentException e) {
            throw new ModelError(e);
        }
        return idClass;
    }

    /**
     * Retrieves the state class of the passed entity class.
     *
     * <p>Though this method is {@code public}, it is <em>not</em> considered a part of the
     * public API. It is used internally by other framework routines and not designed for efficient
     * execution by Spine users.
     */
    private static <S extends Message> Class<S> stateClass(Class<? extends Entity> entityClass) {
        @SuppressWarnings("unchecked") // The type is preserved by the Entity type declaration.
        Class<S> result = (Class<S>) Entity.GenericParameter.STATE.argumentIn(entityClass);
        return result;
    }

    /**
     * Obtains the default state for this class of entities.
     */
    public Message defaultState() {
        Message result = defaultState;
        if (result == null) {
            synchronized (this) {
                result = defaultState;
                if (result == null) {
                    Class<? extends Message> stateClass = stateClass();
                    defaultState = defaultInstance(stateClass);
                    result = defaultState;
                }
            }
        }
        return result;
    }

    /**
     * Obtains constructor for the entities of this class.
     */
    public Constructor<E> constructor() {
        Constructor<E> result = entityConstructor;
        if (result == null) {
            synchronized (this) {
                result = entityConstructor;
                if (result == null) {
                    entityConstructor = findConstructor();
                    result = entityConstructor;
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
    @SuppressWarnings("unchecked" /* The cast is protected by generic params of this class. */)
    protected Constructor<E> findConstructor() {
        Class<? extends E> entityClass = value();
        Class<?> idClass = idClass();
        Constructor<E> result;
        try {
            result = (Constructor<E>) entityClass.getDeclaredConstructor(idClass);
            result.setAccessible(true);
        } catch (NoSuchMethodException ignored) {
            throw noSuchConstructor(entityClass.getName(), idClass.getName());
        }
        return result;
    }

    /**
     * Obtains the class of IDs used by the entities of this class.
     */
    public final Class<?> idClass() {
        return idClass;
    }

    /**
     * Obtains the class of the state of entities of this class.
     */
    public final Class<? extends Message> stateClass() {
        return stateClass;
    }

    /**
     * Obtains type URL of the state of entities of this class.
     */
    public final TypeUrl stateType() {
        return entityStateType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityClass)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        EntityClass<?> that = (EntityClass<?>) o;
        return Objects.equals(idClass, that.idClass) &&
                Objects.equals(stateClass, that.stateClass) &&
                Objects.equals(entityStateType, that.entityStateType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), idClass, stateClass, entityStateType);
    }
}
