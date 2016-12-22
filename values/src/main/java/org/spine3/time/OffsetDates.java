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

import com.google.protobuf.Timestamp;
import org.spine3.protobuf.Timestamps;
import org.spine3.time.change.Changes.ArgumentName;

import java.util.Calendar;

import static org.spine3.validate.Validate.checkPositive;

/**
 * Routines for working with {@link OffsetDate}.
 *
 * @author Alexander Aleksandrov
 */
public class OffsetDates {

    private OffsetDates() {
    }

    /**
     * Obtains offset date in specified {@code ZoneOffset}.
     */
    public static OffsetDate now(ZoneOffset zoneOffset) {
        final Timestamp now = Timestamps.getCurrentTime();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now.getSeconds() / 1000);
        final LocalDate localDate = LocalDates.of(calendar.get(Calendar.YEAR),
                                                  MonthOfYears.getMonth(calendar),
                                                  calendar.get(Calendar.DAY_OF_MONTH));

        final OffsetDate result = OffsetDate.newBuilder()
                                            .setDate(localDate)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

    /**
     * Obtains offset date using {@code LocalDate} and {@code ZoneOffset}.
     */
    public static OffsetDate of(LocalDate localDate, ZoneOffset zoneOffset) {
        final OffsetDate result = OffsetDate.newBuilder()
                                            .setDate(localDate)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

    /**
     * Obtains a copy of this offset date with the specified number of years added.
     */
    public static OffsetDate plusYears(OffsetDate offsetDate, int yearsToAdd) {
        checkPositive(yearsToAdd, ArgumentName.YEARS_TO_ADD);
        return changeYear(offsetDate, yearsToAdd);
    }

    /**
     * Obtains a copy of this offset date with the specified number of months added.
     */
    public static OffsetDate plusMonths(OffsetDate offsetDate, int monthsToAdd) {
        checkPositive(monthsToAdd, ArgumentName.MONTHS_TO_ADD);
        return changeMonth(offsetDate, monthsToAdd);
    }

    /**
     * Obtains a copy of this offset date with the specified number of days added.
     */
    public static OffsetDate plusDays(OffsetDate offsetDate, int daysToAdd) {
        checkPositive(daysToAdd, ArgumentName.DAYS_TO_ADD);
        return changeDays(offsetDate, daysToAdd);
    }

    /**
     * Obtains a copy of this offset date with the specified number of years subtracted.
     */
    public static OffsetDate minusYears(OffsetDate offsetDate, int yearsToSubtract) {
        checkPositive(yearsToSubtract, ArgumentName.YEARS_TO_SUBTRACT);
        return changeYear(offsetDate, -yearsToSubtract);
    }

    /**
     * Obtains a copy of this offset date with the specified number of months subtracted.
     */
    public static OffsetDate minusMonths(OffsetDate offsetDate, int monthsToSubtract) {
        checkPositive(monthsToSubtract, ArgumentName.MONTHS_TO_SUBTRACT);
        return changeMonth(offsetDate, -monthsToSubtract);
    }

    /**
     * Obtains a copy of this offset date with the specified number of days subtracted.
     */
    public static OffsetDate minusDays(OffsetDate offsetDate, int daysToSubtract) {
        checkPositive(daysToSubtract, ArgumentName.DAYS_TO_SUBTRACT);
        return changeDays(offsetDate, -daysToSubtract);
    }

    /**
     * Obtains offset date changed on specified amount of years.
     *
     * @param offsetDate offset date that will be changed
     * @param yearsDelta a number of years that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset date with new years value
     */
    private static OffsetDate changeYear(OffsetDate offsetDate, int yearsDelta) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, offsetDate.getDate().getYear());
        // The Calendar class assumes JANUARY is zero. Therefore subtract 1 to set the value of Calendar.MONTH.
        calendar.set(Calendar.MONTH, offsetDate.getDate().getMonthValue() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, offsetDate.getDate().getDay());
        calendar.add(Calendar.YEAR, yearsDelta);
        final int year = calendar.get(Calendar.YEAR);
        // The Calendar class assumes JANUARY is zero. Therefore add 1 to get the value of MonthOfYear.
        final MonthOfYear month = MonthOfYears.getMonth(calendar);
        final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        final LocalDate localDate = LocalDates.of(year, month, dayOfMonth);
        final ZoneOffset zoneOffset = offsetDate.getOffset();

        final OffsetDate result = OffsetDate.newBuilder()
                                          .setDate(localDate)
                                          .setOffset(zoneOffset)
                                          .build();
        return result;
    }

    /**
     * Obtains offset date changed on specified amount of months.
     *
     * @param offsetDate offset date that will be changed
     * @param monthDelta a number of months that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset date with new months value
     */
    private static OffsetDate changeMonth(OffsetDate offsetDate, int monthDelta) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, offsetDate.getDate().getYear());
        // The Calendar class assumes JANUARY is zero. Therefore subtract 1 to set the value of Calendar.MONTH.
        calendar.set(Calendar.MONTH, offsetDate.getDate().getMonthValue() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, offsetDate.getDate().getDay());
        calendar.add(Calendar.MONTH, monthDelta);
        final int year = calendar.get(Calendar.YEAR);
        // The Calendar class assumes JANUARY is zero. Therefore add 1 to get the value of MonthOfYear.
        final MonthOfYear month = MonthOfYears.getMonth(calendar);
        final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        final LocalDate localDate = LocalDates.of(year, month, dayOfMonth);
        final ZoneOffset zoneOffset = offsetDate.getOffset();

        final OffsetDate result = OffsetDate.newBuilder()
                                            .setDate(localDate)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

    /**
     * Obtains offset date changed on specified amount of days.
     *
     * @param offsetDate offset date that will be changed
     * @param daysDelta a number of days that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset date with new days value
     */
    private static OffsetDate changeDays(OffsetDate offsetDate, int daysDelta) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, offsetDate.getDate().getYear());
        // The Calendar class assumes JANUARY is zero. Therefore subtract 1 to set the value of Calendar.MONTH.
        calendar.set(Calendar.MONTH, offsetDate.getDate().getMonthValue() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, offsetDate.getDate().getDay());
        calendar.add(Calendar.DAY_OF_MONTH, daysDelta);
        final int year = calendar.get(Calendar.YEAR);
        // The Calendar class assumes JANUARY is zero. Therefore add 1 to get the value of MonthOfYear.
        final MonthOfYear month = MonthOfYears.getMonth(calendar);
        final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        final LocalDate localDate = LocalDates.of(year, month, dayOfMonth);
        final ZoneOffset zoneOffset = offsetDate.getOffset();

        final OffsetDate result = OffsetDate.newBuilder()
                                            .setDate(localDate)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }
}
