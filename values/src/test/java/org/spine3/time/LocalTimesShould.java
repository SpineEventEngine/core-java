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
import static org.spine3.time.Calendars.createTime;
import static org.spine3.time.Calendars.getHours;
import static org.spine3.time.Calendars.getMillis;
import static org.spine3.time.Calendars.getMinutes;
import static org.spine3.time.Calendars.getSeconds;

@SuppressWarnings("InstanceMethodNamingConvention")
public class LocalTimesShould {

    private static final int hours = 9;
    private static final int minutes = 25;
    private static final int seconds = 30;
    private static final int millis = 124;
    private static final long nanos = 122L;

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(LocalDates.class));
    }

    @Test
    public void obtain_current_LocalTime() {
        final LocalTime now = LocalTimes.now();
        final Calendar cal = Calendars.createTime();

        assertEquals(getHours(cal), now.getHours());
        assertEquals(getMinutes(cal), now.getMinutes());
        assertEquals(getSeconds(cal), now.getSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes() {
        final LocalTime localTime = LocalTimes.of(hours, minutes);

        assertTrue(hours == localTime.getHours());
        assertTrue(minutes == localTime.getMinutes());
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes_seconds() {
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds);

        assertTrue(hours == localTime.getHours());
        assertTrue(minutes == localTime.getMinutes());
        assertTrue(seconds == localTime.getSeconds());
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes_seconds_millis() {
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis);

        assertTrue(hours == localTime.getHours());
        assertTrue(minutes == localTime.getMinutes());
        assertTrue(seconds == localTime.getSeconds());
        assertTrue(millis == localTime.getMillis());
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes_seconds_millis_nanos() {
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);

        assertTrue(hours == localTime.getHours());
        assertTrue(minutes == localTime.getMinutes());
        assertTrue(seconds == localTime.getSeconds());
        assertTrue(millis == localTime.getMillis());
        assertTrue(nanos == localTime.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_hours() {
        final int hoursToAdd = 2;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime inFewHours = LocalTimes.plusHours(localTime, hoursToAdd);

        assertTrue(hours + hoursToAdd == inFewHours.getHours());
        assertTrue(minutes == inFewHours.getMinutes());
        assertTrue(seconds == inFewHours.getSeconds());
        assertTrue(millis == inFewHours.getMillis());
        assertTrue(nanos == inFewHours.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_minutes() {
        final int minutesToAdd = 15;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime inFewMinutes = LocalTimes.plusMinutes(localTime, minutesToAdd);

        assertTrue(hours == inFewMinutes.getHours());
        assertTrue(minutes + minutesToAdd == inFewMinutes.getMinutes());
        assertTrue(seconds == inFewMinutes.getSeconds());
        assertTrue(millis == inFewMinutes.getMillis());
        assertTrue(nanos == inFewMinutes.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_seconds() {
        final int secondsToAdd = 18;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime inFewSeconds = LocalTimes.plusSeconds(localTime, secondsToAdd);

        assertTrue(hours == inFewSeconds.getHours());
        assertTrue(minutes == inFewSeconds.getMinutes());
        assertTrue(seconds + secondsToAdd == inFewSeconds.getSeconds());
        assertTrue(millis == inFewSeconds.getMillis());
        assertTrue(nanos == inFewSeconds.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_millis() {
        final int millisToAdd = 288;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime inFewMillis = LocalTimes.plusMillis(localTime, millisToAdd);

        assertTrue(hours == inFewMillis.getHours());
        assertTrue(minutes == inFewMillis.getMinutes());
        assertTrue(seconds == inFewMillis.getSeconds());
        assertTrue(millis + millisToAdd == inFewMillis.getMillis());
        assertTrue(nanos == inFewMillis.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_hours() {
        final int hoursToSubtract = 2;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime beforeFewHours = LocalTimes.minusHours(localTime, hoursToSubtract);

        assertTrue(hours - hoursToSubtract == beforeFewHours.getHours());
        assertTrue(minutes == beforeFewHours.getMinutes());
        assertTrue(seconds == beforeFewHours.getSeconds());
        assertTrue(millis == beforeFewHours.getMillis());
        assertTrue(nanos == beforeFewHours.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_minutes() {
        final int minutesToSubtract = 15;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime beforeFewMinutes = LocalTimes.minusMinutes(localTime, minutesToSubtract);

        assertTrue(hours == beforeFewMinutes.getHours());
        assertTrue(minutes - minutesToSubtract == beforeFewMinutes.getMinutes());
        assertTrue(seconds == beforeFewMinutes.getSeconds());
        assertTrue(millis == beforeFewMinutes.getMillis());
        assertTrue(nanos == beforeFewMinutes.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_seconds() {
        final int secondsToSubtract = 12;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime beforeFewSeconds = LocalTimes.minusSeconds(localTime, secondsToSubtract);

        assertTrue(hours == beforeFewSeconds.getHours());
        assertTrue(minutes == beforeFewSeconds.getMinutes());
        assertTrue(seconds - secondsToSubtract == beforeFewSeconds.getSeconds());
        assertTrue(millis == beforeFewSeconds.getMillis());
        assertTrue(nanos == beforeFewSeconds.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_millis() {
        final int millisToSubtract = 28;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime beforeFewMillis = LocalTimes.minusMillis(localTime, millisToSubtract);

        assertTrue(hours == beforeFewMillis.getHours());
        assertTrue(minutes == beforeFewMillis.getMinutes());
        assertTrue(seconds == beforeFewMillis.getSeconds());
        assertTrue(millis - millisToSubtract == beforeFewMillis.getMillis());
        assertTrue(nanos == beforeFewMillis.getNanos());
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
