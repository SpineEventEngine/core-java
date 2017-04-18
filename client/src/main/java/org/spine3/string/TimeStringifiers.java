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

package org.spine3.string;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import org.spine3.time.LocalDate;
import org.spine3.time.LocalDates;

import java.io.Serializable;
import java.text.ParseException;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.util.Exceptions.newIllegalArgumentException;
import static org.spine3.util.Exceptions.wrappedCause;

/**
 * A collection of stringifiers for date/time value objects.
 *
 * @author Alexander Yevsyukov
 * @see TimeStringifiers#forDuration() Duration stringifier
 * @see TimeStringifiers#forTimestamp() Timestap stringifier
 */
public class TimeStringifiers {

    private static final Stringifier<Timestamp> stringifier =
            new TimestampStringifier();
    private static final Stringifier<Timestamp> webSafeStringifier =
            new WebSafeTimestampStringifer();

    private TimeStringifiers() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Register all stringifiers exposed by this class with the {@link StringifierRegistry}.
     */
    public static void registerAll() {
        final StringifierRegistry registry = StringifierRegistry.getInstance();

        registry.register(forLocalDate(), LocalDate.class);

        //TODO:2017-04-18:alexander.yevsyukov: Register other stringifiers
    }

    /**
     * Obtains default stringifier for local dates.
     *
     * <p>The stringifier uses {@code yyyy-MM-dd} format for dates.
     * @see LocalDates#parse(String)
     */
    public static Stringifier<LocalDate> forLocalDate() {
        return LocalDateStringifier.INSTANCE;
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
        return webSafeStringifier;
    }

    /**
     * Obtains a stringifier that coverts a Timestamp into to RFC 3339 date string format.
     *
     * @see Timestamps#toString(Timestamp)
     */
    public static Stringifier<Timestamp> forTimestamp() {
        return stringifier;
    }

    /**
     * The default stringifier for {@link LocalDate} instances.
     */
    private static final class LocalDateStringifier extends Stringifier<LocalDate>
            implements Serializable {

        private static final long serialVersionUID = 1;

        static final LocalDateStringifier INSTANCE = new LocalDateStringifier();

        @Override
        protected String toString(LocalDate date) {
            checkNotNull(date);
            final String result = LocalDates.toString(date);
            return result;
        }

        @Override
        protected LocalDate fromString(String str) {
            checkNotNull(str);
            final LocalDate date;
            try {
                date = LocalDates.parse(str);
            } catch (ParseException e) {
                throw wrappedCause(e);
            }
            return date;
        }

        @Override
        public String toString() {
            return "TimeStringifiers.forLocalDate()";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    /**
     * The default stringifier for {@code Duration}s.
     */
    private static class DurationStringifier extends Stringifier<Duration> {

        @Override
        protected String toString(Duration duration) {
            checkNotNull(duration);
            final String result = Durations.toString(duration);
            return result;
        }

        @Override
        protected Duration fromString(String str) {
            checkNotNull(str);
            final Duration result;
            try {
                result = Durations.parse(str);
            } catch (ParseException e) {
                throw wrappedCause(e);
            }
            return result;
        }

        private enum Singleton {
            INSTANCE;

            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final DurationStringifier value = new DurationStringifier();
        }

        private static DurationStringifier instance() {
            return Singleton.INSTANCE.value;
        }
    }

    /**
     * The stringifier of timestamps into RFC 3339 date string format.
     */
    private static class TimestampStringifier extends Stringifier<Timestamp> {

        @Override
        protected String toString(Timestamp obj) {
            return Timestamps.toString(obj);
        }

        @Override
        @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
        // It is OK because all necessary information from caught exception is passed.
        protected Timestamp fromString(String str) {
            try {
                return Timestamps.parse(str);
            } catch (ParseException e) {
                throw newIllegalArgumentException(e.getMessage(), e);
            }
        }
    }

    /**
     * The stringifier for web-safe representation of timestamps.
     *
     * <p>The stringifier replaces colons in the time part of a a RFC 3339 date string
     * with dashes when converting a timestamp to a string. It also restores the colons
     * back during the backward conversion.
     */
    static class WebSafeTimestampStringifer extends Stringifier<Timestamp> {

        private static final char COLON = ':';
        private static final Pattern PATTERN_COLON = Pattern.compile(String.valueOf(COLON));
        private static final String DASH = "-";

        /**
         * The index of a character separating hours and minutes.
         */
        private static final int HOUR_SEPARATOR_INDEX = 13;
        /**
         * The index of a character separating minutes and seconds.
         */
        private static final int MINUTE_SEPARATOR_INDEX = 16;

        @Override
        protected String toString(Timestamp timestamp) {
            String result = Timestamps.toString(timestamp);
            result = toWebSafe(result);
            return result;
        }

        @Override
        @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
        // It is OK because all necessary information from caught exception is passed.
        protected Timestamp fromString(String webSafe) {
            try {
                final String rfcStr = fromWebSafe(webSafe);
                return Timestamps.parse(rfcStr);
            } catch (ParseException e) {
                throw newIllegalArgumentException(e.getMessage(), e);
            }
        }

        /**
         * Converts the passed timestamp string into a web-safe string, replacing colons to dashes.
         */
        private static String toWebSafe(String str) {
            final String result = PATTERN_COLON.matcher(str)
                                               .replaceAll(DASH);
            return result;
        }

        /**
         * Converts the passed web-safe timestamp representation to the RFC 3339 date string format.
         */
        private static String fromWebSafe(String webSafe) {
            char[] chars = webSafe.toCharArray();
            chars[HOUR_SEPARATOR_INDEX] = COLON;
            chars[MINUTE_SEPARATOR_INDEX] = COLON;
            return String.valueOf(chars);
        }
    }
}
