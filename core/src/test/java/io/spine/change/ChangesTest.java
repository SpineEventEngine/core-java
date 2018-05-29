/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.change;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.test.TimeTests;
import io.spine.time.Interval;
import io.spine.time.Intervals;
import io.spine.time.LocalDate;
import io.spine.time.LocalDates;
import io.spine.time.LocalTime;
import io.spine.time.LocalTimes;
import io.spine.time.OffsetDate;
import io.spine.time.OffsetDateTime;
import io.spine.time.OffsetDateTimes;
import io.spine.time.OffsetDates;
import io.spine.time.OffsetTime;
import io.spine.time.OffsetTimes;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.test.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.time.Durations2.minutes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"ConstantConditions" /* We pass `null` to some of the methods to check handling
                                        of preconditions */,
                   "ResultOfMethodCallIgnored" /* ...when methods throw exceptions */,
                   "ClassWithTooManyMethods",
                   "OverlyCoupledClass" /* we test many data types and utility methods */,
                   "InnerClassMayBeStatic" /* JUnit 5 Nested classes cannot be static */})
@DisplayName("Changes utility should")
class ChangesTest {

    private static final String ERR_PREVIOUS_VALUE_CANNOT_BE_NULL =
            "do_not_accept_null_previousValue";
    private static final String ERR_NEW_VALUE_CANNOT_BE_NULL =
            "do_not_accept_null_newValue";
    private static final String ERR_VALUES_CANNOT_BE_EQUAL =
            "do_not_accept_equal_values";

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(Changes.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(ByteString.class, ByteString.EMPTY)
                .setDefault(Timestamp.class, Time.getCurrentTime())
                .setDefault(OffsetTime.class, OffsetTimes.now(ZoneOffsets.UTC))
                .setDefault(OffsetDate.class, OffsetDates.now(ZoneOffsets.UTC))
                .setDefault(OffsetDateTime.class, OffsetDateTimes.now(ZoneOffsets.UTC))
                .setDefault(LocalDate.class, LocalDates.now())
                .setDefault(LocalTime.class, LocalTimes.now())
                .setDefault(Interval.class,
                            Intervals.between(subtract(getCurrentTime(), minutes(1)),
                                              getCurrentTime()))
                .testAllPublicStaticMethods(Changes.class);
    }

    @Nested
    @DisplayName("when creating value change")
    class CreateChangeTest {

        @Test
        @DisplayName("successfully create instance for string values")
        void createStringChange() {
            final String previousValue = randomUuid();
            final String newValue = randomUuid();

            final StringChange result = Changes.of(previousValue, newValue);

            assertEquals(previousValue, result.getPreviousValue());
            assertEquals(newValue, result.getNewValue());
        }

        @Test
        @DisplayName("successfully create instance for byte string values")
        void createByteStringChange() {
            final ByteString previousValue = ByteString.copyFromUtf8(randomUuid());
            final ByteString newValue = ByteString.copyFromUtf8(randomUuid());

            final BytesChange result = Changes.of(previousValue, newValue);

            assertEquals(previousValue, result.getPreviousValue());
            assertEquals(newValue, result.getNewValue());
        }

        @Test
        @DisplayName("successfully create instance for Timestamp values")
        void createTimestampChange() {
            final Timestamp fiveMinutesAgo = TimeTests.Past.minutesAgo(5);
            final Timestamp now = getCurrentTime();

            final TimestampChange result = Changes.of(fiveMinutesAgo, now);

            assertEquals(fiveMinutesAgo, result.getPreviousValue());
            assertEquals(now, result.getNewValue());
        }

        @Test
        @DisplayName("successfully create instance for boolean values")
        void createBooleanChange() {
            final boolean s1 = true;
            final boolean s2 = false;

            final BooleanChange result = Changes.of(s1, s2);

            assertTrue(Boolean.compare(s1, result.getPreviousValue()) == 0);
            assertTrue(Boolean.compare(s2, result.getNewValue()) == 0);
        }

