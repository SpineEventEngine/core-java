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

package io.spine.core.given;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.core.ActorContext;
import io.spine.core.CommandContext;
import io.spine.core.CommandContext.Schedule;
import io.spine.core.TenantId;
import io.spine.core.UserId;

import static io.spine.time.Time.getCurrentTime;
import static io.spine.time.ZoneOffsets.UTC;

/**
 * Creates Context for tests.
 *
 * @author Mikhail Mikhaylov
 */
public class GivenCommandContext {

    private GivenCommandContext() {
    }

    /** Creates a new {@link CommandContext} instance. */
    public static CommandContext withRandomUser() {
        final UserId userId = GivenUserId.newUuid();
        final Timestamp now = getCurrentTime();
        return withUserAndTime(userId, now);
    }

    /** Creates a new {@link CommandContext} instance. */
    public static CommandContext withUserAndTime(UserId userId, Timestamp when) {
        final TenantId tenantId = GivenTenantId.newUuid();
        final ActorContext.Builder actorContext = ActorContext.newBuilder()
                                                              .setActor(userId)
                                                              .setTimestamp(when)
                                                              .setZoneOffset(UTC)
                                                              .setTenantId(tenantId);
        final CommandContext.Builder builder = CommandContext.newBuilder()
                                                             .setActorContext(actorContext);
        return builder.build();
    }

    /** Creates a new context with the given delay before the delivery time. */
    public static CommandContext withScheduledDelayOf(Duration delay) {
        final Schedule schedule = Schedule.newBuilder()
                                          .setDelay(delay)
                                          .build();
        return withSchedule(schedule);
    }

    /** Creates a new context with the given scheduling options. */
    private static CommandContext withSchedule(Schedule schedule) {
        final CommandContext.Builder builder = withRandomUser().toBuilder()
                                                               .setSchedule(schedule);
        return builder.build();
    }
}
