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

package org.spine3.string.time;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import org.spine3.annotations.Internal;
import org.spine3.string.Stringifier;
import org.spine3.string.StringifierRegistry;
import org.spine3.time.LocalDate;
import org.spine3.time.LocalDates;
import org.spine3.time.LocalTime;
import org.spine3.time.OffsetDateTime;
import org.spine3.time.OffsetTime;
import org.spine3.time.ZoneOffset;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * A collection of stringifiers for date/time value objects.
 *
 * @author Alexander Yevsyukov
 * @see TimeStringifiers#forDuration() Duration stringifier
 * @see TimeStringifiers#forTimestamp() Timestap stringifier
 */
public final class TimeStringifiers {

    private TimeStringifiers() {
        // Prevent instantiation of this utility class.
    }

    @Internal
    public static Map<Type, Stringifier<?>> getAll() {
        final ImmutableMap.Builder<Type, Stringifier<?>> builder =
                ImmutableMap.<Type, Stringifier<?>>builder()
                        .put(ZoneOffset.class, forZoneOffset())
                        .put(Duration.class, forDuration())
                        .put(Timestamp.class, forTimestamp())
                        .put(LocalDate.class, forLocalDate())
                        .put(LocalTime.class, forLocalTime())
                        .put(OffsetDateTime.class, forOffsetDateTime())
                        .put(OffsetTime.class, forOffsetTime());

        return builder.build();
    }

    /**
     * Obtains default stringifier for {@code ZoneOffset}s.
     */
    public static Stringifier<ZoneOffset> forZoneOffset() {
        return ZoneOffsetStringifier.instance();
    }

    /**
     * Obtains the default stringifier for {@code Duration} instances.
     *
     * <p>This stringifier is automatically registered in the
     * {@link StringifierRegistry StringifierRegistry}.
     *
     * @see Durations#toString(Duration)
     * @see Durations#parse(String)
     */
    public static Stringifier<Duration> forDuration() {
        return DurationStringifier.instance();
    }

    /**
     * Obtains a stringifier that coverts a Timestamp into to RFC 3339 date string format.
     *
     * @see Timestamps#toString(Timestamp)
     */
    public static Stringifier<Timestamp> forTimestamp() {
        return TimestampStringifier.instance();
    }

    /**
     * Obtains a stringifier for IDs based on {@code Timestamp}s.
     *
     * <p>The stringifier replaces colons in time part of a string representation of a timestamp.
     *
     * <p>For example, the following string:
     * <pre>
     * "1973-01-01T23:59:59.999999999Z"
     * </pre>
     * would be converted to:
     * <pre>
     * "1973-01-01T23-59-59.999999999Z"
     * </pre>
     *
     * <p>This stringifier can be convenient for storing IDs based on {@code Timestamp}s.
     */
    public static Stringifier<Timestamp> forTimestampWebSafe() {
        return WebSafeTimestampStringifer.instance();
    }

    /**
     * Obtains default stringifier for local dates.
     *
     * <p>The stringifier uses {@code yyyy-MM-dd} format for dates.
     *
     * @see LocalDates#parse(String)
     */
    public static Stringifier<LocalDate> forLocalDate() {
        return LocalDateStringifier.instance();
    }

    /**
     * Obtains default stringifier for {@code LocalTime} values.
     *
     * @see org.spine3.time.LocalTimes#parse(String) LocalTimes.parse(String)
     */
    public static Stringifier<LocalTime> forLocalTime() {
        return LocalTimeStringifier.instance();
    }

    /**
     * Obtains a stringifier for {@code OffsetDateTime} values.
     *
     * @see org.spine3.time.OffsetDateTimes#parse(String) OffsetDateTimes.parse(String)
     */
    public static Stringifier<OffsetDateTime> forOffsetDateTime() {
        return OffsetDateTimeStringifier.instance();
    }

    /**
     * Obtains default stringifier for {@code OffsetTime} values.
     *
     * @see org.spine3.time.OffsetTimes#parse(String) OffsetTimes.parse(String)
     */
    public static Stringifier<OffsetTime> forOffsetTime() {
        return OffsetTimeStringifier.instance();
    }
}
