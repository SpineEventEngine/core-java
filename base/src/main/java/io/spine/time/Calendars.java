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

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.time.Time.MILLIS_PER_SECOND;
import static io.spine.validate.Validate.checkPositive;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static java.util.Calendar.ZONE_OFFSET;
import static java.util.Calendar.getInstance;

/**
 * Utilities for working with {@link Calendar}.
 *
 * <p> This utility class is needed while Spine is based on Java 7.
 * Java 8 introduces new date/time API in the package {@code java.time}.
 * Spine v2 will be based on Java 8 and this class will be deprecated.
 *
 * @author Alexander Aleksandrov
 */
@SuppressWarnings({"ClassWithTooManyMethods" /* OK for this utility class. */,
                   "MagicConstant" /* We use conversion methods instead. */})
final class Calendars {

    private Calendars() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains a zone offset of the passed {@code Calendar}.
     *
     * @return zone offset in seconds
     */
    static int getZoneOffset(Calendar cal) {
        final int zoneOffset = cal.get(ZONE_OFFSET) / MILLIS_PER_SECOND;
        return zoneOffset;
    }

    /**
     * Obtains year using {@code Calendar}.
     */
    static int getYear(Calendar cal) {
        final int year = cal.get(YEAR);
        return year;
    }

    /**
     * Obtains month using {@code Calendar}.
     */
    static int getMonth(Calendar cal) {
        // The Calendar class assumes JANUARY is zero.
        // Therefore add 1 to get the reasonable value of month
        final int month = cal.get(MONTH) + 1;
        return month;
    }

    /**
     * Obtains day of month using {@code Calendar}.
     */
    static int getDay(Calendar cal) {
        final int result = cal.get(DAY_OF_MONTH);
        return result;
    }

    /**
     * Obtains hours using {@code Calendar}.
     */
    static int getHours(Calendar cal) {
        final int hours = cal.get(HOUR_OF_DAY);
        return hours;
    }

    /**
     * Obtains minutes using {@code Calendar}.
     */
    static int getMinutes(Calendar cal) {
        final int minutes = cal.get(MINUTE);
        return minutes;
    }

    /**
     * Obtains seconds using {@code Calendar}.
     */
    static int getSeconds(Calendar cal) {
        final int seconds = cal.get(SECOND);
        return seconds;
    }

    /**
     * Obtains milliseconds using {@code Calendar}.
     */
    static int getMillis(Calendar cal) {
        final int millis = cal.get(MILLISECOND);
        return millis;
    }

    /**
     * Obtains calendar from year, month, and day values.
     */
    private static Calendar createWithDate(int year, int month, int day) {
        final Calendar calendar = getInstance();
        calendar.set(year, toMagicMonthNumber(month), day);
        return calendar;
    }

    /**
     * Converts the passed number of a month into a value of a magic constant used
     * by {@link Calendar} API.
     *
     * @param month a one-based number of a month
     */
    private static int toMagicMonthNumber(int month) {
        return month - 1;
    }

    /**
     * Sets the date to calendar.
     *
     * @param calendar the target calendar
     * @param date     the date to set
     */
    private static void setDate(Calendar calendar, LocalDate date) {
        calendar.set(date.getYear(),
                     toMagicMonthNumber(date.getMonth()
                                            .getNumber()),
                     date.getDay());
    }

    /**
     * Obtains {@code Calendar} from hours, minutes, seconds and milliseconds values.
     */
    private static Calendar createWithTime(int hours, int minutes, int seconds, int millis) {
        final Calendar calendar = getInstance();
        calendar.set(HOUR_OF_DAY, hours);
        calendar.set(MINUTE, minutes);
        calendar.set(SECOND, seconds);
        calendar.set(MILLISECOND, millis);
        return calendar;
    }

    /**
     * Obtains {@code Calendar} from hours, minutes and seconds values.
     */
    static Calendar createWithTime(int hours, int minutes, int seconds) {
        final Calendar calendar = getInstance();
        calendar.set(HOUR_OF_DAY, hours);
        calendar.set(MINUTE, minutes);
        calendar.set(SECOND, seconds);
        return calendar;
    }

    /**
     * Obtains {@code Calendar} from hours and minutes values.
     */
    static Calendar createWithTime(int hours, int minutes) {
        final Calendar calendar = getInstance();
        calendar.set(HOUR_OF_DAY, hours);
        calendar.set(MINUTE, minutes);
        return calendar;
    }

    /**
     * Obtains calendar at the specified zone offset
     *
     * @param zoneOffset time offset for specified zone
     * @return new {@code Calendar} instance at specific zone offset
     */
    static Calendar at(ZoneOffset zoneOffset) {
        final TimeZone timeZone = ZoneConverter.getInstance()
                                               .reverse()
                                               .convert(zoneOffset);
        final Calendar result = getInstance(timeZone);
        return result;
    }

