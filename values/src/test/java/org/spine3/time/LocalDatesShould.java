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

@SuppressWarnings("InstanceMethodNamingConvention")
public class LocalDatesShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(LocalDates.class));
    }

    @Test
    public void obtain_current_date() {
        final LocalDate today = LocalDates.now();
        final Calendar calendar = Calendar.getInstance();

        assertEquals(calendar.get(Calendar.YEAR), today.getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, today.getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), today.getDay());
    }

    @Test
    public void obtain_date_from_year_month_day_values() {
        final int year = 1976;
        final int month = 3;
        final int day = 1;
        final LocalDate birthday = LocalDates.of(1976, MonthOfYear.APRIL, 1);
        final Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        assertEquals(calendar.get(Calendar.YEAR), birthday.getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, birthday.getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), birthday.getDay());
    }

    @Test
    public void obtain_date_in_past_after_specified_number_of_years() {
        final int year = 2007;
        final int month = 1;
        final int day = 28;
        final LocalDate today = LocalDates.of(2008, MonthOfYear.FEBRUARY, 29);
        final LocalDate yearAgo = LocalDates.minusYears(today, 1);
        final Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        assertEquals(calendar.get(Calendar.YEAR), yearAgo.getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, yearAgo.getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), yearAgo.getDay());
    }

    @Test
    public void obtain_date_in_past_after_specified_number_of_months() {
        final int year = 2007;
        final int month = 11;
        final int day = 29;
        final LocalDate today = LocalDates.of(2008, MonthOfYear.FEBRUARY, 29);
        final LocalDate twoMonthAgo = LocalDates.minusMonths(today, 2);
        final Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        assertEquals(calendar.get(Calendar.YEAR), twoMonthAgo.getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, twoMonthAgo.getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), twoMonthAgo.getDay());
    }

    @Test
    public void obtain_date_in_past_after_specified_number_of_days() {
        final int year = 2007;
        final int month = 11;
        final int day = 31;
        final LocalDate today = LocalDates.of(2008, MonthOfYear.FEBRUARY, 29);
        final LocalDate sixtyDaysAgo = LocalDates.minusDays(today, 60);
        final Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        assertEquals(calendar.get(Calendar.YEAR), sixtyDaysAgo.getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, sixtyDaysAgo.getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), sixtyDaysAgo.getDay());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_years() {
        final LocalDate today = LocalDates.now();
        final LocalDate inFiveYears = LocalDates.plusYears(today, 5);
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 5);

        assertEquals(calendar.get(Calendar.YEAR), inFiveYears.getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, inFiveYears.getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), inFiveYears.getDay());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_months() {
        final LocalDate today = LocalDates.now();
        final LocalDate inTwoMonths = LocalDates.plusMonths(today, 2);
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 2);

        assertEquals(calendar.get(Calendar.YEAR), inTwoMonths.getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, inTwoMonths.getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), inTwoMonths.getDay());
    }

    @Test
    public void obtain_date_in_future_after_specified_number_of_days() {
        final LocalDate today = LocalDates.now();
        final LocalDate inFiveDays = LocalDates.plusDays(today, 5);
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 5);

        assertEquals(calendar.get(Calendar.YEAR), inFiveDays.getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, inFiveDays.getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), inFiveDays.getDay());
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
