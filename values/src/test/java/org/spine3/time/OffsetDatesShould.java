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
import static org.spine3.time.Calendars.createDateWithZoneOffset;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getMonth;
import static org.spine3.time.Calendars.getYear;
import static org.spine3.time.Calendars.getZoneOffset;

@SuppressWarnings("InstanceMethodNamingConvention")
public class OffsetDatesShould {

    private static final ZoneOffset ZONE_OFFSET = ZoneOffsets.ofHoursMinutes(5, 30);

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(OffsetDates.class));
    }

    @Test
    public void obtain_current_OffsetDate_using_ZoneOffset() {
        final OffsetDate today = OffsetDates.now(ZONE_OFFSET);
        final Calendar cal = createDateWithZoneOffset(ZONE_OFFSET);

        assertEquals(getYear(cal), today.getDate().getYear());
        assertEquals(getMonth(cal), today.getDate().getMonthValue());
        assertEquals(getDay(cal), today.getDate().getDay());
        assertEquals(getZoneOffset(cal), today.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_current_OffsetDate_using_LocalDate_and_ZoneOffset() {
        final int year = 2006;
        final MonthOfYear month = MonthOfYear.APRIL;
        final int day = 25;
        final LocalDate localDate = LocalDates.of(year, month, day);
        final OffsetDate offsetDate = OffsetDates.of(localDate, ZONE_OFFSET);

        assertTrue(year == offsetDate.getDate().getYear());
        assertTrue(month == offsetDate.getDate().getMonth());
        assertTrue(day == offsetDate.getDate().getDay());
        assertTrue(ZONE_OFFSET.getAmountSeconds() == offsetDate.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_years() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        final OffsetDate today = OffsetDates.now(ZONE_OFFSET);
        final OffsetDate offsetDate = OffsetDates.minusYears(today, 1);

        assertEquals(getYear(cal), offsetDate.getDate().getYear());
        assertEquals(getMonth(cal), offsetDate.getDate().getMonthValue());
        assertEquals(getDay(cal), offsetDate.getDate().getDay());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_months() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -2);
        final OffsetDate today = OffsetDates.now(ZONE_OFFSET);
        final OffsetDate offsetDate = OffsetDates.minusMonths(today, 2);

        assertEquals(getYear(cal), offsetDate.getDate().getYear());
        assertEquals(getMonth(cal), offsetDate.getDate().getMonthValue());
        assertEquals(getDay(cal), offsetDate.getDate().getDay());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_days() {
        final int daysToSubtract = -60;
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, daysToSubtract);
        final OffsetDate today = OffsetDates.now(ZONE_OFFSET);
        final OffsetDate offsetDate = OffsetDates.minusDays(today, 60);

        assertEquals(getYear(cal), offsetDate.getDate().getYear());
        assertEquals(getMonth(cal), offsetDate.getDate().getMonthValue());
        assertEquals(getDay(cal), offsetDate.getDate().getDay());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_years() {
        final OffsetDate today = OffsetDates.now(ZONE_OFFSET);
        final OffsetDate offsetDate = OffsetDates.plusYears(today, 5);
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 5);

        assertEquals(getYear(cal), offsetDate.getDate().getYear());
        assertEquals(getMonth(cal), offsetDate.getDate().getMonthValue());
        assertEquals(getDay(cal), offsetDate.getDate().getDay());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_months() {
        final OffsetDate today = OffsetDates.now(ZONE_OFFSET);
        final OffsetDate offsetDate = OffsetDates.plusMonths(today, 2);
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 2);

        assertEquals(getYear(cal), offsetDate.getDate().getYear());
        assertEquals(getMonth(cal), offsetDate.getDate().getMonthValue());
        assertEquals(getDay(cal), offsetDate.getDate().getDay());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_days() {
        final OffsetDate today = OffsetDates.now(ZONE_OFFSET);
        final OffsetDate offsetDate = OffsetDates.plusDays(today, 5);
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 5);

        assertEquals(getYear(cal), offsetDate.getDate().getYear());
        assertEquals(getMonth(cal), offsetDate.getDate().getMonthValue());
        assertEquals(getDay(cal), offsetDate.getDate().getDay());
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
