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
package io.spine.testing.core.given;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.core.ActorContext;
import io.spine.core.CommandContext;
import io.spine.core.CommandContext.Schedule;
import io.spine.core.UserId;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.protobuf.util.Timestamps.add;
import static io.spine.base.Time.currentTime;
import static io.spine.protobuf.Durations2.hours;
import static io.spine.protobuf.Durations2.minutes;
import static io.spine.testing.core.given.GivenUserId.newUuid;
import static io.spine.validate.Validate.checkValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("GivenCommandContext should")
class GivenCommandContextTest extends UtilityClassTest<GivenCommandContext> {

    GivenCommandContextTest() {
        super(GivenCommandContext.class);
    }

    @Override
    protected void configure(NullPointerTester tester) {
        super.configure(tester);
        tester.setDefault(UserId.class, UserId.getDefaultInstance())
              .setDefault(Timestamp.class, Timestamp.getDefaultInstance());
    }

    @Test
    @DisplayName("create CommandContext with random actor")
    void createWithRandomActor() {
        CommandContext first = GivenCommandContext.withRandomActor();
        CommandContext second = GivenCommandContext.withRandomActor();

        checkValid(first);
        checkValid(second);

        ActorContext firstActorContext = first.getActorContext();
        ActorContext secondActorContext = second.getActorContext();
        assertNotEquals(firstActorContext.getActor(), secondActorContext.getActor());
    }

    @Test
    @DisplayName("create CommandContext with actor and time")
    void createWithActorAndTime() {
        UserId actorId = newUuid();
        Timestamp when = add(currentTime(), minutes(100));

        CommandContext context = GivenCommandContext.withActorAndTime(actorId, when);
        checkValid(context);

        ActorContext actualActorContext = context.getActorContext();

        assertEquals(actorId, actualActorContext.getActor());
        assertEquals(when, actualActorContext.getTimestamp());
    }

    @Test
    @DisplayName("create CommandContext with scheduled delay")
    void createWithScheduledDelay() {
        Duration delay = hours(42);
        Schedule expectedSchedule = Schedule
                .newBuilder()
                .setDelay(delay)
                .build();

        CommandContext context = GivenCommandContext.withScheduledDelayOf(delay);
        checkValid(context);

        Schedule actualSchedule = context.getSchedule();
        assertEquals(expectedSchedule, actualSchedule);
    }
}
