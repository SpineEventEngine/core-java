/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandContext.Schedule;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.users.TenantId;
import org.spine3.users.UserId;

import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;
import static org.spine3.test.Tests.newUserId;
import static org.spine3.time.ZoneOffsets.UTC;


/**
 * Creates Context for tests.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class TestCommandContextFactory {

    private TestCommandContextFactory() {}

    /** Creates a new {@link CommandContext} instance. */
    public static CommandContext createCommandContext() {
        final UserId userId = newUserId(newUuid());
        final Timestamp now = getCurrentTime();
        final CommandId commandId = Commands.generateId();
        return createCommandContext(userId, commandId, now);
    }

    /** Creates a new {@link CommandContext} instance. */
    public static CommandContext createCommandContext(UserId userId, CommandId commandId, Timestamp when) {
        final TenantId.Builder generatedTenantId = TenantId.newBuilder()
                                                         .setValue(newUuid());
        final CommandContext.Builder builder = CommandContext.newBuilder()
                                                             .setCommandId(commandId)
                                                             .setActor(userId)
                                                             .setTimestamp(when)
                                                             .setZoneOffset(UTC)
                                                             .setTenantId(generatedTenantId);
        return builder.build();
    }

    /** Creates a new context with the given delay before the delivery time. */
    public static CommandContext createCommandContext(Duration delay) {
        final Schedule schedule = Schedule.newBuilder()
                                          .setDelay(delay)
                                          .build();
        return createCommandContext(schedule);
    }

    /** Creates a new context with the given scheduling options. */
    public static CommandContext createCommandContext(Schedule schedule) {
        final CommandContext.Builder builder = createCommandContext().toBuilder()
                                                                     .setSchedule(schedule);
        return builder.build();
    }
}
