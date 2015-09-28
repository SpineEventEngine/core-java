/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

/**
 * Abstract implementation of basic features of repositories.
 *
 * @author Alexander Yevsyukov
 */
public abstract class RepositoryBase<I, E extends Entity<I, ?>> implements Repository<I, E> {

    /**
     * The constructor for creating new entity instances.
     */
    private final Constructor<E> entityConstructor;

    private Object storage;

    protected RepositoryBase() {
        this.entityConstructor = getEntityConstructor();
        this.entityConstructor.setAccessible(true);
    }

    private Constructor<E> getEntityConstructor() {
        Constructor<E> result;
        try {
            Class<E> entityClass = getEntityClass();
            Class<I> idClass = getIdClass();

            result = entityClass.getConstructor(idClass);
        } catch (NoSuchMethodException e) {
            throw propagate(e);
        }
        return result;
    }

    /**
     * @return the class of IDs used by this repository
     */
    @CheckReturnValue
    protected Class<I> getIdClass() {
        return TypeInfo.getIdClass(getClass());
    }

    /**
     * @return the class of entities managed by this repository
     */
    @CheckReturnValue
    protected Class<E> getEntityClass() {
        return TypeInfo.getEntityClass(getClass());
    }

    /**
     * Checks if the passed storage object is of required type.
     *
     * <p>Implementation should throw {@link ClassCastException} if the class of the passed
     * object does not match that required by the repository.
     *
     * @param storage the instance of storage to check
     * @throws ClassCastException if the object is not of the required class
     */
    protected abstract void checkStorageClass(Object storage);

    /**
     * @return the storage assigned to this repository or {@code null} if the storage is not assigned yet
     * @see #assignStorage(Object)
     */
    @CheckReturnValue
    @Nullable
    protected Object getStorage() {
        return this.storage;
    }

    /**
     * {@inheritDoc}
     */
    public E create(I id) {
        try {
            E result = entityConstructor.newInstance(id);
            result.setDefault();

            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw propagate(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void assignStorage(@Nullable Object storage) {
        if (storage == null) {
            shutDown();
            this.storage = null;
            return;
        }

        // Ignore if the same instance of the storage is passed more than one time.
        //noinspection ObjectEquality
        if (storage == this.storage) {
            return;
        }

        checkStorageClass(storage);
        this.storage = storage;
    }

    /**
     * Override this method to perform repository clean-up before the storage is detached.
     */
    @SuppressWarnings("NoopMethodInAbstractClass") // Do nothing by default.
    protected void shutDown() {
    }
}
