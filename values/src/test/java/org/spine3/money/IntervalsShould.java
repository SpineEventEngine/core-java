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

package org.spine3.money;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.time.Interval;
import org.spine3.time.Intervals;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class IntervalsShould {

    @Test
    public void return_interval_between_two_timestamps() {
        final Timestamp start = newTimestamp(5, 6);
        final Timestamp end = newTimestamp(10, 10);
        final Interval interval = Interval.newBuilder()
                                          .setStart(start)
                                          .setEnd(end)
                                          .build();

        assertEquals(interval, Intervals.between(start, end));
    }

    @Test
    public void calculate_duration_of_interval() {
        final Timestamp start = newTimestamp(5, 6);
        final Timestamp end = newTimestamp(10, 10);
        final Duration expectedDuration = Duration.newBuilder()
                                          .setSeconds(end.getSeconds() - start.getSeconds())
                                          .setNanos(end.getNanos() - start.getNanos())
                                          .build();
        final Interval interval = Intervals.between(start, end);

        final Duration actualDuration = Intervals.toDuration(interval);

        assertEquals(expectedDuration, actualDuration);
    }

    private static Timestamp newTimestamp(long seconds, int nanos) {
        return Timestamp.newBuilder()
                        .setSeconds(seconds)
                        .setNanos(nanos)
                        .build();
    }
}
