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

package org.spine3.server;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.EntityStorageRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.server.storage.EntityStorage.toRecordId;

/**
 * The base class for repositories managing entities.
 *
 * @param <I> the type of IDs of entities
 * @param <E> the type of entities
 * @param <M> the type of entity state messages
 * @author Alexander Yevsyukov
 */
public class EntityRepository<I, E extends Entity<I, M>, M extends Message> extends Repository<I, E> {

    /**
     * {@inheritDoc}
     */
    public EntityRepository(BoundedContext boundedContext) {
        super(boundedContext);
    }

    @Nullable
    @Override
    protected EntityStorage<I> getStorage() {
        @SuppressWarnings("unchecked") // It is safe to cast as we check the type in checkStorageClass().
        final EntityStorage<I> storage = (EntityStorage<I>) super.getStorage();
        return storage;
    }

    @Override
    public void store(E entity) {
        final EntityStorage<I> storage = checkStorage();
        final EntityStorageRecord record = toEntityRecord(entity);
        storage.write(record);
    }

    @Nullable
    @Override
    public E load(I id) {
        final EntityStorage<I> storage = checkStorage();
        final EntityStorageRecord record = storage.read(id);
        if (record == null) {
            return null;
        }
        final E entity = toEntity(id, record);
        return entity;
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private E toEntity(I id, EntityStorageRecord record) {
        final E entity = create(id);
        final M state = fromAny(record.getState());
        entity.setState(state, record.getVersion(), record.getWhenModified());
        return entity;
    }

    private EntityStorageRecord toEntityRecord(E entity) {
        final I id = entity.getId();
        final EntityStorageRecord.Id entityId = toRecordId(id);
        final M state = entity.getState();
        final Any stateAny = toAny(state);
        final Timestamp whenModified = entity.whenModified();
        final int version = entity.getVersion();
        final EntityStorageRecord.Builder builder = EntityStorageRecord.newBuilder()
                .setState(stateAny)
                .setId(entityId)
                .setWhenModified(whenModified)
                .setVersion(version);
        return builder.build();
    }

    @Nonnull
    private EntityStorage<I> checkStorage() {
        final EntityStorage<I> storage = getStorage();
        checkState(storage != null, "Storage not assigned");
        return storage;
    }

    /**
     * Casts the passed object to {@link EntityStorage}.
     *
     * @param storage the instance of storage to check
     * @throws ClassCastException if the object is not of the required class
     */
    @Override
    protected void checkStorageClass(Object storage) {
        @SuppressWarnings({"unused", "unchecked"})
        final EntityStorage<I> ignored = (EntityStorage<I>) storage;
    }
}
