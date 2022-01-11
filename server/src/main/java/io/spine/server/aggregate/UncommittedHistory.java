/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.aggregate;

import com.google.common.collect.ImmutableList;
import io.spine.core.Event;
import io.spine.server.type.EventEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

/**
 * Uncommitted events and snapshots created for this aggregate during the dispatching.
 *
 * <p>Watches how the events {@linkplain Aggregate#invokeApplier(EventEnvelope) are sent}
 * to the {@link Aggregate} applier methods. Remembers all such events as uncommitted.
 *
 * <p>Once an aggregate is loaded from the storage, the {@code UncommittedHistory}
 * {@linkplain #onAggregateRestored(AggregateHistory) remembers} the event count
 * after the last snapshot.
 *
 * <p>If during the dispatching of events the number of events since the last snapshot exceeds
 * the snapshot trigger, a snapshot is created and remembered as a part of uncommitted history.
 *
 * <p>In order to ignore the events fed to the aggregate when it's being loaded from the storage,
 * the {@code UncommittedHistory}'s tracking is only {@linkplain #startTrackingSession(int) activated}
 * when the new and truly un-yet-committed events are dispatched to the applier methods.
 * The tracking {@linkplain #startTracking() stops} after all the new events have been played
 * on the aggregate instance.
 *
 * @see Aggregate#apply(List, int) on activation and deactivation of event tracking
 * @see Aggregate#replay(AggregateHistory) on supplying the history stats when loading aggregate
 *         instances from the storage
 */
final class UncommittedHistory {

    private final Supplier<Snapshot> makeSnapshot;
    private final List<AggregateHistory> historySegments = new ArrayList<>();
    private final List<Event> currentSegment = new ArrayList<>();
    private int eventCountAfterLastSnapshot;

    /**
     * Creates an instance of the uncommitted history.
     *
     * @param makeSnapshot
     *         a callback which would supply a snapshot of the aggregate upon demand
     */
    UncommittedHistory(Supplier<Snapshot> makeSnapshot) {
        this.makeSnapshot = makeSnapshot;
    }

    /**
     * Tracks the event dispatched to the Aggregate's applier.
     *
     * <p>If the tracking is not {@linkplain #startTrackingSession(int) started}, the event is considered
     * an old one and such as not requiring storage and tracking. In this case, this method
     * does nothing.
     *
     * <p>If the event is a new one, it is remembered as a part of the uncommitted history.
     *
     * <p>If the number of events since the last snapshot equals or exceeds the snapshot trigger,
     * a new snapshot is made and saved to the uncommitted history.
     e
     * @param event
     *         an event to track
     */
    void track(List<Event> events, int snapshotTrigger) {
        for(var event : events) {
            if (event.isRejection()) {
                return;
            }
            currentSegment.add(event);
            var eventsInSegment = currentSegment.size();
            if (eventCountAfterLastSnapshot + eventsInSegment >= snapshotTrigger) {
                var snapshot = makeSnapshot.get();
                var completedSegment = historyFrom(currentSegment, snapshot);
                historySegments.add(completedSegment);
                currentSegment.clear();
                eventCountAfterLastSnapshot = 0;
            }
        }
    }

    /**
     * Composes and obtains the collected uncommitted history.
     *
     * <p>The returned items represent the uncommitted events split into {@code AggregateHistory},
     * each containing a snapshot made for each segment, if the snapshot was triggered.
     */
    ImmutableList<AggregateHistory> get() {
        ImmutableList.Builder<AggregateHistory> builder = ImmutableList.builder();
        builder.addAll(historySegments);
        if (currentSegment.size() > 0) {
            var lastSegment = historyFrom(currentSegment);
            builder.add(lastSegment);
        }
        return builder.build();
    }

    /**
     * Returns all tracked uncommitted events.
     */
    UncommittedEvents events() {
        var events = get().stream()
                                  .flatMap(segment -> segment.getEventList()
                                                             .stream())
                                  .collect(toList());
        return UncommittedEvents.ofNone()
                                .append(events);
    }

    /**
     * Tells if this history contains any uncommitted events.
     */
    boolean hasEvents() {
        return !currentSegment.isEmpty() || !historySegments.isEmpty();
    }

    /**
     * Marks this history as stored and no longer uncommitted.
     */
    void commit() {
        historySegments.clear();
        currentSegment.clear();
    }

    /**
     * Records the history loaded from the aggregate storage.
     *
     * <p>This is only required in order to know the number of events since the last snapshot.
     */
    void onAggregateRestored(AggregateHistory history) {
        this.eventCountAfterLastSnapshot = history.getEventCount();
    }

    private static AggregateHistory historyFrom(List<Event> events, Snapshot snapshot) {
        return AggregateHistory.newBuilder()
                .addAllEvent(events)
                .setSnapshot(snapshot)
                .vBuild();
    }

    private static AggregateHistory historyFrom(List<Event> events) {
        return AggregateHistory.newBuilder()
                .addAllEvent(events)
                .vBuild();
    }
}
