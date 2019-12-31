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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.catchup.CatchUp;
import io.spine.server.catchup.CatchUpStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static io.spine.server.catchup.CatchUpStatus.COMPLETED;
import static io.spine.server.catchup.CatchUpStatus.FINALIZING;
import static io.spine.server.catchup.CatchUpStatus.STARTED;
import static io.spine.server.delivery.InboxMessageStatus.TO_CATCH_UP;
import static io.spine.server.delivery.InboxMessageStatus.TO_DELIVER;

/**
 * @author Alex Tymchenko
 */
public class CatchUpClassifier {

    private final ImmutableList<InboxMessage> delivery;
    private final ImmutableList<InboxMessage> catchUp;
    private final ImmutableList<InboxMessage> removal;
    private final ImmutableList<InboxMessage> paused;

    private CatchUpClassifier(
            ImmutableList<InboxMessage> delivery,
            ImmutableList<InboxMessage> catchUp,
            ImmutableList<InboxMessage> removal,
            ImmutableList<InboxMessage> paused) {
        this.delivery = delivery;
        this.catchUp = catchUp;
        this.removal = removal;
        this.paused = paused;
    }

    static CatchUpClassifier of(ImmutableList<InboxMessage> messages,
                                Iterable<CatchUp> jobs) {

        ImmutableList.Builder<InboxMessage> deliveryBuilder = ImmutableList.builder();
        ImmutableList.Builder<InboxMessage> catchUpBuilder = ImmutableList.builder();
        ImmutableList.Builder<InboxMessage> removalBuilder = ImmutableList.builder();
        List<InboxMessage> toPause = new ArrayList<>();

        Set<DispatchingId> forDispatching = new HashSet<>();

        boolean duplicatesInFinalizing = false;

        for (InboxMessage message : messages) {

            boolean underCatchUp = false;
            for (CatchUp job : jobs) {
                CatchUpStatus jobStatus = job.getStatus();

                if (matches(job, message)) {
                    underCatchUp = true;
                    if (jobStatus == STARTED) {
                        if (message.getStatus() == TO_CATCH_UP) {
                            catchUpBuilder.add(message);
                        } else if (message.getStatus() == TO_DELIVER) {
                            removalBuilder.add(message);
                        }
                    } else if (jobStatus == FINALIZING) {
                        if (message.getStatus() == TO_DELIVER) {
                            System.out.println("Pausing the `TO_DELIVER` message: " + eventDetails(message));
                            toPause.add(message);
                        }
//                        if(message.hasEvent() && message.getEvent().enclosedMessage() instanceof CatchUpStarted) {
//                            System.out.println("`CatchUpStarted` encountered. Moving all paused to removals.");
//                            removalBuilder.addAll(toPause);
//                            toPause.clear();
//                        }
                    } else if (jobStatus == COMPLETED) {
                        if (message.getStatus() == TO_CATCH_UP) {
                            DispatchingId dispatchingId = new DispatchingId(message);
                            if(!forDispatching.contains(dispatchingId)) {
                                catchUpBuilder.add(message);
                                forDispatching.add(dispatchingId);
                            } else {
                                duplicatesInFinalizing = true;
                                System.out.println("Removing `TO_CATCH_UP` duplicate headed to "
                                                           + Identifier.unpack(dispatchingId.inbox.getEntityId().getId()));
                                removalBuilder.add(message);
                            }
                        } else if (message.getStatus() == TO_DELIVER) {
                            DispatchingId dispatchingId = new DispatchingId(message);
                            if(!forDispatching.contains(dispatchingId)) {
                                deliveryBuilder.add(message);
                            } else {
                                duplicatesInFinalizing = true;
                                System.out.println("Removing `TO_DELIVER` duplicate headed to "
                                                           + Identifier.unpack(dispatchingId.inbox.getEntityId().getId()));

                                removalBuilder.add(message);
                            }
                        }
                    }
                }
            }
            if (!underCatchUp) {
                deliveryBuilder.add(message);
            }
        }

        if(duplicatesInFinalizing) {
            System.out.println(" -- There were duplicates. Initially analyzed the events:");
            for (InboxMessage inboxMessage : messages) {
                System.out.println(eventDetails(inboxMessage));
            }
        }

        return new CatchUpClassifier(deliveryBuilder.build(),
                                     catchUpBuilder.build(),
                                     removalBuilder.build(),
                                     ImmutableList.copyOf(toPause));
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

    ImmutableList<InboxMessage> delivery() {
        return delivery;
    }

    ImmutableList<InboxMessage> catchUp() {
        return catchUp;
    }

    ImmutableList<InboxMessage> removal() {
        return removal;
    }

    ImmutableList<InboxMessage> paused() {
        return paused;
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
    
    private static final class DispatchingId {
        
        private final InboxSignalId signal;
        private final InboxId inbox;

        private DispatchingId(InboxMessage message) {
            this.signal = message.getSignalId();
            this.inbox = message.getInboxId();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DispatchingId id = (DispatchingId) o;
            return Objects.equals(signal, id.signal) &&
                    Objects.equals(inbox, id.inbox);
        }

        @Override
        public int hashCode() {
            return Objects.hash(signal, inbox);
        }
    }
}


