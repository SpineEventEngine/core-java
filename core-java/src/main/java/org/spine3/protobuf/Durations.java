/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.protobuf;

import com.google.protobuf.Duration;
import com.google.protobuf.DurationOrBuilder;
import com.google.protobuf.util.TimeUtil;

import javax.annotation.Nullable;

import static com.google.protobuf.util.TimeUtil.createDurationFromMillis;
import static com.google.protobuf.util.TimeUtil.toMillis;
import static org.spine3.util.Math.floorDiv;
import static org.spine3.util.Math.safeMultiply;

/**
 * Utility class for working with durations.
 * 
 * <p>Use {@code import static org.spine3.protobuf.Durations.*} for compact initialization like this:
 * <pre>
 *      Duration d = add(hours(2), minutes(30));
 * </pre>
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"UtilityClass", "ClassWithTooManyMethods"})
public class Durations {

    public static final com.google.protobuf.Duration ZERO = createDurationFromMillis(0L);

    private static final int MILLIS_PER_SECOND = 1000;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;

    private Durations() {}

    /*
     * The following group of factory methods is for mimicking javax.time (or java.time) API
     * of creating durations. The goal of these methods is to allow for quick transition to
     * using com.google.protobuf.Duration just by adding 's' before the dot.
     */

    /**
     * Obtains an instance of {@code Duration} from the number of seconds.
     *
     * @param seconds the number of seconds, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration ofSeconds(long seconds) {
        return createDurationFromMillis(safeMultiply(seconds, MILLIS_PER_SECOND));
    }


    /**
     * Obtains an instance of {@code Duration} representing the passed number of minutes.
     *
     * @param minutes the number of minutes, positive or negative.
     * @return a non-null {@code Duration}
     */
    public static Duration ofMinutes(long minutes) {
        return ofSeconds(safeMultiply(minutes, SECONDS_PER_MINUTE));
    }

    /**
     * Obtains an instance of {@code Duration} representing the passed number of hours.
     * @param hours the number of hours, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration ofHours(long hours) {
        return ofMinutes(safeMultiply(hours, MINUTES_PER_HOUR));
    }

    /**
     * Obtains an instance of {@code Duration} representing the passed number of nanoseconds.
     * @param nanos the number of nanoseconds, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration nanos(long nanos) {
        return TimeUtil.createDurationFromNanos(nanos);
    }

    /**
     * Obtains an instance of {@code Duration} representing the passed number of seconds.
     * @param seconds the number of seconds, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration seconds(long seconds) {
        return ofSeconds(seconds);
    }

    // Methods for brief computations with Durations like
    //       add(hours(2), minutes(30));

    /**
     * This method allows for more compact code of creation of {@code Duration} instance with minutes.
     */
    public static Duration minutes(long minutes) {
        return ofMinutes(minutes);
    }

    /**
     * This method allows for more compact code of creation of {@code Duration} instance with hours.
     */
    public static Duration hours(long hours) {
        return ofHours(hours);
    }

    /**
     * Adds two durations one of which or both can be {@code null}.
     * @param d1 a duration to add, could be {@code null}
     * @param d2 another duration to add, could be {@code null}
     * @return
     *      <ul>
     *          <li>sum of two durations if both of them are {@code non-null}</li>
     *          <li>another {@code non-null} value, if one is {@code null}</li>
     *          <li>{@link #ZERO} if both values are {@code null}</li>
     *      </ul>
     *
     */
    public static Duration add(@Nullable Duration d1, @Nullable Duration d2) {
        if (d1 == null && d2 == null) {
            return ZERO;
        }

        if (d1 == null) {
            return d2;
        }

        if (d2 == null) {
            return d1;
        }

        return TimeUtil.add(d1, d2);
    }


    /**
     * Subtract a duration from another.
     */
    public static Duration subtract(Duration d1, Duration d2) {
        /* The sole purpose of this method is minimize the dependencies of the classes
           working with durations. */
        return TimeUtil.subtract(d1, d2);
    }

    /**
     * This method allows for more compact code of creation of {@code Duration} instance with hours and minutes.
     */
    public static Duration hoursAndMinutes(long hours, long minutes) {
        return add(hours(hours), minutes(minutes));
    }

    /**
     * Convert a duration to the number of nanoseconds.
     */
    public static long toNanos(Duration duration) {
        /* The sole purpose of this method is minimize the dependencies of the classes
           working with durations. */
        return TimeUtil.toNanos(duration);
    }

    /**
     * Convert a duration to the number of seconds.
     */
    public static long toSeconds(Duration duration) {
        return floorDiv(TimeUtil.toMillis(duration), MILLIS_PER_SECOND);
    }

    /**
     * Converts passed duration to long value of minutes.
     *
     * @param duration a duration to convert
     * @return duration in minutes
     */
    public static long toMinutes(Duration duration) {
        final long millis = toMillis(duration);
        final long result = (millis / MILLIS_PER_SECOND) / SECONDS_PER_MINUTE;
        return result;
    }

    /**
     * Returns the number of hours in the passed duration.
     *
     * @param value duration
     * @return number of hours
     */
    public static long getHours(Duration value) {
        final long hours = toMinutes(value);
        final long result = hours / MINUTES_PER_HOUR;
        return result;
    }

    /**
     * Returns the only remainder of minutes from the passed duration subtracting
     * the amount of full hours.
     *
     * @param value duration
     * @return number of minutes
     */
    public static int getMinutes(Duration value) {
        final long allMinutes = toMinutes(value);
        final long remainder = allMinutes % MINUTES_PER_HOUR;
        return Long.valueOf(remainder).intValue();
    }

    /**
     * @return {@code true} of the passed value is greater or equal zero, {@code false} otherwise
     */
    public static boolean isPositiveOrZero(Duration value) {
        final long millis = toMillis(value);
        return millis >= 0;
    }

    /**
     * @return {@code true} if the passed value is greater than zero, {@code false} otherwise
     */
    public static boolean isPositive(DurationOrBuilder value) {
        return value.getSeconds() > 0 || value.getNanos() > 0;

    }

    /**
     * @return {@code true} if the passed value is zero, {@code false} otherwise
     */
    public static boolean isZero(DurationOrBuilder value) {
        return value.getSeconds() == 0 && value.getNanos() == 0;
    }

    /**
     * @return {@code true} if the first argument is greater than the second, {@code false} otherwise
     */
    public static boolean isGreaterThan(Duration value, Duration another) {
        final long nanos = toNanos(value);
        final long anotherNanos = toNanos(another);
        return nanos > anotherNanos;
    }

    /**
     * @return {@code true} if the first argument is less than the second, {@code false} otherwise
     */
    public static boolean isLessThan(Duration value, Duration another) {
        final long nanos = toNanos(value);
        final long anotherNanos = toNanos(another);
        return nanos < anotherNanos;
    }

    /**
     * Numerically compare passed durations as nanosecond values.
     */
    public static int compare(Duration d1, Duration d2) {
        final long nanos = toNanos(d1);
        final long otherNanos = toNanos(d2);
        return Long.compare(nanos, otherNanos);
    }

    /**
     * @return {@code true} if the passed duration is negative, {@code false} otherwise
     */
    public static boolean isNegative(Duration value) {
        final long nanos = toNanos(value);
        return nanos < 0;
    }
}
