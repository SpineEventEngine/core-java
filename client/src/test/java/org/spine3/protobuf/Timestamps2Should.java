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

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.junit.Test;
import org.spine3.base.Stringifier;
import org.spine3.base.Stringifiers;
import org.spine3.test.TimeTests;

import java.util.Date;

import static com.google.protobuf.util.Durations.fromSeconds;
import static com.google.protobuf.util.Timestamps.add;
import static com.google.protobuf.util.Timestamps.subtract;
import static com.google.protobuf.util.Timestamps.toNanos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.Durations2.fromMinutes;
import static org.spine3.protobuf.Timestamps2.HOURS_PER_DAY;
import static org.spine3.protobuf.Timestamps2.MICROS_PER_SECOND;
import static org.spine3.protobuf.Timestamps2.MILLIS_PER_SECOND;
import static org.spine3.protobuf.Timestamps2.NANOS_PER_MICROSECOND;
import static org.spine3.protobuf.Timestamps2.NANOS_PER_SECOND;
import static org.spine3.protobuf.Timestamps2.SECONDS_PER_HOUR;
import static org.spine3.protobuf.Timestamps2.compare;
import static org.spine3.protobuf.Timestamps2.convertToDate;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;
import static org.spine3.protobuf.Timestamps2.isBetween;
import static org.spine3.protobuf.Timestamps2.isLaterThan;
import static org.spine3.protobuf.Timestamps2.resetProvider;
import static org.spine3.protobuf.Timestamps2.setProvider;
import static org.spine3.protobuf.Timestamps2.systemTime;
import static org.spine3.protobuf.Timestamps2.webSafeTimestampStringifier;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

public class Timestamps2Should {

    private static final Duration TEN_SECONDS = fromSeconds(10L);

