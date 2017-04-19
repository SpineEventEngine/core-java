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
import static org.spine3.time.Calendars.getHours;
import static org.spine3.time.Calendars.getMillis;
import static org.spine3.time.Calendars.getMinutes;
import static org.spine3.time.Calendars.getSeconds;
import static org.spine3.time.Calendars.toCalendar;
import static org.spine3.time.Calendars.toLocalTime;
import static org.spine3.time.Formats.appendSubSecond;
import static org.spine3.time.Formats.timeFormat;
import static org.spine3.time.Timestamps2.NANOS_PER_MILLISECOND;
import static org.spine3.validate.Validate.checkPositive;
import static org.spine3.validate.Validate.checkPositiveOrZero;

/**
 * Routines for working with {@link LocalTime}.
 *
 * @author Alexander Aleksandrov
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ClassWithTooManyMethods") // OK for this utility class.
public final class LocalTimes {

    private LocalTimes() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains current local time.
     */
    public static LocalTime now() {
        final Timestamp time = Timestamps2.getCurrentTime();
        final ZoneOffset zoneOffset = ZoneOffsets.getDefault();
        return timeAt(time, zoneOffset);
    }

    /**
     * Obtains local time at the passed time zone.
     */
    public static LocalTime timeAt(Timestamp time, ZoneOffset zoneOffset) {
        final Calendar cal = toCalendar(time, zoneOffset);

        final int remainingNanos = time.getNanos() % NANOS_PER_MILLISECOND;
        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(getHours(cal))
                                          .setMinutes(getMinutes(cal))
                                          .setSeconds(getSeconds(cal))
                                          .setMillis(getMillis(cal))
                                          .setNanos(remainingNanos)
                                          .build();
        return result;
    }

    /**
     * Obtains local time from an hours, minutes, seconds, milliseconds, and nanoseconds.
     */
    public static LocalTime of(int hours, int minutes, int seconds, int millis, int nanos) {
        checkClockTime(hours, minutes, seconds);
        checkPositiveOrZero(millis);
        checkPositiveOrZero(nanos);

        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(hours)
                                          .setMinutes(minutes)
                                          .setSeconds(seconds)
                                          .setMillis(millis)
                                          .setNanos(nanos)
                                          .build();
        return result;
    }

    private static void checkClockTime(int hours, int minutes, int seconds) {
        checkPositiveOrZero(hours);
        checkPositiveOrZero(minutes);
        checkPositiveOrZero(seconds);
    }

    /**
     * Obtains local time from an hours, minutes, seconds, and milliseconds.
     */
    public static LocalTime of(int hours, int minutes, int seconds, int millis) {
        checkClockTime(hours, minutes, seconds);
        checkPositiveOrZero(millis);

        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(hours)
                                          .setMinutes(minutes)
                                          .setSeconds(seconds)
                                          .setMillis(millis)
                                          .build();
        return result;
    }

    /**
     * Obtains local time from an hours, minutes, and seconds.
     */
    public static LocalTime of(int hours, int minutes, int seconds) {
        checkClockTime(hours, minutes, seconds);

        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(hours)
                                          .setMinutes(minutes)
                                          .setSeconds(seconds)
                                          .build();
        return result;
    }

    /**
     * Obtains local time from an hours, and minutes.
     */
    public static LocalTime of(int hours, int minutes) {
        checkClockTime(hours, minutes, 0);

        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(hours)
                                          .setMinutes(minutes)
                                          .build();
        return result;
    }

    /**
     * Obtains a copy of this local time with the specified number of hours added.
     */
    public static LocalTime addHours(LocalTime localTime, int hoursToAdd) {
        checkNotNull(localTime);
        checkPositive(hoursToAdd);

        return changeHours(localTime, hoursToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of minutes added.
     */
    public static LocalTime addMinutes(LocalTime localTime, int minutesToAdd) {
        checkNotNull(localTime);
        checkPositive(minutesToAdd);

        return changeMinutes(localTime, minutesToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of seconds added.
     */
    public static LocalTime addSeconds(LocalTime localTime, int secondsToAdd) {
        checkNotNull(localTime);
        checkPositive(secondsToAdd);

        return changeSeconds(localTime, secondsToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of milliseconds added.
     */
    public static LocalTime addMillis(LocalTime localTime, int millisToAdd) {
        checkNotNull(localTime);
        checkPositive(millisToAdd);

        return changeMillis(localTime, millisToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of hours subtracted.
     */
    public static LocalTime subtractHours(LocalTime localTime, int hoursToSubtract) {
        checkNotNull(localTime);
        checkPositive(hoursToSubtract);

        return changeHours(localTime, -hoursToSubtract);
    }

    /**
     * Obtains a copy of this local time with the specified number of minutes subtracted.
     */
    public static LocalTime subtractMinutes(LocalTime localTime, int minutesToSubtract) {
        checkNotNull(localTime);
        checkPositive(minutesToSubtract);

        return changeMinutes(localTime, -minutesToSubtract);
    }

    /**
     * Obtains a copy of this local time with the specified number of seconds subtracted.
     */
    public static LocalTime subtractSeconds(LocalTime localTime, int secondsToSubtract) {
        checkNotNull(localTime);
        checkPositive(secondsToSubtract);

        return changeSeconds(localTime, -secondsToSubtract);
    }

    /**
     * Obtains a copy of this local time with the specified number of milliseconds subtracted.
     */
    public static LocalTime subtractMillis(LocalTime localTime, int millisToSubtract) {
        checkNotNull(localTime);
        checkPositive(millisToSubtract);

        return changeMillis(localTime, -millisToSubtract);
    }

    /**
     * Obtains local time changed on specified amount of hours.
     *
     * @param localTime  local time that will be changed
     * @param hoursDelta a number of hours that needs to be added or subtracted that can be
     *                   either positive or negative
     * @return copy of this local time with new hours value
     */
    private static LocalTime changeHours(LocalTime localTime, int hoursDelta) {
        return add(localTime, HOUR, hoursDelta);
    }

    /**
     * Obtains local time changed on specified amount of minutes.
     *
     * @param localTime    local time that will be changed
     * @param minutesDelta a number of minutes that needs to be added or subtracted that can be
     *                     either positive or negative
     * @return copy of this local time with new minutes value
     */
    private static LocalTime changeMinutes(LocalTime localTime, int minutesDelta) {
        return add(localTime, MINUTE, minutesDelta);
    }

    /**
     * Obtains local time changed on specified amount of seconds.
     *
     * @param localTime    local time that will be changed
     * @param secondsDelta a number of seconds that needs to be added or subtracted that can be
     *                     either positive or negative
     * @return copy of this local time with new seconds value
     */
    private static LocalTime changeSeconds(LocalTime localTime, int secondsDelta) {
        return add(localTime, SECOND, secondsDelta);
    }

    /**
     * Obtains local time changed on specified amount of milliseconds.
     *
     * @param localTime   local time that will be changed
     * @param millisDelta a number of milliseconds that needs to be added or subtracted that can be
     *                    either positive or negative
     * @return copy of this local time with new milliseconds value
     */
    private static LocalTime changeMillis(LocalTime localTime, int millisDelta) {
        return add(localTime, MILLISECOND, millisDelta);
    }

    /**
     * Performs time calculation using parameters of {@link Calendar#add(int, int)}.
     */
    private static LocalTime add(LocalTime value, int calendarField, int delta) {
        final Calendar cal = toCalendar(value);
        cal.add(calendarField, delta);
        final LocalTime result = toLocalTime(cal).toBuilder()
                                                 .setNanos(value.getNanos())
                                                 .build();
        return result;
    }

    /**
     * Obtains a fraction part of a second as total number of nanoseconds.
     *
     * <p>{@code LocalTime} stores a fractional part of a second as a number of milliseconds and
     * nanoseconds. This method computes the total in nanoseconds.
     */
    static long getTotalNanos(LocalTime time) {
        checkNotNull(time);
        final long result = (long)time.getMillis() * NANOS_PER_MILLISECOND + time.getNanos();
        return result;
    }

    /**
     * Converts the passed time to string with optional part representing a fraction of a second.
     *
     * <p>Examples of results: {@code "13:45:30.123456789"}, {@code "09:37:00"}.
     */
    public static String toString(LocalTime time) {
        final Calendar calendar = toCalendar(time);
        final StringBuilder result = new StringBuilder();

        // Format the time part.
        final Date date = calendar.getTime();
        final String timePart = timeFormat().format(date);
        result.append(timePart);

        // Add the fractional second part.
        appendSubSecond(result, time);

        return result.toString();
    }

    /**
     * Parses the passed string into local time value.
     */
    public static LocalTime parse(String str) throws ParseException {
        return Parser.parseLocalTime(str);
    }

}
