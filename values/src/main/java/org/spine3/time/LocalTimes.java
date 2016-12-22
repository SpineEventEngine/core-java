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
import org.spine3.protobuf.Timestamps;

import java.util.Calendar;

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
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);
        final int hours = calendar.get(Calendar.HOUR);
        final int minutes = calendar.get(Calendar.MINUTE);
        final int seconds = calendar.get(Calendar.SECOND);
        final int millis = calendar.get(Calendar.MILLISECOND);
        final long nanos = time.getNanos();

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
     * Obtains local time from an hours, minutes, seconds millisecond and nanosecond.
     */
    public static LocalTime of(int hours, int minutes, int seconds, int millis, long nanos) {
        checkPositive(hours, "hours");
        checkPositive(minutes, "minutes");
        checkPositive(seconds, "seconds");
        checkPositive(millis, "millis");
        checkPositive(nanos, "nanos");
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
     * Obtains local time from an hours, minutes, seconds and milliseconds.
     */
    public static LocalTime of(int hours, int minutes, int seconds, int millis) {
        checkPositive(hours, "hours");
        checkPositive(minutes, "minutes");
        checkPositive(seconds, "seconds");
        checkPositive(millis, "millis");
        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(hours)
                                          .setMinutes(minutes)
                                          .setSeconds(seconds)
                                          .setMillis(millis)
                                          .build();
        return result;
    }

    /**
     * Obtains local time from an hours, minutes and seconds.
     */
    public static LocalTime of(int hours, int minutes, int seconds) {
        checkPositive(hours, "hours");
        checkPositive(minutes, "minutes");
        checkPositive(seconds, "seconds");
        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(hours)
                                          .setMinutes(minutes)
                                          .setSeconds(seconds)
                                          .build();
        return result;
    }

    /**
     * Obtains local time from an hours and minutes.
     */
    public static LocalTime of(int hours, int minutes) {
        checkPositive(hours, "hours");
        checkPositive(minutes, "minutes");
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
        checkPositive(hoursToAdd, "hoursToAdd");
        return changeHours(localTime, hoursToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of minutes added.
     */
    public static LocalTime plusMinutes(LocalTime localTime, int minutesToAdd) {
        checkPositive(minutesToAdd, "minutesToAdd");
        return changeMinutes(localTime, minutesToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of seconds added.
     */
    public static LocalTime plusSeconds(LocalTime localTime, int secondsToAdd) {
        checkPositive(secondsToAdd, "secondsToAdd");
        return changeSeconds(localTime, secondsToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of milliseconds added.
     */
    public static LocalTime plusMillis(LocalTime localTime, int millisToAdd) {
        checkPositive(millisToAdd, "millisToAdd");
        return changeMillis(localTime, millisToAdd);
    }

    /**
     * Obtains a copy of this local time with the specified number of hours subtracted.
     */
    public static LocalTime minusHours(LocalTime localTime, int hoursToSubtract) {
        checkPositive(hoursToSubtract, "hoursToSubtract");
        return changeHours(localTime, hoursToSubtract);
    }

    /**
     * Obtains a copy of this local time with the specified number of minutes subtracted.
     */
    public static LocalTime minusMinutes(LocalTime localTime, int minutesToSubtract) {
        checkPositive(minutesToSubtract, "minutesToSubtract");
        return changeMinutes(localTime, minutesToSubtract);
    }

    /**
     * Obtains a copy of this local time with the specified number of seconds subtracted.
     */
    public static LocalTime minusSeconds(LocalTime localTime, int secondsToSubtract) {
        checkPositive(secondsToSubtract, "secondsToSubtract");
        return changeSeconds(localTime, secondsToSubtract);
    }

    /**
     * Obtains a copy of this local time with the specified number of milliseconds subtracted.
     */
    public static LocalTime minusMillis(LocalTime localTime, int millisToSubtract) {
        checkPositive(millisToSubtract, "millisToSubtract");
        return changeMillis(localTime, millisToSubtract);
    }
    
    /**
     * Obtains local time changed on specified amount of hours.
     *
     * @param localTime local time that will be changed
     * @param hoursDelta a number of hours that needs to be added or subtracted that can be either positive or negative
     * @return copy of this local time with new hours value
     */
    private static LocalTime changeHours(LocalTime localTime, int hoursDelta) {
        final Timestamp time = Timestamps.getCurrentTime();
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, localTime.getHours());
        calendar.set(Calendar.MINUTE, localTime.getMinutes());
        calendar.set(Calendar.SECOND, localTime.getSeconds());
        calendar.set(Calendar.MILLISECOND, localTime.getMillis());
        calendar.add(Calendar.HOUR, hoursDelta);

        final int hours = calendar.get(Calendar.HOUR);
        final int minutes = calendar.get(Calendar.MINUTE);
        final int seconds = calendar.get(Calendar.SECOND);
        final int millis = calendar.get(Calendar.MILLISECOND);
        final long nanos = time.getNanos();

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
     * Obtains local time changed on specified amount of minutes.
     *
     * @param localTime local time that will be changed
     * @param minutesDelta a number of minutes that needs to be added or subtracted that can be either positive or negative
     * @return copy of this local time with new minutes value
     */
    private static LocalTime changeMinutes(LocalTime localTime, int minutesDelta) {
        final Timestamp time = Timestamps.getCurrentTime();
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, localTime.getHours());
        calendar.set(Calendar.MINUTE, localTime.getMinutes());
        calendar.set(Calendar.SECOND, localTime.getSeconds());
        calendar.set(Calendar.MILLISECOND, localTime.getMillis());
        calendar.add(Calendar.MINUTE, minutesDelta);

        final int hours = calendar.get(Calendar.HOUR);
        final int minutes = calendar.get(Calendar.MINUTE);
        final int seconds = calendar.get(Calendar.SECOND);
        final int millis = calendar.get(Calendar.MILLISECOND);
        final long nanos = time.getNanos();

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
     * Obtains local time changed on specified amount of seconds.
     *
     * @param localTime local time that will be changed
     * @param secondsDelta a number of seconds that needs to be added or subtracted that can be either positive or negative
     * @return copy of this local time with new seconds value
     */
    private static LocalTime changeSeconds(LocalTime localTime, int secondsDelta) {
        final Timestamp time = Timestamps.getCurrentTime();
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, localTime.getHours());
        calendar.set(Calendar.MINUTE, localTime.getMinutes());
        calendar.set(Calendar.SECOND, localTime.getSeconds());
        calendar.set(Calendar.MILLISECOND, localTime.getMillis());
        calendar.add(Calendar.SECOND, secondsDelta);

        final int hours = calendar.get(Calendar.HOUR);
        final int minutes = calendar.get(Calendar.MINUTE);
        final int seconds = calendar.get(Calendar.SECOND);
        final int millis = calendar.get(Calendar.MILLISECOND);
        final long nanos = time.getNanos();

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
     * Obtains local time changed on specified amount of milliseconds.
     *
     * @param localTime local time that will be changed
     * @param millisDelta a number of milliseconds that needs to be added or subtracted that can be either positive or negative
     * @return copy of this local time with new milliseconds value
     */
    private static LocalTime changeMillis(LocalTime localTime, int millisDelta) {
        final Timestamp time = Timestamps.getCurrentTime();
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, localTime.getHours());
        calendar.set(Calendar.MINUTE, localTime.getMinutes());
        calendar.set(Calendar.SECOND, localTime.getSeconds());
        calendar.set(Calendar.MILLISECOND, localTime.getMillis());
        calendar.add(Calendar.MILLISECOND, millisDelta);

        final int hours = calendar.get(Calendar.HOUR);
        final int minutes = calendar.get(Calendar.MINUTE);
        final int seconds = calendar.get(Calendar.SECOND);
        final int millis = calendar.get(Calendar.MILLISECOND);
        final long nanos = time.getNanos();

        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(hours)
                                          .setMinutes(minutes)
                                          .setSeconds(seconds)
                                          .setMillis(millis)
                                          .setNanos(nanos)
                                          .build();
        return result;
    }
    //TODO:2016-12-22:alexander.aleksandrov: Add methods to work with nanos after we will migrate to java 1.8 and java.time
}
