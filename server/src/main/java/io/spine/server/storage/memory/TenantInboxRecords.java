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

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.server.delivery.Inbox;
import io.spine.server.delivery.InboxMessage;
import io.spine.server.delivery.InboxMessageId;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;

/**
 * The memory-based storage for {@link io.spine.server.inbox.InboxMessage InboxMessage}s
 * that represents all storage operations available for inbox data of a single tenant.
 */
class TenantInboxRecords implements TenantStorage<InboxMessageId, InboxMessage> {

    private final Map<InboxMessageId, InboxMessage> records = Maps.newConcurrentMap();

    @Override
    public Iterator<InboxMessageId> index() {
        return records.keySet()
                      .iterator();
    }

    @Override
    public Optional<InboxMessage> get(InboxMessageId id) {
        return Optional.ofNullable(records.get(id));
    }

    /**
     * Obtains the contents of {@link Inbox Inbox} by its identifier.
     *
     * <p>Returns all messages, placing those received earlier first.
     *
     * @return messages of the {@code Inbox}
     */
    public SortedSet<InboxMessage> readAll() {
        ImmutableSortedSet<InboxMessage> result =
                ImmutableSortedSet.copyOf(new InboxStorageRecordComparator(), records.values());
        return result;
    }

    @Override
    public void put(InboxMessageId id, InboxMessage record) {
        records.put(id, record);
    }

    public void remove(InboxMessage message) {
        records.remove(message.getId());
    }

    @Override
    public boolean isEmpty() {
        return records.isEmpty();
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
            if (Objects.equals(first, second)) {
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