        @Test
        @DisplayName("successfully create instance for double values")
        void createDoubleChange() {
            final double s1 = 1957.1004;
            final double s2 = 1957.1103;

            final DoubleChange result = Changes.of(s1, s2);

            assertTrue(Double.compare(s1, result.getPreviousValue()) == 0);
            assertTrue(Double.compare(s2, result.getNewValue()) == 0);
        }

        @Test
        @DisplayName("successfully create instance for int64 values")
        void createInt64Change() {
            final long s1 = 16420225L;
            final long s2 = 17270320L;

            final Int64Change result = Changes.ofInt64(s1, s2);

            assertTrue(Long.compare(s1, result.getPreviousValue()) == 0);
            assertTrue(Long.compare(s2, result.getNewValue()) == 0);
        }

        @Test
        @DisplayName("successfully create instance for int32 values")
        void createInt32Change() {
            final int s1 = 1550;
            final int s2 = 1616;

            final Int32Change result = Changes.ofInt32(s1, s2);

            assertTrue(Integer.compare(s1, result.getPreviousValue()) == 0);
            assertTrue(Integer.compare(s2, result.getNewValue()) == 0);
        }

        @Test
        @DisplayName("successfully create instance for float values")
        void createFloatChange() {
            final float s1 = 1473.0219f;
            final float s2 = 1543.0524f;

            final FloatChange result = Changes.of(s1, s2);

            assertTrue(Float.compare(s1, result.getPreviousValue()) == 0);
            assertTrue(Float.compare(s2, result.getNewValue()) == 0);
        }

        @Test
        @DisplayName("successfully create instance for uint32 values")
        void createUint32Change() {
            final int s1 = 16440925;
            final int s2 = 17100919;

            final UInt32Change result = Changes.ofUInt32(s1, s2);

            assertTrue(Integer.compare(s1, result.getPreviousValue()) == 0);
            assertTrue(Integer.compare(s2, result.getNewValue()) == 0);
        }

        @Test
        @DisplayName("successfully create instance for uint64 values")
        void createUint64Change() {
            final long s1 = 16290414L;
            final long s2 = 16950708L;

            final UInt64Change result = Changes.ofUInt64(s1, s2);

            assertTrue(Long.compare(s1, result.getPreviousValue()) == 0);
            assertTrue(Long.compare(s2, result.getNewValue()) == 0);
        }

        @Test
        @DisplayName("successfully create instance for sint32 values")
        void createSint32Change() {
            final int s1 = 16550106;
            final int s2 = 17050816;

            final SInt32Change result = Changes.ofSInt32(s1, s2);

            assertTrue(Integer.compare(s1, result.getPreviousValue()) == 0);
            assertTrue(Integer.compare(s2, result.getNewValue()) == 0);
        }

        @Test
        @DisplayName("successfully create instance for sint64 values")
        void createSint64Change() {
            final long s1 = 1666L;
            final long s2 = 1736L;

            final SInt64Change result = Changes.ofSInt64(s1, s2);

            assertTrue(Long.compare(s1, result.getPreviousValue()) == 0);
            assertTrue(Long.compare(s2, result.getNewValue()) == 0);
        }

        @Test
        @DisplayName("successfully create instance for fixed32 values")
        void createFixed32Change() {
            final int s1 = 17070415;
            final int s2 = 17830918;

            final Fixed32Change result = Changes.ofFixed32(s1, s2);

            assertTrue(Integer.compare(s1, result.getPreviousValue()) == 0);
            assertTrue(Integer.compare(s2, result.getNewValue()) == 0);
        }

        @Test
        @DisplayName("successfully create instance for fixed64 values")
        void createFixed64Change() {
            final long s1 = 17240422L;
            final long s2 = 18040212L;

            final Fixed64Change result = Changes.ofFixed64(s1, s2);

            assertTrue(Long.compare(s1, result.getPreviousValue()) == 0);
            assertTrue(Long.compare(s2, result.getNewValue()) == 0);
        }

