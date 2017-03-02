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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.BoundedContext;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.storage.Storage;
import org.spine3.server.storage.StorageFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.protobuf.Messages.toMessageClass;
import static org.spine3.server.entity.EntityStorageConverter.tuple;

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
    protected RecordBasedRepository(BoundedContext boundedContext) {
        super(boundedContext);
    }

    /** {@inheritDoc} */
    @Override
    protected Storage createStorage(StorageFactory factory) {
        final Storage result = factory.createRecordStorage(getEntityClass());
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
    @Nonnull
    protected RecordStorage<I> recordStorage() {
        @SuppressWarnings("unchecked") // It is safe to cast as we control the creation in createStorage().
        final RecordStorage<I> storage = (RecordStorage<I>) getStorage();
        return checkStorage(storage);
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
        final RecordStorage<I> storage = recordStorage();
        final EntityRecord record = toRecord(entity);
        storage.write(entity.getId(), record);
    }

    /** {@inheritDoc} */
    @Override
    @CheckReturnValue
    public Optional<E> load(I id) {
        final RecordStorage<I> storage = recordStorage();
        final Optional<EntityRecord> found = storage.read(id);
        if (!found.isPresent()) {
            return Optional.absent();
        }
        final EntityRecord record = found.get();
        if (!Predicates.isEntityVisible().apply(record.getVisibility())) {
            return Optional.absent();
        }
        final E entity = toEntity(id, record);
        return Optional.of(entity);
    }

    /**
     * Loads an entity by the passed ID or creates a new one, if the entity was not found.
     */
    @CheckReturnValue
    protected E loadOrCreate(I id) {
        final Optional<E> loaded = load(id);

        if (!loaded.isPresent()) {
            final E result = create(id);
            return result;
        }

        final E result = loaded.get();
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected void markArchived(I id) {
        final RecordStorage<I> storage = recordStorage();
        storage.markArchived(id);
    }

    /** {@inheritDoc} */
    @Override
    protected void markDeleted(I id) {
        final RecordStorage<I> storage = recordStorage();
        storage.markDeleted(id);
    }

    /**
     * Loads all the entities in this repository with IDs,
     * contained within the passed {@code ids} values.
     *
     * <p>Provides a convenience wrapper around multiple invocations of
     * {@link #load(Object)}. Descendants may optimize the execution of this
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
     * <p>Field mask is applied according to
     * <a
     *  href="https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#google.protobuf.FieldMask"
     * >FieldMask specs</a>.
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

        final Iterator<I> idIterator = ids.iterator();
        final Iterator<EntityRecord> recordIterator = entityStorageRecords.iterator();
        final List<E> entities = Lists.newLinkedList();
        final EntityStorageConverter<I, E, S> converter = entityConverter().withFieldMask(fieldMask);

        while (idIterator.hasNext() && recordIterator.hasNext()) {
            final I id = idIterator.next();
            final EntityRecord record = recordIterator.next();

            if (record == null) { /*    Record is nullable here since `RecordStorage.findBulk()`  *
                                   *    returns an `Iterable` that may contain nulls.             */
                continue;
            }

            final EntityStorageConverter.Tuple<I> tuple = tuple(id, record);
            final E entity = converter.reverse()
                                       .convert(tuple);
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
     * <p>At this point only {@link org.spine3.client.EntityIdFilter EntityIdFilter} is supported.
     * All other filters are ignored.
     *
     * <p>Filtering by IDs set via {@code EntityIdFilter} is performed
     * in the same way as by {@link #loadAll(Iterable)}.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @param filters   entity filters
     * @param fieldMask mask to apply to the entities
     * @return all the entities in this repository passed the filters.
     */
    @CheckReturnValue
    public ImmutableCollection<E> find(EntityFilters filters, FieldMask fieldMask) {
        final Collection<I> domainIds = unpackIds(filters);
        final ImmutableCollection<E> result = loadAll(domainIds, fieldMask);
        return result;
    }

    /**
     * Extracts entity IDs from the passed filters.
     */
    private Collection<I> unpackIds(EntityFilters filters) {
        final List<EntityId> idsList = filters.getIdFilter()
                                              .getIdsList();
        final Class<I> expectedIdClass = getIdClass();

        final Collection<I> result = Collections2.transform(idsList,
                                                            new EntityIdFunction<>(expectedIdClass));

        return result;
    }

    /**
     * Converts the passed entity into {@code EntityStorageRecord} that
     * stores the entity data.
     */
    protected EntityRecord toRecord(E entity) {
        final EntityStorageConverter.Tuple<I> tuple = entityConverter().convert(entity);
        return tuple != null ? tuple.getState()
                             : EntityRecord.getDefaultInstance();
    }

    private E toEntity(I id, EntityRecord record) {
        EntityStorageConverter.Tuple<I> tuple = tuple(id, record);
        final E result = entityConverter().reverse()
                                          .convert(tuple);
        return result;
    }

    /**
     * Creates a function that transforms a {@code EntityStorageRecord} stored in a map
     * into an entity of type {@code <E>}.
     *
     * @return new instance of the transforming function
     */
    private Function<Map.Entry<I, EntityRecord>, E> storageRecordToEntity() {
        return new Function<Map.Entry<I, EntityRecord>, E>() {
            @Nullable
            @Override
            public E apply(@Nullable Map.Entry<I, EntityRecord> input) {
                checkNotNull(input);
                final E result = toEntity(input.getKey(), input.getValue());
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
            final Class messageClass = toMessageClass(typeUrl);
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
                final String errMsg = format(
                        "Unexpected ID class encountered: %s. Expected: %s",
                        messageClass, expectedIdClass
                );
                throw new IllegalStateException(errMsg);
            }
        }
    }
}
