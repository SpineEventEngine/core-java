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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;

import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.time.Calendars.at;
import static io.spine.time.Calendars.getDay;
import static io.spine.time.Calendars.getHours;
import static io.spine.time.Calendars.getMinutes;
import static io.spine.time.Calendars.getMonth;
import static io.spine.time.Calendars.getSeconds;
import static io.spine.time.Calendars.getYear;
import static io.spine.time.Calendars.getZoneOffset;
import static io.spine.time.OffsetDateTimes.addDays;
import static io.spine.time.OffsetDateTimes.addHours;
import static io.spine.time.OffsetDateTimes.addMillis;
import static io.spine.time.OffsetDateTimes.addMinutes;
import static io.spine.time.OffsetDateTimes.addMonths;
import static io.spine.time.OffsetDateTimes.addSeconds;
import static io.spine.time.OffsetDateTimes.addYears;
import static io.spine.time.OffsetDateTimes.of;
import static io.spine.time.OffsetDateTimes.subtractDays;
import static io.spine.time.OffsetDateTimes.subtractHours;
import static io.spine.time.OffsetDateTimes.subtractMillis;
import static io.spine.time.OffsetDateTimes.subtractMinutes;
import static io.spine.time.OffsetDateTimes.subtractMonths;
import static io.spine.time.OffsetDateTimes.subtractSeconds;
import static io.spine.time.OffsetDateTimes.subtractYears;
import static io.spine.time.Time.getCurrentTime;
import static org.junit.Assert.assertEquals;

public class OffsetDateTimesShould extends AbstractZonedTimeTest {

    private static final int YEAR = 2012;
    private static final MonthOfYear MONTH = MonthOfYear.JULY;
    private static final int DAY = 16;
    private static final int HOURS = 9;
    private static final int MINUTES = 30;
    private static final int SECONDS = 23;
    private static final int MILLIS = 124;
    private static final int NANOS = 122;

    private LocalDate gmtToday;
    private LocalTime now;
    private OffsetDateTime todayNow;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        gmtToday = LocalDates.of(YEAR, MONTH, DAY);
        now = LocalTimes.of(HOURS, MINUTES, SECONDS, MILLIS, NANOS);
        todayNow = OffsetDateTimes.now(zoneOffset);
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

        assertEquals(YEAR + yearsToAdd, date.getYear());
        assertEquals(MONTH, date.getMonth());
        assertEquals(DAY, date.getDay());
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

        assertEquals(YEAR - yearsToSubtract, date.getYear());
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

        assertEquals(YEAR, date.getYear());
        assertEquals(DAY, date.getDay());
        assertEquals(MONTH.getNumber() + monthsToAdd, date.getMonth()
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

        assertEquals(YEAR, date.getYear());
        assertEquals(MONTH.getNumber() - monthsToSubtract, date.getMonth()
                                                               .getNumber());
        assertEquals(DAY, date.getDay());
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

        assertEquals(YEAR, date.getYear());
        assertEquals(MONTH, date.getMonth());
        assertEquals(DAY + daysToAdd, date.getDay());
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

        assertEquals(YEAR, date.getYear());
        assertEquals(MONTH, date.getMonth());
        assertEquals(DAY - daysToSubtract, date.getDay());
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
        assertEquals(HOURS + hoursToAdd, time.getHours());
        assertEquals(MINUTES, time.getMinutes());
        assertEquals(SECONDS, time.getSeconds());
        assertEquals(MILLIS, time.getMillis());
        assertEquals(NANOS, time.getNanos());
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
        assertEquals(HOURS - hoursToSubtract, time.getHours());
        assertEquals(MINUTES, time.getMinutes());
        assertEquals(SECONDS, time.getSeconds());
        assertEquals(MILLIS, time.getMillis());
        assertEquals(NANOS, time.getNanos());
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
        assertEquals(HOURS, time.getHours());
        assertEquals(MINUTES + minutesToAdd, time.getMinutes());
        assertEquals(SECONDS, time.getSeconds());
        assertEquals(MILLIS, time.getMillis());
        assertEquals(NANOS, time.getNanos());
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
        assertEquals(HOURS, time.getHours());
        assertEquals(MINUTES - minutesToSubtract, time.getMinutes());
        assertEquals(SECONDS, time.getSeconds());
        assertEquals(MILLIS, time.getMillis());
        assertEquals(NANOS, time.getNanos());
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
        assertEquals(HOURS, time.getHours());
        assertEquals(MINUTES, time.getMinutes());
        assertEquals(SECONDS + secondsToAdd, time.getSeconds());
        assertEquals(MILLIS, time.getMillis());
        assertEquals(NANOS, time.getNanos());
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
        assertEquals(HOURS, time.getHours());
        assertEquals(MINUTES, time.getMinutes());
        assertEquals(SECONDS - secondsToSubtract, time.getSeconds());
        assertEquals(MILLIS, time.getMillis());
        assertEquals(NANOS, time.getNanos());
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
        assertEquals(HOURS, time.getHours());
        assertEquals(MINUTES, time.getMinutes());
        assertEquals(SECONDS, time.getSeconds());
        assertEquals(MILLIS + millisToAdd, time.getMillis());
        assertEquals(NANOS, time.getNanos());
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
        assertEquals(HOURS, time.getHours());
        assertEquals(MINUTES, time.getMinutes());
        assertEquals(SECONDS, time.getSeconds());
        assertEquals(MILLIS - millisToSubtract, time.getMillis());
        assertEquals(NANOS, time.getNanos());
        assertEquals(zoneOffset, offset);
    }

