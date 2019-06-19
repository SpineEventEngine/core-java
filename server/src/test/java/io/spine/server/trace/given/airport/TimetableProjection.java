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

import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.test.trace.AirportId;
import io.spine.test.trace.FlightCanceled;
import io.spine.test.trace.FlightId;
import io.spine.test.trace.FlightRescheduled;
import io.spine.test.trace.FlightScheduled;
import io.spine.test.trace.Timetable;
import io.spine.test.trace.Timetable.Schedule;
import io.spine.time.ZonedDateTime;

import java.util.List;

import static io.spine.util.Exceptions.newIllegalStateException;

final class TimetableProjection extends Projection<AirportId, Timetable, Timetable.Builder> {

    @Subscribe
    void on(FlightScheduled event) {
        Schedule schedule = Schedule
                .newBuilder()
                .setFlight(event.getId())
                .setDestination(event.getTo()
                                     .getId())
                .setScheduledDeparture(event.getScheduledDeparture())
                .vBuild();
        builder().addScheduledFlight(schedule);
    }

    @Subscribe
    void on(FlightRescheduled event) {
        ZonedDateTime newDepartureTime = event.getScheduledDeparture();
        findFlight(event.getId())
                .setScheduledDeparture(newDepartureTime);
    }

    @Subscribe
    void on(FlightCanceled event) {
        builder().getScheduledFlightList()
                 .removeIf(schedule -> schedule.getFlight().equals(event.getId()));
    }

    private Schedule.Builder findFlight(FlightId id) {
        List<Schedule.Builder> schedules = builder().getScheduledFlightBuilderList();
        Schedule.Builder flight = schedules
                .stream()
                .filter(schedule -> schedule.getFlight()
                                            .equals(id))
                .findAny()
                .orElseThrow(() -> newIllegalStateException("Unknown flight %s.", id.getUuid()));
        return flight;
    }
}
