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
import com.google.protobuf.util.Durations;
import io.spine.core.Event;
import io.spine.server.delivery.event.CatchUpStarted;
import io.spine.server.event.EventComparator;
import io.spine.type.TypeUrl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.spine.server.delivery.CatchUpStatus.COMPLETED;
import static io.spine.server.delivery.CatchUpStatus.FINALIZING;
import static io.spine.server.delivery.CatchUpStatus.IN_PROGRESS;
import static io.spine.server.delivery.InboxMessageStatus.TO_CATCH_UP;
import static io.spine.server.delivery.InboxMessageStatus.TO_DELIVER;

/**
 * A station that performs the delivery of messages to the catching-up targets.
 *
 * <h1>Overview</h1>
 *
 * <p>Matches the messages on the passed {@link Conveyor} to the known {@link CatchUp} jobs basing
 * on the target entity type and the identifier of the entity to which the message is sent.
 *
 * <p>Depending on the status of the job, the matched messages are processed accordingly. See
 * more on that below.
 *
 * <b>1. Catch-up {@code IN_PROGRESS}.</b>
 *
 * <p>The matched messages in {@link InboxMessageStatus#TO_CATCH_UP TO_CATCH_UP} status are
 * dispatched to their targets. All the matched live messages (i.e. in {@link
 * InboxMessageStatus#TO_DELIVER TO_DELIVER} status) are ignored and removed from their inboxes.
 *
 * <b>2. Catch-up {@code FINALIZING}.</b>
 *
 * <p>When the catch-up job is being finalized, it means that the historical events may be dated
 * close to the present time and, thus, interfere with the live events headed to the same entities.
 * Therefore, the matched messages in either status are "paused" meaning they are NEITHER dispatched
 * NOR removed from their inboxes. Instead, they are held until the catch-up job is completed to be
 * deduplicated and delivered all at once.
 *
 * <p>To hold the live messages from being delivered down the conveyor pipeline, the live messages
 * are marked as {@code TO_CATCH_UP}.
 *
 * <b>3. Catch-up {@code COMPLETED}.</b>
 *
 * <p>At this stage the event history is fully processed. The inboxes contain the messages in
 * {@code TO_CATCH_UP} status, which are in fact the mix of the last portion of the replayed event
 * history and potentially some "paused" live events. So, the station dispatched all the matching
 * messages in {@code TO_CATCH_UP} status.
 *
 * <p>If the deduplication window is {@linkplain DeliveryBuilder#setDeduplicationWindow(Duration)
 * set in the system}, all the delivered messages are set to be kept in their storages
 * for the duration, corresponding to the width of the window. In this way, they will become usable
 * for the potential deduplication.
 *
 * <h1>Deduplication and reordering</h1>
 *
 * <p>Prior to the dispatching, the messages are deduplicated in scope of this message batch.
 * Please note, that the deduplication window is NOT taken into the account, as the historical
 * events may all have been delivered to their entities somewhen in the past.
 *
 * <p>All the duplicates are marked as such in the conveyor and are removed from their storage
 * later.
 *
 * <p>Another change made before the actual dispatching is reordering of the messages. The messages
 * are sorted chronologically, putting the events of a framework-internal {@link CatchUpStarted}
 * type first. It allows to guarantee that the targets will know of the started catch-up before
 * any message in {@code TO_CATCH_UP} status is dispatched to them.
 *
 * <p>The ordering changes are not reflected on the message order in the conveyor.
 */
final class CatchUpStation extends Station {

    private static final Duration HOW_LONG_TO_KEEP = Durations.fromMillis(1000);
    private static final Comparator<InboxMessage> COMPARATOR = new CatchUpMessageComparator();

    private final DeliveryAction action;
    private final Iterable<CatchUp> jobs;

    CatchUpStation(DeliveryAction action, Iterable<CatchUp> jobs) {
        super();
        this.action = action;
        this.jobs = jobs;
    }

    /**
     * Processes the messages on the conveyor, delivering those sent for catch-up.
     *
     * @param conveyor
     *         the conveyor on which the messages are travelling
     * @return the result of the processing telling how many messages were dispatched and whether
     *         there were any errors in that
     */
    @SuppressWarnings({"MethodWithMultipleLoops", "OverlyComplexMethod", "OverlyNestedMethod"})
    @Override
    public final Result process(Conveyor conveyor) {
        Map<DispatchingId, InboxMessage> dispatchToCatchUp = new HashMap<>();

        for (InboxMessage message : conveyor) {
            for (CatchUp job : jobs) {
                CatchUpStatus jobStatus = job.getStatus();

                if (job.matches(message)) {
                    DispatchingId dispatchingId = new DispatchingId(message);
                    if (jobStatus == IN_PROGRESS) {
                        if (message.getStatus() == TO_CATCH_UP) {
                            if (dispatchToCatchUp.containsKey(dispatchingId)) {
                                conveyor.remove(message);
                            } else {
                                dispatchToCatchUp.put(dispatchingId, message);
                            }
                        } else if (message.getStatus() == TO_DELIVER) {
                            conveyor.remove(message);
                        }
                    } else if (jobStatus == FINALIZING) {
                        if (message.getStatus() == TO_DELIVER) {
                            conveyor.markCatchUp(message);
                        }
                    } else if (jobStatus == COMPLETED) {
                        if (message.getStatus() == TO_CATCH_UP) {
                            if (!dispatchToCatchUp.containsKey(dispatchingId)) {
                                dispatchToCatchUp.put(dispatchingId, message);
                                conveyor.keepForLonger(message, HOW_LONG_TO_KEEP);
                            } else {
                                conveyor.remove(message);
                            }
                        } else if (message.getStatus() == TO_DELIVER
                                && dispatchToCatchUp.containsKey(dispatchingId)) {
                            conveyor.remove(message);
                        }
                    }
                }
            }
        }

        return dispatch(conveyor, dispatchToCatchUp);
    }

    private Result dispatch(Conveyor conveyor, Map<DispatchingId, InboxMessage> dispatchToCatchUp) {
        if (!dispatchToCatchUp.isEmpty()) {
            List<InboxMessage> messages = new ArrayList<>(dispatchToCatchUp.values());
            messages.sort(COMPARATOR);

            DeliveryErrors errors = action.executeFor(messages);
            conveyor.markDelivered(messages);
            Result result = new Result(dispatchToCatchUp.size(), errors);
            return result;
        }
        return emptyResult();
    }

    /**
     * The comparator which sorts the messages chronologically, but ensures that if there is
     * a {@link CatchUpStarted} event in the sorted batch, it goes on top.
     */
    private static final class CatchUpMessageComparator
            implements Comparator<InboxMessage>, Serializable {

        private static final long serialVersionUID = 0L;
        private static final TypeUrl CATCH_UP_STARTED =
                TypeUrl.from(CatchUpStarted.getDescriptor());

        @Override
        public int compare(InboxMessage m1, InboxMessage m2) {
            if (m1.hasEvent() && m2.hasEvent()) {
                Event e1 = m1.getEvent();
                String typeOfFirst = e1.getMessage()
                                       .getTypeUrl();
                if (typeOfFirst.equals(CATCH_UP_STARTED.toString())) {
                    return -1;
                }
                Event e2 = m2.getEvent();
                String typeOfSecond = e2.getMessage()
                                        .getTypeUrl();
                if (typeOfSecond.equals(CATCH_UP_STARTED.toString())) {
                    return 1;
                }
                return EventComparator.chronologically.compare(e1, e2);
            } else {
                return InboxMessageComparator.chronologically.compare(m1, m2);
            }
        }
    }
}
