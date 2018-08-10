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
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.EntityColumnCache;
import io.spine.server.entity.storage.EntityQueries;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.LifecycleFlagField;
import io.spine.server.storage.RecordReadRequest;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

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
        RecordStorage<I> storage = (RecordStorage<I>) getStorage();
        return storage;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Caches {@link Column} definitions of the {@link Entity} class managed by this repository.
     */
    @Override
    @OverridingMethodsMustInvokeSuper
    public void onRegistered() {
        super.onRegistered();

        cacheEntityColumns();
    }

    @Override
    public E create(I id) {
        E result = entityFactory().create(id);
        return result;
    }

    @Override
    public void store(E entity) {
        EntityRecordWithColumns record = toRecord(entity);
        RecordStorage<I> storage = recordStorage();
        storage.write(entity.getId(), record);
    }

    @Override
    public Iterator<E> iterator(Predicate<E> filter) {
        Iterator<E> allEntities = loadAll();
        Iterator<E> result = filter(allEntities, filter::test);
        return result;
    }

    @Override
    protected RecordStorage<I> createStorage(StorageFactory factory) {
        RecordStorage<I> result = factory.createRecordStorage(getEntityClass());
        return result;
    }

    /**
     * Stores {@linkplain Entity Entities} in bulk.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @param entities the {@linkplain Entity Entities} to store
     */
    public void store(Collection<E> entities) {
        Map<I, EntityRecordWithColumns> records = newHashMapWithExpectedSize(entities.size());
        for (E entity : entities) {
            EntityRecordWithColumns recordWithColumns = toRecord(entity);
            records.put(entity.getId(), recordWithColumns);
        }
        recordStorage().write(records);
    }

    /**
     * Finds an entity with the passed ID if this entity is
     * {@linkplain EntityWithLifecycle.Predicates#isEntityVisible() visible}.
     *
     * @param id the ID of the entity to find
     * @return the entity or {@link Optional#empty()} if there is no entity with such ID
     * or this entity is not visible
     */
    @Override
    public Optional<E> find(I id) {
        Optional<EntityRecord> optional = findRecord(id);
        if (!optional.isPresent()) {
            return Optional.empty();
        }
        EntityRecord record = optional.get();
        boolean recordVisible = isEntityVisible().test(record.getLifecycleFlags());
        if (!recordVisible) {
            return Optional.empty();
        }
        E entity = toEntity(record);
        return Optional.of(entity);
    }

    /**
     * Finds a record and returns it if its {@link LifecycleFlags} don't make it
     * {@linkplain EntityWithLifecycle.Predicates#isEntityVisible()}.
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
     * <p>An entity will be loaded despite its {@linkplain LifecycleFlags visibility}.
     * I.e. even if the entity is either {@linkplain EntityWithLifecycle#isArchived()  archived} or
     * {@linkplain EntityWithLifecycle#isDeleted() deleted}, it is loaded and returned.
     *
     * <p>The new entity is created if and only if there is no record with the corresponding ID.
     *
     * @param id the ID of the entity to load
     * @return the entity with the specified ID
     */
    protected E findOrCreate(I id) {
        Optional<EntityRecord> optional = findRecord(id);
        if (!optional.isPresent()) {
            return create(id);
        }
        EntityRecord record = optional.get();
        E entity = toEntity(record);
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
    public Iterator<E> loadAll(Iterable<I> ids, FieldMask fieldMask) {
        RecordStorage<I> storage = recordStorage();
        Iterator<EntityRecord> entityStorageRecords = storage.readMultiple(ids, fieldMask);
        Iterator<EntityRecord> presentRecords = filter(entityStorageRecords, Objects::nonNull);
        Function<EntityRecord, E> toEntity = entityConverter().reverse();
        Iterator<E> result = transform(presentRecords, toEntity::apply);
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
    public Iterator<E> loadAll() {
        RecordStorage<I> storage = recordStorage();
        Iterator<EntityRecord> records = storage.readAll();
        Function<EntityRecord, E> toEntity = entityConverter().reverse();
        Iterator<E> result = transform(records, toEntity::apply);
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
    public Iterator<E> find(EntityFilters filters, FieldMask fieldMask) {
        checkNotNull(filters);
        checkNotNull(fieldMask);

        EntityQuery<I> entityQuery = EntityQueries.from(filters, recordStorage());
        EntityQuery<I> completeQuery = toCompleteQuery(entityQuery);
        Iterator<EntityRecord> records = recordStorage().readAll(completeQuery, fieldMask);
        Function<EntityRecord, E> toEntity = entityConverter().reverse();
        Iterator<E> result = transform(records, toEntity::apply);
        return result;
    }

    /**
     * Obtains iterator over {@link EntityRecord} for entities matching the passed filters.
     *
     * @param filters   the filters for filtering entities
     * @param fieldMask the mask to apply for returned records
     * @return an iterator over the matching records
     */
    @SuppressWarnings("WeakerAccess") // Used in the code outside of the repository.
    @Internal
    public Iterator<EntityRecord> findRecords(EntityFilters filters, FieldMask fieldMask) {
        checkNotNull(filters);
        checkNotNull(fieldMask);

        EntityQuery<I> entityQuery = EntityQueries.from(filters, recordStorage());
        EntityQuery<I> completeQuery = toCompleteQuery(entityQuery);
        return recordStorage().readAll(completeQuery, fieldMask);
    }

    /**
     * Creates an {@link EntityQuery} instance which has:
     * <ul>
     *     <li>All the parameters from the {@code src} Query;
     *     <li>At least one parameter limiting
     *         the {@link LifecycleFlagField Lifecycle Flags Columns}.
     * </ul>
     *
     * <p>If the {@code src} instance
     * {@linkplain EntityQuery#isLifecycleAttributesSet() contains the lifecycle attributes}, then
     * it is returned with no change. Otherwise, a new instance containing the default values for
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
     *         the {@link LifecycleFlagField Lifecycle Flags Columns} unless
     *         they are not supported
     */
    private EntityQuery<I> toCompleteQuery(EntityQuery<I> src) {
        EntityQuery<I> completeQuery;
        completeQuery = !src.isLifecycleAttributesSet()
                                && EntityWithLifecycle.class.isAssignableFrom(getEntityClass())
                        ? src.withLifecycleFlags(recordStorage())
                        : src;
        return completeQuery;
    }

    /**
     * Converts the passed entity into the record.
     */
    protected EntityRecordWithColumns toRecord(E entity) {
        EntityRecord entityRecord = entityConverter().convert(entity);
        checkNotNull(entityRecord);
        EntityRecordWithColumns result =
                EntityRecordWithColumns.create(entityRecord, entity, recordStorage());
        return result;
    }

    private E toEntity(EntityRecord record) {
        E result = entityConverter().reverse()
                                    .convert(record);
        checkNotNull(result);
        return result;
    }

    /**
     * Retrieves the {@link EntityColumnCache} used by this repository's
     * {@linkplain RecordStorage storage}.
     *
     * @return the entity column cache from the storage
     * @throws IllegalStateException
     *         if the {@link EntityColumnCache} is not supported by this repository's storage
     */
    private EntityColumnCache columnCache() {
        return recordStorage().entityColumnCache();
    }

    /**
     * Caches {@link Column} definitions of the {@link Entity} class managed by this repository.
     *
     * <p>The process of caching columns also acts as a check of {@link Column} definitions,
     * because {@linkplain Column columns} with incorrect definitions cannot be retrieved and
     * stored.
     *
     * <p>If {@link Column} definitions are incorrect, the {@link IllegalStateException} is thrown.
     *
     * @throws IllegalStateException in case entity column definitions are incorrect
     */
    private void cacheEntityColumns() {
        columnCache().ensureColumnsCached();
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

        @Override
        public @Nullable I apply(@Nullable EntityId input) {
            checkNotNull(input);
            Any idAsAny = input.getId();

            TypeUrl typeUrl = TypeUrl.ofEnclosed(idAsAny);
            Class messageClass = typeUrl.getJavaClass();
            checkIdClass(messageClass);

            Message idAsMessage = unpack(idAsAny);

            @SuppressWarnings("unchecked") /* As the message class is the same as expected,
                                              the conversion is safe. */
            I id = (I) idAsMessage;
            return id;
        }

        private void checkIdClass(Class messageClass) {
            boolean classIsSame = expectedIdClass.equals(messageClass);
            if (!classIsSame) {
                throw newIllegalStateException("Unexpected ID class encountered: %s. Expected: %s",
                                               messageClass, expectedIdClass);
            }
        }
    }
}
