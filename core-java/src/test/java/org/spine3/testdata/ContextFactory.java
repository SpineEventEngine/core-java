/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.*;
import org.spine3.protobuf.Messages;
import org.spine3.time.ZoneOffset;
import org.spine3.util.Commands;
import org.spine3.util.Events;


/**
 * Creates Context for tests.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class ContextFactory {

    private ContextFactory() {
    }

    public static CommandContext getCommandContext(UserId userId) {
        return getCommandContext(userId, TimeUtil.getCurrentTime());
    }

    public static CommandContext getCommandContext(UserId userId, Timestamp when) {
        final CommandId commandId = CommandId.newBuilder()
                .setActor(userId)
                .setTimestamp(when)
                .build();
        return CommandContext.newBuilder()
                .setCommandId(commandId)
                .setZoneOffset(ZoneOffset.getDefaultInstance())
                .build();
    }

    public static EventContext newEventContext() {

        final Timestamp now = TimeUtil.getCurrentTime();
        final CommandId commandId = CommandId.newBuilder()
                .setActor(UserId.getDefaultInstance())
                .setTimestamp(now)
                .build();
        final EventId eventId = Events.generateId(commandId);

        return EventContext.newBuilder()
                .setEventId(eventId)
                .setAggregateId(Messages.toAny(AggregateIdFactory.newProjectId()))
                .build();
    }

    public static EventContext getEventContext(UserId userId, Message aggregateId) {

        final CommandId commandId = Commands.generateId(userId);
        final EventId eventId = Events.generateId(commandId);

        final EventContext.Builder builder = EventContext.newBuilder()
                .setEventId(eventId)
                .setAggregateId(Messages.toAny(aggregateId));

        return builder.build();
    }
}
