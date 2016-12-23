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

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;
import static org.spine3.time.Calendars.createTimeWithZoneOffset;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getHours;
import static org.spine3.time.Calendars.getMinutes;
import static org.spine3.time.Calendars.getMonth;
import static org.spine3.time.Calendars.getSeconds;
import static org.spine3.time.Calendars.getYear;
import static org.spine3.time.Calendars.getZoneOffset;

@SuppressWarnings("InstanceMethodNamingConvention")

public class OffsetDateTimesShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(OffsetDateTimes.class));
    }

    @Test
    public void obtain_current_OffsetDateTime_using_ZoneOffset() {
        final ZoneOffset inKiev = ZoneOffsets.ofHours(3);
        final OffsetDateTime today = OffsetDateTimes.now(inKiev);
        final Calendar cal = createTimeWithZoneOffset(inKiev);


        assertEquals(getYear(cal), today.getDate().getYear());
        assertEquals(getMonth(cal), today.getDate().getMonthValue());
        assertEquals(getDay(cal), today.getDate().getDay());
        assertEquals(getHours(cal), today.getTime().getHours());
        assertEquals(getMinutes(cal), today.getTime().getMinutes());
        assertEquals(getSeconds(cal), today.getTime().getSeconds());
        assertEquals(getZoneOffset(cal), today.getOffset().getAmountSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_current_OffsetDateTime_using_OffsetDate_OffsetTime_ZoneOffset() {
        final int year = 2006;
        final MonthOfYear month = MonthOfYear.APRIL;
        final int day = 25;
        final int hours = 12;
        final int minutes = 5;
        final int seconds = 23;
        final int millis = 124;
        final long nanos = 122L;
        final ZoneOffset inDelhi = ZoneOffsets.ofHoursMinutes(3, 30);
        final LocalDate someDay = LocalDates.of(year, month, day);
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final OffsetDateTime todayInDelhi = OffsetDateTimes.of(someDay, localTime, inDelhi);

        assertTrue(year == todayInDelhi.getDate().getYear());
        assertTrue(month == todayInDelhi.getDate().getMonth());
        assertTrue(day == todayInDelhi.getDate().getDay());
        assertTrue(hours == todayInDelhi.getTime().getHours());
        assertTrue(minutes == todayInDelhi.getTime().getMinutes());
        assertTrue(seconds == todayInDelhi.getTime().getSeconds());
        assertTrue(millis == todayInDelhi.getTime().getMillis());
        assertTrue(nanos == todayInDelhi.getTime().getNanos());
        assertTrue(inDelhi.getAmountSeconds() == todayInDelhi.getOffset().getAmountSeconds());
    }

}
