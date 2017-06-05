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
import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;

import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.time.Calendars.getDay;
import static io.spine.time.Calendars.getMonth;
import static io.spine.time.Calendars.getYear;
import static java.util.Calendar.getInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LocalDatesShould {

    private static final int YEAR = 2014;
    private static final MonthOfYear MONTH = MonthOfYear.JULY;
    private static final int DAY = 20;

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
        final LocalDate localDate = LocalDates.of(YEAR, MONTH, DAY);

        assertTrue(YEAR == localDate.getYear());
        assertTrue(MONTH == localDate.getMonth());
        assertTrue(DAY == localDate.getDay());
    }

    /*
     * Math with years
     */
    @Test
    public void subtract_years() {
        final int yearsToSubstract = 2;
        final LocalDate localDate = LocalDates.of(YEAR, MONTH, DAY);
        final LocalDate fewYearsAgo = LocalDates.subtractYears(localDate, yearsToSubstract);

        assertTrue(YEAR - yearsToSubstract == fewYearsAgo.getYear());
        assertTrue(MONTH == fewYearsAgo.getMonth());
        assertTrue(DAY == fewYearsAgo.getDay());
    }

    @Test
    public void add_years() {
        final int yearsToAdd = 2;
        final LocalDate localDate = LocalDates.of(YEAR, MONTH, DAY);
        final LocalDate plusYears = LocalDates.addYears(localDate, yearsToAdd);

        assertTrue(YEAR + yearsToAdd == plusYears.getYear());
        assertTrue(MONTH == plusYears.getMonth());
        assertTrue(DAY == plusYears.getDay());
    }

    /*
     * Math with months
     */
    @Test
    public void subtract_months() {
        final int monthsToSubstract = 2;
        final LocalDate localDate = LocalDates.of(YEAR, MONTH, DAY);
        final LocalDate fewMonthsAgo = LocalDates.subtractMonths(localDate, monthsToSubstract);

        assertTrue(YEAR == fewMonthsAgo.getYear());
        assertTrue(MONTH.getNumber() - monthsToSubstract ==
                   fewMonthsAgo.getMonth().getNumber());
        assertTrue(DAY == fewMonthsAgo.getDay());
    }

    @Test
    public void add_months() {
        final int monthsToAdd = 2;
        final LocalDate localDate = LocalDates.of(YEAR, MONTH, DAY);
        final LocalDate plusMonths = LocalDates.addMonths(localDate, monthsToAdd);

        assertTrue(YEAR == plusMonths.getYear());
        assertTrue(MONTH.getNumber() + monthsToAdd == plusMonths.getMonth().getNumber());
        assertTrue(DAY == plusMonths.getDay());
    }

    /*
     * Math with days
     */
    @Test
    public void subtract_days() {
        final int daysToSubstract = 15;
        final LocalDate localDate = LocalDates.of(YEAR, MONTH, DAY);
        final LocalDate fewDaysAgo = LocalDates.subtractDays(localDate, daysToSubstract);

        assertTrue(YEAR == fewDaysAgo.getYear());
        assertTrue(MONTH == fewDaysAgo.getMonth());
        assertTrue(DAY - daysToSubstract == fewDaysAgo.getDay());
    }

    @Test
    public void add_days() {
        final int daysToAdd = 8;
        final LocalDate localDate = LocalDates.of(YEAR, MONTH, DAY);
        final LocalDate plusDays = LocalDates.addDays(localDate, daysToAdd);

        assertTrue(YEAR == plusDays.getYear());
        assertTrue(MONTH == plusDays.getMonth());
        assertTrue(DAY + daysToAdd == plusDays.getDay());
    }

    @Test
    public void pass_null_tolerance_test() {
        new NullPointerTester()
                .setDefault(LocalDate.class, LocalDates.now())
                .setDefault(int.class, 1)
                .testAllPublicStaticMethods(LocalDates.class);
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
        LocalDates.addYears(now, yearsToAdd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_monthsToAdd() {
        final int monthsToAdd = -7;
        final LocalDate now = LocalDates.now();
        LocalDates.addMonths(now, monthsToAdd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_daysToAdd() {
        final int daysToAdd = -25;
        final LocalDate now = LocalDates.now();
        LocalDates.addDays(now, daysToAdd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_yearsToSubtract() {
        final int yearsToSubtract = -6;
        final LocalDate now = LocalDates.now();
        LocalDates.subtractYears(now, yearsToSubtract);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_monthsToSubtract() {
        final int monthsToSubtract = -8;
        final LocalDate now = LocalDates.now();
        LocalDates.subtractMonths(now, monthsToSubtract);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_daysToSubtract() {
        final int daysToSubtract = -27;
        final LocalDate now = LocalDates.now();
        LocalDates.subtractDays(now, daysToSubtract);
    }

    @Test
    public void convert_to_string() throws ParseException {
        final LocalDate today = LocalDates.now();

        final String str = LocalDates.toString(today);

        assertEquals(today, LocalDates.parse(str));
    }
}
