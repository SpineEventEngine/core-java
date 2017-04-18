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

package org.spine3.time;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import static java.lang.String.format;
import static org.spine3.time.Timestamps2.NANOS_PER_MICROSECOND;
import static org.spine3.time.Timestamps2.NANOS_PER_MILLISECOND;

/**
 * Utilities and constants for working with date/time formats.
 *
 * @author Alexander Yevsyukov
 */
final class Formats {

    static final String DATE_FORMAT = "yyyy-MM-dd";
    static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    static final String HOURS_AND_MINUTES_FORMAT = "%02d:%02d";

    static final char TIME_SEPARATOR = 'T';
    static final char SUB_SECOND_SEPARATOR = '.';
    static final char PLUS = '+';
    static final char MINUS = '-';
    static final char UTC_ZONE_SIGN = 'Z';

    private Formats() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates format for local date.
     */
    static DateFormat createDateFormat() {
        final SimpleDateFormat sdf = createDigitOnlyFormat(DATE_FORMAT);
        GregorianCalendar calendar = Calendars.newProlepticGregorianCalendar();
        sdf.setCalendar(calendar);
        return sdf;
    }

    /**
     * Creates RFC 3339 date string format.
     */
    static SimpleDateFormat createDateTimeFormat() {
        final SimpleDateFormat sdf = createDigitOnlyFormat(DATE_TIME_FORMAT);
        GregorianCalendar calendar = Calendars.newProlepticGregorianCalendar();
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

    /** Format the nano part of a timestamp or a duration. */
    static String formatNanos(long nanos) {
        // Determine whether to use 3, 6, or 9 digits for the nano part.
        if (nanos % NANOS_PER_MILLISECOND == 0) {
            return format("%1$03d", nanos / NANOS_PER_MILLISECOND);
        } else if (nanos % NANOS_PER_MICROSECOND == 0) {
            return format("%1$06d", nanos / NANOS_PER_MICROSECOND);
        } else {
            return format("%1$09d", nanos);
        }
    }
}
