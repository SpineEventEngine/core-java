/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.protobuf.Duration;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A station that delivers those messages which are incoming in a live mode.
 *
 * <p>This station dispatches all the messages in {@link InboxMessageStatus#TO_DELIVER TO_DELIVER}
 * status that are present in the passed conveyor. After the messages are dispatched, they are
 * marked as {@link InboxMessageStatus#DELIVERED DELIVERED}.
 *
 * <p>If the deduplication window is {@linkplain DeliveryBuilder#setDeduplicationWindow(Duration)
 * set in the system}, all the delivered messages are set to be kept in their storages for
 * the duration, corresponding to the width of the window. In this way, they will become usable for
 * the potential deduplication.
 *
 * <p>Before the dispatching, the messages are deduplicated, taking into account {@linkplain
 * Conveyor#recentlyDelivered() all known delivered messages}. In this process, the messages
 * delivered previously and kept for longer are taken into account as well. The detected duplicates
 * are marked as such in the conveyor and are removed from the storage later.
 *
 * <p>The dispatched messages are reordered chronologically. However, the changes in ordering
 * are not propagated to the conveyor.
 *
 * @see CatchUpStation for the station performing the catch-up
 */
final class LiveDeliveryStation extends Station {

    /**
     * The action to use for the delivery of the messages to their targets.
     */
    private final DeliveryAction action;

    /**
     * The current setting of the deduplication window.
     *
     * <p>Is {@code null}, if not set.
     */
    private final @Nullable Duration deduplicationWindow;

    /**
     * Creates a new instance of {@code LiveDeliveryStation} with the action to use for the delivery
     * and the deduplication window.
     */
    LiveDeliveryStation(DeliveryAction action, Duration deduplicationWindow) {
        super();
        this.action = action;
        this.deduplicationWindow = !deduplicationWindow.equals(Duration.getDefaultInstance())
                                   ? deduplicationWindow
                                   : null;
    }

    /**
     * Dispatches the messages from the conveyor.
     *
     * @param conveyor
     *         the conveyor on which the messages are travelling
     * @return how many messages were delivered and whether there were any errors during the
     *         dispatching
     */
    @Override
    public final Result process(Conveyor conveyor) {
        Map<DispatchingId, InboxMessage> seen = new HashMap<>();
        for (InboxMessage message : conveyor) {
            InboxMessageStatus status = message.getStatus();
            if (status == InboxMessageStatus.TO_DELIVER) {
                DispatchingId dispatchingId = new DispatchingId(message);
                if (seen.containsKey(dispatchingId)) {
                    conveyor.markDuplicateAndRemove(message);
                } else {
                    seen.put(dispatchingId, message);
                    if (deduplicationWindow != null) {
                        conveyor.keepForLonger(message, deduplicationWindow);
                    }
                }
            }
        }
        if (!seen.isEmpty()) {
            Collection<InboxMessage> toDeliver = seen.values();
            List<InboxMessage> toDispatch = deduplicateAndSort(toDeliver, conveyor);
            DeliveryErrors errors = action.executeFor(toDispatch);
            conveyor.markDelivered(toDispatch);
            Result result = new Result(toDispatch.size(), errors);
            return result;
        } else {
            return emptyResult();
        }
    }

    /**
     * Deduplicates and sorts the messages.
     *
     * <p>The passed conveyor is used to understand which messages were previously delivered
     * and should be used as a deduplication source.
     *
     * <p>Duplicated messages are {@linkplain Conveyor#recentDuplicates() remembered by the
     * conveyor} and marked for removal.
     *
     * <p>Messages are sorted {@linkplain InboxMessageComparator#chronologically chronologically}.
     *
     * @param messages
     *         message to deduplicate and sort
     * @param conveyor
     *         current conveyor
     * @return deduplicated and sorted messages
     */
    private static List<InboxMessage> deduplicateAndSort(Collection<InboxMessage> messages,
                                                         Conveyor conveyor) {
        Set<DispatchingId> previouslyDelivered = conveyor.allDelivered();
        List<InboxMessage> result = new ArrayList<>();
        for (InboxMessage message : messages) {
            DispatchingId id = new DispatchingId(message);
            if (previouslyDelivered.contains(id)) {
                conveyor.markDuplicateAndRemove(message);
            } else {
                result.add(message);
            }
        }
        result.sort(InboxMessageComparator.chronologically);
        return result;
    }
}
