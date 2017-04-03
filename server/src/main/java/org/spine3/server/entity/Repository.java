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
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.protobuf.Message;
import org.spine3.base.Identifiers;
import org.spine3.server.reflect.GenericTypeIndex;
import org.spine3.server.storage.Storage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.type.ClassName;
import org.spine3.type.KnownTypes;
import org.spine3.type.TypeUrl;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.server.reflect.Classes.getGenericParameterType;
import static org.spine3.util.Exceptions.newIllegalStateException;
import static org.spine3.util.Exceptions.unsupported;
import static org.spine3.util.Exceptions.wrappedCause;

/**
 * Abstract base class for repositories.
 *
 * @param <I> the type of IDs of entities managed by the repository
 * @param <E> the entity type
 * @author Alexander Yevsyukov
 */
public abstract class Repository<I, E extends Entity<I, ?>>
                implements RepositoryView<I, E>,
                           AutoCloseable {

    private static final String ERR_MSG_STORAGE_NOT_ASSIGNED = "Storage is not assigned.";

    /**
     * The data storage for this repository.
     *
     * <p>This field is null if the storage was not {@linkplain #initStorage(StorageFactory)
     * initialized} or the repository was {@linkplain #close() closed}.
     */
    @Nullable
    private Storage<I, ?> storage;

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
    @Nullable
    private volatile Class<E> entityClass;

    /**
     * Cached value for the entity ID class.
     *
     * <p>Used to optimize heavy {@link #getIdClass()} calls.
     */
    @Nullable
    private volatile Class<I> idClass;

    /**
     * Creates the repository.
     */
    protected Repository() {
    }

    /** Returns the class of IDs used by this repository. */
    @CheckReturnValue
    protected Class<I> getIdClass() {
        if (idClass == null) {
            final Class<I> candidateClass = Entity.TypeInfo.getIdClass(getEntityClass());
            checkIdClass(candidateClass);
            idClass = candidateClass;
        }
        return idClass;
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
     * @throws IllegalStateException of unsupported ID class passed
     */
    private static <I> void checkIdClass(Class<I> idClass) throws IllegalStateException {
        try {
            Identifiers.checkSupported(idClass);
        } catch (IllegalArgumentException e) {
            throw wrappedCause(e);
        }
    }

    /** Returns the class of entities managed by this repository. */
    @CheckReturnValue
    public Class<E> getEntityClass() {
        if (entityClass == null) {
            @SuppressWarnings("unchecked") // By the cast we enforce having generic params.
            final Class<? extends Repository<I, E>> repoClass =
                    (Class<? extends Repository<I, E>>) getClass();
            entityClass = TypeInfo.getEntityClass(repoClass);
        }
        checkNotNull(entityClass);
        return entityClass;
    }

    /**
     * Obtains the class of the entity state.
     *
     * <p>The default implementation uses generic type parameter from the entity class declaration.
     */
    protected Class<? extends Message> getEntityStateClass() {
        final Class<E> entityClass = getEntityClass();
        return Entity.TypeInfo.getStateClass(entityClass);
    }

    /**
     * Returns the {@link TypeUrl} for the state objects wrapped by entities
     * managed by this repository
     */
    @CheckReturnValue
    public TypeUrl getEntityStateType() {
        if (entityStateType == null) {
            final Class<? extends Message> stateClass = getEntityStateClass();
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
    public abstract E create(I id);

    /**
     * Stores the passed object.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @param obj an instance to store
     */
    protected abstract void store(E obj);

    /**
     * {@inheritDoc}
     *
     * <p>The returned iterator does not support removal.
     *
     * <p>Iteration through entities is performed by {@linkplain #find(Object) loading}
     * them one by one.
     */
    @Override
    public Iterator<E> iterator(Predicate<E> filter) {
        final Iterator<E> unfiltered = new EntityIterator<>(this);
        final Iterator<E> filtered = Iterators.filter(unfiltered, filter);
        return filtered;
    }

    /**
     * Returns the storage assigned to this repository or {@code null} if
     * the storage is not assigned yet.
     */
    @CheckReturnValue
    @Nullable
    protected Storage<I, ?> getStorage() {
        return this.storage;
    }

    /** Returns {@code true} if the storage is assigned, {@code false} otherwise. */
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
            throw newIllegalStateException("The repository %s already has storage %s.",
                                           this, this.storage);
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
    protected abstract Storage<I, ?> createStorage(StorageFactory factory);

    /**
     * Closes the repository by closing the underlying storage.
     *
     * <p>The reference to the storage becomes null after this call.
     */
    @Override
    public void close() {
        if (this.storage != null) {
            this.storage.close();
            this.storage = null;
        }
    }

    private boolean isOpen() {
        return storage != null;
    }

    private Storage<I, ?> ensureStorage() {
        checkState(storage != null, "No storage assigned in repository %s", this);
        return storage;
    }

    /**
     * Ensures that the repository {@linkplain #isOpen() is open}.
     *
     * <p>If not throws {@code IllegalStateException}
     */
    protected void checkNotClosed() {
        checkState(isOpen(), "The repository (%s) is closed.", getClass().getName());
    }

    /**
     * Enumeration of generic type parameters of this class.
     */
    enum GenericParameter implements GenericTypeIndex {

        /** The index of the generic type {@code <I>}. */
        ID(0),

        /** The index of the generic type {@code <E>}. */
        ENTITY(1);

        private final int index;

        GenericParameter(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return this.index;
        }
    }

    private static class TypeInfo {

        private static <E extends Entity<I, ?>, I>
        Class<E> getEntityClass(Class<? extends Repository<I, E>> repositoryClass) {
            final Class<E> result = getGenericParameterType(repositoryClass,
                                                            GenericParameter.ENTITY.getIndex());
            return result;
        }
    }

    /**
     * An iterator of all entities from the storage.
     *
     * <p>This iterator does not allow removal.
     */
    private static class EntityIterator<I, E extends Entity<I, ?>> implements Iterator<E> {

        private final Repository<I, E> repository;
        private final Iterator<I> index;

        private EntityIterator(Repository<I, E> repository) {
            this.repository = repository;
            this.index = repository.ensureStorage()
                                   .index();
        }

        @Override
        public boolean hasNext() {
            final boolean result = index.hasNext();
            return result;
        }

        @Override
        public E next() {
            final I id = index.next();
            final Optional<E> loaded = repository.find(id);
            if (!loaded.isPresent()) {
                final String idStr = idToString(id);
                throw newIllegalStateException("Unable to load entity with ID: %s", idStr);
            }

            final E entity = loaded.get();
            return entity;
        }

        @Override
        public void remove() {
            throw unsupported();
        }
    }
}
