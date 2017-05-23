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

package org.spine3.base;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import org.junit.Test;
import org.spine3.base.Identifier.Type;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Wrapper;
import org.spine3.test.identifiers.NestedMessageId;
import org.spine3.test.identifiers.SeveralFieldsId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifier.EMPTY_ID;
import static org.spine3.base.Identifier.NULL_ID;
import static org.spine3.base.Identifier.newUuid;
import static org.spine3.base.Identifier.pack;
import static org.spine3.protobuf.Wrapper.forInteger;
import static org.spine3.protobuf.Wrapper.forLong;
import static org.spine3.protobuf.Wrapper.forString;
import static org.spine3.protobuf.Wrapper.forUnsignedInteger;
import static org.spine3.protobuf.Wrapper.forUnsignedLong;
import static org.spine3.test.Tests.newUuidValue;

public class IdentifierShould {

    private static final String TEST_ID = "someTestId 1234567890 !@#$%^&()[]{}-+=_";

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_object_of_unsupported_class_passed() {
        //noinspection UnnecessaryBoxing
        Identifier.toString(Boolean.valueOf(true));
    }

    @SuppressWarnings("UnnecessaryBoxing") // OK as we want to show types clearly.
    @Test
    public void convert_to_string_number_ids() {
        assertEquals("10", Identifier.toString(Integer.valueOf(10)));
        assertEquals("100", Identifier.toString(Long.valueOf(100)));
    }

    @Test
    public void unpack_passed_Any() {
        final StringValue id = newUuidValue();
        assertEquals(id.getValue(), Identifier.toString(AnyPacker.pack(id)));
    }

    @Test
    public void generate_new_UUID() {
        // We have non-empty values.
        assertTrue(newUuid().length() > 0);

        // Values are random.
        assertNotEquals(newUuid(), newUuid());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_convert_unsupported_ID_type_to_Any() {
        //noinspection UnnecessaryBoxing
        pack(Boolean.valueOf(false));
    }

    @Test
    public void return_NULL_ID_if_convert_null_to_string() {
        assertEquals(NULL_ID, Identifier.toString(null));
    }

    @Test
    public void return_EMPTY_ID_if_convert_empty_string_to_string() {
        assertEquals(EMPTY_ID, Identifier.toString(""));
    }

    @Test
    public void return_EMPTY_ID_if_result_of_Message_to_string_conversion_is_empty_string() {
        assertEquals(EMPTY_ID, Identifier.toString(CommandId.getDefaultInstance()));
    }

    @Test
    public void return_EMPTY_ID_if_convert_empty_message_to_string() {
        assertEquals(EMPTY_ID, Identifier.toString(StringValue.getDefaultInstance()));
    }

    @Test
    public void return_string_id_as_is() {
        assertEquals(TEST_ID, Identifier.toString(TEST_ID));
    }

    @Test
    public void return_same_string_when_convert_string_wrapped_into_message() {

        final StringValue id = forString(TEST_ID);

        final String result = Identifier.toString(id);

        assertEquals(TEST_ID, result);
    }

    @Test
    public void convert_to_string_integer_id_wrapped_into_message() {
        final Integer value = 1024;
        final Int32Value id = forInteger(value);
        final String expected = value.toString();

        final String actual = Identifier.toString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_long_id_wrapped_into_message() {
        final Long value = 100500L;
        final Int64Value id = forLong(value);
        final String expected = value.toString();

        final String actual = Identifier.toString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_string_field() {
        final StringValue id = forString(TEST_ID);

        final String result = Identifier.toString(id);

        assertEquals(TEST_ID, result);
    }

    @Test
    public void convert_to_string_message_id_with_message_field() {
        final StringValue value = forString(TEST_ID);
        final NestedMessageId idToConvert = NestedMessageId.newBuilder()
                                                           .setId(value)
                                                           .build();

        final String result = Identifier.toString(idToConvert);

        assertEquals(TEST_ID, result);
    }

    @Test
    public void have_default_to_string_conversion_of_message_id_with_several_fields() {
        final String nestedString = "nested_string";
        final String outerString = "outer_string";
        final Integer number = 256;

        final SeveralFieldsId idToConvert = SeveralFieldsId.newBuilder()
                                                           .setString(outerString)
                                                           .setInt(number)
                                                           .setMessage(
                                                                   forString(nestedString))
                                                           .build();

        final String expected =
                "string=\"" + outerString + '\"' +
                        " int=" + number +
                        " message { value=\"" + nestedString + "\" }";

        final String actual = Identifier.toString(idToConvert);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_wrapped_in_Any() {
        final StringValue messageToWrap = forString(TEST_ID);
        final Any any = AnyPacker.pack(messageToWrap);

        final String result = Identifier.toString(any);

        assertEquals(TEST_ID, result);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .testAllPublicStaticMethods(Identifier.class);
    }

    @Test
    public void getDefaultValue_by_class_id() {
        assertEquals(0L, Identifier.getDefaultValue(Long.class)
                                   .longValue());
        assertEquals(0, Identifier.getDefaultValue(Integer.class)
                                  .intValue());
        assertEquals("", Identifier.getDefaultValue(String.class));
        assertEquals(Timestamp.getDefaultInstance(), Identifier.getDefaultValue(Timestamp.class));
    }

    @Test
    public void create_values_by_type() {
        assertTrue(Identifier.from("")
                             .isString());
        assertTrue(Identifier.from(0)
                             .isInteger());
        assertTrue(Identifier.from(0L)
                             .isLong());
        assertTrue(Identifier.from(Wrapper.forInteger(300))
                             .isMessage());
    }

    @Test
    public void recognize_type_by_supported_message_type() {
        assertTrue(Type.INTEGER.matchMessage(forUnsignedInteger(10)));
        assertTrue(Type.LONG.matchMessage(forUnsignedLong(1020L)));
        assertTrue(Type.STRING.matchMessage(Wrapper.forString("")));
        assertTrue(Type.MESSAGE.matchMessage(Timestamp.getDefaultInstance()));

        assertFalse(Type.MESSAGE.matchMessage(StringValue.getDefaultInstance()));
        assertFalse(Type.MESSAGE.matchMessage(UInt32Value.getDefaultInstance()));
        assertFalse(Type.MESSAGE.matchMessage(UInt64Value.getDefaultInstance()));
    }

    @Test
    public void create_values_depending_from_message_type() {
        assertEquals(10, Type.INTEGER.fromMessage(forUnsignedInteger(10)));
        assertEquals(1024L, Type.LONG.fromMessage(forUnsignedLong(1024L)));
        final String value = getClass().getSimpleName();
        assertEquals(value, Type.STRING.fromMessage(Wrapper.forString(value)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_unpack_unsupported_types() {
        Identifier.idFromAny(Any.getDefaultInstance());
    }
}
