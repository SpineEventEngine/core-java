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

package io.spine.server.trace.given.airport;

import io.spine.core.CommandContext;
import io.spine.server.command.Assign;
import io.spine.server.command.Command;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.procman.ProcessManager;
import io.spine.test.trace.Boarding;
import io.spine.test.trace.BoardingCanceled;
import io.spine.test.trace.BoardingCompleted;
import io.spine.test.trace.BoardingStarted;
import io.spine.test.trace.CancelBoarding;
import io.spine.test.trace.CompleteBoarding;
import io.spine.test.trace.FlightCanceled;
import io.spine.test.trace.FlightId;
import io.spine.test.trace.FlightScheduled;
import io.spine.time.Now;
import io.spine.time.ZonedDateTime;
import io.spine.time.ZonedDateTimes;

import java.time.Duration;

import static io.spine.test.trace.Boarding.Status.NOT_STARTED;
import static io.spine.test.trace.Boarding.Status.STARTED;
import static io.spine.time.ZonedDateTimes.toJavaTime;
import static java.time.temporal.ChronoUnit.HOURS;

final class BoardingProcman extends ProcessManager<FlightId, Boarding, Boarding.Builder> {

    @React
    BoardingStarted on(FlightScheduled event) {
        ZonedDateTime scheduledDeparture = event.getScheduledDeparture();
        Duration twoHours = Duration.of(2, HOURS);
        ZonedDateTime start = ZonedDateTimes.of(
                toJavaTime(scheduledDeparture).minus(twoHours)
        );
        builder().setFlight(event.getId())
                 .setScheduledStart(start)
                 .setScheduledEnd(scheduledDeparture)
                 .setStatus(NOT_STARTED);
        return BoardingStarted
                .newBuilder()
                .setId(id())
                .setWhen(Now.get(scheduledDeparture.getZone())
                            .asZonedDateTime())
                .vBuild();
    }

    @React
    Nothing to(BoardingStarted event) {
        builder().setWhenStarted(event.getWhen())
                 .setStatus(STARTED);
        return nothing();
    }

    @Assign
    BoardingCompleted handle(CompleteBoarding command, CommandContext context) {
        ZonedDateTime whenCompleted = now(context);
        builder().setWhenEnded(whenCompleted);
        return BoardingCompleted
                .newBuilder()
                .setId(command.getId())
                .setWhen(whenCompleted)
                .vBuild();
    }

    @Command
    CancelBoarding handle(FlightCanceled event) {
        return CancelBoarding
                .newBuilder()
                .setId(event.getId())
                .vBuild();
    }

    @Assign
    BoardingCanceled handle(CancelBoarding command, CommandContext context) {
        ZonedDateTime whenCanceled = now(context);
        return BoardingCanceled
                .newBuilder()
                .setId(command.getId())
                .setWhen(whenCanceled)
                .vBuild();
    }

    @React
    Nothing on(BoardingCanceled event) {
        builder().setWhenEnded(event.getWhen());
        return nothing();
    }

    private static ZonedDateTime now(CommandContext context) {
        return Now.get(context.getActorContext().getZoneId())
                  .asZonedDateTime();
    }
}
