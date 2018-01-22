/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
 * Factory methods to create {@code CommandContext} instances for test purposes.
 *
 * @author Mikhail Mikhaylov
 */
public class GivenCommandContext {

    private GivenCommandContext() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates a new {@link CommandContext} instance with a randomly
     * generated {@linkplain UserId actor} and current timestamp as a creation date.
     *
     * @return a new {@code CommandContext} instance
     */
    public static CommandContext withRandomActor() {
        final UserId userId = GivenUserId.newUuid();
        final Timestamp now = getCurrentTime();
        return withActorAndTime(userId, now);
    }

    /**
     * Creates a new {@link CommandContext} instance based upon given actor and creation date.
     *
     * @return a new {@code CommandContext} instance
     */
    public static CommandContext withActorAndTime(UserId actor, Timestamp when) {
        final TenantId tenantId = GivenTenantId.newUuid();
        final ActorContext.Builder actorContext = ActorContext.newBuilder()
                                                              .setActor(actor)
                                                              .setTimestamp(when)
                                                              .setZoneOffset(UTC)
                                                              .setTenantId(tenantId);
        final CommandContext.Builder builder = CommandContext.newBuilder()
                                                             .setActorContext(actorContext);
        return builder.build();
    }

    /**
     * Creates a new {@link CommandContext} with the given delay before the delivery time.
     *
     * <p>The actor identifier for the context is generated randomly.
     *
     * @return a new {@code CommandContext} instance
     */
    public static CommandContext withScheduledDelayOf(Duration delay) {
        final Schedule schedule = Schedule.newBuilder()
                                          .setDelay(delay)
                                          .build();
        return withSchedule(schedule);
    }

    /**
     * Creates a new {@link CommandContext} with the given scheduling options.
     *
     * <p>The actor identifier for the context is generated randomly.
     *
     * @return a new {@code CommandContext} instance
     */
    private static CommandContext withSchedule(Schedule schedule) {
        final CommandContext.Builder builder = withRandomActor().toBuilder()
                                                                .setSchedule(schedule);
        return builder.build();
    }
}
