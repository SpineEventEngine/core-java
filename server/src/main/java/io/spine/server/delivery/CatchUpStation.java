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

import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import io.spine.base.Identifier;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.catchup.CatchUp;
import io.spine.server.catchup.CatchUpStatus;
import io.spine.server.catchup.event.CatchUpStarted;
import io.spine.server.event.EventComparator;
import io.spine.type.TypeUrl;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.spine.server.catchup.CatchUpStatus.COMPLETED;
import static io.spine.server.catchup.CatchUpStatus.FINALIZING;
import static io.spine.server.catchup.CatchUpStatus.STARTED;
import static io.spine.server.delivery.InboxMessageStatus.TO_CATCH_UP;
import static io.spine.server.delivery.InboxMessageStatus.TO_DELIVER;

/**
 * @author Alex Tymchenko
 */
final class CatchUpStation extends Station {

    private static final Duration HOW_LONG_TO_KEEP = Durations.fromMillis(1000);
    private static final Comparator<InboxMessage> COMPARATOR = new CatchUpMessageComparator();

    private final DeliverByType action;
    private final Iterable<CatchUp> jobs;

    CatchUpStation(DeliverByType action, Iterable<CatchUp> jobs) {
        this.action = action;
        this.jobs = jobs;
    }

    @Override
    public final Result process(Conveyor conveyor) {

        Map<DispatchingId, InboxMessage> dispatchToCatchUp = new LinkedHashMap<>();

        for (InboxMessage message : conveyor) {
            for (CatchUp job : jobs) {
                CatchUpStatus jobStatus = job.getStatus();

                if (matches(job, message)) {
                    DispatchingId dispatchingId = new DispatchingId(message);
                    if (jobStatus == STARTED) {
                        if (message.getStatus() == TO_CATCH_UP) {
                            dispatchToCatchUp.put(dispatchingId, message);
                        } else if (message.getStatus() == TO_DELIVER) {
                            conveyor.remove(message);
                        }
                    } else if (jobStatus == FINALIZING) {
                        if (message.getStatus() == TO_DELIVER) {
                            System.out.println(
                                    "Pausing the `TO_DELIVER` message: " + eventDetails(message));
                            conveyor.markCatchUp(message);
                        }
                    } else if (jobStatus == COMPLETED) {
                        if (message.getStatus() == TO_CATCH_UP) {
                            if (!dispatchToCatchUp.containsKey(dispatchingId)) {
                                dispatchToCatchUp.put(dispatchingId, message);
                                conveyor.keepForLonger(message, HOW_LONG_TO_KEEP);
                            } else {
                                System.out.println("Removing `TO_CATCH_UP` duplicate: " +
                                                           eventDetails(message));
                                conveyor.remove(message);
                            }
                        } else if (message.getStatus() == TO_DELIVER) {
                            if (dispatchToCatchUp.containsKey(dispatchingId)) {
                                System.out.println("Removing `TO_DELIVER` duplicate: " +
                                                           eventDetails(message));
                                conveyor.remove(message);
                            }
                        }
                    }
                }
            }
        }
        List<InboxMessage> messages = deduplicateAndSort(dispatchToCatchUp.values(),
                                                         conveyor,
                                                         COMPARATOR);
        DeliveryErrors errors = action.executeFor(messages);
        conveyor.markDelivered(messages);
        Result result = new Result(dispatchToCatchUp.size(), errors);
        return result;
    }

    private static boolean matches(CatchUp job, InboxMessage message) {
        List<Any> targets = job.getRequest()
                               .getTargetList();
        Any rawEntityId = message.getInboxId()
                                 .getEntityId()
                                 .getId();
        return targets.stream()
                      .anyMatch((t) -> t.equals(rawEntityId));
    }

    private static String eventDetails(InboxMessage inboxMessage) {
        if (!inboxMessage.hasEvent()) {
            return "";
        }
        Object entityId = Identifier.unpack(inboxMessage.getInboxId()
                                                        .getEntityId()
                                                        .getId());
        Event event = inboxMessage.getEvent();
        EventContext eventContext = event.getContext();
        Timestamp timestamp = eventContext.getTimestamp();
        Timestamp whenReceived = inboxMessage.getWhenReceived();
        int version = inboxMessage.getVersion();
        return " + [" + entityId + "] "
                + '(' + event.getId()
                             .getValue() + ") "
                + timestamp.getSeconds()
                + '.' + timestamp.getNanos()
                + " of type " + event.getMessage()
                                     .getTypeUrl()
                + " in status " + inboxMessage.getStatus()
                                              .toString()
                + ". Inbox received at " + whenReceived.getSeconds()
                + '.' + whenReceived.getNanos()
                + " in version " + version;
    }

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
