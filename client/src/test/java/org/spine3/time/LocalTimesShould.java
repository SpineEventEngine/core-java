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

import com.google.common.testing.NullPointerTester;
import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.time.Calendars.getHours;
import static org.spine3.time.Calendars.getMinutes;
import static org.spine3.time.Calendars.getSeconds;
import static org.spine3.time.LocalTimes.parse;

/**
 * @author Alexander Aleksandrov
 * @author Alexander Yevsyukov
 */
public class LocalTimesShould {

    private static final int hours = 9;
    private static final int minutes = 25;
    private static final int seconds = 30;
    private static final int millis = 124;
    private static final int nanos = 122;

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(LocalTimes.class);
    }

    @Test
    public void obtain_current_LocalTime() {
        final LocalTime now = LocalTimes.now();
        final Calendar cal = Calendars.now();

        assertEquals(getHours(cal), now.getHours());
        assertEquals(getMinutes(cal), now.getMinutes());
        assertEquals(getSeconds(cal), now.getSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes() {
        final LocalTime localTime = LocalTimes.of(hours, minutes);

        assertEquals(hours, localTime.getHours());
        assertEquals(minutes, localTime.getMinutes());
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes_seconds() {
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds);

        assertEquals(hours, localTime.getHours());
        assertEquals(minutes, localTime.getMinutes());
        assertEquals(seconds, localTime.getSeconds());
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes_seconds_millis() {
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis);

        assertEquals(hours, localTime.getHours());
        assertEquals(minutes, localTime.getMinutes());
        assertEquals(seconds, localTime.getSeconds());
        assertEquals(millis, localTime.getMillis());
    }

    @Test
    public void obtain_LocalTime_using_hours_minutes_seconds_millis_nanos() {
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);

        assertEquals(hours, localTime.getHours());
        assertEquals(minutes, localTime.getMinutes());
        assertEquals(seconds, localTime.getSeconds());
        assertEquals(millis, localTime.getMillis());
        assertEquals(nanos, localTime.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_hours() {
        final int hoursToAdd = 2;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime inFewHours = LocalTimes.plusHours(localTime, hoursToAdd);

        assertEquals(hours + hoursToAdd, inFewHours.getHours());
        assertEquals(minutes, inFewHours.getMinutes());
        assertEquals(seconds, inFewHours.getSeconds());
        assertEquals(millis, inFewHours.getMillis());
        assertEquals(nanos, inFewHours.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_minutes() {
        final int minutesToAdd = 15;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime inFewMinutes = LocalTimes.plusMinutes(localTime, minutesToAdd);

        assertEquals(hours, inFewMinutes.getHours());
        assertEquals(minutes + minutesToAdd, inFewMinutes.getMinutes());
        assertEquals(seconds, inFewMinutes.getSeconds());
        assertEquals(millis, inFewMinutes.getMillis());
        assertEquals(nanos, inFewMinutes.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_seconds() {
        final int secondsToAdd = 18;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime inFewSeconds = LocalTimes.plusSeconds(localTime, secondsToAdd);

        assertEquals(hours, inFewSeconds.getHours());
        assertEquals(minutes, inFewSeconds.getMinutes());
        assertEquals(seconds + secondsToAdd, inFewSeconds.getSeconds());
        assertEquals(millis, inFewSeconds.getMillis());
        assertEquals(nanos, inFewSeconds.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_future_after_specified_number_of_millis() {
        final int millisToAdd = 288;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime inFewMillis = LocalTimes.plusMillis(localTime, millisToAdd);

        assertEquals(hours, inFewMillis.getHours());
        assertEquals(minutes, inFewMillis.getMinutes());
        assertEquals(seconds, inFewMillis.getSeconds());
        assertEquals(millis + millisToAdd, inFewMillis.getMillis());
        assertEquals(nanos, inFewMillis.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_hours() {
        final int hoursToSubtract = 2;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime beforeFewHours = LocalTimes.minusHours(localTime, hoursToSubtract);

        assertEquals(hours - hoursToSubtract, beforeFewHours.getHours());
        assertEquals(minutes, beforeFewHours.getMinutes());
        assertEquals(seconds, beforeFewHours.getSeconds());
        assertEquals(millis, beforeFewHours.getMillis());
        assertEquals(nanos, beforeFewHours.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_minutes() {
        final int minutesToSubtract = 15;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime beforeFewMinutes = LocalTimes.minusMinutes(localTime, minutesToSubtract);

        assertEquals(hours, beforeFewMinutes.getHours());
        assertEquals(minutes - minutesToSubtract, beforeFewMinutes.getMinutes());
        assertEquals(seconds, beforeFewMinutes.getSeconds());
        assertEquals(millis, beforeFewMinutes.getMillis());
        assertEquals(nanos, beforeFewMinutes.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_seconds() {
        final int secondsToSubtract = 12;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime beforeFewSeconds = LocalTimes.minusSeconds(localTime, secondsToSubtract);

        assertEquals(hours, beforeFewSeconds.getHours());
        assertEquals(minutes, beforeFewSeconds.getMinutes());
        assertEquals(seconds - secondsToSubtract, beforeFewSeconds.getSeconds());
        assertEquals(millis, beforeFewSeconds.getMillis());
        assertEquals(nanos, beforeFewSeconds.getNanos());
    }

    @Test
    public void obtain_LocalTime_in_past_before_specified_number_of_millis() {
        final int millisToSubtract = 28;
        final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);
        final LocalTime beforeFewMillis = LocalTimes.minusMillis(localTime, millisToSubtract);

        assertEquals(hours, beforeFewMillis.getHours());
        assertEquals(minutes, beforeFewMillis.getMinutes());
        assertEquals(seconds, beforeFewMillis.getSeconds());
        assertEquals(millis - millisToSubtract, beforeFewMillis.getMillis());
        assertEquals(nanos, beforeFewMillis.getNanos());
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester().setDefault(LocalTime.class, LocalTimes.now())
                               .testAllPublicStaticMethods(LocalTimes.class);
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

    @Test
    public void convert_to_string_and_back() throws ParseException {
        final LocalTime localTime = LocalTimes.now();
        
        assertEquals(localTime, parse(LocalTimes.toString(localTime)));
    }
}
