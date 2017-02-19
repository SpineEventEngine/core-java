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

import com.google.protobuf.Duration;
import org.spine3.protobuf.Durations2;

import java.util.TimeZone;

import static com.google.common.base.Strings.nullToEmpty;
import static org.spine3.protobuf.Durations2.hours;
import static org.spine3.protobuf.Durations2.minutes;
import static org.spine3.protobuf.Timestamps2.MILLIS_PER_SECOND;
import static org.spine3.validate.Validate.checkBounds;

/**
 * Utilities for working with ZoneOffset objects.
 *
 * @author Alexander Yevsyukov
 * @author Alexander Aleksandrov
 * @see ZoneOffset
 */
public class ZoneOffsets {

    public static final int MIN_HOURS_OFFSET = -11;
    public static final int MAX_HOURS_OFFSET = 14;

    public static final int MIN_MINUTES_OFFSET = 0;
    public static final int MAX_MINUTES_OFFSET = 60;

    public static final ZoneOffset UTC = ZoneOffset.newBuilder()
                                                   .setId("UTC")
                                                   .setAmountSeconds(0)
                                                   .build();

    private ZoneOffsets() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains the {@code ZoneOffset} instance using {@code TimeZone}.
     *
     * @param timeZone target time zone
     * @return zone offset instance of specified timezone
     */
    public static ZoneOffset toZoneOffset(TimeZone timeZone) {
        return ZoneOffset.newBuilder()
                         .setAmountSeconds(getOffsetInSeconds(timeZone))
                         .setId(nullToEmpty(timeZone.getID()))
                         .build();
    }

    /**
     * Obtains offset of the passed {@code TimeZone} in seconds.
     */
    public static int getOffsetInSeconds(TimeZone timeZone) {
        final int seconds = timeZone.getRawOffset() / (int) MILLIS_PER_SECOND;
        return seconds;
    }

    /**
     * Obtains a {@code ZoneOffset} instance using default {@code TimeZone} of the Java
     * virtual machine.
     *
     * @see TimeZone#getDefault()
     */
    public static ZoneOffset getDefault() {
        final TimeZone timeZone = TimeZone.getDefault();
        return toZoneOffset(timeZone);
    }

    /**
     * Obtains the ZoneOffset instance using an offset in hours.
     */
    public static ZoneOffset ofHours(int hours) {
        checkHourOffset(hours, false);

        final Duration hourDuration = Durations2.ofHours(hours);
        final int seconds = toSeconds(hourDuration);
        return ZoneOffset.newBuilder()
                         .setAmountSeconds(seconds)
                         .build();
    }

    /**
     * Obtains the ZoneOffset instance using an offset in hours and minutes.
     */
    public static ZoneOffset ofHoursMinutes(int hours, int minutes) {
        checkHourOffset(hours, true);
        checkMinuteOffset(minutes);

        final int secondsInHours = toSeconds(hours(hours));
        final int secondsInMinutes = toSeconds(minutes(minutes));
        final int seconds = secondsInHours + secondsInMinutes;
        return ZoneOffset.newBuilder()
                         .setAmountSeconds(seconds)
                         .build();
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    // It is safe, as we check bounds of the arguments.
    private static int toSeconds(Duration duration) {
        return (int) Durations2.toSeconds(duration);
    }

    private static void checkHourOffset(int hours, boolean assumingMinutes) {
        // If the offset contains minutes too, we make the range smaller by one hour from each end.
        final int shift = (assumingMinutes ? 1 : 0);
        checkBounds(hours, "hours", MIN_HOURS_OFFSET + shift,
                    MAX_HOURS_OFFSET - shift);
    }

    private static void checkMinuteOffset(int minutes) {
        checkBounds(minutes, "minutes", MIN_MINUTES_OFFSET, MAX_MINUTES_OFFSET);
    }
}
