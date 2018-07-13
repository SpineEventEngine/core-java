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

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Experimental;
import io.spine.time.Interval;
import io.spine.time.LocalDate;
import io.spine.time.LocalTime;
import io.spine.time.OffsetDate;
import io.spine.time.OffsetDateTime;
import io.spine.time.OffsetTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.change.Preconditions2.checkNewValueNotEmpty;
import static io.spine.change.Preconditions2.checkNotEqual;

/**
 * Utility class for working with field changes.
 *
 * @author Alexander Yevsyukov
 * @author Alexander Aleksandrov
 */
@SuppressWarnings("OverlyCoupledClass")
    /* ... because we want one utility class for all the Changes classes. */
public final class Changes {

    private Changes() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates {@link StringChange} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static StringChange of(String previousValue, String newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNewValueNotEmpty(newValue);
        checkNotEqual(previousValue, newValue);

        StringChange result = StringChange.newBuilder()
                                                .setPreviousValue(previousValue)
                                                .setNewValue(newValue)
                                                .build();
        return result;
    }

    /**
     * Creates {@link TimestampChange} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static TimestampChange of(Timestamp previousValue, Timestamp newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNotEqual(previousValue, newValue);

        TimestampChange result = TimestampChange.newBuilder()
                                                      .setPreviousValue(previousValue)
                                                      .setNewValue(newValue)
                                                      .build();
        return result;
    }

    /**
     * Creates {@link DoubleChange} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static DoubleChange of(double previousValue, double newValue) {
        checkNotEqual(previousValue, newValue);

        DoubleChange result = DoubleChange.newBuilder()
                                                .setPreviousValue(previousValue)
                                                .setNewValue(newValue)
                                                .build();
        return result;
    }

    /**
     * Creates {@link FloatChange} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static FloatChange of(float previousValue, float newValue) {
        checkNotEqual(previousValue, newValue);

        FloatChange result = FloatChange.newBuilder()
                                              .setPreviousValue(previousValue)
                                              .setNewValue(newValue)
                                              .build();
        return result;
    }

    /**
     * Creates {@link Int32Change} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static Int32Change ofInt32(int previousValue, int newValue) {
        checkNotEqual(previousValue, newValue);

        Int32Change result = Int32Change.newBuilder()
                                              .setPreviousValue(previousValue)
                                              .setNewValue(newValue)
                                              .build();
        return result;
    }

    /**
     * Creates {@link Int64Change} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static Int64Change ofInt64(long previousValue, long newValue) {
        checkNotEqual(previousValue, newValue);

        Int64Change result = Int64Change.newBuilder()
                                              .setPreviousValue(previousValue)
                                              .setNewValue(newValue)
                                              .build();
        return result;
    }

    /**
     * Creates {@link UInt32Change} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static UInt32Change ofUInt32(int previousValue, int newValue) {
        checkNotEqual(previousValue, newValue);

        UInt32Change result = UInt32Change.newBuilder()
                                                .setPreviousValue(previousValue)
                                                .setNewValue(newValue)
                                                .build();
        return result;
    }

    /**
     * Creates {@link UInt64Change} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static UInt64Change ofUInt64(long previousValue, long newValue) {
        checkNotEqual(previousValue, newValue);

        UInt64Change result = UInt64Change.newBuilder()
                                                .setPreviousValue(previousValue)
                                                .setNewValue(newValue)
                                                .build();
        return result;
    }

    /**
     * Creates {@link SInt32Change} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static SInt32Change ofSInt32(int previousValue, int newValue) {
        checkNotEqual(previousValue, newValue);

        SInt32Change result = SInt32Change.newBuilder()
                                                .setPreviousValue(previousValue)
                                                .setNewValue(newValue)
                                                .build();
        return result;
    }

    /**
     * Creates {@link SInt64Change} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static SInt64Change ofSInt64(long previousValue, long newValue) {
        checkNotEqual(previousValue, newValue);

        SInt64Change result = SInt64Change.newBuilder()
                                                .setPreviousValue(previousValue)
                                                .setNewValue(newValue)
                                                .build();
        return result;
    }

    /**
     * Creates {@link Fixed32Change} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static Fixed32Change ofFixed32(int previousValue, int newValue) {
        checkNotEqual(previousValue, newValue);

        Fixed32Change result = Fixed32Change.newBuilder()
                                                  .setPreviousValue(previousValue)
                                                  .setNewValue(newValue)
                                                  .build();
        return result;
    }

    /**
     * Creates {@link Fixed64Change} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static Fixed64Change ofFixed64(long previousValue, long newValue) {
        checkNotEqual(previousValue, newValue);

        Fixed64Change result = Fixed64Change.newBuilder()
                                                  .setPreviousValue(previousValue)
                                                  .setNewValue(newValue)
                                                  .build();
        return result;
    }

    /**
     * Creates {@link Sfixed32Change} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static Sfixed32Change ofSfixed32(int previousValue, int newValue) {
        checkNotEqual(previousValue, newValue);

        Sfixed32Change result = Sfixed32Change.newBuilder()
                                                    .setPreviousValue(previousValue)
                                                    .setNewValue(newValue)
                                                    .build();
        return result;
    }

    /**
     * Creates {@link Sfixed64Change} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static Sfixed64Change ofSfixed64(long previousValue, long newValue) {
        checkNotEqual(previousValue, newValue);

        Sfixed64Change result = Sfixed64Change.newBuilder()
                                                    .setPreviousValue(previousValue)
                                                    .setNewValue(newValue)
                                                    .build();
        return result;
    }

    /**
     * Creates {@link BytesChange} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static BytesChange of(ByteString previousValue, ByteString newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNewValueNotEmpty(newValue);
        checkNotEqual(previousValue, newValue);

        BytesChange result = BytesChange.newBuilder()
                                              .setPreviousValue(previousValue)
                                              .setNewValue(newValue)
                                              .build();
        return result;
    }

    /**
     * Creates {@link BooleanChange} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static BooleanChange of(boolean previousValue, boolean newValue) {
        checkNotEqual(previousValue, newValue);

        BooleanChange result = BooleanChange.newBuilder()
                                                  .setPreviousValue(previousValue)
                                                  .setNewValue(newValue)
                                                  .build();
        return result;
    }

    /**
     * Creates {@link IntervalChange} object for the passed previous and new field values of
     * time interval.
     *
     * <p>Passed values cannot be equal.
     */
    @Experimental
    public static IntervalChange of(Interval previousValue, Interval newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNotEqual(previousValue, newValue);

        IntervalChange result = IntervalChange.newBuilder()
                                                    .setPreviousValue(previousValue)
                                                    .setNewValue(newValue)
                                                    .build();
        return result;
    }

