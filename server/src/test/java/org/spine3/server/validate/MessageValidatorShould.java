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
import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.protobuf.Durations;
import org.spine3.test.validation.msg.AnnotatedBooleanFieldValue;
import org.spine3.test.validation.msg.AnnotatedEnumFieldValue;
import org.spine3.test.validation.msg.DecimalMaxIncNumberFieldValue;
import org.spine3.test.validation.msg.DecimalMaxNotIncNumberFieldValue;
import org.spine3.test.validation.msg.DecimalMinIncNumberFieldValue;
import org.spine3.test.validation.msg.DecimalMinNotIncNumberFieldValue;
import org.spine3.test.validation.msg.DigitsCountNumberFieldValue;
import org.spine3.test.validation.msg.EnclosedMessageFieldValue;
import org.spine3.test.validation.msg.EnclosedMessageWithoutAnnotationFieldValue;
import org.spine3.test.validation.msg.EntityIdIntFieldValue;
import org.spine3.test.validation.msg.EntityIdLongFieldValue;
import org.spine3.test.validation.msg.EntityIdMsgFieldValue;
import org.spine3.test.validation.msg.EntityIdRepeatedFieldValue;
import org.spine3.test.validation.msg.EntityIdStringFieldValue;
import org.spine3.test.validation.msg.PatternStringFieldValue;
import org.spine3.test.validation.msg.RepeatedRequiredMsgFieldValue;
import org.spine3.test.validation.msg.RequiredByteStringFieldValue;
import org.spine3.test.validation.msg.RequiredMsgFieldValue;
import org.spine3.test.validation.msg.RequiredStringFieldValue;
import org.spine3.test.validation.msg.TimeInFutureFieldValue;
import org.spine3.test.validation.msg.TimeInPastFieldValue;
import org.spine3.test.validation.msg.TimeWithoutOptsFieldValue;
import org.spine3.validation.options.ConstraintViolation;

import java.util.List;

import static com.google.protobuf.util.TimeUtil.*;
import static java.lang.String.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods", "OverlyCoupledClass"})
public class MessageValidatorShould {

    private static final double EQUAL_MIN = 16.5;
    private static final double GREATER_THAN_MIN = EQUAL_MIN + 5;
    private static final double LESS_THAN_MIN = EQUAL_MIN - 5;

    private static final double EQUAL_MAX = 64.5;
    private static final double GREATER_THAN_MAX = EQUAL_MAX + 5;
    private static final double LESS_THAN_MAX = EQUAL_MAX - 5;

    private static final double INT_DIGIT_COUNT_GREATER_THAN_MAX = 123.5;
    private static final double INT_DIGIT_COUNT_EQUAL_MAX = 12.5;
    private static final double INT_DIGIT_COUNT_LESS_THAN_MAX = 1.5;

    private static final double FRACTIONAL_DIGIT_COUNT_GREATER_THAN_MAX = 1.123;
    private static final double FRACTIONAL_DIGIT_COUNT_EQUAL_MAX = 1.12;
    private static final double FRACTIONAL_DIGIT_COUNT_LESS_THAN_MAX = 1.0;

    private final MessageValidator validator = new MessageValidator();

    private List<ConstraintViolation> violations;

    /*
     * Required option tests.
     */

