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
import static org.spine3.protobuf.Timestamps.*;
import static org.spine3.util.Math.floorDiv;
import static org.spine3.util.Math.safeMultiply;

/**
 * Utility class for working with durations in addition to those available from {@link TimeUtil}.
 *
 * <p>Use {@code import static org.spine3.protobuf.Durations.*} for compact initialization like this:
 * <pre>
 *      Duration d = add(hours(2), minutes(30));
 * </pre>
 *
 * @author Alexander Yevsyukov
 * @see TimeUtil
 */
@SuppressWarnings({"UtilityClass", "ClassWithTooManyMethods"})
public class Durations {

    public static final com.google.protobuf.Duration ZERO = createDurationFromMillis(0L);

    private Durations() {}

    /*
     * The following group of factory methods is for mimicking javax.time (or java.time) API
     * of creating durations. The goal of these methods is to allow for quick transition to
     * using com.google.protobuf.Duration just by adding 's' before the dot.
     */

    /**
     * Obtains an instance of {@code Duration} representing the passed number of milliseconds.
     *
     * @param milliseconds the number of milliseconds, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration ofMilliseconds(long milliseconds) {
        final Duration result = createDurationFromMillis(milliseconds);
        return result;
    }

    /**
     * Obtains an instance of {@code Duration} from the number of seconds.
     *
     * @param seconds the number of seconds, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration ofSeconds(long seconds) {
        final Duration result = createDurationFromMillis(safeMultiply(seconds, MILLIS_PER_SECOND));
        return result;
    }

    /**
     * Obtains an instance of {@code Duration} representing the passed number of minutes.
     *
     * @param minutes the number of minutes, positive or negative.
     * @return a non-null {@code Duration}
     */
    public static Duration ofMinutes(long minutes) {
        final Duration duration = ofSeconds(safeMultiply(minutes, SECONDS_PER_MINUTE));
        return duration;
    }

    /**
     * Obtains an instance of {@code Duration} representing the passed number of hours.
     *
     * @param hours the number of hours, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration ofHours(long hours) {
        final Duration duration = ofMinutes(safeMultiply(hours, MINUTES_PER_HOUR));
        return duration;
    }

    // Methods for brief computations with Durations like
    //       add(hours(2), minutes(30));
    /////////////////////////////////////////////////////

    /**
     * Obtains an instance of {@code Duration} representing the passed number of nanoseconds.
     *
     * @param nanos the number of nanoseconds, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration nanos(long nanos) {
        final Duration duration = TimeUtil.createDurationFromNanos(nanos);
        return duration;
    }

    /**
     * Obtains an instance of {@code Duration} representing the passed number of milliseconds.
     *
     * @param milliseconds the number of milliseconds, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration milliseconds(long milliseconds) {
        return ofMilliseconds(milliseconds);
    }

    /**
     * Obtains an instance of {@code Duration} representing the passed number of seconds.
     *
     * @param seconds the number of seconds, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration seconds(long seconds) {
        return ofSeconds(seconds);
    }

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
     *
     * @param d1 a duration to add, could be {@code null}
     * @param d2 another duration to add, could be {@code null}
     * @return <ul>
     * <li>sum of two durations if both of them are {@code non-null}
     * <li>another {@code non-null} value, if one is {@code null}
     * <li>{@link #ZERO} if both values are {@code null}
     * </ul>
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
        final Duration result = TimeUtil.add(d1, d2);
        return result;
    }

    /**
     * Subtract a duration from another.
     */
    public static Duration subtract(Duration d1, Duration d2) {
        /* The sole purpose of this method is minimize the dependencies of the classes
           working with durations. */
        final Duration result = TimeUtil.subtract(d1, d2);
        return result;
    }

    /**
     * This method allows for more compact code of creation of {@code Duration} instance with hours and minutes.
     */
    public static Duration hoursAndMinutes(long hours, long minutes) {
        final Duration result = add(hours(hours), minutes(minutes));
        return result;
    }

    /**
     * Convert a duration to the number of nanoseconds.
     */
    public static long toNanos(Duration duration) {
        /* The sole purpose of this method is minimize the dependencies of the classes
           working with durations. */
        final long result = TimeUtil.toNanos(duration);
        return result;
    }

    /**
     * Convert a duration to the number of seconds.
     */
    public static long toSeconds(Duration duration) {
        final long millis = TimeUtil.toMillis(duration);
        final long seconds = floorDiv(millis, MILLIS_PER_SECOND);
        return seconds;
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
        final int result = Long.valueOf(remainder)
                               .intValue();
        return result;
    }

    /**
     * @return {@code true} of the passed value is greater or equal zero, {@code false} otherwise
     */
    public static boolean isPositiveOrZero(Duration value) {
        final long millis = toMillis(value);
        final boolean result = millis >= 0;
        return result;
    }

    /**
     * @return {@code true} if the passed value is greater than zero, {@code false} otherwise
     */
    public static boolean isPositive(DurationOrBuilder value) {
        final boolean secondsPositive = value.getSeconds() > 0;
        final boolean nanosPositive = value.getNanos() > 0;
        final boolean result = secondsPositive || nanosPositive;
        return result;

    }

    /**
     * @return {@code true} if the passed value is zero, {@code false} otherwise
     */
    public static boolean isZero(DurationOrBuilder value) {
        final boolean noSeconds = value.getSeconds() == 0;
        final boolean noNanos = value.getNanos() == 0;
        final boolean result = noSeconds && noNanos;
        return result;
    }

    /**
     * @return {@code true} if the first argument is greater than the second, {@code false} otherwise
     */
    public static boolean isGreaterThan(Duration value, Duration another) {
        final long nanos = toNanos(value);
        final long anotherNanos = toNanos(another);
        final boolean isGreater = nanos > anotherNanos;
        return isGreater;
    }

    /**
     * @return {@code true} if the first argument is less than the second, {@code false} otherwise
     */
    public static boolean isLessThan(Duration value, Duration another) {
        final long nanos = toNanos(value);
        final long anotherNanos = toNanos(another);
        final boolean isLessThan = nanos < anotherNanos;
        return isLessThan;
    }

    /**
     * Numerically compare passed durations as nanosecond values.
     */
    public static int compare(Duration d1, Duration d2) {
        final long nanos = toNanos(d1);
        final long otherNanos = toNanos(d2);
        final int result = Long.compare(nanos, otherNanos);
        return result;
    }

    /**
     * @return {@code true} if the passed duration is negative, {@code false} otherwise
     */
    public static boolean isNegative(Duration value) {
        final long nanos = toNanos(value);
        final boolean isNegative = nanos < 0;
        return isNegative;
    }
}
