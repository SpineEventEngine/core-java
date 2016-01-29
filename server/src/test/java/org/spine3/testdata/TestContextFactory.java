/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.testdata;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.*;
import org.spine3.client.Commands;
import org.spine3.server.event.Events;
import org.spine3.time.ZoneOffset;

import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testdata.TestAggregateIdFactory.createProjectId;


/**
 * Creates Context for tests.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings({"UtilityClass", "OverloadedMethodsWithSameNumberOfParameters"})
public class TestContextFactory {

    private static final Any AGGREGATE_ID = toAny(createProjectId("dummy_project_id_123"));

    private TestContextFactory() {
    }

    /**
     * Creates a new {@link CommandContext} with the given userId and command time.
     */
    public static CommandContext createCommandContext(UserId userId, CommandId commandId, Timestamp when) {
        return CommandContext.newBuilder()
                .setCommandId(commandId)
                .setActor(userId)
                .setTimestamp(when)
                .setZoneOffset(ZoneOffset.getDefaultInstance())
                .build();
    }

    /**
     * Creates a new {@link EventContext} with default properties.
     */
    public static EventContext createEventContext() {
        final Timestamp now = TimeUtil.getCurrentTime();
        final CommandContext commandContext = createCommandContext(
                UserId.getDefaultInstance(), Commands.generateId(), now);
        final EventId eventId = Events.generateId();

        return EventContext.newBuilder()
                .setEventId(eventId)
                .setCommandContext(commandContext)
                .setAggregateId(AGGREGATE_ID)
                .build();
    }

    /**
     * Creates a new {@link EventContext} with the given userId and aggregateId.
     */
    public static EventContext createEventContext(Message aggregateId) {
        final EventId eventId = Events.generateId();
        final EventContext.Builder builder = EventContext.newBuilder()
                .setEventId(eventId)
                .setAggregateId(toAny(aggregateId));
        return builder.build();
    }

    /**
     * Creates a new {@link EventContext} with the given timestamp.
     */
    public static EventContext createEventContext(Timestamp timestamp) {
        final EventId eventId = Events.generateId();
        final EventContext.Builder builder = EventContext.newBuilder()
                .setEventId(eventId)
                .setTimestamp(timestamp)
                .setAggregateId(AGGREGATE_ID);
        return builder.build();
    }
}
