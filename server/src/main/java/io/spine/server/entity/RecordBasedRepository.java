/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.annotation.Experimental;
import io.spine.annotation.Internal;
import io.spine.base.EntityState;
import io.spine.client.EntityId;
import io.spine.client.OrderBy;
import io.spine.client.ResponseFormat;
import io.spine.client.TargetFilters;
import io.spine.client.Targets;
import io.spine.core.Event;
import io.spine.core.Signal;
import io.spine.server.entity.storage.EntityQueries;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.RecordReadRequest;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterators.transform;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.validate.Validate.checkValid;

/**
 * The base class for repositories that store entities as records.
 *
 * <p>Such a repository is backed by {@link RecordStorage}.
 * Entity states are stored as {@link EntityRecord}s.
 *
 * @param <I>
 *         the type of entity identifiers
 * @param <E>
 *         the type of stored entities
 * @param <S>
 *         the type of entity state messages
 */
@SuppressWarnings("ClassWithTooManyMethods") // OK for this core class.
public abstract class RecordBasedRepository<I, E extends Entity<I, S>, S extends EntityState>
        extends Repository<I, E> {

    /** Creates a new instance. */
    protected RecordBasedRepository() {
        super();
    }

    /**
     * Obtains {@link EntityFactory} associated with this repository.
     */
    protected abstract EntityFactory<E> entityFactory();

    /**
     * Obtains {@link StorageConverter} associated with this repository.
     */
    protected abstract StorageConverter<I, E, S> storageConverter();

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException
     *         if the storage is null
     */
    protected RecordStorage<I> recordStorage() {
        @SuppressWarnings("unchecked") // OK as we control the creation in createStorage().
        RecordStorage<I> storage = (RecordStorage<I>) storage();
        return storage;
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public E create(I id) {
        E result = entityFactory().create(id);
        return result;
    }

    @Override
    public void store(E entity) {
        EntityRecordWithColumns record = toRecord(entity);
        RecordStorage<I> storage = recordStorage();
        storage.write(entity.id(), record);
    }

    @Override
    public Iterator<E> iterator(Predicate<E> filter) {
        Iterator<E> allEntities = loadAll(ResponseFormat.getDefaultInstance());
        Iterator<E> result = Iterators.filter(allEntities, filter::test);
        return result;
    }

    /**
     * Applies a given {@link Migration} to an entity with the given ID.
     *
     * <p>The operation is performed in three steps:
     * <ol>
     *     <li>Load an entity by the given ID.
     *     <li>Transform it through the migration operation.
     *     <li>Store the entity back to the repository or delete it depending on the migration
     *         configuration.
     * </ol>
     *
     * <p>This operation is only supported for entities that are
     * {@linkplain TransactionalEntity transactional}.
     *
     * @throws IllegalArgumentException
     *         if the entity with the given ID is not found in the repository
     * @throws IllegalStateException
     *         if the repository manages a non-transactional entity type
     * @see Migration
     * @see #applyMigration(Set, Migration) the batch version of the method
     */
    @SuppressWarnings("unchecked") // Checked at runtime.
    @Experimental
    public final <T extends TransactionalEntity<I, S, ?>>
    void applyMigration(I id, Migration<I, T, S> migration) {
        checkNotNull(id);
        checkNotNull(migration);
        checkEntityIsTransactional();

        E entity = findOrThrow(id);
        migration.applyTo((T) entity, (RecordBasedRepository<I, T, S>) this);
        if (migration.physicallyRemoveRecord()) {
            delete(id, migration);
        } else {
            store(entity);
        }
        migration.finishCurrentOperation();
    }

    /**
     * Applies a {@link Migration} to several entities in batch.
     *
     * <p>The operation is performed in three steps:
     * <ol>
     *     <li>Load entities by the given IDs.
     *     <li>Transform each entity through the migration operation.
     *     <li>Store the entities which are not configured to be deleted by the {@link Migration}
     *         back to the repository.
     * </ol>
     *
     * <p>This operation is only supported for entities that are
     * {@linkplain TransactionalEntity transactional}.
     *
     * @throws IllegalStateException
     *         if the repository manages a non-transactional entity type
     * @see Migration
     */
    @SuppressWarnings("unchecked") // Checked at runtime.
    @Experimental
    public final <T extends TransactionalEntity<I, S, ?>>
    void applyMigration(Set<I> ids, Migration<I, T, S> migration) {
        checkNotNull(ids);
        checkNotNull(migration);
        checkEntityIsTransactional();

        TargetFilters filters = Targets.someOf(entityModelClass().stateClass(), ids)
                                       .getFilters();
        Iterator<E> entities = find(filters, ResponseFormat.getDefaultInstance());

        Deque<E> toStore = newLinkedList();
        while (entities.hasNext()) {
            E entity = entities.next();
            migration.applyTo((T) entity, (RecordBasedRepository<I, T, S>) this);
            if (migration.physicallyRemoveRecord()) {
                I id = entity.id();
                delete(id, migration);
            } else {
                toStore.add(entity);
            }
            migration.finishCurrentOperation();
        }
        store(toStore);
    }

    @Override
    protected RecordStorage<I> createStorage() {
        StorageFactory sf = defaultStorageFactory();
        RecordStorage<I> result = sf.createRecordStorage(context().spec(), entityClass());
        return result;
    }

    /**
     * Stores {@linkplain Entity Entities} in bulk.
     *
     * <p>Note: The storage must be assigned before calling this method.
     *
     * @param entities
     *         the {@linkplain Entity Entities} to store
     */
    public void store(Collection<E> entities) {
        Map<I, EntityRecordWithColumns> records = newHashMapWithExpectedSize(entities.size());
        for (E entity : entities) {
            EntityRecordWithColumns recordWithColumns = toRecord(entity);
            records.put(entity.id(), recordWithColumns);
        }
        recordStorage().write(records);
    }

    /**
     * Finds an entity with the passed ID.
     *
     * @param id
     *         the ID of the entity to find
     * @return the entity or {@link Optional#empty()} if there is no entity with such ID
     */
    @Override
    public Optional<E> find(I id) {
        Optional<EntityRecord> record = findRecord(id);
        return record.map(this::toEntity);
    }

    /**
     * Finds an entity with the passed ID even if the entity is
     * {@linkplain WithLifecycle#isActive() active}.
     *
     * @param id
     *         the ID of the entity to find
     * @return the entity or {@link Optional#empty()} if there is no entity with such ID,
     *         or the entity is not active
     */
    public Optional<E> findActive(I id) {
        Optional<EntityRecord> record = findRecord(id);
        Optional<E> result = record.filter(WithLifecycle::isActive)
                                   .map(this::toEntity);
        return result;
    }

    /**
     * Finds a record and returns it if its {@link LifecycleFlags} don't make it
     * {@linkplain WithLifecycle#isActive() active}.
     */
    private Optional<EntityRecord> findRecord(I id) {
        RecordStorage<I> storage = recordStorage();
        RecordReadRequest<I> request = new RecordReadRequest<>(id);
        Optional<EntityRecord> found = storage.read(request);
        if (!found.isPresent()) {
            return Optional.empty();
        }
        EntityRecord record = found.get();
        return Optional.of(record);
    }

    /**
     * Loads an entity by the passed ID or creates a new one, if the entity was not found.
     *
     * <p>An entity will be loaded whether its {@linkplain WithLifecycle#isActive() active} or not.
     *
     * <p>The new entity is created if and only if there is no record with the corresponding ID.
     *
     * @param id
     *         the ID of the entity to load
     * @return the entity with the specified ID
     */
    protected E findOrCreate(I id) {
        Optional<EntityRecord> record = findRecord(id);
        E result = record.map(this::toEntity)
                         .orElseGet(() -> create(id));
        return result;
    }

    @VisibleForTesting
    public Iterator<E> loadAll(ResponseFormat format) {
        Iterator<EntityRecord> records = loadAllRecords(format);
        Function<EntityRecord, E> toEntity = storageConverter().reverse();
        Iterator<E> result = transform(records, toEntity::apply);
        return result;
    }

    /**
     * Loads all the entities in this repository with IDs,
     * contained within the passed {@code ids} values.
     *
     * <p>Provides a convenience wrapper around multiple invocations of
     * {@link #find(Object)}. Descendants may optimize the execution of this
     * method, choosing the most suitable way for the particular storage engine used.
     *
     * <p>The result only contains those entities which IDs are contained inside
     * the passed {@code ids}. The resulting collection is always returned
     * with no {@code null} values.
     *
     * <p>The order of objects in the result is not guaranteed to be the same
     * as the order of IDs passed as argument.
     *
     * <p>If the IDs contain duplicates, the result may also contain duplicates
     * depending on a particular implementation.
     *
     * <p>The resulting entity state must be valid in terms of {@code (required)},
     * {@code (required_fields)}, and {@code (goes).with} options after the mask is applied.
     * Otherwise, an {@link InvalidEntityStateException} is thrown.
     *
     * <p>Note: The storage must be assigned before calling this method.
     *
     * @param ids
     *         entity IDs to search for
     * @param fieldMask
     *         the entity state fields to load
     * @return all the entities in this repository with the IDs matching the given {@code Iterable}
     */
    public Iterator<E> loadAll(Iterable<I> ids, FieldMask fieldMask) {
        RecordStorage<I> storage = recordStorage();
        Iterator<@Nullable EntityRecord> records = storage.readMultiple(ids, fieldMask);
        Iterator<EntityRecord> presentRecords = Iterators.filter(records, Objects::nonNull);
        Function<EntityRecord, E> toEntity = storageConverter().reverse();
        Iterator<E> result = transform(presentRecords, toEntity::apply);
        return result;
    }

    /**
     * Obtains iterator over all present {@linkplain EntityRecord entity records}.
     *
     * <p>The maximum number of resulting entity states is limited by
     * the {@code ResponseFormat.limit}. If the limit is {@code 0}, all the entity states are
     * retrieved.
     *
     * <p>The order of the resulting entity states is defined by {@code ResponseFormat.order_by}.
     *
     * <p>The resulting entity states have only the specified in {@code ResponseFormat.field_mask}
     * fields. If the mask is empty, all the fields are retrieved.
     *
     * @param format
     *         the expected format of the response
     * @return an iterator over all records
     */
    @Internal
    public Iterator<EntityRecord> loadAllRecords(ResponseFormat format) {
        checkNotNull(format);
        RecordStorage<I> storage = recordStorage();
        Iterator<EntityRecord> records = storage.readAll(format);
        return records;
    }

    /**
     * Finds the entities passing the given filters and applies the given {@link FieldMask}
     * to the results.
     *
     * <p>A number of elements to retrieve can be limited to a certain number. The order of
     * the resulting entities is specified by the {@link OrderBy}.
     *
     * <p>Field mask is applied according to <a href="https://goo.gl/tW5wIU">FieldMask specs</a>.
     *
     * <p>The field paths in the entity column field filters are specified
     * to contain a single path member - the name of the entity column.
     *
     * <p>The filtering process is delegated to the underlying {@link RecordStorage}.
     *
     * <p>Note: The storage must be assigned before calling this method.
     *
     * @param filters
     *         the entity filters
     * @param format
     *         the expected format of the query response
     * @return all the entities in this repository passed through the filters
     * @see EntityQuery
     */
    public Iterator<E> find(TargetFilters filters, ResponseFormat format) {
        checkNotNull(filters);
        checkNotNull(format);

        Iterator<EntityRecord> records = findRecords(filters, format);
        Function<EntityRecord, E> toEntity = storageConverter().reverse();
        Iterator<E> result = transform(records, toEntity::apply);
        return result;
    }

    /**
     * Obtains iterator over {@linkplain EntityRecord entity records} matching the passed filters.
     *
     * @param filters
     *         entity filters
     *         the mask to apply to the entities
     * @return the iterator over the matching records
     */
    @Internal
    public Iterator<EntityRecord> findRecords(TargetFilters filters, ResponseFormat format) {
        checkNotNull(filters);
        checkValid(filters);
        checkNotNull(format);

        RecordStorage<I> storage = recordStorage();
        EntityQuery<I> entityQuery = EntityQueries.from(filters, storage);
        Iterator<EntityRecord> records = storage.readAll(entityQuery, format);
        return records;
    }

    private E findOrThrow(I id) {
        return find(id).orElseThrow(() -> newIllegalArgumentException(
                "An entity `%s` with ID `%s` is not found in the repository.",
                entityClass().getCanonicalName(), id
        ));
    }

    /**
     * Removes an entity record with a passed ID from the storage.
     */
    private boolean delete(I id) {
        boolean deleted = recordStorage().delete(id);
        return deleted;
    }

    /**
     * Removes an entity record from the storage and posts a corresponding system event.
     *
     * @param id
     *         the entity ID
     * @param deletionCause
     *         the {@code Signal} which caused the deletion
     */
    private boolean deleteAndPostEvent(I id, Signal<?, ?, ?> deletionCause) {
        boolean deleted = delete(id);
        if (deleted) {
            lifecycleOf(id).onRemovedFromStorage(ImmutableList.of(deletionCause.messageId()));
        }
        return deleted;
    }

    /**
     * Deletes an entity record as a result of the {@link Migration} operation.
     */
    private void delete(I id, Migration<I, ?, S> migration) {
        Optional<Event> event = migration.systemEvent();
        boolean deleted = event.map(value -> deleteAndPostEvent(id, value))
                               .orElseGet(() -> delete(id));
        if (!deleted) {
            _warn().log("Could not delete an entity record of type `%s` with ID `%s`.",
                        entityStateType(), id);
        }
    }

    /**
     * Converts the passed entity into the record.
     */
    @VisibleForTesting
    EntityRecordWithColumns toRecord(E entity) {
        EntityRecord entityRecord = storageConverter().convert(entity);
        checkNotNull(entityRecord);
        EntityRecordWithColumns result =
                EntityRecordWithColumns.create(entityRecord, entity, recordStorage());
        return result;
    }

    /**
     * Converts the passed record into an entity.
     */
    @OverridingMethodsMustInvokeSuper
    protected E toEntity(EntityRecord record) {
        E result = storageConverter().reverse()
                                     .convert(record);
        checkNotNull(result);
        return result;
    }

    private void checkEntityIsTransactional() {
        checkState(TransactionalEntity.class.isAssignableFrom(entityClass()),
                   "`%s` is not a transactional entity type. The requested operation is only " +
                           "supported for transactional entity types.",
                   entityClass().getCanonicalName()
        );
    }

    /**
     * Transforms an instance of {@link EntityId} into an identifier
     * of the required type.
     *
     * @param <I>
     *         the target type of identifiers
     */
    @VisibleForTesting
    static class EntityIdFunction<I> implements Function<EntityId, I> {

        private final Class<I> expectedIdClass;

        EntityIdFunction(Class<I> expectedIdClass) {
            this.expectedIdClass = expectedIdClass;
        }

        @Override
        public @Nullable I apply(@Nullable EntityId input) {
            checkNotNull(input);
            Any idAsAny = input.getId();

            TypeUrl typeUrl = TypeUrl.ofEnclosed(idAsAny);
            Class<?> messageClass = typeUrl.toJavaClass();
            checkIdClass(messageClass);

            Message idAsMessage = unpack(idAsAny);

            @SuppressWarnings("unchecked")
            // As the message class is the same as expected, the conversion is safe.
            I id = (I) idAsMessage;
            return id;
        }

        private void checkIdClass(Class<?> messageClass) {
            boolean classIsSame = expectedIdClass.equals(messageClass);
            if (!classIsSame) {
                throw newIllegalStateException(
                        "Unexpected ID class encountered: `%s`. Expected: `%s`.",
                        messageClass.getCanonicalName(), expectedIdClass.getCanonicalName()
                );
            }
        }
    }
}
