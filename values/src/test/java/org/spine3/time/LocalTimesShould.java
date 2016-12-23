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

import org.junit.Test;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;
import static org.spine3.time.Calendars.createTime;
import static org.spine3.time.Calendars.createTimeInMillis;
import static org.spine3.time.Calendars.getHours;
import static org.spine3.time.Calendars.getMillis;
import static org.spine3.time.Calendars.getMinutes;
import static org.spine3.time.Calendars.getSeconds;

@SuppressWarnings("InstanceMethodNamingConvention")
public class LocalTimesShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(LocalDates.class));
    }

    @Test
    public void obtain_current_LocalTime() {
        final LocalTime now = LocalTimes.now();
        final Calendar cal = createTimeInMillis();

        assertEquals(getHours(cal), now.getHours());
        assertEquals(getMinutes(cal), now.getMinutes());
        assertEquals(getSeconds(cal), now.getSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes() {
        final int hours = 5;
        final int minutes = 30;
        final LocalTime localTime = LocalTimes.of(hours, minutes);
        final Calendar cal = createTime(hours, minutes);

        assertEquals(getHours(cal), localTime.getHours());
        assertEquals(getMinutes(cal), localTime.getMinutes());
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes_seconds() {
        final int hours = 5;
        final int minutes = 31;
        final int seconds = 25;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds);
        final Calendar cal = createTime(hours, minutes, seconds);

        assertEquals(getHours(cal), localTime.getHours());
        assertEquals(getMinutes(cal), localTime.getMinutes());
        assertEquals(getSeconds(cal), localTime.getSeconds());
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes_seconds_millis() {
        final int hours = 5;
        final int minutes = 31;
        final int seconds = 25;
        final int millis = 125;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis);
        final Calendar cal = createTime(hours, minutes, seconds, millis);

        assertEquals(getHours(cal), localTime.getHours());
        assertEquals(getMinutes(cal), localTime.getMinutes());
        assertEquals(getSeconds(cal), localTime.getSeconds());
        assertEquals(getMillis(cal), localTime.getMillis());
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes_seconds_millis_nanos() {
        final int hours = 5;
        final int minutes = 31;
        final int seconds = 25;
        final int millis = 125;
        final long nanos = 2356L;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, 2356L);
        final Calendar cal = createTime(hours, minutes, seconds, millis);

        assertEquals(getHours(cal), localTime.getHours());
        assertEquals(getMinutes(cal), localTime.getMinutes());
        assertEquals(getSeconds(cal), localTime.getSeconds());
        assertEquals(getMillis(cal), localTime.getMillis());
        assertEquals(nanos, localTime.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_hours() {
        final int hoursToAdd = 2;
        final LocalTime now = LocalTimes.now();
        final LocalTime inFewHours = LocalTimes.plusHours(now, hoursToAdd);
        final Calendar cal = createTimeInMillis();
        cal.add(Calendar.HOUR, hoursToAdd);
        
        assertEquals(getHours(cal), inFewHours.getHours());
        assertEquals(getMinutes(cal), inFewHours.getMinutes());
        assertEquals(getSeconds(cal), inFewHours.getSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_minutes() {
        final int minutesToAdd = 25;
        final LocalTime now = LocalTimes.now();
        final LocalTime inFewMinutes = LocalTimes.plusMinutes(now, minutesToAdd);
        final Calendar cal = createTimeInMillis();
        cal.add(Calendar.MINUTE, minutesToAdd);

        assertEquals(getHours(cal), inFewMinutes.getHours());
        assertEquals(getMinutes(cal), inFewMinutes.getMinutes());
        assertEquals(getSeconds(cal), inFewMinutes.getSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_seconds() {
        final int secondsToAdd = 28;
        final LocalTime now = LocalTimes.now();
        final LocalTime inFewSeconds = LocalTimes.plusSeconds(now, secondsToAdd);
        final Calendar cal = createTimeInMillis();
        cal.add(Calendar.SECOND, secondsToAdd);

        assertEquals(getHours(cal), inFewSeconds.getHours());
        assertEquals(getMinutes(cal), inFewSeconds.getMinutes());
        assertEquals(getSeconds(cal), inFewSeconds.getSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_millis() {
        final int millisToAdd = 288;
        final LocalTime now = LocalTimes.now();
        final int millisNow = now.getMillis();
        final Calendar cal = createTimeInMillis();
        final LocalTime inFewmillis = LocalTimes.plusMillis(now, millisToAdd);

        assertEquals(getHours(cal), inFewmillis.getHours());
        assertEquals(getMinutes(cal), inFewmillis.getMinutes());
        assertEquals(getSeconds(cal), inFewmillis.getSeconds());
        assertEquals(millisNow + millisToAdd, inFewmillis.getMillis());
        /* We cannot check nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_hours() {
        final int hoursToSubtract = 2;
        final LocalTime now = LocalTimes.now();
        final LocalTime beforeFewHours = LocalTimes.minusHours(now, hoursToSubtract);
        final Calendar cal = createTimeInMillis();
        cal.add(Calendar.HOUR, hoursToSubtract);

        assertEquals(getHours(cal), beforeFewHours.getHours());
        assertEquals(getMinutes(cal), beforeFewHours.getMinutes());
        assertEquals(getSeconds(cal), beforeFewHours.getSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_minutes() {
        final int minutesToSubtract = 25;
        final LocalTime now = LocalTimes.now();
        final LocalTime beforeFewMinutes = LocalTimes.minusMinutes(now, minutesToSubtract);
        final Calendar cal = createTimeInMillis();
        cal.add(Calendar.MINUTE, minutesToSubtract);

        assertEquals(getHours(cal), beforeFewMinutes.getHours());
        assertEquals(getMinutes(cal), beforeFewMinutes.getMinutes());
        assertEquals(getSeconds(cal), beforeFewMinutes.getSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_seconds() {
        final int secondsToSubtract = 28;
        final LocalTime now = LocalTimes.now();
        final LocalTime beforeFewSeconds = LocalTimes.minusSeconds(now, secondsToSubtract);
        final Calendar cal = createTimeInMillis();
        cal.add(Calendar.SECOND, secondsToSubtract);

        assertEquals(getHours(cal), beforeFewSeconds.getHours());
        assertEquals(getMinutes(cal), beforeFewSeconds.getMinutes());
        assertEquals(getSeconds(cal), beforeFewSeconds.getSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_millis() {
        final int millisToSubtract = 288;
        final LocalTime now = LocalTimes.now();
        final int millisNow = now.getMillis();
        final Calendar cal = createTimeInMillis();
        final LocalTime beforeFewmillis = LocalTimes.minusMillis(now, millisToSubtract);

        assertEquals(getHours(cal), beforeFewmillis.getHours());
        assertEquals(getMinutes(cal), beforeFewmillis.getMinutes());
        assertEquals(getSeconds(cal), beforeFewmillis.getSeconds());
        assertEquals(millisNow + millisToSubtract, beforeFewmillis.getMillis());
        /* We cannot check nanos due to time gap between object creation */
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalTime_value_with_hoursToAdd() {
        final int hoursToAdd = -5;
        final LocalTime now = null;
        LocalTimes.plusHours(now, hoursToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalTime_value_with_minutesToAdd() {
        final int minutesToAdd = 7;
        final LocalTime now = null;
        LocalTimes.plusMinutes(now, minutesToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalTime_value_with_secondsToAdd() {
        final int secondsToAdd = 25;
        final LocalTime now = null;
        LocalTimes.plusSeconds(now, secondsToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalTime_value_with_millisToAdd() {
        final int millisToAdd = 205;
        final LocalTime now = null;
        LocalTimes.plusMillis(now, millisToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalTime_value_with_hoursToSubtract() {
        final int hoursToSubtract = 6;
        final LocalTime now = null;
        LocalTimes.minusHours(now, hoursToSubtract);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalTime_value_with_minutesToSubtract() {
        final int minutesToSubtract = 8;
        final LocalTime now = null;
        LocalTimes.minusMinutes(now, minutesToSubtract);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalTime_value_with_secondsToSubtract() {
        final int secondsToSubtract = 27;
        final LocalTime now = null;
        LocalTimes.minusSeconds(now, secondsToSubtract);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_LocalTime_value_with_millisToSubtract() {
        final int millisToSubtract = 245;
        final LocalTime now = null;
        LocalTimes.minusMillis(now, millisToSubtract);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_hours() {
        final int hours = -2;
        final int minutes = 20;
        LocalTimes.of(hours, minutes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_minutes() {
        final int hours = 2;
        final int minutes = -20;
        LocalTimes.of(hours, minutes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_seconds() {
        final int hours = 2;
        final int minutes = 20;
        final int seconds = -50;
        LocalTimes.of(hours, minutes, seconds);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_millis() {
        final int hours = 2;
        final int minutes = 20;
        final int seconds = 50;
        final int millis = -150;
        LocalTimes.of(hours, minutes, seconds, millis);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_nanos() {
        final int hours = 2;
        final int minutes = 20;
        final int seconds = 50;
        final int millis = 150;
        final int nanos = -1501;
        LocalTimes.of(hours, minutes, seconds, millis, nanos);
    }
}
