/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.client.EntityIdFilter;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.BoundedContext;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.storage.Storage;
import org.spine3.server.storage.StorageFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.protobuf.Messages.toMessageClass;
import static org.spine3.validate.Validate.isDefault;

/**
 * The base class for repositories managing entities.
 *
 * @param <I> the type of IDs of entities
 * @param <E> the type of entities
 * @param <M> the type of entity state messages
 * @author Alexander Yevsyukov
 */
public abstract class EntityRepository<I, E extends Entity<I, M>, M extends Message> extends Repository<I, E> {

    public EntityRepository(BoundedContext boundedContext) {
        super(boundedContext);
    }

    /** {@inheritDoc} */
    @Override
    protected Storage createStorage(StorageFactory factory) {
        final Storage result = factory.createRecordStorage(getEntityClass());
        return result;
    }

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
    public void store(E entity) {
        final RecordStorage<I> storage = recordStorage();
        final EntityStorageRecord record = toEntityRecord(entity);
        storage.write(entity.getId(), record);
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public E load(I id) {
        final RecordStorage<I> storage = recordStorage();
        final EntityStorageRecord record = storage.read(id);
        if (isDefault(record)) {
            return null;
        }
        final E entity = toEntity(id, record);
        return entity;
    }

    /**
     * Loads all the entities in this repository with IDs, contained within the passed {@code ids} values.
     *
     * <p>Provides a convenience wrapper around multiple invocations of {@link #load(Object)}. Descendants may
     * optimize the execution of this method, choosing the most suitable way for the particular storage engine used.
     *
     * <p>The result only contains those entities which IDs are contained inside the passed {@code ids}.
     * The resulting collection is always returned with no {@code null} values.
     *
     * <p>The order of objects in the result is not guaranteed to be the same as the order of IDs passed as argument.
     *
     * <p>In case IDs contain duplicates, the result may also contain duplicates, depending on particular
     * implementation.
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
     * Loads all the entities in this repository by their IDs and applies the {@link FieldMask} to each of them.
     *
     * <p>Acts in the same way as {@link #loadAll(Iterable)}, with the {@code FieldMask} applied to the results.
     *
     * <p>Field mask is applied according to
     * <a href="https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#google.protobuf.FieldMask>FieldMask specs</a>.
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
        final Iterable<EntityStorageRecord> entityStorageRecords = storage.readMultiple(ids);

        final Iterator<I> idIterator = ids.iterator();
        final Iterator<EntityStorageRecord> recordIterator = entityStorageRecords.iterator();
        final List<E> entities = new LinkedList<>();

        while (idIterator.hasNext() && recordIterator.hasNext()) {
            final I id = idIterator.next();
            final EntityStorageRecord record = recordIterator.next();

            if (record == null) { /*    Record is nullable here since {@code RecordStorage#findBulk}    *
                                   *    returns an {@code Iterable} that may contain nulls.             */
                continue;
            }

            final E entity = toEntity(id, record, fieldMask);
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
        final Map<I, EntityStorageRecord> recordMap = storage.readAll();

        final ImmutableCollection<E> entities =
                FluentIterable.from(recordMap.entrySet())
                              .transform(storageRecordToEntityTransformer())
                              .toList();
        return entities;
    }

    /**
     * Finds all the entities passing the given filters and applies the given {@link FieldMask} to the results.
     *
     * <p>Field mask is applied according to
     * <a href="https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#google.protobuf.FieldMask>FieldMask specs</a>.
     *
     * <p>At this point only {@link EntityIdFilter} is supported. All other filters are ignored.
     *
     * <p>Filtering by IDs set via {@code EntityIdFilter} is performed in the same way as by {@link #loadAll(Iterable)}.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @param filters   entity filters
     * @param fieldMask mask to apply to the entities
     * @return all the entities in this repository passed the filters.
     */
    @CheckReturnValue
    public ImmutableCollection<E> find(EntityFilters filters, FieldMask fieldMask) {
        final List<EntityId> idsList = filters.getIdFilter()
                                              .getIdsList();
        final Class<I> expectedIdClass = getIdClass();

        final Collection<I> domainIds = Collections2.transform(idsList, new Function<EntityId, I>() {
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
                    final String errMsg = String.format("Unexpected ID class encountered: %s. Expected: %s",
                                                        messageClass, expectedIdClass);
                    throw new IllegalStateException(errMsg);
                }
            }
        });

        final ImmutableCollection<E> result = loadAll(domainIds, fieldMask);
        return result;
    }

    private E toEntity(I id, EntityStorageRecord record) {
        return toEntity(id, record, FieldMask.getDefaultInstance());
    }

    private E toEntity(I id, EntityStorageRecord record, FieldMask fieldMask) {
        final E entity = create(id);
        final Message unpacked = unpack(record.getState());
        final TypeUrl entityStateType = getEntityStateType();
        @SuppressWarnings("unchecked")
        final M state = (M) FieldMasks.applyMask(fieldMask, unpacked, entityStateType);
        entity.setState(state, record.getVersion(), record.getWhenModified());
        return entity;
    }

    private EntityStorageRecord toEntityRecord(E entity) {
        final M state = entity.getState();
        final Any stateAny = pack(state);
        final Timestamp whenModified = entity.whenModified();
        final int version = entity.getVersion();
        final EntityStorageRecord.Builder builder = EntityStorageRecord.newBuilder()
                                                                       .setState(stateAny)
                                                                       .setWhenModified(whenModified)
                                                                       .setVersion(version);
        return builder.build();
    }

    private Function<Map.Entry<I, EntityStorageRecord>, E> storageRecordToEntityTransformer() {
        return new Function<Map.Entry<I, EntityStorageRecord>, E>() {
            @Nullable
            @Override
            public E apply(@Nullable Map.Entry<I, EntityStorageRecord> input) {
                checkNotNull(input);
                final E result = toEntity(input.getKey(), input.getValue());
                return result;
            }
        };
    }
}