    @Test
    public void find_out_that_required_Message_field_is_set() {
        final RequiredMsgFieldValue validMsg = RequiredMsgFieldValue.newBuilder().setValue(newStringValue()).build();
        validate(validMsg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_required_Message_field_is_NOT_set() {
        final RequiredMsgFieldValue invalidMsg = RequiredMsgFieldValue.getDefaultInstance();
        validate(invalidMsg);
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_required_String_field_is_set() {
        final RequiredStringFieldValue validMsg = RequiredStringFieldValue.newBuilder().setValue(newUuid()).build();
        validate(validMsg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_required_String_field_is_NOT_set() {
        final RequiredStringFieldValue invalidMsg = RequiredStringFieldValue.getDefaultInstance();
        validate(invalidMsg);
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_required_ByteString_field_is_set() {
        final ByteString byteString = ByteString.copyFromUtf8(newUuid());
        final RequiredByteStringFieldValue validMsg = RequiredByteStringFieldValue.newBuilder().setValue(byteString).build();
        validate(validMsg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_required_ByteString_field_is_NOT_set() {
        final RequiredByteStringFieldValue invalidMsg = RequiredByteStringFieldValue.getDefaultInstance();
        validate(invalidMsg);
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_repeated_required_field_has_valid_values() {
        final RepeatedRequiredMsgFieldValue invalidMsg = RepeatedRequiredMsgFieldValue.newBuilder()
                .addValue(newStringValue())
                .addValue(newStringValue())
                .build();
        validate(invalidMsg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_repeated_required_field_has_no_values() {
        validate(RepeatedRequiredMsgFieldValue.getDefaultInstance());
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_repeated_required_field_has_empty_value() {
        final RepeatedRequiredMsgFieldValue invalidMsg = RepeatedRequiredMsgFieldValue.newBuilder()
                .addValue(newStringValue()) // valid value
                .addValue(StringValue.getDefaultInstance()) // invalid value
                .build();
        validate(invalidMsg);
        assertMessageIsValid(false);
    }

    @Test
    public void consider_field_is_valid_if_no_required_option_set() {
        validate(StringValue.getDefaultInstance());
        assertMessageIsValid(true);
    }

    @Test
    public void provide_validation_error_message_if_required_field_is_not_set() {
        final RequiredStringFieldValue invalidMsg = RequiredStringFieldValue.getDefaultInstance();

        validate(invalidMsg);

        assertEquals("Value must be set.", firstViolation().getMessage());
    }

    /*
     * Time option tests.
     */

    @Test
    public void find_out_that_time_is_in_future() {
        final TimeInFutureFieldValue validMsg = TimeInFutureFieldValue.newBuilder().setValue(getFuture()).build();
        validate(validMsg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_time_is_NOT_in_future() {
        final TimeInFutureFieldValue invalidMsg = TimeInFutureFieldValue.newBuilder().setValue(getPast()).build();
        validate(invalidMsg);
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_time_is_in_past() {
        final TimeInPastFieldValue validMsg = TimeInPastFieldValue.newBuilder().setValue(getPast()).build();
        validate(validMsg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_time_is_NOT_in_past() {
        final TimeInPastFieldValue invalidMsg = TimeInPastFieldValue.newBuilder().setValue(getFuture()).build();
        validate(invalidMsg);
        assertMessageIsValid(false);
    }

    @Test
    public void consider_timestamp_field_is_valid_if_no_time_option_set() {
        validate(TimeWithoutOptsFieldValue.getDefaultInstance());
        assertMessageIsValid(true);
    }

    @Test
    public void provide_validation_error_message_if_time_is_invalid() {
        final TimeInFutureFieldValue invalidMsg = TimeInFutureFieldValue.newBuilder().setValue(getPast()).build();

        validate(invalidMsg);

        assertMessageIsValid(false);
        assertEquals(
                "Timestamp value must be in the future.",
                format(firstViolation().getMessage(), firstViolation().getFormatParam(0))
        );
    }

    /*
     * Min value option tests.
     */

    @Test
    public void consider_number_field_is_valid_if_no_number_options_set() {
        final Message nonZeroValue = DoubleValue.newBuilder().setValue(5).build();
        validate(nonZeroValue);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_number_is_greater_than_min_inclusive() {
        minDecimalNumberTest(GREATER_THAN_MIN, /*inclusive=*/true, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_equal_to_min_inclusive() {
        minDecimalNumberTest(EQUAL_MIN, /*inclusive=*/true, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_less_than_min_inclusive() {
        minDecimalNumberTest(LESS_THAN_MIN, /*inclusive=*/true, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_greater_than_min_NOT_inclusive() {
        minDecimalNumberTest(GREATER_THAN_MIN, /*inclusive=*/false, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_equal_to_min_NOT_inclusive() {
        minDecimalNumberTest(EQUAL_MIN, /*inclusive=*/false, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_less_than_min_NOT_inclusive() {
        minDecimalNumberTest(LESS_THAN_MIN, /*inclusive=*/false, /*valid=*/false);
    }

    @Test
    public void provide_validation_error_message_if_number_is_less_than_min() {
        minDecimalNumberTest(LESS_THAN_MIN, /*inclusive=*/true, /*valid=*/false);
        final ConstraintViolation violation = firstViolation();
        assertEquals(
                "Number must be greater than or equal to 16.5.",
                format(violation.getMessage(), violation.getFormatParam(0), violation.getFormatParam(1))
        );
    }

    /*
     * Max value option tests.
     */

    @Test
    public void find_out_that_number_is_greater_than_max_inclusive() {
        maxDecimalNumberTest(GREATER_THAN_MAX, /*inclusive=*/true, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_equal_to_max_inclusive() {
        maxDecimalNumberTest(EQUAL_MAX, /*inclusive=*/true, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_less_than_max_inclusive() {
        maxDecimalNumberTest(LESS_THAN_MAX, /*inclusive=*/true, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_greater_than_max_NOT_inclusive() {
        maxDecimalNumberTest(GREATER_THAN_MAX, /*inclusive=*/false, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_equal_to_max_NOT_inclusive() {
        maxDecimalNumberTest(EQUAL_MAX, /*inclusive=*/false, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_less_than_max_NOT_inclusive() {
        maxDecimalNumberTest(LESS_THAN_MAX, /*inclusive=*/false, /*valid=*/true);
    }

    @Test
    public void provide_validation_error_message_if_number_is_greater_than_max() {
        maxDecimalNumberTest(GREATER_THAN_MAX, /*inclusive=*/true, /*valid=*/false);
        final ConstraintViolation violation = firstViolation();
        assertEquals(
                "Number must be less than or equal to 64.5.",
                format(violation.getMessage(), violation.getFormatParam(0), violation.getFormatParam(1))
        );
    }

    /*
     * Digits option tests.
     */

    @Test
    public void find_out_that_integral_digit_count_is_greater_than_max() {
        digitsCountTest(INT_DIGIT_COUNT_GREATER_THAN_MAX, /*valid=*/false);
    }

    @Test
    public void find_out_that_integral_digits_count_is_equal_to_max() {
        digitsCountTest(INT_DIGIT_COUNT_EQUAL_MAX, /*valid=*/true);
    }

    @Test
    public void find_out_that_integral_digit_count_is_less_than_max() {
        digitsCountTest(INT_DIGIT_COUNT_LESS_THAN_MAX, /*valid=*/true);
    }

    @Test
    public void find_out_that_fractional_digit_count_is_greater_than_max() {
        digitsCountTest(FRACTIONAL_DIGIT_COUNT_GREATER_THAN_MAX, /*valid=*/false);
    }

    @Test
    public void find_out_that_fractional_digit_count_is_equal_to_max() {
        digitsCountTest(FRACTIONAL_DIGIT_COUNT_EQUAL_MAX, /*valid=*/true);
    }

    @Test
    public void find_out_that_fractional_digit_count_is_less_than_max() {
        digitsCountTest(FRACTIONAL_DIGIT_COUNT_LESS_THAN_MAX, /*valid=*/true);
    }

    @Test
    public void provide_validation_error_message_if_integral_digit_count_is_greater_than_max() {
        digitsCountTest(INT_DIGIT_COUNT_GREATER_THAN_MAX, /*valid=*/false);
        assertEquals(
                "Number value is out of bounds, expected: <%s max digits>.<%s max digits>.",
                firstViolation().getMessage()
        );
        final ProtocolStringList formatParams = firstViolation().getFormatParamList();
        assertEquals(2, formatParams.size());
        assertEquals("2", formatParams.get(0));
        assertEquals("2", formatParams.get(1));
    }

    /*
     * String pattern option tests.
     */

    @Test
    public void find_out_that_string_matches_to_regex_pattern() {
        final PatternStringFieldValue msg = PatternStringFieldValue.newBuilder().setEmail("valid.email@mail.com").build();
        validate(msg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_string_does_not_match_to_regex_pattern() {
        final PatternStringFieldValue msg = PatternStringFieldValue.newBuilder().setEmail("invalid email").build();
        validate(msg);
        assertMessageIsValid(false);
    }

    @Test
    public void consider_field_is_valid_if_no_pattern_option_set() {
        validate(StringValue.getDefaultInstance());
        assertMessageIsValid(true);
    }

    @Test
    public void provide_validation_error_message_if_string_does_not_match_to_regex_pattern() {
        final PatternStringFieldValue msg = PatternStringFieldValue.newBuilder().setEmail("invalid.email").build();

        validate(msg);

        assertEquals("String must match the regular expression '%s'.", firstViolation().getMessage());
        assertEquals(
                "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$",
                firstViolation().getFormatParam(0)
        );
    }

    /*
     * Enclosed message field validation option tests.
     */

    @Test
    public void find_out_that_enclosed_message_field_is_valid() {
        final RequiredStringFieldValue enclosedMsg = RequiredStringFieldValue.newBuilder().setValue(newUuid()).build();
        final EnclosedMessageFieldValue msg = EnclosedMessageFieldValue.newBuilder().setValue(enclosedMsg).build();

        validate(msg);

        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_enclosed_message_field_is_NOT_valid() {
        final RequiredStringFieldValue enclosedMsg = RequiredStringFieldValue.getDefaultInstance();
        final EnclosedMessageFieldValue msg = EnclosedMessageFieldValue.newBuilder().setValue(enclosedMsg).build();

        validate(msg);

        assertMessageIsValid(false);
    }

    @Test
    public void consider_field_is_valid_if_no_valid_option_set() {
        final RequiredStringFieldValue invalidEnclosedMsg = RequiredStringFieldValue.getDefaultInstance();
        final EnclosedMessageWithoutAnnotationFieldValue msg = EnclosedMessageWithoutAnnotationFieldValue.newBuilder()
                .setValue(invalidEnclosedMsg)
                .build();

        validate(msg);

        assertMessageIsValid(true);
    }

    @Test
    public void provide_validation_error_message_if_enclosed_message_field_is_not_valid() {
        final RequiredStringFieldValue enclosedMsg = RequiredStringFieldValue.getDefaultInstance();
        final EnclosedMessageFieldValue msg = EnclosedMessageFieldValue.newBuilder().setValue(enclosedMsg).build();

        validate(msg);

        assertEquals("Message must have valid properties.", firstViolation().getMessage());
    }

    /*
     * Exceptional conditions tests.
     */

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_annotate_field_of_enum_type() {
        validate(AnnotatedEnumFieldValue.getDefaultInstance());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_annotate_field_of_boolean_type() {
        validate(AnnotatedBooleanFieldValue.getDefaultInstance());
    }
    
    /*
     * Entity ID in command validation tests.
     */

    @Test
    public void find_out_that_Message_entity_id_in_command_is_valid() {
        final EntityIdMsgFieldValue msg = EntityIdMsgFieldValue.newBuilder().setValue(newStringValue()).build();
        validate(msg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_Message_entity_id_in_command_is_NOT_valid() {
        validate(EntityIdMsgFieldValue.getDefaultInstance());
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_String_entity_id_in_command_is_valid() {
        final EntityIdStringFieldValue msg = EntityIdStringFieldValue.newBuilder().setValue(newUuid()).build();
        validate(msg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_String_entity_id_in_command_is_NOT_valid() {
        validate(EntityIdStringFieldValue.getDefaultInstance());
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_Integer_entity_id_in_command_is_valid() {
        final EntityIdIntFieldValue msg = EntityIdIntFieldValue.newBuilder().setValue(5).build();
        validate(msg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_Integer_entity_id_in_command_is_NOT_valid() {
        validate(EntityIdIntFieldValue.getDefaultInstance());
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_Long_entity_id_in_command_is_valid() {
        final EntityIdLongFieldValue msg = EntityIdLongFieldValue.newBuilder().setValue(5).build();
        validate(msg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_Long_entity_id_in_command_is_NOT_valid() {
        validate(EntityIdLongFieldValue.getDefaultInstance());
        assertMessageIsValid(false);
    }

    @Test
    public void find_out_that_repeated_entity_id_in_command_is_not_valid() {
        final EntityIdRepeatedFieldValue msg = EntityIdRepeatedFieldValue.newBuilder().addValue(newUuid()).build();
        validate(msg);
        assertMessageIsValid(false);
    }

    @Test
    public void provide_validation_error_message_if_entity_id_in_command_is_not_valid() {
        // TODO:2016-03-25:alexander.litus: impl
    }

    /*
     * Utility methods.
     */

    private void minDecimalNumberTest(double value, boolean inclusive, boolean isValid) {
        final Message msg = inclusive ?
                            DecimalMinIncNumberFieldValue.newBuilder().setValue(value).build() :
                            DecimalMinNotIncNumberFieldValue.newBuilder().setValue(value).build();
        validate(msg);
        assertMessageIsValid(isValid);
    }

    private void maxDecimalNumberTest(double value, boolean inclusive, boolean isValid) {
        final Message msg = inclusive ?
                            DecimalMaxIncNumberFieldValue.newBuilder().setValue(value).build() :
                            DecimalMaxNotIncNumberFieldValue.newBuilder().setValue(value).build();
        validate(msg);
        assertMessageIsValid(isValid);
    }

    private void digitsCountTest(double value, boolean isValid) {
        final Message msg = DigitsCountNumberFieldValue.newBuilder().setValue(value).build();
        validate(msg);
        assertMessageIsValid(isValid);
    }

    private void validate(Message msg) {
        violations = validator.validate(msg);
    }

    private ConstraintViolation firstViolation() {
        return violations.get(0);
    }

    private void assertMessageIsValid(boolean isValid) {
        if (isValid) {
            assertTrue(violations.isEmpty());
        } else {
            assertTrue(!violations.isEmpty());
            for (ConstraintViolation violation : violations) {
                assertTrue(!violation.getMessage().isEmpty());
                assertTrue(!violation.getFieldPath().getFieldNameList().isEmpty());
            }
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
