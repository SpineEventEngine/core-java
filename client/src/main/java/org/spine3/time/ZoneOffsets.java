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
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import java.text.ParseException;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.nullToEmpty;
import static java.lang.String.format;
import static org.spine3.time.Durations2.hours;
import static org.spine3.time.Durations2.minutes;
import static org.spine3.time.Timestamps2.MILLIS_PER_SECOND;
import static org.spine3.time.Timestamps2.MINUTES_PER_HOUR;
import static org.spine3.time.Timestamps2.SECONDS_PER_MINUTE;
import static org.spine3.time.Timestamps2.getCurrentTime;
import static org.spine3.validate.Validate.checkBounds;

/**
 * Utilities for working with {@code ZoneOffset}s.
 *
 * @author Alexander Yevsyukov
 * @author Alexander Aleksandrov
 * @see ZoneOffset
 */
public final class ZoneOffsets {

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
    static ZoneOffset toZoneOffset(TimeZone timeZone) {
        final Timestamp now = getCurrentTime();
        final long date = Timestamps.toMillis(now);
        final int offsetInSeconds = getOffsetInSeconds(timeZone, date);
        final String id = nullToEmpty(timeZone.getID());
        return ZoneOffset.newBuilder()
                         .setAmountSeconds(offsetInSeconds)
                         .setId(id)
                         .build();
    }

    /**
     * Obtains offset of the passed {@code TimeZone} in seconds.
     */
    private static int getOffsetInSeconds(TimeZone timeZone, long date) {
        final int seconds = timeZone.getOffset(date) / MILLIS_PER_SECOND;
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

        final Duration hourDuration = Durations2.fromHours(hours);
        final int seconds = toSeconds(hourDuration);
        return ofSeconds(seconds);
    }

    public static ZoneOffset ofSeconds(int seconds) {
        return ZoneOffset.newBuilder()
                         .setAmountSeconds(seconds)
                         .build();
    }

    /**
     * Obtains the ZoneOffset instance using an offset in hours and minutes.
     *
     * <p>If a negative zone offset is created both passed values must be negative.
     */
    public static ZoneOffset ofHoursMinutes(int hours, int minutes) {
        checkHourOffset(hours, true);
        checkMinuteOffset(minutes);
        checkArgument(((hours < 0) == (minutes < 0)) || (minutes == 0),
                      "Hours (%s) and minutes (%s) must have the same sign.", hours, minutes);

        final int secondsInHours = toSeconds(hours(hours));
        final int secondsInMinutes = toSeconds(minutes(minutes));
        final int seconds = secondsInHours + secondsInMinutes;
        return ofSeconds(seconds);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    // It is safe, as we check bounds of the arguments.
    private static int toSeconds(Duration duration) {
        return (int) Durations2.toSeconds(duration);
    }

    private static void checkHourOffset(int hours, boolean assumingMinutes) {
        // If the offset contains minutes too, we make the range smaller by one hour from each end.
        final int shift = (assumingMinutes ? 1 : 0);
        checkBounds(hours, "hours", MIN_HOURS_OFFSET + shift, MAX_HOURS_OFFSET - shift);
    }

    private static void checkMinuteOffset(int minutes) {
        checkBounds(Math.abs(minutes), "minutes", MIN_MINUTES_OFFSET, MAX_MINUTES_OFFSET);
    }

    /**
     * Parses the time zone offset value formatted as a signed value of hours and minutes.
     *
     * <p>Examples of accepted values: {@code +3:00}, {@code -04:30}.
     *
     * @throws ParseException if the passed value has invalid format
     */
    public static ZoneOffset parse(String value) throws ParseException {
        int pos = value.indexOf(':');
        if (pos == -1) {
            final String errMsg = format("Invalid offset value: \"%s\"", value);
            throw new ParseException(errMsg, 0);
        }
        final boolean positive = value.charAt(0) == Formats.PLUS;
        final boolean negative = value.charAt(0) == Formats.MINUS;

        if (!(positive || negative)) {
            final String errMsg = format("Missing sign char in offset value: \"%s\"", value);
            throw new ParseException(errMsg, 0);
        }

        final String hoursStr = value.substring(1, pos);
        final String minutesStr = value.substring(pos + 1);
        final long hours = Long.parseLong(hoursStr);
        final long minutes = Long.parseLong(minutesStr);
        final long totalMinutes = hours * MINUTES_PER_HOUR + minutes;
        long seconds = totalMinutes * SECONDS_PER_MINUTE;

        if (negative) {
            seconds = -seconds;
        }

        @SuppressWarnings("NumericCastThatLosesPrecision") // OK since the value cannot grow larger.
        final ZoneOffset result = ofSeconds((int) seconds);
        return result;
    }

    /**
     * Converts the passed zone offset into a string with a signed amount of hours and minutes.
     */
    public static String toString(ZoneOffset zoneOffset) {
        checkNotNull(zoneOffset);
        final long seconds = zoneOffset.getAmountSeconds();
        final long totalMinutes = seconds / SECONDS_PER_MINUTE;
        final long hours = totalMinutes / MINUTES_PER_HOUR;
        final long minutes = totalMinutes % MINUTES_PER_HOUR;
        final StringBuilder builder = new StringBuilder(6)
            .append(seconds >= 0 ? Formats.PLUS : Formats.MINUS)
            .append(format(Formats.HOURS_AND_MINUTES_FORMAT, Math.abs(hours), Math.abs(minutes)));
        return builder.toString();
    }
}
