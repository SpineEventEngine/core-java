/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.util;

import com.google.protobuf.Timestamp;

import java.util.Comparator;

/**
 * Utility class for working with timestamps.
 *
 * @author Mikhail Melnik
 */
@SuppressWarnings("UtilityClass")
public class Timestamps {

    private static final long BILLION = 1_000_000_000;

    /**
     * Returns current system time as a {@link Timestamp} object.
     * <p>
     * Timestamp is represented as the combination of milliseconds and nanoseconds.
     * <p>
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
    public static int compare(Timestamp t1, Timestamp t2) {
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

    public static Comparator<? super Timestamp> comparator() {
        return new TimestampComparator();
    }

    private static class TimestampComparator implements Comparator<Timestamp> {
        @Override
        public int compare(Timestamp t1, Timestamp t2) {
            return Timestamps.compare(t1, t2);
        }
    }

    private Timestamps() {
    }

}
