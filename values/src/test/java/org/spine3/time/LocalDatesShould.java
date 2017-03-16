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

import static java.util.Calendar.getInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getMonth;
import static org.spine3.time.Calendars.getYear;

public class LocalDatesShould {

    private static final int year = 2014;
    private static final MonthOfYear month = MonthOfYear.JULY;
    private static final int day = 20;

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(LocalDates.class);
    }

    @Test
    public void obtain_current_date() {
        final LocalDate today = LocalDates.now();
        final Calendar cal = getInstance();

        assertEquals(getYear(cal), today.getYear());
        assertEquals(getMonth(cal), today.getMonthValue());
        assertEquals(getDay(cal), today.getDay());
    }

    @Test
    public void obtain_date_from_year_month_day_values() {
        final LocalDate localDate = LocalDates.of(year, month, day);

        assertTrue(year == localDate.getYear());
        assertTrue(month == localDate.getMonth());
        assertTrue(day == localDate.getDay());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_years() {
        final int yearsToSubstract = 2;
        final LocalDate localDate = LocalDates.of(year, month, day);
        final LocalDate fewYearsAgo = LocalDates.minusYears(localDate, yearsToSubstract);

        assertTrue(year - yearsToSubstract == fewYearsAgo.getYear());
        assertTrue(month == fewYearsAgo.getMonth());
        assertTrue(day == fewYearsAgo.getDay());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_months() {
        final int monthsToSubstract = 2;
        final LocalDate localDate = LocalDates.of(year, month, day);
        final LocalDate fewMonthsAgo = LocalDates.minusMonths(localDate, monthsToSubstract);

        assertTrue(year == fewMonthsAgo.getYear());
        assertTrue(month.getNumber() - monthsToSubstract ==
                   fewMonthsAgo.getMonth().getNumber());
        assertTrue(day == fewMonthsAgo.getDay());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_days() {
        final int daysToSubstract = 15;
        final LocalDate localDate = LocalDates.of(year, month, day);
        final LocalDate fewDaysAgo = LocalDates.minusDays(localDate, daysToSubstract);

        assertTrue(year == fewDaysAgo.getYear());
        assertTrue(month == fewDaysAgo.getMonth());
        assertTrue(day - daysToSubstract == fewDaysAgo.getDay());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_years() {
        final int yearsToAdd = 2;
        final LocalDate localDate = LocalDates.of(year, month, day);
        final LocalDate plusYears = LocalDates.plusYears(localDate, yearsToAdd);

        assertTrue(year + yearsToAdd == plusYears.getYear());
        assertTrue(month == plusYears.getMonth());
        assertTrue(day == plusYears.getDay());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_months() {
        final int monthsToAdd = 2;
        final LocalDate localDate = LocalDates.of(year, month, day);
        final LocalDate plusMonths = LocalDates.plusMonths(localDate, monthsToAdd);

        assertTrue(year == plusMonths.getYear());
        assertTrue(month.getNumber() + monthsToAdd == plusMonths.getMonth().getNumber());
        assertTrue(day == plusMonths.getDay());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_days() {
        final int daysToAdd = 8;
        final LocalDate localDate = LocalDates.of(year, month, day);
        final LocalDate plusDays = LocalDates.plusDays(localDate, daysToAdd);

        assertTrue(year == plusDays.getYear());
        assertTrue(month == plusDays.getMonth());
        assertTrue(day + daysToAdd == plusDays.getDay());
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalDate_value_with_yearsToAdd() {
        final int yearsToAdd = -5;
        final LocalDate now = null;
        LocalDates.plusYears(now, yearsToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalDate_value_with_monthsToAdd() {
        final int monthsToAdd = 7;
        final LocalDate now = null;
        LocalDates.plusMonths(now, monthsToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalDate_value_with_daysToAdd() {
        final int daysToAdd = 25;
        final LocalDate now = null;
        LocalDates.plusDays(now, daysToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalDate_value_with_yearsToSubtract() {
        final int yearsToSubtract = 6;
        final LocalDate now = null;
        LocalDates.minusYears(now, yearsToSubtract);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalDate_value_with_monthsToSubtract() {
        final int monthsToSubtract = 8;
        final LocalDate now = null;
        LocalDates.minusMonths(now, monthsToSubtract);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalDate_value_with_daysToSubtract() {
        final int daysToSubtract = 27;
        final LocalDate now = null;
        LocalDates.minusDays(now, daysToSubtract);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_years() {
        final int year = -1987;
        final int day = 20;
        LocalDates.of(year, MonthOfYear.AUGUST, day);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_days() {
        final int year = 1987;
        final int day = -20;
        LocalDates.of(year, MonthOfYear.AUGUST, day);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_yearsToAdd() {
        final int yearsToAdd = -5;
        final LocalDate now = LocalDates.now();
        LocalDates.plusYears(now, yearsToAdd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_monthsToAdd() {
        final int monthsToAdd = -7;
        final LocalDate now = LocalDates.now();
        LocalDates.plusMonths(now, monthsToAdd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_daysToAdd() {
        final int daysToAdd = -25;
        final LocalDate now = LocalDates.now();
        LocalDates.plusDays(now, daysToAdd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_yearsToSubtract() {
        final int yearsToSubtract = -6;
        final LocalDate now = LocalDates.now();
        LocalDates.minusYears(now, yearsToSubtract);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_monthsToSubtract() {
        final int monthsToSubtract = -8;
        final LocalDate now = LocalDates.now();
        LocalDates.minusMonths(now, monthsToSubtract);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_daysToSubtract() {
        final int daysToSubtract = -27;
        final LocalDate now = LocalDates.now();
        LocalDates.minusDays(now, daysToSubtract);
    }
}
