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

package io.spine.time;

import org.junit.Test;

import java.util.Calendar;

import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.time.Calendars.at;
import static io.spine.time.Calendars.createWithTime;
import static io.spine.time.Calendars.getHours;
import static io.spine.time.Calendars.getMinutes;
import static io.spine.time.Calendars.getMonthOfYear;
import static io.spine.time.Calendars.getSeconds;
import static io.spine.time.Calendars.getZoneOffset;
import static io.spine.time.Calendars.toCalendar;
import static io.spine.time.Calendars.toLocalTime;
import static org.junit.Assert.assertEquals;

public class CalendarsShould {

    @Test
    public void have_utility_constructor() {
        assertHasPrivateParameterlessCtor(Calendars.class);
    }

    @Test
    public void obtain_zoneOffset() {
        final int amountOfSeconds = 3*3600;
        final ZoneOffset zoneOffset = ZoneOffsets.ofHours(3);
        final Calendar cal = at(zoneOffset);

        assertEquals(amountOfSeconds, getZoneOffset(cal));
    }

    @Test
    public void obtain_calendar_using_hours_minutes_seconds() {
        final int hours = 3;
        final int minutes = 23;
        final int seconds = 12;
        final Calendar cal = createWithTime(hours, minutes, seconds);

        assertEquals(hours, getHours(cal));
        assertEquals(minutes, getMinutes(cal));
        assertEquals(seconds, getSeconds(cal));
    }

    @Test
    public void obtain_calendar_using_hours_minutes() {
        final int hours = 3;
        final int minutes = 23;
        final Calendar cal = createWithTime(hours, minutes);

        assertEquals(hours, getHours(cal));
        assertEquals(minutes, getMinutes(cal));
    }

    @Test
    public void obtain_month_of_year_using_calendar() {
        final Calendar calendar = Calendar.getInstance();
        int april = 3;
        calendar.set(Calendar.MONTH, april);
        // The Calendar class assumes JANUARY is zero. Therefore add 1 to expected result.
        assertEquals(calendar.get(Calendar.MONTH) + 1, getMonthOfYear(calendar).getNumber());
    }

    @Test
    public void convert_from_local_time_and_back() {
        final LocalTime time = LocalTimes.now();

        assertEquals(time, toLocalTime(toCalendar(time), time.getNanos()));
    }
}
