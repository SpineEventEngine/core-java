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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.time.Time.NANOS_PER_MICROSECOND;
import static io.spine.time.Time.NANOS_PER_MILLISECOND;
import static java.lang.String.format;

/**
 * Utilities and constants for working with date/time formats.
 *
 * @author Alexander Yevsyukov
 */
final class Formats {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String TIME_FORMAT = "HH:mm:ss";
    private static final String HOURS_AND_MINUTES_FORMAT = "%02d:%02d";

    static final char TIME_SEPARATOR = 'T';
    static final char SUB_SECOND_SEPARATOR = '.';
    static final char TIME_VALUE_SEPARATOR = ':';
    static final char PLUS = '+';
    static final char MINUS = '-';
    static final char UTC_ZONE_SIGN = 'Z';

    private static final ThreadLocal<DateFormat> dateTimeFormat =
            new ThreadLocal<DateFormat>() {
                @Override
                protected DateFormat initialValue() {
                    return createDateTimeFormat(TimeZone.getDefault());
                }
            };

    private static final ThreadLocal<DateFormat> timeFormat =
            new ThreadLocal<DateFormat>() {
                @Override
                protected DateFormat initialValue() {
                    return createTimeFormat(TimeZone.getDefault());
                }
            };

    private static final ThreadLocal<DateFormat> dateFormat =
            new ThreadLocal<DateFormat>() {
                @Override
                protected DateFormat initialValue() {
                    return createDateFormat(TimeZone.getDefault());
                }
            };

    static String formatOffsetTime(long hours, long minutes) {
        return format(HOURS_AND_MINUTES_FORMAT, Math.abs(hours),
                      Math.abs(minutes));
    }

    /**
     * Names of arguments in preconditions checks.
     */
    enum Parameter {
        hours,
        minutes,
        seconds,
        millis,
        nanos
    }

    private Formats() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates format for local date.
     */
    private static DateFormat createDateFormat(TimeZone timeZone) {
        final SimpleDateFormat sdf = createDigitOnlyFormat(DATE_FORMAT);
        GregorianCalendar calendar = Calendars.newProlepticGregorianCalendar(timeZone);
        sdf.setCalendar(calendar);
        return sdf;
    }

    /**
     * Creates ISO 8601 date string format.
     */
    private static SimpleDateFormat createDateTimeFormat(TimeZone timeZone) {
        final SimpleDateFormat sdf = createDigitOnlyFormat(DATE_TIME_FORMAT);
        GregorianCalendar calendar = Calendars.newProlepticGregorianCalendar(timeZone);
        sdf.setCalendar(calendar);
        return sdf;
    }

    /**
     * Creates ISO 8601 time string format.
     */
    private static SimpleDateFormat createTimeFormat(TimeZone timeZone) {
        final SimpleDateFormat sdf = createDigitOnlyFormat(TIME_FORMAT);
        GregorianCalendar calendar = Calendars.newProlepticGregorianCalendar(timeZone);
        sdf.setCalendar(calendar);
        return sdf;
    }

    /**
     * Creates a format instance for a format that uses only digits.
     *
     * <p>The digit-only format does not require a locale.
     */
    private static SimpleDateFormat createDigitOnlyFormat(String dateFormat) {
        @SuppressWarnings("SimpleDateFormatWithoutLocale") // See Javadoc.
        final SimpleDateFormat result = new SimpleDateFormat(dateFormat);
        return result;
    }

    //
    // Default time zone formats
    //----------------------------

    static DateFormat dateTimeFormat() {
        return dateTimeFormat.get();
    }

    static DateFormat dateFormat() {
        return dateFormat.get();
    }

    static DateFormat timeFormat() {
        return timeFormat.get();
    }

    // Zoned formats
    //---------------------

    static DateFormat dateTimeFormat(ZoneOffset offset) {
        final TimeZone timeZone = toTimeZone(offset);
        return createDateTimeFormat(timeZone);
    }

    private static TimeZone toTimeZone(ZoneOffset offset) {
        final TimeZone timeZone = ZoneConverter.getInstance()
                                               .reverse()
                                               .convert(offset);
        checkNotNull(timeZone);
        return timeZone;
    }

    static DateFormat dateFormat(ZoneOffset offset) {
        final TimeZone timeZone = toTimeZone(offset);
        return createDateFormat(timeZone);
    }

    static DateFormat timeFormat(ZoneOffset offset) {
        final TimeZone timeZone = toTimeZone(offset);
        return createTimeFormat(timeZone);
    }

    // String generation
    //----------------------

    /**
     * Appends the fractional second part of the time to the passed string builder.
     */
    static void appendSubSecond(StringBuilder builder, LocalTime time) {
        final long nanos = LocalTimes.getTotalNanos(time);
        if (nanos != 0) {
            builder.append(SUB_SECOND_SEPARATOR);
            builder.append(formatNanos(nanos));
        }
    }

    /** Format the nano part of a timestamp or a duration. */
    private static String formatNanos(long nanos) {
        // Determine whether to use 3, 6, or 9 digits for the nano part.
        if (nanos % NANOS_PER_MILLISECOND == 0) {
            return format("%1$03d", nanos / NANOS_PER_MILLISECOND);
        } else if (nanos % NANOS_PER_MICROSECOND == 0) {
            return format("%1$06d", nanos / NANOS_PER_MICROSECOND);
        } else {
            return format("%1$09d", nanos);
        }
    }

    /**
     * Appends the string with zone offset info to the passed string builder.
     */
    static void appendZoneOffset(StringBuilder builder, ZoneOffset offset) {
        if (offset.getAmountSeconds() == 0) {
            builder.append(UTC_ZONE_SIGN);
        } else {
            final String offsetStr = ZoneOffsets.toString(offset);
            builder.append(offsetStr);
        }
    }
}
