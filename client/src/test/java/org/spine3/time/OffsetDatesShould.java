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

import com.google.common.testing.NullPointerTester;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.abs;
import static org.junit.Assert.assertEquals;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.test.Tests.random;
import static org.spine3.time.Calendars.at;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getMonth;
import static org.spine3.time.Calendars.getYear;
import static org.spine3.time.MonthOfYear.forNumber;
import static org.spine3.time.OffsetDates.addDays;
import static org.spine3.time.OffsetDates.addMonths;
import static org.spine3.time.OffsetDates.addYears;
import static org.spine3.time.OffsetDates.subtractDays;
import static org.spine3.time.OffsetDates.subtractMonths;
import static org.spine3.time.OffsetDates.subtractYears;
import static org.spine3.time.ZoneOffsets.MAX_HOURS_OFFSET;
import static org.spine3.time.ZoneOffsets.MAX_MINUTES_OFFSET;
import static org.spine3.time.ZoneOffsets.MIN_HOURS_OFFSET;
import static org.spine3.time.ZoneOffsets.MIN_MINUTES_OFFSET;

/**
 * @author Alexander Aleksandrov
 * @author Alexander Yevsyukov
 */
public class OffsetDatesShould {

    private static final int YEARS = 2017;
    private static final int MONTH_PER_YEAR = 12;
    private static final int DAY_PER_MONTH = 31;

    private LocalDate gmtToday;
    private ZoneOffset zoneOffset;
    private OffsetDate now;

    @Before
    public void setUp() {
        gmtToday = getCurrentLocalDate();
        zoneOffset = generateOffset();
        now = OffsetDates.dateAt(gmtToday, zoneOffset);
    }

    private static LocalDate getCurrentLocalDate() {
        int year = random(1, YEARS);
        MonthOfYear month = checkNotNull(forNumber(random(MONTH_PER_YEAR)));
        int day = random(1, DAY_PER_MONTH);
        return LocalDates.of(year, month, day);
    }

