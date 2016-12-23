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
import static org.spine3.change.Changes.ErrorMessage;
import static org.spine3.time.Calendars.createDate;
import static org.spine3.time.Calendars.createDateWithNoOffset;
import static org.spine3.time.Calendars.createDateWithZoneOffset;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getYear;
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
        checkNotNull(zoneOffset, ErrorMessage.ZONE_OFFSET);
        final Calendar cal = createDateWithZoneOffset(zoneOffset);
        final LocalDate localDate = LocalDates.of(getYear(cal),
                                                  MonthOfYears.getMonth(cal),
                                                  getDay(cal));
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
        checkNotNull(localDate, ErrorMessage.LOCAL_DATE);
        checkNotNull(zoneOffset, ErrorMessage.ZONE_OFFSET);
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
        checkNotNull(offsetDate, ErrorMessage.OFFSET_DATE);
        checkPositive(yearsToAdd, ArgumentName.YEARS_TO_ADD);
        return changeYear(offsetDate, yearsToAdd);
    }

    /**
     * Obtains a copy of this offset date with the specified number of months added.
     */
    public static OffsetDate plusMonths(OffsetDate offsetDate, int monthsToAdd) {
        checkNotNull(offsetDate, ErrorMessage.OFFSET_DATE);
        checkPositive(monthsToAdd, ArgumentName.MONTHS_TO_ADD);
        return changeMonth(offsetDate, monthsToAdd);
    }

    /**
     * Obtains a copy of this offset date with the specified number of days added.
     */
    public static OffsetDate plusDays(OffsetDate offsetDate, int daysToAdd) {
        checkNotNull(offsetDate, ErrorMessage.OFFSET_DATE);
        checkPositive(daysToAdd, ArgumentName.DAYS_TO_ADD);
        return changeDays(offsetDate, daysToAdd);
    }

    /**
     * Obtains a copy of this offset date with the specified number of years subtracted.
     */
    public static OffsetDate minusYears(OffsetDate offsetDate, int yearsToSubtract) {
        checkNotNull(offsetDate, ErrorMessage.OFFSET_DATE);
        checkPositive(yearsToSubtract, ArgumentName.YEARS_TO_SUBTRACT);
        return changeYear(offsetDate, -yearsToSubtract);
    }

    /**
     * Obtains a copy of this offset date with the specified number of months subtracted.
     */
    public static OffsetDate minusMonths(OffsetDate offsetDate, int monthsToSubtract) {
        checkNotNull(offsetDate, ErrorMessage.OFFSET_DATE);
        checkPositive(monthsToSubtract, ArgumentName.MONTHS_TO_SUBTRACT);
        return changeMonth(offsetDate, -monthsToSubtract);
    }

    /**
     * Obtains a copy of this offset date with the specified number of days subtracted.
     */
    public static OffsetDate minusDays(OffsetDate offsetDate, int daysToSubtract) {
        checkNotNull(offsetDate, ErrorMessage.OFFSET_DATE);
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
        final Calendar cal = createDate(offsetDate.getDate().getYear(),
                                        offsetDate.getDate().getMonthValue(),
                                        offsetDate.getDate().getDay());
        cal.add(Calendar.YEAR, yearsDelta);

        final LocalDate localDate = LocalDates.of(getYear(cal),
                                                  MonthOfYears.getMonth(cal),
                                                  getDay(cal));

        final OffsetDate result = OffsetDate.newBuilder()
                                          .setDate(localDate)
                                          .setOffset(offsetDate.getOffset())
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
        final Calendar cal = createDate(offsetDate.getDate().getYear(),
                                        offsetDate.getDate().getMonthValue(),
                                        offsetDate.getDate().getDay());
        cal.add(Calendar.MONTH, monthDelta);

        final LocalDate localDate = LocalDates.of(getYear(cal),
                                                  MonthOfYears.getMonth(cal),
                                                  getDay(cal));

        final OffsetDate result = OffsetDate.newBuilder()
                                            .setDate(localDate)
                                            .setOffset(offsetDate.getOffset())
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
        final Calendar cal = createDate(offsetDate.getDate().getYear(),
                                        offsetDate.getDate().getMonthValue(),
                                        offsetDate.getDate().getDay());
        cal.add(Calendar.DAY_OF_MONTH, daysDelta);

        final LocalDate localDate = LocalDates.of(getYear(cal),
                                                  MonthOfYears.getMonth(cal),
                                                  getDay(cal));

        final OffsetDate result = OffsetDate.newBuilder()
                                            .setDate(localDate)
                                            .setOffset(offsetDate.getOffset())
                                            .build();
        return result;
    }
}
