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
import io.spine.logging.Logging;
import io.spine.server.delivery.Inbox;
import io.spine.server.delivery.InboxMessage;
import io.spine.server.delivery.InboxMessageId;
import io.spine.server.delivery.InboxMessageStatus;
import io.spine.server.delivery.InboxReadRequest;
import io.spine.server.delivery.InboxStorage;
import io.spine.server.delivery.Page;
import io.spine.server.delivery.ShardIndex;
import io.spine.validate.Validated;

import java.util.Iterator;
import java.util.Optional;

import static com.google.protobuf.util.Timestamps.compare;

/**
 * In-memory implementation of messages stored in {@link Inbox Inbox}.
 */
public class InMemoryInboxStorage extends InboxStorage implements Logging {

    private final MultitenantStorage<TenantInboxRecords> multitenantStorage;

    public InMemoryInboxStorage(boolean multitenant) {
        super(multitenant);
        this.multitenantStorage = new MultitenantStorage<TenantInboxRecords>(multitenant) {
            @Override
            TenantInboxRecords createSlice() {
                return new TenantInboxRecords();
            }
        };
    }

    @Override
    public Page<InboxMessage> contentsBackwards(ShardIndex index) {
        TenantInboxRecords storage = multitenantStorage.currentSlice();
        ImmutableList<InboxMessage> filtered =
                storage.readAll()
                       .stream()
                       .filter((r) -> index.equals(r.getShardIndex()))
                       .sorted((m1, m2) -> compare(m2.getWhenReceived(), m1.getWhenReceived()))
                       .collect(ImmutableList.toImmutableList());

        return new InMemoryPage(filtered);
    }

    @Override
    public void write(InboxMessage message) {
        multitenantStorage.currentSlice()
                          .put(message.getId(), message);
    }

    @Override
    public void markDelivered(Iterable<InboxMessage> messages) {
        for (InboxMessage message : messages) {
            @Validated InboxMessage updated = message.toBuilder()
                                                      .setStatus(InboxMessageStatus.DELIVERED)
                                                      .vBuild();
            write(updated);
        }
    }

    @Override
    public Iterator<InboxMessageId> index() {
        return multitenantStorage.currentSlice()
                                 .index();
    }

    @Override
    public Optional<InboxMessage> read(InboxReadRequest request) {
        return multitenantStorage.currentSlice()
                                 .get(request.recordId());
    }

    @Override
    public void write(InboxMessageId id, InboxMessage record) {
        multitenantStorage.currentSlice()
                          .put(id, record);
    }

    @Override
    public void removeAll(Iterable<InboxMessage> messages) {
        TenantInboxRecords storage = multitenantStorage.currentSlice();
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
