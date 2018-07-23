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

package io.spine.server.entity.model;

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.protobuf.Messages;
import io.spine.server.entity.Entity;
import io.spine.server.model.ModelClass;
import io.spine.server.model.ModelError;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * A class of entities.
 *
 * @param <E> the type of entities
 * @author Alexander Yevsyukov
 */
public class EntityClass<E extends Entity> extends ModelClass<E> {

    private static final long serialVersionUID = 0L;

    /** The class of entity IDs. */
    private final Class<?> idClass;

    /** The class of the entity state. */
    private final Class<? extends Message> stateClass;

    /** Type of the entity state. */
    private final TypeUrl entityStateType;

    /** The constructor for entities of this class. */
    @SuppressWarnings("TransientFieldNotInitialized") // Lazily initialized via accessor method.
    private transient @Nullable Constructor<E> entityConstructor;

    /** Creates new instance of the model class for the passed class of entities. */
    public EntityClass(Class<? extends E> cls) {
        super(cls);
        checkNotNull((Class<? extends Entity>) cls);
        Class<?> idClass = Entity.GenericParameter.ID.getArgumentIn(cls);
        checkIdClass(idClass);
        this.idClass = idClass;
        this.stateClass = getStateClass(cls);
        this.entityStateType = TypeUrl.of(stateClass);
    }

    /**
     * Creates new entity.
     */
    public E createEntity(Object constructorArgument) {
        checkNotNull(constructorArgument);
        Constructor<E> ctor = getConstructor();
        E result;
        try {
            result = ctor.newInstance(constructorArgument);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    private static ModelError noSuchConstructor(String entityClass, String idClass) {
        String errMsg = format(
                "%s class must declare a constructor with a single %s ID parameter.",
                entityClass, idClass
        );
        return new ModelError(new NoSuchMethodException(errMsg));
    }

    /**
     * Retrieves the state class of the passed entity class.
     *
     * <p>Though this method is {@code public}, it is <em>not</em> considered a part of the
     * public API. It is used internally by other framework routines and not designed for efficient
     * execution by Spine users.
     */
    private static <S extends Message> Class<S> getStateClass(Class<? extends Entity> entityClass) {
        @SuppressWarnings("unchecked") // The type is preserved by the Entity type declaration.
        Class<S> result = (Class<S>) Entity.GenericParameter.STATE.getArgumentIn(entityClass);
        return result;
    }

    /**
     * Creates default state by entity class.
     *
     * @return default state
     */
    @Internal
    public static Message createDefaultState(Class<? extends Entity> entityClass) {
        Class<? extends Message> stateClass = getStateClass(entityClass);
        Message result = Messages.newInstance(stateClass);
        return result;
    }

    /**
     * Obtains constructor for the entities of this class.
     */
    public Constructor<E> getConstructor() {
        if (entityConstructor == null) {
            entityConstructor = findConstructor();
        }
        return entityConstructor;
    }

    /**
     * Obtains the constructor for the passed entity class.
     *
     * <p>The entity class must have a constructor with the single parameter of type defined by
     * generic type {@code <I>}.
     * 
     * @throws IllegalStateException if the entity class does not have the required constructor
    */
    @SuppressWarnings({"JavaReflectionMemberAccess" /* Entity ctor must accept ID parameter */,
                       "unchecked" /* The cast is protected by generic params of this class. */})
    protected Constructor<E> findConstructor() {
        Class<? extends E> entityClass = value();
        Class<?> idClass = getIdClass();
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
     * Checks that this class of identifiers is supported by the framework.
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
    private static <I> void checkIdClass(Class<I> idClass) throws ModelError {
        try {
            Identifier.checkSupported(idClass);
        } catch (IllegalArgumentException e) {
            throw new ModelError(e);
        }
    }

    /**
     * Obtains the class of IDs used by the entities of this class.
     */
    public final Class<?> getIdClass() {
        return idClass;
    }

    /**
     * Obtains the class of the state of entities of this class.
     */
    public final Class<? extends Message> getStateClass() {
        return stateClass;
    }

    /**
     * Obtains type URL of the state of entities of this class.
     */
    public final TypeUrl getStateType() {
        return entityStateType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
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
