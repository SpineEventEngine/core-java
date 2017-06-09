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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.server.entity.storage.EntityQueries;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.type.TypeUrl;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
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

    /** {@inheritDoc} */
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
        final boolean recordVisible = !isEntityVisible().apply(record.getLifecycleFlags());
        if (recordVisible) {
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
        final Optional<EntityRecord> found = storage.read(id);
        if (!found.isPresent()) {
            return Optional.absent();
        }
        final EntityRecord record = found.get();
        return Optional.of(record);
    }

    /**
     * Obtains a record for an entity with the passed ID.
     *
     * <p>If there is no such entity yet, it is created and {@link EntityRecord} for this entity is
     * returned. The new entity is not stored as the result of this call.
     *
     * <p>This method is used internally and should not be called by the application code directly.
     *
     * @param id the ID of the entity
     * @return loaded or created {@link EntityRecord}
     */
    @Internal
    @CheckReturnValue
    public EntityRecord findOrCreateRecord(I id) {
        final E entity = findOrCreate(id);
        final EntityRecordWithColumns recordWithColumns = toRecord(entity);
        return recordWithColumns.getRecord();
    }

    /**
     * Loads an entity by the passed ID or creates a new one, if the entity was not found.
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

    @Override
    public Iterator<E> iterator(Predicate<E> filter) {
        final Iterable<E> allEntities = loadAll();
        final Iterator<E> result = FluentIterable.from(allEntities)
                                                 .filter(filter)
                                                 .iterator();
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
     * <p>In case IDs contain duplicates, the result may also contain duplicates,
     * depending on particular implementation.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @param ids entity IDs to search for
     * @return all the entities in this repository with the IDs matching the given {@code Iterable}
     */
    @CheckReturnValue
    public ImmutableCollection<E> loadAll(Iterable<I> ids) {
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
    public ImmutableCollection<E> loadAll(Iterable<I> ids, FieldMask fieldMask) {
        final RecordStorage<I> storage = recordStorage();
        final Iterable<EntityRecord> entityStorageRecords = storage.readMultiple(ids);

        final Iterator<EntityRecord> recordIterator = entityStorageRecords.iterator();
        final List<E> entities = Lists.newLinkedList();
        final EntityStorageConverter<I, E, S> converter =
                entityConverter().withFieldMask(fieldMask);

        while (recordIterator.hasNext()) {
            final EntityRecord record = recordIterator.next();

            if (record == null) { /*    Record is nullable here since `RecordStorage.findBulk()`  *
                                   *    returns an `Iterable` that may contain nulls.             */
                continue;
            }

            final E entity = converter.reverse()
                                      .convert(record);
            entities.add(entity);
        }

        return ImmutableList.copyOf(entities);
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
    public ImmutableCollection<E> loadAll() {
        final RecordStorage<I> storage = recordStorage();
        final Map<I, EntityRecord> recordMap = storage.readAll();

        final ImmutableCollection<E> entities =
                FluentIterable.from(recordMap.entrySet())
                              .transform(storageRecordToEntity())
                              .toList();
        return entities;
    }

    /**
     * Finds all the entities passing the given filters and
     * applies the given {@link FieldMask} to the results.
     *
     * <p>Field mask is applied according to <a href="https://goo.gl/tW5wIU">FieldMask specs</a>.
     *
     * <p>The field paths in the Column field filters are specified to contain a single path
     * member - the name of the Entity Column.
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
    public ImmutableCollection<E> find(EntityFilters filters, FieldMask fieldMask) {
        final Map<I, EntityRecord> records = findRecords(filters, fieldMask);
        final ImmutableCollection<E> result = FluentIterable.from(records.entrySet())
                                                            .transform(storageRecordToEntity())
                                                            .toList();
        return result;
    }

    public Map<I, EntityRecord> findRecords(EntityFilters filters, FieldMask fieldMask) {
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
     * Creates a function that transforms a {@code EntityStorageRecord} stored in a map
     * into an entity of type {@code <E>}.
     *
     * @return new instance of the transforming function
     */
    protected final Function<Map.Entry<I, EntityRecord>, E> storageRecordToEntity() {
        return new Function<Map.Entry<I, EntityRecord>, E>() {
            @Nullable
            @Override
            public E apply(@Nullable Map.Entry<I, EntityRecord> input) {
                checkNotNull(input);
                final E result = toEntity(input.getValue());
                return result;
            }
        };
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

            // As the message class is the same as expected, the conversion is safe.
            @SuppressWarnings("unchecked")
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
