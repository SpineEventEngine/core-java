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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alex Tymchenko
 */
final class LiveDeliveryStation extends Station {

    private final Delivery.DeliverByType action;
    private final @Nullable Duration idempotenceWindow;

    LiveDeliveryStation(Delivery.DeliverByType action, Duration idempotenceWindow) {
        this.action = action;
        this.idempotenceWindow = !idempotenceWindow.equals(Duration.getDefaultInstance())
                                 ? idempotenceWindow
                                 : null;
    }

    @Override
    public final Result process(Conveyor conveyor) {
        Map<DispatchingId, InboxMessage> seen = new LinkedHashMap<>();
        for (InboxMessage message : conveyor) {
            InboxMessageStatus status = message.getStatus();
            if (status == InboxMessageStatus.TO_DELIVER) {
                DispatchingId dispatchingId = new DispatchingId(message);
                //TODO:2019-12-20:alex.tymchenko: proper de-duplication!
                if (seen.containsKey(dispatchingId)) {
                    conveyor.remove(message);
                } else {
                    seen.put(dispatchingId, message);
                    if (idempotenceWindow != null) {
                        conveyor.keepForLonger(message, idempotenceWindow);
                    }
                }
            }

//            if (message.hasEvent() && message.getEvent()
//                                             .enclosedMessage() instanceof CatchUpStarted) {
//                InboxId inboxId = message.getInboxId();
//                Object targetId = Identifier.unpack(inboxId.getEntityId()
//                                                           .getId());
//                System.out.println(
//                        "Live Delivery encountered `CatchUpStarted` for `"
//                                + targetId + "`.");
//            }
        }
        Collection<InboxMessage> toProcess = seen.values();
        List<InboxMessage> toDispatch = deduplicateAndSort(toProcess,
                                                           conveyor,
                                                           InboxMessageComparator.chronologically);
        DeliveryErrors errors = action.executeFor(toDispatch, new ArrayList<>());
        for (InboxMessage justDelivered : toProcess) {
            conveyor.markDelivered(justDelivered);
        }
        Result result = new Result(seen.size(), errors);
        return result;
    }
}
