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

package io.spine.server.storage.memory;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import io.spine.server.inbox.InboxMessage;
import io.spine.server.inbox.InboxMessageId;
import io.spine.server.inbox.InboxReadRequest;
import io.spine.server.inbox.InboxStorage;
import io.spine.server.sharding.ShardIndex;

import java.util.Iterator;
import java.util.Optional;

/**
 * In-memory implementation of messages stored in {@link io.spine.server.inbox.Inbox Inbox}.
 */
public class InMemoryInboxStorage extends InboxStorage {

    private final MultitenantStorage<TenantInboxRecords> multitenantStorage;

    InMemoryInboxStorage(boolean multitenant) {
        super(multitenant);
        this.multitenantStorage = new MultitenantStorage<TenantInboxRecords>(multitenant) {
            @Override
            TenantInboxRecords createSlice() {
                return new TenantInboxRecords();
            }
        };
    }

    @Override
    public Page<InboxMessage> readAll(ShardIndex index, Timestamp from, Timestamp till) {
        TenantInboxRecords storage = multitenantStorage.getStorage();
        ImmutableList<InboxMessage> filtered =
                storage.readAll()
                       .stream()
                       .filter((r) -> index.equals(r.getShardIndex()))
                       .collect(ImmutableList.toImmutableList());
        return new InMemoryPage(filtered);
    }

    @Override
    protected void write(InboxMessage message) {
        multitenantStorage.getStorage()
                          .put(message.getId(), message);
    }

    @Override
    public Iterator<InboxMessageId> index() {
        return multitenantStorage.getStorage()
                                 .index();
    }

    @Override
    public Optional<InboxMessage> read(InboxReadRequest request) {
        return multitenantStorage.getStorage()
                                 .get(request.getRecordId());
    }

    @Override
    public void write(InboxMessageId id, InboxMessage record) {
        multitenantStorage.getStorage()
                          .put(id, record);
    }

    @Override
    public void removeAll(Iterable<InboxMessage> messages) {
        TenantInboxRecords storage = multitenantStorage.getStorage();
        for (InboxMessage message : messages) {
            storage.remove(message);
        }
    }

    /**
     * An in-memory implementation of a page of read operation results.
     *
     * <p>Always contains the whole result set, so {@link #next() next()} always returns
     * {@code Optional.empty()}.
     */
    private static final class InMemoryPage implements Page<InboxMessage> {

        private final ImmutableList<InboxMessage> contents;

        private InMemoryPage(ImmutableList<InboxMessage> contents) {
            this.contents = contents;
        }

        @Override
        public ImmutableList<InboxMessage> contents() {
            return contents;
        }

        @Override
        public int size() {
            return contents.size();
        }

        @Override
        public Optional<Page<InboxMessage>> next() {
            return Optional.empty();
        }
    }
}
