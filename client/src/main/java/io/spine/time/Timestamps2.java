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
import com.google.protobuf.TimestampOrBuilder;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * Utilities class for working with {@link Timestamp}s in addition to those available from
 * {@link com.google.protobuf.util.Timestamps Timestamps} class from Protobuf.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public final class Timestamps2 {

    private Timestamps2() {
        // Prevent instantiation of this utility class.
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
        result = (result == 0)
                 ? Integer.compare(t1.getNanos(), t2.getNanos())
                 : result;
        return result;
    }

    /**
     * Calculates if the {@code timestamp} is between the {@code start} and
     * {@code finish} timestamps.
     *
     * @param timestamp the timestamp to check if it is between the {@code start} and {@code finish}
     * @param start     the first point in time, must be before the {@code finish} timestamp
     * @param finish    the second point in time, must be after the {@code start} timestamp
     * @return {@code true} if the {@code timestamp} is after the {@code start} and before
     * the {@code finish} timestamps, {@code false} otherwise
     */
    public static boolean isBetween(Timestamp timestamp, Timestamp start, Timestamp finish) {
        final boolean isAfterStart = compare(start, timestamp) < 0;
        final boolean isBeforeFinish = compare(timestamp, finish) < 0;
        return isAfterStart && isBeforeFinish;
    }

    /**
     * Calculates if {@code timestamp} is later {@code thanTime} timestamp.
     *
     * @param timestamp the timestamp to check if it is later then {@code thanTime}
     * @param thanTime  the first point in time which is supposed to be before the {@code timestamp}
     * @return {@code true} if the {@code timestamp} is later than {@code thanTime} timestamp,
     * {@code false} otherwise
     */
    public static boolean isLaterThan(Timestamp timestamp, Timestamp thanTime) {
        final boolean isAfter = compare(timestamp, thanTime) > 0;
        return isAfter;
    }

    /**
     * Converts a {@link Timestamp} to {@link Date} to the nearest millisecond.
     *
     * @return a {@link Date} instance
     */
    public static Date toDate(TimestampOrBuilder timestamp) {
        final long millisecsFromNanos = timestamp.getNanos() / Time.NANOS_PER_MILLISECOND;
        final long millisecsFromSeconds = timestamp.getSeconds() * Time.MILLIS_PER_SECOND;
        final Date date = new Date(millisecsFromSeconds + millisecsFromNanos);
        return date;
    }
}
