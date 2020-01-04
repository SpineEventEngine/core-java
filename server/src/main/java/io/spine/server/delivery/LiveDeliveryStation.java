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
 * @see CatchUpStation for the station performing the catch-up
 */
final class LiveDeliveryStation extends Station {

    private final DeliverByType action;
    private final @Nullable Duration idempotenceWindow;

    LiveDeliveryStation(DeliverByType action, Duration idempotenceWindow) {
        super();
        this.action = action;
        this.idempotenceWindow = !idempotenceWindow.equals(Duration.getDefaultInstance())
                                 ? idempotenceWindow
                                 : null;
    }

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
                    if (idempotenceWindow != null) {
                        conveyor.keepForLonger(message, idempotenceWindow);
                    }
                }
            }
        }
        Collection<InboxMessage> toDeliver = seen.values();
        List<InboxMessage> toDispatch = deduplicateAndSort(toDeliver, conveyor);
        DeliveryErrors errors = action.executeFor(toDispatch);
        conveyor.markDelivered(toDeliver);
        Result result = new Result(seen.size(), errors);
        return result;
    }

    /**
     * De-duplicates and sorts the messages.
     *
     * <p>The conveyor is used to understand which messages were previously delivered and should
     * be used as a de-duplication source.
     *
     * <p>Duplicated messages are {@linkplain Conveyor#recentDuplicates() remembered by the
     * conveyor} and marked for removal.
     *
     * <p>Messages are sorted {@linkplain InboxMessageComparator#chronologically chronologically}.
     *
     * @param messages
     *         message to de-duplicate and sort
     * @param conveyor
     *         current conveyor
     * @return de-duplicated and sorted messages
     */
    List<InboxMessage> deduplicateAndSort(Collection<InboxMessage> messages, Conveyor conveyor) {
        Set<DispatchingId> previouslyDelivered = conveyor.previouslyDelivered();
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
