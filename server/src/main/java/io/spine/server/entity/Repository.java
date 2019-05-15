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

package io.spine.server.entity;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.annotation.Internal;
import io.spine.annotation.SPI;
import io.spine.base.Identifier;
import io.spine.logging.Logging;
import io.spine.reflect.GenericTypeIndex;
import io.spine.server.BoundedContext;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.storage.Storage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.type.EventClass;
import io.spine.server.type.MessageEnvelope;
import io.spine.system.server.SystemWriteSide;
import io.spine.type.MessageClass;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.server.entity.model.EntityClass.asEntityClass;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;

/**
 * Abstract base class for repositories.
 */
@SuppressWarnings("ClassWithTooManyMethods") // OK for this core class.
public abstract class Repository<I, E extends Entity<I, ?>> implements AutoCloseable, Logging {

    private static final String ERR_MSG_STORAGE_NOT_ASSIGNED = "Storage is not assigned.";

    /**
     * The {@link BoundedContext} to which the repository belongs.
     *
     * <p>This field is null when a repository is not {@linkplain
     * BoundedContext#register(Repository) registered} yet and
     * after the repository is {@linkplain #close() closed}.
     */
    private @Nullable BoundedContext boundedContext;

    /**
     * Model class of entities managed by this repository.
     *
     * <p>This field is null if {@link #entityModelClass()} is never called.
     */
    private volatile @MonotonicNonNull EntityClass<E> entityClass;

    /**
     * The data storage for this repository.
     *
     * <p>This field is null if the storage was not {@linkplain #initStorage(StorageFactory)
     * initialized} or the repository was {@linkplain #close() closed}.
     */
    private @Nullable Storage<I, ?, ?> storage;

    /**
     * Creates the repository.
     */
    protected Repository() {
    }

    /**
     * Create a new entity instance with its default state.
     *
     * @param id the id of the entity
     * @return new entity instance
     */
    public abstract E create(I id);

    /**
     * Stores the passed object.
     *
     * <p>Note: The storage must be assigned before calling this method.
     *
     * @param obj an instance to store
     */
    @SuppressWarnings("AbstractMethodWithMissingImplementations") // work-around IDEA bug.
    protected abstract void store(E obj);

    /**
     * Finds an entity with the passed ID.
     *
     * @param id the ID of the entity to load
     * @return the entity or {@link Optional#empty()} if there's no entity with such ID
     */
    public abstract Optional<E> find(I id);

    /**
     * Returns an iterator over the entities managed by the repository that match the passed filter.
     *
     * <p>The returned iterator does not support removal.
     *
     * <p>Iteration through entities is performed by {@linkplain #find(Object) loading}
     * them one by one.
     */
    public Iterator<E> iterator(Predicate<E> filter) {
        Iterator<E> unfiltered = new EntityIterator<>(this);
        Iterator<E> filtered = Iterators.filter(unfiltered, filter::test);
        return filtered;
    }

    /**
     * Obtains model class for the entities managed by this repository.
     */
    protected EntityClass<E> entityModelClass() {
        if (entityClass == null) {
            @SuppressWarnings("unchecked") // The type is ensured by the declaration of this class.
            Class<E> cast = (Class<E>) GenericParameter.ENTITY.argumentIn(getClass());
            entityClass = toModelClass(cast);
        }
        return entityClass;
    }

    /**
     * Obtains a model class for the passed entity class value.
     */
    @Internal
    protected EntityClass<E> toModelClass(Class<E> cls) {
        return asEntityClass(cls);
    }

    /** Returns the class of IDs used by this repository. */
    @SuppressWarnings("unchecked") // The cast is ensured by generic parameters of the repository.
    public Class<I> idClass() {
        return (Class<I>) entityModelClass().idClass();
    }

    /** Returns the class of entities managed by this repository. */
    public Class<E> entityClass() {
        return entityModelClass().value();
    }

    /**
     * Obtains the {@link TypeUrl} for the state objects wrapped by entities
     * managed by this repository.
     */
    public TypeUrl entityStateType() {
        return entityModelClass().stateType();
    }

