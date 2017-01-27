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

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateParameterlessCtor;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getMonth;
import static org.spine3.time.Calendars.getYear;
import static org.spine3.time.Calendars.getZoneOffset;
import static org.spine3.time.Calendars.nowAt;

@SuppressWarnings("InstanceMethodNamingConvention")
public class OffsetDatesShould {

    private static final ZoneOffset ZONE_OFFSET = ZoneOffsets.ofHoursMinutes(5, 30);
    private static final int year = 2012;
    private static final MonthOfYear month = MonthOfYear.JULY;
    private static final int day = 16;
    private static final LocalDate localDate = LocalDates.of(year, month, day);

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateParameterlessCtor(OffsetDates.class));
    }

    @Test
    public void obtain_current_OffsetDate_using_ZoneOffset() {
        final OffsetDate today = OffsetDates.now(ZONE_OFFSET);
        final Calendar cal = nowAt(ZONE_OFFSET);

        final LocalDate date = today.getDate();
        assertEquals(getYear(cal), date.getYear());
        assertEquals(getMonth(cal), date.getMonthValue());
        assertEquals(getDay(cal), date.getDay());

        final ZoneOffset offset = today.getOffset();
        assertEquals(getZoneOffset(cal), offset.getAmountSeconds());
    }

    @Test
    public void obtain_current_OffsetDate_using_LocalDate_and_ZoneOffset() {
        final OffsetDate offsetDate = OffsetDates.of(localDate, ZONE_OFFSET);

        final LocalDate date = offsetDate.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day, date.getDay());

        final ZoneOffset offset = offsetDate.getOffset();
        assertEquals(ZONE_OFFSET.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_years() {
        final int yearsToSubstract = 2;
        final OffsetDate offsetDate = OffsetDates.of(localDate, ZONE_OFFSET);
        final OffsetDate offsetDateMinusYears = OffsetDates.minusYears(offsetDate, yearsToSubstract);

        final LocalDate date = offsetDateMinusYears.getDate();
        assertEquals(year - yearsToSubstract, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day, date.getDay());

        final ZoneOffset offset = offsetDateMinusYears.getOffset();
        assertEquals(ZONE_OFFSET.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_months() {
        final int monthsToSubstract = 2;
        final OffsetDate offsetDate = OffsetDates.of(localDate, ZONE_OFFSET);
        final OffsetDate offsetDateMinusMonths = OffsetDates.minusMonths(offsetDate, monthsToSubstract);

        final LocalDate date = offsetDateMinusMonths.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month.getNumber() - monthsToSubstract, date.getMonth()
                                                                .getNumber());
        assertEquals(day, date.getDay());

        final ZoneOffset offset = offsetDateMinusMonths.getOffset();
        assertEquals(ZONE_OFFSET.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_days() {
        final int daysToSubstract = 5;
        final OffsetDate offsetDate = OffsetDates.of(localDate, ZONE_OFFSET);
        final OffsetDate offsetDateMinusMonths = OffsetDates.minusDays(offsetDate, daysToSubstract);

        final LocalDate date = offsetDateMinusMonths.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day - daysToSubstract, date.getDay());

        final ZoneOffset offset = offsetDateMinusMonths.getOffset();
        assertEquals(ZONE_OFFSET.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_years() {
        final int yearsToAdd = 2;
        final OffsetDate offsetDate = OffsetDates.of(localDate, ZONE_OFFSET);
        final OffsetDate offsetDatePlusYears = OffsetDates.plusYears(offsetDate, yearsToAdd);

        final LocalDate date = offsetDatePlusYears.getDate();
        assertEquals(year + yearsToAdd, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day, date.getDay());

        final ZoneOffset offset = offsetDatePlusYears.getOffset();
        assertEquals(ZONE_OFFSET.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_months() {
        final int monthsToAdd = 2;
        final OffsetDate offsetDate = OffsetDates.of(localDate, ZONE_OFFSET);
        final OffsetDate offsetDatePlusMonths = OffsetDates.plusMonths(offsetDate, monthsToAdd);

        final LocalDate date = offsetDatePlusMonths.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month.getNumber() + monthsToAdd, date.getMonth()
                                                          .getNumber());
        assertEquals(day, date.getDay());

        final ZoneOffset offset = offsetDatePlusMonths.getOffset();
        assertEquals(ZONE_OFFSET.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_days() {
        final int daysToAdd = 15;
        final OffsetDate offsetDate = OffsetDates.of(localDate, ZONE_OFFSET);
        final OffsetDate offsetDatePlusMonths = OffsetDates.plusDays(offsetDate, daysToAdd);

        final LocalDate date = offsetDatePlusMonths.getDate();
        assertEquals(year, date.getYear());
        assertEquals(month, date.getMonth());
        assertEquals(day + daysToAdd, date.getDay());

        final ZoneOffset offset = offsetDatePlusMonths.getOffset();
        assertEquals(ZONE_OFFSET.getAmountSeconds(), offset.getAmountSeconds());
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_ZoneOffset_value() {
        final ZoneOffset zoneOffset = null;
        OffsetDates.now(zoneOffset);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalDate_value() {
        final LocalDate localDate = null;
        OffsetDates.of(localDate, ZONE_OFFSET);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_ZoneOffset_value_in_of_method() {
        final LocalDate localDate = LocalDates.now();
        final ZoneOffset zoneOffset = null;
        OffsetDates.of(localDate, zoneOffset);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetDate_value_with_yearsToAdd() {
        final int yearsToAdd = -5;
        final OffsetDate now = null;
        OffsetDates.plusYears(now, yearsToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetDate_value_with_monthsToAdd() {
        final int monthsToAdd = 7;
        final OffsetDate now = null;
        OffsetDates.plusMonths(now, monthsToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetDate_value_with_daysToAdd() {
        final int daysToAdd = 25;
        final OffsetDate now = null;
        OffsetDates.plusDays(now, daysToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetDate_value_with_yearsToSubtract() {
        final int yearsToSubtract = 6;
        final OffsetDate now = null;
        OffsetDates.minusYears(now, yearsToSubtract);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetDate_value_with_monthsToSubtract() {
        final int monthsToSubtract = 8;
        final OffsetDate now = null;
        OffsetDates.minusMonths(now, monthsToSubtract);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetDate_value_with_daysToSubtract() {
        final int daysToSubtract = 27;
        final OffsetDate now = null;
        OffsetDates.minusDays(now, daysToSubtract);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_yearsToAdd() {
        final int yearsToAdd = -5;
        final OffsetDate now = OffsetDates.now(ZONE_OFFSET);
        OffsetDates.plusYears(now, yearsToAdd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_monthsToAdd() {
        final int monthsToAdd = -7;
        final OffsetDate now = OffsetDates.now(ZONE_OFFSET);
        OffsetDates.plusMonths(now, monthsToAdd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_daysToAdd() {
        final int daysToAdd = -25;
        final OffsetDate now = OffsetDates.now(ZONE_OFFSET);
        OffsetDates.plusDays(now, daysToAdd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_yearsToSubtract() {
        final int yearsToSubtract = -6;
        final OffsetDate now = OffsetDates.now(ZONE_OFFSET);
        OffsetDates.minusYears(now, yearsToSubtract);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_monthsToSubtract() {
        final int monthsToSubtract = -8;
        final OffsetDate now = OffsetDates.now(ZONE_OFFSET);
        OffsetDates.minusMonths(now, monthsToSubtract);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_daysToSubtract() {
        final int daysToSubtract = -27;
        final OffsetDate now = OffsetDates.now(ZONE_OFFSET);
        OffsetDates.minusDays(now, daysToSubtract);
    }
}
