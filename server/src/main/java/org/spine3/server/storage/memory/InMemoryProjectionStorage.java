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

package org.spine3.server.storage.memory;

import com.google.protobuf.Timestamp;
import org.spine3.server.EntityId;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.ProjectionStorage;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The in-memory implementation of StreamProjectionStorage.
 *
 * @param <I> the type of stream projection IDs. See {@link EntityId} for supported types.
 * @author Alexander Litus
 */
public class InMemoryProjectionStorage<I> extends ProjectionStorage<I> {

    private final InMemoryEntityStorage<I> entityStorage;

    /**
     * The time of the last handled event.
     */
    private Timestamp timestamp;

    public static <I> InMemoryProjectionStorage<I> newInstance(InMemoryEntityStorage<I> entityStorage) {
        return new InMemoryProjectionStorage<>(entityStorage);
    }

    private InMemoryProjectionStorage(InMemoryEntityStorage<I> entityStorage) {
        this.entityStorage = entityStorage;
    }

    @Override
    public void writeLastHandledEventTime(Timestamp timestamp) {
        checkNotNull(timestamp);
        this.timestamp = timestamp;
    }

    @Override
    public Timestamp readLastHandledEventTime() {
        return timestamp;
    }

    @Override
    protected EntityStorage<I> getEntityStorage() {
        return entityStorage;
    }

    /**
     * Clears all data in the storage.
     */
    protected void clear() {
        timestamp = null;
        entityStorage.clear();
    }

    @Override
    public void close() throws Exception {
        clear();
        entityStorage.close();
        super.close();
    }
}
