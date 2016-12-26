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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.change.Changes.*;
import static org.spine3.time.Calendars.createDate;
import static org.spine3.time.Calendars.createDateWithZoneOffset;
import static org.spine3.time.Calendars.createTime;
import static org.spine3.time.Calendars.createTimeWithZoneOffset;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getHours;
import static org.spine3.time.Calendars.getMillis;
import static org.spine3.time.Calendars.getMinutes;
import static org.spine3.time.Calendars.getSeconds;
import static org.spine3.time.Calendars.getYear;
import static org.spine3.time.change.Changes.*;
import static org.spine3.validate.Validate.checkPositive;

/**
 * Routines for working with {@link OffsetDateTime}.
 *
 * @author Alexander Aleksandrov
 */
public class OffsetDateTimes {

    private OffsetDateTimes() {
    }

    /**
     * Obtains current OffsetDateTime instance using {@code ZoneOffset}.
     */
    public static OffsetDateTime now(ZoneOffset zoneOffset) {
        checkNotNull(zoneOffset, ErrorMessage.ZONE_OFFSET);
        final Calendar time = createTimeWithZoneOffset(zoneOffset);
        final LocalTime localTime = LocalTimes.of(getHours(time), getMinutes(time), getSeconds(time));
        final Calendar date = createDateWithZoneOffset(zoneOffset);
        final LocalDate localDate = LocalDates.of(getYear(date), MonthOfYears.getMonth(date), getDay(date));

        final OffsetDateTime result = OffsetDateTime.newBuilder()
                                                    .setDate(localDate)
                                                    .setTime(localTime)
                                                    .setOffset(zoneOffset)
                                                    .build();
        return result;
    }