        @Test
        @DisplayName("successfully create instance for sfixed32 values")
        void createSfixed32Change() {
            final int s1 = 1550;
            final int s2 = 1616;

            final Sfixed32Change result = Changes.ofSfixed32(s1, s2);

            assertTrue(Integer.compare(s1, result.getPreviousValue()) == 0);
            assertTrue(Integer.compare(s2, result.getNewValue()) == 0);
        }

        @Test
        @DisplayName("successfully create instance for sfixed64 values")
        void createSfixed64Change() {
            final long s1 = 16420225L;
            final long s2 = 17270320L;

            final Sfixed64Change result = Changes.ofSfixed64(s1, s2);

            assertTrue(Long.compare(s1, result.getPreviousValue()) == 0);
            assertTrue(Long.compare(s2, result.getNewValue()) == 0);
        }

        @Test
        @DisplayName("successfully create instance for Interval values")
        void createIntervalChange() {
            final Timestamp fiveMinutesAgo = TimeTests.Past.minutesAgo(5);
            final Timestamp fourMinutesAgo = TimeTests.Past.minutesAgo(4);
            final Timestamp now = getCurrentTime();
            final Interval fourMinutes = Intervals.between(fourMinutesAgo, now);
            final Interval fiveMinutes = Intervals.between(fiveMinutesAgo, now);

            final IntervalChange result = Changes.of(fourMinutes, fiveMinutes);

            assertEquals(fourMinutes, result.getPreviousValue());
            assertEquals(fiveMinutes, result.getNewValue());
        }

        @Test
        @DisplayName("successfully create instance for LocalDate values")
        void createLocalDateChange() {
            final LocalDate today = LocalDates.now();
            final LocalDate tomorrow = LocalDates.addDays(today, 1);

            final LocalDateChange result = Changes.of(today, tomorrow);

            assertEquals(today, result.getPreviousValue());
            assertEquals(tomorrow, result.getNewValue());
        }

        @Test
        @DisplayName("successfully create instance for LocalTime values")
        void createLocalTimeChange() {
            final LocalTime now = LocalTimes.now();
            final LocalTime inFiveHours = LocalTimes.addHours(now, 5);

            final LocalTimeChange result = Changes.of(now, inFiveHours);

            assertEquals(now, result.getPreviousValue());
            assertEquals(inFiveHours, result.getNewValue());
        }

        @Test
        @DisplayName("successfully create instance for OffsetDate values")
        void createOffsetDateChange() {
            final ZoneOffset inKiev = ZoneOffsets.ofHours(3);
            final ZoneOffset inLuxembourg = ZoneOffsets.ofHours(1);
            final OffsetDate previousDate = OffsetDates.now(inKiev);
            final OffsetDate newDate = OffsetDates.now(inLuxembourg);

            final OffsetDateChange result = Changes.of(previousDate, newDate);

            assertEquals(previousDate, result.getPreviousValue());
            assertEquals(newDate, result.getNewValue());
        }

        @Test
        @DisplayName("successfully create instance for OffsetTime values")
        void createOffsetTimeChange() {
            final ZoneOffset inKiev = ZoneOffsets.ofHours(3);
            final ZoneOffset inLuxembourg = ZoneOffsets.ofHours(1);
            final OffsetTime previousTime = OffsetTimes.now(inKiev);
            final OffsetTime newTime = OffsetTimes.now(inLuxembourg);

            final OffsetTimeChange result = Changes.of(previousTime, newTime);

            assertEquals(previousTime, result.getPreviousValue());
            assertEquals(newTime, result.getNewValue());
        }

        @Test
        @DisplayName("successfully create instance for OffsetDateTime values")
        void createOffsetDateTimeChange() {
            final ZoneOffset inKiev = ZoneOffsets.ofHours(3);
            final ZoneOffset inLuxembourg = ZoneOffsets.ofHours(1);
            final OffsetDateTime previousDateTime = OffsetDateTimes.now(inKiev);
            final OffsetDateTime newDateTime = OffsetDateTimes.now(inLuxembourg);

            final OffsetDateTimeChange result = Changes.of(previousDateTime, newDateTime);

            assertEquals(previousDateTime, result.getPreviousValue());
            assertEquals(newDateTime, result.getNewValue());
        }

