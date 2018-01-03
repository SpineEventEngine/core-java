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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.core.ActorContext;
import io.spine.core.CommandContext;
import io.spine.core.CommandContext.Schedule;
import io.spine.core.UserId;
import io.spine.test.Tests;
import io.spine.time.Durations2;
import org.junit.Test;

import static com.google.protobuf.util.Timestamps.add;
import static io.spine.core.given.GivenUserId.newUuid;
import static io.spine.time.Durations2.fromMinutes;
import static io.spine.time.Time.getCurrentTime;
import static io.spine.validate.Validate.checkValid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Alex Tymchenko
 */
public class GivenCommandContextShould {

    @Test
    public void have_utility_ctor() {
        Tests.assertHasPrivateParameterlessCtor(GivenCommandContext.class);
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(UserId.class, UserId.getDefaultInstance())
                .setDefault(Timestamp.class, Timestamp.getDefaultInstance())
                .testAllPublicStaticMethods(GivenCommandContext.class);
    }

    @Test
    public void create_with_random_actor() {
        final CommandContext first = GivenCommandContext.withRandomActor();
        final CommandContext second = GivenCommandContext.withRandomActor();

        checkValid(first);
        checkValid(second);

        final ActorContext firstActorContext = first.getActorContext();
        final ActorContext secondActorContext = second.getActorContext();
        assertNotEquals(firstActorContext.getActor(), secondActorContext.getActor());
    }

    @Test
    public void create_with_actor_and_time() {
        final UserId actorId = newUuid();
        final Timestamp when = add(getCurrentTime(), fromMinutes(42));

        final CommandContext context = GivenCommandContext.withActorAndTime(actorId, when);
        checkValid(context);

        final ActorContext actualActorContext = context.getActorContext();

        assertEquals(actorId, actualActorContext.getActor());
        assertEquals(when, actualActorContext.getTimestamp());
    }

    @Test
    public void create_with_scheduled_delay() {
        final Duration delay = Durations2.fromHours(42);
        final Schedule expectedSchedule = Schedule.newBuilder()
                                                  .setDelay(delay)
                                                  .build();

        final CommandContext context = GivenCommandContext.withScheduledDelayOf(delay);
        checkValid(context);

        final Schedule actualSchedule = context.getSchedule();
        assertEquals(expectedSchedule, actualSchedule);
    }
}
