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
import org.spine3.server.storage.StreamProjectionStorage;

/**
 * The in-memory implementation of StreamProjectionStorage.
 *
 * @param <I> the type of stream projection IDs. See {@link EntityId} for supported types.
 * @author Alexander Litus
 */
public class InMemoryStreamProjectionStorage<I> extends StreamProjectionStorage<I> {

    private final EntityStorage<I> entityStorage;

    /**
     * The time of the last handled event.
     */
    private Timestamp timestamp;

    public static <I> StreamProjectionStorage<I> newInstance(EntityStorage<I> entityStorage) {
        return new InMemoryStreamProjectionStorage<>(entityStorage);
    }

    private InMemoryStreamProjectionStorage(EntityStorage<I> entityStorage) {
        this.entityStorage = entityStorage;
    }

    @Override
    public void writeLastHandledEventTime(Timestamp timestamp) {
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
}
