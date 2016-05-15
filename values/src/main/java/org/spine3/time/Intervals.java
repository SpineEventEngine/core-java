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

import static java.lang.Math.abs;

/**
 * A utility class for working with {@link Interval}s.
 *
 * @author Alexander Litus
 */
public class Intervals {

    private Intervals() {}

    /**
     * Returns an interval between two timestamps.
     *
     * @param start the first point in time
     * @param end the second point in time
     * @return an interval between {@code start} and {@code end}
     */
    public static Interval between(Timestamp start, Timestamp end) {
        final long secondsBetween = end.getSeconds() - start.getSeconds();
        final int nanosBetween = end.getNanos() - start.getNanos();
        final Duration duration = Duration.newBuilder()
                .setSeconds(abs(secondsBetween))
                .setNanos(abs(nanosBetween))
                .build();
        final Interval.Builder interval = Interval.newBuilder()
                .setStart(start)
                .setEnd(end)
                .setDuration(duration);
        return interval.build();
    }
}
