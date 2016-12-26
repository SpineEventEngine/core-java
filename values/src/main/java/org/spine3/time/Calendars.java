/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

/**
 * Routines for working with {@link Calendar}.
 *
 * @author Alexander Aleksandrov
 */
public class Calendars {

    private Calendars() {
    }

    /**
     * Obtains zone offset using {@code Calendar}.
     */
    public static int getZoneOffset(Calendar cal) {
        final int zoneOffset = cal.get(Calendar.ZONE_OFFSET) / 1000;
        return zoneOffset;
    }

    /**
     * Obtains year using {@code Calendar}.
     */
    public static int getYear(Calendar cal) {
        final int year = cal.get(Calendar.YEAR);
        return year;
    }

    /**
     * Obtains month using {@code Calendar}.
     */
    public static int getMonth(Calendar cal) {
        // The Calendar class assumes JANUARY is zero. Therefore add 1 to get the reasonable value of month
        final int month = cal.get(Calendar.MONTH) + 1;
        return month;
    }

    /**
     * Obtains day of month using {@code Calendar}.
     */
    public static int getDay(Calendar cal) {
        final int result = cal.get(Calendar.DAY_OF_MONTH);
        return result;
    }

    /**
     * Obtains hours using {@code Calendar}.
     */
    public static int getHours(Calendar cal) {
        final int hours = cal.get(Calendar.HOUR);
        return hours;
    }

    /**
     * Obtains minutes using {@code Calendar}.
     */
    public static int getMinutes(Calendar cal) {
        final int minutes = cal.get(Calendar.MINUTE);
        return minutes;
    }

    /**
     * Obtains seconds using {@code Calendar}.
     */
    public static int getSeconds(Calendar cal) {
        final int seconds = cal.get(Calendar.SECOND);
        return seconds;
    }

    /**
     * Obtains milliseconds using {@code Calendar}.
     */
    public static int getMillis(Calendar cal) {
        final int millis = cal.get(Calendar.MILLISECOND);
        return millis;
    }

    /**
     * Obtains calendar from year, month and day values.
     */
    public static Calendar createDate(int year, int month, int day) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day);
        return calendar;
    }

    /**
     * Obtains calendar from hours, minutes, seconds and milliseconds values.
     */
    public static Calendar createTime(int hours, int minutes, int seconds, int millis) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, millis);
        return calendar;
    }

    /**
     * Obtains calendar from hours, minutes and seconds values.
     */
    public static Calendar createTime(int hours, int minutes, int seconds) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        return calendar;
    }

    /**
     * Obtains calendar from hours and minutes values.
     */
    public static Calendar createTime(int hours, int minutes) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, hours);
        calendar.set(Calendar.MINUTE, minutes);
        return calendar;
    }

    /**
     * Obtains calendar using zone offset in seconds.
     */
    public static Calendar createDateWithZoneOffset(ZoneOffset zoneOffset) {

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -zoneOffset.getAmountSeconds());
        calendar.set(Calendar.ZONE_OFFSET, zoneOffset.getAmountSeconds() * 1000);

        return calendar;
    }

    /**
     * Obtains calendar time using {@code ZoneOffset}.
     */
    public static Calendar createTimeWithZoneOffset(ZoneOffset zoneOffset) {
        final Calendar calendar = createTime();
        final int currentZoneOffset = getZoneOffset(calendar);
        calendar.add(Calendar.SECOND, -currentZoneOffset);
        calendar.add(Calendar.SECOND, zoneOffset.getAmountSeconds());
        calendar.set(Calendar.ZONE_OFFSET, zoneOffset.getAmountSeconds() * 1000);

        return calendar;
    }

    /**
     * Obtains calendar with GMT 00:00 zone offset.
     */
    public static Calendar createDateWithNoOffset() {
        final Calendar calendar = Calendar.getInstance();
        final int zoneOffset = getZoneOffset(calendar);
        calendar.add(Calendar.SECOND, -zoneOffset);
        calendar.set(Calendar.ZONE_OFFSET, 0);

        return calendar;
    }

    /**
     * Obtains current calendar.
     */
    public static Calendar createTime() {
        final Calendar calendar = Calendar.getInstance();
        return calendar;
    }
}
