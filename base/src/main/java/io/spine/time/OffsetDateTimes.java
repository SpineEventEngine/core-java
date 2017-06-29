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
import static io.spine.time.Calendars.at;
import static io.spine.time.Calendars.checkArguments;
import static io.spine.time.Calendars.toCalendar;
import static io.spine.time.Calendars.toLocalDate;
import static io.spine.time.Calendars.toLocalTime;
import static io.spine.time.Formats.appendSubSecond;
import static io.spine.time.Formats.appendZoneOffset;
import static io.spine.time.Formats.dateTimeFormat;
import static io.spine.time.ZoneOffsets.adjustZero;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;

/**
 * Routines for working with {@link OffsetDateTime}.
 *
 * @author Alexander Aleksandrov
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ClassWithTooManyMethods")
public final class OffsetDateTimes {

    private OffsetDateTimes() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains current date/time at the passed time zone.
     */
    public static OffsetDateTime now(ZoneOffset zoneOffset) {
        checkNotNull(zoneOffset);
        final Calendar now = at(zoneOffset);
        final LocalTime localTime = toLocalTime(now);
        final LocalDate localDate = toLocalDate(now);

        return create(localDate, localTime, zoneOffset);
    }

    /**
     * Creates a new {@code OffsetDateTime} instance with the passed values.
     */
    public static OffsetDateTime of(LocalDate date, LocalTime time, ZoneOffset offset) {
        return create(date, time, offset);
    }

    private static OffsetDateTime create(LocalDate date, LocalTime time, ZoneOffset offset) {
        return OffsetDateTime.newBuilder()
                             .setDate(date)
                             .setTime(time)
                             .setOffset(adjustZero(offset))
                             .build();
    }

    /**
     * Obtains a copy of the passed value with added number of years.
     *
     * @param value      the value to update
     * @param yearsToAdd a positive number of years to add
     */
    public static OffsetDateTime addYears(OffsetDateTime value, int yearsToAdd) {
        checkArguments(value, yearsToAdd);
        return changeYear(value, yearsToAdd);
    }

    /**
     * Obtains a copy of the passed value with the specified number of months added.
     *
     * @param value       the value to update
     * @param monthsToAdd a positive number of months to add
     */
    public static OffsetDateTime addMonths(OffsetDateTime value, int monthsToAdd) {
        checkArguments(value, monthsToAdd);
        return changeMonth(value, monthsToAdd);
    }

    /**
     * Obtains a copy of the passed value with the specified number of days added.
     *
     * @param value     the value to update
     * @param daysToAdd a positive number of days to add
     */
    public static OffsetDateTime addDays(OffsetDateTime value, int daysToAdd) {
        checkArguments(value, daysToAdd);
        return changeDays(value, daysToAdd);
    }

    /**
     * Obtains a copy of the passed value with the specified number of hours added.
     *
     * @param value      the value to update
     * @param hoursToAdd a positive number of hours to add
     */
    public static OffsetDateTime addHours(OffsetDateTime value, int hoursToAdd) {
        checkArguments(value, hoursToAdd);
        return changeHours(value, hoursToAdd);
    }

    /**
     * Obtains a copy of the passed value with the specified number of minutes added.
     *
     * @param value        the value to update
     * @param minutesToAdd a positive number of minutes to add
     */
    public static OffsetDateTime addMinutes(OffsetDateTime value, int minutesToAdd) {
        checkArguments(value, minutesToAdd);
        return changeMinutes(value, minutesToAdd);
    }

    /**
     * Obtains a copy of the passed value with the specified number of seconds added.
     *
     * @param value        the value to update
     * @param secondsToAdd a positive number of seconds to add
     */
    public static OffsetDateTime addSeconds(OffsetDateTime value, int secondsToAdd) {
        checkArguments(value, secondsToAdd);
        return changeSeconds(value, secondsToAdd);
    }

    /**
     * Obtains a copy of the passed value with the specified number of milliseconds added.
     *
     * @param value       the value to update
     * @param millisToAdd a positive number of milliseconds to add
     */
    public static OffsetDateTime addMillis(OffsetDateTime value, int millisToAdd) {
        checkArguments(value, millisToAdd);
        return changeMillis(value, millisToAdd);
    }

    /**
     * Obtains a copy of the passed value with the specified number of years subtracted.
     *
     * @param value           the value to update
     * @param yearsToSubtract a positive number of years to subtract
     */
    public static OffsetDateTime subtractYears(OffsetDateTime value, int yearsToSubtract) {
        checkArguments(value, yearsToSubtract);
        return changeYear(value, -yearsToSubtract);
    }

    /**
     * Obtains a copy of the passed value with the specified number of months subtracted.
     *
     * @param value            the value to update
     * @param monthsToSubtract a positive number of months to subtract
     */
    public static OffsetDateTime subtractMonths(OffsetDateTime value, int monthsToSubtract) {
        checkArguments(value, monthsToSubtract);
        return changeMonth(value, -monthsToSubtract);
    }

    /**
     * Obtains a copy of the passed value with the specified number of days subtracted.
     *
     * @param value          the value to update
     * @param daysToSubtract a positive number of days to subtract
     */
    public static OffsetDateTime subtractDays(OffsetDateTime value, int daysToSubtract) {
        checkArguments(value, daysToSubtract);
        return changeDays(value, -daysToSubtract);
    }

    /**
     * Obtains a copy of the passed value with the specified number of hours subtracted.
     *
     * @param value           the value to update
     * @param hoursToSubtract a positive number of hours to subtract
     */
    public static OffsetDateTime subtractHours(OffsetDateTime value, int hoursToSubtract) {
        checkArguments(value, hoursToSubtract);
        return changeHours(value, -hoursToSubtract);
    }

    /**
     * Obtains a copy of the passed value with the specified number of minutes subtracted.
     *
     * @param value             the value to update
     * @param minutesToSubtract a positive number of minutes to subtract
     */
    public static OffsetDateTime subtractMinutes(OffsetDateTime value, int minutesToSubtract) {
        checkArguments(value, minutesToSubtract);
        return changeMinutes(value, -minutesToSubtract);
    }

    /**
     * Obtains a copy of the passed value with the specified number of seconds subtracted.
     *
     * @param value             the value to update
     * @param secondsToSubtract a positive number of seconds to subtract
     */
    public static OffsetDateTime subtractSeconds(OffsetDateTime value, int secondsToSubtract) {
        checkArguments(value, secondsToSubtract);
        return changeSeconds(value, -secondsToSubtract);
    }

    /**
     * Obtains a copy of the passed value with the specified number of milliseconds subtracted.
     *
     * @param value            the value to update
     * @param millisToSubtract a positive number of milliseconds to subtract
     */
    public static OffsetDateTime subtractMillis(OffsetDateTime value, int millisToSubtract) {
        checkArguments(value, millisToSubtract);
        return changeMillis(value, -millisToSubtract);
    }

    /**
     * Obtains offset date and time changed on specified amount of years.
     *
     * @param value      the value to update
     * @param yearsDelta a number of years that needs to be added or subtracted that can be
     *                   either positive or negative
     * @return copy of the passed value with new years value
     */
    private static OffsetDateTime changeYear(OffsetDateTime value, int yearsDelta) {
        return change(value, YEAR, yearsDelta);
    }

    /**
     * Obtains offset date and time changed on specified amount of months.
     *
     * @param value      the value to update
     * @param monthDelta a number of months that needs to be added or subtracted that can be
     *                   either positive or negative
     * @return copy of the passed value with new months value
     */
    private static OffsetDateTime changeMonth(OffsetDateTime value, int monthDelta) {
        return change(value, MONTH, monthDelta);
    }

    /**
     * Obtains offset date and time changed on specified amount of days.
     *
     * @param value     the value to update
     * @param daysDelta a number of days that needs to be added or subtracted that can be
     *                  either positive or negative
     * @return copy of the passed value with new days value
     */
    private static OffsetDateTime changeDays(OffsetDateTime value, int daysDelta) {
        return change(value, DAY_OF_MONTH, daysDelta);
    }

    /**
     * Obtains offset date and time changed on specified amount of hours.
     *
     * @param value      the value to update
     * @param hoursDelta a number of hours that needs to be added or subtracted that can be
     *                   either positive or negative
     * @return copy of the passed value with new hours value
     */
    private static OffsetDateTime changeHours(OffsetDateTime value, int hoursDelta) {
        return change(value, HOUR, hoursDelta);
    }

    /**
     * Obtains offset date and time changed on specified amount of minutes.
     *
     * @param value        offset date and time that will be changed
     * @param minutesDelta a number of minutes that needs to be added or subtracted that can be
     *                     either positive or negative
     * @return copy of the passed value with new minutes value
     */
    private static OffsetDateTime changeMinutes(OffsetDateTime value, int minutesDelta) {
        return change(value, MINUTE, minutesDelta);
    }

    /**
     * Obtains offset date and time changed on specified amount of seconds.
     *
     * @param value        offset date and time that will be changed
     * @param secondsDelta a number of seconds that needs to be added or subtracted that can be
     *                     either positive or negative
     * @return copy of the passed value with new seconds value
     */
    private static OffsetDateTime changeSeconds(OffsetDateTime value, int secondsDelta) {
        return change(value, SECOND, secondsDelta);
    }

    /**
     * Obtains offset date and time changed on specified amount of milliseconds.
     *
     * @param value       offset date and time that will be changed
     * @param millisDelta a number of milliseconds that needs to be added or subtracted that can be
     *                    either positive or negative
     * @return copy of the passed value with new milliseconds value
     */
    private static OffsetDateTime changeMillis(OffsetDateTime value, int millisDelta) {
        return change(value, MILLISECOND, millisDelta);
    }

    /**
     * Performs date and time calculation using parameters of {@link Calendar#add(int, int)}.
     */
    private static OffsetDateTime change(OffsetDateTime value, int calendarField, int delta) {
        final Calendar calendar = toCalendar(value);
        calendar.add(calendarField, delta);

        final LocalDate date = toLocalDate(calendar);
        final LocalTime time = toLocalTime(calendar);
        final int nanos = value.getTime()
                               .getNanos();
        final LocalTime timeWithNanos = time.toBuilder()
                                            .setNanos(nanos)
                                            .build();
        return value.toBuilder()
                    .setDate(date)
                    .setTime(timeWithNanos)
                    .build();
    }

    /**
     * Returns a ISO 8601 date/time string corresponding to the passed value.
     */
    public static String toString(OffsetDateTime value) {
        final Calendar calendar = toCalendar(value);
        final ZoneOffset offset = value.getOffset();
        final StringBuilder result = new StringBuilder();

        // Format the date/time part.
        final Date date = calendar.getTime();
        final String dateTime = dateTimeFormat(offset).format(date);
        result.append(dateTime);

        // Format the fractional second part.
        final LocalTime time = value.getTime();
        appendSubSecond(result, time);

        // Add time zone.
        appendZoneOffset(result, offset);

        return result.toString();
    }

    /**
     * Parse from ISO 8601 date/time string to {@code OffsetDateTime}.
     *
     * @throws ParseException if the passed string is not a valid date-time value
     */
    public static OffsetDateTime parse(String value) throws ParseException {
        return Parser.parseOffsetDateTime(value);
    }
}
