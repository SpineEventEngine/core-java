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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.server.BoundedContext;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.storage.Storage;
import org.spine3.server.storage.StorageFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
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
        final Storage result = factory.createEntityStorage(getEntityClass());
        return result;
    }

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException if the storage is null
     */
    @Nonnull
    protected EntityStorage<I> entityStorage() {
        @SuppressWarnings("unchecked") // It is safe to cast as we control the creation in createStorage().
        final EntityStorage<I> storage = (EntityStorage<I>) getStorage();
        return checkStorage(storage);
    }

    /** {@inheritDoc} */
    @Override
    public void store(E entity) {
        final EntityStorage<I> storage = entityStorage();
        final EntityStorageRecord record = toEntityRecord(entity);
        storage.write(entity.getId(), record);
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public E load(I id) {
        final EntityStorage<I> storage = entityStorage();
        final EntityStorageRecord record = storage.read(id);
        if (isDefault(record)) {
            return null;
        }
        final E entity = toEntity(id, record);
        return entity;
    }

    private E toEntity(I id, EntityStorageRecord record) {
        final E entity = create(id);
        final M state = fromAny(record.getState());
        entity.setState(state, record.getVersion(), record.getWhenModified());
        return entity;
    }

    private EntityStorageRecord toEntityRecord(E entity) {
        final M state = entity.getState();
        final Any stateAny = toAny(state);
        final Timestamp whenModified = entity.whenModified();
        final int version = entity.getVersion();
        final EntityStorageRecord.Builder builder = EntityStorageRecord.newBuilder()
                .setState(stateAny)
                .setWhenModified(whenModified)
                .setVersion(version);
        return builder.build();
    }
}
