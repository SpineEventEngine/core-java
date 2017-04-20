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

import com.google.protobuf.Timestamp;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static org.spine3.time.Calendars.at;
import static org.spine3.time.Calendars.toCalendar;
import static org.spine3.time.Calendars.toLocalTime;
import static org.spine3.time.Formats.appendSubSecond;
import static org.spine3.time.Formats.appendZoneOffset;
import static org.spine3.time.Formats.timeFormat;
import static org.spine3.validate.Validate.checkPositive;

/**
 * Routines for working with {@link OffsetTime}.
 *
 * @author Alexander Aleksandrov
 * @author Alexander Yevsyukov
 */
public final class OffsetTimes {

    private OffsetTimes() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains offset time using {@code ZoneOffset}.
     */
    public static OffsetTime now(ZoneOffset zoneOffset) {
        checkNotNull(zoneOffset);
        final Calendar cal = at(zoneOffset);
        final LocalTime localTime = toLocalTime(cal);
        final OffsetTime result = OffsetTime.newBuilder()
                                            .setTime(localTime)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

    /**
     * Converts the passed timestamp to offset time at the passed time zone.
     */
    public static OffsetTime timeAt(Timestamp time, ZoneOffset zoneOffset) {
        final LocalTime localTime = LocalTimes.timeAt(time, zoneOffset);
        return of(localTime, zoneOffset);
    }

    /**
     * Obtains offset time using {@code LocalTime} and {@code ZoneOffset}.
     */
    public static OffsetTime of(LocalTime time, ZoneOffset zoneOffset) {
        checkNotNull(time);
        checkNotNull(zoneOffset);

        final OffsetTime result = OffsetTime.newBuilder()
                                            .setTime(time)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

    /**
     * Obtains a copy of this offset time with the specified number of hours added.
     */
    public static OffsetTime addHours(OffsetTime value, int hoursToAdd) {
        checkNotNull(value);
        checkPositive(hoursToAdd);

        return changeHours(value, hoursToAdd);
    }

    /**
     * Obtains a copy of this offset time with the specified number of minutes added.
     */
    public static OffsetTime addMinutes(OffsetTime value, int minutesToAdd) {
        checkNotNull(value);
        checkPositive(minutesToAdd);

        return changeMinutes(value, minutesToAdd);
    }

    /**
     * Obtains a copy of this offset time with the specified number of seconds added.
     */
    public static OffsetTime addSeconds(OffsetTime value, int secondsToAdd) {
        checkNotNull(value);
        checkPositive(secondsToAdd);

        return changeSeconds(value, secondsToAdd);
    }

    /**
     * Obtains a copy of this offset time with the specified number of milliseconds added.
     */
    public static OffsetTime addMillis(OffsetTime value, int millisToAdd) {
        checkNotNull(value);
        checkPositive(millisToAdd);

        return changeMillis(value, millisToAdd);
    }

    /**
     * Obtains a copy of this offset time with the specified number of hours subtracted.
     */
    public static OffsetTime subtractHours(OffsetTime value, int hoursToSubtract) {
        checkNotNull(value);
        checkPositive(hoursToSubtract);

        return changeHours(value, -hoursToSubtract);
    }

    /**
     * Obtains a copy of this offset time with the specified number of minutes subtracted.
     */
    public static OffsetTime subtractMinutes(OffsetTime value, int minutesToSubtract) {
        checkNotNull(value);
        checkPositive(minutesToSubtract);

        return changeMinutes(value, -minutesToSubtract);
    }

    /**
     * Obtains a copy of this offset time with the specified number of seconds subtracted.
     */
    public static OffsetTime subtractSeconds(OffsetTime value, int secondsToSubtract) {
        checkNotNull(value);
        checkPositive(secondsToSubtract);

        return changeSeconds(value, -secondsToSubtract);
    }

    /**
     * Obtains a copy of this offset time with the specified number of milliseconds subtracted.
     */
    public static OffsetTime subtractMillis(OffsetTime value, int millisToSubtract) {
        checkNotNull(value);
        checkPositive(millisToSubtract);

        return changeMillis(value, -millisToSubtract);
    }

    /**
     * Obtains offset time changed on specified amount of hours.
     *
     * @param value      offset time that will be changed
     * @param hoursDelta a number of hours that needs to be added or subtracted that can be
     *                   either positive or negative
     * @return copy of this offset time with new hours value
     */
    private static OffsetTime changeHours(OffsetTime value, int hoursDelta) {
        final OffsetTime result = add(value, HOUR, hoursDelta);
        return result;
    }

    /**
     * Obtains offset time changed on specified amount of minutes.
     *
     * @param value        offset time that will be changed
     * @param minutesDelta a number of minutes that needs to be added or subtracted that can be
     *                     either positive or negative
     * @return copy of this offset time with new minutes value
     */
    private static OffsetTime changeMinutes(OffsetTime value, int minutesDelta) {
        final OffsetTime result = add(value, MINUTE, minutesDelta);
        return result;
    }

    /**
     * Obtains offset time changed on specified amount of seconds.
     *
     * @param value        offset time that will be changed
     * @param secondsDelta a number of seconds that needs to be added or subtracted that can be
     *                     either positive or negative
     * @return copy of this offset time with new seconds value
     */
    private static OffsetTime changeSeconds(OffsetTime value, int secondsDelta) {
        final OffsetTime result = add(value, SECOND, secondsDelta);
        return result;
    }

    /**
     * Obtains offset time changed on specified amount of milliseconds.
     *
     * @param value       offset time that will be changed
     * @param millisDelta a number of milliseconds that needs to be added or subtracted that can be
     *                    either positive or negative
     * @return copy of this offset time with new milliseconds value
     */
    private static OffsetTime changeMillis(OffsetTime value, int millisDelta) {
        final OffsetTime result = add(value, MILLISECOND, millisDelta);
        return result;
    }

    /**
     * Performs time calculation using parameters of {@link Calendar#add(int, int)}.
     */
    private static OffsetTime add(OffsetTime value, int calendarField, int delta) {
        final Calendar cal = toCalendar(value);
        cal.add(calendarField, delta);
        final int nanos = value.getTime()
                               .getNanos();
        final LocalTime localTime = toLocalTime(cal).toBuilder()
                                                    .setNanos(nanos)
                                                    .build();
        final ZoneOffset zoneOffset = value.getOffset();
        return OffsetTime.newBuilder()
                         .setTime(localTime)
                         .setOffset(zoneOffset)
                         .build();
    }

    /**
     * Returns a ISO 8601 time string corresponding to the passed value.
     */
    public static String toString(OffsetTime value) {
        final Calendar calendar = toCalendar(value);
        final StringBuilder result = new StringBuilder();

        // Format the date/time part.
        final Date date = calendar.getTime();
        final String dateTime = timeFormat().format(date);
        result.append(dateTime);

        // Format the fractional second part.
        final LocalTime time = value.getTime();
        appendSubSecond(result, time);

        // Add time zone.
        final ZoneOffset offset = value.getOffset();
        appendZoneOffset(result, offset);

        return result.toString();
    }

    /**
     * Parse from ISO 8601 string to {@code OffsetTime}.
     */
    public static OffsetTime parse(String str) throws ParseException {
        return Parser.parseOffsetTime(str);
    }
}
