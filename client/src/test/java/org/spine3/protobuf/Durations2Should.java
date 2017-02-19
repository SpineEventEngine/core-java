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
package org.spine3.protobuf;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Duration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.Durations2.ZERO;
import static org.spine3.protobuf.Durations2.add;
import static org.spine3.protobuf.Durations2.fromHours;
import static org.spine3.protobuf.Durations2.fromMinutes;
import static org.spine3.protobuf.Durations2.getHours;
import static org.spine3.protobuf.Durations2.getMinutes;
import static org.spine3.protobuf.Durations2.hoursAndMinutes;
import static org.spine3.protobuf.Durations2.isGreaterThan;
import static org.spine3.protobuf.Durations2.isLessThan;
import static org.spine3.protobuf.Durations2.isNegative;
import static org.spine3.protobuf.Durations2.isPositive;
import static org.spine3.protobuf.Durations2.isPositiveOrZero;
import static org.spine3.protobuf.Durations2.isZero;
import static org.spine3.protobuf.Durations2.minutes;
import static org.spine3.protobuf.Durations2.nanos;
import static org.spine3.protobuf.Durations2.seconds;
import static org.spine3.protobuf.Durations2.toMinutes;
import static org.spine3.protobuf.Durations2.toNanos;
import static org.spine3.protobuf.Durations2.toSeconds;
import static org.spine3.test.Tests.hasPrivateParameterlessCtor;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"MagicNumber", "ClassWithTooManyMethods"})
public class Durations2Should {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateParameterlessCtor(Durations2.class));
    }

    @Test
    public void have_ZERO_constant() {
        assertEquals(0, toNanos(ZERO));
    }

    @Test(expected = ArithmeticException.class)
    public void fail_to_convert_minutes_to_duration_if_input_is_too_big() {
        fromMinutes(Long.MAX_VALUE);
    }

    @Test
    public void convert_hours_to_duration() {
        testHourConversion(0);
        testHourConversion(36);
        testHourConversion(-384);
    }

    private static void testHourConversion(long hours) {
        final Duration expected = seconds(hoursToSeconds(hours));
        final Duration actual = fromHours(hours);
        assertEquals(expected, actual);
    }

    @Test(expected = ArithmeticException.class)
    public void fail_to_convert_hours_to_duration_if_input_is_too_big() {
        fromHours(Long.MAX_VALUE);
    }

    @Test
    public void add_null_durations_return_zero() {
        assertEquals(ZERO, add(null, null));
    }

    @Test
    public void add_duration_and_null() {
        final Duration duration = seconds(525);
        assertEquals(duration, add(duration, null));
        assertEquals(duration, add(null, duration));
    }

    @Test
    public void add_positive_durations() {
        addDurationsTest(25, 5);
        addDurationsTest(300, 338);
    }

    @Test
    public void add_negative_durations() {
        addDurationsTest(-25, -5);
        addDurationsTest(-300, -338);
    }

    @Test
    public void add_negative_and_positive_durations() {
        addDurationsTest(25, -5);
        addDurationsTest(-300, 338);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_add_too_big_durations() {
        add(seconds(Long.MAX_VALUE), seconds(256));
    }

    private static void addDurationsTest(long seconds1, long seconds2) {

        final long secondsTotal = seconds1 + seconds2;
        final Duration sumExpected = seconds(secondsTotal);

        final Duration sumActual = add(seconds(seconds1), seconds(seconds2));

        assertEquals(sumExpected, sumActual);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void fail_to_subtract_too_big_values() {
        final Duration duration1 = seconds(Long.MAX_VALUE);
        final Duration duration2 = seconds(Long.MAX_VALUE / 2);
        com.google.protobuf.util.Durations.subtract(duration1, duration2);
    }

    @Test
    public void convert_hours_and_minutes_to_duration() {

        final long hours = 3;
        final long minutes = 25;
        final long secondsTotal = hoursToSeconds(hours) + minutesToSeconds(minutes);
        final Duration expected = seconds(secondsTotal);

        final Duration actual = hoursAndMinutes(hours, minutes);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_duration_to_nanoseconds() {
        assertEquals(10, toNanos(nanos(10)));
        assertEquals(-256, toNanos(nanos(-256)));
    }

    @Test
    public void convert_duration_to_seconds() {
        assertEquals(1, toSeconds(seconds(1)));
        assertEquals(-256, toSeconds(seconds(-256)));
    }

    @Test
    public void convert_duration_to_minutes() {
        assertEquals(1, toMinutes(minutes(1)));
        assertEquals(-256, toMinutes(minutes(-256)));
    }

    @Test
    public void return_hours_from_duration() {
        assertEquals(1, getHours(fromHours(1)));
        assertEquals(-256, getHours(fromHours(-256)));
    }

    @Test
    public void return_remainder_of_minutes_from_duration() {
        final long minutesRemainder = 8;
        final long minutesTotal = minutesRemainder + 60; // add 1 hour
        assertEquals(minutesRemainder, getMinutes(fromMinutes(minutesTotal)));
    }

    @Test
    public void verify_if_positive_or_zero() {
        assertTrue(isPositiveOrZero(seconds(360)));
        assertTrue(isPositiveOrZero(seconds(0)));
        assertFalse(isPositiveOrZero(seconds(-32)));
    }

    @Test
    public void verify_if_positive() {
        assertTrue(isPositive(seconds(360)));
        assertFalse(isPositive(seconds(0)));
        assertFalse(isPositive(seconds(-32)));
    }

    @Test
    public void verify_if_zero() {
        assertTrue(isZero(seconds(0)));
        assertFalse(isZero(seconds(360)));
        assertFalse(isZero(seconds(-32)));
    }

    @Test
    public void verify_if_negative() {
        assertTrue(isNegative(seconds(-32)));
        assertFalse(isNegative(seconds(360)));
        assertFalse(isNegative(seconds(0)));
    }

    @Test
    public void verify_if_greater() {
        assertTrue(isGreaterThan(seconds(64), seconds(2)));
        assertFalse(isGreaterThan(seconds(2), seconds(64)));
        assertFalse(isGreaterThan(seconds(5), seconds(5)));
    }

    @Test
    public void verify_if_less() {
        assertTrue(isLessThan(seconds(2), seconds(64)));
        assertFalse(isLessThan(seconds(64), seconds(2)));
        assertFalse(isLessThan(seconds(5), seconds(5)));
    }
    
    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Duration.class, Duration.getDefaultInstance())
                .testStaticMethods(Durations2.class, NullPointerTester.Visibility.PACKAGE);
    }

    private static long hoursToSeconds(long hours) {
        return hours * 60L * 60L;
    }

    private static long minutesToSeconds(long minutes) {
        return minutes * 60L;
    }

}
