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
import org.spine3.test.validation.cmd.AnnotatedBooleanFieldValue;
import org.spine3.test.validation.cmd.AnnotatedEnumFieldValue;
import org.spine3.test.validation.cmd.DecimalMaxIncNumberFieldValue;
import org.spine3.test.validation.cmd.DecimalMaxNotIncNumberFieldValue;
import org.spine3.test.validation.cmd.DecimalMinIncNumberFieldValue;
import org.spine3.test.validation.cmd.DecimalMinNotIncNumberFieldValue;
import org.spine3.test.validation.cmd.DigitsCountNumberFieldValue;
import org.spine3.test.validation.cmd.EnclosedMessageFieldValue;
import org.spine3.test.validation.cmd.EnclosedMessageWithoutAnnotationFieldValue;
import org.spine3.test.validation.cmd.EntityIdIntFieldValue;
import org.spine3.test.validation.cmd.EntityIdLongFieldValue;
import org.spine3.test.validation.cmd.EntityIdMsgFieldValue;
import org.spine3.test.validation.cmd.EntityIdRepeatedFieldValue;
import org.spine3.test.validation.cmd.EntityIdStringFieldValue;
import org.spine3.test.validation.cmd.PatternStringFieldValue;
import org.spine3.test.validation.cmd.RepeatedRequiredMsgFieldValue;
import org.spine3.test.validation.cmd.RequiredByteStringFieldValue;
import org.spine3.test.validation.cmd.RequiredMsgFieldValue;
import org.spine3.test.validation.cmd.RequiredStringFieldValue;
import org.spine3.test.validation.cmd.TimeInFutureFieldValue;
import org.spine3.test.validation.cmd.TimeInPastFieldValue;
import org.spine3.test.validation.cmd.TimeWithoutOptsFieldValue;

