/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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


import com.google.protobuf.Duration;
import org.junit.Test;
import org.spine3.util.Tests;

import static com.google.protobuf.Duration.newBuilder;
import static org.junit.Assert.*;
import static org.spine3.protobuf.Durations.*;


/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "MagicNumber", "ClassWithTooManyMethods"})
public class DurationsShould {

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Test
    public void have_private_constructor() throws Exception {
        Tests.callPrivateUtilityConstructor(Durations.class);
    }

    @Test
    public void have_ZERO_constant() {
        assertEquals(0, toNanos(ZERO));
    }


    @Test
    public void convert_seconds_to_duration() {
        convertSecondsToDurationTest(0);
        convertSecondsToDurationTest(27);
        convertSecondsToDurationTest(-384);
    }

    private static void convertSecondsToDurationTest(long seconds) {
        final Duration expected = durationFromSec(seconds);
        final Duration actual = ofSeconds(seconds);
        assertEquals(expected, actual);
    }

    @Test(expected = ArithmeticException.class)
    public void fail_to_convert_seconds_to_duration_if_input_is_too_big() {
        ofSeconds(Long.MAX_VALUE);
    }


    @Test
    public void convert_minutes_to_duration() {
        convertMinutesToDurationTest(0);
        convertMinutesToDurationTest(36);
        convertMinutesToDurationTest(-384);
    }

    private static void convertMinutesToDurationTest(long minutes) {
        final long seconds = minutes * 60;
        final Duration expected = durationFromSec(seconds);
        final Duration actual = ofMinutes(minutes);
        assertEquals(expected, actual);
    }

    @Test(expected = ArithmeticException.class)
    public void fail_to_convert_minutes_to_duration_if_input_is_too_big() {
        ofMinutes(Long.MAX_VALUE);
    }


    @Test
    public void convert_hours_to_duration() {
        convertHoursToDurationTest(0);
        convertHoursToDurationTest(36);
        convertHoursToDurationTest(-384);
    }

    private static void convertHoursToDurationTest(long hours) {
        final Duration expected = durationFromSec(hoursToSeconds(hours));
        final Duration actual = ofHours(hours);
        assertEquals(expected, actual);
    }

    @Test(expected = ArithmeticException.class)
    public void fail_to_convert_hours_to_duration_if_input_is_too_big() {
        ofHours(Long.MAX_VALUE);
    }


    @Test
    public void convert_nanoseconds_to_duration_less_than_second() {
        assertEquals(0, nanos(0).getNanos());
        assertEquals(358000, nanos(358000).getNanos());
        assertEquals(-122000, nanos(-122000).getNanos());
    }

    @Test
    @SuppressWarnings("LocalVariableNamingConvention")
    public void convert_nanoseconds_to_duration_bigger_than_second() {

        final long oneSecondInNanos = 1000 * 1000 * 1000;
        final long nanosExtra = 1616;
        final long biggerThanSecondInNanos = oneSecondInNanos + nanosExtra;

        final Duration duration = nanos(biggerThanSecondInNanos);

        assertEquals(1, duration.getSeconds());
        assertEquals(nanosExtra, duration.getNanos());
    }


    @Test
    public void add_null_durations_return_zero() {
        assertEquals(ZERO, add(null, null));
    }

    @Test
    public void add_duration_and_null() {
        final Duration duration = durationFromSec(525);
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
        add(durationFromSec(Long.MAX_VALUE), durationFromSec(256));
    }

    private static void addDurationsTest(long seconds1, long seconds2) {

        final long secondsTotal = seconds1 + seconds2;
        final Duration sumExpected = durationFromSec(secondsTotal);

        final Duration sumActual = add(durationFromSec(seconds1), durationFromSec(seconds2));

        assertEquals(sumExpected, sumActual);
    }


    @Test
    public void subtract_zero_durations() {
        subtractDurationsTest(0, 0);
    }

    @Test
    public void subtract_positive_durations() {
        subtractDurationsTest(25, 5);
        subtractDurationsTest(300, 338);
    }

    @Test
    public void subtract_negative_durations() {
        subtractDurationsTest(-25, -5);
        subtractDurationsTest(-300, -338);
    }

