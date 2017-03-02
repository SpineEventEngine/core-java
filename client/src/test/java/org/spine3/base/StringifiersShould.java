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

import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.test.NullToleranceTest;
import org.spine3.test.identifiers.IdWithPrimitiveFields;
import org.spine3.test.identifiers.NestedMessageId;
import org.spine3.test.identifiers.SeveralFieldsId;
import org.spine3.test.identifiers.TimestampFieldId;

import javax.annotation.Nullable;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Stringifiers.EMPTY_ID;
import static org.spine3.base.Stringifiers.NULL_ID;
import static org.spine3.base.Stringifiers.idToString;
import static org.spine3.base.Stringifiers.toIdString;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;
import static org.spine3.protobuf.Values.newIntValue;
import static org.spine3.protobuf.Values.newLongValue;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

public class StringifiersShould {

    private static final String TEST_ID = "someTestId 1234567890 !@#$%^&()[]{}-+=_";

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Stringifiers.class);
    }

    @Test
    public void return_NULL_ID_if_convert_null_to_string() {
        assertEquals(NULL_ID, idToString(null));
    }

    @Test
    public void return_EMPTY_ID_if_convert_empty_string_to_string() {
        assertEquals(EMPTY_ID, idToString(""));
    }

    @Test
    public void return_EMPTY_ID_if_convert_blank_string_to_string() {
        assertEquals(EMPTY_ID, idToString(" "));
    }

    @Test
    public void return_EMPTY_ID_if_result_of_Message_to_string_conversion_is_empty_string() {
        assertEquals(EMPTY_ID, idToString(CommandId.getDefaultInstance()));
    }

    @Test
    public void return_EMPTY_ID_if_result_of_Message_to_string_conversion_is_blank_string() {
        assertEquals(EMPTY_ID, idToString(newStringValue("  ")));
    }

    @Test
    public void return_EMPTY_ID_if_convert_empty_message_to_string() {
        assertEquals(EMPTY_ID, idToString(StringValue.getDefaultInstance()));
    }

    @Test
    public void return_string_id_as_is() {
        assertEquals(TEST_ID, idToString(TEST_ID));
    }

    @Test
    public void return_same_string_when_convert_string_wrapped_into_message() {

        final StringValue id = newStringValue(TEST_ID);

        final String result = idToString(id);

        assertEquals(TEST_ID, result);
    }

    @Test
    public void convert_to_string_integer_id_wrapped_into_message() {
        final Integer value = 1024;
        final Int32Value id = newIntValue(value);
        final String expected = value.toString();

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_long_id_wrapped_into_message() {
        final Long value = 100500L;
        final Int64Value id = newLongValue(value);
        final String expected = value.toString();

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_string_field() {
        final StringValue id = newStringValue(TEST_ID);

        final String result = idToString(id);

        assertEquals(TEST_ID, result);
    }

    @Test
    public void convert_to_string_message_id_with_timestamp_field() {
        final Timestamp currentTime = getCurrentTime();
        final TimestampFieldId id = TimestampFieldId.newBuilder().setId(currentTime).build();
        final String expected = toIdString(currentTime);

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_message_field() {
        final StringValue value = newStringValue(TEST_ID);
        final NestedMessageId idToConvert = NestedMessageId.newBuilder().setId(value).build();

        final String result = idToString(idToConvert);

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
                                                           .setMessage(newStringValue(nestedString))
                                                           .build();

        final String expected =
                "string=\"" + outerString + '\"' +
                        " int=" + number +
                        " message { value=\"" + nestedString + "\" }";

        final String actual = idToString(idToConvert);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_wrapped_in_Any() {
        final StringValue messageToWrap = newStringValue(TEST_ID);
        final Any any = AnyPacker.pack(messageToWrap);

        final String result = idToString(any);

        assertEquals(TEST_ID, result);
    }

    private static final Stringifier<IdWithPrimitiveFields> ID_TO_STRING_CONVERTER =
            new Stringifier<IdWithPrimitiveFields>() {
        @Override
        public String apply(@Nullable IdWithPrimitiveFields id) {
            if (id == null) {
                return NULL_ID;
            }
            return id.getName();
        }
    };

    @Test
    public void convert_to_string_registered_id_message_type() {
        StringifierRegistry.getInstance().register(IdWithPrimitiveFields.class, ID_TO_STRING_CONVERTER);

        final IdWithPrimitiveFields id = IdWithPrimitiveFields.newBuilder().setName(TEST_ID).build();
        final String result = idToString(id);

        assertEquals(TEST_ID, result);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // OK as these are standard Stringifiers we add ourselves.
    @Test
    public void handle_null_in_standard_converters() {
        final StringifierRegistry registry = StringifierRegistry.getInstance();

        assertEquals(NULL_ID, registry.get(Timestamp.class).get().apply(null));
        assertEquals(NULL_ID, registry.get(EventId.class).get().apply(null));
        assertEquals(NULL_ID, registry.get(CommandId.class).get().apply(null));
    }

    @Test
    public void return_false_on_attempt_to_find_unregistered_type() {
        assertFalse(StringifierRegistry.getInstance().hasStringiferFor(Random.class));
    }

    @Test
    public void convert_command_id_to_string() {
        final CommandId id = Commands.generateId();
        final String actual = new Stringifiers.CommandIdStringifier().apply(id);

        assertEquals(idToString(id), actual);
    }

    @Test
    public void convert_event_id_to_string() {
        final EventId id = Events.generateId();
        final String actual = new Stringifiers.EventIdStringifier().apply(id);

        assertEquals(idToString(id), actual);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(Stringifiers.class)
                                                                     .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }
}
