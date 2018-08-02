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

package io.spine.server.entity;

import com.google.common.collect.Iterators;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.MessageEnvelope;
import io.spine.logging.Logging;
import io.spine.option.EntityOption;
import io.spine.reflect.GenericTypeIndex;
import io.spine.server.BoundedContext;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.stand.Stand;
import io.spine.server.storage.Storage;
import io.spine.server.storage.StorageFactory;
import io.spine.string.Stringifiers;
import io.spine.system.server.ChangeEntityState;
import io.spine.system.server.CreateEntity;
import io.spine.system.server.DispatchCommandToHandler;
import io.spine.system.server.DispatchEventToReactor;
import io.spine.system.server.DispatchEventToSubscriber;
import io.spine.system.server.MarkCommandAsHandled;
import io.spine.system.server.SystemGateway;
import io.spine.type.MessageClass;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.server.entity.Repository.GenericParameter.ENTITY;
import static io.spine.server.entity.model.EntityClass.asEntityClass;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;

/**
 * Abstract base class for repositories.
 *
 * @param <I> the type of IDs of entities managed by the repository
 * @param <E> the entity type
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ClassWithTooManyMethods") // OK for this core class.
public abstract class Repository<I, E extends Entity<I, ?>>
        implements RepositoryView<I, E>, AutoCloseable {

    private static final String ERR_MSG_STORAGE_NOT_ASSIGNED = "Storage is not assigned.";

    /**
     * The {@link BoundedContext} to which the repository belongs.
     *
     * <p>This field is null when a repository is not {@linkplain
     * BoundedContext#register(Repository) registered} yet.
     */
    private @MonotonicNonNull BoundedContext boundedContext;

    /**
     * Model class of entities managed by this repository.
     *
     * <p>This field is null if {@link #entityClass()} is never called.
     */
    private volatile @MonotonicNonNull EntityClass<E> entityClass;

    /**
     * The data storage for this repository.
     *
     * <p>This field is null if the storage was not {@linkplain #initStorage(StorageFactory)
     * initialized} or the repository was {@linkplain #close() closed}.
     */
    private @Nullable Storage<I, ?, ?> storage;

    /** Lazily initialized logger. */
    private final Supplier<Logger> loggerSupplier = Logging.supplyFor(getClass());

    /**
     * Creates the repository.
     */
    protected Repository() {
    }

    /**
     * Obtains model class for the entities managed by this repository.
     */
    protected final EntityClass<E> entityClass() {
        if (entityClass == null) {
            @SuppressWarnings("unchecked") // The type is ensured by the declaration of this class.
            Class<E> cast = (Class<E>) ENTITY.getArgumentIn(getClass());
            entityClass = getModelClass(cast);
        }
        return entityClass;
    }

    /**
     * Obtains a model class for the passed entity class value.
     */
    @Internal
    protected EntityClass<E> getModelClass(Class<E> cls) {
        return asEntityClass(cls);
    }

    /** Returns the class of IDs used by this repository. */
    @SuppressWarnings("unchecked") // The cast is ensured by generic parameters of the repository.
    public Class<I> getIdClass() {
        return (Class<I>) entityClass().getIdClass();
    }

    /** Returns the class of entities managed by this repository. */
    @SuppressWarnings("unchecked") // The cast is ensured by generic parameters of the repository.
    public Class<E> getEntityClass() {
        return (Class<E>) entityClass().value();
    }

    /**
     * Returns the {@link TypeUrl} for the state objects wrapped by entities
     * managed by this repository
     */
    public TypeUrl getEntityStateType() {
        return entityClass().getStateType();
    }

    /**
     * Assigns a {@code BoundedContext} to this repository.
     *
     * <p>If the repository does not have a storage assigned prior to this call, the storage
     * will be {@linkplain #initStorage(StorageFactory) initialized} from a {@code StorageFactory}
     * associated with the passed {@code BoundedContext}.
     */
    public final void setBoundedContext(BoundedContext boundedContext) {
        this.boundedContext = boundedContext;
        if (!isStorageAssigned()) {
            initStorage(boundedContext.getStorageFactory());
        }
    }

    /**
     * Verifies whether the registry is registered with a {@code BoundedContext}.
     */
    protected boolean isRegistered() {
        return boundedContext != null;
    }

    /**
     * Obtains {@code BoundedContext} to which this repository belongs.
     *
     * @return parent {@code BoundedContext}
     * @throws IllegalStateException if the repository is not registered {@linkplain
     *                               BoundedContext#register(Repository) registered} yet
     */
    protected final BoundedContext getBoundedContext() {
        checkState(boundedContext != null,
                   "The repository (class: %s) is not registered with a BoundedContext.",
                   getClass().getName());
        return boundedContext;
    }

    /**
     * The callback called by a {@link BoundedContext} during the {@linkplain
     * BoundedContext#register(Repository) registration} of the repository.
     *
     * <p>If entities managed by this repository are {@linkplain VersionableEntity versionable},
     * registers itself as a type supplier with the {@link Stand} of the parent
     * {@linkplain #getBoundedContext() parent} {@code BoundedContext}.
     */
    @OverridingMethodsMustInvokeSuper
    public void onRegistered() {
        if (managesVersionableEntities()) {
            getBoundedContext().getStand()
                               .registerTypeSupplier(cast(this));
        }
    }

    /**
     * Verifies if the repository manages instances of {@link VersionableEntity}.
     */
    private boolean managesVersionableEntities() {
        Class entityClass = getEntityClass();
        boolean result = VersionableEntity.class.isAssignableFrom(entityClass);
        return result;
    }

    /**
     * Casts the passed repository to one that manages {@link VersionableEntity}
     * instead of just {@link Entity}.
     *
     * <p>The cast is required for registering the repository as a type supplier
     * in the {@link Stand}.
     *
     * <p>The cast is safe because the method is called after the
     * {@linkplain #managesVersionableEntities() type check}.
     *
     * @see #onRegistered()
     */
    @SuppressWarnings("unchecked") // See Javadoc above.
    private static <I, E extends Entity<I, ?>>
    Repository<I, VersionableEntity<I, ?>> cast(Repository<I, E> repository) {
        return (Repository<I, VersionableEntity<I, ?>>) repository;
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
        Iterator<E> unfiltered = new EntityIterator<>(this);
        Iterator<E> filtered = Iterators.filter(unfiltered, filter::test);
        return filtered;
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
     * Returns the storage assigned to this repository.
     *
     * <p>In order to verify if the storage is assigned use {@link #isStorageAssigned()}.
     *
     * @throws IllegalStateException if the storage is not assigned
     */
    protected final Storage<I, ?, ?> getStorage() {
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
    protected static <S extends AutoCloseable> S checkStorage(@Nullable S storage) {
        checkState(storage != null, ERR_MSG_STORAGE_NOT_ASSIGNED);
        return storage;
    }

    private Storage<I, ?, ?> ensureStorage() {
        checkState(storage != null, "No storage assigned in repository %s", this);
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
    }

    /**
     * Verifies if the repository open.
     */
    public boolean isOpen() {
        return storage != null;
    }

    /**
     * Obtains the instance of logger associated with the class of the repository.
     */
    protected Logger log() {
        return loggerSupplier.get();
    }

    /**
     * Logs error caused by a message processing into the {@linkplain #log() repository log}.
     *
     * <p>The formatted message has the following parameters:
     * <ol>
     *     <li>The name of the message class.
     *     <li>The message ID.
     * </ol>
     *
     * @param msgFormat the format of the message
     * @param envelope  the envelope of the message caused the error
     * @param exception the error
     */
    protected void logError(String msgFormat,
                            MessageEnvelope envelope,
                            RuntimeException exception) {
        MessageClass messageClass = envelope.getMessageClass();
        String messageId = Stringifiers.toString(envelope.getId());
        String errorMessage = format(msgFormat, messageClass, messageId);
        log().error(errorMessage, exception);
    }

    /**
     * Obtains an instance of {@link Lifecycle} for the entity with the given ID.
     *
     * @param id the ID of the target entity
     * @return {@link Lifecycle} of the given entity
     */
    @Internal
    protected Lifecycle lifecycleOf(I id) {
        checkNotNull(id);
        SystemGateway gateway = getBoundedContext().getSystemGateway();
        return new DefaultLifecycle<>(gateway, id, getEntityStateType());
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
        public int getIndex() {
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
            this.index = repository.ensureStorage()
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
                throw newIllegalStateException("Unable to load entity with ID: %s", idStr);
            }

            E entity = loaded.get();
            return entity;
        }
    }

    /**
     * The lifecycle of an entity.
     *
     * <p>An instance of {@code Lifecycle} posts the system commands related to the entity
     * lifecycle.
     */
    @Internal
    public interface Lifecycle {

        /**
         * Posts the {@link CreateEntity} system command.
         */
        void onEntityCreated(EntityOption.Kind entityKind);

        /**
         * Posts the {@link DispatchCommandToHandler} system command.
         */
        void onDispatchCommand(Command command);

        /**
         * Posts the {@link io.spine.system.server.AssignTargetToCommand AssignTargetToCommand}
         * system command.
         */
        void onTargetAssignedToCommand(CommandId id);

        /**
         * Posts the {@link MarkCommandAsHandled} system command.
         */
        void onCommandHandled(Command command);

        /**
         * Posts the {@link DispatchEventToSubscriber} system command.
         */
        void onDispatchEventToSubscriber(Event event);

        /**
         * Posts the {@link DispatchEventToReactor} system command.
         */
        void onDispatchEventToReactor(Event event);

        /**
         * Posts the {@link ChangeEntityState} system command and the commands related to
         * the lifecycle flags.
         *
         * @param change     the change in the entity state and attributes
         * @param messageIds the IDs of the messages which caused the {@code change}; typically,
         *                   {@link io.spine.core.EventId EventId}s or {@link CommandId}s
         */
        void onStateChanged(EntityRecordChange change, Set<? extends Message> messageIds);
    }
}