    /**
     * Obtains classes of the events produced by this {@code Repository}.
     *
     * <p>For convenience purposes the default version returns an empty set.
     * This method should be overridden by repositories which actually produce events.
     */
    public ImmutableSet<EventClass> outgoingEvents() {
        return ImmutableSet.of();
    }

    /**
     * Assigns a {@code BoundedContext} to this repository.
     *
     * <p>If the repository does not have a storage assigned prior to this call, the storage
     * will be {@linkplain #initStorage(StorageFactory) initialized} from a {@code StorageFactory}
     * associated with the passed {@code BoundedContext}.
     *
     * <p>A context for a repository can be set only once. Passing the same second time will have
     * no effect.
     *
     * @throws IllegalStateException
     *          if the repository has a context value already assigned, and the passed value is
     *          not equal to the assigned one
     */
    @Internal
    public final void setBoundedContext(BoundedContext context) {
        checkNotNull(context);
        boolean sameValue = context.equals(this.boundedContext);
        if (this.boundedContext != null && !sameValue) {
            throw newIllegalStateException(
                    "The repository `%s` has the Bounded Context (`%s`) assigned." +
                            " This operation can be performed only once." +
                            " Attempted to set: `%s`.",
                    this, this.boundedContext, context);
        }

        if (sameValue) {
            return;
        }

        this.boundedContext = context;
        if (!isStorageAssigned()) {
            initStorage(context.storageFactory());
        }
        init(context);
    }

    /**
     * Initializes the repository during its {@linkplain BoundedContext#register(Repository)
     * registration} with a {@code BoundedContext}.
     *
     * <p>When this method is called, the repository already has {@link #context() BoundedContext}
     * and the {@link #storage() Storage} {@linkplain #initStorage(StorageFactory) assigned}.
     *
     * <p>Registers itself as a type supplier with the {@link io.spine.server.stand.Stand Stand}
     * of the parent {@code BoundedContext}.
     *
     * @param context
     *          the {@code BoundedContext} of this repository
     */
    @OverridingMethodsMustInvokeSuper
    protected void init(BoundedContext context) {
        context.stand()
               .registerTypeSupplier(this);
    }

    /**
     * Verifies whether the repository is registered with a {@code BoundedContext}.
     */
    protected final boolean isRegistered() {
        return boundedContext != null;
    }

    /**
     * Obtains the {@code BoundedContext} to which this repository belongs.
     *
     * @return parent {@code BoundedContext}
     * @throws IllegalStateException
     *         if the repository is not registered {@linkplain BoundedContext#register(Repository)
     *         registered} yet
     */
    protected final BoundedContext context() {
        checkState(boundedContext != null,
                   "The repository (class: `%s`) is not registered with a `BoundedContext`.",
                   getClass().getName());
        return boundedContext;
    }

    /**
     * The callback called by a {@link BoundedContext} during the {@linkplain
     * BoundedContext#register(Repository) registration} of the repository.
     */
    @SuppressWarnings("NoopMethodInAbstractClass") // see Javadoc
    @OverridingMethodsMustInvokeSuper
    public void onRegistered() {
        // Do nothing by default.
    }

    /**
     * Initializes the storage using the passed factory.
     *
     * @param factory the storage factory
     * @throws IllegalStateException if the repository already has storage initialized
     */
    public void initStorage(StorageFactory factory) {
        if (this.storage != null) {
            throw newIllegalStateException(
                    "The repository `%s` already has storage `%s`.",
                    this, this.storage);
        }
        this.storage = createStorage(factory);
    }

    /**
     * Returns the storage assigned to this repository.
     *
     * <p>To verify if the storage is assigned, use {@link #isStorageAssigned()}.
     *
     * @throws IllegalStateException if the storage is not assigned
     */
    protected final Storage<I, ?, ?> storage() {
        return checkStorage(this.storage);
    }

    /**
     * Returns {@code true} if the storage is assigned, {@code false} otherwise.
     */
    public final boolean isStorageAssigned() {
        return this.storage != null;
    }

