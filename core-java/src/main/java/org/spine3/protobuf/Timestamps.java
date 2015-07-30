/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Timestamp;
import com.google.protobuf.TimestampOrBuilder;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

/**
 * Utility class for working with timestamps.
 *
 * @author Mikhail Melnik
 */
@SuppressWarnings("UtilityClass")
public class Timestamps {

    private static final long THOUSAND = 1_000;
    private static final long MILLION = 1_000_000;
    private static final long BILLION = 1_000_000_000;

    /**
     * Returns current system time as a {@link Timestamp} object.
     * <p/>
     * Timestamp is represented as the combination of milliseconds and nanoseconds.
     * <p/>
     * Current implementation is based on {@link System#nanoTime()}
     * that is native, so it's behavior and result may vary on different platforms.
     *
     * @return current system time as {@link Timestamp}
     */
    public static Timestamp now() {
        long nanoTime = System.nanoTime();

        //noinspection NumericCastThatLosesPrecision
        return Timestamp.newBuilder()
                .setSeconds(nanoTime / BILLION)
                .setNanos((int) (nanoTime % BILLION))
                .build();
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public static int compare(@Nullable Timestamp t1, @Nullable Timestamp t2) {
        if (t1 == null) {
            return t2 == null ? 0 : 1;
        }
        if (t2 == null) {
            return -1;
        }

        int result = Long.compare(t1.getSeconds(), t2.getSeconds());
        return result == 0
                ? Integer.compare(t1.getNanos(), t2.getNanos())
                : result;
    }

    public static boolean isBetween(Timestamp timestamp, Timestamp from, Timestamp to) {
        return (compare(from, timestamp) < 0)
                && (compare(timestamp, to) < 0);
    }

    public static boolean isAfter(Timestamp timestamp, Timestamp from) {
        return (compare(from, timestamp) < 0);
    }

    public static Comparator<? super Timestamp> comparator() {
        return new TimestampComparator();
    }

    public static Date convertToDate(TimestampOrBuilder timestamp) {
        final long millis = timestamp.getNanos() / MILLION * timestamp.getSeconds();
        final Date date = new Date(millis);
        return date;
    }

    public static Timestamp parseFromDate(Date date) {
        final long time = date.getTime();
        //noinspection NumericCastThatLosesPrecision
        final int nanos = (int) ((time * MILLION) % BILLION);

        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(time / THOUSAND)
                .setNanos(nanos)
                .build();
        return timestamp;
    }

    private static class TimestampComparator implements Comparator<Timestamp>, Serializable {
        @Override
        public int compare(Timestamp t1, Timestamp t2) {
            return Timestamps.compare(t1, t2);
        }

        private static final long serialVersionUID = 0;
    }

    private Timestamps() {
    }

}
