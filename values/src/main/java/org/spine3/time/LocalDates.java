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

import org.spine3.time.change.Changes.ArgumentName;

import java.util.Calendar;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Calendar.YEAR;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.DAY_OF_MONTH;
import static org.spine3.change.Changes.ErrorMessage;
import static org.spine3.time.Calendars.createDate;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getYear;
import static org.spine3.validate.Validate.checkPositive;

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
        final Calendar cal = Calendar.getInstance();
        final LocalDate result = LocalDate.newBuilder()
                                          .setYear(getYear(cal))
                                          .setMonth(MonthOfYears.getMonth(cal))
                                          .setDay(getDay(cal))
                                          .build();
        return result;
    }

    /**
     * Obtains local date from a year, month, and day.
     */
    public static LocalDate of(int year, MonthOfYear month, int day) {
        checkPositive(year, "year");
        checkPositive(day, "day");
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
        checkNotNull(localDate, ErrorMessage.LOCAL_DATE);
        checkPositive(yearsToAdd, ArgumentName.YEARS_TO_ADD);
        return changeYear(localDate, yearsToAdd);
    }

    /**
     * Obtains a copy of this local date with the specified number of months added.
     */
    public static LocalDate plusMonths(LocalDate localDate, int monthsToAdd) {
        checkNotNull(localDate, ErrorMessage.LOCAL_DATE);
        checkPositive(monthsToAdd, ArgumentName.MONTHS_TO_ADD);
        return changeMonth(localDate, monthsToAdd);
    }

    /**
     * Obtains a copy of this local date with the specified number of days added.
     */
    public static LocalDate plusDays(LocalDate localDate, int daysToAdd) {
        checkNotNull(localDate, ErrorMessage.LOCAL_DATE);
        checkPositive(daysToAdd, ArgumentName.DAYS_TO_ADD);
        return changeDays(localDate, daysToAdd);
    }

    /**
     * Obtains a copy of this local date with the specified number of years subtracted.
     */
    public static LocalDate minusYears(LocalDate localDate, int yearsToSubtract) {
        checkNotNull(localDate, ErrorMessage.LOCAL_DATE);
        checkPositive(yearsToSubtract, ArgumentName.YEARS_TO_SUBTRACT);
        return changeYear(localDate, -yearsToSubtract);
    }

    /**
     * Obtains a copy of this local date with the specified number of months subtracted.
     */
    public static LocalDate minusMonths(LocalDate localDate, int monthsToSubtract) {
        checkNotNull(localDate, ErrorMessage.LOCAL_DATE);
        checkPositive(monthsToSubtract, ArgumentName.MONTHS_TO_SUBTRACT);
        return changeMonth(localDate, -monthsToSubtract);
    }

    /**
     * Obtains a copy of this local date with the specified number of days subtracted.
     */
    public static LocalDate minusDays(LocalDate localDate, int daysToSubtract) {
        checkNotNull(localDate, ErrorMessage.LOCAL_DATE);
        checkPositive(daysToSubtract, ArgumentName.DAYS_TO_SUBTRACT);
        return changeDays(localDate, -daysToSubtract);
    }

    /**
     * Obtains local date changed on specified amount of years.
     *
     * @param localDate  local date that will be changed
     * @param yearsDelta a number of years that needs to be added or subtracted that can be either positive or negative
     * @return copy of this local date with new years value
     */
    private static LocalDate changeYear(LocalDate localDate, int yearsDelta) {
        final Calendar cal = createDate(localDate.getYear(), localDate.getMonthValue(), localDate.getDay());
        cal.add(YEAR, yearsDelta);

        final LocalDate result = LocalDate.newBuilder()
                                          .setYear(getYear(cal))
                                          .setMonth(MonthOfYears.getMonth(cal))
                                          .setDay(getDay(cal))
                                          .build();
        return result;
    }

    /**
     * Obtains local date changed on specified amount of months.
     *
     * @param localDate  local date that will be changed
     * @param monthDelta a number of months that needs to be added or subtracted that can be either positive or negative
     * @return copy of this local date with new months value
     */
    private static LocalDate changeMonth(LocalDate localDate, int monthDelta) {
        final Calendar cal = createDate(localDate.getYear(), localDate.getMonthValue(), localDate.getDay());
        cal.add(MONTH, monthDelta);

        final LocalDate result = LocalDate.newBuilder()
                                          .setYear(getYear(cal))
                                          .setMonth(MonthOfYears.getMonth(cal))
                                          .setDay(getDay(cal))
                                          .build();
        return result;
    }

    /**
     * Obtains local date changed on specified amount of days.
     *
     * @param localDate local date that will be changed
     * @param daysDelta a number of days that needs to be added or subtracted that can be either positive or negative
     * @return copy of this local date with new days value
     */
    private static LocalDate changeDays(LocalDate localDate, int daysDelta) {
        final Calendar cal = createDate(localDate.getYear(), localDate.getMonthValue(), localDate.getDay());
        cal.add(DAY_OF_MONTH, daysDelta);

        final LocalDate result = LocalDate.newBuilder()
                                          .setYear(getYear(cal))
                                          .setMonth(MonthOfYears.getMonth(cal))
                                          .setDay(getDay(cal))
                                          .build();
        return result;
    }
}
