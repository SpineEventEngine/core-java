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
package org.spine3.protobuf;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.TimestampOrBuilder;
import com.google.protobuf.util.TimeUtil;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static com.google.protobuf.util.TimeUtil.subtract;

/**
 * Utilities class for working with {@link Timestamp}s in addition to those available from {@link TimeUtil}.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 * @see TimeUtil
 */
public class Timestamps {

    /**
     * The count of nanoseconds in one millisecond.
     */
    public static final long NANOS_PER_MILLISECOND = 1_000_000;

    /**
     * The count of milliseconds in one second.
     */
    public static final int MILLIS_PER_SECOND = 1000;

    /**
     * The count of seconds in one minute.
     */
    public static final int SECONDS_PER_MINUTE = 60;

    /**
     * The count of minutes in one hour.
     */
    public static final int MINUTES_PER_HOUR = 60;

    /**
     * The duration of one minute.
     */
    private static final Duration MINUTE = Durations.ofMinutes(1);


    /**
     * Returns a timestamp of the moment a minute ago from now.
     *
     * @return a timestamp instance
     */
    public static Timestamp minuteAgo() {
        final Timestamp currentTime = getCurrentTime();
        final Timestamp result = subtract(currentTime, MINUTE);
        return result;
    }

    /**
     * Compares two timestamps. Returns a negative integer, zero, or a positive integer
     * if the first timestamp is less than, equal to, or greater than the second one.
     *
     * @param t1 a timestamp to compare
     * @param t2 another timestamp to compare
     * @return a negative integer, zero, or a positive integer
     * if the first timestamp is less than, equal to, or greater than the second one
     */
    public static int compare(@Nullable Timestamp t1, @Nullable Timestamp t2) {
        if (t1 == null) {
            return (t2 == null) ? 0 : -1;
        }
        if (t2 == null) {
            return 1;
        }
        int result = Long.compare(t1.getSeconds(), t2.getSeconds());
        result = (result == 0) ?
                Integer.compare(t1.getNanos(), t2.getNanos()) :
                result;
        return result;
    }

    /**
     * Calculates if the {@code timestamp} is between the {@code start} and {@code finish} timestamps.
     *
     * @param timestamp the timestamp to check if it is between the {@code start} and {@code finish}
     * @param start the first point in time, must be before the {@code finish} timestamp
     * @param finish the second point in time, must be after the {@code start} timestamp
     * @return true if the {@code timestamp} is after the {@code start} and before the {@code finish} timestamps,
     * false otherwise
     */
    public static boolean isBetween(Timestamp timestamp, Timestamp start, Timestamp finish) {
        final boolean isAfterStart = compare(start, timestamp) < 0;
        final boolean isBeforeFinish = compare(timestamp, finish) < 0;
        return isAfterStart && isBeforeFinish;
    }

    /**
     * Calculates if the {@code isAfter} timestamp is after the {@code point} timestamp.
     *
     * @param point the first point in time which is supposed to be before the {@code isAfter} timestamp
     * @param isAfter the timestamp to check if it is after the {@code point} timestamp
     * @return true if the {@code isAfter} timestamp is after the {@code point} timestamp, false otherwise
     */
    public static boolean isAfter(Timestamp point, Timestamp isAfter) {
        final boolean result = compare(point, isAfter) < 0;
        return result;
    }

    /**
     * Compares two timestamps. Returns a negative integer, zero, or a positive integer
     * if the first timestamp is less than, equal to, or greater than the second one.
     *
     * @return a negative integer, zero, or a positive integer
     * if the first timestamp is less than, equal to, or greater than the second one
     */
    public static Comparator<? super Timestamp> comparator() {
        return new TimestampComparator();
    }

    /**
     * Converts a {@link Timestamp} to {@link Date} to the nearest millisecond.
     *
     * @return a {@link Date} instance
     */
    public static Date convertToDate(TimestampOrBuilder timestamp) {
        final long millisecsFromNanos = timestamp.getNanos() / NANOS_PER_MILLISECOND;
        final long millisecsFromSeconds = timestamp.getSeconds() * MILLIS_PER_SECOND;
        final Date date = new Date(millisecsFromSeconds + millisecsFromNanos);
        return date;
    }

    private static class TimestampComparator implements Comparator<Timestamp>, Serializable {
        @Override
        public int compare(Timestamp t1, Timestamp t2) {
            return Timestamps.compare(t1, t2);
        }

        private static final long serialVersionUID = 0;
    }

    // @formatter:off
    private Timestamps() {}
}
