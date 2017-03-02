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

package org.spine3.server.validate;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.base.FieldPath;
import org.spine3.protobuf.Durations2;
import org.spine3.protobuf.Values;
import org.spine3.test.validate.msg.CustomMessageRequiredByteStringFieldValue;
import org.spine3.test.validate.msg.CustomMessageRequiredEnumFieldValue;
import org.spine3.test.validate.msg.CustomMessageRequiredMsgFieldValue;
import org.spine3.test.validate.msg.CustomMessageRequiredRepeatedMsgFieldValue;
import org.spine3.test.validate.msg.CustomMessageRequiredStringFieldValue;
import org.spine3.test.validate.msg.CustomMessageWithNoRequiredOption;
import org.spine3.test.validate.msg.DecimalMaxIncNumberFieldValue;
import org.spine3.test.validate.msg.DecimalMaxNotIncNumberFieldValue;
import org.spine3.test.validate.msg.DecimalMinIncNumberFieldValue;
import org.spine3.test.validate.msg.DecimalMinNotIncNumberFieldValue;
import org.spine3.test.validate.msg.DigitsCountNumberFieldValue;
import org.spine3.test.validate.msg.EnclosedMessageFieldValue;
import org.spine3.test.validate.msg.EnclosedMessageFieldValueWithCustomInvalidMessage;
import org.spine3.test.validate.msg.EnclosedMessageFieldValueWithoutAnnotationFieldValueWithCustomInvalidMessage;
import org.spine3.test.validate.msg.EnclosedMessageWithoutAnnotationFieldValue;
import org.spine3.test.validate.msg.EntityIdByteStringFieldValue;
import org.spine3.test.validate.msg.EntityIdDoubleFieldValue;
import org.spine3.test.validate.msg.EntityIdIntFieldValue;
import org.spine3.test.validate.msg.EntityIdLongFieldValue;
import org.spine3.test.validate.msg.EntityIdMsgFieldValue;
import org.spine3.test.validate.msg.EntityIdRepeatedFieldValue;
import org.spine3.test.validate.msg.EntityIdStringFieldValue;
import org.spine3.test.validate.msg.MaxNumberFieldValue;
import org.spine3.test.validate.msg.MinNumberFieldValue;
import org.spine3.test.validate.msg.PatternStringFieldValue;
import org.spine3.test.validate.msg.RepeatedRequiredMsgFieldValue;
import org.spine3.test.validate.msg.RequiredBooleanFieldValue;
import org.spine3.test.validate.msg.RequiredByteStringFieldValue;
import org.spine3.test.validate.msg.RequiredEnumFieldValue;
import org.spine3.test.validate.msg.RequiredMsgFieldValue;
import org.spine3.test.validate.msg.RequiredStringFieldValue;
import org.spine3.test.validate.msg.TimeInFutureFieldValue;
import org.spine3.test.validate.msg.TimeInPastFieldValue;
import org.spine3.test.validate.msg.TimeWithoutOptsFieldValue;
import org.spine3.validate.ConstraintViolation;
import org.spine3.validate.internal.Time;
import org.spine3.validate.internal.ValidationProto;

