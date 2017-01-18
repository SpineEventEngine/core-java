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

package org.spine3.server.entity;

import com.google.common.base.Optional;
import org.spine3.protobuf.KnownTypes;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.BoundedContext;
import org.spine3.server.reflect.Classes;
import org.spine3.server.storage.Storage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.type.ClassName;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Abstract base class for repositories.
 *
 * @param <I> the type of IDs of entities managed by the repository
 * @param <E> the entity type
 * @author Alexander Yevsyukov
 */
public abstract class Repository<I, E extends Entity<I, ?>> implements AutoCloseable {

    /** The index of the declaration of the generic type {@code I} in this class. */
    private static final int ID_CLASS_GENERIC_INDEX = 0;

    /** The index of the declaration of the generic type {@code E} in this class. */
    private static final int ENTITY_CLASS_GENERIC_INDEX = 1;

    protected static final String ERR_MSG_STORAGE_NOT_ASSIGNED = "Storage is not assigned.";

    /** The {@code BoundedContext} in which this repository works. */
    private final BoundedContext boundedContext;

    /** The constructor for creating entity instances. */
    private final Constructor<E> entityConstructor;

    /** The data storage for this repository. */
    private Storage storage;

    /**
     * Cached value for the entity state type.
     *
     * <p>Used to optimise heavy {@link #getEntityStateType()} calls.
     **/
    private volatile TypeUrl entityStateType;

    /**
     * Cached value for the entity class.
     *
     * <p>Used to optimize heavy {@link #getEntityClass()} calls.
     **/
    private volatile Class<E> entityClass;

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
        final Constructor<E> result = Entity.getConstructor(entityClass, idClass);
        return result;
    }

    /** Returns the {@link BoundedContext} in which this repository works. */
    protected BoundedContext getBoundedContext() {
        return boundedContext;
    }

    /** Returns the class of IDs used by this repository. */
    @CheckReturnValue
    protected Class<I> getIdClass() {
        return Classes.getGenericParameterType(getClass(), ID_CLASS_GENERIC_INDEX);
    }

    /** Returns the class of entities managed by this repository. */
    @CheckReturnValue
    protected Class<E> getEntityClass() {
        if (entityClass == null) {
            entityClass = Classes.getGenericParameterType(getClass(), ENTITY_CLASS_GENERIC_INDEX);
        }
        checkNotNull(entityClass);
        return entityClass;
    }

    /** Returns the {@link TypeUrl} for the state objects wrapped by entities managed by this repository */
    @CheckReturnValue
    public TypeUrl getEntityStateType() {
        if (entityStateType == null) {
            final Class<E> entityClass = getEntityClass();
            final Class<Object> stateClass = Classes.getGenericParameterType(entityClass, Entity.STATE_CLASS_GENERIC_INDEX);
            final ClassName stateClassName = ClassName.of(stateClass);
            entityStateType = KnownTypes.getTypeUrl(stateClassName);
        }
        checkNotNull(entityStateType);
        return entityStateType;
    }

    /**
     * Create a new entity instance with its default state.
     *
     * @param id the id of the entity
     * @return new entity instance
     */
    @CheckReturnValue
    public E create(I id) {
        return Entity.createEntity(this.entityConstructor, id);
    }

    /**
     * Stores the passed object.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @param obj an instance to store
     */
    protected abstract void store(E obj);

    /**
     * Loads the entity with the passed ID.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @param id the id of the entity to load
     * @return the entity or empty {@code Optional} if there's no entity with such id
     */
    @CheckReturnValue
    protected abstract Optional<E> load(I id);

    /** Returns the storage assigned to this repository or {@code null} if the storage is not assigned yet. */
    @CheckReturnValue
    @Nullable
    protected AutoCloseable getStorage() {
        return this.storage;
    }

    /** Returns true if the storage is assigned, false otherwise. */
    public boolean storageAssigned() {
        return this.storage != null;
    }

    /**
     * Ensures that the storage is not null.
     *
     * @return passed value if it's not not null
     * @throws IllegalStateException if the passed instance is null
     */
    protected static <S extends AutoCloseable> S checkStorage(@Nullable S storage) {
        checkState(storage != null, ERR_MSG_STORAGE_NOT_ASSIGNED);
        return storage;
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
                    "The repository %s already has storage %s.", this, this.storage));
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
    protected abstract Storage createStorage(StorageFactory factory);

    /**
     * Closes the repository by closing the underlying storage.
     *
     * <p>The reference to the storage becomes null after this call.
     *
     * @throws Exception which occurred during closing of the storage
     */
    @Override
    public void close() throws Exception {
        if (this.storage != null) {
            this.storage.close();
            this.storage = null;
        }
    }
}