        private String randomUuid() {
            return UUID.randomUUID()
                       .toString();
        }
    }

    @Nested
    @DisplayName("when given equal values")
    class CreateFromEqualTest {

        @Test
        @DisplayName("not create string change")
        void notAcceptEqualStrings() {
            final String value = ERR_VALUES_CANNOT_BE_EQUAL;
            assertThrows(IllegalArgumentException.class, () -> Changes.of(value, value));
        }

        @Test
        @DisplayName("not create byte string change")
        void notAcceptEqualByteStrings() {
            final ByteString value = ByteString.copyFromUtf8(ERR_VALUES_CANNOT_BE_EQUAL);
            assertThrows(IllegalArgumentException.class, () -> Changes.of(value, value));
        }

        @Test
        @DisplayName("not create Timestamp change")
        void notAcceptEqualTimestamps() {
            final Timestamp now = getCurrentTime();
            assertThrows(IllegalArgumentException.class, () -> Changes.of(now, now));
        }

        @Test
        @DisplayName("not create boolean change")
        void notAcceptEqualBooleans() {
            final boolean value = true;
            assertThrows(IllegalArgumentException.class, () -> Changes.of(value, value));
        }

        @Test
        @DisplayName("not create double change")
        void notAcceptEqualDoubles() {
            final double value = 1961.0412;
            assertThrows(IllegalArgumentException.class, () -> Changes.of(value, value));
        }

        @Test
        @DisplayName("not create float change")
        void notAcceptEqualFloats() {
            final float value = 1543.0f;
            assertThrows(IllegalArgumentException.class, () -> Changes.of(value, value));
        }

        @Test
        @DisplayName("not create int32 change")
        void notAcceptEqualInt32() {
            final int value = 1614;
            assertThrows(IllegalArgumentException.class, () -> Changes.of(value, value));
        }

        @Test
        @DisplayName("not create int64 change")
        void notAcceptEqualInt64() {
            final long value = 1666L;
            assertThrows(IllegalArgumentException.class, () -> Changes.of(value, value));
        }

        @Test
        @DisplayName("not create uint32 change")
        void notAcceptEqualUint32() {
            final int value = 1776;
            assertThrows(IllegalArgumentException.class, () -> Changes.ofUInt32(value, value));
        }

        @Test
        @DisplayName("not create uint64 change")
        void notAcceptEqualUint64() {
            final long value = 1690L;
            assertThrows(IllegalArgumentException.class, () -> Changes.ofUInt64(value, value));
        }

        @Test
        @DisplayName("not create sint32 change")
        void notAcceptEqualSint32() {
            final int value = 1694;
            assertThrows(IllegalArgumentException.class, () -> Changes.ofSInt32(value, value));
        }

        @Test
        @DisplayName("not create sint64 change")
        void notAcceptEqualSint64() {
            final long value = 1729L;
            assertThrows(IllegalArgumentException.class, () -> Changes.ofSInt64(value, value));
        }

        @Test
        @DisplayName("not create fixed32 change")
        void notAcceptEqualFixed32() {
            final int value = 1736;
            assertThrows(IllegalArgumentException.class, () -> Changes.ofFixed32(value, value));
        }

        @Test
        @DisplayName("not create fixed64 change")
        void notAcceptEqualFixed64() {
            final long value = 1755L;
            assertThrows(IllegalArgumentException.class, () -> Changes.ofFixed64(value, value));
        }

        @Test
        @DisplayName("not create sfixed32 change")
        void notAcceptEqualSfixed32() {
            final int value = 1614;
            assertThrows(IllegalArgumentException.class, () -> Changes.ofSfixed32(value, value));
        }

