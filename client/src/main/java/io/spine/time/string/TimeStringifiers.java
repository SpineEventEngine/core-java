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

package io.spine.time.string;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.spine.string.Stringifier;
import io.spine.string.StringifierRegistry;
import io.spine.time.LocalDate;
import io.spine.time.LocalDates;
import io.spine.time.LocalTime;
import io.spine.time.OffsetDateTime;
import io.spine.time.OffsetTime;
import io.spine.time.ZoneOffset;

/**
 * A collection of stringifiers for date/time value objects.
 *
 * @author Alexander Yevsyukov
 * @see #forDuration() Duration stringifier
 * @see #forTimestamp() Timestap stringifier
 */
public final class TimeStringifiers {

    static {
        registerAll();
    }

    private TimeStringifiers() {
        // Prevent instantiation of this utility class.
    }

    private static void registerAll() {
        final StringifierRegistry registry = StringifierRegistry.getInstance();
        if (registry.get(Duration.class).isPresent()) {
            // Already registered.
            return;
        }

        registry.register(forDuration(), Duration.class);
        registry.register(forZoneOffset(), ZoneOffset.class);
        registry.register(forTimestamp(), Timestamp.class);
        registry.register(forLocalDate(), LocalDate.class);
        registry.register(forLocalTime(), LocalTime.class);
        registry.register(forOffsetDateTime(), OffsetDateTime.class);
        registry.register(forOffsetTime(), OffsetTime.class);
    }

    /**
     * Obtains default stringifier for {@code ZoneOffset}s.
     *
     * <p>This stringifier is automatically registered in the
     * {@link StringifierRegistry StringifierRegistry}.
     */
    public static Stringifier<ZoneOffset> forZoneOffset() {
        return ZoneOffsetStringifier.getInstance();
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
        return DurationStringifier.getInstance();
    }

    /**
     * Obtains a stringifier that coverts a Timestamp into to RFC 3339 date string format.
     *
     * @see Timestamps#toString(Timestamp)
     */
    public static Stringifier<Timestamp> forTimestamp() {
        return TimestampStringifier.getInstance();
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
        return WebSafeTimestampStringifer.getInstance();
    }

    /**
     * Obtains default stringifier for local dates.
     *
     * <p>The stringifier uses {@code yyyy-MM-dd} format for dates.
     *
     * <p>This stringifier is automatically registered in the
     * {@link StringifierRegistry StringifierRegistry}.
     *
     * @see LocalDates#parse(String)
     */
    public static Stringifier<LocalDate> forLocalDate() {
        return LocalDateStringifier.getInstance();
    }

    /**
     * Obtains default stringifier for {@code LocalTime} values.
     *
     * <p>This stringifier is automatically registered in the
     * {@link StringifierRegistry StringifierRegistry}.
     *
     * @see io.spine.time.LocalTimes#parse(String) LocalTimes.parse(String)
     */
    public static Stringifier<LocalTime> forLocalTime() {
        return LocalTimeStringifier.getInstance();
    }

    /**
     * Obtains a stringifier for {@code OffsetDateTime} values.
     *
     * <p>This stringifier is automatically registered in the
     * {@link StringifierRegistry StringifierRegistry}.
     *
     * @see io.spine.time.OffsetDateTimes#parse(String) OffsetDateTimes.parse(String)
     */
    public static Stringifier<OffsetDateTime> forOffsetDateTime() {
        return OffsetDateTimeStringifier.getInstance();
    }

    /**
     * Obtains default stringifier for {@code OffsetTime} values.
     *
     * <p>This stringifier is automatically registered in the
     * {@link StringifierRegistry StringifierRegistry}.
     *
     * @see io.spine.time.OffsetTimes#parse(String) OffsetTimes.parse(String)
     */
    public static Stringifier<OffsetTime> forOffsetTime() {
        return OffsetTimeStringifier.getInstance();
    }
}
