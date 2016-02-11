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

package org.spine3.server.storage;

import com.google.protobuf.Timestamp;
import org.spine3.SPI;
import org.spine3.server.EntityId;
import org.spine3.server.projection.Projection;

import javax.annotation.Nullable;

/**
 * The storage used by projection repositories for keeping {@link Projection}s
 * and the timestamp of the last event processed by the projection repository.
 *
 * <p>This timestamp is used for 'catch-up' operation of the projection repositories.
 *
 * @param <I> the type of stream projection IDs. See {@link EntityId} for supported types.
 * @author Alexander Litus
 */
@SPI
public abstract class ProjectionStorage<I> extends AbstractStorage<I, EntityStorageRecord> {

    @Nullable
    @Override
    public EntityStorageRecord read(I id) {
        checkNotClosed();

        final EntityStorage<I> storage = getEntityStorage();
        final EntityStorageRecord record = storage.read(id);
        return record;
    }

    @Override
    public void write(I id, EntityStorageRecord record) {
        checkNotClosed();

        final EntityStorage<I> storage = getEntityStorage();
        storage.write(id, record);
    }

    /**
     * Writes the time of the last handled event to the storage.
     *
     * @param time the time of the event
     */
    public abstract void writeLastHandledEventTime(Timestamp time);

    /**
     * Reads the time of the last handled event from the storage.
     *
     * @return the time of the last event or {@code null} if there is no event in the storage
     */
    @Nullable
    public abstract Timestamp readLastHandledEventTime();

    /**
     * Returns an entity storage implementation.
     */
    public abstract EntityStorage<I> getEntityStorage();
}