import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.protobuf.util.Timestamps.add;
import static com.google.protobuf.util.Timestamps.subtract;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;
import static org.spine3.test.Verify.assertSize;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass", "OverlyComplexClass"})
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

    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static final String VALUE = "value";
    private static final String OUTER_MSG_FIELD = "outer_msg_field";

    private static final String NO_VALUE_MSG = "Value must be set.";
    private static final String LESS_THAN_MIN_MSG = "Number must be greater than or equal to 16.5.";
    private static final String GREATER_MAX_MSG = "Number must be less than or equal to 64.5.";

    private final MessageValidator validator = MessageValidator.newInstance();

    private List<ConstraintViolation> violations;

    /*
     * Required option tests.
     */

    @Test
    public void find_out_that_required_Message_field_is_set() {
        final RequiredMsgFieldValue validMsg = RequiredMsgFieldValue.newBuilder().setValue(newStringValue()).build();
        validate(validMsg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_required_Message_field_is_NOT_set() {
        final RequiredMsgFieldValue invalidMsg = RequiredMsgFieldValue.getDefaultInstance();
        validate(invalidMsg);
        assertIsValid(false);
    }

    @Test
    public void find_out_that_required_String_field_is_set() {
        final RequiredStringFieldValue validMsg = RequiredStringFieldValue.newBuilder().setValue(newUuid()).build();
        validate(validMsg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_required_String_field_is_NOT_set() {
        final RequiredStringFieldValue invalidMsg = RequiredStringFieldValue.getDefaultInstance();
        validate(invalidMsg);
        assertIsValid(false);
    }

    @Test
    public void find_out_that_required_ByteString_field_is_set() {
        final RequiredByteStringFieldValue validMsg = RequiredByteStringFieldValue.newBuilder().setValue(newByteString()).build();
        validate(validMsg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_required_ByteString_field_is_NOT_set() {
        final RequiredByteStringFieldValue invalidMsg = RequiredByteStringFieldValue.getDefaultInstance();
        validate(invalidMsg);
        assertIsValid(false);
    }

    @Test
    public void find_out_that_required_Enum_field_is_NOT_set() {
        final RequiredEnumFieldValue invalidMsg = RequiredEnumFieldValue.getDefaultInstance();
        validate(invalidMsg);
        assertIsValid(false);
    }

    @Test
    public void find_out_that_required_Enum_field_is_set() {
        final RequiredEnumFieldValue invalidMsg = RequiredEnumFieldValue.newBuilder()
                                                                        .setValue(Time.FUTURE)
                                                                        .build();
        validate(invalidMsg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_required_NOT_set_Boolean_field_pass_validation() {
        validate(RequiredBooleanFieldValue.getDefaultInstance());
        assertIsValid(true);
    }

    @Test
    public void find_out_that_repeated_required_field_has_valid_values() {
        final RepeatedRequiredMsgFieldValue invalidMsg = RepeatedRequiredMsgFieldValue.newBuilder()
                                                                                      .addValue(newStringValue())
                                                                                      .addValue(newStringValue())
                                                                                      .build();
        validate(invalidMsg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_repeated_required_field_has_no_values() {
        validate(RepeatedRequiredMsgFieldValue.getDefaultInstance());
        assertIsValid(false);
    }

    @Test
    public void find_out_that_repeated_required_field_has_empty_value() {
        final RepeatedRequiredMsgFieldValue invalidMsg =
                RepeatedRequiredMsgFieldValue.newBuilder()
                                             .addValue(newStringValue()) // valid value
                                             .addValue(StringValue.getDefaultInstance()) // invalid value
                                             .build();
        validate(invalidMsg);
        assertIsValid(false);
    }

    @Test
    public void consider_field_is_valid_if_no_required_option_set() {
        validate(StringValue.getDefaultInstance());
        assertIsValid(true);
    }

    @Test
    public void provide_one_valid_violation_if_required_field_is_not_set() {
        final RequiredStringFieldValue invalidMsg = RequiredStringFieldValue.getDefaultInstance();

        validate(invalidMsg);

        assertEquals(1, violations.size());
        final ConstraintViolation violation = firstViolation();
        assertEquals(NO_VALUE_MSG, violation.getMsgFormat());
        assertFieldPathIs(violation, VALUE);
        assertTrue(violation.getViolationList()
                            .isEmpty());
    }

    @Test
    public void propagate_proper_error_message_if_custom_message_set_and_required_Message_field_is_NOT_set() {
        final CustomMessageRequiredMsgFieldValue invalidMsg = CustomMessageRequiredMsgFieldValue.getDefaultInstance();
        final String expectedMessage = getCustomErrorMessage(CustomMessageRequiredMsgFieldValue.getDescriptor());

        validate(invalidMsg);
        assertIsValid(false);

        checkErrorMessage(expectedMessage);
    }

    @Test
    public void propagate_proper_error_message_if_custom_message_set_and_required_String_field_is_NOT_set() {
        final CustomMessageRequiredStringFieldValue invalidMsg =
                CustomMessageRequiredStringFieldValue.getDefaultInstance();
        final String expectedMessage = getCustomErrorMessage(CustomMessageRequiredStringFieldValue.getDescriptor());

        validate(invalidMsg);
        assertIsValid(false);

        checkErrorMessage(expectedMessage);
    }

    @Test
    public void propagate_proper_error_message_if_custom_message_set_and_required_ByteString_field_is_NOT_set() {
        final CustomMessageRequiredByteStringFieldValue invalidMsg =
                CustomMessageRequiredByteStringFieldValue.getDefaultInstance();
        final String expectedMessage = getCustomErrorMessage(CustomMessageRequiredByteStringFieldValue.getDescriptor());

        validate(invalidMsg);
        assertIsValid(false);

        checkErrorMessage(expectedMessage);
    }

    @Test
    public void propagate_proper_error_message_if_custom_message_set_and_required_RepeatedMsg_field_is_NOT_set() {
        final CustomMessageRequiredRepeatedMsgFieldValue invalidMsg =
                CustomMessageRequiredRepeatedMsgFieldValue.getDefaultInstance();
        final String expectedMessage = getCustomErrorMessage(CustomMessageRequiredRepeatedMsgFieldValue.getDescriptor());

        validate(invalidMsg);
        assertIsValid(false);

        checkErrorMessage(expectedMessage);
    }

    @Test
    public void propagate_proper_error_message_if_custom_message_set_and_required_Enum_field_is_NOT_set() {
        final CustomMessageRequiredEnumFieldValue invalidMsg =
                CustomMessageRequiredEnumFieldValue.getDefaultInstance();
        final String expectedMessage = getCustomErrorMessage(CustomMessageRequiredEnumFieldValue.getDescriptor());

        validate(invalidMsg);
        assertIsValid(false);

        checkErrorMessage(expectedMessage);
    }

    @Test
    public void ignore_if_missing_option_if_field_not_marked_required() {
        final CustomMessageWithNoRequiredOption invalidMsg =
                CustomMessageWithNoRequiredOption.getDefaultInstance();

        validate(invalidMsg);
        assertIsValid(true);

        assertTrue(violations.isEmpty());
    }

    private void checkErrorMessage(String expectedMessage) {
        final ConstraintViolation constraintViolation = firstViolation();
        assertEquals(expectedMessage, constraintViolation.getMsgFormat());
    }

    private static String getCustomErrorMessage(Descriptors.Descriptor descriptor) {
        final Descriptors.FieldDescriptor firstFieldDescriptor = descriptor.getFields()
                                                                           .get(0);
        return firstFieldDescriptor.getOptions()
                                   .getExtension(ValidationProto.ifMissing)
                                   .getMsgFormat();
    }

    /*
     * Time option tests.
     */

    @Test
    public void find_out_that_time_is_in_future() {
        final TimeInFutureFieldValue validMsg = TimeInFutureFieldValue.newBuilder().setValue(getFuture()).build();
        validate(validMsg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_time_is_NOT_in_future() {
        final TimeInFutureFieldValue invalidMsg = TimeInFutureFieldValue.newBuilder().setValue(getPast()).build();
        validate(invalidMsg);
        assertIsValid(false);
    }

    @Test
    public void find_out_that_time_is_in_past() {
        final TimeInPastFieldValue validMsg = TimeInPastFieldValue.newBuilder().setValue(getPast()).build();
        validate(validMsg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_time_is_NOT_in_past() {
        final TimeInPastFieldValue invalidMsg = TimeInPastFieldValue.newBuilder().setValue(getFuture()).build();
        validate(invalidMsg);
        assertIsValid(false);
    }

    @Test
    public void consider_timestamp_field_is_valid_if_no_time_option_set() {
        validate(TimeWithoutOptsFieldValue.getDefaultInstance());
        assertIsValid(true);
    }

    @Test
    public void provide_one_valid_violation_if_time_is_invalid() {
        final TimeInFutureFieldValue invalidMsg = TimeInFutureFieldValue.newBuilder().setValue(getPast()).build();

        validate(invalidMsg);

        assertEquals(1, violations.size());
        final ConstraintViolation violation = firstViolation();
        assertEquals(
                "Timestamp value must be in the future.",
                format(firstViolation().getMsgFormat(), firstViolation().getParam(0))
        );
        assertFieldPathIs(violation, VALUE);
        assertTrue(violation.getViolationList()
                            .isEmpty());
    }

    /*
     * Decimal min option tests.
     */

    @Test
    public void consider_number_field_is_valid_if_no_number_options_set() {
        final Message nonZeroValue = DoubleValue.newBuilder().setValue(5).build();
        validate(nonZeroValue);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_number_is_greater_than_decimal_min_inclusive() {
        minDecimalNumberTest(GREATER_THAN_MIN, /*inclusive=*/true, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_equal_to_decimal_min_inclusive() {
        minDecimalNumberTest(EQUAL_MIN, /*inclusive=*/true, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_less_than_decimal_min_inclusive() {
        minDecimalNumberTest(LESS_THAN_MIN, /*inclusive=*/true, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_greater_than_decimal_min_NOT_inclusive() {
        minDecimalNumberTest(GREATER_THAN_MIN, /*inclusive=*/false, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_equal_to_decimal_min_NOT_inclusive() {
        minDecimalNumberTest(EQUAL_MIN, /*inclusive=*/false, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_less_than_decimal_min_NOT_inclusive() {
        minDecimalNumberTest(LESS_THAN_MIN, /*inclusive=*/false, /*valid=*/false);
    }

    @Test
    public void provide_one_valid_violation_if_number_is_less_than_decimal_min() {
        minDecimalNumberTest(LESS_THAN_MIN, /*inclusive=*/true, /*valid=*/false);

        assertEquals(1, violations.size());
        final ConstraintViolation violation = firstViolation();
        assertEquals(LESS_THAN_MIN_MSG, format(violation.getMsgFormat(), violation.getParam(0), violation.getParam(1)));
        assertFieldPathIs(violation, VALUE);
        assertTrue(violation.getViolationList()
                            .isEmpty());
    }

    /*
     * Decimal max option tests.
     */

    @Test
    public void find_out_that_number_is_greater_than_decimal_max_inclusive() {
        maxDecimalNumberTest(GREATER_THAN_MAX, /*inclusive=*/true, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_equal_to_decimal_max_inclusive() {
        maxDecimalNumberTest(EQUAL_MAX, /*inclusive=*/true, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_less_than_decimal_max_inclusive() {
        maxDecimalNumberTest(LESS_THAN_MAX, /*inclusive=*/true, /*valid=*/true);
    }

    @Test
    public void find_out_that_number_is_greater_than_decimal_max_NOT_inclusive() {
        maxDecimalNumberTest(GREATER_THAN_MAX, /*inclusive=*/false, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_equal_to_decimal_max_NOT_inclusive() {
        maxDecimalNumberTest(EQUAL_MAX, /*inclusive=*/false, /*valid=*/false);
    }

    @Test
    public void find_out_that_number_is_less_than_decimal_max_NOT_inclusive() {
        maxDecimalNumberTest(LESS_THAN_MAX, /*inclusive=*/false, /*valid=*/true);
    }

    @Test
    public void provide_one_valid_violation_if_number_is_greater_than_decimal_max() {
        maxDecimalNumberTest(GREATER_THAN_MAX, /*inclusive=*/true, /*valid=*/false);

        assertEquals(1, violations.size());
        final ConstraintViolation violation = firstViolation();
        assertEquals(GREATER_MAX_MSG, format(violation.getMsgFormat(), violation.getParam(0), violation.getParam(1)));
        assertFieldPathIs(violation, VALUE);
        assertTrue(violation.getViolationList()
                            .isEmpty());
    }

    /*
     * Min option tests.
     */

    @Test
    public void find_out_that_number_is_greater_than_min() {
        final MinNumberFieldValue msg = MinNumberFieldValue.newBuilder().setValue(GREATER_THAN_MIN).build();
        validate(msg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_number_is_equal_to_min() {
        final MinNumberFieldValue msg = MinNumberFieldValue.newBuilder().setValue(EQUAL_MIN).build();
        validate(msg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_number_is_less_than_min() {
        final MinNumberFieldValue msg = MinNumberFieldValue.newBuilder().setValue(LESS_THAN_MIN).build();
        validate(msg);
        assertIsValid(false);
    }

    @Test
    public void provide_one_valid_violation_if_number_is_less_than_min() {
        final MinNumberFieldValue msg = MinNumberFieldValue.newBuilder().setValue(LESS_THAN_MIN).build();

        validate(msg);

        assertEquals(1, violations.size());
        final ConstraintViolation violation = firstViolation();
        assertEquals(LESS_THAN_MIN_MSG, format(violation.getMsgFormat(), violation.getParam(0)));
        assertFieldPathIs(violation, VALUE);
        assertTrue(violation.getViolationList()
                            .isEmpty());
    }

    /*
     * Max option tests.
     */

    @Test
    public void find_out_that_number_is_greater_than_max_inclusive() {
        final MaxNumberFieldValue msg = MaxNumberFieldValue.newBuilder().setValue(GREATER_THAN_MAX).build();
        validate(msg);
        assertIsValid(false);
    }

    @Test
    public void find_out_that_number_is_equal_to_max_inclusive() {
        final MaxNumberFieldValue msg = MaxNumberFieldValue.newBuilder().setValue(EQUAL_MAX).build();
        validate(msg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_number_is_less_than_max_inclusive() {
        final MaxNumberFieldValue msg = MaxNumberFieldValue.newBuilder().setValue(LESS_THAN_MAX).build();
        validate(msg);
        assertIsValid(true);
    }

    @Test
    public void provide_one_valid_violation_if_number_is_greater_than_max() {
        final MaxNumberFieldValue msg = MaxNumberFieldValue.newBuilder().setValue(GREATER_THAN_MAX).build();

        validate(msg);

        assertEquals(1, violations.size());
        final ConstraintViolation violation = firstViolation();
        assertEquals(GREATER_MAX_MSG, format(violation.getMsgFormat(), violation.getParam(0)));
        assertFieldPathIs(violation, VALUE);
        assertTrue(violation.getViolationList()
                            .isEmpty());
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
    public void provide_one_valid_violation_if_integral_digit_count_is_greater_than_max() {
        digitsCountTest(INT_DIGIT_COUNT_GREATER_THAN_MAX, /*valid=*/false);

        assertEquals(1, violations.size());
        final ConstraintViolation violation = firstViolation();
        assertEquals(
                "Number value is out of bounds, expected: <2 max digits>.<2 max digits>.",
                format(violation.getMsgFormat(), violation.getParam(0), violation.getParam(1))
        );
        assertFieldPathIs(violation, VALUE);
        assertTrue(violation.getViolationList()
                            .isEmpty());
    }

    /*
     * String pattern option tests.
     */

    @Test
    public void find_out_that_string_matches_to_regex_pattern() {
        final PatternStringFieldValue msg = PatternStringFieldValue.newBuilder()
                                                                   .setEmail("valid.email@mail.com")
                                                                   .build();
        validate(msg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_string_does_not_match_to_regex_pattern() {
        final PatternStringFieldValue msg = PatternStringFieldValue.newBuilder().setEmail("invalid email").build();
        validate(msg);
        assertIsValid(false);
    }

    @Test
    public void consider_field_is_valid_if_no_pattern_option_set() {
        validate(StringValue.getDefaultInstance());
        assertIsValid(true);
    }

    @Test
    public void provide_one_valid_violation_if_string_does_not_match_to_regex_pattern() {
        final PatternStringFieldValue msg = PatternStringFieldValue.newBuilder().setEmail("invalid.email").build();

        validate(msg);

        assertEquals(1, violations.size());
        final ConstraintViolation violation = firstViolation();
        assertEquals("String must match the regular expression '%s'.", violation.getMsgFormat());
        assertEquals(
                "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$",
                firstViolation().getParam(0));
        assertFieldPathIs(violation, "email");
        assertTrue(violation.getViolationList()
                            .isEmpty());
    }

    /*
     * Enclosed message field validation option tests.
     */

    @Test
    public void find_out_that_enclosed_message_field_is_valid() {
        final RequiredStringFieldValue enclosedMsg = RequiredStringFieldValue.newBuilder()
                                                                             .setValue(newUuid())
                                                                             .build();
        final EnclosedMessageFieldValue msg = EnclosedMessageFieldValue.newBuilder()
                                                                       .setOuterMsgField(enclosedMsg)
                                                                       .build();
        validate(msg);

        assertIsValid(true);
    }

    @Test
    public void find_out_that_enclosed_message_field_is_NOT_valid() {
        final RequiredStringFieldValue enclosedMsg = RequiredStringFieldValue.getDefaultInstance();
        final EnclosedMessageFieldValue msg = EnclosedMessageFieldValue.newBuilder()
                                                                       .setOuterMsgField(enclosedMsg)
                                                                       .build();
        validate(msg);

        assertIsValid(false);
    }

    @Test
    public void consider_field_is_valid_if_no_valid_option_set() {
        final RequiredStringFieldValue invalidEnclosedMsg = RequiredStringFieldValue.getDefaultInstance();
        final EnclosedMessageWithoutAnnotationFieldValue msg =
                EnclosedMessageWithoutAnnotationFieldValue.newBuilder()
                                                          .setOuterMsgField(invalidEnclosedMsg)
                                                          .build();
        validate(msg);

        assertIsValid(true);
    }

    @Test
    public void provide_valid_violations_if_enclosed_message_field_is_not_valid() {
        final RequiredStringFieldValue enclosedMsg = RequiredStringFieldValue.getDefaultInstance();
        final EnclosedMessageFieldValue msg = EnclosedMessageFieldValue.newBuilder()
                                                                       .setOuterMsgField(enclosedMsg)
                                                                       .build();
        validate(msg);

        assertEquals(1, violations.size());
        final ConstraintViolation violation = firstViolation();
        assertEquals("Message must have valid properties.", violation.getMsgFormat());
        assertFieldPathIs(violation, OUTER_MSG_FIELD);
        final List<ConstraintViolation> innerViolations = violation.getViolationList();
        assertEquals(1, innerViolations.size());

        final ConstraintViolation innerViolation = innerViolations.get(0);
        assertEquals(NO_VALUE_MSG, innerViolation.getMsgFormat());
        assertFieldPathIs(innerViolation, OUTER_MSG_FIELD, VALUE);
        assertTrue(innerViolation.getViolationList()
                                 .isEmpty());
    }

    /*
     * Entity ID in command validation tests.
     */

    @Test
    public void find_out_that_Message_entity_id_in_command_is_valid() {
        final EntityIdMsgFieldValue msg = EntityIdMsgFieldValue.newBuilder().setValue(newStringValue()).build();
        validate(msg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_Message_entity_id_in_command_is_NOT_valid() {
        validate(EntityIdMsgFieldValue.getDefaultInstance());
        assertIsValid(false);
    }

    @Test
    public void find_out_that_String_entity_id_in_command_is_valid() {
        final EntityIdStringFieldValue msg = EntityIdStringFieldValue.newBuilder().setValue(newUuid()).build();
        validate(msg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_String_entity_id_in_command_is_NOT_valid() {
        validate(EntityIdStringFieldValue.getDefaultInstance());
        assertIsValid(false);
    }

    @Test
    public void find_out_that_entity_id_in_command_cannot_be_ByteString() {
        final EntityIdByteStringFieldValue msg = EntityIdByteStringFieldValue.newBuilder()
                                                                             .setValue(newByteString())
                                                                             .build();
        validate(msg);
        assertIsValid(false);
    }

    @Test
    public void find_out_that_entity_id_in_command_cannot_be_float_number() {
        @SuppressWarnings("MagicNumber")
        final EntityIdDoubleFieldValue msg = EntityIdDoubleFieldValue.newBuilder().setValue(1.1).build();
        validate(msg);
        assertIsValid(false);
    }

    @Test
    public void find_out_that_Integer_entity_id_in_command_is_valid() {
        final EntityIdIntFieldValue msg = EntityIdIntFieldValue.newBuilder().setValue(5).build();
        validate(msg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_Integer_entity_id_in_command_is_NOT_valid() {
        validate(EntityIdIntFieldValue.getDefaultInstance());
        assertIsValid(false);
    }

    @Test
    public void find_out_that_Long_entity_id_in_command_is_valid() {
        final EntityIdLongFieldValue msg = EntityIdLongFieldValue.newBuilder().setValue(5).build();
        validate(msg);
        assertIsValid(true);
    }

    @Test
    public void find_out_that_Long_entity_id_in_command_is_NOT_valid() {
        validate(EntityIdLongFieldValue.getDefaultInstance());
        assertIsValid(false);
    }

    @Test
    public void find_out_that_repeated_entity_id_in_command_is_not_valid() {
        final EntityIdRepeatedFieldValue msg = EntityIdRepeatedFieldValue.newBuilder().addValue(newUuid()).build();
        validate(msg);
        assertIsValid(false);
    }

    @Test
    public void provide_one_valid_violation_if_entity_id_in_command_is_not_valid() {
        validate(EntityIdMsgFieldValue.getDefaultInstance());

        assertEquals(1, violations.size());
        final ConstraintViolation violation = firstViolation();
        assertEquals(NO_VALUE_MSG, violation.getMsgFormat());
        assertFieldPathIs(violation, VALUE);
        assertTrue(violation.getViolationList().isEmpty());
    }

    @Test
    public void provide_custom_invalid_field_message_if_specified() {
        validate(EnclosedMessageFieldValueWithCustomInvalidMessage.getDefaultInstance());

        assertSize(1, violations);
        final ConstraintViolation violation = firstViolation();
        assertEquals("Custom error", violation.getMsgFormat());
    }

    @Test
    public void ignore_custom_invalid_field_message_if_validation_is_disabled() {
        validate(EnclosedMessageFieldValueWithoutAnnotationFieldValueWithCustomInvalidMessage.getDefaultInstance());
        assertIsValid(true);
    }

    /*
     * Utility methods.
     */

    private void minDecimalNumberTest(double value, boolean inclusive, boolean isValid) {
        final Message msg = inclusive ?
                            DecimalMinIncNumberFieldValue.newBuilder().setValue(value).build() :
                            DecimalMinNotIncNumberFieldValue.newBuilder().setValue(value).build();
        validate(msg);
        assertIsValid(isValid);
    }

    private void maxDecimalNumberTest(double value, boolean inclusive, boolean isValid) {
        final Message msg = inclusive ?
                            DecimalMaxIncNumberFieldValue.newBuilder().setValue(value).build() :
                            DecimalMaxNotIncNumberFieldValue.newBuilder().setValue(value).build();
        validate(msg);
        assertIsValid(isValid);
    }

    private void digitsCountTest(double value, boolean isValid) {
        final Message msg = DigitsCountNumberFieldValue.newBuilder().setValue(value).build();
        validate(msg);
        assertIsValid(isValid);
    }

    private void validate(Message msg) {
        violations = validator.validate(msg);
    }

    private ConstraintViolation firstViolation() {
        return violations.get(0);
    }

    private void assertIsValid(boolean isValid) {
        if (isValid) {
            assertTrue(violations.isEmpty());
        } else {
            assertFalse(violations.isEmpty());
            for (ConstraintViolation violation : violations) {
                final String format = violation.getMsgFormat();
                assertTrue(!format.isEmpty());
                final boolean noParams = violation.getParamList().isEmpty();
                if (format.contains("%s")) {
                    assertFalse(noParams);
                } else {
                    assertTrue(noParams);
                }
                assertFalse(violation.getFieldPath().getFieldNameList().isEmpty());
            }
        }
    }

    private static void assertFieldPathIs(ConstraintViolation violation, String... expectedFields) {
        final FieldPath path = violation.getFieldPath();
        final ProtocolStringList actualFields = path.getFieldNameList();
        assertEquals(expectedFields.length, actualFields.size());
        assertEquals(copyOf(expectedFields), copyOf(actualFields));
    }

    private static Timestamp getFuture() {
        final Timestamp future = add(getCurrentTime(), Durations2.fromMinutes(5));
        return future;
    }

    private static Timestamp getPast() {
        final Timestamp past = subtract(getCurrentTime(), Durations2.fromMinutes(5));
        return past;
    }

    private static StringValue newStringValue() {
        return Values.newStringValue(newUuid());
    }

    private static ByteString newByteString() {
        final ByteString bytes = ByteString.copyFromUtf8(newUuid());
        return bytes;
    }
}
