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

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.spine3.protobuf.Durations;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;
import static org.spine3.protobuf.Timestamps.isLaterThan;

/**
 * A utility class for working with {@link Interval}s.
 *
 * @author Alexander Litus
 */
public class Intervals {

    private Intervals() {
    }

    /**
     * Returns an interval between two timestamps.
     *
     * @param start the first point in time
     * @param end   the second point in time
     * @return an interval between {@code start} and {@code end}
     * @throws IllegalArgumentException if the {@code end} is before the {@code start}
     */
    public static Interval between(Timestamp start, Timestamp end) {
        checkArgument(isLaterThan(end, /*than*/ start), "The end must be after the start of the interval.");
        final Interval.Builder interval = Interval.newBuilder()
                                                  .setStart(start)
                                                  .setEnd(end);
        return interval.build();
    }

    /**
     * Returns a duration of the interval.
     *
     * @param interval the interval to calculate its duration
     * @return the duration between the start and the end of the interval
     */
    public static Duration toDuration(Interval interval) {
        final Timestamp start = interval.getStart();
        final Timestamp end = interval.getEnd();
        if (start.equals(end)) {
            return Durations.ZERO;
        }
        final long secondsBetween = end.getSeconds() - start.getSeconds();
        final int nanosBetween = end.getNanos() - start.getNanos();
        final Duration.Builder duration = Duration.newBuilder()
                                                  .setSeconds(abs(secondsBetween))
                                                  .setNanos(abs(nanosBetween));
        return duration.build();
    }
}
