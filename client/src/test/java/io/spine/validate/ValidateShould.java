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

package io.spine.validate;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.protobuf.Wrapper;
import io.spine.test.Tests;
import io.spine.type.TypeName;
import org.junit.Test;

import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Tests.newUuidValue;
import static io.spine.validate.Validate.checkBounds;
import static io.spine.validate.Validate.checkDefault;
import static io.spine.validate.Validate.checkDefaultState;
import static io.spine.validate.Validate.checkNotDefault;
import static io.spine.validate.Validate.checkNotDefaultState;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;
import static io.spine.validate.Validate.isDefault;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValidateShould {

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Validate.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_int_value_is_not_positive() {
        Validate.checkPositive(-1, "negativeInteger");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_long_value_is_not_positive() {
        Validate.checkPositive(-2L, "negativeLong");
    }

    @Test
    public void verify_that_message_is_not_in_default_state() {
        final Message msg = Wrapper.forString("check_if_message_is_not_in_default_state");

        assertTrue(isNotDefault(msg));
        assertFalse(isNotDefault(StringValue.getDefaultInstance()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_checked_value_out_of_bounds() {
        checkBounds(10, "checked value", -5, 9);
    }

    @Test
    public void verify_that_message_is_in_default_state() {
        final Message nonDefault = newUuidValue();

        assertTrue(isDefault(StringValue.getDefaultInstance()));
        assertFalse(isDefault(nonDefault));
    }

    @Test(expected = IllegalStateException.class)
    public void check_if_message_is_in_default_state() {
        final StringValue nonDefault = newUuidValue();
        checkDefaultState(nonDefault);
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_if_message_is_default() {
        final StringValue nonDefault = newUuidValue();
        checkDefault(nonDefault);
    }

    @Test(expected = IllegalStateException.class)
    public void check_a_message_is_in_default_state_with_parametrized_error_message() {
        final StringValue nonDefault = newUuidValue();
        checkDefaultState(nonDefault,
                          "Message value: %s, Type name: %s",
                          nonDefault,
                          TypeName.of(nonDefault));
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_a_message_is_default_with_parametrized_error_message() {
        final StringValue nonDefault = newUuidValue();
        checkDefault(nonDefault,
                     "Message value: %s, Type name: %s",
                     nonDefault,
                     TypeName.of(nonDefault));
    }

    @Test
    public void return_default_value_on_checkDefaultState() {
        final Message defaultValue = StringValue.getDefaultInstance();
        assertEquals(defaultValue, checkDefaultState(defaultValue));
        assertEquals(defaultValue, checkDefaultState(defaultValue, "error message"));
        assertEquals(defaultValue, checkDefaultState(defaultValue,
                                                     "error message",
                                                     "error args"));
    }

    @Test
    public void return_default_value_on_checkDefault() {
        final Message defaultValue = StringValue.getDefaultInstance();
        assertEquals(defaultValue, checkDefault(defaultValue));
        assertEquals(defaultValue, checkDefault(defaultValue, "error message"));
        assertEquals(defaultValue, checkDefaultState(defaultValue,
                                                     "error message",
                                                     "error args"));
    }

    @Test(expected = IllegalStateException.class)
    public void check_if_message_is_in_not_in_default_state_throwing_exception_if_not() {
        checkNotDefaultState(StringValue.getDefaultInstance());
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_if_message_is_not_default_throwing_exception_if_not() {
        checkNotDefault(StringValue.getDefaultInstance());
    }

    @Test
    public void return_non_default_value_on_checkNotDefaultState() {
        final StringValue nonDefault = newUuidValue();
        assertEquals(nonDefault, checkNotDefaultState(nonDefault));
        assertEquals(nonDefault, checkNotDefaultState(nonDefault, "with error message"));
    }

    @Test
    public void return_non_default_value_on_checkNotDefault() {
        final StringValue nonDefault = newUuidValue();
        assertEquals(nonDefault, checkNotDefault(nonDefault));
        assertEquals(nonDefault, checkNotDefault(nonDefault, "with error message"));
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_checked_string_is_null() {
        checkNotEmptyOrBlank(Tests.<String>nullRef(), "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_checked_string_is_empty() {
        checkNotEmptyOrBlank("", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_checked_string_is_blank() {
        checkNotEmptyOrBlank("   ", "");
    }

    @Test
    public void do_not_throw_exception_if_checked_string_is_valid() {
        checkNotEmptyOrBlank("valid_string", "");
    }

    @Test
    public void format_message_from_constraint_violation() {
        final ConstraintViolation violation = ConstraintViolation.newBuilder()
                                                                 .setMsgFormat("test %s test %s")
                                                                 .addParam("1")
                                                                 .addParam("2")
                                                                 .build();
        final String formatted = ConstraintViolations.toText(violation);

        assertEquals("test 1 test 2", formatted);
    }

    @Test
    public void format_message_using_params_from_constraint_violation() {
        final ConstraintViolation violation = ConstraintViolation.newBuilder()
                                                                 .addParam("1")
                                                                 .addParam("2")
                                                                 .build();
        final String formatted = ConstraintViolations.toText("abc %s abc %s", violation);

        assertEquals("abc 1 abc 2", formatted);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .testAllPublicStaticMethods(Validate.class);
    }
}
