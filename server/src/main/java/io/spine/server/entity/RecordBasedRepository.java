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

package io.spine.server.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.server.entity.storage.EntityQueries;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.RecordReadRequest;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.type.TypeUrl;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.entity.EntityWithLifecycle.Predicates.isEntityVisible;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The base class for repositories that store entities as records.
 *
 * <p>Such a repository is backed by {@link RecordStorage}.
 * Entity states are stored as {@link EntityRecord}s.
 *
 * @param <I> the type of IDs of entities
 * @param <E> the type of entities
 * @param <S> the type of entity state messages
 * @author Alexander Yevsyukov
 */
public abstract class RecordBasedRepository<I, E extends Entity<I, S>, S extends Message>
                extends Repository<I, E> {

    /** Creates a new instance. */
    protected RecordBasedRepository() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    protected RecordStorage<I> createStorage(StorageFactory factory) {
        final RecordStorage<I> result = factory.createRecordStorage(getEntityClass());
        return result;
    }

    /**
     * Obtains {@link EntityFactory} associated with this repository.
     */
    protected abstract EntityFactory<I, E> entityFactory();

    /**
     * Obtains {@link EntityStorageConverter} associated with this repository.
     */
    protected abstract EntityStorageConverter<I, E, S> entityConverter();

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException if the storage is null
     */
    protected RecordStorage<I> recordStorage() {
        @SuppressWarnings("unchecked") // OK as we control the creation in createStorage().
        final RecordStorage<I> storage = (RecordStorage<I>) getStorage();
        return storage;
    }

    /** {@inheritDoc} */
    @Override
    public E create(I id) {
        final E result = entityFactory().create(id);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void store(E entity) {
        final EntityRecordWithColumns record = toRecord(entity);
        final RecordStorage<I> storage = recordStorage();
        storage.write(entity.getId(), record);
    }

    /**
     * Stores {@linkplain Entity Entities} in bulk.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @param entities the {@linkplain Entity Entities} to store
     */
    public void store(Collection<E> entities) {
        final Map<I, EntityRecordWithColumns> records = newHashMapWithExpectedSize(entities.size());
        for (E entity : entities) {
            final EntityRecordWithColumns recordWithColumns = toRecord(entity);
            records.put(entity.getId(), recordWithColumns);
        }
        recordStorage().write(records);
    }

    /**
     * Stores the passed entity record.
     */
    @Internal
    public void storeRecord(EntityRecord record) {
        final E entity = toEntity(record);
        store(entity);
    }

    /** {@inheritDoc} */
    @Override
    @CheckReturnValue
    public Optional<E> find(I id) {
        Optional<EntityRecord> optional = findRecord(id);
        if (!optional.isPresent()) {
            return Optional.absent();
        }
        final EntityRecord record = optional.get();
        final boolean recordVisible = isEntityVisible().apply(record.getLifecycleFlags());
        if (!recordVisible) {
            return Optional.absent();
        }
        final E entity = toEntity(record);
        return Optional.of(entity);
    }

    /**
     * Finds a record and returns it if its {@link LifecycleFlags} don't make it
     * {@linkplain EntityWithLifecycle.Predicates#isEntityVisible()}.
     */
    private Optional<EntityRecord> findRecord(I id) {
        final RecordStorage<I> storage = recordStorage();
        final RecordReadRequest<I> request = new RecordReadRequest<>(id);
        final Optional<EntityRecord> found = storage.read(request);
        if (!found.isPresent()) {
            return Optional.absent();
        }
        final EntityRecord record = found.get();
        return Optional.of(record);
    }

    @Override
    public Iterator<E> iterator(Predicate<E> filter) {
        final Iterator<E> allEntities = loadAll();
        final Iterator<E> result = filter(allEntities, filter);
        return result;
    }

    @Internal
    @CheckReturnValue
    public EntityRecord findOrCreateRecord(I id) {
        final E entity = findOrCreate(id);
        final EntityRecordWithColumns recordWithColumns = toRecord(entity);
        return recordWithColumns.getRecord();
    }

    /**
     * Loads an entity by the passed ID or creates a new one, if the entity was not found.
     *
     * <p>If the entity exists, but has non-default {@link LifecycleFlags}
     * a newly created entity will be returned.
     */
    @CheckReturnValue
    protected E findOrCreate(I id) {
        final Optional<E> loaded = find(id);

        if (!loaded.isPresent()) {
            final E result = create(id);
            return result;
        }

        final E result = loaded.get();
        return result;
    }

    /**
     * Loads an entity by the passed ID or creates a new one, if the entity was not found.
     *
     * <p>An entity will be loaded despite its {@linkplain LifecycleFlags visibility}.
     *
     * @param id the ID of the entity to load
     * @return the entity with the specified ID
     */
    @CheckReturnValue
    protected E findWithAnyVisibilityOrCreate(I id) {
        Optional<EntityRecord> optional = findRecord(id);
        if (!optional.isPresent()) {
            return create(id);
        }
        final EntityRecord record = optional.get();
        final E entity = toEntity(record);
        return entity;
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
     * <p>In case IDs contain duplicates, the result may also contain duplicates,
     * depending on particular implementation.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @param ids entity IDs to search for
     * @return all the entities in this repository with the IDs matching the given {@code Iterable}
     */
    @CheckReturnValue
    public Iterator<E> loadAll(Iterable<I> ids) {
        return loadAll(ids, FieldMask.getDefaultInstance());
    }

    /**
     * Loads all the entities in this repository by their IDs and
     * applies the {@link FieldMask} to each of them.
     *
     * <p>Acts in the same way as {@link #loadAll(Iterable)}, with
     * the {@code FieldMask} applied to the results.
     *
     * <p>Field mask is applied according to <a href="https://goo.gl/tW5wIU">FieldMask specs</a>.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @param ids       entity IDs to search for
     * @param fieldMask mask to apply on entities
     * @return all the entities in this repository with the IDs contained in the given {@code ids}
     * @see #loadAll(Iterable)
     */
    @CheckReturnValue
    public Iterator<E> loadAll(Iterable<I> ids, FieldMask fieldMask) {
        final RecordStorage<I> storage = recordStorage();
        final Iterator<EntityRecord> entityStorageRecords = storage.readMultiple(ids, fieldMask);
        final Iterator<EntityRecord> presentRecords = filter(entityStorageRecords,
                                                             Predicates.<EntityRecord>notNull());
        final Function<EntityRecord, E> toEntity = entityConverter().reverse();
        final Iterator<E> result = transform(presentRecords, toEntity);
        return result;
    }

    /**
     * Loads all the entities in this repository.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @return all the entities in this repository
     * @see #loadAll(Iterable)
     */
    @CheckReturnValue
    public Iterator<E> loadAll() {
        final RecordStorage<I> storage = recordStorage();
        final Iterator<EntityRecord> records = storage.readAll();
        final Function<EntityRecord, E> toEntity = entityConverter().reverse();
        final Iterator<E> result = transform(records, toEntity);
        return result;
    }

    /**
     * Finds all the entities passing the given filters and
     * applies the given {@link FieldMask} to the results.
     *
     * <p>Field mask is applied according to <a href="https://goo.gl/tW5wIU">FieldMask specs</a>.
     *
     * <p>The field paths in the entity column field filters are specified
     * to contain a single path member - the name of the entity column.
     *
     * <p>The filtering process is delegated to the underlying {@link RecordStorage}.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @param filters   entity filters
     * @param fieldMask mask to apply to the entities
     * @return all the entities in this repository passed through the filters
     * @see EntityQuery
     */
    @CheckReturnValue
    public Iterator<E> find(EntityFilters filters, FieldMask fieldMask) {
        checkNotNull(filters);
        checkNotNull(fieldMask);

        final EntityQuery<I> entityQuery = EntityQueries.from(filters, getEntityClass());
        final EntityQuery<I> completeQuery = toCompleteQuery(entityQuery);
        final Iterator<EntityRecord> records = recordStorage().readAll(completeQuery, fieldMask);
        final Function<EntityRecord, E> toEntity = entityConverter().reverse();
        final Iterator<E> result = transform(records, toEntity);
        return result;
    }

    /**
     * Obtains iterator over {@link EntityRecord} for entities matching the passed filters.
     *
     * @param filters   the filters for filtering entities
     * @param fieldMask the mask to apply for returned records
     * @return an iterator over the matching records
     */
    @Internal
    public Iterator<EntityRecord> findRecords(EntityFilters filters, FieldMask fieldMask) {
        checkNotNull(filters);
        checkNotNull(fieldMask);

        final EntityQuery<I> entityQuery = EntityQueries.from(filters, getEntityClass());
        final EntityQuery<I> completeQuery = toCompleteQuery(entityQuery);
        return recordStorage().readAll(completeQuery, fieldMask);
    }

    /**
     * Creates an {@link EntityQuery} instance which has:
     * <ul>
     *     <li>All the parameters from the {@code src} Query;
     *     <li>At least one parameter limiting
     *         the {@link io.spine.server.storage.LifecycleFlagField Lifecycle Flags Columns}.
     * </ul>
     *
     * <p>If the {@code src} instance
     * {@linkplain EntityQuery#isLifecycleAttributesSet() contains the lifecycle attributes}, then
     * it is returned with no change. Otherwise - a new instance containing the default values for
     * the Lifecycle attributes is returned.
     *
     * <p>The default values are:
     * <pre>
     *     {@code
     *     archived -> false,
     *     deleted  -> false
     *     }
     * </pre>
     *
     * <p>If the type of the Entity which this repository works with is not derived from
     * the {@link EntityWithLifecycle}, then no lifecycle attributes are appended and
     * the {@code src} query is returned.
     *
     * @param src the source {@link EntityQuery} to take the parameters from
     * @return an {@link EntityQuery} which includes
     *         the {@link io.spine.server.storage.LifecycleFlagField Lifecycle Flags Columns} unless
     *         they are not supported
     */
    private EntityQuery<I> toCompleteQuery(EntityQuery<I> src) {
        final EntityQuery<I> completeQuery;
        if (!src.isLifecycleAttributesSet()
                && EntityWithLifecycle.class.isAssignableFrom(getEntityClass())) {
            @SuppressWarnings("unchecked") // Checked at runtime
            final Class<? extends EntityWithLifecycle<I, ?>> cls =
                    (Class<? extends EntityWithLifecycle<I, ?>>) getEntityClass();
            completeQuery = src.withLifecycleFlags(cls);
        } else {
            completeQuery = src;
        }
        return completeQuery;
    }

    /**
     * Converts the passed entity into the record.
     */
    protected EntityRecordWithColumns toRecord(E entity) {
        final EntityRecord entityRecord = entityConverter().convert(entity);
        checkNotNull(entityRecord);
        final EntityRecordWithColumns recordWithColumns =
                EntityRecordWithColumns.create(entityRecord, entity);
        return recordWithColumns;
    }

    private E toEntity(EntityRecord record) {
        final E result = entityConverter().reverse()
                                          .convert(record);
        return result;
    }

    /**
     * Transforms an instance of {@link EntityId} into an identifier
     * of the required type.
     *
     * @param <I> the target type of identifiers
     */
    @VisibleForTesting
    static class EntityIdFunction<I> implements Function<EntityId, I> {

        private final Class<I> expectedIdClass;

        public EntityIdFunction(Class<I> expectedIdClass) {
            this.expectedIdClass = expectedIdClass;
        }

        @Nullable
        @Override
        public I apply(@Nullable EntityId input) {
            checkNotNull(input);
            final Any idAsAny = input.getId();

            final TypeUrl typeUrl = TypeUrl.ofEnclosed(idAsAny);
            final Class messageClass = typeUrl.getJavaClass();
            checkIdClass(messageClass);

            final Message idAsMessage = unpack(idAsAny);

            @SuppressWarnings("unchecked")
                // As the message class is the same as expected, the conversion is safe.
            final I id = (I) idAsMessage;
            return id;
        }

        private void checkIdClass(Class messageClass) {
            final boolean classIsSame = expectedIdClass.equals(messageClass);
            if (!classIsSame) {
                throw newIllegalStateException("Unexpected ID class encountered: %s. Expected: %s",
                                               messageClass, expectedIdClass);
            }
        }
    }
}
