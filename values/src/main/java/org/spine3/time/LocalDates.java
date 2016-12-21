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

import java.util.Calendar;
import java.util.Date;

/**
 * Routines for working with {@link LocalDate}.
 *
 * @author Alexander Yevsyukov
 * @author Alexander Aleksandrov
 */
public class LocalDates {

    private LocalDates() {
    }

    /**
     * Obtains current local date.
     */
    public static LocalDate now() {
        final Calendar calendar = Calendar.getInstance();
        final int year = calendar.get(Calendar.YEAR);
        // The Calendar class assumes JANUARY is zero. Therefore add 1 to get the value of MonthOfYear.
        final MonthOfYear month = MonthOfYear.forNumber(calendar.get(Calendar.MONTH) + 1);
        final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        final LocalDate result = LocalDate.newBuilder()
                                          .setYear(year)
                                          .setMonth(month)
                                          .setDay(dayOfMonth)
                                          .build();
        return result;
    }

    /**
     * Obtains local date from a year, month and day.
     */
    public static LocalDate of(int year, MonthOfYear month, int day) {
        final LocalDate result = LocalDate.newBuilder()
                                          .setYear(year)
                                          .setMonth(month)
                                          .setDay(day)
                                          .build();
        return result;
    }

    /**
     * Obtains a copy of this local date with the specified number of years added.
     */
    public static LocalDate plusYears(LocalDate localDate, int yearsToAdd) {
        return changeYear(localDate, yearsToAdd);
    }

    /**
     * Obtains a copy of this local date with the specified number of months added.
     */
    public static LocalDate plusMonths(LocalDate localDate, int monthsToAdd) {
        return changeMonth(localDate, monthsToAdd);
    }

    /**
     * Obtains a copy of this local date with the specified number of days added.
     */
    public static LocalDate plusDays(LocalDate localDate, int daysToAdd) {
        return changeDays(localDate, daysToAdd);
    }

    /**
     * Obtains a copy of this local date with the specified number of years subtracted.
     */
    public static LocalDate minusYears(LocalDate localDate, int yearsToSubtract) {
        return changeYear(localDate, -yearsToSubtract);
    }

    /**
     * Obtains a copy of this local date with the specified number of months subtracted.
     */
    public static LocalDate minusMonths(LocalDate localDate, int monthsToSubtract) {
        return changeMonth(localDate, -monthsToSubtract);
    }

    /**
     * Obtains a copy of this local date with the specified number of days subtracted.
     */
    public static LocalDate minusDays(LocalDate localDate, int daysToSubtract) {
        return changeDays(localDate, -daysToSubtract);
    }

    private static LocalDate changeYear(LocalDate localDate, int yearsToChange) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, localDate.getYear());
        // The Calendar class assumes JANUARY is zero. Therefore subtract 1 to set the value of Calendar.MONTH.
        calendar.set(Calendar.MONTH, localDate.getMonthValue() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, localDate.getDay());
        calendar.add(Calendar.YEAR, yearsToChange);
        final int year = calendar.get(Calendar.YEAR);
        // The Calendar class assumes JANUARY is zero. Therefore add 1 to get the value of MonthOfYear.
        final MonthOfYear month = MonthOfYear.forNumber(calendar.get(Calendar.MONTH) + 1);
        final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        final LocalDate result = LocalDate.newBuilder()
                                          .setYear(year)
                                          .setMonth(month)
                                          .setDay(dayOfMonth)
                                          .build();
        return result;
    }

    private static LocalDate changeMonth(LocalDate localDate, int monthToChange) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, localDate.getYear());
        // The Calendar class assumes JANUARY is zero. Therefore subtract 1 to set the value of Calendar.MONTH.
        calendar.set(Calendar.MONTH, localDate.getMonthValue() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, localDate.getDay());
        calendar.add(Calendar.MONTH, monthToChange);
        final int year = calendar.get(Calendar.YEAR);
        // The Calendar class assumes JANUARY is zero. Therefore add 1 to get the value of MonthOfYear.
        final MonthOfYear month = MonthOfYear.forNumber(calendar.get(Calendar.MONTH) + 1);
        final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        final LocalDate result = LocalDate.newBuilder()
                                          .setYear(year)
                                          .setMonth(month)
                                          .setDay(dayOfMonth)
                                          .build();
        return result;
    }

    private static LocalDate changeDays(LocalDate localDate, int daysToChange) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, localDate.getYear());
        // The Calendar class assumes JANUARY is zero. Therefore subtract 1 to set the value of Calendar.MONTH.
        calendar.set(Calendar.MONTH, localDate.getMonthValue() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, localDate.getDay());
        calendar.add(Calendar.DAY_OF_MONTH, daysToChange);
        final int year = calendar.get(Calendar.YEAR);
        // The Calendar class assumes JANUARY is zero. Therefore add 1 to get the value of MonthOfYear.
        final MonthOfYear month = MonthOfYear.forNumber(calendar.get(Calendar.MONTH) + 1);
        final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        final LocalDate result = LocalDate.newBuilder()
                                          .setYear(year)
                                          .setMonth(month)
                                          .setDay(dayOfMonth)
                                          .build();
        return result;
    }
}
