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

import java.util.Calendar;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;
import static org.spine3.time.Calendars.nowAt;
import static org.spine3.time.Calendars.toCalendar;
import static org.spine3.time.Calendars.toLocalDate;
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
        checkNotNull(zoneOffset);

        final Calendar cal = nowAt(zoneOffset);
        final LocalDate localDate = toLocalDate(cal);
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
        checkNotNull(localDate);
        checkNotNull(zoneOffset);

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
        checkNotNull(offsetDate);
        checkPositive(yearsToAdd);

        return changeYear(offsetDate, yearsToAdd);
    }

    /**
     * Obtains a copy of this offset date with the specified number of months added.
     */
    public static OffsetDate plusMonths(OffsetDate offsetDate, int monthsToAdd) {
        checkNotNull(offsetDate);
        checkPositive(monthsToAdd);

        return changeMonth(offsetDate, monthsToAdd);
    }

    /**
     * Obtains a copy of this offset date with the specified number of days added.
     */
    public static OffsetDate plusDays(OffsetDate offsetDate, int daysToAdd) {
        checkNotNull(offsetDate);
        checkPositive(daysToAdd);

        return changeDays(offsetDate, daysToAdd);
    }

    /**
     * Obtains a copy of this offset date with the specified number of years subtracted.
     */
    public static OffsetDate minusYears(OffsetDate offsetDate, int yearsToSubtract) {
        checkNotNull(offsetDate);
        checkPositive(yearsToSubtract);

        return changeYear(offsetDate, -yearsToSubtract);
    }

    /**
     * Obtains a copy of this offset date with the specified number of months subtracted.
     */
    public static OffsetDate minusMonths(OffsetDate offsetDate, int monthsToSubtract) {
        checkNotNull(offsetDate);
        checkPositive(monthsToSubtract);

        return changeMonth(offsetDate, -monthsToSubtract);
    }

    /**
     * Obtains a copy of this offset date with the specified number of days subtracted.
     */
    public static OffsetDate minusDays(OffsetDate offsetDate, int daysToSubtract) {
        checkNotNull(offsetDate);
        checkPositive(daysToSubtract);

        return changeDays(offsetDate, -daysToSubtract);
    }

    /**
     * Obtains offset date changed on specified amount of years.
     *
     * @param offsetDate offset date that will be changed
     * @param yearsDelta a number of years that needs to be added or subtracted that can be
     *                   either positive or negative
     * @return copy of this offset date with new years value
     */
    private static OffsetDate changeYear(OffsetDate offsetDate, int yearsDelta) {
        final OffsetDate result = add(offsetDate, YEAR, yearsDelta);
        return result;
    }

    /**
     * Obtains offset date changed on specified amount of months.
     *
     * @param offsetDate offset date that will be changed
     * @param monthDelta a number of months that needs to be added or subtracted that can be
     *                   either positive or negative
     * @return copy of this offset date with new months value
     */
    private static OffsetDate changeMonth(OffsetDate offsetDate, int monthDelta) {
        final OffsetDate result = add(offsetDate, MONTH, monthDelta);
        return result;
    }

    /**
     * Obtains offset date changed on specified amount of days.
     *
     * @param offsetDate offset date that will be changed
     * @param daysDelta  a number of days that needs to be added or subtracted that can be
     *                   either positive or negative
     * @return copy of this offset date with new days value
     */
    private static OffsetDate changeDays(OffsetDate offsetDate, int daysDelta) {
        final OffsetDate result = add(offsetDate, DAY_OF_MONTH, daysDelta);
        return result;
    }

    /**
     * Returns a new instance of offset date with changed local time parameter.
     *
     * @param offsetDate offset date that will be changed
     * @param localDate  new local date for this offset date
     * @return new {@code OffsetDate} instance with changed parameter
     */
    private static OffsetDate withDate(OffsetDate offsetDate, LocalDate localDate) {
        return offsetDate.toBuilder()
                         .setDate(localDate)
                         .build();
    }

    /**
     * Performs date calculation using parameters of {@link Calendar#add(int, int)}.
     */
    private static OffsetDate add(OffsetDate offsetDate, int calendarField, int delta) {
        final Calendar cal = toCalendar(offsetDate);
        cal.add(calendarField, delta);
        final LocalDate localDate = toLocalDate(cal);
        final OffsetDate result = withDate(offsetDate, localDate);
        return result;
    }
}
