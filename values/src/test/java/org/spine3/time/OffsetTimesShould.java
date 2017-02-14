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

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateParameterlessCtor;
import static org.spine3.time.Calendars.getHours;
import static org.spine3.time.Calendars.getMinutes;
import static org.spine3.time.Calendars.getSeconds;
import static org.spine3.time.Calendars.getZoneOffset;

@SuppressWarnings("InstanceMethodNamingConvention")
public class OffsetTimesShould {

    private static final int hours = 9;
    private static final int minutes = 25;
    private static final int seconds = 30;
    private static final int millis = 124;
    private static final long nanos = 122L;
    private static final ZoneOffset ZONE_OFFSET = ZoneOffsets.ofHoursMinutes(3, 30);
    private static final LocalTime localTime = LocalTimes.of(hours, minutes, seconds, millis, nanos);

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateParameterlessCtor(OffsetTimes.class));
    }

    @Test
    public void obtain_current_OffsetTime_using_ZoneOffset() {
        final OffsetTime now = OffsetTimes.now(ZONE_OFFSET);
        final Calendar cal = Calendars.nowAt(ZONE_OFFSET);

        final LocalTime time = now.getTime();
        assertEquals(getHours(cal), time.getHours());
        assertEquals(getMinutes(cal), time.getMinutes());
        assertEquals(getSeconds(cal), time.getSeconds());
        assertEquals(getZoneOffset(cal), now.getOffset().getAmountSeconds());
        /* We cannot check milliseconds and nanos due to time gap between object creation */
    }

    @Test
    public void obtain_current_OffsetTime_using_LocalTime_and_ZoneOffset() {
        final OffsetTime localTimeInDelhi = OffsetTimes.of(localTime, ZONE_OFFSET);

        final LocalTime time = localTimeInDelhi.getTime();
        assertEquals(hours, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());
        assertEquals(ZONE_OFFSET.getAmountSeconds(),
                     localTimeInDelhi.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_OffsetTime_in_future_after_specified_number_of_hours() {
        final int hoursToAdd = 2;
        final OffsetTime offsetTime = OffsetTimes.of(localTime, ZONE_OFFSET);
        final OffsetTime inFewHours = OffsetTimes.plusHours(offsetTime, hoursToAdd);

        final LocalTime time = inFewHours.getTime();
        assertEquals(hours + hoursToAdd, time.getHours());
        assertEquals(minutes, time.getMinutes());
        assertEquals(seconds, time.getSeconds());
        assertEquals(millis, time.getMillis());
        assertEquals(nanos, time.getNanos());
    }

    @Test
    public void obtain_OffsetTime_in_future_after_specified_number_of_minutes() {
        final int minutesToAdd = 15;
        final OffsetTime offsetTime = OffsetTimes.of(localTime, ZONE_OFFSET);
        final OffsetTime inFewHours = OffsetTimes.plusMinutes(offsetTime, minutesToAdd);

        assertEquals(hours, inFewHours.getTime().getHours());
        assertEquals(minutes + minutesToAdd, inFewHours.getTime().getMinutes());
        assertEquals(seconds, inFewHours.getTime().getSeconds());
        assertEquals(millis, inFewHours.getTime().getMillis());
        assertEquals(nanos, inFewHours.getTime().getNanos());
    }

    @Test
    public void obtain_OffsetTime_in_future_after_specified_number_of_seconds() {
        final int secondsToAdd = 17;
        final OffsetTime offsetTime = OffsetTimes.of(localTime, ZONE_OFFSET);
        final OffsetTime inFewHours = OffsetTimes.plusSeconds(offsetTime, secondsToAdd);

        assertEquals(hours, inFewHours.getTime().getHours());
        assertEquals(minutes, inFewHours.getTime().getMinutes());
        assertEquals(seconds + secondsToAdd, inFewHours.getTime().getSeconds());
        assertEquals(millis, inFewHours.getTime().getMillis());
        assertEquals(nanos, inFewHours.getTime().getNanos());
    }

    @Test
    public void obtain_OffsetTime_in_future_after_specified_number_of_millis() {
        final int millisToAdd = 271;
        final OffsetTime offsetTime = OffsetTimes.of(localTime, ZONE_OFFSET);
        final OffsetTime inFewHours = OffsetTimes.plusMillis(offsetTime, millisToAdd);

        assertEquals(hours, inFewHours.getTime().getHours());
        assertEquals(minutes, inFewHours.getTime().getMinutes());
        assertEquals(seconds, inFewHours.getTime().getSeconds());
        assertEquals(millis + millisToAdd, inFewHours.getTime().getMillis());
        assertEquals(nanos, inFewHours.getTime().getNanos());
    }

    @Test
    public void obtain_OffsetTime_in_past_before_specified_number_of_hours() {
        final int hoursToSubtract = 2;
        final OffsetTime offsetTime = OffsetTimes.of(localTime, ZONE_OFFSET);
        final OffsetTime inFewHours = OffsetTimes.minusHours(offsetTime, hoursToSubtract);

        assertEquals(hours - hoursToSubtract, inFewHours.getTime().getHours());
        assertEquals(minutes, inFewHours.getTime().getMinutes());
        assertEquals(seconds, inFewHours.getTime().getSeconds());
        assertEquals(millis, inFewHours.getTime().getMillis());
        assertEquals(nanos, inFewHours.getTime().getNanos());
    }

    @Test
    public void obtain_OffsetTime_in_past_before_specified_number_of_minutes() {
        final int minutesToSubtract = 11;
        final OffsetTime offsetTime = OffsetTimes.of(localTime, ZONE_OFFSET);
        final OffsetTime inFewHours = OffsetTimes.minusMinutes(offsetTime, minutesToSubtract);

        assertEquals(hours, inFewHours.getTime().getHours());
        assertEquals(minutes - minutesToSubtract, inFewHours.getTime().getMinutes());
        assertEquals(seconds, inFewHours.getTime().getSeconds());
        assertEquals(millis, inFewHours.getTime().getMillis());
        assertEquals(nanos, inFewHours.getTime().getNanos());
    }

    @Test
    public void obtain_OffsetTime_in_past_before_specified_number_of_seconds() {
        final int secondsToSubtract = 28;
        final OffsetTime offsetTime = OffsetTimes.of(localTime, ZONE_OFFSET);
        final OffsetTime inFewHours = OffsetTimes.minusSeconds(offsetTime, secondsToSubtract);

        assertEquals(hours, inFewHours.getTime().getHours());
        assertEquals(minutes, inFewHours.getTime().getMinutes());
        assertEquals(seconds - secondsToSubtract, inFewHours.getTime().getSeconds());
        assertEquals(millis, inFewHours.getTime().getMillis());
        assertEquals(nanos, inFewHours.getTime().getNanos());

    }

    @Test
    public void obtain_OffsetTime_in_past_before_specified_number_of_millis() {
        final int millisToSubtract = 99;
        final OffsetTime offsetTime = OffsetTimes.of(localTime, ZONE_OFFSET);
        final OffsetTime inFewHours = OffsetTimes.minusMillis(offsetTime, millisToSubtract);

        assertEquals(hours, inFewHours.getTime().getHours());
        assertEquals(minutes, inFewHours.getTime().getMinutes());
        assertEquals(seconds, inFewHours.getTime().getSeconds());
        assertEquals(millis - millisToSubtract, inFewHours.getTime().getMillis());
        assertEquals(nanos, inFewHours.getTime().getNanos());
    }
    
    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetTime_value_with_hoursToAdd() {
        final int hoursToAdd = -5;
        final OffsetTime now = null;
        OffsetTimes.plusHours(now, hoursToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetTime_value_with_minutesToAdd() {
        final int minutesToAdd = 7;
        final OffsetTime now = null;
        OffsetTimes.plusMinutes(now, minutesToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetTime_value_with_secondsToAdd() {
        final int secondsToAdd = 25;
        final OffsetTime now = null;
        OffsetTimes.plusSeconds(now, secondsToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetTime_value_with_millisToAdd() {
        final int millisToAdd = 205;
        final OffsetTime now = null;
        OffsetTimes.plusMillis(now, millisToAdd);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetTime_value_with_hoursToSubtract() {
        final int hoursToSubtract = 6;
        final OffsetTime now = null;
        OffsetTimes.minusHours(now, hoursToSubtract);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetTime_value_with_minutesToSubtract() {
        final int minutesToSubtract = 8;
        final OffsetTime now = null;
        OffsetTimes.minusMinutes(now, minutesToSubtract);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetTime_value_with_secondsToSubtract() {
        final int secondsToSubtract = 27;
        final OffsetTime now = null;
        OffsetTimes.minusSeconds(now, secondsToSubtract);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_OffsetTime_value_with_millisToSubtract() {
        final int millisToSubtract = 245;
        final OffsetTime now = null;
        OffsetTimes.minusMillis(now, millisToSubtract);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_hoursToAdd() {
        final int hoursToAdd = -5;
        final OffsetTime now = OffsetTimes.now(ZONE_OFFSET);
        OffsetTimes.plusHours(now, hoursToAdd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_minutesToAdd() {
        final int minutesToAdd = -7;
        final OffsetTime now = OffsetTimes.now(ZONE_OFFSET);
        OffsetTimes.plusMinutes(now, minutesToAdd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_secondsToAdd() {
        final int secondsToAdd = -25;
        final OffsetTime now = OffsetTimes.now(ZONE_OFFSET);
        OffsetTimes.plusSeconds(now, secondsToAdd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_hoursToSubtract() {
        final int hoursToSubtract = -6;
        final OffsetTime now = OffsetTimes.now(ZONE_OFFSET);
        OffsetTimes.minusHours(now, hoursToSubtract);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_minutesToSubtract() {
        final int minutesToSubtract = -8;
        final OffsetTime now = OffsetTimes.now(ZONE_OFFSET);
        OffsetTimes.minusMinutes(now, minutesToSubtract);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_amount_of_secondsToSubtract() {
        final int secondsToSubtract = -27;
        final OffsetTime now = OffsetTimes.now(ZONE_OFFSET);
        OffsetTimes.minusSeconds(now, secondsToSubtract);
    }
}
