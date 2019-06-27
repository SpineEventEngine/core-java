/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import io.spine.annotation.SPI;
import io.spine.server.storage.AbstractStorage;
import io.spine.server.storage.ReadRequest;

/**
 * An abstract class for the {@link io.spine.server.storage.RecordStorage RecordStorage}s, which
 * spread their records across shards.
 *
 * @param <M>
 *         the type of the messages stored
 */
@SPI
public abstract class ShardedStorage<I, M extends ShardedRecord, R extends ReadRequest<I>>
        extends AbstractStorage<I, M, R> {

    @SuppressWarnings("WeakerAccess")   // A part of the API for extensions.
    protected ShardedStorage(boolean multitenant) {
        super(multitenant);
    }

    /**
     * Reads the contents of the storage by the given shard index and returns the first page
     * of the results.
     *
     * <p>The older items go first.
     *
     * @param index
     *         the shard index to return the results for
     * @return the first page of the results
     */
    public abstract Page<M> readAll(ShardIndex index);

    /**
     * Removes the passed messages from the storage.
     *
     * <p>Does nothing for messages that aren't in the storage already.
     *
     * @param messages
     *         the messages to remove
     */
    public abstract void removeAll(Iterable<M> messages);
}
