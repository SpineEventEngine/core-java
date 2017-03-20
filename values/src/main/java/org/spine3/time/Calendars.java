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

import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR;
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
class Calendars {

    private static final String TIME_ZONE_GMT = "GMT";

    private Calendars() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains zone offset using {@code Calendar}.
     */
    static int getZoneOffset(Calendar cal) {
        final int zoneOffset = cal.get(ZONE_OFFSET) / 1000;
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
        final int hours = cal.get(HOUR);
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
        calendar.set(HOUR, hours);
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
        calendar.set(HOUR, hours);
        calendar.set(MINUTE, minutes);
        calendar.set(SECOND, seconds);
        return calendar;
    }

    /**
     * Obtains {@code Calendar} from hours and minutes values.
     */
    static Calendar createWithTime(int hours, int minutes) {
        final Calendar calendar = getInstance();
        calendar.set(HOUR, hours);
        calendar.set(MINUTE, minutes);
        return calendar;
    }

    /**
     * Obtains a {@code Calendar} with GMT time zone.
     *
     * @return new {@code Calendar} instance
     */
    private static Calendar nowAtGmt() {
        Calendar gmtCal = getInstance(TimeZone.getTimeZone(TIME_ZONE_GMT));
        return gmtCal;
    }

    /**
     * Obtains current time calendar for the passed zone offset.
     */
    static Calendar nowAt(ZoneOffset zoneOffset) {
        final Calendar utc = nowAtGmt();
        final Date timeAtGmt = utc.getTime();
        long msFromEpoch = timeAtGmt.getTime();

        final Calendar calendar = at(zoneOffset);

        // Gives you the current offset in ms from UTC at the current date
        int offsetFromGmt = calendar.getTimeZone()
                                    .getOffset(msFromEpoch);
        calendar.setTime(timeAtGmt);
        calendar.add(MILLISECOND, offsetFromGmt);

        return calendar;
    }

    /**
     * Obtains calendar at the specified zone offset
     *
     * @param zoneOffset time offset for specified zone
     * @return new {@code Calendar} instance at specific zone offset
     */
    static Calendar at(ZoneOffset zoneOffset) {
        @SuppressWarnings("NumericCastThatLosesPrecision") // OK as a valid zoneOffset isn't that big.
        final int offsetMillis = (int) TimeUnit.SECONDS.toMillis(zoneOffset.getAmountSeconds());
        final SimpleTimeZone timeZone = new SimpleTimeZone(offsetMillis, "temp");
        final Calendar result = getInstance(timeZone);
        return result;
    }

    /**
     * Obtains current calendar.
     */
    static Calendar now() {
        final Calendar calendar = getInstance();
        return calendar;
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
        return LocalDates.of(getYear(cal),
                             getMonthOfYear(cal),
                             getDay(cal));
    }

    /**
     * Obtains local time using calendar.
     */
    static LocalTime toLocalTime(Calendar calendar) {
        final int hours = getHours(calendar);
        final int minutes = getMinutes(calendar);
        final int seconds = getSeconds(calendar);
        final int millis = getMillis(calendar);

        return LocalTimes.of(hours, minutes, seconds, millis);
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
}
