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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A stage of processing the messages on the {@link Conveyor}.
 *
 * <p>During the processing, some of the conveyor messages may be delivered, removed or modified.
 */
abstract class Station {

    /**
     * Processes the conveyor messages.
     *
     * @param conveyor
     *         the conveyor on which the messages are travelling
     * @return the processing result
     */
    abstract Result process(Conveyor conveyor);

    /**
     * De-duplicates and sorts the messages.
     *
     * <p>The conveyor is used to understand which messages were previously delivered and should
     * be used as a de-duplication source.
     *
     * <p>Duplicated messages are {@linkplain Conveyor#knownDuplicates() remembered by the conveyor}
     * and marked for removal.
     *
     * <p>The comparator for the messages is customizable. This is requires as long as different
     * stations may have their own requirements on sorting.
     *
     * @param messages
     *         message to de-duplicate and sort
     * @param conveyor
     *         current conveyor
     * @param comparator
     *         to sort messages
     * @return de-duplicated and sorted messages
     */
    List<InboxMessage> deduplicateAndSort(Collection<InboxMessage> messages,
                                          Conveyor conveyor,
                                          Comparator<InboxMessage> comparator) {
        List<InboxMessage> previouslyDelivered = conveyor.previouslyDelivered();
        Set<DispatchingId> dispatchedIds = previouslyDelivered.stream()
                                                              .map(DispatchingId::new)
                                                              .collect(Collectors.toSet());
        List<InboxMessage> result = new ArrayList<>();
        for (InboxMessage message : messages) {
            DispatchingId id = new DispatchingId(message);
            if (dispatchedIds.contains(id)) {
                conveyor.markDuplicateAndRemove(message);
            } else {
                result.add(message);
            }
        }
        result.sort(comparator);
        return result;
    }

    /**
     * Tells what the results of the processing at a particular station were.
     */
    static class Result {

        private final int deliveredCount;
        private final DeliveryErrors errors;

        Result(int count, DeliveryErrors errors) {
            deliveredCount = count;
            this.errors = errors;
        }

        /**
         * Tells how many messages were delivered during the processing at the station.
         */
        int deliveredCount() {
            return deliveredCount;
        }

        /**
         * Returns the errors occurred during the delivery of the messages by the station, if any.
         */
        DeliveryErrors errors() {
            return errors;
        }
    }
}