    @Test
    public void pass_null_tolerance_test() {
        new NullPointerTester()
                .setDefault(Timestamp.class, getCurrentTime())
                .setDefault(OffsetDateTime.class, OffsetDateTimes.now(zoneOffset))
                .setDefault(ZoneOffset.class, zoneOffset)
                .setDefault(LocalTime.class, LocalTimes.now())
                .setDefault(LocalDate.class, LocalDates.now())
                .testAllPublicStaticMethods(OffsetDateTimes.class);
    }

    /*
     * Illegal argsIllegal args. check for math with years.
     */

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_years_to_add() {
        OffsetDateTimes.addYears(todayNow, -5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_years_to_add() {
        OffsetDateTimes.addYears(todayNow, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_years_to_subtract() {
        OffsetDateTimes.subtractYears(todayNow, -6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_years_to_subtract() {
        OffsetDateTimes.subtractYears(todayNow, 0);
    }

    /*
     * Illegal args. check for math with months.
     */

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_months_to_add() {
        OffsetDateTimes.addMonths(todayNow, -5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_months_to_add() {
        OffsetDateTimes.addMonths(todayNow, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_months_to_subtract() {
        OffsetDateTimes.subtractMonths(todayNow, -6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_months_to_subtract() {
        OffsetDateTimes.subtractMonths(todayNow, 0);
    }

    /*
     * Illegal args. check for math with days.
     */

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_days_to_add() {
        OffsetDateTimes.addDays(todayNow, -5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_days_to_add() {
        OffsetDateTimes.addDays(todayNow, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_days_to_subtract() {
        OffsetDateTimes.subtractDays(todayNow, -6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_days_to_subtract() {
        OffsetDateTimes.subtractDays(todayNow, 0);
    }

    /*
     * Illegal args. check for math with hours.
     */

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_hours_to_add() {
        OffsetDateTimes.addHours(todayNow, -5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_hours_to_add() {
        OffsetDateTimes.addHours(todayNow, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_hours_to_subtract() {
        OffsetDateTimes.subtractHours(todayNow, -6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_hours_to_subtract() {
        OffsetDateTimes.subtractHours(todayNow, 0);
    }

    /*
     * Illegal args. check for math with minutes.
     */

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_minutes_to_add() {
        OffsetDateTimes.addMinutes(todayNow, -7);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_minutes_to_add() {
        OffsetDateTimes.addMinutes(todayNow, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_minutes_to_subtract() {
        OffsetDateTimes.subtractMinutes(todayNow, -8);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_minutes_to_subtract() {
        OffsetDateTimes.subtractMinutes(todayNow, 0);
    }

    /*
     * Illegal args. check for math with seconds.
     */

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_seconds_to_add() {
        OffsetDateTimes.addSeconds(todayNow, -25);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_seconds_to_add() {
        OffsetDateTimes.addSeconds(todayNow, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_seconds_to_subtract() {
        OffsetDateTimes.subtractSeconds(todayNow, -27);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_seconds_to_subtract() {
        OffsetDateTimes.subtractSeconds(todayNow, 0);
    }

    /*
     * Illegal args. check for math with millis.
     */

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_millis_to_add() {
        OffsetDateTimes.addMillis(todayNow, -500);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_millis_to_add() {
        OffsetDateTimes.addMillis(todayNow, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_millis_to_subtract() {
        OffsetDateTimes.subtractMillis(todayNow, -270);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_millis_to_subtract() {
        OffsetDateTimes.subtractMillis(todayNow, 0);
    }

    /*
     * Stringification
     */

    @Override
    protected void assertConversionAt(ZoneOffset zoneOffset) throws ParseException {
        final OffsetDateTime now = OffsetDateTimes.now(zoneOffset);
        final String str = OffsetDateTimes.toString(now);
        final OffsetDateTime parsed = OffsetDateTimes.parse(str);

        assertEquals(now, parsed);
    }
}
