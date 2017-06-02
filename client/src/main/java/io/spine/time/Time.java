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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Timestamps.fromMillis;
import static io.spine.validate.Validate.checkPositive;
import static java.lang.String.format;

/**
 * Utilities for working with time information.
 *
 * @author Alexander Yevsyukov
 */
public class Time {

    /** The count of nanoseconds in one second. */
    public static final int NANOS_PER_SECOND = 1_000_000_000;

    /** The count of nanoseconds in one millisecond. */
    public static final int NANOS_PER_MILLISECOND = 1_000_000;

    /** The count of milliseconds in one second. */
    public static final int MILLIS_PER_SECOND = 1000;

    /** The count of nanoseconds in a microsecond. */
    public static final int NANOS_PER_MICROSECOND = 1000;

    /** The count of microseconds in one second. */
    public static final int MICROS_PER_SECOND = 1_000_000;

    /** The count of seconds in one minute. */
    public static final int SECONDS_PER_MINUTE = 60;

    /** The count of seconds in one minute. */
    public static final int SECONDS_PER_HOUR = 3600;

    /** The count of minutes in one hour. */
    public static final int MINUTES_PER_HOUR = 60;

    /** The count of hours per day. */
    public static final int HOURS_PER_DAY = 24;

    private static final int FEBRUARY_MIN = 28;

    private static final ThreadLocal<Provider> timeProvider = new ThreadLocal<Provider>() {
        @Override
        protected Provider initialValue() {
            return new SystemTimeProvider();
        }
    };

    private Time() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains a number of days in the passed month of the year.
     */
    public static int daysInMonth(int year, MonthOfYear month) {
        final int monthNumber = month.getNumber();
        final int days;
        if (isLeapYear(year) && monthNumber == 2){
            return FEBRUARY_MIN + 1;
        }
        days = FEBRUARY_MIN + ((0x3bbeecc >> (monthNumber * 2)) & 3);
        return days;
    }

    /**
     * Ensures that the passed date is valid.
     *
     * @throws IllegalArgumentException if
     * <ul>
     *     <li>the year is less or equal zero,
     *     <li>the month is {@code UNDEFINED},
     *     <li>the day is less or equal zero or greater than can be in the month.
     * </ul>
     */
    public static void checkDate(int year, MonthOfYear month, int day) {
        checkPositive(year);
        checkNotNull(month);
        checkPositive(month.getNumber());
        checkPositive(day);

        final int daysInMonth = daysInMonth(year, month);

        if (day > daysInMonth) {
            final String errMsg = format(
                    "A number of days cannot be more than %d, for this month and year.",
                    daysInMonth);
            throw new IllegalArgumentException(errMsg);
        }
    }

    /**
     * Tests whether the passed year is a leap one.
     *
     * @return {@code true} for a leap year, {@code false} otherwise
     */
    @SuppressWarnings("MagicNumber") // The number is part of leap year calc.
    public static boolean isLeapYear(int year) {
        return year % 4 == 0 && year % 100 != 0 || year % 400 == 0;
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
     * The provider of the current time.
     *
     * <p>Implement this interface and pass the resulting class to
     */
    @Internal
    public interface Provider {
        Timestamp getCurrentTime();
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
}
