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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.client.EntityId;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.MessageEnvelope;
import io.spine.logging.Logging;
import io.spine.option.EntityOption;
import io.spine.reflect.GenericTypeIndex;
import io.spine.server.BoundedContext;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.model.Model;
import io.spine.server.stand.Stand;
import io.spine.server.storage.Storage;
import io.spine.server.storage.StorageFactory;
import io.spine.string.Stringifiers;
import io.spine.system.server.ArchiveEntity;
import io.spine.system.server.ChangeEntityState;
import io.spine.system.server.CommandReceiver;
import io.spine.system.server.CreateEntity;
import io.spine.system.server.DeleteEntity;
import io.spine.system.server.DispatchCommandToHandler;
import io.spine.system.server.DispatchEventToReactor;
import io.spine.system.server.DispatchEventToSubscriber;
import io.spine.system.server.DispatchedMessageId;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.ExtractEntityFromArchive;
import io.spine.system.server.MarkCommandAsHandled;
import io.spine.system.server.RestoreEntity;
import io.spine.type.MessageClass;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.base.Identifier.pack;
import static io.spine.server.entity.Repository.GenericParameter.ENTITY;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

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
    @SuppressWarnings("unchecked") // The cast is ensured by generic parameters of the repository.
    protected EntityClass<E> getModelClass(Class<E> cls) {
        return (EntityClass<E>) Model.getInstance()
                                     .asEntityClass(cls);
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
        Iterator<E> filtered = Iterators.filter(unfiltered, filter);
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
     * Ensures that the repository {@linkplain #isOpen() is open}.
     *
     * <p>If not throws {@code IllegalStateException}
     */
    protected void checkNotClosed() {
        checkState(isOpen(), "The repository (%s) is closed.", getClass().getName());
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

    private void postSystem(Message systemCommand) {
        getBoundedContext().getSystemGateway()
                           .postCommand(systemCommand);
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
        return new Lifecycle(id);
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
    @SuppressWarnings("OverlyCoupledClass") // Posts system events.
    protected class Lifecycle {

        private final EntityHistoryId id;

        @VisibleForTesting
        protected Lifecycle(I id) {
            this.id = historyId(id);
        }

        /**
         * Posts the {@link CreateEntity} system command.
         */
        public void onEntityCreated(EntityOption.Kind entityKind) {
            CreateEntity command = CreateEntity
                    .newBuilder()
                    .setId(id)
                    .setKind(entityKind)
                    .build();
            postSystem(command);
        }

        /**
         * Posts the {@link DispatchCommandToHandler} system command.
         */
        public void onDispatchCommand(Command command) {
            DispatchCommandToHandler systemCommand = DispatchCommandToHandler
                    .newBuilder()
                    .setReceiver(id)
                    .setCommandId(command.getId())
                    .build();
            postSystem(systemCommand);
        }

        /**
         * Posts the {@link MarkCommandAsHandled} system command.
         */
        public void onCommandHandled(Command command) {
            CommandReceiver receiver = CommandReceiver
                    .newBuilder()
                    .setEntityId(id.getEntityId())
                    .setTypeUrl(id.getTypeUrl())
                    .build();
            MarkCommandAsHandled systemCommand = MarkCommandAsHandled
                    .newBuilder()
                    .setId(command.getId())
                    .setReceiver(receiver)
                    .build();
            postSystem(systemCommand);
        }

        /**
         * Posts the {@link DispatchEventToSubscriber} system command.
         */
        public void onDispatchEventToSubscriber(Event event) {
            DispatchEventToSubscriber systemCommand = DispatchEventToSubscriber
                    .newBuilder()
                    .setReceiver(id)
                    .setEventId(event.getId())
                    .build();
            postSystem(systemCommand);
        }

        /**
         * Posts the {@link DispatchEventToReactor} system command.
         */
        public void onDispatchEventToReactor(Event event) {
            DispatchEventToReactor systemCommand = DispatchEventToReactor
                    .newBuilder()
                    .setReceiver(id)
                    .setEventId(event.getId())
                    .build();
            postSystem(systemCommand);
        }

        /**
         * Posts the {@link ChangeEntityState} system command and the commands related to
         * the lifecycle flags.
         */
        void onStateChanged(EntityRecordChange change,
                            Set<? extends Message> messageIds) {
            Collection<DispatchedMessageId> dispatchedMessageIds = toDispatched(messageIds);

            postIfChanged(change, dispatchedMessageIds);
            postIfArchived(change, dispatchedMessageIds);
            postIfDeleted(change, dispatchedMessageIds);
            postIfExtracted(change, dispatchedMessageIds);
            postIfRestored(change, dispatchedMessageIds);
        }

        private void postIfChanged(EntityRecordChange change,
                                   Collection<DispatchedMessageId> messageIds) {
            Any oldState = change.getPreviousValue().getState();
            Any newState = change.getNewValue().getState();

            if (!oldState.equals(newState)) {
                ChangeEntityState command = ChangeEntityState
                        .newBuilder()
                        .setId(id)
                        .setNewState(newState)
                        .addAllMessageId(messageIds)
                        .build();
                postSystem(command);
            }
        }

        private void postIfArchived(EntityRecordChange change,
                                    Collection<DispatchedMessageId> messageIds) {
            boolean oldValue = change.getPreviousValue()
                                     .getLifecycleFlags()
                                     .getArchived();
            boolean newValue = change.getNewValue()
                                     .getLifecycleFlags()
                                     .getArchived();
            if (newValue && !oldValue) {
                ArchiveEntity command = ArchiveEntity
                        .newBuilder()
                        .setId(id)
                        .addAllMessageId(messageIds)
                        .build();
                postSystem(command);
            }
        }

        private void postIfDeleted(EntityRecordChange change,
                                   Collection<DispatchedMessageId> messageIds) {
            boolean oldValue = change.getPreviousValue()
                                     .getLifecycleFlags()
                                     .getDeleted();
            boolean newValue = change.getNewValue()
                                     .getLifecycleFlags()
                                     .getDeleted();
            if (newValue && !oldValue) {
                DeleteEntity command = DeleteEntity
                        .newBuilder()
                        .setId(id)
                        .addAllMessageId(messageIds)
                        .build();
                postSystem(command);
            }
        }

        private void postIfExtracted(EntityRecordChange change,
                                     Collection<DispatchedMessageId> messageIds) {
            boolean oldValue = change.getPreviousValue()
                                     .getLifecycleFlags()
                                     .getArchived();
            boolean newValue = change.getNewValue()
                                     .getLifecycleFlags()
                                     .getArchived();
            if (!newValue && oldValue) {
                ExtractEntityFromArchive command = ExtractEntityFromArchive
                        .newBuilder()
                        .setId(id)
                        .addAllMessageId(messageIds)
                        .build();
                postSystem(command);
            }
        }

        private void postIfRestored(EntityRecordChange change,
                                    Collection<DispatchedMessageId> messageIds) {
            boolean oldValue = change.getPreviousValue()
                                     .getLifecycleFlags()
                                     .getDeleted();
            boolean newValue = change.getNewValue()
                                     .getLifecycleFlags()
                                     .getDeleted();
            if (!newValue && oldValue) {
                RestoreEntity command = RestoreEntity
                        .newBuilder()
                        .setId(id)
                        .addAllMessageId(messageIds)
                        .build();
                postSystem(command);
            }
        }

        private Collection<DispatchedMessageId>
        toDispatched(Collection<? extends Message> messageIds) {
            Collection<DispatchedMessageId> dispatchedMessageIds =
                    messageIds.stream()
                              .map(this::dispatchedMessageId)
                              .collect(toList());
            return dispatchedMessageIds;
        }

        private EntityHistoryId historyId(I id) {
            EntityId entityId = EntityId
                    .newBuilder()
                    .setId(pack(id))
                    .build();
            TypeUrl type = getEntityStateType();
            EntityHistoryId historyId = EntityHistoryId
                    .newBuilder()
                    .setEntityId(entityId)
                    .setTypeUrl(type.value())
                    .build();
            return historyId;
        }

        @SuppressWarnings("ChainOfInstanceofChecks")
        private DispatchedMessageId dispatchedMessageId(Message messageId) {
            checkNotNull(messageId);
            if (messageId instanceof EventId) {
                EventId eventId = (EventId) messageId;
                return DispatchedMessageId.newBuilder()
                                          .setEventId(eventId)
                                          .build();
            } else if (messageId instanceof CommandId) {
                CommandId commandId = (CommandId) messageId;
                return DispatchedMessageId.newBuilder()
                                          .setCommandId(commandId)
                                          .build();
            } else {
                throw newIllegalArgumentException(
                        "Unexpected message ID of type %s. Expected EventId or CommandId.",
                        messageId.getClass()
                );
            }
        }
    }
}
