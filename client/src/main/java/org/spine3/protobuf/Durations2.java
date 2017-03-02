/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.protobuf;

import com.google.protobuf.Duration;
import com.google.protobuf.DurationOrBuilder;
import com.google.protobuf.util.Durations;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Durations.fromMillis;
import static com.google.protobuf.util.Durations.fromNanos;
import static com.google.protobuf.util.Durations.fromSeconds;
import static com.google.protobuf.util.Durations.toMillis;
import static org.spine3.protobuf.Timestamps2.MILLIS_PER_SECOND;
import static org.spine3.protobuf.Timestamps2.MINUTES_PER_HOUR;
import static org.spine3.protobuf.Timestamps2.SECONDS_PER_MINUTE;
import static org.spine3.util.Math.floorDiv;
import static org.spine3.util.Math.safeMultiply;

/**
 * Utility class for working with durations in addition to those available from the
 * {@link com.google.protobuf.util.Durations Durations} class in the Protobuf library.
 *
 * <p>Use {@code import static org.spine3.protobuf.Durations2.*} for compact initialization
 * like this:
 * <pre>
 *      Duration d = add(hours(2), minutes(30));
 * </pre>
 *
 * @author Alexander Yevsyukov
 * @see com.google.protobuf.util.Durations Durations
 */
@SuppressWarnings({"UtilityClass", "ClassWithTooManyMethods"})
public class Durations2 {

    public static final Duration ZERO = fromMillis(0L);

    private Durations2() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains an instance of {@code Duration} representing the passed number of minutes.
     *
     * @param minutes the number of minutes, positive or negative.
     * @return a non-null {@code Duration}
     */
    public static Duration fromMinutes(long minutes) {
        final Duration duration = fromSeconds(safeMultiply(minutes, SECONDS_PER_MINUTE));
        return duration;
    }

    /**
     * Obtains an instance of {@code Duration} representing the passed number of hours.
     *
     * @param hours the number of hours, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration fromHours(long hours) {
        final Duration duration = fromMinutes(safeMultiply(hours, MINUTES_PER_HOUR));
        return duration;
    }

    /*
     * Methods for brief computations with Durations like
     *       add(hours(2), minutes(30));
     ******************************************************/

    /**
     * Obtains an instance of {@code Duration} representing the passed number of nanoseconds.
     *
     * @param nanos the number of nanoseconds, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration nanos(long nanos) {
        final Duration duration = fromNanos(nanos);
        return duration;
    }

    /**
     * Obtains an instance of {@code Duration} representing the passed number of milliseconds.
     *
     * @param milliseconds the number of milliseconds, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration milliseconds(long milliseconds) {
        return fromMillis(milliseconds);
    }

    /**
     * Obtains an instance of {@code Duration} representing the passed number of seconds.
     *
     * @param seconds the number of seconds, positive or negative
     * @return a non-null {@code Duration}
     */
    public static Duration seconds(long seconds) {
        return fromSeconds(seconds);
    }

    /** This method allows for more compact code of creation of
     * {@code Duration} instance with minutes. */
    public static Duration minutes(long minutes) {
        return fromMinutes(minutes);
    }

    /** This method allows for more compact code of creation of
     * {@code Duration} instance with hours. */
    public static Duration hours(long hours) {
        return fromHours(hours);
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
        final Duration result = Durations.add(d1, d2);
        return result;
    }

    /** This method allows for more compact code of creation of
     * {@code Duration} instance with hours and minutes. */
    public static Duration hoursAndMinutes(long hours, long minutes) {
        final Duration result = add(hours(hours), minutes(minutes));
        return result;
    }

    /** Convert a duration to the number of nanoseconds. */
    public static long toNanos(Duration duration) {
        /* The sole purpose of this method is minimize the dependencies of the classes
           working with durations. */
        checkNotNull(duration);
        final long result = Durations.toNanos(duration);
        return result;
    }

    /** Convert a duration to the number of seconds. */
    public static long toSeconds(Duration duration) {
        checkNotNull(duration);
        final long millis = toMillis(duration);
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
        checkNotNull(duration);
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
        checkNotNull(value);
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
        checkNotNull(value);
        final long allMinutes = toMinutes(value);
        final long remainder = allMinutes % MINUTES_PER_HOUR;
        final int result = Long.valueOf(remainder)
                               .intValue();
        return result;
    }

    /** Returns {@code true} of the passed value is greater or equal zero,
     * {@code false} otherwise. */
    public static boolean isPositiveOrZero(Duration value) {
        checkNotNull(value);
        final long millis = toMillis(value);
        final boolean result = millis >= 0;
        return result;
    }

    /** Returns {@code true} if the passed value is greater than zero,
     * {@code false} otherwise. */
    public static boolean isPositive(DurationOrBuilder value) {
        checkNotNull(value);
        final boolean secondsPositive = value.getSeconds() > 0;
        final boolean nanosPositive = value.getNanos() > 0;
        final boolean result = secondsPositive || nanosPositive;
        return result;

    }

    /** Returns {@code true} if the passed value is zero, {@code false} otherwise. */
    public static boolean isZero(DurationOrBuilder value) {
        checkNotNull(value);
        final boolean noSeconds = value.getSeconds() == 0;
        final boolean noNanos = value.getNanos() == 0;
        final boolean result = noSeconds && noNanos;
        return result;
    }

    /** Returns {@code true} if the first argument is greater than the second,
     * {@code false} otherwise. */
    public static boolean isGreaterThan(Duration value, Duration another) {
        final boolean result = compare(value, another) > 0;
        return result;
    }

    /** Returns {@code true} if the first argument is less than the second,
     * {@code false} otherwise. */
    public static boolean isLessThan(Duration value, Duration another) {
        final boolean result = compare(value, another) < 0;
        return result;
    }

    /** Numerically compare passed durations as nanosecond values. */
    public static int compare(Duration d1, Duration d2) {
        return Durations.comparator().compare(d1, d2);
    }

    /** Returns {@code true} if the passed duration is negative, {@code false} otherwise. */
    public static boolean isNegative(Duration value) {
        checkNotNull(value);
        final long nanos = toNanos(value);
        final boolean isNegative = nanos < 0;
        return isNegative;
    }
}
