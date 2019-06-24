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

package io.spine.server.trace.given;

import io.spine.test.trace.Airport;
import io.spine.test.trace.AirportId;
import io.spine.test.trace.Boarding;
import io.spine.test.trace.CancelFlight;
import io.spine.test.trace.Flight;
import io.spine.test.trace.FlightId;
import io.spine.test.trace.ScheduleFlight;
import io.spine.test.trace.Timetable;
import io.spine.time.LocalDateTime;
import io.spine.time.LocalDateTimes;
import io.spine.time.LocalDates;
import io.spine.time.LocalTimes;
import io.spine.time.Month;
import io.spine.time.ZoneId;
import io.spine.time.ZoneIds;
import io.spine.time.ZonedDateTime;
import io.spine.time.ZonedDateTimes;
import io.spine.type.TypeUrl;

/**
 * The data factory for tests of the Spine Trace API.
 */
public final class TracingTestEnv {

    public static final AirportId FROM = AirportId
            .newBuilder()
            .setCode("KBP")
            .vBuild();
    private static final Airport FROM_AIRPORT = Airport
            .newBuilder()
            .setId(FROM)
            .setCity("Kyiv")
            .setCountry("Ukraine")
            .vBuild();
    private static final AirportId TO = AirportId
            .newBuilder()
            .setCode("HRK")
            .vBuild();
    private static final Airport TO_AIRPORT = Airport
            .newBuilder()
            .setId(TO)
            .setCity("Kharkiv")
            .setCountry("Ukraine")
            .vBuild();
    private static final ZoneId ZONE = ZoneIds.of(java.time.ZoneId.systemDefault());
    public static final FlightId FLIGHT = FlightId.generate();
    public static final TypeUrl FLIGHT_TYPE = TypeUrl.of(Flight.class);
    public static final TypeUrl BOARDING_TYPE = TypeUrl.of(Boarding.class);
    public static final TypeUrl TIMETABLE_TYPE = TypeUrl.of(Timetable.class);

    /**
     * Prevents the utility class instantiation.
     */
    private TracingTestEnv() {
    }

    public static ScheduleFlight scheduleFlight() {
        LocalDateTime localDepartureTime = LocalDateTimes.of(
                LocalDates.of(2019, Month.JUNE, 20),
                LocalTimes.of(7, 30, 0)
        );
        LocalDateTime.Builder builder = localDepartureTime.toBuilder();
        builder.getTimeBuilder()
               .setHour(8);
        LocalDateTime localArrivalTime = builder.vBuild();
        ZonedDateTime departureTime = ZonedDateTimes.of(localDepartureTime, ZONE);
        ZonedDateTime arrivalTime = ZonedDateTimes.of(localArrivalTime, ZONE);
        return ScheduleFlight
                .newBuilder()
                .setFrom(FROM_AIRPORT)
                .setTo(TO_AIRPORT)
                .setId(FLIGHT)
                .setScheduledDeparture(departureTime)
                .setScheduledArrival(arrivalTime)
                .vBuild();
    }

    public static CancelFlight cancelFlight() {
        return CancelFlight
                .newBuilder()
                .setId(FLIGHT)
                .vBuild();
    }
}
