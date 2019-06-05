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

package io.spine.server.sharding;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import io.spine.server.storage.AbstractStorage;
import io.spine.server.storage.ReadRequest;

import java.util.Optional;

/**
 * An abstract class for the {@link io.spine.server.storage.RecordStorage RecordStorage}s, which
 * spread their records across shards.
 *
 * @param <M>
 *         the type of the messages stored
 * @author Alex Tymchenko
 */
public abstract class ShardedStorage<I, M extends ShardedRecord, R extends ReadRequest<I>>
        extends AbstractStorage<I, M, R> {

    protected ShardedStorage(boolean multitenant) {
        super(multitenant);
    }

    /**
     * Reads the records by the given shard index from the storage
     * and returns the first page of the results.
     *
     * @param index
     *         the shard index to return the results for
     * @return the first page of the results
     */
    public abstract Page<M> readAll(ShardIndex index, Timestamp from, Timestamp till);

    public abstract void removeAll(Iterable<M> messages);

    /**
     * A page of messages obtained from a sharded storage in a read operation.
     *
     * @param <M>
     *         the type of the messages stored
     */
    public interface Page<M extends ShardedRecord> {

        /**
         * Obtains the messages of this page.
         *
         * @return the list of page contents
         */
        ImmutableList<M> contents();

        /**
         * Obtains the size of this page.
         *
         * @return non-negative number of items in this page
         */
        int size();

        /**
         * Obtains the next page.
         *
         * @return the next page wrapped into {@code Optional}, or {@code Optional.empty()} if
         *         this page is the last one
         */
        Optional<Page<M>> next();
    }
}
