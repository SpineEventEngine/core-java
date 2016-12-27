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
import org.spine3.change.Changes;
import org.spine3.protobuf.Timestamps;
import org.spine3.time.change.Changes.ArgumentName;

import java.util.Calendar;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.MILLISECOND;
import static org.spine3.time.Calendars.createTime;
import static org.spine3.time.Calendars.getHours;
import static org.spine3.time.Calendars.getMillis;
import static org.spine3.time.Calendars.getMinutes;
import static org.spine3.time.Calendars.getSeconds;
import static org.spine3.validate.Validate.checkPositive;

/**
 * Routines for working with {@link LocalTime}.
 *
 * @author Alexander Aleksandrov
 */
public class LocalTimes {

    private LocalTimes() {
    }

    /**
     * Obtains current local time.
     */
    public static LocalTime now() {
        final Timestamp time = Timestamps.getCurrentTime();
        final Calendar cal = Calendars.createTime();

        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(getHours(cal))
                                          .setMinutes(getMinutes(cal))
                                          .setSeconds(getSeconds(cal))
                                          .setMillis(getMillis(cal))
                                          .setNanos(time.getNanos())
                                          .build();
        return result;
    }

    /**
     * Obtains local time from an hours, minutes, seconds, milliseconds, and nanoseconds.
     */
    public static LocalTime of(int hours, int minutes, int seconds, int millis, long nanos) {
        checkPositive(hours, ArgumentName.HOURS);
        checkPositive(minutes, ArgumentName.MINUTES);
        checkPositive(seconds, ArgumentName.SECONDS);
        checkPositive(millis, ArgumentName.MILLIS);
        checkPositive(nanos, ArgumentName.NANOS);

        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(hours)
                                          .setMinutes(minutes)
                                          .setSeconds(seconds)
                                          .setMillis(millis)
                                          .setNanos(nanos)
                                          .build();
        return result;
    }

    /**
     * Obtains local time from an hours, minutes, seconds, and milliseconds.
     */
    public static LocalTime of(int hours, int minutes, int seconds, int millis) {
        checkPositive(hours, ArgumentName.HOURS);
        checkPositive(minutes, ArgumentName.MINUTES);
        checkPositive(seconds, ArgumentName.SECONDS);
        checkPositive(millis, ArgumentName.MILLIS);

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
        checkPositive(hours, ArgumentName.HOURS);
        checkPositive(minutes, ArgumentName.MINUTES);
        checkPositive(seconds, ArgumentName.SECONDS);

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
        checkPositive(hours, ArgumentName.HOURS);
        checkPositive(minutes, ArgumentName.MINUTES);

        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(hours)
                                          .setMinutes(minutes)
                                          .build();
        return result;
    }