    /**
     * Creates {@link LocalDateChange} object for the passed previous and new field values of
     * local date.
     *
     * <p>Passed values cannot be equal.
     */
    public static LocalDateChange of(LocalDate previousValue, LocalDate newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNotEqual(previousValue, newValue);

        LocalDateChange result = LocalDateChange.newBuilder()
                                                      .setPreviousValue(previousValue)
                                                      .setNewValue(newValue)
                                                      .build();
        return result;
    }

    /**
     * Creates {@link LocalTimeChange} object for the passed previous and new field values of
     * local time.
     *
     * <p>Passed values cannot be equal.
     */
    public static LocalTimeChange of(LocalTime previousValue, LocalTime newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNotEqual(previousValue, newValue);

        LocalTimeChange result = LocalTimeChange.newBuilder()
                                                      .setPreviousValue(previousValue)
                                                      .setNewValue(newValue)
                                                      .build();
        return result;
    }

    /**
     * Creates {@link OffsetTimeChange} object for the passed previous and new field values of
     * offset time.
     *
     * <p>Passed values cannot be equal.
     */
    public static OffsetTimeChange of(OffsetTime previousValue, OffsetTime newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNotEqual(previousValue, newValue);

        OffsetTimeChange result = OffsetTimeChange.newBuilder()
                                                        .setPreviousValue(previousValue)
                                                        .setNewValue(newValue)
                                                        .build();
        return result;
    }

    /**
     * Creates {@link OffsetDateChange} object for the passed previous and new field values of
     * offset time.
     *
     * <p>Passed values cannot be equal.
     */
    public static OffsetDateChange of(OffsetDate previousValue, OffsetDate newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNotEqual(previousValue, newValue);

        OffsetDateChange result = OffsetDateChange.newBuilder()
                                                        .setPreviousValue(previousValue)
                                                        .setNewValue(newValue)
                                                        .build();
        return result;
    }

    /**
     * Creates {@link OffsetDateTimeChange} object for the passed previous and new field values of
     * offset time.
     *
     * <p>Passed values cannot be equal.
     */
    public static OffsetDateTimeChange of(OffsetDateTime previousValue, OffsetDateTime newValue) {
        checkNotNull(previousValue);
        checkNotNull(newValue);
        checkNotEqual(previousValue, newValue);

        OffsetDateTimeChange result = OffsetDateTimeChange.newBuilder()
                                                                .setPreviousValue(previousValue)
                                                                .setNewValue(newValue)
                                                                .build();
        return result;
    }
}
