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

package org.spine3.server.validate;

import com.google.protobuf.ByteString;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.protobuf.Durations;
import org.spine3.test.validation.DigitsCountNumberFieldValue;
import org.spine3.test.validation.MaxIncNumberFieldValue;
import org.spine3.test.validation.MaxNotIncNumberFieldValue;
import org.spine3.test.validation.MinIncNumberFieldValue;
import org.spine3.test.validation.MinNotIncNumberFieldValue;
import org.spine3.test.validation.NotRequiredMsgFieldValue;
import org.spine3.test.validation.RepeatedRequiredMsgFieldValue;
import org.spine3.test.validation.RequiredByteStringFieldValue;
import org.spine3.test.validation.RequiredMsgFieldValue;
import org.spine3.test.validation.RequiredStringFieldValue;
import org.spine3.test.validation.TimeInFutureFieldValue;
import org.spine3.test.validation.TimeInPastFieldValue;
import org.spine3.test.validation.TimeWithoutOptsFieldValue;

import static com.google.protobuf.util.TimeUtil.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public class MessageValidatorShould {

    private static final double EQUAL_MIN = 16.5;
    private static final double BIGGER_THAN_MIN = EQUAL_MIN + 5;
    private static final double LESS_THAN_MIN = EQUAL_MIN - 5;

    private static final double EQUAL_MAX = 64.5;
    private static final double BIGGER_THAN_MAX = EQUAL_MAX + 5;
    private static final double LESS_THAN_MAX = EQUAL_MAX - 5;

    private static final double INT_DIGITS_COUNT_BIGGER_THAN_MAX = 123.5;
    private static final double INT_DIGITS_COUNT_EQUAL_MAX = 12.5;
    private static final double INT_DIGITS_COUNT_LESS_THAN_MAX = 1.5;

    private static final double FRACTIONAL_DIGITS_COUNT_BIGGER_THAN_MAX = 1.123;
    private static final double FRACTIONAL_DIGITS_COUNT_EQUAL_MAX = 1.12;
    private static final double FRACTIONAL_DIGITS_COUNT_LESS_THAN_MAX = 1.0;

    private final MessageValidator validator = new MessageValidator();

    /**
     * Required option tests.
     */

    @Test
    public void find_out_that_required_Message_field_is_set() {
        final RequiredMsgFieldValue validMsg = RequiredMsgFieldValue.newBuilder().setValue(newStringValue()).build();
        validator.validate(validMsg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_required_Message_field_is_NOT_set() {
        final RequiredMsgFieldValue invalidMsg = RequiredMsgFieldValue.getDefaultInstance();
        validator.validate(invalidMsg);
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_required_String_field_is_set() {
        final RequiredStringFieldValue validMsg = RequiredStringFieldValue.newBuilder().setValue(newUuid()).build();
        validator.validate(validMsg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_required_String_field_is_NOT_set() {
        final RequiredStringFieldValue invalidMsg = RequiredStringFieldValue.getDefaultInstance();
        validator.validate(invalidMsg);
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_required_ByteString_field_is_set() {
        final ByteString byteString = ByteString.copyFromUtf8(newUuid());
        final RequiredByteStringFieldValue validMsg = RequiredByteStringFieldValue.newBuilder().setValue(byteString).build();
        validator.validate(validMsg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_required_ByteString_field_is_NOT_set() {
        final RequiredByteStringFieldValue invalidMsg = RequiredByteStringFieldValue.getDefaultInstance();
        validator.validate(invalidMsg);
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_repeated_required_field_has_valid_values() {
        final RepeatedRequiredMsgFieldValue invalidMsg = RepeatedRequiredMsgFieldValue.newBuilder()
                .addValue(newStringValue())
                .addValue(newStringValue())
                .build();
        validator.validate(invalidMsg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_repeated_required_field_has_no_values() {
        validator.validate(RepeatedRequiredMsgFieldValue.getDefaultInstance());
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_repeated_required_field_has_empty_value() {
        final RepeatedRequiredMsgFieldValue invalidMsg = RepeatedRequiredMsgFieldValue.newBuilder()
                .addValue(newStringValue()) // valid value
                .addValue(StringValue.getDefaultInstance()) // invalid value
                .build();
        validator.validate(invalidMsg);
        assertMessageIsValid(false);
    }

    @Test
    public void consider_field_is_valid_if_no_required_option_set() {
        validator.validate(NotRequiredMsgFieldValue.getDefaultInstance());
        assertMessageIsValid(true);
    }

    /**
     * Time option tests.
     */

    @Test
    public void find_out_that_time_is_in_future() {
        final TimeInFutureFieldValue validMsg = TimeInFutureFieldValue.newBuilder().setValue(getFuture()).build();
        validator.validate(validMsg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_time_is_NOT_in_future() {
        final TimeInFutureFieldValue invalidMsg = TimeInFutureFieldValue.newBuilder().setValue(getPast()).build();
        validator.validate(invalidMsg);
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_time_is_in_past() {
        final TimeInPastFieldValue validMsg = TimeInPastFieldValue.newBuilder().setValue(getPast()).build();
        validator.validate(validMsg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_time_is_NOT_in_past() {
        final TimeInPastFieldValue invalidMsg = TimeInPastFieldValue.newBuilder().setValue(getFuture()).build();
        validator.validate(invalidMsg);
        assertMessageIsValid(false);
    }

    @Test
    public void consider_timestamp_field_is_valid_if_no_time_option_set() {
        validator.validate(TimeWithoutOptsFieldValue.getDefaultInstance());
        assertMessageIsValid(true);
    }

    /**
     * Min value option tests.
     */

    @Test
    public void consider_number_field_is_valid_if_no_number_options_set() {
        final Message nonZeroValue = DoubleValue.newBuilder().setValue(5).build();
        validator.validate(nonZeroValue);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_number_is_bigger_than_min_inclusive() {
        minNumberTest(BIGGER_THAN_MIN, /*inclusive=*/true, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_equal_to_min_inclusive() {
        minNumberTest(EQUAL_MIN, /*inclusive=*/true, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_less_than_min_inclusive() {
        minNumberTest(LESS_THAN_MIN, /*inclusive=*/true, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_bigger_than_min_NOT_inclusive() {
        minNumberTest(BIGGER_THAN_MIN, /*inclusive=*/false, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_equal_to_min_NOT_inclusive() {
        minNumberTest(EQUAL_MIN, /*inclusive=*/false, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_less_than_min_NOT_inclusive() {
        minNumberTest(LESS_THAN_MIN, /*inclusive=*/false, /*valid=*/false);
    }

    /**
     * Max value option tests.
     */

    @Test
    public void find_out_that_number_is_bigger_than_max_inclusive() {
        maxNumberTest(BIGGER_THAN_MAX, /*inclusive=*/true, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_equal_to_max_inclusive() {
        maxNumberTest(EQUAL_MAX, /*inclusive=*/true, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_less_than_max_inclusive() {
        maxNumberTest(LESS_THAN_MAX, /*inclusive=*/true, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_bigger_than_max_NOT_inclusive() {
        maxNumberTest(BIGGER_THAN_MAX, /*inclusive=*/false, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_equal_to_max_NOT_inclusive() {
        maxNumberTest(EQUAL_MAX, /*inclusive=*/false, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_less_than_max_NOT_inclusive() {
        maxNumberTest(LESS_THAN_MAX, /*inclusive=*/false, /*valid=*/true);
    }

    /**
     * Digits option tests.
     */

    @Test
    public void find_out_that_integral_digits_count_is_bigger_than_max() {
        digitsCountTest(INT_DIGITS_COUNT_BIGGER_THAN_MAX, /*valid=*/false);
    }

    @Test
    public void find_out_that_integral_digits_count_is_equal_to_max() {
        digitsCountTest(INT_DIGITS_COUNT_EQUAL_MAX, /*valid=*/true);
    }

    @Test
    public void find_out_that_integral_digits_count_is_less_than_max() {
        digitsCountTest(INT_DIGITS_COUNT_LESS_THAN_MAX, /*valid=*/true);
    }

    @Test
    public void find_out_that_fractional_digits_count_is_bigger_than_max() {
        digitsCountTest(FRACTIONAL_DIGITS_COUNT_BIGGER_THAN_MAX, /*valid=*/false);
    }

    @Test
    public void find_out_that_fractional_digits_count_is_equal_to_max() {
        digitsCountTest(FRACTIONAL_DIGITS_COUNT_EQUAL_MAX, /*valid=*/true);
    }

    @Test
    public void find_out_that_fractional_digits_count_is_less_than_max() {
        digitsCountTest(FRACTIONAL_DIGITS_COUNT_LESS_THAN_MAX, /*valid=*/true);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_get_results_but_msg_is_not_validated() {
        validator.isMessageInvalid();
    }

    private void minNumberTest(double value, boolean inclusive, boolean isValid) {
        final Message msg = inclusive ?
                            MinIncNumberFieldValue.newBuilder().setValue(value).build() :
                            MinNotIncNumberFieldValue.newBuilder().setValue(value).build();
        validator.validate(msg);
        assertMessageIsValid(isValid);
    }

    private void maxNumberTest(double value, boolean inclusive, boolean isValid) {
        final Message msg = inclusive ?
                            MaxIncNumberFieldValue.newBuilder().setValue(value).build() :
                            MaxNotIncNumberFieldValue.newBuilder().setValue(value).build();
        validator.validate(msg);
        assertMessageIsValid(isValid);
    }

    private void digitsCountTest(double value, boolean isValid) {
        final Message msg = DigitsCountNumberFieldValue.newBuilder().setValue(value).build();
        validator.validate(msg);
        assertMessageIsValid(isValid);
    }

    private void assertMessageIsValid(boolean isValid) {
        if (isValid) {
            assertFalse(validator.isMessageInvalid());
            assertTrue(validator.getErrorMessage().isEmpty());
        } else {
            assertTrue(validator.isMessageInvalid());
            assertFalse(validator.getErrorMessage().isEmpty());
        }
    }

    private static Timestamp getFuture() {
        final Timestamp future = add(getCurrentTime(), Durations.ofMinutes(5));
        return future;
    }

    private static Timestamp getPast() {
        final Timestamp past = subtract(getCurrentTime(), Durations.ofMinutes(5));
        return past;
    }

    private static StringValue newStringValue() {
        return StringValue.newBuilder().setValue(newUuid()).build();
    }
}
