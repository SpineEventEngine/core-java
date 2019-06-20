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

package io.spine.server.trace;

import io.spine.core.Command;
import io.spine.server.BoundedContext;
import io.spine.server.trace.given.FakeTracer;
import io.spine.server.trace.given.FakeTracerFactory;
import io.spine.server.trace.given.airport.AirportContext;
import io.spine.test.trace.Airport;
import io.spine.test.trace.AirportId;
import io.spine.test.trace.FlightId;
import io.spine.test.trace.ScheduleFlight;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.time.LocalDateTime;
import io.spine.time.LocalDateTimes;
import io.spine.time.LocalDates;
import io.spine.time.LocalTimes;
import io.spine.time.Month;
import io.spine.time.ZoneId;
import io.spine.time.ZoneIds;
import io.spine.time.ZonedDateTime;
import io.spine.time.ZonedDateTimes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.grpc.StreamObservers.noOpObserver;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Message tracing should")
class TracingTest {

    private static final TestActorRequestFactory requests =
            new TestActorRequestFactory(TracingTest.class);

    private static final AirportId FROM = AirportId
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
    private static final FlightId FLIGHT = FlightId.generate();

    private FakeTracerFactory tracing;
    private BoundedContext context;

    @BeforeEach
    void setUp() {
        tracing = new FakeTracerFactory();
        context = AirportContext
                .builder()
                .setTracerFactorySupplier(() -> tracing)
                .build();
    }

    @Test
    @DisplayName("trace actor commands")
    void traceCommands() {
        Command command = requests.command()
                                  .create(scheduleFlight());
        context.commandBus().post(command, noOpObserver());
        FakeTracer tracer = tracing.tracer(ScheduleFlight.class);
        assertTrue(tracer.isReceiver(FLIGHT));
    }

    private static ScheduleFlight scheduleFlight() {
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
}