    @Test
    public void subtract_negative_and_positive_durations() {
        subtractDurationsTest(25, -5);
        subtractDurationsTest(-300, 338);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_subtract_too_big_values() {
        final Duration duration1 = durationFromSec(Long.MAX_VALUE);
        final Duration duration2 = durationFromSec(Long.MAX_VALUE / 2);
        subtract(duration1, duration2);
    }

    private static void subtractDurationsTest(long seconds1, long seconds2) {

        final long resultSeconds = seconds1 - seconds2;
        final Duration resultExpected = durationFromSec(resultSeconds);

        final Duration resultActual = subtract(durationFromSec(seconds1), durationFromSec(seconds2));

        assertEquals(resultExpected, resultActual);
    }


    @Test
    public void convert_hours_and_minutes_to_duration() {

        final long hours = 3;
        final long minutes = 25;
        final long secondsTotal = hoursToSeconds(hours) + minutesToSeconds(minutes);
        final Duration expected = durationFromSec(secondsTotal);

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
        assertEquals(1, getHours(ofHours(1)));
        assertEquals(-256, getHours(ofHours(-256)));
    }

    @Test
    public void return_remainder_of_minutes_from_duration() {
        final long minutesRemainder = 8;
        final long minutesTotal = minutesRemainder + 60; // add 1 hour
        assertEquals(minutesRemainder, getMinutes(ofMinutes(minutesTotal)));
    }

    @Test
    public void return_true_if_positive_number_or_zero_case_positive() {
        assertTrue(isPositiveOrZero(durationFromSec(360)));
    }

    @Test
    public void return_true_if_positive_number_or_zero_case_zero() {
        assertTrue(isPositiveOrZero(durationFromSec(0)));
    }

    @Test
    public void return_false_if_not_positive_number_or_zero() {
        assertFalse(isPositiveOrZero(durationFromSec(-32)));
    }


    @Test
    public void return_true_if_positive_number() {
        assertTrue(isPositive(durationFromSec(360)));
    }

    @Test
    public void return_false_if_not_positive_number_case_zero() {
        assertFalse(isPositive(durationFromSec(0)));
    }

    @Test
    public void return_false_if_not_positive_number_case_negative_number() {
        assertFalse(isPositive(durationFromSec(-32)));
    }

    @Test
    public void return_true_if_zero() {
        assertTrue(isZero(durationFromSec(0)));
    }

    @Test
    public void return_false_if_not_zero_case_positive_number() {
        assertFalse(isZero(durationFromSec(360)));
    }

    @Test
    public void return_false_if_not_zero_case_negative_number() {
        assertFalse(isZero(durationFromSec(-32)));
    }


    @Test
    public void return_true_if_is_negative_number() {
        assertTrue(isNegative(durationFromSec(-32)));
    }

    @Test
    public void return_false_if_is_not_negative_case_positive_number() {
        assertFalse(isNegative(durationFromSec(360)));
    }

    @Test
    public void return_false_if_is_not_negative_case_zero() {
        assertFalse(isNegative(durationFromSec(0)));
    }


    @Test
    public void return_true_if_first_number_is_greater() {
        assertTrue(isGreaterThan(durationFromSec(64), durationFromSec(2)));
    }

    @Test
    public void return_false_if_first_is_not_greater_case_less_than_second() {
        assertFalse(isGreaterThan(durationFromSec(2), durationFromSec(64)));
    }

    @Test
    public void return_false_if_first_is_not_greater_case_equal() {
        assertFalse(isGreaterThan(durationFromSec(5), durationFromSec(5)));
    }


    @Test
    public void return_true_if_first_number_is_less() {
        assertTrue(isLessThan(durationFromSec(2), durationFromSec(64)));
    }

    @Test
    public void return_false_if_first_number_is_not_less_case_bigger_than_second() {
        assertFalse(isLessThan(durationFromSec(64), durationFromSec(2)));
    }

    @Test
    public void return_false_if_first_number_is_not_less_case_equal() {
        assertFalse(isLessThan(durationFromSec(5), durationFromSec(5)));
    }


    @Test
    public void return_positive_number_when_compare_case_first_is_greater() {
        final Duration first = durationFromSec(64);
        final Duration second = durationFromSec(2);
        assertTrue(compare(first, second) > 0);
    }

    @Test
    public void return_negative_number_when_compare_case_second_is_greater() {
        final Duration first = durationFromSec(2);
        final Duration second = durationFromSec(64);
        assertTrue(compare(first, second) < 0);
    }

    @Test
    public void return_zero_when_compare_case_numbers_are_equal() {
        final Duration first = durationFromSec(5);
        final Duration second = durationFromSec(5);
        assertTrue(compare(first, second) == 0);
    }


    private static long hoursToSeconds(long hours) {
        return hours * 60L * 60L;
    }

    private static long minutesToSeconds(long minutes) {
        return minutes * 60L;
    }

    private static Duration durationFromSec(long seconds) {
        return newBuilder().setSeconds(seconds).build();
    }
}
