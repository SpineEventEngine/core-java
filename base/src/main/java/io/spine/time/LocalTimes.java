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

import com.google.protobuf.Timestamp;
import io.spine.time.Formats.Parameter;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.time.Calendars.checkArguments;
import static io.spine.time.Calendars.getHours;
import static io.spine.time.Calendars.getMillis;
import static io.spine.time.Calendars.getMinutes;
import static io.spine.time.Calendars.getSeconds;
import static io.spine.time.Calendars.toCalendar;
import static io.spine.time.Calendars.toLocalTime;
import static io.spine.time.Formats.appendSubSecond;
import static io.spine.time.Formats.timeFormat;
import static io.spine.time.Time.HOURS_PER_DAY;
import static io.spine.time.Time.MILLIS_PER_SECOND;
import static io.spine.time.Time.MINUTES_PER_HOUR;
import static io.spine.time.Time.NANOS_PER_MILLISECOND;
import static io.spine.validate.Validate.checkBounds;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

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
        final Timestamp time = Time.getCurrentTime();
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
        checkBounds(millis, Parameter.millis.name(), 0, MILLIS_PER_SECOND - 1);
        checkBounds(nanos, Parameter.nanos.name(), 0, NANOS_PER_MILLISECOND - 1);

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
        checkBounds(hours, Parameter.hours.name(), 0, HOURS_PER_DAY - 1);
        checkBounds(minutes, Parameter.hours.name(), 0, MINUTES_PER_HOUR - 1);
        checkBounds(seconds, Parameter.seconds.name(), 0, MINUTES_PER_HOUR - 1);
    }

    /**
     * Obtains local time from hours, minutes, seconds, and milliseconds.
     */
    public static LocalTime of(int hours, int minutes, int seconds, int millis) {
        return of(hours, minutes, seconds, millis, 0);
    }

    /**
     * Obtains local time from hours, minutes, and seconds.
     */
    public static LocalTime of(int hours, int minutes, int seconds) {
        return of(hours, minutes, seconds, 0, 0);
    }

    /**
     * Obtains local time from hours and minutes.
     */
    public static LocalTime of(int hours, int minutes) {
        return of(hours, minutes, 0, 0, 0);
    }

    /**
     * Obtains a copy of this local time with the specified number of hours added.
     */
    public static LocalTime addHours(LocalTime localTime, int hoursToAdd) {
        checkArguments(localTime, hoursToAdd);
        return changeHours(localTime, hoursToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of minutes added.
     */
    public static LocalTime addMinutes(LocalTime localTime, int minutesToAdd) {
        checkArguments(localTime, minutesToAdd);
        return changeMinutes(localTime, minutesToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of seconds added.
     */
    public static LocalTime addSeconds(LocalTime localTime, int secondsToAdd) {
        checkArguments(localTime, secondsToAdd);
        return changeSeconds(localTime, secondsToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of milliseconds added.
     */
    public static LocalTime addMillis(LocalTime localTime, int millisToAdd) {
        checkArguments(localTime, millisToAdd);
        return changeMillis(localTime, millisToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of hours subtracted.
     */
    public static LocalTime subtractHours(LocalTime localTime, int hoursToSubtract) {
        checkArguments(localTime, hoursToSubtract);
        return changeHours(localTime, -hoursToSubtract);
    }

    /**
     * Obtains a copy of this local time with the specified number of minutes subtracted.
     */
    public static LocalTime subtractMinutes(LocalTime localTime, int minutesToSubtract) {
        checkArguments(localTime, minutesToSubtract);
        return changeMinutes(localTime, -minutesToSubtract);
    }

    /**
     * Obtains a copy of this local time with the specified number of seconds subtracted.
     */
    public static LocalTime subtractSeconds(LocalTime localTime, int secondsToSubtract) {
        checkArguments(localTime, secondsToSubtract);
        return changeSeconds(localTime, -secondsToSubtract);
    }

    /**
     * Obtains a copy of this local time with the specified number of milliseconds subtracted.
     */
    public static LocalTime subtractMillis(LocalTime localTime, int millisToSubtract) {
        checkArguments(localTime, millisToSubtract);
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
        return change(localTime, HOUR, hoursDelta);
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
        return change(localTime, MINUTE, minutesDelta);
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
        return change(localTime, SECOND, secondsDelta);
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
        return change(localTime, MILLISECOND, millisDelta);
    }

    /**
     * Performs time calculation using parameters of {@link Calendar#add(int, int)}.
     */
    private static LocalTime change(LocalTime value, int calendarField, int delta) {
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
