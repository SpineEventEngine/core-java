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

import static org.spine3.time.Calendars.createDate;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getMonth;
import static org.spine3.time.Calendars.getYear;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings("InstanceMethodNamingConvention")
public class LocalDatesShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(LocalDates.class));
    }

    @Test
    public void obtain_current_date() {
        final LocalDate today = LocalDates.now();
        final Calendar cal = Calendar.getInstance();

        assertEquals(getYear(cal), today.getYear());
        assertEquals(getMonth(cal), today.getMonthValue());
        assertEquals(getDay(cal), today.getDay());
    }

    @Test
    public void obtain_date_from_year_month_day_values() {
        final int year = 1976;
        final int month = 4;
        final int day = 1;
        final Calendar cal = createDate(year, month, day);
        final LocalDate localDate = LocalDates.of(year, MonthOfYear.APRIL, day);

        assertEquals(getYear(cal), localDate.getYear());
        assertEquals(getMonth(cal), localDate.getMonthValue());
        assertEquals(getDay(cal), localDate.getDay());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_years() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        final LocalDate today = LocalDates.now();
        final LocalDate yearAgo = LocalDates.minusYears(today, 1);


        assertEquals(getYear(cal), yearAgo.getYear());
        assertEquals(getMonth(cal), yearAgo.getMonthValue());
        assertEquals(getDay(cal), yearAgo.getDay());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_months() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -2);
        final LocalDate today = LocalDates.now();
        final LocalDate twoMonthAgo = LocalDates.minusMonths(today, 2);

        assertEquals(getYear(cal), twoMonthAgo.getYear());
        assertEquals(getMonth(cal), twoMonthAgo.getMonthValue());
        assertEquals(getDay(cal), twoMonthAgo.getDay());
    }

    @Test
    public void obtain_date_in_past_before_specified_number_of_days() {
        final int daysToSubtract = -60;
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, daysToSubtract);
        final LocalDate today = LocalDates.now();
        final LocalDate sixtyDaysAgo = LocalDates.minusDays(today, 60);

        assertEquals(getYear(cal), sixtyDaysAgo.getYear());
        assertEquals(getMonth(cal), sixtyDaysAgo.getMonthValue());
        assertEquals(getDay(cal), sixtyDaysAgo.getDay());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_years() {
        final LocalDate today = LocalDates.now();
        final LocalDate inFiveYears = LocalDates.plusYears(today, 5);
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 5);

        assertEquals(getYear(cal), inFiveYears.getYear());
        assertEquals(getMonth(cal), inFiveYears.getMonthValue());
        assertEquals(getDay(cal), inFiveYears.getDay());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_months() {
        final LocalDate today = LocalDates.now();
        final LocalDate inTwoMonths = LocalDates.plusMonths(today, 2);
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 2);

        assertEquals(getYear(cal), inTwoMonths.getYear());
        assertEquals(getMonth(cal), inTwoMonths.getMonthValue());
        assertEquals(getDay(cal), inTwoMonths.getDay());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_days() {
        final LocalDate today = LocalDates.now();
        final LocalDate inFiveDays = LocalDates.plusDays(today, 5);
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 5);

        assertEquals(getYear(cal), inFiveDays.getYear());
        assertEquals(getMonth(cal), inFiveDays.getMonthValue());
        assertEquals(getDay(cal), inFiveDays.getDay());
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
