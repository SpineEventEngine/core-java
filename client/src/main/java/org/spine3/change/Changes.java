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

package org.spine3.change;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with field changes.
 *
 * @author Alexander Yevsyukov
 * @author Alexander Aleksandrov
 */
@SuppressWarnings("OverlyCoupledClass") /* ... because we want one utility class for all the Changes classes. */
public class Changes {

    public interface ErrorMessage {
        String PREVIOUS_VALUE = "previousValue";
        String NEW_VALUE = "newValue";
        String ZONE_OFFSET = "zoneOffset";
        String LOCAL_DATE = "localDate";
        String OFFSET_DATE = "offsetDate";
        String LOCAL_TIME = "localTime";
        String OFFSET_TIME = "offsetTime";
        String OFFSET_DATE_TIME = "offsetDateTime";
        String VALUES_CANNOT_BE_EQUAL = "newValue cannot be equal to previousValue";
        String NEW_VALUE_CANNOT_BE_EMPTY = "newValue cannot be empty";
        String MUST_BE_A_POSITIVE_VALUE = "%s must be a positive value";
    }

    private Changes() {
    }

    /**
     * Creates {@link StringChange} object for the passed previous and new field values.
     *
     * <p>Passed values cannot be equal.
     */
    public static StringChange of(String previousValue, String newValue) {
        checkNotNull(previousValue, ErrorMessage.PREVIOUS_VALUE);
        checkNotNull(newValue, ErrorMessage.NEW_VALUE);
        checkArgument(!newValue.isEmpty(), ErrorMessage.NEW_VALUE_CANNOT_BE_EMPTY);
        checkArgument(!newValue.equals(previousValue), ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final StringChange result = StringChange.newBuilder()
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
        checkNotNull(previousValue, ErrorMessage.PREVIOUS_VALUE);
        checkNotNull(newValue, ErrorMessage.NEW_VALUE);
        checkArgument(!newValue.equals(previousValue), ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final TimestampChange result = TimestampChange.newBuilder()
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
        checkArgument(Double.compare(newValue, previousValue) != 0, ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final DoubleChange result = DoubleChange.newBuilder()
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
        checkArgument(Float.compare(newValue, previousValue) != 0, ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final FloatChange result = FloatChange.newBuilder()
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
        checkArgument(Integer.compare(newValue, previousValue) != 0, ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final Int32Change result = Int32Change.newBuilder()
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
        checkArgument(Long.compare(newValue, previousValue) != 0, ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final Int64Change result = Int64Change.newBuilder()
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
        checkArgument(Integer.compare(newValue, previousValue) != 0, ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final UInt32Change result = UInt32Change.newBuilder()
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
        checkArgument(Long.compare(newValue, previousValue) != 0, ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final UInt64Change result = UInt64Change.newBuilder()
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
        checkArgument(Integer.compare(newValue, previousValue) != 0, ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final SInt32Change result = SInt32Change.newBuilder()
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
        checkArgument(Long.compare(newValue, previousValue) != 0, ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final SInt64Change result = SInt64Change.newBuilder()
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
        checkArgument(Integer.compare(newValue, previousValue) != 0, ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final Fixed32Change result = Fixed32Change.newBuilder()
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
        checkArgument(Long.compare(newValue, previousValue) != 0, ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final Fixed64Change result = Fixed64Change.newBuilder()
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
        checkArgument(Integer.compare(newValue, previousValue) != 0, ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final Sfixed32Change result = Sfixed32Change.newBuilder()
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
        checkArgument(Long.compare(newValue, previousValue) != 0, ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final Sfixed64Change result = Sfixed64Change.newBuilder()
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
        checkNotNull(previousValue, ErrorMessage.PREVIOUS_VALUE);
        checkNotNull(newValue, ErrorMessage.NEW_VALUE);
        checkArgument(!newValue.isEmpty(), ErrorMessage.NEW_VALUE_CANNOT_BE_EMPTY);
        checkArgument(!newValue.equals(previousValue), ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final BytesChange result = BytesChange.newBuilder()
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
        checkArgument(Boolean.compare(newValue, previousValue) != 0, ErrorMessage.VALUES_CANNOT_BE_EQUAL);

        final BooleanChange result = BooleanChange.newBuilder()
                                                  .setPreviousValue(previousValue)
                                                  .setNewValue(newValue)
                                                  .build();
        return result;
    }
}