        @Test
        @DisplayName("not create sfixed64 change")
        void notAcceptEqualSfixed64() {
            final long value = 1666L;
            assertThrows(IllegalArgumentException.class, () -> Changes.ofSfixed64(value, value));
        }

        @Test
        @DisplayName("not create Interval change")
        void notAcceptEqualIntervals() {
            final Timestamp fourMinutesAgo = TimeTests.Past.minutesAgo(4);
            final Timestamp now = getCurrentTime();
            assertThrows(IllegalArgumentException.class, () -> {
                final Interval fourMinutes = Intervals.between(now, fourMinutesAgo);
                Changes.of(fourMinutes, fourMinutes);
            });
        }

        @Test
        @DisplayName("not create LocalDate change")
        void notAcceptEqualLocalDates() {
            final LocalDate today = LocalDates.now();
            assertThrows(IllegalArgumentException.class, () -> Changes.of(today, today));
        }

        @Test
        @DisplayName("not create LocalTime change")
        void notAcceptEqualLocalTimes() {
            final LocalTime now = LocalTimes.now();
            assertThrows(IllegalArgumentException.class, () -> Changes.of(now, now));
        }

        @Test
        @DisplayName("not create OffsetDate change")
        void notAcceptEqualOffsetDate() {
            final ZoneOffset inLuxembourg = ZoneOffsets.ofHours(1);
            final OffsetDate date = OffsetDates.now(inLuxembourg);
            assertThrows(IllegalArgumentException.class, () -> Changes.of(date, date));
        }

        @Test
        @DisplayName("not create OffsetTime change")
        void notAcceptEqualOffsetTimes() {
            final ZoneOffset inLuxembourg = ZoneOffsets.ofHours(1);
            final OffsetTime now = OffsetTimes.now(inLuxembourg);
            assertThrows(IllegalArgumentException.class, () -> Changes.of(now, now));
        }

        @Test
        @DisplayName("not create OffsetDateTime change")
        void notAcceptEqualOffsetDateTimes() {
            final ZoneOffset inLuxembourg = ZoneOffsets.ofHours(1);
            final OffsetDateTime now = OffsetDateTimes.now(inLuxembourg);
            assertThrows(IllegalArgumentException.class, () -> Changes.of(now, now));
        }
    }

    @Nested
    @DisplayName("when given null values")
    class CreateFromNullTest {

        @Test
        @DisplayName("not accept null String previousValue")
        void notAcceptNullStringPrevious() {
            assertThrows(NullPointerException.class,
                         () -> Changes.of(null, ERR_PREVIOUS_VALUE_CANNOT_BE_NULL));
        }

        @Test
        @DisplayName("not accept null String newValue")
        void notAcceptNullStringNew() {
            assertThrows(NullPointerException.class,
                         () -> Changes.of(ERR_NEW_VALUE_CANNOT_BE_NULL, null));
        }

        @Test
        @DisplayName("not accept null ByteString previousValue")
        void notAcceptNullByteStringPrevious() {
            assertThrows(NullPointerException.class,
                         () -> Changes.of(null,
                                          ByteString.copyFromUtf8(ERR_PREVIOUS_VALUE_CANNOT_BE_NULL)));
        }

        @Test
        @DisplayName("not accept null ByteString newValue")
        void notAcceptNullByteStringNew() {
            assertThrows(NullPointerException.class,
                         () -> Changes.of(ByteString.copyFromUtf8(ERR_NEW_VALUE_CANNOT_BE_NULL), null));
        }

        @Test
        @DisplayName("not accept null Timestamp previousValue")
        void notAcceptNullTimestampPrevious() {
            assertThrows(NullPointerException.class, () -> Changes.of(null, getCurrentTime()));
        }

        @Test
        @DisplayName("not accept null Timestamp newValue")
        void notAcceptNullTimestampNew() {
            assertThrows(NullPointerException.class, () -> Changes.of(getCurrentTime(), null));
        }

