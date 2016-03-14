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
import org.spine3.test.validation.RequiredMsgFieldValue;
import org.spine3.test.validation.TimeInFutureFieldValue;
import org.spine3.test.validation.TimeInPastFieldValue;

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
    public void find_out_that_required_message_field_is_set() {
        final RequiredMsgFieldValue validMsg = RequiredMsgFieldValue.newBuilder().setValue(newStringValue()).build();

        validator.validate(validMsg);

        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_required_message_field_is_NOT_set() {
        final RequiredMsgFieldValue invalidMsg = RequiredMsgFieldValue.getDefaultInstance();

        validator.validate(invalidMsg);

        assertMessageIsValid(false);
    }

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

    /**
     * Min value option tests.
     */

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

    @Test
    public void do_not_validate_number_field_if_no_max_and_min_options_set() {
        final Message nonZeroValue = DoubleValue.newBuilder().setValue(5).build();

        validator.validate(nonZeroValue);

        assertMessageIsValid(true);
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
