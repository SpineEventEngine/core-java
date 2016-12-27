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
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static org.spine3.time.Calendars.createDate;
import static org.spine3.time.Calendars.getDay;
import static org.spine3.time.Calendars.getHours;
import static org.spine3.time.Calendars.getMillis;
import static org.spine3.time.Calendars.getMinutes;
import static org.spine3.time.Calendars.getSeconds;
import static org.spine3.time.Calendars.getYear;
import static org.spine3.time.Calendars.nowAt;
import static org.spine3.time.Calendars.toLocalDate;
import static org.spine3.time.Calendars.toLocalTime;
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
        checkNotNull(zoneOffset);

        final Calendar now = nowAt(zoneOffset);
        final LocalTime localTime = toLocalTime(now);
        final LocalDate localDate = toLocalDate(now);

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
        checkNotNull(offsetDateTime);
        checkPositive(yearsToAdd);

        return changeYear(offsetDateTime, yearsToAdd);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of months added.
     */
    public static OffsetDateTime plusMonths(OffsetDateTime offsetDateTime, int monthsToAdd) {
        checkNotNull(offsetDateTime);
        checkPositive(monthsToAdd);

        return changeMonth(offsetDateTime, monthsToAdd);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of days added.
     */
    public static OffsetDateTime plusDays(OffsetDateTime offsetDateTime, int daysToAdd) {
        checkNotNull(offsetDateTime);
        checkPositive(daysToAdd);

        return changeDays(offsetDateTime, daysToAdd);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of hours added.
     */
    public static OffsetDateTime plusHours(OffsetDateTime offsetDateTime, int hoursToAdd) {
        checkNotNull(offsetDateTime);
        checkPositive(hoursToAdd);

        return changeHours(offsetDateTime, hoursToAdd);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of minutes added.
     */
    public static OffsetDateTime plusMinutes(OffsetDateTime offsetDateTime, int minutesToAdd) {
        checkNotNull(offsetDateTime);
        checkPositive(minutesToAdd);

        return changeMinutes(offsetDateTime, minutesToAdd);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of seconds added.
     */
    public static OffsetDateTime plusSeconds(OffsetDateTime offsetDateTime, int secondsToAdd) {
        checkNotNull(offsetDateTime);
        checkPositive(secondsToAdd);

        return changeSeconds(offsetDateTime, secondsToAdd);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of milliseconds added.
     */
    public static OffsetDateTime plusMillis(OffsetDateTime offsetDateTime, int millisToAdd) {
        checkNotNull(offsetDateTime);
        checkPositive(millisToAdd);

        return changeMillis(offsetDateTime, millisToAdd);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of years subtracted.
     */
    public static OffsetDateTime minusYears(OffsetDateTime offsetDateTime, int yearsToSubtract) {
        checkNotNull(offsetDateTime);
        checkPositive(yearsToSubtract);

        return changeYear(offsetDateTime, -yearsToSubtract);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of months subtracted.
     */
    public static OffsetDateTime minusMonths(OffsetDateTime offsetDateTime, int monthsToSubtract) {
        checkNotNull(offsetDateTime);
        checkPositive(monthsToSubtract);

        return changeMonth(offsetDateTime, -monthsToSubtract);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of days subtracted.
     */
    public static OffsetDateTime minusDays(OffsetDateTime offsetDateTime, int daysToSubtract) {
        checkNotNull(offsetDateTime);
        checkPositive(daysToSubtract);

        return changeDays(offsetDateTime, -daysToSubtract);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of hours subtracted.
     */
    public static OffsetDateTime minusHours(OffsetDateTime offsetDateTime, int hoursToSubtract) {
        checkNotNull(offsetDateTime);
        checkPositive(hoursToSubtract);

        return changeHours(offsetDateTime, -hoursToSubtract);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of minutes subtracted.
     */
    public static OffsetDateTime minusMinutes(OffsetDateTime offsetDateTime, int minutesToSubtract) {
        checkNotNull(offsetDateTime);
        checkPositive(minutesToSubtract);

        return changeMinutes(offsetDateTime, -minutesToSubtract);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of seconds subtracted.
     */
    public static OffsetDateTime minusSeconds(OffsetDateTime offsetDateTime, int secondsToSubtract) {
        checkNotNull(offsetDateTime);
        checkPositive(secondsToSubtract);

        return changeSeconds(offsetDateTime, -secondsToSubtract);
    }

    /**
     * Obtains a copy of this offset date and time with the specified number of milliseconds subtracted.
     */
    public static OffsetDateTime minusMillis(OffsetDateTime offsetDateTime, int millisToSubtract) {
        checkNotNull(offsetDateTime);
        checkPositive(millisToSubtract);

        return changeMillis(offsetDateTime, -millisToSubtract);
    }

    /**
     * Obtains offset date and time changed on specified amount of years.
     *
     * @param offsetDateTime offset date and time that will be changed
     * @param yearsDelta a number of years that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset date and time with new years value
     */
    private static OffsetDateTime changeYear(OffsetDateTime offsetDateTime, int yearsDelta) {
        final LocalDate date = offsetDateTime.getDate();
        final Calendar calDate = createDate(date.getYear(),
                                            date.getMonthValue(),
                                            date.getDay());
        final Calendar calTime = Calendars.createWithTime(offsetDateTime.getTime().getHours(),
                                                          offsetDateTime.getTime().getMinutes(),
                                                          offsetDateTime.getTime().getSeconds(),
                                                          offsetDateTime.getTime().getMillis());
        calDate.add(Calendar.YEAR, yearsDelta);

        final LocalDate localDate = LocalDates.of(getYear(calDate),
                                                  Calendars.getMonthOfYear(calDate),
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
        final Calendar calTime = Calendars.createWithTime(offsetDateTime.getTime().getHours(),
                                                          offsetDateTime.getTime().getMinutes(),
                                                          offsetDateTime.getTime().getSeconds(),
                                                          offsetDateTime.getTime().getMillis());
        calDate.add(MONTH, monthDelta);

        final LocalDate localDate = LocalDates.of(getYear(calDate),
                                                  Calendars.getMonthOfYear(calDate),
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
        final Calendar calTime = Calendars.createWithTime(offsetDateTime.getTime().getHours(),
                                                          offsetDateTime.getTime().getMinutes(),
                                                          offsetDateTime.getTime().getSeconds(),
                                                          offsetDateTime.getTime().getMillis());
        calDate.add(DAY_OF_MONTH, daysDelta);

        final LocalDate localDate = LocalDates.of(getYear(calDate),
                                                  Calendars.getMonthOfYear(calDate),
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
     * Obtains offset date and time changed on specified amount of hours.
     *
     * @param offsetDateTime  offset date and time that will be changed
     * @param hoursDelta a number of hours that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset date and time with new hours value
     */
    private static OffsetDateTime changeHours(OffsetDateTime offsetDateTime, int hoursDelta) {
        final Calendar calDate = createDate(offsetDateTime.getDate().getYear(),
                                            offsetDateTime.getDate().getMonthValue(),
                                            offsetDateTime.getDate().getDay());
        final Calendar calTime = Calendars.createWithTime(offsetDateTime.getTime().getHours(),
                                                          offsetDateTime.getTime().getMinutes(),
                                                          offsetDateTime.getTime().getSeconds(),
                                                          offsetDateTime.getTime().getMillis());
        calTime.add(HOUR, hoursDelta);

        final LocalDate localDate = LocalDates.of(getYear(calDate),
                                                  Calendars.getMonthOfYear(calDate),
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
     * Obtains offset date and time changed on specified amount of minutes.
     *
     * @param offsetDateTime    offset date and time that will be changed
     * @param minutesDelta a number of minutes that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset date and time with new minutes value
     */
    private static OffsetDateTime changeMinutes(OffsetDateTime offsetDateTime, int minutesDelta) {
        final Calendar calDate = createDate(offsetDateTime.getDate().getYear(),
                                            offsetDateTime.getDate().getMonthValue(),
                                            offsetDateTime.getDate().getDay());
        final Calendar calTime = Calendars.createWithTime(offsetDateTime.getTime().getHours(),
                                                          offsetDateTime.getTime().getMinutes(),
                                                          offsetDateTime.getTime().getSeconds(),
                                                          offsetDateTime.getTime().getMillis());
        calTime.add(MINUTE, minutesDelta);

        final LocalDate localDate = LocalDates.of(getYear(calDate),
                                                  Calendars.getMonthOfYear(calDate),
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
     * Obtains offset date and time changed on specified amount of seconds.
     *
     * @param offsetDateTime    offset date and time that will be changed
     * @param secondsDelta a number of seconds that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset date and time with new seconds value
     */
    private static OffsetDateTime changeSeconds(OffsetDateTime offsetDateTime, int secondsDelta) {
        final Calendar calDate = createDate(offsetDateTime.getDate().getYear(),
                                            offsetDateTime.getDate().getMonthValue(),
                                            offsetDateTime.getDate().getDay());
        final Calendar calTime = Calendars.createWithTime(offsetDateTime.getTime().getHours(),
                                                          offsetDateTime.getTime().getMinutes(),
                                                          offsetDateTime.getTime().getSeconds(),
                                                          offsetDateTime.getTime().getMillis());
        calTime.add(SECOND, secondsDelta);

        final LocalDate localDate = LocalDates.of(getYear(calDate),
                                                  Calendars.getMonthOfYear(calDate),
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
     * Obtains offset date and time changed on specified amount of milliseconds.
     *
     * @param offsetDateTime   offset date and time that will be changed
     * @param millisDelta a number of milliseconds that needs to be added or subtracted that can be either positive or negative
     * @return copy of this offset date and time with new milliseconds value
     */
    private static OffsetDateTime changeMillis(OffsetDateTime offsetDateTime, int millisDelta) {
        final Calendar calDate = createDate(offsetDateTime.getDate().getYear(),
                                            offsetDateTime.getDate().getMonthValue(),
                                            offsetDateTime.getDate().getDay());
        final Calendar calTime = Calendars.createWithTime(offsetDateTime.getTime().getHours(),
                                                          offsetDateTime.getTime().getMinutes(),
                                                          offsetDateTime.getTime().getSeconds(),
                                                          offsetDateTime.getTime().getMillis());
        calTime.add(MILLISECOND, millisDelta);

        final LocalDate localDate = LocalDates.of(getYear(calDate),
                                                  Calendars.getMonthOfYear(calDate),
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
