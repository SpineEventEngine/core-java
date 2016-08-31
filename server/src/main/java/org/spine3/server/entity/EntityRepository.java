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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.BoundedContext;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.storage.Storage;
import org.spine3.server.storage.StorageFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;

import static org.spine3.protobuf.AnyPacker.unpack;
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
     * Finds the entity with the passed ID.
     *
     * <p>As opposed to {@link #load(Object)}, the invocation of this method never leads to creation of a new entity.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @param id the id of the entity to find.
     * @return the entity or {@code null} if there's no entity with such id
     */
    @CheckReturnValue
    @Nullable
    public E find(I id) {
        // TODO[alex.tymchenko]: check whether using #load(id) straightaway is a good idea;
        return this.load(id);
    }


    /**
     * Finds all the entities in this repository with IDs, contained within the passed {@code Iterable}.
     *
     * <p>Provides a convenience wrapper around multiple invocations of {@link #find(Object)}. Descendants may
     * optimize the execution of this method, choosing the most suitable way for the particular storage engine used.
     *
     * <p>The order of resulting objects in the {@link Iterable} is not guaranteed to be the same as the order
     * of IDs passed as argument.
     *
     * <p>The instance of {@code Iterable} is always returned with no {@code null} values.
     *
     * <p>In case IDs contain duplicates, the resulting {@code Iterable} may also contain duplicates, depending
     * on particular implementation.
     *
     * <p>Similar to {@link #find(Object)}, the invocation of this method never leads to creation of new objects.
     *
     * <p>NOTE: The storage must be assigned before calling this method.
     *
     * @param ids entity IDs to search for
     * @return all the entities in this repository with the IDs matching the given {@code Iterable}
     */
    @CheckReturnValue
    public ImmutableCollection<E> findBulk(Iterable<I> ids) {
        final RecordStorage<I> storage = recordStorage();
        final Iterable<EntityStorageRecord> entityStorageRecords = storage.readBulk(ids);

        final Iterator<I> idIterator = ids.iterator();
        final Iterator<EntityStorageRecord> recordIterator = entityStorageRecords.iterator();
        final ImmutableList.Builder<E> builder = ImmutableList.builder();

        while (idIterator.hasNext() && recordIterator.hasNext()) {
            final I id = idIterator.next();
            final EntityStorageRecord record = recordIterator.next();
            final E entity = toEntity(id, record);
            builder.add(entity);
        }

        final ImmutableList<E> result = builder.build();
        return result;

    }

    private E toEntity(I id, EntityStorageRecord record) {
        final E entity = create(id);
        final M state = unpack(record.getState());
        entity.setState(state, record.getVersion(), record.getWhenModified());
        return entity;
    }

    private EntityStorageRecord toEntityRecord(E entity) {
        final M state = entity.getState();
        final Any stateAny = AnyPacker.pack(state);
        final Timestamp whenModified = entity.whenModified();
        final int version = entity.getVersion();
        final EntityStorageRecord.Builder builder = EntityStorageRecord.newBuilder()
                                                                       .setState(stateAny)
                                                                       .setWhenModified(whenModified)
                                                                       .setVersion(version);
        return builder.build();
    }
}
