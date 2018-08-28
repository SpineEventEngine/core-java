/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.protobuf.Message;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.MessageEnvelope;
import io.spine.core.Version;

import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.core.Events.getMessage;
import static io.spine.core.Events.substituteVersion;

/**
 * The utility which helps {@linkplain Aggregate Aggregate}s dealing with
 * {@linkplain io.spine.core.Event events} during the {@code Aggregate lifecycle}.
 *
 * @author Alex Tymchenko
 */
final class AggregateEvents {

    /**
     * Prevents this utility class from instantiation.
     */
    private AggregateEvents() {
    }

    /**
     * Prepares {@code Event}s for applying to an aggregate.
     *
     * @param origin
     *         the origin of the event
     * @return a function that prepares.
     */
    static BiFunction<Event, Version, Event> prepareEventForApplyFn(MessageEnvelope origin) {
        checkNotNull(origin);
        return (event, version) -> prepareEvent(event, origin, version);
    }

    /**
     * Prepares the given event to be applied to an aggregate.
     *
     * <p>Updates the event version with the given {@code projectedVersion}.
     *
     * <p>If the given event is an imported event, un-boxes the event message.
     *
     * @param event
     *         the event to prepare
     * @param origin
     *         the event origin
     * @param projectedVersion
     *         the version to set to the event
     * @return the event ready to be applied to this aggregate
     */
    private static Event prepareEvent(Event event,
                                      MessageEnvelope origin,
                                      Version projectedVersion) {
        Message eventMessage = getMessage(event);

        Event eventToApply;
        if (eventMessage instanceof Event) {
            Event importEvent = (Event) eventMessage;
            CommandEnvelope command = (CommandEnvelope) origin;
            eventToApply = importedEvent(importEvent, command, projectedVersion);
        } else {
            eventToApply = substituteVersion(event, projectedVersion);
        }
        return eventToApply;
    }

    /**
     * Creates an event based on the event received in an import command.
     *
     * @param event
     *         the event to import
     * @param command
     *         the import command
     * @param version
     *         the version of the aggregate to use for the event
     * @return an event with updated command context and entity version
     */
    private static Event importedEvent(Event event, CommandEnvelope command, Version version) {
        EventContext eventContext =
                event.getContext()
                     .toBuilder()
                     .setCommandContext(command.getCommandContext())
                     .setTimestamp(getCurrentTime())
                     .setVersion(version)
                     .build();
        Event result =
                event.toBuilder()
                     .setContext(eventContext)
                     .build();
        return result;
    }
}
