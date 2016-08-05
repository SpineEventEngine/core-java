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

package org.spine3.protobuf;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.test.Tests;

import java.util.Date;

import static com.google.protobuf.util.Timestamps.add;
import static com.google.protobuf.util.Timestamps.subtract;
import static org.junit.Assert.*;
import static org.spine3.protobuf.Timestamps.*;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings("InstanceMethodNamingConvention")
public class TimestampsShould {

    private static final int NANOS_IN_SECOND = 1000000000;

    private static final Duration TEN_SECONDS = Durations.ofSeconds(10);

    private static final Duration MINUTE = Durations.ofMinutes(1);

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Timestamps.class));
    }

    @Test
    public void not_throw_exception_if_timestamp_is_valid() {
        Timestamps.checkTimestamp(Timestamp.newBuilder()
                                           .setSeconds(8)
                                           .setNanos(7)
                                           .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_have_negative_nanos() {
        Timestamps.checkTimestamp(Timestamp.newBuilder()
                                           .setNanos(-1)
                                           .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_have_negative_greater_than_NANOS_PER_SECOND() {
        Timestamps.checkTimestamp(Timestamp.newBuilder().setNanos((int) Timestamps.NANOS_PER_SECOND + 1).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void have_seconds_greater_than_TIMESTAMP_SECONDS_MIN() {
        Timestamps.checkTimestamp(Timestamp.newBuilder().setSeconds(Timestamps.TIMESTAMP_SECONDS_MIN).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void have_seconds_lower_than_TIMESTAMP_SECONDS_MAX() {
        Timestamps.checkTimestamp(Timestamp.newBuilder().setSeconds(Timestamps.TIMESTAMP_SECONDS_MAX).build());
    }

    @Test
    public void calculate_timestamp_of_moment_minute_ago() {
        final Timestamp currentTime = Timestamps.getCurrentTime();
        final Timestamp expected = com.google.protobuf.util.Timestamps.subtract(currentTime, MINUTE);

        final Timestamp actual = Timestamps.minutesAgo(1);

        assertEquals(expected.getSeconds(), actual.getSeconds());
    }

    @Test
    public void calculate_timestamp_of_moment_seconds_ago() {
        final Timestamp currentTime = Timestamps.getCurrentTime();
        final Timestamp expected = com.google.protobuf.util.Timestamps.subtract(currentTime, TEN_SECONDS);

        final Timestamp actual = Timestamps.secondsAgo(TEN_SECONDS.getSeconds());

        assertEquals(expected.getSeconds(), actual.getSeconds());
    }

    @Test
    public void compare_two_timestamps_return_negative_int_if_first_less_than_second_one() {
        final Timestamp time1 = Timestamps.getCurrentTime();
        final Timestamp time2 = add(time1, TEN_SECONDS);

        final int result = Timestamps.compare(time1, time2);

        assertTrue(result < 0);
    }

    @Test
    public void compare_two_timestamps_return_negative_int_if_first_is_null() {
        final Timestamp currentTime = Timestamps.getCurrentTime();

        final int result = Timestamps.compare(null, currentTime);

        assertTrue(result < 0);
    }

    @Test
    public void compare_two_timestamps_return_zero_if_timestamps_are_equal() {
        final int secs = 256;
        final int nanos = 512;
        final Timestamp time1 = Timestamp.newBuilder()
                                         .setSeconds(secs)
                                         .setNanos(nanos)
                                         .build();
        final Timestamp time2 = Timestamp.newBuilder()
                                         .setSeconds(secs)
                                         .setNanos(nanos)
                                         .build();

        final int result = Timestamps.compare(time1, time2);

        assertEquals(0, result);
    }

    @Test
    public void compare_two_timestamps_return_zero_if_pass_null() {
        final int result = Timestamps.compare(null, null);

        assertEquals(0, result);
    }

    @Test
    public void compare_two_timestamps_return_positive_int_if_first_greater_than_second_one() {
        final Timestamp currentTime = Timestamps.getCurrentTime();
        final Timestamp timeAfterCurrent = com.google.protobuf.util.Timestamps.add(currentTime, TEN_SECONDS);

        final int result = Timestamps.compare(timeAfterCurrent, currentTime);

        assertTrue(result > 0);
    }

    @Test
    public void compare_two_timestamps_return_positive_int_if_second_one_is_null() {
        final Timestamp currentTime = Timestamps.getCurrentTime();

        final int result = Timestamps.compare(currentTime, null);

        assertTrue(result > 0);
    }

    @Test
    public void return_true_if_timestamp_is_between_two_timestamps() {
        final Timestamp start = Timestamps.getCurrentTime();
        final Timestamp timeBetween = com.google.protobuf.util.Timestamps.add(start, TEN_SECONDS);
        final Timestamp finish = com.google.protobuf.util.Timestamps.add(timeBetween, TEN_SECONDS);

        final boolean isBetween = Timestamps.isBetween(timeBetween, start, finish);

        assertTrue(isBetween);
    }

    @Test
    public void return_false_if_timestamp_is_not_between_two_timestamps() {
        final Timestamp start = Timestamps.getCurrentTime();
        final Timestamp finish = add(start, TEN_SECONDS);
        final Timestamp timeNotBetween = add(finish, TEN_SECONDS);

        final boolean isBetween = Timestamps.isBetween(timeNotBetween, start, finish);

        assertFalse(isBetween);
    }

    @Test
    public void return_true_if_timestamp_is_after_another_one() {
        final Timestamp fromPoint = Timestamps.getCurrentTime();
        final Timestamp timeToCheck = add(fromPoint, TEN_SECONDS);

        final boolean isAfter = Timestamps.isLaterThan(timeToCheck, fromPoint);

        assertTrue(isAfter);
    }

    @Test
    public void return_false_if_timestamp_is_not_after_another_one() {
        final Timestamp fromPoint = Timestamps.getCurrentTime();
        final Timestamp timeToCheck = subtract(fromPoint, TEN_SECONDS);

        final boolean isAfter = Timestamps.isLaterThan(timeToCheck, fromPoint);

        assertFalse(isAfter);
    }

    @Test
    public void compare_two_timestamps_using_comparator_return_negative_int_if_first_less_than_second_one() {
        final Timestamp time1 = Timestamps.getCurrentTime();
        final Timestamp time2 = add(time1, TEN_SECONDS);

        final int result = Timestamps.comparator()
                                     .compare(time1, time2);

        assertTrue(result < 0);
    }

    @Test
    public void compare_two_timestamps_using_comparator_return_zero_if_timestamps_are_equal() {
        final int secs = 256;
        final int nanos = 512;
        final Timestamp time1 = Timestamp.newBuilder()
                                         .setSeconds(secs)
                                         .setNanos(nanos)
                                         .build();
        final Timestamp time2 = Timestamp.newBuilder()
                                         .setSeconds(secs)
                                         .setNanos(nanos)
                                         .build();

        final int result = Timestamps.comparator()
                                     .compare(time1, time2);

        assertEquals(0, result);
    }

    @Test
    public void compare_two_timestamps_using_comparator_return_positive_int_if_first_greater_than_second_one() {
        final Timestamp currentTime = Timestamps.getCurrentTime();
        final Timestamp timeAfterCurrent = add(currentTime, TEN_SECONDS);

        final int result = Timestamps.comparator()
                                     .compare(timeAfterCurrent, currentTime);

        assertTrue(result > 0);
    }

    @Test
    public void convert_timestamp_to_date_to_nearest_second() {

        final Timestamp expectedTime = Timestamps.getCurrentTime();

        final Date actualDate = convertToDate(expectedTime);
        final long actualSeconds = actualDate.getTime() / MILLIS_PER_SECOND;

        assertEquals(expectedTime.getSeconds(), actualSeconds);
    }

    @Test
    public void convert_timestamp_to_nanos() {
        final Timestamp expectedTime = Timestamps.getCurrentTime();

        final long nanos = convertToNanos(expectedTime);
        final long expectedNanos = expectedTime.getSeconds() * NANOS_IN_SECOND + expectedTime.getNanos();

        assertEquals(expectedNanos, nanos);
    }

    @Test
    public void accept_time_provider() {
        final Timestamp fiveMinutesAgo = com.google.protobuf.util.Timestamps.subtract(Timestamps.getCurrentTime(), Durations.ofMinutes(5));

        Timestamps.setProvider(new Tests.FrozenMadHatterParty(fiveMinutesAgo));

        assertEquals(fiveMinutesAgo, Timestamps.getCurrentTime());
    }
}
