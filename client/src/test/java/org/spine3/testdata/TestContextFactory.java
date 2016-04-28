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
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.base.Schedule;
import org.spine3.base.UserId;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.client.UserUtil.newUserId;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.time.ZoneOffsets.UTC;


/**
 * Creates Context for tests.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings({"UtilityClass", "OverloadedMethodsWithSameNumberOfParameters"})
public class TestContextFactory {

    private static final Any AGGREGATE_ID = toAny(newStringValue(newUuid()));

    private static final String TEST_BC_NAME = "Test BC";

    private TestContextFactory() {}

    /**
     * Creates a new {@link CommandContext} with the random userId, commandId and current timestamp.
     */
    public static CommandContext createCommandContext() {
        final UserId userId = newUserId(newUuid());
        final Timestamp now = getCurrentTime();
        final CommandId commandId = Commands.generateId();
        return createCommandContext(userId, commandId, now);
    }

    /**
     * Creates a new {@link CommandContext} with the given userId, command time and timestamp.
     */
    public static CommandContext createCommandContext(UserId userId, CommandId commandId, Timestamp when) {
        final CommandContext.Builder builder = CommandContext.newBuilder()
                                                             .setCommandId(commandId)
                                                             .setActor(userId)
                                                             .setTimestamp(when)
                                                             .setZoneOffset(UTC)
                                                             .setSource(TEST_BC_NAME);
        return builder.build();
    }

    /**
     * Creates a new context with the given delay before the delivery time.
     */
    public static CommandContext createCommandContext(Duration delay) {
        final Schedule schedule = Schedule.newBuilder()
                                          .setAfter(delay)
                                          .build();
        return createCommandContext(schedule);
    }

    /**
     * Creates a new context with the given scheduling options.
     */
    public static CommandContext createCommandContext(Schedule schedule) {
        final CommandContext.Builder builder = createCommandContext().toBuilder()
                                                                     .setSchedule(schedule);
        return builder.build();
    }

    /*
     * Event context factory methods.
     */

    /**
     * Creates a new {@link EventContext} with default properties.
     */
    public static EventContext createEventContext() {
        final Timestamp now = getCurrentTime();
        final UserId userId = newUserId(newUuid());
        final CommandContext commandContext = createCommandContext(userId, Commands.generateId(), now);
        final EventId eventId = Events.generateId();
        final EventContext.Builder builder = EventContext.newBuilder()
                                                         .setEventId(eventId)
                                                         .setCommandContext(commandContext)
                                                         .setProducerId(AGGREGATE_ID)
                                                         .setSource(TEST_BC_NAME)
                                                         .setTimestamp(now);
        return builder.build();
    }

    /**
     * Creates a new {@link EventContext} with the given userId and aggregateId.
     */
    public static EventContext createEventContext(Message aggregateId) {
        final EventId eventId = Events.generateId();
        final EventContext.Builder builder = EventContext.newBuilder()
                                                         .setEventId(eventId)
                                                         .setProducerId(toAny(aggregateId))
                                                         .setSource(TEST_BC_NAME)
                                                         .setTimestamp(getCurrentTime());
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
                                                         .setProducerId(AGGREGATE_ID)
                                                         .setSource(TEST_BC_NAME);
        return builder.build();
    }

    /**
     * Creates a new {@link EventContext} with the given aggregate ID and timestamp.
     */
    public static EventContext createEventContext(Message aggregateId, Timestamp timestamp) {
        final EventId eventId = Events.generateId();
        final EventContext.Builder builder = EventContext.newBuilder()
                                                         .setEventId(eventId)
                                                         .setTimestamp(timestamp)
                                                         .setProducerId(toAny(aggregateId))
                                                         .setSource(TEST_BC_NAME);
        return builder.build();
    }
}
