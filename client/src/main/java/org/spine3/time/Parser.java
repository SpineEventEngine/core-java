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
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static java.lang.String.format;
import static org.spine3.time.Formats.MINUS;
import static org.spine3.time.Formats.PLUS;
import static org.spine3.time.Formats.SUB_SECOND_SEPARATOR;
import static org.spine3.time.Formats.UTC_ZONE_SIGN;
import static org.spine3.time.Formats.dateTimeFormat;
import static org.spine3.time.Formats.timeFormat;
import static org.spine3.time.Timestamps2.MILLIS_PER_SECOND;
import static org.spine3.time.Timestamps2.NANOS_PER_MILLISECOND;

/**
 * The parser for date/time values.
 *
 * <p>The code of this class is based on {@code com.google.protobuf.Timestamps}.
 * This class and the code which uses it should be re-worked after the framework is migrated to
 * Java 8 where new date/time routines are available from {@code java.time} package.
 *
 * @author Alexander Yevsyukov
 */
final class Parser {

    private final String value;

    private int dayOffset = -1;
    private int timezoneOffsetPosition = -1;
    private String secondValue;
    private String nanoValue;

    private long seconds;
    private long nanos;
    private ZoneOffset zoneOffset;

    Parser(String value) {
        this.value = value;
    }

    static OffsetDateTime parserOffsetDateTime(String value) throws ParseException {
        final Parser parser = new Parser(value);
        final OffsetDateTime result = parser.parseOffsetDateTime();
        return result;
    }

    OffsetDateTime parseOffsetDateTime() throws ParseException {
        initDayOffset();
        initTimezoneOffsetPosition();
        initTimeValues();

        parseTime(dateTimeFormat());
        parseZoneOffset();

        final Calendar calendar = createCalendar();
        final LocalDate localDate = Calendars.toLocalDate(calendar);
        final LocalTime localTime = Calendars.toLocalTime(calendar);
        final OffsetDateTime result = OffsetDateTimes.of(localDate, localTime, zoneOffset);
        return result;
    }

    LocalTime parseLocalTime() throws ParseException {
        // The input string for local time does not have zone offset.
        timezoneOffsetPosition = value.length() - 1;
        initTimeValues();
        parseTime(timeFormat());
        zoneOffset = ZoneOffsets.getDefault();
        Calendar calendar = createCalendar();
        final LocalTime localTime = Calendars.toLocalTime(calendar);
        //TODO:2017-04-19:alexander.yevsyukov: Set nanos.
        return localTime;
    }

    private Calendar createCalendar() {
        final Calendar calendar = Calendars.at(zoneOffset);
        final long millis = seconds * MILLIS_PER_SECOND + nanos / NANOS_PER_MILLISECOND;
        final Date date = new Date(millis);
        calendar.setTime(date);
        return calendar;
    }

    private void parseZoneOffset() throws ParseException {
        if (value.charAt(timezoneOffsetPosition) == UTC_ZONE_SIGN) {
            if (value.length() != timezoneOffsetPosition + 1) {
                final String errMsg = format(
                        "Failed to parse date/time value: missing zone offset info \"%s\"",
                        value.substring(timezoneOffsetPosition)
                );
                throw new ParseException(errMsg, 0);
            }
            zoneOffset = ZoneOffsets.UTC;
        } else {
            final String offsetValue = value.substring(timezoneOffsetPosition);
            zoneOffset = ZoneOffsets.parse(offsetValue);
            final int offsetSeconds = ZoneOffsets.parse(offsetValue)
                                                 .getAmountSeconds();
            if (value.charAt(timezoneOffsetPosition) == '+') {
                seconds -= offsetSeconds;
            } else {
                seconds += offsetSeconds;
            }
        }
    }

    private void initTimeValues() {
        // Parse seconds and nanos.
        final String timeSubstring = value.substring(0, timezoneOffsetPosition);
        secondValue = timeSubstring;
        nanoValue = "";
        int pointPosition = timeSubstring.indexOf(SUB_SECOND_SEPARATOR);
        if (pointPosition != -1) {
            secondValue = timeSubstring.substring(0, pointPosition);
            nanoValue = timeSubstring.substring(pointPosition + 1);
        }
    }

    private void initDayOffset() throws ParseException {
        dayOffset = value.indexOf(Formats.TIME_SEPARATOR);
        if (dayOffset == -1) {
            final String errMsg = format(
                    "Failed to parse date/time value: missing time separator in: \"%s\"",
                    value
            );
            throw new ParseException(errMsg, 0);
        }
    }

    private void initTimezoneOffsetPosition() throws ParseException {
        timezoneOffsetPosition = value.indexOf(UTC_ZONE_SIGN, dayOffset);
        if (timezoneOffsetPosition == -1) {
            timezoneOffsetPosition = value.indexOf(PLUS, dayOffset);
        }
        if (timezoneOffsetPosition == -1) {
            timezoneOffsetPosition = value.indexOf(MINUS, dayOffset);
        }
        if (timezoneOffsetPosition == -1) {
            final String errMsg = format(
                    "Failed to parse date/time value: missing timezone in: \"%s\"", value
            );
            throw new ParseException(errMsg, 0);
        }
    }

    private void parseTime(DateFormat format) throws ParseException {
        final Date date = format.parse(secondValue);
        seconds = date.getTime() / MILLIS_PER_SECOND;
        nanos = nanoValue.isEmpty() ? 0 : parseNanos(nanoValue);
    }

    @SuppressWarnings("CharUsedInArithmeticContext") // OK for this parsing method.
    static int parseNanos(String value) throws ParseException {
        int result = 0;
        for (int i = 0; i < 9; ++i) {
            result = result * 10;
            if (i < value.length()) {
                if (value.charAt(i) < '0' || value.charAt(i) > '9') {
                    final String errMsg = format("Invalid nanoseconds in: \"%s\"", value);
                    throw new ParseException(errMsg, 0);
                }
                result += value.charAt(i) - '0';
            }
        }
        return result;
    }
}
