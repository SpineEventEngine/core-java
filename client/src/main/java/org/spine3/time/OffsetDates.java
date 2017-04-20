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
import static org.spine3.time.Calendars.at;
import static org.spine3.time.Calendars.toCalendar;
import static org.spine3.time.Calendars.toLocalDate;
import static org.spine3.validate.Validate.checkPositive;

/**
 * Routines for working with {@link OffsetDate}.
 *
 * @author Alexander Aleksandrov
 * @author Alexander Yevsyukov
 */
public final class OffsetDates {

    private OffsetDates() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains offset date in specified {@code ZoneOffset}.
     */
    public static OffsetDate now(ZoneOffset zoneOffset) {
        checkNotNull(zoneOffset);

        final Calendar cal = at(zoneOffset);
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
    public static OffsetDate of(LocalDate value, ZoneOffset zoneOffset) {
        checkNotNull(value);
        checkNotNull(zoneOffset);

        final OffsetDate result = OffsetDate.newBuilder()
                                            .setDate(value)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

    /**
     * Obtains a copy of this offset date with the specified number of years added.
     *
     * @param value      the value to update
     * @param yearsToAdd a positive number of years to add
     */
    public static OffsetDate addYears(OffsetDate value, int yearsToAdd) {
        checkNotNull(value);
        checkPositive(yearsToAdd);

        return changeYear(value, yearsToAdd);
    }

    /**
     * Obtains a copy of this offset date with the specified number of months added.
     *
     * @param value       the value to update
     * @param monthsToAdd a positive number of months to add
     */
    public static OffsetDate addMonths(OffsetDate value, int monthsToAdd) {
        checkNotNull(value);
        checkPositive(monthsToAdd);

        return changeMonth(value, monthsToAdd);
    }

    /**
     * Obtains a copy of this offset date with the specified number of days added.
     *
     * @param value     the value to update
     * @param daysToAdd a positive number of days to add
     */
    public static OffsetDate addDays(OffsetDate value, int daysToAdd) {
        checkNotNull(value);
        checkPositive(daysToAdd);

        return changeDays(value, daysToAdd);
    }

    /**
     * Obtains a copy of this offset date with the specified number of years subtracted.
     *
     * @param value           the value to update
     * @param yearsToSubtract a positive number of years to subtract
     */
    public static OffsetDate subtractYears(OffsetDate value, int yearsToSubtract) {
        checkNotNull(value);
        checkPositive(yearsToSubtract);

        return changeYear(value, -yearsToSubtract);
    }

    /**
     * Obtains a copy of this offset date with the specified number of months subtracted.
     *
     * @param value            the value to update
     * @param monthsToSubtract a positive number of months to subtract
     */
    public static OffsetDate subtractMonths(OffsetDate value, int monthsToSubtract) {
        checkNotNull(value);
        checkPositive(monthsToSubtract);

        return changeMonth(value, -monthsToSubtract);
    }

    /**
     * Obtains a copy of this offset date with the specified number of days subtracted.
     *
     * @param value          the value to update
     * @param daysToSubtract a positive number of days to subtract
     */
    public static OffsetDate subtractDays(OffsetDate value, int daysToSubtract) {
        checkNotNull(value);
        checkPositive(daysToSubtract);

        return changeDays(value, -daysToSubtract);
    }

    /**
     * Obtains offset date changed on specified amount of years.
     */
    private static OffsetDate changeYear(OffsetDate value, int yearsDelta) {
        final OffsetDate result = add(value, YEAR, yearsDelta);
        return result;
    }

    /**
     * Obtains offset date changed on specified amount of months.
     */
    private static OffsetDate changeMonth(OffsetDate value, int monthDelta) {
        final OffsetDate result = add(value, MONTH, monthDelta);
        return result;
    }

    /**
     * Obtains offset date changed on specified amount of days.
     */
    private static OffsetDate changeDays(OffsetDate value, int daysDelta) {
        final OffsetDate result = add(value, DAY_OF_MONTH, daysDelta);
        return result;
    }

    /**
     * Performs date calculation using parameters of {@link Calendar#add(int, int)}.
     */
    private static OffsetDate add(OffsetDate value, int calendarField, int delta) {
        final Calendar cal = toCalendar(value);
        cal.add(calendarField, delta);
        final LocalDate localDate = toLocalDate(cal);
        final OffsetDate result = value.toBuilder()
                                       .setDate(localDate)
                                       .build();
        return result;
    }
}
