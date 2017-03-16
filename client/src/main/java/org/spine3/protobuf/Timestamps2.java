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
package org.spine3.protobuf;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Timestamp;
import com.google.protobuf.TimestampOrBuilder;
import org.spine3.annotations.Internal;

import javax.annotation.Nullable;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Timestamps.fromMillis;

/**
 * Utilities class for working with {@link Timestamp}s in addition to those available from
 * {@link com.google.protobuf.util.Timestamps Timestamps} class from Protobuf.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class Timestamps2 {

    /**
     * The following constants are taken from the
     * {@link com.google.protobuf.util.Timestamps Timestamps} class
     * in order to make them publicly visible to time management utils:
     * <ul>
     *   <li>{@link #NANOS_PER_SECOND}
     *   <li>{@link #NANOS_PER_MILLISECOND}
     *   <li>{@link #NANOS_PER_MICROSECOND}
     *   <li>{@link #MILLIS_PER_SECOND}
     *   <li>{@link #MICROS_PER_SECOND}
     * </ul>
     * Consider removing these constants if they become public in the Protobuf utils API.
     **/

    /** The count of nanoseconds in one second. */
    public static final long NANOS_PER_SECOND = 1_000_000_000L;

    /** The count of nanoseconds in one millisecond. */
    public static final long NANOS_PER_MILLISECOND = 1_000_000L;

    /** The count of milliseconds in one second. */
    public static final long MILLIS_PER_SECOND = 1000L;

    /** The count of nanoseconds in a microsecond. */
    public static final long NANOS_PER_MICROSECOND = 1000L;

    /** The count of microseconds in one second. */
    public static final long MICROS_PER_SECOND = 1_000_000L;

    /** The count of seconds in one minute. */
    public static final int SECONDS_PER_MINUTE = 60;

    /** The count of seconds in one minute. */
    public static final int SECONDS_PER_HOUR = 3600;

    /** The count of minutes in one hour. */
    public static final int MINUTES_PER_HOUR = 60;

    /** The count of hours per day. */
    public static final int HOURS_PER_DAY = 24;

    private static final ThreadLocal<Provider> timeProvider = new ThreadLocal<Provider>() {
        @SuppressWarnings("RefusedBequest") // We want to provide our default value.
        @Override
        protected Provider initialValue() {
            return new SystemTimeProvider();
        }
    };

    private Timestamps2() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains current time.
     *
     * @return current time
     */
    public static Timestamp getCurrentTime() {
        final Timestamp result = timeProvider.get()
                                             .getCurrentTime();
        return result;
    }

    /**
     * Obtains system time.
     *
     * <p>Unlike {@link #getCurrentTime()} this method <strong>always</strong> uses
     * system time millis.
     *
     * @return current system time
     */
    public static Timestamp systemTime() {
        return fromMillis(System.currentTimeMillis());
    }

    /**
     * The provider of the current time.
     *
     * <p>Implement this interface and pass the resulting class to
     */
    @Internal
    public interface Provider {
        Timestamp getCurrentTime();
    }

    /**
     * Sets provider of the current time.
     *
     * <p>The most common scenario for using this method is test cases of code that deals
     * with current time.
     *
     * @param provider the provider to set
     */
    @Internal
    @VisibleForTesting
    public static void setProvider(Provider provider) {
        timeProvider.set(checkNotNull(provider));
    }

    /**
     * Sets the default current time provider that obtains current time from system millis.
     */
    public static void resetProvider() {
        timeProvider.set(new SystemTimeProvider());
    }

    /**
     * Default implementation of current time provider based on {@link System#currentTimeMillis()}.
     *
     * <p>This is the only place, which should invoke obtaining current time from the system millis.
     */
    private static class SystemTimeProvider implements Provider {
        @Override
        public Timestamp getCurrentTime() {
            final Timestamp result = fromMillis(System.currentTimeMillis());
            return result;
        }
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
    public static Date convertToDate(TimestampOrBuilder timestamp) {
        final long millisecsFromNanos = timestamp.getNanos() / NANOS_PER_MILLISECOND;
        final long millisecsFromSeconds = timestamp.getSeconds() * MILLIS_PER_SECOND;
        final Date date = new Date(millisecsFromSeconds + millisecsFromNanos);
        return date;
    }
}