    /**
     * Obtains month of year using calendar.
     */
    static MonthOfYear getMonthOfYear(Calendar calendar) {
        // The Calendar class assumes JANUARY is zero.
        // Therefore add 1 to get the value of MonthOfYear.
        final int monthByCalendar = calendar.get(MONTH);
        final MonthOfYear month = MonthOfYear.forNumber(monthByCalendar + 1);
        return month;
    }

    /**
     * Obtains local date using calendar.
     */
    static LocalDate toLocalDate(Calendar cal) {
        final int year = getYear(cal);
        final MonthOfYear month = getMonthOfYear(cal);
        final int day = getDay(cal);
        final LocalDate result = LocalDates.of(year, month, day);
        return result;
    }

    /**
     * Obtains local time using calendar.
     */
    static LocalTime toLocalTime(Calendar calendar) {
        final int nanos = 0;
        return toLocalTime(calendar, nanos);
    }

    /**
     * Obtains local time from Calendar with nanosecond precision.
     */
    static LocalTime toLocalTime(Calendar calendar, int nanos) {
        final int hours = getHours(calendar);
        final int minutes = getMinutes(calendar);
        final int seconds = getSeconds(calendar);
        final int millis = getMillis(calendar);
        return LocalTimes.of(hours, minutes, seconds, millis, nanos);
    }

    /**
     * Converts the passed Timestamp into Calendar at the passed time zone.
     */
    static Calendar toCalendar(Timestamp timestamp, ZoneOffset zoneOffset) {
        final long millis = Timestamps.toMillis(timestamp);
        final Calendar calendar = at(zoneOffset);
        calendar.setTimeInMillis(millis);
        return calendar;
    }

    /**
     * Converts the passed {@code OffsetDate} into {@code Calendar}.
     *
     * <p>The calendar is initialized at the offset from the passed date.
     */
    static Calendar toCalendar(OffsetDate offsetDate) {
        final Calendar calendar = at(offsetDate.getOffset());
        setDate(calendar, offsetDate.getDate());
        return calendar;
    }

    /**
     * Converts the passed {@code LocalTime} into {@code Calendar}.
     */
    static Calendar toCalendar(LocalTime localTime) {
        return createWithTime(localTime.getHours(),
                              localTime.getMinutes(),
                              localTime.getSeconds(),
                              localTime.getMillis());
    }

    /**
     * Converts the passed {@code LocalDate} into {@code Calendar}.
     */
    static Calendar toCalendar(LocalDate localDate) {
        return createWithDate(localDate.getYear(),
                              localDate.getMonthValue(),
                              localDate.getDay());
    }

    /**
     * Converts the passed {@code OffsetTime} into {@code Calendar}.
     */
    static Calendar toCalendar(OffsetTime offsetTime) {
        final LocalTime time = offsetTime.getTime();
        final Calendar calendar = at(offsetTime.getOffset());
        calendar.set(HOUR_OF_DAY, time.getHours());
        calendar.set(MINUTE, time.getMinutes());
        calendar.set(SECOND, time.getSeconds());
        calendar.set(MILLISECOND, time.getMillis());
        return calendar;
    }

    /**
     * Converts the passed {@code OffsetDateTime} into {@code Calendar}.
     */
    static Calendar toCalendar(OffsetDateTime dateTime) {
        final Calendar cal = at(dateTime.getOffset());
        final LocalDate date = dateTime.getDate();
        final LocalTime time = dateTime.getTime();
        cal.set(date.getYear(),
                toMagicMonthNumber(date.getMonth()
                                       .getNumber()),
                date.getDay(),
                time.getHours(),
                time.getMinutes(),
                time.getSeconds());
        cal.set(MILLISECOND, time.getMillis());
        return cal;
    }

    /**
     * Creates a new instance of Proleptic Gregorian Calendar in the default time zone.
     *
     * <p>The created instance is Gregorial calendar which extends to year one.
     */
    static GregorianCalendar newProlepticGregorianCalendar() {
        return newProlepticGregorianCalendar(TimeZone.getDefault());
    }

    /**
     * Creates a new instance of Proleptic Gregorian Calendar.
     *
     * <p>The created instance is Gregorial calendar which extends to year one.
     *
     * @param timeZone the time zone in which the calendar is created
     */
    static GregorianCalendar newProlepticGregorianCalendar(TimeZone timeZone) {
        GregorianCalendar calendar = new GregorianCalendar(timeZone);
        calendar.setGregorianChange(new Date(Long.MIN_VALUE));
        return calendar;
    }

    /**
     * Ensures that the passed value is not null and the delta value is positive.
     */
    static void checkArguments(Object value, int delta) {
        checkNotNull(value);
        checkPositive(delta);
    }
}