        @Test
        @DisplayName("not accept null Interval previousValue")
        void notAcceptNullIntervalPrevious() {
            final Timestamp fourMinutesAgo = TimeTests.Past.minutesAgo(4);
            final Timestamp now = getCurrentTime();
            final Interval fourMinutes = Intervals.between(fourMinutesAgo, now);
            assertThrows(NullPointerException.class, () -> Changes.of(null, fourMinutes));
        }

        @Test
        @DisplayName("not accept null Interval newValue")
        void notAcceptNullIntervalNew() {
            final Timestamp fourMinutesAgo = TimeTests.Past.minutesAgo(4);
            final Timestamp now = getCurrentTime();
            final Interval fourMinutes = Intervals.between(fourMinutesAgo, now);
            assertThrows(NullPointerException.class, () -> Changes.of(fourMinutes, null));
        }

        @Test
        @DisplayName("not accept null LocalDate previousValue")
        void notAcceptNullLocalDatePrevious() {
            final LocalDate today = LocalDates.now();
            assertThrows(NullPointerException.class, () -> Changes.of(null, today));
        }

        @Test
        @DisplayName("not accept null LocalDate newValue")
        void notAcceptNullLocalDateNew() {
            final LocalDate today = LocalDates.now();
            assertThrows(NullPointerException.class, () -> Changes.of(today, null));
        }

        @Test
        @DisplayName("not accept null LocalTime previousValue")
        void notAcceptNullLocalTimePrevious() {
            final LocalTime now = LocalTimes.now();
            assertThrows(NullPointerException.class, () -> Changes.of(null, now));
        }

        @Test
        @DisplayName("not accept null LocalTime newValue")
        void notAcceptNullLocalTimeNew() {
            final LocalTime now = LocalTimes.now();
            assertThrows(NullPointerException.class, () -> Changes.of(now, null));
        }

        @Test
        @DisplayName("not accept null OffsetDate previousValue")
        void notAcceptNullOffsetDatePrevious() {
            final ZoneOffset inLassVegas = ZoneOffsets.ofHours(8);
            final OffsetDate date = OffsetDates.now(inLassVegas);
            assertThrows(NullPointerException.class, () -> Changes.of(null, date));
        }

        @Test
        @DisplayName("not accept null OffsetDate newValue")
        void notAcceptNullOffsetDateNew() {
            final ZoneOffset inKiev = ZoneOffsets.ofHours(3);
            final OffsetDate date = OffsetDates.now(inKiev);
            assertThrows(NullPointerException.class, () -> Changes.of(date, null));
        }

        @Test
        @DisplayName("not accept null OffsetTime previousValue")
        void notAcceptNullOffsetTimePrevious() {
            final ZoneOffset inLassVegas = ZoneOffsets.ofHours(8);
            final OffsetTime now = OffsetTimes.now(inLassVegas);
            assertThrows(NullPointerException.class, () -> Changes.of(null, now));
        }

        @Test
        @DisplayName("not accept null OffsetTime newValue")
        void notAcceptNullOffsetTimeNew() {
            final ZoneOffset inKiev = ZoneOffsets.ofHours(3);
            final OffsetTime now = OffsetTimes.now(inKiev);
            assertThrows(NullPointerException.class, () -> Changes.of(now, null));
        }

        @Test
        @DisplayName("not accept null OffsetDateTime previousValue")
        void notAcceptNullOffsetDateTimePrevious() {
            final ZoneOffset inLassVegas = ZoneOffsets.ofHours(8);
            final OffsetDateTime now = OffsetDateTimes.now(inLassVegas);
            assertThrows(NullPointerException.class, () -> Changes.of(null, now));
        }

        @Test
        @DisplayName("not accept null OffsetDateTime newValue")
        void notAcceptNullOffsetDateTimeNew() {
            final ZoneOffset inKiev = ZoneOffsets.ofHours(3);
            final OffsetDateTime now = OffsetDateTimes.now(inKiev);
            assertThrows(NullPointerException.class, () -> Changes.of(now, null));
        }
    }
}