    private static ZoneOffset generateOffset() {
        // Reduce the hour range by one assuming minutes are also generated.
        final int hours = random(MIN_HOURS_OFFSET + 1, MAX_HOURS_OFFSET - 1);
        int minutes = random(MIN_MINUTES_OFFSET, MAX_MINUTES_OFFSET);
        // Make minutes of the same sign with hours.
        minutes = hours >= 0 ? abs(minutes) : -abs(minutes);
        return ZoneOffsets.ofHoursMinutes(hours, minutes);
    }

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(OffsetDates.class);
    }

    @Test
    public void obtain_current_OffsetDate_using_ZoneOffset() {
        final OffsetDate today = OffsetDates.now(zoneOffset);
        final Calendar cal = at(zoneOffset);

        final LocalDate date = today.getDate();
        assertEquals(getYear(cal), date.getYear());
        assertEquals(getMonth(cal), date.getMonthValue());
        assertEquals(getDay(cal), date.getDay());
        assertEquals(zoneOffset, today.getOffset());
    }

    @Test
    public void obtain_current_OffsetDate_using_LocalDate_and_ZoneOffset() {
        final OffsetDate offsetDate = OffsetDates.dateAt(gmtToday, zoneOffset);

        assertEquals(gmtToday, offsetDate.getDate());
        assertEquals(zoneOffset, offsetDate.getOffset());
    }

    @Test
    public void add_years() {
        final int yearsDelta = random(1, 2015);
        final OffsetDate offsetDate = OffsetDates.dateAt(gmtToday, zoneOffset);
        final OffsetDate offsetDatePlusYears = addYears(offsetDate, yearsDelta);
        final LocalDate date = offsetDatePlusYears.getDate();

        assertEquals(gmtToday.getYear() + yearsDelta, date.getYear());
        assertEquals(zoneOffset, offsetDate.getOffset());
    }

    @Test
    public void subtract_years() {
        final int yearsDelta = random(1, 1007);
        final OffsetDate offsetDate = OffsetDates.dateAt(gmtToday, zoneOffset);
        final OffsetDate offsetDateMinusYears = subtractYears(offsetDate, yearsDelta);

        final LocalDate date = offsetDateMinusYears.getDate();
        assertEquals(gmtToday.getYear() - yearsDelta, date.getYear());
        assertEquals(zoneOffset, offsetDate.getOffset());
    }

    @Test
    public void add_month() {
        final int monthsDelta = random(1, 12);
        final OffsetDate value = OffsetDates.dateAt(gmtToday, zoneOffset);
        final OffsetDate offsetDatePlusMonths = addMonths(value, monthsDelta);
        final int expectedMonth = (gmtToday.getMonth().getNumber() + monthsDelta) % 12;

        final LocalDate date = offsetDatePlusMonths.getDate();
        assertEquals(expectedMonth, date.getMonth().getNumber());
        assertEquals(zoneOffset, value.getOffset());
    }

    @Test
    public void subtract_month() {
        final int monthsDelta = random(1, 12);
        final OffsetDate offsetDate = OffsetDates.dateAt(gmtToday, zoneOffset);
        final OffsetDate offsetDateMinusMonths = subtractMonths(offsetDate, monthsDelta);
        final int expectedMonth = (gmtToday.getMonth().getNumber() - monthsDelta) % 12;

        final LocalDate date = offsetDateMinusMonths.getDate();

        assertEquals(expectedMonth, date.getMonth().getNumber());
        assertEquals(zoneOffset, offsetDate.getOffset());
    }

    @Test
    public void add_days() {
        final int daysDelta = random(1, 31);
        final OffsetDate offsetDate = OffsetDates.dateAt(gmtToday, zoneOffset);
        final OffsetDate offsetDatePlusMonths = addDays(offsetDate, daysDelta);

        final LocalDate date = offsetDatePlusMonths.getDate();
        assertEquals(gmtToday.getDay() + daysDelta, date.getDay());
        assertEquals(zoneOffset, offsetDate.getOffset());
    }

    @Test
    public void subtract_days() {
        final int daysDelta = random(1, 31);
        final OffsetDate offsetDate = OffsetDates.dateAt(gmtToday, zoneOffset);
        final OffsetDate offsetDateMinusMonths = subtractDays(offsetDate, daysDelta);

        final LocalDate date = offsetDateMinusMonths.getDate();
        assertEquals(gmtToday.getDay() - daysDelta, date.getDay());
        assertEquals(zoneOffset, offsetDate.getOffset());
    }

    //
    // Arguments check
    //-------------------

    @Test
    public void pass_null_tolerance_check() {
        final ZoneOffset zoneOffset = ZoneOffsets.getDefault();
        new NullPointerTester()
                .setDefault(ZoneOffset.class, zoneOffset)
                .setDefault(LocalDate.class, LocalDates.now())
                .setDefault(OffsetDate.class, OffsetDates.now(ZoneOffsets.UTC))
                .testAllPublicStaticMethods(OffsetDates.class);
    }

    //
    // Illegal args. check for math with years.
    //------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_years_to_add() {
        addYears(now, -5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_years_to_subtract() {
        subtractYears(now, -6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_years_to_add() {
        addYears(now, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_years_to_subtract() {
        subtractYears(now, 0);
    }

    //
    // Illegal args. check for math with months.
    //------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_months_to_add() {
        addMonths(now, -7);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_months_to_subtract() {
        subtractMonths(now, -8);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_months_to_add() {
        addMonths(now, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_months_to_subtract() {
        subtractMonths(now, 0);
    }

    //
    // Illegal args. check for math with days.
    //------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_days_to_subtract() {
        subtractDays(now, -27);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_days_to_add() {
        addDays(now, -25);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_days_to_subtract() {
        subtractDays(now, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_zero_days_to_add() {
        addDays(now, 0);
    }

    //
    // Stringification
    //----------------------

    @Ignore
    @Test
    public void convert_to_string_and_back_at_UTC() throws ParseException {
        final OffsetDate todayAtUTC = OffsetDates.now(ZoneOffsets.UTC);

        final String str = OffsetDates.toString(todayAtUTC);
        assertEquals(todayAtUTC, OffsetDates.parse(str));
    }
}
