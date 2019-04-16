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

package io.spine.server.aggregate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.Version;
import io.spine.core.Versions;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * Updates the version of aggregate events before they are played over the aggregate.
 */
final class VersionSequence {

    private final Version start;

    VersionSequence(Version start) {
        this.start = start;
    }

    /**
     * Prepares the given events to be applied to this aggregate.
     *
     * @param originalEvents
     *         the events to be applied
     * @return events ready to be applied to this aggregate
     * @see Aggregate#apply(List)
     */
    ImmutableList<Event> update(Collection<Event> originalEvents) {
        Stream<Version> versions =
                Stream.iterate(start, Versions::increment)
                      .skip(1) // Skip current version
                      .limit(originalEvents.size());
        Stream<Event> events = originalEvents.stream();
        ImmutableList<Event> eventsToApply =
                Streams.zip(events, versions,
                            VersionSequence::substituteVersion)
                       .collect(toImmutableList());
        return eventsToApply;
    }

    /**
     * Replaces the event version with the given {@code newVersion}.
     *
     * @param event
     *         original event
     * @param newVersion
     *         the version to set
     * @return the copy of the original event but with the new version
     */
    private static Event substituteVersion(Event event, Version newVersion) {
        EventContext newContext =
                event.context()
                     .toVBuilder()
                     .setVersion(newVersion)
                     .build();
        Event result =
                event.toVBuilder()
                     .setContext(newContext)
                     .build();
        return result;
    }
}
