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
     * The {@code BoundedContext} in which this repository works.
     */
    private final BoundedContext boundedContext;

    /**
     * The constructor for creating new entity instances.
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
        return RepositoryTypeInfo.getIdClass(getClass());
    }

    /**
     * @return the class of entities managed by this repository
     */
    @CheckReturnValue
    protected Class<E> getEntityClass() {
        return RepositoryTypeInfo.getEntityClass(getClass());
    }

    /**
     * Stores the passed object.
     *
     * @param obj an instance to store
     */
    protected abstract void store(E obj);

    /**
     * Loads the entity with the passed ID.
     *
     * @param id the id of the entity to load
     * @return the entity or {@code null} if there's no entity with such id
     */
    @CheckReturnValue
    @Nullable
    protected abstract E load(I id);

    /**
     * Checks if the passed storage object is of required type.
     * <p/>
     * <p>Implementation should throw {@link ClassCastException} if the class of the passed
     * object does not match that required by the repository.
     *
     * @param storage the instance of storage to check
     * @throws ClassCastException if the object is not of the required class
     */
    protected abstract void checkStorageClass(Object storage);

    /**
     * @return the storage assigned to this repository or {@code null} if the storage is not assigned yet
     * @see #assignStorage(AutoCloseable)
     */
    @CheckReturnValue
    @Nullable
    protected AutoCloseable getStorage() {
        return this.storage;
    }

    /**
     * Create a new entity instance with its default state.
     *
     * @param id the id of the entity
     * @return new entity instance
     */
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
     * Assigns the storage to the repository.
     * <p/>
     * <p>The type of the storage depends on and should be checked by the implementations.
     * <p/>
     * <p>This method should be normally called once during registration of the repository with {@link BoundedContext}.
     * An attempt to call this method twice with different parameters will cause {@link IllegalStateException}.
     *
     * @param storage the storage instance
     * @throws ClassCastException    if the passed storage is not of the required type
     * @throws IllegalStateException on attempt to assign a storage if another storage is already assigned
     */
    public void assignStorage(AutoCloseable storage) {
        // NOTE: This method is not named `setStorage` according to JavaBean conventions to highlight
        // the fact that conventions for calling it are different.

        // Ignore if the same instance of the storage is passed more than one time.
        //noinspection ObjectEquality
        if (storage == this.storage) {
            return;
        }

        if (this.storage != null) {
            throw new IllegalStateException(String.format(
                    "Repository has already storage assigned: %s. Passed: %s.", this.storage, storage));
        }

        checkStorageClass(storage);
        this.storage = storage;
    }

    @Override
    public void close() throws Exception {
        if (this.storage != null) {
            this.storage.close();
            this.storage = null;
        }
    }

}
