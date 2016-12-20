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

/**
 * Routines for working with {@link LocalTime}.
 *
 * @author Alexander Aleksandrov
 */
public class LocalTimes {

    private LocalTimes() {
    }

    /**
     * Obtains current LocalTime instance.
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
     * Obtains LocalTime instance from an hour, minute, second millisecond and nanosecond.
     */
    public static LocalTime of(int hour, int minute, int second, int milli, int nano) {
        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(hour)
                                          .setMinutes(minute)
                                          .setSeconds(second)
                                          .setMillis(milli)
                                          .setNanos(nano)
                                          .build();
        return result;
    }

    /**
     * Obtains LocalTime instance from an hour, minute, second millisecond.
     */
    public static LocalTime of(int hour, int minute, int second, int milli) {
        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(hour)
                                          .setMinutes(minute)
                                          .setSeconds(second)
                                          .setMillis(milli)
                                          .build();
        return result;
    }

    /**
     * Obtains LocalTime instance from an hour, minute, second.
     */
    public static LocalTime of(int hour, int minute, int second) {
        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(hour)
                                          .setMinutes(minute)
                                          .setSeconds(second)
                                          .build();
        return result;
    }

    /**
     * Obtains LocalTime instance from an hour and minute.
     */
    public static LocalTime of(int hour, int minute) {
        final LocalTime result = LocalTime.newBuilder()
                                          .setHours(hour)
                                          .setMinutes(minute)
                                          .build();
        return result;
    }

    /**
     * Obtains current LocalTime instance with the specified number of hours added.
     */
    public static LocalTime plusHours(int hoursToAdd) {
        final Timestamp time = Timestamps.getCurrentTime();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);
        calendar.add(Calendar.HOUR, hoursToAdd);
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
     * Obtains current LocalTime instance with the specified number of seconds added.
     */
    public static LocalTime plusSeconds(int secondsToAdd) {
        final Timestamp time = Timestamps.getCurrentTime();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);
        calendar.add(Calendar.SECOND, secondsToAdd);
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
}
