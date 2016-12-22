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

import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.protobuf.Timestamps;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings("InstanceMethodNamingConvention")
public class LocalTimesShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(LocalDates.class));
    }

    @Test
    public void obtain_current_LocalTime() {
        final LocalTime now = LocalTimes.now();
        final Timestamp time = Timestamps.getCurrentTime();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);

        assertEquals(calendar.get(Calendar.HOUR), now.getHours());
        assertEquals(calendar.get(Calendar.MINUTE), now.getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), now.getSeconds());

        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes() {
        final int hours = 5;
        final int minutes = 30;
        final LocalTime localTime = LocalTimes.of(5, 30);
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, hours);
        calendar.set(Calendar.MINUTE, minutes);

        assertEquals(calendar.get(Calendar.HOUR), localTime.getHours());
        assertEquals(calendar.get(Calendar.MINUTE), localTime.getMinutes());
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes_seconds() {
        final int hours = 5;
        final int minutes = 31;
        final int seconds = 25;
        final LocalTime localTime = LocalTimes.of(5, 31, 25);
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);

        assertEquals(calendar.get(Calendar.HOUR), localTime.getHours());
        assertEquals(calendar.get(Calendar.MINUTE), localTime.getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), localTime.getSeconds());
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes_seconds_millis() {
        final int hours = 5;
        final int minutes = 31;
        final int seconds = 25;
        final int millis = 125;
        final LocalTime localTime = LocalTimes.of(5, 31, 25, 125);
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, millis);

        assertEquals(calendar.get(Calendar.HOUR), localTime.getHours());
        assertEquals(calendar.get(Calendar.MINUTE), localTime.getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), localTime.getSeconds());
        assertEquals(calendar.get(Calendar.MILLISECOND), localTime.getMillis());
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes_seconds_millis_nanos() {
        final int hours = 5;
        final int minutes = 31;
        final int seconds = 25;
        final int millis = 125;
        final long nanos = 2356L;
        final LocalTime localTime = LocalTimes.of(5, 31, 25, 125, 2356L);
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, millis);

        assertEquals(calendar.get(Calendar.HOUR), localTime.getHours());
        assertEquals(calendar.get(Calendar.MINUTE), localTime.getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), localTime.getSeconds());
        assertEquals(calendar.get(Calendar.MILLISECOND), localTime.getMillis());
        assertEquals(nanos, localTime.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_hours() {
        final int hoursToAdd = 2;
        final LocalTime now = LocalTimes.now();
        final Timestamp time = Timestamps.getCurrentTime();
        final LocalTime inFewHours = LocalTimes.plusHours(now, hoursToAdd);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);
        calendar.add(Calendar.HOUR, hoursToAdd);
        
        assertEquals(calendar.get(Calendar.HOUR), inFewHours.getHours());
        assertEquals(calendar.get(Calendar.MINUTE), inFewHours.getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), inFewHours.getSeconds());

        /* We cannot check milliseconds and nanos due to time gap between object creation */

    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_minutes() {
        final int minutesToAdd = 25;
        final LocalTime now = LocalTimes.now();
        final Timestamp time = Timestamps.getCurrentTime();
        final LocalTime inFewMinutes = LocalTimes.plusMinutes(now, minutesToAdd);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);
        calendar.add(Calendar.MINUTE, minutesToAdd);

        assertEquals(calendar.get(Calendar.HOUR), inFewMinutes.getHours());
        assertEquals(calendar.get(Calendar.MINUTE), inFewMinutes.getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), inFewMinutes.getSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_seconds() {
        final int secondsToAdd = 28;
        final LocalTime now = LocalTimes.now();
        final Timestamp time = Timestamps.getCurrentTime();
        final LocalTime inFewSeconds = LocalTimes.plusSeconds(now, secondsToAdd);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);
        calendar.add(Calendar.SECOND, secondsToAdd);

        assertEquals(calendar.get(Calendar.HOUR), inFewSeconds.getHours());
        assertEquals(calendar.get(Calendar.MINUTE), inFewSeconds.getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), inFewSeconds.getSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_millis() {
        final int millisToAdd = 288;
        final LocalTime now = LocalTimes.now();
        final int millisNow = now.getMillis();
        final Timestamp time = Timestamps.getCurrentTime();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);
        final LocalTime inFewmillis = LocalTimes.plusMillis(now, millisToAdd);

        assertEquals(calendar.get(Calendar.HOUR), inFewmillis.getHours());
        assertEquals(calendar.get(Calendar.MINUTE), inFewmillis.getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), inFewmillis.getSeconds());
        assertEquals(millisNow + millisToAdd, inFewmillis.getMillis());
        /* We cannot check nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_hours() {
        final int hoursToSubtract = 2;
        final LocalTime now = LocalTimes.now();
        final Timestamp time = Timestamps.getCurrentTime();
        final LocalTime beforeFewHours = LocalTimes.minusHours(now, hoursToSubtract);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);
        calendar.add(Calendar.HOUR, hoursToSubtract);

        assertEquals(calendar.get(Calendar.HOUR), beforeFewHours.getHours());
        assertEquals(calendar.get(Calendar.MINUTE), beforeFewHours.getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), beforeFewHours.getSeconds());

        /* We cannot check milliseconds and nanos due to time gap between object creation */

    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_minutes() {
        final int minutesToSubtract = 25;
        final LocalTime now = LocalTimes.now();
        final Timestamp time = Timestamps.getCurrentTime();
        final LocalTime beforeFewMinutes = LocalTimes.minusMinutes(now, minutesToSubtract);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);
        calendar.add(Calendar.MINUTE, minutesToSubtract);

        assertEquals(calendar.get(Calendar.HOUR), beforeFewMinutes.getHours());
        assertEquals(calendar.get(Calendar.MINUTE), beforeFewMinutes.getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), beforeFewMinutes.getSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_seconds() {
        final int secondsToSubtract = 28;
        final LocalTime now = LocalTimes.now();
        final Timestamp time = Timestamps.getCurrentTime();
        final LocalTime beforeFewSeconds = LocalTimes.minusSeconds(now, secondsToSubtract);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);
        calendar.add(Calendar.SECOND, secondsToSubtract);

        assertEquals(calendar.get(Calendar.HOUR), beforeFewSeconds.getHours());
        assertEquals(calendar.get(Calendar.MINUTE), beforeFewSeconds.getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), beforeFewSeconds.getSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_millis() {
        final int millisToSubtract = 288;
        final LocalTime now = LocalTimes.now();
        final int millisNow = now.getMillis();
        final Timestamp time = Timestamps.getCurrentTime();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time.getSeconds() / 1000);
        final LocalTime beforeFewmillis = LocalTimes.minusMillis(now, millisToSubtract);

        assertEquals(calendar.get(Calendar.HOUR), beforeFewmillis.getHours());
        assertEquals(calendar.get(Calendar.MINUTE), beforeFewmillis.getMinutes());
        assertEquals(calendar.get(Calendar.SECOND), beforeFewmillis.getSeconds());
        assertEquals(millisNow + millisToSubtract, beforeFewmillis.getMillis());
        /* We cannot check nanos due to time gap between object creation */
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
