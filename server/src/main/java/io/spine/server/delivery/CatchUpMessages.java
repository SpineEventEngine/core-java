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
import com.google.protobuf.ProtocolStringList;
import io.spine.server.delivery.event.CatchUpCompleted;
import io.spine.server.delivery.event.CatchUpStarted;
import io.spine.server.delivery.event.HistoryEventsRecalled;
import io.spine.server.delivery.event.HistoryFullyRecalled;
import io.spine.server.delivery.event.LiveEventsPickedUp;
import io.spine.server.delivery.event.ShardProcessingRequested;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStreamQuery;

import static com.google.common.collect.ImmutableList.toImmutableList;

final class CatchUpMessages {

    private CatchUpMessages() {
    }

    static CatchUpStarted started(CatchUpId id) {
        return CatchUpStarted.newBuilder()
                             .setId(id)
                             .vBuild();
    }

    static EventStreamQuery.Limit limitOf(int value) {
        return EventStreamQuery.Limit.newBuilder()
                                     .setValue(value)
                                     .vBuild();
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

    static ShardProcessingRequested shardProcessingRequested(ShardIndex shardIndex) {
        return ShardProcessingRequested
                .newBuilder()
                .setId(shardIndex)
                .vBuild();
    }
}