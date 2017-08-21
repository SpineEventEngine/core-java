/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.server.entity.Entity;
import io.spine.type.ClassName;
import io.spine.type.KnownTypes;
import io.spine.type.TypeUrl;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;

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
    @Nullable
    private transient Constructor<E> entityConstructor;

    protected EntityClass(Class<? extends E> cls) {
        super(cls);
        final Class<?> idClass = Entity.TypeInfo.getIdClass(cls);
        checkIdClass(idClass);
        this.idClass = idClass;
        this.stateClass = Entity.TypeInfo.getStateClass(cls);
        final ClassName stateClassName = ClassName.of(stateClass);
        this.entityStateType = KnownTypes.getTypeUrl(stateClassName);
    }

    public static <E extends Entity> EntityClass<E> valueOf(Class<? extends E> cls) {
        return new EntityClass<>(cls);
    }

    /**
     * Obtains the constructor for the passed entity class.
     *
     * <p>The entity class must have a constructor with the single parameter of type defined by
     * generic type {@code <I>}.
     *
     * @param entityClass the entity class
     * @param idClass     the class of entity identifiers
     * @param <E>         the entity type
     * @param <I>         the ID type
     * @return the constructor
     * @throws IllegalStateException if the entity class does not have the required constructor
     */
    public static <E extends Entity<I, ?>, I> Constructor<E> getConstructor(Class<E> entityClass,
                                                                            Class<I> idClass) {
        checkNotNull(entityClass);
        checkNotNull(idClass);

        try {
            @SuppressWarnings("JavaReflectionMemberAccess") // Required in the Entity definition.
            final Constructor<E> result = entityClass.getDeclaredConstructor(idClass);
            result.setAccessible(true);
            return result;
        } catch (NoSuchMethodException ignored) {
            throw noSuchConstructor(entityClass.getName(), idClass.getName());
        }
    }

    private static IllegalStateException noSuchConstructor(String entityClass, String idClass) {
        final String errMsg = format(
                "%s class must declare a constructor with a single %s ID parameter.",
                entityClass, idClass
        );
        return new IllegalStateException(new NoSuchMethodException(errMsg));
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

    @SuppressWarnings("unchecked")
    protected Constructor<E> findConstructor() {
        return (Constructor<E>) getConstructor(value(), getIdClass());
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
}