    /**
     * Obtains a copy of this local time with the specified number of hours added.
     */
    public static LocalTime plusHours(LocalTime localTime, int hoursToAdd) {
        checkNotNull(localTime, Changes.ErrorMessage.LOCAL_TIME);
        checkPositive(hoursToAdd, ArgumentName.HOURS_TO_ADD);
        return changeHours(localTime, hoursToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of minutes added.
     */
    public static LocalTime plusMinutes(LocalTime localTime, int minutesToAdd) {
        checkNotNull(localTime, Changes.ErrorMessage.LOCAL_TIME);
        checkPositive(minutesToAdd, ArgumentName.MINUTES_TO_ADD);
        return changeMinutes(localTime, minutesToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of seconds added.
     */
    public static LocalTime plusSeconds(LocalTime localTime, int secondsToAdd) {
        checkNotNull(localTime, Changes.ErrorMessage.LOCAL_TIME);
        checkPositive(secondsToAdd, ArgumentName.SECONDS_TO_ADD);
        return changeSeconds(localTime, secondsToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of milliseconds added.
     */
    public static LocalTime plusMillis(LocalTime localTime, int millisToAdd) {
        checkNotNull(localTime, Changes.ErrorMessage.LOCAL_TIME);
        checkPositive(millisToAdd, ArgumentName.MILLIS_TO_ADD);
        return changeMillis(localTime, millisToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of hours subtracted.
     */
    public static LocalTime minusHours(LocalTime localTime, int hoursToSubtract) {
        checkNotNull(localTime, Changes.ErrorMessage.LOCAL_TIME);
        checkPositive(hoursToSubtract, ArgumentName.HOURS_TO_SUBTRACT);
        return changeHours(localTime, -hoursToSubtract);
    }

    /**
     * Obtains a copy of this local time with the specified number of minutes subtracted.
     */
    public static LocalTime minusMinutes(LocalTime localTime, int minutesToSubtract) {
        checkNotNull(localTime, Changes.ErrorMessage.LOCAL_TIME);
        checkPositive(minutesToSubtract, ArgumentName.MINUTES_TO_SUBTRACT);
        return changeMinutes(localTime, -minutesToSubtract);
    }

    /**
     * Obtains a copy of this local time with the specified number of seconds subtracted.
     */
    public static LocalTime minusSeconds(LocalTime localTime, int secondsToSubtract) {
        checkNotNull(localTime, Changes.ErrorMessage.LOCAL_TIME);
        checkPositive(secondsToSubtract, ArgumentName.SECONDS_TO_SUBTRACT);
        return changeSeconds(localTime, -secondsToSubtract);
    }

    /**
     * Obtains a copy of this local time with the specified number of milliseconds subtracted.
     */
    public static LocalTime minusMillis(LocalTime localTime, int millisToSubtract) {
        checkNotNull(localTime, Changes.ErrorMessage.LOCAL_TIME);
        checkPositive(millisToSubtract, ArgumentName.MILLIS_TO_SUBTRACT);
        return changeMillis(localTime, -millisToSubtract);
    }

    /**
     * Obtains local time changed on specified amount of hours.
     *
     * @param localTime  local time that will be changed
     * @param hoursDelta a number of hours that needs to be added or subtracted that can be either positive or negative
     * @return copy of this local time with new hours value
     */
    private static LocalTime changeHours(LocalTime localTime, int hoursDelta) {
        final Calendar cal = createTime(localTime.getHours(), localTime.getMinutes(),
                                        localTime.getSeconds(), localTime.getMillis());
        cal.add(HOUR, hoursDelta);

        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(getHours(cal))
                                          .setMinutes(getMinutes(cal))
                                          .setSeconds(getSeconds(cal))
                                          .setMillis(getMillis(cal))
                                          .setNanos(localTime.getNanos())
                                          .build();
        return result;
    }

    /**
     * Obtains local time changed on specified amount of minutes.
     *
     * @param localTime    local time that will be changed
     * @param minutesDelta a number of minutes that needs to be added or subtracted that can be either positive or negative
     * @return copy of this local time with new minutes value
     */
    private static LocalTime changeMinutes(LocalTime localTime, int minutesDelta) {
        final Calendar cal = createTime(localTime.getHours(), localTime.getMinutes(),
                                        localTime.getSeconds(), localTime.getMillis());
        cal.add(MINUTE, minutesDelta);

        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(getHours(cal))
                                          .setMinutes(getMinutes(cal))
                                          .setSeconds(getSeconds(cal))
                                          .setMillis(getMillis(cal))
                                          .setNanos(localTime.getNanos())
                                          .build();
        return result;
    }

    /**
     * Obtains local time changed on specified amount of seconds.
     *
     * @param localTime    local time that will be changed
     * @param secondsDelta a number of seconds that needs to be added or subtracted that can be either positive or negative
     * @return copy of this local time with new seconds value
     */
    private static LocalTime changeSeconds(LocalTime localTime, int secondsDelta) {
        final Calendar cal = createTime(localTime.getHours(), localTime.getMinutes(),
                                        localTime.getSeconds(), localTime.getMillis());
        cal.add(SECOND, secondsDelta);

        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(getHours(cal))
                                          .setMinutes(getMinutes(cal))
                                          .setSeconds(getSeconds(cal))
                                          .setMillis(getMillis(cal))
                                          .setNanos(localTime.getNanos())
                                          .build();
        return result;
    }

    /**
     * Obtains local time changed on specified amount of milliseconds.
     *
     * @param localTime   local time that will be changed
     * @param millisDelta a number of milliseconds that needs to be added or subtracted that can be either positive or negative
     * @return copy of this local time with new milliseconds value
     */
    private static LocalTime changeMillis(LocalTime localTime, int millisDelta) {
        final Calendar cal = createTime(localTime.getHours(), localTime.getMinutes(),
                                        localTime.getSeconds(), localTime.getMillis());
        cal.add(MILLISECOND, millisDelta);

        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(getHours(cal))
                                          .setMinutes(getMinutes(cal))
                                          .setSeconds(getSeconds(cal))
                                          .setMillis(getMillis(cal))
                                          .setNanos(localTime.getNanos())
                                          .build();
        return result;
    }
}
