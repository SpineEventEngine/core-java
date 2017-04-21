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

import static com.google.protobuf.TextFormat.shortDebugString;
import static org.junit.Assert.assertEquals;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.time.Calendars.at;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getHours;
import static org.spine3.time.Calendars.getMinutes;
import static org.spine3.time.Calendars.getMonth;
import static org.spine3.time.Calendars.getSeconds;
import static org.spine3.time.Calendars.getYear;
import static org.spine3.time.Calendars.getZoneOffset;
import static org.spine3.time.OffsetDateTimes.addDays;
import static org.spine3.time.OffsetDateTimes.addHours;
import static org.spine3.time.OffsetDateTimes.addMillis;
import static org.spine3.time.OffsetDateTimes.addMinutes;
import static org.spine3.time.OffsetDateTimes.addMonths;
import static org.spine3.time.OffsetDateTimes.addSeconds;
import static org.spine3.time.OffsetDateTimes.addYears;
import static org.spine3.time.OffsetDateTimes.of;
import static org.spine3.time.OffsetDateTimes.subtractDays;
import static org.spine3.time.OffsetDateTimes.subtractHours;
import static org.spine3.time.OffsetDateTimes.subtractMillis;
import static org.spine3.time.OffsetDateTimes.subtractMinutes;
import static org.spine3.time.OffsetDateTimes.subtractMonths;
import static org.spine3.time.OffsetDateTimes.subtractSeconds;
import static org.spine3.time.OffsetDateTimes.subtractYears;

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
    private LocalTime now;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        gmtToday = LocalDates.of(year, month, day);
        now = LocalTimes.of(hours, minutes, seconds, millis, nanos);
    }

    @Test
    public void have_utility_constructor() {
        assertHasPrivateParameterlessCtor(OffsetDateTimes.class);
    }

    @Test
    public void get_current_date_time() {
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
    public void create_instance_with_date_time_at_offset() {
        final OffsetDateTime offsetDateTime = of(gmtToday, now, zoneOffset);

        final LocalDate date = offsetDateTime.getDate();
        LocalDates.checkDate(date);
        final LocalTime time = offsetDateTime.getTime();

        assertEquals(gmtToday, date);
        assertEquals(now, time);
        assertEquals(zoneOffset, offsetDateTime.getOffset());
    }

    /*
     * Math with date
     */

    @Test
    public void add_years() {
        final int yearsToAdd = 3;
        final OffsetDateTime offsetDateTime = of(gmtToday, now, zoneOffset);
        final OffsetDateTime plusYears = addYears(offsetDateTime, yearsToAdd);

        final LocalDate date = plusYears.getDate();
        final LocalTime time = plusYears.getTime();
        final ZoneOffset offset = plusYears.getOffset();

        assertEquals(year + yearsToAdd, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day, date.getDay());
        assertEquals(now, time);
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void subtract_years() {
        final int yearsToSubtract = 3;
        final OffsetDateTime offsetDateTime = of(gmtToday, now, zoneOffset);
        final OffsetDateTime minusYears = subtractYears(offsetDateTime, yearsToSubtract);

        final LocalDate date = minusYears.getDate();
        LocalDates.checkDate(date);
        final LocalTime time = minusYears.getTime();

        assertEquals(year - yearsToSubtract, date.getYear());
        assertEquals(now, time);
        assertEquals(zoneOffset, offsetDateTime.getOffset());
    }

    @Test
    public void add_months() {
        final int monthsToAdd = 3;
        final OffsetDateTime offsetDateTime = of(gmtToday, now, zoneOffset);
        final OffsetDateTime plusYears = addMonths(offsetDateTime, monthsToAdd);

        final LocalDate date = plusYears.getDate();
        final LocalTime time = plusYears.getTime();
        final ZoneOffset offset = plusYears.getOffset();

        assertEquals(year, date.getYear());
        assertEquals(day, date.getDay());
        assertEquals(month.getNumber() + monthsToAdd, date.getMonth()
                                                          .getNumber());
        assertEquals(now, time);
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void subtract_months() {
        final int monthsToSubtract = 3;
        final OffsetDateTime offsetDateTime = of(gmtToday, now, zoneOffset);
        final OffsetDateTime minusYears = subtractMonths(offsetDateTime, monthsToSubtract);

        final LocalDate date = minusYears.getDate();
        final LocalTime time = minusYears.getTime();
        LocalDates.checkDate(date);

        assertEquals(year, date.getYear());
        assertEquals(month.getNumber() - monthsToSubtract, date.getMonth()
                                                               .getNumber());
        assertEquals(day, date.getDay());
        assertEquals(now, time);
        assertEquals(zoneOffset, offsetDateTime.getOffset());
    }

    @Test
    public void add_days() {
        final int daysToAdd = 3;
        final OffsetDateTime offsetDateTime = of(gmtToday, now, zoneOffset);
        final OffsetDateTime plusYears = addDays(offsetDateTime, daysToAdd);

        final LocalDate date = plusYears.getDate();
        final LocalTime time = plusYears.getTime();
        final ZoneOffset offset = plusYears.getOffset();

        assertEquals(year, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day + daysToAdd, date.getDay());
        assertEquals(now, time);
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void subtract_days() {
        final int daysToSubtract = 3;
        final OffsetDateTime offsetDateTime = of(gmtToday, now, zoneOffset);
        final OffsetDateTime minusYears = subtractDays(offsetDateTime, daysToSubtract);

        final LocalDate date = minusYears.getDate();
        final LocalTime time = minusYears.getTime();
        final ZoneOffset offset = minusYears.getOffset();

        assertEquals(year, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day - daysToSubtract, date.getDay());
        assertEquals(now, time);
        assertEquals(zoneOffset, offset);
    }

    /*
     * Math with time
     */

    @Test
    public void add_hours() {
        final OffsetDateTime dateTime = of(gmtToday, now, zoneOffset);
        final int hoursToAdd = 2;
        final OffsetDateTime withAddedHours = addHours(dateTime, hoursToAdd);

        final LocalDate date = withAddedHours.getDate();
        final LocalTime time = withAddedHours.getTime();

        assertEquals(gmtToday, date);
        assertEquals(hours + hoursToAdd, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());
        assertEquals(zoneOffset, withAddedHours.getOffset());
    }

    @Test
    public void subtract_hours() {
        final int hoursToSubtract = 4;
        final OffsetDateTime offsetDateTime = of(gmtToday, now, zoneOffset);
        final OffsetDateTime minusHours = subtractHours(offsetDateTime, hoursToSubtract);

        final LocalDate date = minusHours.getDate();
        final LocalTime time = minusHours.getTime();
        final ZoneOffset offset = minusHours.getOffset();

        assertEquals(gmtToday, date);
        assertEquals(hours - hoursToSubtract, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void add_minutes() {
        final int minutesToAdd = 11;
        final OffsetDateTime dateTime = of(gmtToday, now, zoneOffset);
        final OffsetDateTime withAddedMinutes = addMinutes(dateTime, minutesToAdd);

        final LocalDate date = withAddedMinutes.getDate();
        final LocalTime time = withAddedMinutes.getTime();
        final ZoneOffset offset = withAddedMinutes.getOffset();

        assertEquals(gmtToday, date);
        assertEquals(hours, time.getHours());
        assertEquals(minutes + minutesToAdd, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());
        assertEquals(zoneOffset.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void subtract_minutes() {
        final int minutesToSubtract = 11;
        final OffsetDateTime offsetDateTime = of(gmtToday, now, zoneOffset);
        final OffsetDateTime minusMinutes = subtractMinutes(offsetDateTime, minutesToSubtract);

        final LocalDate date = minusMinutes.getDate();
        final LocalTime time = minusMinutes.getTime();
        final ZoneOffset offset = minusMinutes.getOffset();

        assertEquals(gmtToday, date);
        assertEquals(hours, time.getHours());
        assertEquals(minutes - minutesToSubtract, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void add_seconds() {
        final int secondsToAdd = 18;
        final OffsetDateTime dateTime = of(gmtToday, now, zoneOffset);
        final OffsetDateTime withAddedSeconds = addSeconds(dateTime, secondsToAdd);

        final LocalDate date = withAddedSeconds.getDate();
        final LocalTime time = withAddedSeconds.getTime();
        final ZoneOffset offset = withAddedSeconds.getOffset();

        assertEquals(gmtToday, date);
        assertEquals(hours, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds + secondsToAdd, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void subtract_seconds() {
        final int secondsToSubtract = 18;
        final OffsetDateTime dateTime = of(gmtToday, now, zoneOffset);
        final OffsetDateTime withSubtractedSeconds = subtractSeconds(dateTime, secondsToSubtract);

        final LocalDate date = withSubtractedSeconds.getDate();
        final LocalTime time = withSubtractedSeconds.getTime();
        final ZoneOffset offset = withSubtractedSeconds.getOffset();

        assertEquals(gmtToday, date);
        assertEquals(hours, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds - secondsToSubtract, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void add_millis() {
        final int millisToAdd = 118;
        final OffsetDateTime dateTime = of(gmtToday, now, zoneOffset);
        final OffsetDateTime withAddedMillis = addMillis(dateTime, millisToAdd);

        final LocalDate date = withAddedMillis.getDate();
        final LocalTime time = withAddedMillis.getTime();
        final ZoneOffset offset = withAddedMillis.getOffset();

        assertEquals(gmtToday, date);
        assertEquals(hours, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis + millisToAdd, time.getMillis());
        assertEquals(nanos, time.getNanos());
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void subtract_millis() {
        final int millisToSubtract = 118;
        final OffsetDateTime dateTime = of(gmtToday, now, zoneOffset);
        final OffsetDateTime withSubtractedMillis = subtractMillis(dateTime, millisToSubtract);

        final LocalDate date = withSubtractedMillis.getDate();
        final LocalTime time = withSubtractedMillis.getTime();
        final ZoneOffset offset = withSubtractedMillis.getOffset();

        assertEquals(gmtToday, date);
        assertEquals(hours, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis - millisToSubtract, time.getMillis());
        assertEquals(nanos, time.getNanos());
        assertEquals(zoneOffset, offset);
    }

    /*
     * Stringification
     */

    @Override
    protected void assertConversionAt(ZoneOffset zoneOffset) throws ParseException {
        final OffsetDateTime now = OffsetDateTimes.now(zoneOffset);
        final String str = OffsetDateTimes.toString(now);
        final OffsetDateTime parsed = OffsetDateTimes.parse(str);

        System.err.format("Now is: %s %n", shortDebugString(now));
        System.err.format("String is: %s %n", str);
        System.err.format("Parsed is: %s %n", shortDebugString(parsed));

        assertEquals(now, parsed);
    }
}
