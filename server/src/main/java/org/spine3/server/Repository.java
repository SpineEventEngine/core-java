/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server;

import org.spine3.server.storage.StorageFactory;
import org.spine3.server.util.Classes;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Throwables.propagate;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isPublic;

/**
 * Abstract base class for repositories.
 *
 * @author Alexander Yevsyukov
 */
public abstract class Repository<I, E extends Entity<I, ?>> implements AutoCloseable {

    /**
     * The index of the declaration of the generic type {@code I} in this class.
     */
    private static final int ID_CLASS_GENERIC_INDEX = 0;

    /**
     * The index of the declaration of the generic type {@code E} in this class.
     */
    private static final int ENTITY_CLASS_GENERIC_INDEX = 1;

    /**
     * The {@code BoundedContext} in which this repository works.
     */
    private final BoundedContext boundedContext;

    /**
     * The constructor for creating entity instances.
     */
    private final Constructor<E> entityConstructor;

    /**
     * The data storage for this repository.
     */
    private AutoCloseable storage;

    /**
     * Creates the repository in the passed {@link BoundedContext}.
     *
     * @param boundedContext the {@link BoundedContext} in which this repository works
     */
    protected Repository(BoundedContext boundedContext) {
        this.boundedContext = boundedContext;
        this.entityConstructor = getEntityConstructor();
        this.entityConstructor.setAccessible(true);
    }

    private Constructor<E> getEntityConstructor() {
        final Class<E> entityClass = getEntityClass();
        final Class<I> idClass = getIdClass();
        try {
            final Constructor<E> result = entityClass.getDeclaredConstructor(idClass);
            checkConstructorAccessModifier(result.getModifiers(), entityClass.getName());
            return result;
        } catch (NoSuchMethodException ignored) {
            throw noSuchConstructorException(entityClass.getName(), idClass.getName());
        }
    }

    private static void checkConstructorAccessModifier(int modifiers, String entityClass) {
        if (!isPublic(modifiers)) {
            final String constructorModifier = isPrivate(modifiers) ? "private." : "protected.";
            final String message = "Constructor must be public in the " + entityClass + " class, found: " + constructorModifier;
            throw propagate(new IllegalAccessException(message));
        }
    }

    private static RuntimeException noSuchConstructorException(String entityClass, String idClass) {
        final String message = entityClass + " class must declare a constructor with a single " + idClass + " ID parameter.";
        return propagate(new NoSuchMethodException(message));
    }

    /**
     * @return the {@link BoundedContext} in which this repository works
     */
    protected BoundedContext getBoundedContext() {
        return boundedContext;
    }

    /**
     * @return the class of IDs used by this repository
     */
    @CheckReturnValue
    protected Class<I> getIdClass() {
        return Classes.getGenericParameterType(getClass(), ID_CLASS_GENERIC_INDEX);
    }

    /**
     * @return the class of entities managed by this repository
     */
    @CheckReturnValue
    protected Class<E> getEntityClass() {
        return Classes.getGenericParameterType(getClass(), ENTITY_CLASS_GENERIC_INDEX);
    }

    /**
     * Stores the passed object.
     *
     * <p>The storage must be assigned before calling this method.
     *
     * @param obj an instance to store
     */
    protected abstract void store(E obj);

    /**
     * Loads the entity with the passed ID.
     *
     * <p>The storage must be assigned before calling this method.
     *
     * @param id the id of the entity to load
     * @return the entity or {@code null} if there's no entity with such id
     */
    @CheckReturnValue
    @Nullable
    protected abstract E load(I id);

    /**
     * @return the storage assigned to this repository or {@code null} if the storage is not assigned yet
     */
    @CheckReturnValue
    @Nullable
    protected AutoCloseable getStorage() {
        return this.storage;
    }

    /**
     * @return true if the storage is assigned, false otherwise
     */
    public boolean storageAssigned() {
        return this.storage != null;
    }

    /**
     * Create a new entity instance with its default state.
     *
     * @param id the id of the entity
     * @return new entity instance
     */
    @CheckReturnValue
    public E create(I id) {
        try {
            final E result = entityConstructor.newInstance(id);
            result.setDefault();

            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw propagate(e);
        }
    }

    /**
     * Initializes the storage using the passed factory.
     *
     * @param factory storage factory
     * @throws IllegalStateException if the repository already has storage initialized
     */
    public void initStorage(StorageFactory factory) {
        if (this.storage != null) {
            throw new IllegalStateException(String.format(
                    "Repository %s has already storage %s.", this, this.storage));
        }

        this.storage = createStorage(factory);
    }

    /**
     * Creates the storage using the passed factory.
     *
     * <p>Implementations are responsible for properly calling the factory
     * for creating the storage, which is compatible with the repository.
     *
     * @param factory the factory to create the storage
     * @return the created storage instance
     */
    protected abstract AutoCloseable createStorage(StorageFactory factory);

    @Override
    public void close() throws Exception {
        if (this.storage != null) {
            this.storage.close();
            this.storage = null;
        }
    }

}
