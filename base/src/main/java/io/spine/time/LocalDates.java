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

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.time.Calendars.checkArguments;
import static io.spine.time.Calendars.toCalendar;
import static io.spine.time.Calendars.toLocalDate;
import static io.spine.validate.Validate.checkPositive;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;
import static java.util.Calendar.getInstance;

/**
 * Utilities for working with {@link LocalDate}.
 *
 * @author Alexander Yevsyukov
 * @author Alexander Aleksandrov
 */
public final class LocalDates {

    private LocalDates() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains current local date.
     */
    public static LocalDate now() {
        final Calendar cal = getInstance();
        final LocalDate result = toLocalDate(cal);
        return result;
    }

    /**
     * Obtains local date from a year, month, and day.
     */
    public static LocalDate of(int year, MonthOfYear month, int day) {
        checkPositive(year);
        checkPositive(day);
        Time.checkDate(year, month, day);

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
    public static LocalDate addYears(LocalDate localDate, int yearsToAdd) {
        checkArguments(localDate, yearsToAdd);
        return changeYear(localDate, yearsToAdd);
    }

    /**
     * Obtains a copy of this local date with the specified number of months added.
     */
    public static LocalDate addMonths(LocalDate localDate, int monthsToAdd) {
        checkArguments(localDate, monthsToAdd);
        return changeMonth(localDate, monthsToAdd);
    }

    /**
     * Obtains a copy of this local date with the specified number of days added.
     */
    public static LocalDate addDays(LocalDate localDate, int daysToAdd) {
        checkArguments(localDate, daysToAdd);
        return changeDays(localDate, daysToAdd);
    }

    /**
     * Obtains a copy of this local date with the specified number of years subtracted.
     */
    public static LocalDate subtractYears(LocalDate localDate, int yearsToSubtract) {
        checkArguments(localDate, yearsToSubtract);
        return changeYear(localDate, -yearsToSubtract);
    }

    /**
     * Obtains a copy of this local date with the specified number of months subtracted.
     */
    public static LocalDate subtractMonths(LocalDate localDate, int monthsToSubtract) {
        checkArguments(localDate, monthsToSubtract);
        return changeMonth(localDate, -monthsToSubtract);
    }

    /**
     * Obtains a copy of this local date with the specified number of days subtracted.
     */
    public static LocalDate subtractDays(LocalDate localDate, int daysToSubtract) {
        checkArguments(localDate, daysToSubtract);
        return changeDays(localDate, -daysToSubtract);
    }

    /**
     * Obtains local date changed on specified amount of years.
     *
     * @param localDate  local date that will be changed
     * @param yearsDelta a number of years that needs to be added or subtracted that can be
     *                   either positive or negative
     * @return copy of this local date with new years value
     */
    private static LocalDate changeYear(LocalDate localDate, int yearsDelta) {
        return change(localDate, YEAR, yearsDelta);
    }

    /**
     * Obtains local date changed on specified amount of months.
     *
     * @param localDate  local date that will be changed
     * @param monthDelta a number of months that needs to be added or subtracted that can be
     *                   either positive or negative
     * @return copy of this local date with new months value
     */
    private static LocalDate changeMonth(LocalDate localDate, int monthDelta) {
        return change(localDate, MONTH, monthDelta);
    }

    /**
     * Obtains local date changed on specified amount of days.
     *
     * @param localDate local date that will be changed
     * @param daysDelta a number of days that needs to be added or subtracted that can be
     *                  either positive or negative
     * @return copy of this local date with new days value
     */
    private static LocalDate changeDays(LocalDate localDate, int daysDelta) {
        return change(localDate, DAY_OF_MONTH, daysDelta);
    }

    /**
     * Performs date calculation using parameters of {@link Calendar#add(int, int)}.
     */
    private static LocalDate change(LocalDate localDate, int calendarField, int delta) {
        final Calendar cal = toCalendar(localDate);
        cal.add(calendarField, delta);
        return toLocalDate(cal);
    }

    /**
     * Parse from ISO 8601 date representation of the format {@code yyyy-MM-dd}.
     *
     * @return a LocalDate parsed from the string
     * @throws ParseException if parsing fails.
     */
    public static LocalDate parse(String str) throws ParseException {
        final Date date = Formats.dateFormat().parse(str);

        final Calendar calendar = getInstance();
        calendar.setTime(date);

        final LocalDate result = toLocalDate(calendar);
        return result;
    }

    /**
     * Converts a local date into ISO 8601 string with the format {@code yyyy-MM-dd}.
     */
    public static String toString(LocalDate date) {
        checkNotNull(date);
        final Date classicDate = toCalendar(date).getTime();
        final String result = Formats.dateFormat().format(classicDate);
        return result;
    }

    public static void checkDate(LocalDate date){
        Time.checkDate(date.getYear(), date.getMonth(), date.getDay());
    }

}