    private static final Duration MINUTE = fromMinutes(1);

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Timestamps2.class);
    }

    @Test
    public void declare_unit_constants() {
        // Make these useful constant used from our library code to prevent
        // accidental removal.
        assertNotEquals(0, NANOS_PER_MICROSECOND);
        assertNotEquals(0, MICROS_PER_SECOND);
        assertNotEquals(0, SECONDS_PER_HOUR);
        assertNotEquals(0, HOURS_PER_DAY);
    }

    @Test
    public void calculate_timestamp_of_moment_minute_ago() {
        final Timestamp currentTime = getCurrentTime();
        final Timestamp expected = subtract(currentTime, MINUTE);

        final Timestamp actual = TimeTests.Past.minutesAgo(1);

        assertEquals(expected.getSeconds(), actual.getSeconds());
    }

    @Test
    public void calculate_timestamp_of_moment_seconds_ago() {
        final Timestamp currentTime = getCurrentTime();
        final Timestamp expected = subtract(currentTime, TEN_SECONDS);

        final Timestamp actual = TimeTests.Past.secondsAgo(TEN_SECONDS.getSeconds());

        assertEquals(expected.getSeconds(), actual.getSeconds());
    }

    @Test
    public void compare_two_timestamps_return_negative_int_if_first_less_than_second_one() {
        final Timestamp time1 = getCurrentTime();
        final Timestamp time2 = add(time1, TEN_SECONDS);

        final int result = compare(time1, time2);

        assertTrue(result < 0);
    }

    @Test
    public void compare_two_timestamps_return_negative_int_if_first_is_null() {
        final Timestamp currentTime = getCurrentTime();

        final int result = compare(null, currentTime);

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

        final int result = compare(time1, time2);

        assertEquals(0, result);
    }

    @Test
    public void compare_two_timestamps_return_zero_if_pass_null() {
        final int result = compare(null, null);

        assertEquals(0, result);
    }

    @Test
    public void compare_two_timestamps_return_positive_int_if_first_greater_than_second_one() {
        final Timestamp currentTime = getCurrentTime();
        final Timestamp timeAfterCurrent = add(currentTime, TEN_SECONDS);

        final int result = compare(timeAfterCurrent, currentTime);

        assertTrue(result > 0);
    }

    @Test
    public void compare_two_timestamps_return_positive_int_if_second_one_is_null() {
        final Timestamp currentTime = getCurrentTime();

        final int result = compare(currentTime, null);

        assertTrue(result > 0);
    }

    @Test
    public void return_true_if_timestamp_is_between_two_timestamps() {
        final Timestamp start = getCurrentTime();
        final Timestamp timeBetween = add(start, TEN_SECONDS);
        final Timestamp finish = add(timeBetween, TEN_SECONDS);

        final boolean isBetween = isBetween(timeBetween, start, finish);

        assertTrue(isBetween);
    }

    @Test
    public void return_false_if_timestamp_is_not_between_two_timestamps() {
        final Timestamp start = getCurrentTime();
        final Timestamp finish = add(start, TEN_SECONDS);
        final Timestamp timeNotBetween = add(finish, TEN_SECONDS);

        final boolean isBetween = isBetween(timeNotBetween, start, finish);

        assertFalse(isBetween);
    }

    @Test
    public void return_true_if_timestamp_is_after_another_one() {
        final Timestamp fromPoint = getCurrentTime();
        final Timestamp timeToCheck = add(fromPoint, TEN_SECONDS);

        final boolean isAfter = isLaterThan(timeToCheck, fromPoint);

        assertTrue(isAfter);
    }

    @Test
    public void return_false_if_timestamp_is_not_after_another_one() {
        final Timestamp fromPoint = getCurrentTime();
        final Timestamp timeToCheck = subtract(fromPoint, TEN_SECONDS);

        final boolean isAfter = isLaterThan(timeToCheck, fromPoint);

        assertFalse(isAfter);
    }

    @Test
    public void compare_timestamps_return_negative_int_if_first_less_than_second_one() {
        final Timestamp time1 = getCurrentTime();
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
    public void compare_timestamps_return_positive_int_if_first_greater_than_second_one() {
        final Timestamp currentTime = getCurrentTime();
        final Timestamp timeAfterCurrent = add(currentTime, TEN_SECONDS);

        final int result = Timestamps.comparator()
                                     .compare(timeAfterCurrent, currentTime);

        assertTrue(result > 0);
    }

    @Test
    public void convert_timestamp_to_date_to_nearest_second() {

        final Timestamp expectedTime = getCurrentTime();

        final Date actualDate = convertToDate(expectedTime);
        final long actualSeconds = actualDate.getTime() / MILLIS_PER_SECOND;

        assertEquals(expectedTime.getSeconds(), actualSeconds);
    }

    @Test
    public void convert_timestamp_to_nanos() {
        final Timestamp expectedTime = getCurrentTime();

        final long nanos = toNanos(expectedTime);
        final long expectedNanos = expectedTime.getSeconds() * NANOS_PER_SECOND +
                                   expectedTime.getNanos();

        assertEquals(expectedNanos, nanos);
    }

    @Test
    public void accept_time_provider() {
        final Timestamp fiveMinutesAgo = subtract(getCurrentTime(),
                                                  fromMinutes(5));

        setProvider(new TimeTests.FrozenMadHatterParty(fiveMinutesAgo));

        assertEquals(fiveMinutesAgo, getCurrentTime());
    }

    @Test
    public void reset_time_provider_to_default() {
        final Timestamp aMinuteAgo = subtract(
                systemTime(),
                fromMinutes(1));

        setProvider(new TimeTests.FrozenMadHatterParty(aMinuteAgo));
        resetProvider();

        assertNotEquals(aMinuteAgo, getCurrentTime());
    }

    @Test
    public void obtain_system_time_millis() {
        assertNotEquals(0, systemTime());
    }

    @Test
    public void provide_webSafe_timestamp_stringifier() {
        final Timestamp timestamp = getCurrentTime();
        final Stringifier<Timestamp> stringifier = webSafeTimestampStringifier();

        final String str = stringifier.convert(timestamp);
        assertEquals(timestamp, stringifier.reverse()
                                           .convert(str));
    }

    @Test
    public void provide_stringifier_for_Timestamp() {
        final Timestamp timestamp = getCurrentTime();

        final String str = Stringifiers.toString(timestamp);
        final Timestamp convertedBack = Stringifiers.fromString(str, Timestamp.class);

        assertEquals(timestamp, convertedBack);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_try_to_convert_inappropriate_string_to_timestamp() {
        // This uses TextFormat printing, for the output won't be parsable.
        final String time = Timestamps2.getCurrentTime()
                                       .toString();
        Stringifiers.fromString(time, Timestamp.class);
    }
}
