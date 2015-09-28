/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Message;
import com.google.protobuf.util.TimeUtil;
import org.spine3.server.storage.EntityStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * The base class for repositories managing entities.
 *
 * @param <I> the type of IDs of entities
 * @param <E> the type of entities
 * @param <M> the type of entity state messages
 * @author Alexander Yevsyukov
 */
public class EntityRepository<I, E extends Entity<I, M>, M extends Message> extends RepositoryBase<I, E> {

    @Nullable
    @Override
    protected EntityStorage<I, M> getStorage() {
        @SuppressWarnings("unchecked") // It is safe to cast as we check the type in checkStorageClass().
        final EntityStorage<I, M> storage = (EntityStorage<I, M>) super.getStorage();
        return storage;
    }

    @Override
    public void store(E obj) {
        final M state = obj.getState();
        checkArgument(state != null, "Entity does not have state.");
        final EntityStorage<I, M> storage = checkStorage();
        storage.write(obj.getId(), state);
    }

    @Nullable
    @Override
    public E load(I id) {
        EntityStorage<I, M> storage = checkStorage();

        M entityState = storage.read(id);
        if (entityState != null) {
            E result = create(id);

            //TODO:2015-09-28:alexander.yevsyukov: What do we pass for version and timestamp?
            // Presumably EntityStorage should store these two fields and set it here.

            result.setState(entityState, 0, TimeUtil.getCurrentTime());
            return result;
        }
        // No state stored -> no entity.
        return null;
    }

    @Nonnull
    private EntityStorage<I, M> checkStorage() {
        EntityStorage<I, M> storage = getStorage();
        checkState(storage != null, "Storage not assigned");
        return storage;
    }

    /**
     * Casts the passed object to {@link EntityStorage}.
     *
     * @param storage the instance of storage to check
     * @throws ClassCastException if the object is not of the required class
     */
    protected void checkStorageClass(Object storage) {
        @SuppressWarnings({"unused", "unchecked"}) EntityStorage<I, M> ignored = (EntityStorage<I, M>) storage;
    }

}