    /**
     * Obtains OffsetDateTime instance using {@code LocalDate}, {@code LocalTime} and {@code ZoneOffset}.
     */
    public static OffsetDateTime of(LocalDate localDate, LocalTime localTime, ZoneOffset zoneOffset) {
        final OffsetDateTime result = OffsetDateTime.newBuilder()
                                                    .setDate(localDate)
                                                    .setTime(localTime)
                                                    .setOffset(zoneOffset)
                                                    .build();
        return result;
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of years added.
     */
    public static OffsetDateTime plusYears(OffsetDateTime offsetDateTime, int yearsToAdd) {
        checkNotNull(offsetDateTime, ErrorMessage.OFFSET_DATE_TIME);
        checkPositive(yearsToAdd, ArgumentName.YEARS_TO_ADD);
        return changeYear(offsetDateTime, yearsToAdd);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of months added.
     */
    public static OffsetDateTime plusMonths(OffsetDateTime offsetDateTime, int monthsToAdd) {
        checkNotNull(offsetDateTime, ErrorMessage.OFFSET_DATE_TIME);
        checkPositive(monthsToAdd, ArgumentName.MONTHS_TO_ADD);
        return changeMonth(offsetDateTime, monthsToAdd);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of days added.
     */
    public static OffsetDateTime plusDays(OffsetDateTime offsetDateTime, int daysToAdd) {
        checkNotNull(offsetDateTime, ErrorMessage.OFFSET_DATE_TIME);
        checkPositive(daysToAdd, ArgumentName.DAYS_TO_ADD);
        return changeDays(offsetDateTime, daysToAdd);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of years subtracted.
     */
    public static OffsetDateTime minusYears(OffsetDateTime offsetDateTime, int yearsToSubtract) {
        checkNotNull(offsetDateTime, ErrorMessage.OFFSET_DATE_TIME);
        checkPositive(yearsToSubtract, ArgumentName.YEARS_TO_SUBTRACT);
        return changeYear(offsetDateTime, -yearsToSubtract);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of months subtracted.
     */
    public static OffsetDateTime minusMonths(OffsetDateTime offsetDateTime, int monthsToSubtract) {
        checkNotNull(offsetDateTime, ErrorMessage.OFFSET_DATE_TIME);
        checkPositive(monthsToSubtract, ArgumentName.MONTHS_TO_SUBTRACT);
        return changeMonth(offsetDateTime, -monthsToSubtract);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of days subtracted.
     */
    public static OffsetDateTime minusDays(OffsetDateTime offsetDateTime, int daysToSubtract) {
        checkNotNull(offsetDateTime, ErrorMessage.OFFSET_DATE_TIME);
        checkPositive(daysToSubtract, ArgumentName.DAYS_TO_SUBTRACT);
        return changeDays(offsetDateTime, -daysToSubtract);
    }

    /**
     * Obtains offset date and time changed on specified amount of years.
     *
     * @param offsetDateTime offset date and time that will be changed
     * @param yearsDelta a number of years that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset date and time with new years value
     */
    private static OffsetDateTime changeYear(OffsetDateTime offsetDateTime, int yearsDelta) {
        final Calendar calDate = createDate(offsetDateTime.getDate().getYear(),
                                            offsetDateTime.getDate().getMonthValue(),
                                            offsetDateTime.getDate().getDay());
        final Calendar calTime = createTime(offsetDateTime.getTime().getHours(), 
                                            offsetDateTime.getTime().getMinutes(),
                                            offsetDateTime.getTime().getSeconds(), 
                                            offsetDateTime.getTime().getMillis());
        calDate.add(Calendar.YEAR, yearsDelta);

        final LocalDate localDate = LocalDates.of(getYear(calDate),
                                                  MonthOfYears.getMonth(calDate),
                                                  getDay(calDate));
        final LocalTime localTime = LocalTimes.of(getHours(calTime), getMinutes(calTime),
                                                  getSeconds(calTime), getMillis(calTime),
                                                  offsetDateTime.getTime().getNanos());

        final OffsetDateTime result = OffsetDateTime.newBuilder()
                                                    .setDate(localDate)
                                                    .setTime(localTime)
                                                    .setOffset(offsetDateTime.getOffset())
                                                    .build();
        return result;
    }

    /**
     * Obtains offset date and time changed on specified amount of months.
     *
     * @param offsetDateTime offset date that will be changed
     * @param monthDelta a number of months that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset date and time with new months value
     */
    private static OffsetDateTime changeMonth(OffsetDateTime offsetDateTime, int monthDelta) {
        final Calendar calDate = createDate(offsetDateTime.getDate().getYear(),
                                            offsetDateTime.getDate().getMonthValue(),
                                            offsetDateTime.getDate().getDay());
        final Calendar calTime = createTime(offsetDateTime.getTime().getHours(),
                                            offsetDateTime.getTime().getMinutes(),
                                            offsetDateTime.getTime().getSeconds(),
                                            offsetDateTime.getTime().getMillis());
        calDate.add(Calendar.MONTH, monthDelta);

        final LocalDate localDate = LocalDates.of(getYear(calDate),
                                                  MonthOfYears.getMonth(calDate),
                                                  getDay(calDate));
        final LocalTime localTime = LocalTimes.of(getHours(calTime), getMinutes(calTime),
                                                  getSeconds(calTime), getMillis(calTime),
                                                  offsetDateTime.getTime().getNanos());

        final OffsetDateTime result = OffsetDateTime.newBuilder()
                                                    .setDate(localDate)
                                                    .setTime(localTime)
                                                    .setOffset(offsetDateTime.getOffset())
                                                    .build();
        return result;
    }

    /**
     * Obtains offset date and time changed on specified amount of days.
     *
     * @param offsetDateTime offset date that will be changed
     * @param daysDelta a number of days that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset date and time with new days value
     */
    private static OffsetDateTime changeDays(OffsetDateTime offsetDateTime, int daysDelta) {
        final Calendar calDate = createDate(offsetDateTime.getDate().getYear(),
                                            offsetDateTime.getDate().getMonthValue(),
                                            offsetDateTime.getDate().getDay());
        final Calendar calTime = createTime(offsetDateTime.getTime().getHours(),
                                            offsetDateTime.getTime().getMinutes(),
                                            offsetDateTime.getTime().getSeconds(),
                                            offsetDateTime.getTime().getMillis());
        calDate.add(Calendar.DAY_OF_MONTH, daysDelta);

        final LocalDate localDate = LocalDates.of(getYear(calDate),
                                                  MonthOfYears.getMonth(calDate),
                                                  getDay(calDate));
        final LocalTime localTime = LocalTimes.of(getHours(calTime), getMinutes(calTime),
                                                  getSeconds(calTime), getMillis(calTime),
                                                  offsetDateTime.getTime().getNanos());

        final OffsetDateTime result = OffsetDateTime.newBuilder()
                                                    .setDate(localDate)
                                                    .setTime(localTime)
                                                    .setOffset(offsetDateTime.getOffset())
                                                    .build();
        return result;
    }
}
