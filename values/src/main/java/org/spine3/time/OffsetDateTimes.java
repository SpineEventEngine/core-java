/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
package org.spine3.time;

import org.spine3.change.Changes;

import java.util.Calendar;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.change.Changes.*;
import static org.spine3.time.Calendars.createDateWithZoneOffset;
import static org.spine3.time.Calendars.createTimeWithZoneOffset;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getHours;
import static org.spine3.time.Calendars.getMinutes;
import static org.spine3.time.Calendars.getSeconds;
import static org.spine3.time.Calendars.getYear;

/**
 * Routines for working with {@link OffsetDateTime}.
 *
 * @author Alexander Aleksandrov
 */
public class OffsetDateTimes {

    private OffsetDateTimes() {
    }

    /**
     * Obtains current OffsetDateTime instance using {@code ZoneOffset}.
     */
    public static OffsetDateTime now(ZoneOffset zoneOffset) {
        checkNotNull(zoneOffset, ErrorMessage.ZONE_OFFSET);
        final Calendar time = createTimeWithZoneOffset(zoneOffset);
        final LocalTime localTime = LocalTimes.of(getHours(time), getMinutes(time), getSeconds(time));
        final Calendar date = createDateWithZoneOffset(zoneOffset);
        final LocalDate localDate = LocalDates.of(getYear(date), MonthOfYears.getMonth(date), getDay(date));

        final OffsetDateTime result = OffsetDateTime.newBuilder()
                                                    .setDate(localDate)
                                                    .setTime(localTime)
                                                    .setOffset(zoneOffset)
                                                    .build();
        return result;
    }

    /**
     * Obtains OffsetDateTime instance using {@code LocalDate}, {@code LocalTime} and {@code ZoneOffset}.
     */
    public static OffsetDateTime of(LocalDate localDate, LocalTime localTime, ZoneOffset zoneOffset) {
        final OffsetDateTime result = OffsetDateTime.newBuilder()
                                                    .setDate(localDate)
                                                    .setTime(localTime)
                                                    .setOffset(zoneOffset)
                                                    .build();
        return result;
    }
}
