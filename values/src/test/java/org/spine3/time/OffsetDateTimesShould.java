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

    private static final ZoneOffset ZONE_OFFSET = ZoneOffsets.ofHoursMinutes(3, 30);
    private static final int year = 2012;
    private static final MonthOfYear month = MonthOfYear.JULY;
    private static final int day = 16;
    private static final int hours = 9;
    private static final int minutes = 30;
    private static final int seconds = 23;
    private static final int millis = 124;
    private static final long nanos = 122L;
    private static final LocalDate localDate = LocalDates.of(year, month, day);
    private static final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(OffsetDateTimes.class));
    }

    @Test
    public void obtain_current_OffsetDateTime_using_ZoneOffset() {
        final OffsetDateTime today = OffsetDateTimes.now(ZONE_OFFSET);
        final Calendar cal = createTimeWithZoneOffset(ZONE_OFFSET);

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
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);

        assertTrue(year == offsetDateTime.getDate().getYear());
        assertTrue(month == offsetDateTime.getDate().getMonth());
        assertTrue(day == offsetDateTime.getDate().getDay());
        assertTrue(hours == offsetDateTime.getTime().getHours());
        assertTrue(minutes == offsetDateTime.getTime().getMinutes());
        assertTrue(seconds == offsetDateTime.getTime().getSeconds());
        assertTrue(millis == offsetDateTime.getTime().getMillis());
        assertTrue(nanos == offsetDateTime.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == offsetDateTime.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_past_before_specified_number_of_years() {
        final int yearsToSubtract = 3;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime minusYears = OffsetDateTimes.minusYears(offsetDateTime, yearsToSubtract);

        assertTrue(year - yearsToSubtract == minusYears.getDate().getYear());
        assertTrue(month == minusYears.getDate().getMonth());
        assertTrue(day == minusYears.getDate().getDay());
        assertTrue(hours == minusYears.getTime().getHours());
        assertTrue(minutes == minusYears.getTime().getMinutes());
        assertTrue(seconds == minusYears.getTime().getSeconds());
        assertTrue(millis == minusYears.getTime().getMillis());
        assertTrue(nanos == minusYears.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == minusYears.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_past_before_specified_number_of_months() {
        final int monthsToSubtract = 3;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime minusYears = OffsetDateTimes.minusMonths(offsetDateTime, monthsToSubtract);

        assertTrue(year == minusYears.getDate().getYear());
        assertTrue(month.getNumber() - monthsToSubtract == minusYears.getDate().getMonth().getNumber());
        assertTrue(day == minusYears.getDate().getDay());
        assertTrue(hours == minusYears.getTime().getHours());
        assertTrue(minutes == minusYears.getTime().getMinutes());
        assertTrue(seconds == minusYears.getTime().getSeconds());
        assertTrue(millis == minusYears.getTime().getMillis());
        assertTrue(nanos == minusYears.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == minusYears.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_past_before_specified_number_of_days() {
        final int daysToSubtract = 3;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime minusYears = OffsetDateTimes.minusDays(offsetDateTime, daysToSubtract);

        assertTrue(year == minusYears.getDate().getYear());
        assertTrue(month == minusYears.getDate().getMonth());
        assertTrue(day - daysToSubtract== minusYears.getDate().getDay());
        assertTrue(hours == minusYears.getTime().getHours());
        assertTrue(minutes == minusYears.getTime().getMinutes());
        assertTrue(seconds == minusYears.getTime().getSeconds());
        assertTrue(millis == minusYears.getTime().getMillis());
        assertTrue(nanos == minusYears.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == minusYears.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_years() {
        final int yearsToAdd = 3;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime plusYears = OffsetDateTimes.plusYears(offsetDateTime, yearsToAdd);

        assertTrue(year + yearsToAdd == plusYears.getDate().getYear());
        assertTrue(month == plusYears.getDate().getMonth());
        assertTrue(day == plusYears.getDate().getDay());
        assertTrue(hours == plusYears.getTime().getHours());
        assertTrue(minutes == plusYears.getTime().getMinutes());
        assertTrue(seconds == plusYears.getTime().getSeconds());
        assertTrue(millis == plusYears.getTime().getMillis());
        assertTrue(nanos == plusYears.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == plusYears.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_months() {
        final int monthsToAdd = 3;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime plusYears = OffsetDateTimes.plusMonths(offsetDateTime, monthsToAdd);

        assertTrue(year == plusYears.getDate().getYear());
        assertTrue(month.getNumber() + monthsToAdd == plusYears.getDate().getMonth().getNumber());
        assertTrue(day == plusYears.getDate().getDay());
        assertTrue(hours == plusYears.getTime().getHours());
        assertTrue(minutes == plusYears.getTime().getMinutes());
        assertTrue(seconds == plusYears.getTime().getSeconds());
        assertTrue(millis == plusYears.getTime().getMillis());
        assertTrue(nanos == plusYears.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == plusYears.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_days() {
        final int daysToAdd = 3;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime plusYears = OffsetDateTimes.plusDays(offsetDateTime, daysToAdd);

        assertTrue(year == plusYears.getDate().getYear());
        assertTrue(month == plusYears.getDate().getMonth());
        assertTrue(day + daysToAdd== plusYears.getDate().getDay());
        assertTrue(hours == plusYears.getTime().getHours());
        assertTrue(minutes == plusYears.getTime().getMinutes());
        assertTrue(seconds == plusYears.getTime().getSeconds());
        assertTrue(millis == plusYears.getTime().getMillis());
        assertTrue(nanos == plusYears.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == plusYears.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_past_before_specified_number_of_hours() {
        final int hoursToSubtract = 4;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime minusHours = OffsetDateTimes.minusHours(offsetDateTime, hoursToSubtract);

        assertTrue(year == minusHours.getDate().getYear());
        assertTrue(month.getNumber() == minusHours.getDate().getMonth().getNumber());
        assertTrue(day == minusHours.getDate().getDay());
        assertTrue(hours - hoursToSubtract== minusHours.getTime().getHours());
        assertTrue(minutes == minusHours.getTime().getMinutes());
        assertTrue(seconds == minusHours.getTime().getSeconds());
        assertTrue(millis == minusHours.getTime().getMillis());
        assertTrue(nanos == minusHours.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == minusHours.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_past_before_specified_number_of_minutes() {
        final int minutesToSubtract = 11;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime minusMinutes= OffsetDateTimes.minusMinutes(offsetDateTime, minutesToSubtract);

        assertTrue(year == minusMinutes.getDate().getYear());
        assertTrue(month.getNumber() == minusMinutes.getDate().getMonth().getNumber());
        assertTrue(day == minusMinutes.getDate().getDay());
        assertTrue(hours == minusMinutes.getTime().getHours());
        assertTrue(minutes - minutesToSubtract == minusMinutes.getTime().getMinutes());
        assertTrue(seconds == minusMinutes.getTime().getSeconds());
        assertTrue(millis == minusMinutes.getTime().getMillis());
        assertTrue(nanos == minusMinutes.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == minusMinutes.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_past_before_specified_number_of_seconds() {
        final int secondsToSubtract = 18;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime minusSeconds = OffsetDateTimes.minusSeconds(offsetDateTime, secondsToSubtract);

        assertTrue(year == minusSeconds.getDate().getYear());
        assertTrue(month.getNumber() == minusSeconds.getDate().getMonth().getNumber());
        assertTrue(day == minusSeconds.getDate().getDay());
        assertTrue(hours == minusSeconds.getTime().getHours());
        assertTrue(minutes == minusSeconds.getTime().getMinutes());
        assertTrue(seconds - secondsToSubtract== minusSeconds.getTime().getSeconds());
        assertTrue(millis == minusSeconds.getTime().getMillis());
        assertTrue(nanos == minusSeconds.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == minusSeconds.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_past_before_specified_number_of_millis() {
        final int millisToSubtract = 118;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime minusMillis = OffsetDateTimes.minusMillis(offsetDateTime, millisToSubtract);

        assertTrue(year == minusMillis.getDate().getYear());
        assertTrue(month.getNumber() == minusMillis.getDate().getMonth().getNumber());
        assertTrue(day == minusMillis.getDate().getDay());
        assertTrue(hours == minusMillis.getTime().getHours());
        assertTrue(minutes == minusMillis.getTime().getMinutes());
        assertTrue(seconds == minusMillis.getTime().getSeconds());
        assertTrue(millis - millisToSubtract == minusMillis.getTime().getMillis());
        assertTrue(nanos == minusMillis.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == minusMillis.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_hours() {
        final int hoursToAdd = 2;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime plusHours = OffsetDateTimes.plusHours(offsetDateTime, hoursToAdd);

        assertTrue(year == plusHours.getDate().getYear());
        assertTrue(month.getNumber() == plusHours.getDate().getMonth().getNumber());
        assertTrue(day == plusHours.getDate().getDay());
        assertTrue(hours + hoursToAdd== plusHours.getTime().getHours());
        assertTrue(minutes == plusHours.getTime().getMinutes());
        assertTrue(seconds == plusHours.getTime().getSeconds());
        assertTrue(millis == plusHours.getTime().getMillis());
        assertTrue(nanos == plusHours.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == plusHours.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_minutes() {
        final int minutesToAdd = 11;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime plusMinutes= OffsetDateTimes.plusMinutes(offsetDateTime, minutesToAdd);

        assertTrue(year == plusMinutes.getDate().getYear());
        assertTrue(month.getNumber() == plusMinutes.getDate().getMonth().getNumber());
        assertTrue(day == plusMinutes.getDate().getDay());
        assertTrue(hours == plusMinutes.getTime().getHours());
        assertTrue(minutes + minutesToAdd == plusMinutes.getTime().getMinutes());
        assertTrue(seconds == plusMinutes.getTime().getSeconds());
        assertTrue(millis == plusMinutes.getTime().getMillis());
        assertTrue(nanos == plusMinutes.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == plusMinutes.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_seconds() {
        final int secondsToAdd = 18;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime plusSeconds = OffsetDateTimes.plusSeconds(offsetDateTime, secondsToAdd);

        assertTrue(year == plusSeconds.getDate().getYear());
        assertTrue(month.getNumber() == plusSeconds.getDate().getMonth().getNumber());
        assertTrue(day == plusSeconds.getDate().getDay());
        assertTrue(hours == plusSeconds.getTime().getHours());
        assertTrue(minutes == plusSeconds.getTime().getMinutes());
        assertTrue(seconds + secondsToAdd== plusSeconds.getTime().getSeconds());
        assertTrue(millis == plusSeconds.getTime().getMillis());
        assertTrue(nanos == plusSeconds.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == plusSeconds.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_millis() {
        final int millisToAdd = 118;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(localDate, localTime, ZONE_OFFSET);
        final OffsetDateTime plusMillis = OffsetDateTimes.plusMillis(offsetDateTime, millisToAdd);

        assertTrue(year == plusMillis.getDate().getYear());
        assertTrue(month.getNumber() == plusMillis.getDate().getMonth().getNumber());
        assertTrue(day == plusMillis.getDate().getDay());
        assertTrue(hours == plusMillis.getTime().getHours());
        assertTrue(minutes == plusMillis.getTime().getMinutes());
        assertTrue(seconds == plusMillis.getTime().getSeconds());
        assertTrue(millis + millisToAdd == plusMillis.getTime().getMillis());
        assertTrue(nanos == plusMillis.getTime().getNanos());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == plusMillis.getOffset().getAmountSeconds());
    }
}
