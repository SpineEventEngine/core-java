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

package io.spine.testdata;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.base.ActorContext;
import io.spine.base.CommandContext;
import io.spine.base.CommandContext.Schedule;
import io.spine.users.TenantId;
import io.spine.users.UserId;

import static io.spine.base.Identifier.newUuid;
import static io.spine.test.Values.newUserId;
import static io.spine.time.Time.getCurrentTime;
import static io.spine.time.ZoneOffsets.UTC;

/**
 * Creates Context for tests.
 *
 * @author Mikhail Mikhaylov
 */
public class TestCommandContextFactory {

    private TestCommandContextFactory() {
    }

    /** Creates a new {@link CommandContext} instance. */
    public static CommandContext createCommandContext() {
        final UserId userId = newUserId(newUuid());
        final Timestamp now = getCurrentTime();
        return createCommandContext(userId, now);
    }

    /** Creates a new {@link CommandContext} instance. */
    public static CommandContext createCommandContext(UserId userId, Timestamp when) {

        //TODO:2017-03-23:alexander.yevsyukov: Generate commands using TestActorRequestFactory

        final TenantId.Builder generatedTenantId = TenantId.newBuilder()
                                                           .setValue(newUuid());
        final ActorContext.Builder actorContext = ActorContext.newBuilder()
                                                              .setActor(userId)
                                                              .setTimestamp(when)
                                                              .setZoneOffset(UTC)
                                                              .setTenantId(generatedTenantId);
        final CommandContext.Builder builder = CommandContext.newBuilder()
                                                             .setActorContext(actorContext);
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
