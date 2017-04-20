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

import static org.junit.Assert.assertEquals;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.time.Calendars.at;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getMonth;
import static org.spine3.time.Calendars.getYear;
import static org.spine3.time.Calendars.getZoneOffset;
import static org.spine3.time.OffsetDates.addDays;
import static org.spine3.time.OffsetDates.addMonths;
import static org.spine3.time.OffsetDates.addYears;
import static org.spine3.time.OffsetDates.subtractDays;
import static org.spine3.time.OffsetDates.subtractMonths;
import static org.spine3.time.OffsetDates.subtractYears;

public class OffsetDatesShould {

    private static final int year = 2012;
    private static final MonthOfYear month = MonthOfYear.JULY;
    private static final int day = 16;
    private static final LocalDate localDate = LocalDates.of(year, month, day);

    private ZoneOffset zoneOffset;
    private OffsetDate now;

    @Before
    public void setUp() {
        zoneOffset = ZoneOffsets.ofHoursMinutes(5, 30);
        now = OffsetDates.now(zoneOffset);
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

        final ZoneOffset offset = today.getOffset();
        assertEquals(getZoneOffset(cal), offset.getAmountSeconds());
    }

    @Test
    public void obtain_current_OffsetDate_using_LocalDate_and_ZoneOffset() {
        final OffsetDate offsetDate = OffsetDates.of(localDate, zoneOffset);

        final LocalDate date = offsetDate.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day, date.getDay());

        final ZoneOffset offset = offsetDate.getOffset();
        assertEquals(zoneOffset.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_years() {
        final int yearsToSubstract = 2;
        final OffsetDate offsetDate = OffsetDates.of(localDate, zoneOffset);
        final OffsetDate offsetDateMinusYears = subtractYears(offsetDate, yearsToSubstract);

        final LocalDate date = offsetDateMinusYears.getDate();
        assertEquals(year - yearsToSubstract, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day, date.getDay());

        final ZoneOffset offset = offsetDateMinusYears.getOffset();
        assertEquals(zoneOffset.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_months() {
        final int monthsToSubstract = 2;
        final OffsetDate offsetDate = OffsetDates.of(localDate, zoneOffset);
        final OffsetDate offsetDateMinusMonths = subtractMonths(offsetDate, monthsToSubstract);

        final LocalDate date = offsetDateMinusMonths.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month.getNumber() - monthsToSubstract, date.getMonth()
                                                                .getNumber());
        assertEquals(day, date.getDay());

        final ZoneOffset offset = offsetDateMinusMonths.getOffset();
        assertEquals(zoneOffset.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_days() {
        final int daysToSubstract = 5;
        final OffsetDate offsetDate = OffsetDates.of(localDate, zoneOffset);
        final OffsetDate offsetDateMinusMonths = subtractDays(offsetDate, daysToSubstract);

        final LocalDate date = offsetDateMinusMonths.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day - daysToSubstract, date.getDay());

        final ZoneOffset offset = offsetDateMinusMonths.getOffset();
        assertEquals(zoneOffset.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_years() {
        final int yearsToAdd = 2;
        final OffsetDate offsetDate = OffsetDates.of(localDate, zoneOffset);
        final OffsetDate offsetDatePlusYears = addYears(offsetDate, yearsToAdd);

        final LocalDate date = offsetDatePlusYears.getDate();
        assertEquals(year + yearsToAdd, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day, date.getDay());

        final ZoneOffset offset = offsetDatePlusYears.getOffset();
        assertEquals(zoneOffset.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_months() {
        final int monthsToAdd = 2;
        final OffsetDate offsetDate = OffsetDates.of(localDate, zoneOffset);
        final OffsetDate offsetDatePlusMonths = addMonths(offsetDate, monthsToAdd);

        final LocalDate date = offsetDatePlusMonths.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month.getNumber() + monthsToAdd, date.getMonth()
                                                          .getNumber());
        assertEquals(day, date.getDay());

        final ZoneOffset offset = offsetDatePlusMonths.getOffset();
        assertEquals(zoneOffset.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_days() {
        final int daysToAdd = 15;
        final OffsetDate offsetDate = OffsetDates.of(localDate, zoneOffset);
        final OffsetDate offsetDatePlusMonths = addDays(offsetDate, daysToAdd);

        final LocalDate date = offsetDatePlusMonths.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day + daysToAdd, date.getDay());

        final ZoneOffset offset = offsetDatePlusMonths.getOffset();
        assertEquals(zoneOffset.getAmountSeconds(), offset.getAmountSeconds());
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

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_years_to_add() {
        addYears(now, -5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_months_to_add() {
        addMonths(now, -7);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_days_to_add() {
        addDays(now, -25);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_years_to_subtract() {
        subtractYears(now, -6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_months_to_subtract() {
        subtractMonths(now, -8);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_days_to_subtract() {
        subtractDays(now, -27);
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
