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

package org.spine3.time;

import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.time.Calendars.at;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getHours;
import static org.spine3.time.Calendars.getMinutes;
import static org.spine3.time.Calendars.getMonth;
import static org.spine3.time.Calendars.getSeconds;
import static org.spine3.time.Calendars.getYear;
import static org.spine3.time.Calendars.getZoneOffset;


public class OffsetDateTimesShould extends AbstractZonedTimeTest {

    private static final int year = 2012;
    private static final MonthOfYear month = MonthOfYear.JULY;
    private static final int day = 16;
    private static final int hours = 9;
    private static final int minutes = 30;
    private static final int seconds = 23;
    private static final int millis = 124;
    private static final int nanos = 122;

    private LocalDate gmtToday;
    private OffsetDateTime todayNow;
    private LocalTime now;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        gmtToday = LocalDates.of(year, month, day);
        now = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        todayNow = OffsetDateTimes.of(gmtToday, now, zoneOffset);
    }

    @Test
    public void have_utility_constructor() {
        assertHasPrivateParameterlessCtor(OffsetDateTimes.class);
    }

    @Test
    public void obtain_current_OffsetDateTime_using_ZoneOffset() {
        final OffsetDateTime now = OffsetDateTimes.now(zoneOffset);
        final Calendar cal = at(zoneOffset);

        final LocalDate today = now.getDate();
        assertEquals(getYear(cal), today.getYear());
        assertEquals(getMonth(cal), today.getMonthValue());
        assertEquals(getDay(cal), today.getDay());

        final LocalTime time = now.getTime();
        assertEquals(getHours(cal), time.getHours());
        assertEquals(getMinutes(cal), time.getMinutes());
        assertEquals(getSeconds(cal), time.getSeconds());
        assertEquals(getZoneOffset(cal), now.getOffset()
                                            .getAmountSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_current_OffsetDateTime_using_OffsetDate_OffsetTime_ZoneOffset() {
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);

        final LocalDate date = offsetDateTime.getDate();
        final LocalTime time = offsetDateTime.getTime();

        assertEquals(gmtToday, date);
        assertEquals(now, time);
        assertEquals(zoneOffset, offsetDateTime.getOffset());
    }

    //
    // Math with date
    //------------------

    @Test
    public void subtract_years() {
        final int yearsToSubtract = 3;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final OffsetDateTime minusYears = OffsetDateTimes.subtractYears(offsetDateTime,
                                                                        yearsToSubtract);

        final LocalDate date = minusYears.getDate();
        final LocalTime time = minusYears.getTime();
        LocalDates.checkDate(date);

        assertEquals(year - yearsToSubtract, date.getYear());
        assertEquals(now, time);
        assertEquals(zoneOffset, offsetDateTime.getOffset());
    }

    @Test
    public void obtain_date_and_time_in_past_before_specified_number_of_months() {
        final int monthsToSubtract = 3;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final OffsetDateTime minusYears = OffsetDateTimes.subtractMonths(offsetDateTime,
                                                                         monthsToSubtract);

        final LocalDate date = minusYears.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month.getNumber() - monthsToSubtract,
                     date.getMonth().getNumber());
        assertEquals(day, date.getDay());

        final LocalTime time = minusYears.getTime();
        assertEquals(now, time);
        assertEquals(zoneOffset, offsetDateTime.getOffset());
    }

    @Test
    public void obtain_date_and_time_in_past_before_specified_number_of_days() {
        final int daysToSubtract = 3;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final OffsetDateTime minusYears = OffsetDateTimes.subtractDays(offsetDateTime, daysToSubtract);

        final LocalDate date = minusYears.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day - daysToSubtract, date.getDay());

        final LocalTime time = minusYears.getTime();
        assertEquals(now, time);

        final ZoneOffset offset = minusYears.getOffset();
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_years() {
        final int yearsToAdd = 3;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final OffsetDateTime plusYears = OffsetDateTimes.addYears(offsetDateTime, yearsToAdd);

        final LocalDate date = plusYears.getDate();
        assertEquals(year + yearsToAdd, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day, date.getDay());

        final LocalTime time = plusYears.getTime();
        assertEquals(now, time);

        final ZoneOffset offset = plusYears.getOffset();
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_months() {
        final int monthsToAdd = 3;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final OffsetDateTime plusYears = OffsetDateTimes.addMonths(offsetDateTime, monthsToAdd);

        final LocalDate date = plusYears.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month.getNumber() + monthsToAdd, date.getMonth()
                                                          .getNumber());
        assertEquals(day, date.getDay());

        final LocalTime time = plusYears.getTime();
        assertEquals(now, time);

        final ZoneOffset offset = plusYears.getOffset();
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_days() {
        final int daysToAdd = 3;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final OffsetDateTime plusYears = OffsetDateTimes.addDays(offsetDateTime, daysToAdd);

        final LocalDate date = plusYears.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day + daysToAdd, date.getDay());

        final LocalTime time = plusYears.getTime();
        assertEquals(now, time);

        final ZoneOffset offset = plusYears.getOffset();
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void obtain_date_and_time_in_past_before_specified_number_of_hours() {
        final int hoursToSubtract = 4;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final OffsetDateTime minusHours = OffsetDateTimes.subtractHours(offsetDateTime,
                                                                        hoursToSubtract);

        final LocalDate date = minusHours.getDate();
        assertEquals(gmtToday, date);

        final LocalTime time = minusHours.getTime();
        assertEquals(hours - hoursToSubtract, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());

        final ZoneOffset offset = minusHours.getOffset();
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void obtain_date_and_time_in_past_before_specified_number_of_minutes() {
        final int minutesToSubtract = 11;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final OffsetDateTime minusMinutes = OffsetDateTimes.subtractMinutes(offsetDateTime,
                                                                            minutesToSubtract);

        final LocalDate date = minusMinutes.getDate();
        assertEquals(gmtToday, date);

        final LocalTime time = minusMinutes.getTime();
        assertEquals(hours, time.getHours());
        assertEquals(minutes - minutesToSubtract, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());

        final ZoneOffset offset = minusMinutes.getOffset();
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void obtain_date_and_time_in_past_before_specified_number_of_seconds() {
        final int secondsToSubtract = 18;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final OffsetDateTime minusSeconds = OffsetDateTimes.subtractSeconds(offsetDateTime,
                                                                            secondsToSubtract);

        final LocalDate date = minusSeconds.getDate();

        assertEquals(gmtToday, date);

        final LocalTime time = minusSeconds.getTime();
        assertEquals(hours, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds - secondsToSubtract, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());

        final ZoneOffset offset = minusSeconds.getOffset();
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void obtain_date_and_time_in_past_before_specified_number_of_millis() {
        final int millisToSubtract = 118;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final OffsetDateTime minusMillis = OffsetDateTimes.subtractMillis(offsetDateTime,
                                                                          millisToSubtract);

        final LocalDate date = minusMillis.getDate();
        assertEquals(gmtToday, date);

        final LocalTime time = minusMillis.getTime();
        assertEquals(hours, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis - millisToSubtract, time.getMillis());
        assertEquals(nanos, time.getNanos());

        final ZoneOffset offset = minusMillis.getOffset();
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_hours() {
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final int hoursToAdd = 2;
        final OffsetDateTime plusHours = OffsetDateTimes.addHours(offsetDateTime, hoursToAdd);

        final LocalDate date = plusHours.getDate();
        assertEquals(gmtToday, date);

        final LocalTime time = plusHours.getTime();
        assertEquals(hours + hoursToAdd, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());
        assertEquals(zoneOffset, plusHours.getOffset());
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_minutes() {
        final int minutesToAdd = 11;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final OffsetDateTime plusMinutes = OffsetDateTimes.addMinutes(offsetDateTime,
                                                                      minutesToAdd);

        final LocalDate date = plusMinutes.getDate();
        assertEquals(gmtToday, date);

        final LocalTime time = plusMinutes.getTime();
        assertEquals(hours, time.getHours());
        assertEquals(minutes + minutesToAdd, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());

        final ZoneOffset offset = plusMinutes.getOffset();
        assertEquals(zoneOffset.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_seconds() {
        final int secondsToAdd = 18;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final OffsetDateTime plusSeconds = OffsetDateTimes.addSeconds(offsetDateTime,
                                                                      secondsToAdd);

        final LocalDate date = plusSeconds.getDate();
        assertEquals(gmtToday, date);

        final LocalTime time = plusSeconds.getTime();
        assertEquals(hours, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds + secondsToAdd, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());

        final ZoneOffset offset = plusSeconds.getOffset();
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void obtain_date_and_time_in_future_after_specified_number_of_millis() {
        final int millisToAdd = 118;
        final OffsetDateTime offsetDateTime = OffsetDateTimes.of(gmtToday, now, zoneOffset);
        final OffsetDateTime plusMillis = OffsetDateTimes.addMillis(offsetDateTime, millisToAdd);

        final LocalDate date = plusMillis.getDate();
        assertEquals(gmtToday, date);

        final LocalTime time = plusMillis.getTime();
        assertEquals(hours, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis + millisToAdd, time.getMillis());
        assertEquals(nanos, time.getNanos());

        final ZoneOffset offset = plusMillis.getOffset();
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void convert_values_at_UTC_to_string() throws ParseException {
        final OffsetDateTime nowAtUTC = OffsetDateTimes.now(ZoneOffsets.UTC);

        final String value = OffsetDateTimes.toString(nowAtUTC);
        assertTrue(value.contains("Z"));
        final OffsetDateTime parsed = OffsetDateTimes.parse(value);
        assertEquals(nowAtUTC, parsed);
    }

    @Test
    public void convert_values_at_current_time_zone() throws ParseException {
        // Get current zone offset and strip ID value because it's not stored into date/time.
        final ZoneOffset zoneOffset = ZoneOffsets.getDefault()
                                                 .toBuilder()
                                                 .clearId()
                                                 .build();
        final OffsetDateTime now = OffsetDateTimes.now(zoneOffset);

        final String value = OffsetDateTimes.toString(now);
        final OffsetDateTime parsed = OffsetDateTimes.parse(value);
        assertEquals(now, parsed);
    }
}
