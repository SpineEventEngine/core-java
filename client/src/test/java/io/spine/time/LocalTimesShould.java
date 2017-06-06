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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;

import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.time.Calendars.getHours;
import static io.spine.time.Calendars.getMinutes;
import static io.spine.time.LocalTimes.addHours;
import static io.spine.time.LocalTimes.addMillis;
import static io.spine.time.LocalTimes.addMinutes;
import static io.spine.time.LocalTimes.addSeconds;
import static io.spine.time.LocalTimes.of;
import static io.spine.time.LocalTimes.parse;
import static io.spine.time.LocalTimes.subtractHours;
import static io.spine.time.LocalTimes.subtractMillis;
import static io.spine.time.LocalTimes.subtractMinutes;
import static io.spine.time.LocalTimes.subtractSeconds;
import static io.spine.time.Time.MILLIS_PER_SECOND;
import static io.spine.time.Time.NANOS_PER_MILLISECOND;
import static java.util.Calendar.getInstance;
import static org.junit.Assert.assertEquals;

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

    /**
     * Local time value for computation tests.
     */
    private LocalTime value;

    @Test
    public void have_utility_constructor() {
        assertHasPrivateParameterlessCtor(LocalTimes.class);
    }

    @Before
    public void setUp() {
        value = of(hours, minutes, seconds, millis, nanos);
    }

    @Test
    public void obtain_current_time() {
        final LocalTime now = LocalTimes.now();
        final Calendar cal = getInstance();

        final int expectedHours = getHours(cal);
        final int expectedMinutes = getMinutes(cal);

        /*
          Assert only hours and minutes to reduce the probability of going over the second boundary
          between the LocalTime and Calendar instances construction in the initialization above.
        */
        assertEquals(expectedHours, now.getHours());
        assertEquals(expectedMinutes, now.getMinutes());
    }

    @Test
    public void create_by_hours_and_minutes() {
        final LocalTime localTime = of(hours, minutes);

        assertEquals(hours, localTime.getHours());
        assertEquals(minutes, localTime.getMinutes());
    }

    @Test
    public void create_by_hours_minutes_seconds() {
        final LocalTime localTime = of(hours, minutes, seconds);

        assertEquals(hours, localTime.getHours());
        assertEquals(minutes, localTime.getMinutes());
        assertEquals(seconds, localTime.getSeconds());
    }

    @Test
    public void create_by_hours_minutes_seconds_millis() {
        final LocalTime localTime = of(hours, minutes, seconds, millis);

        assertEquals(hours, localTime.getHours());
        assertEquals(minutes, localTime.getMinutes());
        assertEquals(seconds, localTime.getSeconds());
        assertEquals(millis, localTime.getMillis());
    }

    @Test
    public void create_with_nano_precision() {
        final LocalTime localTime = of(hours, minutes, seconds, millis, nanos);

        assertEquals(hours, localTime.getHours());
        assertEquals(minutes, localTime.getMinutes());
        assertEquals(seconds, localTime.getSeconds());
        assertEquals(millis, localTime.getMillis());
        assertEquals(nanos, localTime.getNanos());
    }

    @Test
    public void add_hours() {
        final int hoursToAdd = 2;
        final LocalTime localTime = of(hours, minutes, seconds, millis, nanos);
        final LocalTime inFewHours = addHours(localTime, hoursToAdd);

        assertEquals(hours + hoursToAdd, inFewHours.getHours());
        assertEquals(minutes, inFewHours.getMinutes());
        assertEquals(seconds, inFewHours.getSeconds());
        assertEquals(millis, inFewHours.getMillis());
        assertEquals(nanos, inFewHours.getNanos());
    }

    @Test
    public void add_minutes() {
        final int minutesToAdd = 15;
        final LocalTime inFewMinutes = addMinutes(value, minutesToAdd);

        assertEquals(hours, inFewMinutes.getHours());
        assertEquals(minutes + minutesToAdd, inFewMinutes.getMinutes());
        assertEquals(seconds, inFewMinutes.getSeconds());
        assertEquals(millis, inFewMinutes.getMillis());
        assertEquals(nanos, inFewMinutes.getNanos());
    }

    @Test
    public void add_seconds() {
        final int secondsToAdd = 18;
        final LocalTime inFewSeconds = addSeconds(value, secondsToAdd);

        assertEquals(hours, inFewSeconds.getHours());
        assertEquals(minutes, inFewSeconds.getMinutes());
        assertEquals(seconds + secondsToAdd, inFewSeconds.getSeconds());
        assertEquals(millis, inFewSeconds.getMillis());
        assertEquals(nanos, inFewSeconds.getNanos());
    }

    @Test
    public void add_millis() {
        final int millisToAdd = 288;
        final LocalTime inFewMillis = addMillis(value, millisToAdd);

        assertEquals(hours, inFewMillis.getHours());
        assertEquals(minutes, inFewMillis.getMinutes());
        assertEquals(seconds, inFewMillis.getSeconds());
        assertEquals(millis + millisToAdd, inFewMillis.getMillis());
        assertEquals(nanos, inFewMillis.getNanos());
    }

    @Test
    public void subtract_hours() {
        final int hoursToSubtract = 2;
        final LocalTime beforeFewHours = subtractHours(value, hoursToSubtract);

        assertEquals(hours - hoursToSubtract, beforeFewHours.getHours());
        assertEquals(minutes, beforeFewHours.getMinutes());
        assertEquals(seconds, beforeFewHours.getSeconds());
        assertEquals(millis, beforeFewHours.getMillis());
        assertEquals(nanos, beforeFewHours.getNanos());
    }

    @Test
    public void subtract_minutes() {
        final int minutesToSubtract = 15;
        final LocalTime beforeFewMinutes = subtractMinutes(value, minutesToSubtract);

        assertEquals(hours, beforeFewMinutes.getHours());
        assertEquals(minutes - minutesToSubtract, beforeFewMinutes.getMinutes());
        assertEquals(seconds, beforeFewMinutes.getSeconds());
        assertEquals(millis, beforeFewMinutes.getMillis());
        assertEquals(nanos, beforeFewMinutes.getNanos());
    }

    @Test
    public void subtract_seconds() {
        final int secondsToSubtract = 12;
        final LocalTime beforeFewSeconds = subtractSeconds(value, secondsToSubtract);

        assertEquals(hours, beforeFewSeconds.getHours());
        assertEquals(minutes, beforeFewSeconds.getMinutes());
        assertEquals(seconds - secondsToSubtract, beforeFewSeconds.getSeconds());
        assertEquals(millis, beforeFewSeconds.getMillis());
        assertEquals(nanos, beforeFewSeconds.getNanos());
    }

    @Test
    public void subtract_millis() {
        final int millisToSubtract = 28;
        final LocalTime beforeFewMillis = subtractMillis(value, millisToSubtract);

        assertEquals(hours, beforeFewMillis.getHours());
        assertEquals(minutes, beforeFewMillis.getMinutes());
        assertEquals(seconds, beforeFewMillis.getSeconds());
        assertEquals(millis - millisToSubtract, beforeFewMillis.getMillis());
        assertEquals(nanos, beforeFewMillis.getNanos());
    }

    //
    // Arguments check
    //-------------------

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Timestamp.class, Time.getCurrentTime())
                .setDefault(ZoneOffset.class, ZoneOffsets.UTC)
                .setDefault(LocalTime.class, LocalTimes.now())
                .testAllPublicStaticMethods(LocalTimes.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_hours() {
        of(-2, 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_oob_hours() {
        of(24, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_minutes() {
        of(0, -20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_oob_minutes() {
        of(0, 60);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_seconds() {
        of(0, 0, -50);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_oob_seconds() {
        of(0, 0, 60);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_millis() {
        of(0, 0, 0, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_oob_millis() {
        of(0, 0, 0, MILLIS_PER_SECOND);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_negative_nanos() {
        of(0, 0, 0, 0, -1501);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_oob_nanos() {
        of(0, 0, 0, 0, NANOS_PER_MILLISECOND);
    }

    //
    // Stringification
    //---------------------

    @Test
    public void convert_to_string_and_back() throws ParseException {
        final LocalTime localTime = of(10, 20, 30, 40, 50);

        final String str = LocalTimes.toString(localTime);
        final LocalTime convertedBack = parse(str);
        assertEquals(localTime, convertedBack);
    }
}
