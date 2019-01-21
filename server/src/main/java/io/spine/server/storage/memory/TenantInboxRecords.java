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
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.server.inbox.InboxId;
import io.spine.server.inbox.InboxMessage;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static io.spine.util.Exceptions.unsupported;

/**
 * The memory-based storage for {@link io.spine.server.inbox.InboxContentRecord InboxContentRecord}
 * that represents all storage operations available for inbox data of a single tenant.
 */
class TenantInboxRecords implements TenantStorage<InboxId, InboxMessage> {

    private final Multimap<InboxId, InboxMessage> records = TreeMultimap.create(
            new InboxStorageKeyComparator(), // key comparator
            new InboxStorageRecordComparator() // value comparator
    );

    @Override
    public Iterator<InboxId> index() {
        return records.keySet()
                      .iterator();
    }

    @Override
    public Optional<InboxMessage> get(InboxId id) {
        throw unsupported("Returning single record by Inbox ID is not supported");
    }

    /**
     * Obtains the contents of {@link io.spine.server.inbox.Inbox Inbox} by its identifier.
     *
     * <p>Returns all messages, placing those received earlier first.
     *
     * @param id
     *         the identifier of {@code Inbox}
     * @return messages of the {@code Inbox}
     */
    public List<InboxMessage> getAll(InboxId id) {
        Collection<InboxMessage> messages = records.get(id);
        return ImmutableList.copyOf(messages);
    }

    @Override
    public void put(InboxId id, InboxMessage record) {
        records.put(id, record);
    }

    @Override
    public boolean isEmpty() {
        return records.isEmpty();
    }

    /**
     * {@code hashCode()}-based comparator for {@code InboxId}-typed keys of the in-memory storage.
     */
    static class InboxStorageKeyComparator implements Comparator<InboxId>, Serializable {

        private static final long serialVersionUID = 0L;

        @Override
        public int compare(InboxId first, InboxId second) {
            if (first == second) {
                return 0;
            }
            int firstHashCode = first.hashCode();
            int secondHashCode = second.hashCode();
            int result = Integer.compare(firstHashCode, secondHashCode);
            return result;
        }
    }

    /**
     * Comparator for the {@code InboxMessage}s that are stored in in-memory storage.
     *
     * <p>Defines the order of messages, placing the messages
     * {@linkplain io.spine.server.inbox.InboxMessage#getWhenReceived() received earlier} on top.
     */
    static class InboxStorageRecordComparator implements Comparator<InboxMessage>, Serializable {

        private static final long serialVersionUID = 0L;

        @Override
        public int compare(InboxMessage first, InboxMessage second) {
            if (first == second) {
                return 0;
            }
            Timestamp whenFirstReceived = first.getWhenReceived();
            Timestamp whenSecondReceived = second.getWhenReceived();
            int timeComparison = Timestamps.compare(whenFirstReceived, whenSecondReceived);
            if (timeComparison != 0) {
                return timeComparison;
            }
            int hashCodeComparison = Integer.compare(first.hashCode(), second.hashCode());
            return hashCodeComparison;
        }
    }
}