import static com.google.protobuf.util.TimeUtil.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.spine3.base.Identifiers.newUuid;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
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

    private final MessageValidator validator = new MessageValidator(FieldValidatorFactory.newInstance());
    private final MessageValidator cmdValidator = new MessageValidator(CommandFieldValidatorFactory.newInstance());

    /*
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
        validator.validate(StringValue.getDefaultInstance());
        assertMessageIsValid(true);
    }

    @Test
    public void provide_validation_error_message_if_required_field_is_not_set() {
        final RequiredStringFieldValue invalidMsg = RequiredStringFieldValue.getDefaultInstance();

        validator.validate(invalidMsg);

        assertEquals(
                "Message spine.test.validation.cmd.RequiredStringFieldValue is invalid: 'value' must be set.",
                validator.getErrorMessage()
        );
    }

    /*
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

    @Test
    public void provide_validation_error_message_if_time_is_invalid() {
        final TimeInFutureFieldValue invalidMsg = TimeInFutureFieldValue.newBuilder().setValue(getPast()).build();

        validator.validate(invalidMsg);

        assertMessageIsValid(false);
        assertEquals(
                "Message spine.test.validation.cmd.TimeInFutureFieldValue is invalid: " +
                "'value' must be a timestamp in the future.",
                validator.getErrorMessage()
        );
    }

    /*
     * Min value option tests.
     */

    @Test
    public void consider_number_field_is_valid_if_no_number_options_set() {
        final Message nonZeroValue = DoubleValue.newBuilder().setValue(5).build();
        validator.validate(nonZeroValue);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_number_is_greater_than_min_inclusive() {
        minNumberTest(GREATER_THAN_MIN, /*inclusive=*/true, /*valid=*/true);
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
    public void find_out_that_number_is_greater_than_min_NOT_inclusive() {
        minNumberTest(GREATER_THAN_MIN, /*inclusive=*/false, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_equal_to_min_NOT_inclusive() {
        minNumberTest(EQUAL_MIN, /*inclusive=*/false, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_less_than_min_NOT_inclusive() {
        minNumberTest(LESS_THAN_MIN, /*inclusive=*/false, /*valid=*/false);
    }

    @Test
    public void provide_validation_error_message_if_number_is_less_than_min() {
        minNumberTest(LESS_THAN_MIN, /*inclusive=*/true, /*valid=*/false);
        assertEquals(
                "Message spine.test.validation.cmd.DecimalMinIncNumberFieldValue is invalid: " +
                "'value' must be greater than or equal to 16.5, actual: 11.5.",
                validator.getErrorMessage()
        );
    }

    /*
     * Max value option tests.
     */

    @Test
    public void find_out_that_number_is_greater_than_max_inclusive() {
        maxNumberTest(GREATER_THAN_MAX, /*inclusive=*/true, /*valid=*/false);
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
    public void find_out_that_number_is_greater_than_max_NOT_inclusive() {
        maxNumberTest(GREATER_THAN_MAX, /*inclusive=*/false, /*valid=*/false);
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
    public void provide_validation_error_message_if_number_is_greater_than_max() {
        maxNumberTest(GREATER_THAN_MAX, /*inclusive=*/true, /*valid=*/false);
        assertEquals(
                "Message spine.test.validation.cmd.DecimalMaxIncNumberFieldValue is invalid: " +
                "'value' must be less than or equal to 64.5, actual: 69.5.",
                validator.getErrorMessage()
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
                "Message spine.test.validation.cmd.DigitsCountNumberFieldValue is invalid: " +
                "'value' number is out of bounds, " +
                "expected: <2 max digits>.<2 max digits>, actual: <3 digits>.<1 digits>.",
                validator.getErrorMessage()
        );
    }

    /*
     * String pattern option tests.
     */

    @Test
    public void find_out_that_string_matches_to_regex_pattern() {
        final PatternStringFieldValue msg = PatternStringFieldValue.newBuilder().setEmail("valid.email@mail.com").build();
        validator.validate(msg);
        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_string_does_not_match_to_regex_pattern() {
        final PatternStringFieldValue msg = PatternStringFieldValue.newBuilder().setEmail("invalid email").build();
        validator.validate(msg);
        assertMessageIsValid(false);
    }

    @Test
    public void consider_field_is_valid_if_no_pattern_option_set() {
        validator.validate(StringValue.getDefaultInstance());
        assertMessageIsValid(true);
    }

    @Test
    public void provide_validation_error_message_if_string_does_not_match_to_regex_pattern() {
        final PatternStringFieldValue msg = PatternStringFieldValue.newBuilder().setEmail("invalid.email").build();

        validator.validate(msg);

        assertEquals(
                "Message spine.test.validation.cmd.PatternStringFieldValue is invalid: " +
                "'email' must match the regular expression: " +
                "'^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$', " +
                "found: 'invalid.email'.",
                validator.getErrorMessage()
        );
    }

    /*
     * Enclosed message field validation option tests.
     */

    @Test
    public void find_out_that_enclosed_message_field_is_valid() {
        final RequiredStringFieldValue enclosedMsg = RequiredStringFieldValue.newBuilder().setValue(newUuid()).build();
        final EnclosedMessageFieldValue msg = EnclosedMessageFieldValue.newBuilder().setValue(enclosedMsg).build();

        validator.validate(msg);

        assertMessageIsValid(true);
    }

    @Test
    public void find_out_that_enclosed_message_field_is_NOT_valid() {
        final RequiredStringFieldValue enclosedMsg = RequiredStringFieldValue.getDefaultInstance();
        final EnclosedMessageFieldValue msg = EnclosedMessageFieldValue.newBuilder().setValue(enclosedMsg).build();

        validator.validate(msg);

        assertMessageIsValid(false);
    }

    @Test
    public void consider_field_is_valid_if_no_valid_option_set() {
        final RequiredStringFieldValue invalidEnclosedMsg = RequiredStringFieldValue.getDefaultInstance();
        final EnclosedMessageWithoutAnnotationFieldValue msg = EnclosedMessageWithoutAnnotationFieldValue.newBuilder()
                .setValue(invalidEnclosedMsg)
                .build();

        validator.validate(msg);

        assertMessageIsValid(true);
    }

    @Test
    public void provide_validation_error_message_if_enclosed_message_field_is_not_valid() {
        final RequiredStringFieldValue enclosedMsg = RequiredStringFieldValue.getDefaultInstance();
        final EnclosedMessageFieldValue msg = EnclosedMessageFieldValue.newBuilder().setValue(enclosedMsg).build();

        validator.validate(msg);

        assertEquals(
                "Message spine.test.validation.cmd.EnclosedMessageFieldValue is invalid: " +
                "'value' message field value must have valid properties, error message: " +
                "<Message spine.test.validation.cmd.RequiredStringFieldValue is invalid: 'value' must be set.>.",
                validator.getErrorMessage()
        );
    }

    /*
     * Exceptional conditions tests.
     */

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_get_results_but_msg_is_not_validated() {
        validator.isMessageInvalid();
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_annotate_field_of_enum_type() {
        validator.validate(AnnotatedEnumFieldValue.getDefaultInstance());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_annotate_field_of_boolean_type() {
        validator.validate(AnnotatedBooleanFieldValue.getDefaultInstance());
    }
    
    /*
     * Entity ID in command validation tests.
     */

    @Test
    public void find_out_that_Message_entity_id_in_command_is_valid() {
        final EntityIdMsgFieldValue msg = EntityIdMsgFieldValue.newBuilder().setValue(newStringValue()).build();
        cmdValidator.validate(msg);
        assertCmdMessageIsValid(true);
    }

    @Test
    public void find_out_that_Message_entity_id_in_command_is_NOT_valid() {
        cmdValidator.validate(EntityIdMsgFieldValue.getDefaultInstance());
        assertCmdMessageIsValid(false);
    }

    @Test
    public void find_out_that_String_entity_id_in_command_is_valid() {
        final EntityIdStringFieldValue msg = EntityIdStringFieldValue.newBuilder().setValue(newUuid()).build();
        cmdValidator.validate(msg);
        assertCmdMessageIsValid(true);
    }

    @Test
    public void find_out_that_String_entity_id_in_command_is_NOT_valid() {
        cmdValidator.validate(EntityIdStringFieldValue.getDefaultInstance());
        assertCmdMessageIsValid(false);
    }

    @Test
    public void find_out_that_Integer_entity_id_in_command_is_valid() {
        final EntityIdIntFieldValue msg = EntityIdIntFieldValue.newBuilder().setValue(5).build();
        cmdValidator.validate(msg);
        assertCmdMessageIsValid(true);
    }

    @Test
    public void find_out_that_Integer_entity_id_in_command_is_NOT_valid() {
        cmdValidator.validate(EntityIdIntFieldValue.getDefaultInstance());
        assertCmdMessageIsValid(false);
    }

    @Test
    public void find_out_that_Long_entity_id_in_command_is_valid() {
        final EntityIdLongFieldValue msg = EntityIdLongFieldValue.newBuilder().setValue(5).build();
        cmdValidator.validate(msg);
        assertCmdMessageIsValid(true);
    }

    @Test
    public void find_out_that_Long_entity_id_in_command_is_NOT_valid() {
        cmdValidator.validate(EntityIdLongFieldValue.getDefaultInstance());
        assertCmdMessageIsValid(false);
    }

    @Test
    public void find_out_that_repeated_entity_id_in_command_is_not_valid() {
        final EntityIdRepeatedFieldValue msg = EntityIdRepeatedFieldValue.newBuilder().addValue(newUuid()).build();
        cmdValidator.validate(msg);
        assertCmdMessageIsValid(false);
    }

    @Test
    public void provide_validation_error_message_if_entity_id_in_command_is_not_valid() {
        // TODO:2016-03-25:alexander.litus: impl
    }

    /*
     * Other tests.
     */

    @Test
    public void build_validation_error_message() {
        final String msg = MessageValidator.buildErrorMessage(asList("msg1", "msg2", "msg3"), StringValue.getDescriptor());
        assertEquals("Message google.protobuf.StringValue is invalid: msg1; msg2; msg3.", msg);
    }

    private void minNumberTest(double value, boolean inclusive, boolean isValid) {
        final Message msg = inclusive ?
                            DecimalMinIncNumberFieldValue.newBuilder().setValue(value).build() :
                            DecimalMinNotIncNumberFieldValue.newBuilder().setValue(value).build();
        validator.validate(msg);
        assertMessageIsValid(isValid);
    }

    private void maxNumberTest(double value, boolean inclusive, boolean isValid) {
        final Message msg = inclusive ?
                            DecimalMaxIncNumberFieldValue.newBuilder().setValue(value).build() :
                            DecimalMaxNotIncNumberFieldValue.newBuilder().setValue(value).build();
        validator.validate(msg);
        assertMessageIsValid(isValid);
    }

    private void digitsCountTest(double value, boolean isValid) {
        final Message msg = DigitsCountNumberFieldValue.newBuilder().setValue(value).build();
        validator.validate(msg);
        assertMessageIsValid(isValid);
    }

    private void assertMessageIsValid(boolean isValid) {
        assertMessageIsValid(isValid, validator);
    }

    private void assertCmdMessageIsValid(boolean isValid) {
        assertMessageIsValid(isValid, cmdValidator);
    }

    private static void assertMessageIsValid(boolean isValid, MessageValidator validator) {
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
