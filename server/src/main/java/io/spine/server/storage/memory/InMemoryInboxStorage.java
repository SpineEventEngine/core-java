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
import io.spine.server.storage.AbstractStorage;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.protobuf.util.Timestamps.compare;
import static java.util.stream.Collectors.groupingBy;

/**
 * In-memory implementation of messages stored in {@link Inbox Inbox}.
 */
public final class InMemoryInboxStorage
        extends AbstractStorage<InboxMessageId, InboxMessage, InboxReadRequest>
        implements InboxStorage, Logging {

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
    public synchronized Page<InboxMessage> readAll(ShardIndex index, int pageSize) {
        TenantInboxRecords storage = multitenantStorage.currentSlice();

        AtomicInteger counter = new AtomicInteger();
        Map<Integer, ImmutableList<InboxMessage>> pages =
                storage.readAll()
                       .stream()
                       .filter((r) -> index.equals(r.getShardIndex()))
                       .sorted(InMemoryInboxStorage::compareMessages)
                       .collect(groupingBy(m -> counter.getAndIncrement() / pageSize,
                                           toImmutableList()));

        PageByNumber supplier = number -> Optional.ofNullable(pages.get(number));
        return new InMemoryPage(0, supplier);
    }

    @Override
    public synchronized Optional<InboxMessage> oldestMessageToDeliver(ShardIndex index) {
        TenantInboxRecords storage = multitenantStorage.currentSlice();
        Optional<InboxMessage> result =
                storage.readAll()
                       .stream()
                       .filter((r) -> index.equals(r.getShardIndex()) && isToDeliver(r))
                       .min(InMemoryInboxStorage::compareMessages);
        return result;
    }

    private static boolean isToDeliver(InboxMessage r) {
        return r.getStatus() == InboxMessageStatus.TO_DELIVER;
    }

    private static int compareMessages(InboxMessage m1, InboxMessage m2) {
        int timeComparison = compare(m1.getWhenReceived(), m2.getWhenReceived());
        if (timeComparison != 0) {
            return timeComparison;
        }
        int versionComparison = Integer.compare(m1.getVersion(), m2.getVersion());
        if(versionComparison == 0) {
            throw new IllegalStateException("Versions must not be equal.");
        }
        return versionComparison;
    }

    @Override
    public synchronized void write(InboxMessage message) {
        multitenantStorage.currentSlice()
                          .put(message.getId(), message);
    }

    @Override
    public synchronized void writeAll(Iterable<InboxMessage> messages) {
        for (InboxMessage inboxMessage : messages) {
            write(inboxMessage);
        }
    }

    @Override
    public synchronized Iterator<InboxMessageId> index() {
        return multitenantStorage.currentSlice()
                                 .index();
    }

    @Override
    public synchronized Optional<InboxMessage> read(InboxReadRequest request) {
        return multitenantStorage.currentSlice()
                                 .get(request.recordId());
    }

    @Override
    public synchronized void write(InboxMessageId id, InboxMessage record) {
        multitenantStorage.currentSlice()
                          .put(id, record);
    }

    @Override
    public synchronized void removeAll(Iterable<InboxMessage> messages) {
        TenantInboxRecords storage = multitenantStorage.currentSlice();
        for (InboxMessage message : messages) {
            storage.remove(message);
        }
    }

    /**
     * An in-memory implementation of a page of messages read from the {@code InboxStorage}.
     */
    private static final class InMemoryPage implements Page<InboxMessage> {

        private final int pageNumber;
        private final PageByNumber pageSupplier;

        private InMemoryPage(int number, PageByNumber supplier) {
            this.pageNumber = number;
            this.pageSupplier = supplier;
        }

        @Override
        public ImmutableList<InboxMessage> contents() {
            return pageSupplier.getBy(pageNumber)
                               .orElse(ImmutableList.of());
        }

        @Override
        public int size() {
            return contents().size();
        }

        @Override
        public Optional<Page<InboxMessage>> next() {
            int nextNumber = pageNumber + 1;
            Optional<Page<InboxMessage>> result =
                    pageSupplier.getBy(nextNumber)
                                .map(l -> new InMemoryPage(nextNumber, pageSupplier));
            return result;
        }
    }

    /**
     * Supplies the contents of the {@linkplain InMemoryPage page} by its number.
     */
    private interface PageByNumber {

        Optional<ImmutableList<InboxMessage>> getBy(int number);
    }
}
