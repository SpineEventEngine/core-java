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

import org.spine3.protobuf.Durations;

import static org.spine3.validate.Validate.checkBounds;

/**
 * Utilities for working with ZoneOffset objects.
 *
 * @author Alexander Yevsyukov
 * @author Alexander Aleksandrov
 * @see ZoneOffset
 */
public class ZoneOffsets {

    public static final int MAX_HOURS_OFFSET = 14;
    public static final int MIN_HOURS_OFFSET = -11;
    public static final int MAX_MINUTES_OFFSET = 60;
    public static final int MIN_MINUTES_OFFSET = 0;

    public static final ZoneOffset UTC = ZoneOffset.newBuilder()
                                                   .setId("UTC")
                                                   .setAmountSeconds(0)
                                                   .build();

    private ZoneOffsets() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains the ZoneOffset instance using an offset in hours.
     */
    public static ZoneOffset ofHours(int hours) {
        checkBounds(hours, "hours", MIN_HOURS_OFFSET, MAX_HOURS_OFFSET);

        @SuppressWarnings("NumericCastThatLosesPrecision") // It is safe, as we check bounds of the argument.
        final int seconds = (int) Durations.toSeconds(Durations.ofHours(hours));
        return ZoneOffset.newBuilder()
                         .setAmountSeconds(seconds)
                         .build();
    }

    /**
     * Obtains the ZoneOffset instance using an offset in hours and minutes.
     */
    @SuppressWarnings("NumericCastThatLosesPrecision") // It is safe, as we check bounds of the argument.
    public static ZoneOffset ofHoursMinutes(int hours, int minutes) {
        checkBounds(hours, "hours", MIN_HOURS_OFFSET + 1, MAX_HOURS_OFFSET - 1);
        checkBounds(minutes, "minutes", MIN_MINUTES_OFFSET, MAX_MINUTES_OFFSET);

        final int secondsInHours = (int) Durations.toSeconds(Durations.ofHours(hours));
        final int secondsInMinutes = (int) Durations.toSeconds(Durations.ofMinutes(minutes));
        final int seconds = secondsInHours + secondsInMinutes;
        return ZoneOffset.newBuilder()
                         .setAmountSeconds(seconds)
                         .build();
    }
}