    /**
     * Ensures that the storage is not null.
     *
     * @return passed value if it's not not null
     * @throws IllegalStateException if the passed instance is null
     */
    protected static <S extends AutoCloseable> @NonNull S checkStorage(@Nullable S storage) {
        checkState(storage != null, ERR_MSG_STORAGE_NOT_ASSIGNED);
        return storage;
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
    protected abstract Storage<I, ?, ?> createStorage(StorageFactory factory);

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
        this.boundedContext = null;
    }

    /**
     * Verifies if the repository is open.
     */
    public final boolean isOpen() {
        return storage != null;
    }

    /**
     * Logs error caused by a message processing into the {@linkplain #log() repository log}.
     *
     * <p>The formatted message has the following parameters:
     * <ol>
     *     <li>The name of the message class.
     *     <li>The message ID.
     *     <li>The URL of the entity state type.
     * </ol>
     *
     * @param msgFormat the format of the message
     * @param envelope  the envelope of the message caused the error
     * @param exception the error
     */
    protected void logError(String msgFormat,
                            MessageEnvelope envelope,
                            RuntimeException exception) {
        MessageClass messageClass = envelope.messageClass();
        String stateType = entityStateType().value();
        String errorMessage = format(msgFormat, messageClass, envelope.idAsString(), stateType);
        _error(errorMessage, exception);
    }

    /**
     * Obtains an instance of {@link EntityLifecycle} for the entity with the given ID.
     *
     * <p>It is necessary that a tenant ID is set when calling this method in a multitenant
     * environment.
     *
     * @param id the ID of the target entity
     * @return {@link EntityLifecycle} of the given entity
     */
    @Internal
    protected EntityLifecycle lifecycleOf(I id) {
        checkNotNull(id);
        TypeUrl stateType = entityStateType();
        SystemWriteSide writeSide = context().systemClient()
                                             .writeSide();
        EventFilter eventFilter = eventFilter();
        EntityLifecycle lifecycle = EntityLifecycle
                .newBuilder()
                .setEntityId(id)
                .setEntityType(stateType)
                .setSystemWriteSide(writeSide)
                .setEventFilter(eventFilter)
                .build();
        return lifecycle;
    }

    /**
     * Creates an {@link EventFilter} for this repository.
     *
     * <p>All the events posted by this repository, domain, and system are first passed through this
     * filter.
     *
     * <p>By default, the filter allows all the events to be posted. For entities which do not allow
     * state subscription, the {@link io.spine.system.server.event.EntityStateChanged} event is
     * filtered out. Override this method to change this behaviour.
     *
     * @return an {@link EventFilter} to apply to all posted events
     * @implNote This method may be called many times for a single repository. It is reasonable that
     *           it does not re-initialize the filter each time. Also, it is necessary that
     *           the filter returned from this method is always (at least effectively) the same.
     *           See {@link Pure @Pure} for the details on the expected behaviour.
     */
    @SPI
    @Pure
    protected EventFilter eventFilter() {
        EntityClass<E> entityClass = entityModelClass();
        return EntityStateChangedFilter.forType(entityClass);
    }

    /**
     * Enumeration of generic type parameters of this class.
     */
    enum GenericParameter implements GenericTypeIndex<Repository> {

        /** The index of the generic type {@code <I>}. */
        ID(0),

        /** The index of the generic type {@code <E>}. */
        ENTITY(1);

        private final int index;

        GenericParameter(int index) {
            this.index = index;
        }

        @Override
        public int index() {
            return this.index;
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
            this.index = repository.storage()
                                   .index();
        }

        @Override
        public boolean hasNext() {
            boolean result = index.hasNext();
            return result;
        }

        @Override
        public E next() {
            I id = index.next();
            Optional<E> loaded = repository.find(id);
            if (!loaded.isPresent()) {
                String idStr = Identifier.toString(id);
                throw newIllegalStateException("Unable to load entity with ID: `%s`.", idStr);
            }

            E entity = loaded.get();
            return entity;
        }
    }
}
