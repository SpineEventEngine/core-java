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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.Timestamp;
import io.spine.server.catchup.CatchUpId;
import io.spine.server.catchup.event.CatchUpCompleted;
import io.spine.server.catchup.event.CatchUpStarted;
import io.spine.server.catchup.event.HistoryEventsRecalled;
import io.spine.server.catchup.event.HistoryFullyRecalled;
import io.spine.server.catchup.event.LiveEventsPickedUp;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStreamQuery;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.protobuf.util.Durations.fromNanos;
import static com.google.protobuf.util.Timestamps.subtract;

final class CatchUpMessages {

    private CatchUpMessages() {}

    static CatchUpStarted started(CatchUpId id) {
        return CatchUpStarted.newBuilder()
                             .setId(id)
                             .vBuild();
    }

    static EventStreamQuery.Limit limitOf(int value) {
        return EventStreamQuery.Limit.newBuilder()
                                     .setValue(value)
                                     .build();
    }

    static HistoryEventsRecalled recalled(CatchUpId id) {
        return HistoryEventsRecalled.newBuilder()
                                    .setId(id)
                                    .vBuild();
    }

    static HistoryFullyRecalled fullyRecalled(CatchUpId id) {
        return HistoryFullyRecalled.newBuilder()
                                   .setId(id)
                                   .vBuild();
    }

    static ImmutableList<EventFilter> toFilters(ProtocolStringList rawEventTypes) {
        return rawEventTypes.stream()
                            .map(type -> EventFilter
                                    .newBuilder()
                                    .setEventType(type)
                                    .build())
                            .collect(toImmutableList());
    }

    static CatchUpId targetOf(Message message) {
        return ((CatchUpSignal) message).getId();
    }

    static Timestamp withWindow(Timestamp value) {
        return subtract(value, fromNanos(1));
    }

    static LiveEventsPickedUp liveEventsPickedUp(CatchUpId id) {
        return LiveEventsPickedUp.newBuilder()
                                 .setId(id)
                                 .vBuild();
    }

    static CatchUpCompleted catchUpCompleted(CatchUpId id) {
        return CatchUpCompleted.newBuilder()
                               .setId(id)
                               .vBuild();
    }
}
