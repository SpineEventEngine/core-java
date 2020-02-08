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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.base.Time;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * @author Alex Tymchenko
 */
final class Conveyor implements Iterable<InboxMessage> {

    private final Map<InboxMessageId, InboxMessage> messages = new LinkedHashMap<>();
    private final DeliveredMessages deliveredMessages;
    private final Set<InboxMessageId> dirtyMessages = new HashSet<>();
    private final Set<InboxMessage> removals = new HashSet<>();
    private final Set<InboxMessage> duplicates = new HashSet<>();

    /**
     * Creates an instance of conveyor with the messages to process and the cache of the previously
     * delivered messages.
     *
     * @param messages
     *         messages to process
     * @param deliveredMessages
     *         cache of the previously delivered messages
     */
    Conveyor(Collection<InboxMessage> messages, DeliveredMessages deliveredMessages) {
        this.deliveredMessages = deliveredMessages;
        for (InboxMessage message : messages) {
            this.messages.put(message.getId(), message);
        }
    }

    @Override
    public Iterator<InboxMessage> iterator() {
        return new ArrayList<>(messages.values()).iterator();
    }

    private void markDelivered(InboxMessage message) {
        changeStatus(message, InboxMessageStatus.DELIVERED);
        deliveredMessages.recordDelivered(message);
    }

    void markDelivered(Collection<InboxMessage> messages) {
        for (InboxMessage message : messages) {
            markDelivered(message);
        }
    }

    void remove(InboxMessage message) {
        messages.remove(message.getId());
        removals.add(message);
        dirtyMessages.remove(message.getId());
    }

    void markDuplicateAndRemove(InboxMessage message) {
        duplicates.add(message);
        remove(message);
    }

    void markCatchUp(InboxMessage message) {
        changeStatus(message, InboxMessageStatus.TO_CATCH_UP);
    }

    private void changeStatus(InboxMessage message, InboxMessageStatus status) {
        InboxMessage modified = findInConveyor(message).toBuilder()
                                                       .setStatus(status)
                                                       .build();
        messages.put(message.getId(), modified);
        dirtyMessages.add(message.getId());
    }

    private InboxMessage findInConveyor(InboxMessage message) {
        InboxMessage existingMessage = messages.get(message.getId());
        checkNotNull(existingMessage);
        return existingMessage;
    }

    /**
     * Returns the read-only view on the set of the {@code DispatchingId}s
     * of the {@link InboxMessage}s which are known to be already delivered.
     *
     * <p>This includes both the IDs of messages delivered within the lifetime of this conveyor
     * instance and of the messages delivered
     * {@linkplain Conveyor#Conveyor(Collection, DeliveredMessages) before it}.
     */
    Set<DispatchingId> idsOfDelivered() {
        Set<DispatchingId> recentlyDelivered =
                delivered()
                        .map(DispatchingId::new)
                        .collect(Collectors.toSet());
        Sets.SetView<DispatchingId> result = Sets.union(recentlyDelivered,
                                                        deliveredMessages.allDelivered());
        return result;
    }

    /**
     * Returns the stream of the {@link InboxMessage}s which are known to be delivered.
     *
     * <p>This includes all the messages on this conveyor with the {@code DELIVERED} status.
     */
    Stream<InboxMessage> delivered() {
        return messages.values()
                       .stream()
                       .filter(m -> m.getStatus() ==
                               InboxMessageStatus.DELIVERED);
    }

    /**
     * Marks the passed message as such to be kept in its {@code Inbox} for a certain amount of
     * time starting from now.
     *
     * <p>Such an operation may be used to keep the message as a de-duplication source.
     */
    void keepForLonger(InboxMessage message, Duration howLongTooKeep) {
        Timestamp keepUntil = Timestamps.add(Time.currentTime(), howLongTooKeep);
        InboxMessage modified = findInConveyor(message).toBuilder()
                                                       .setKeepUntil(keepUntil)
                                                       .build();
        messages.put(message.getId(), modified);
        dirtyMessages.add(message.getId());
    }

    /**
     * Returns the stream of the messages that are already detected as duplicates.
     */
    Stream<InboxMessage> recentDuplicates() {
        return duplicates.stream();
    }

    /**
     * Writes all the pending changes to the passed {@code InboxStorage}.
     */
    void flushTo(InboxStorage storage) {
        List<InboxMessage> dirtyMessages =
                messages.values()
                        .stream()
                        .filter(message -> this.dirtyMessages.contains(message.getId()))
                        .collect(toList());
        storage.writeAll(dirtyMessages);
        storage.removeAll(removals);
        dirtyMessages.clear();
        removals.clear();
        duplicates.clear();
    }

    /**
     * Returns the messages that are marked to be removed.
     */
    @VisibleForTesting
    Iterator<InboxMessage> removals() {
        return removals.iterator();
    }
}
