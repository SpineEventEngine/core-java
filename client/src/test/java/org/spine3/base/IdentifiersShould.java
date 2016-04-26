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

package org.spine3.base;

import com.google.common.base.Function;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.test.IdWithStructure;
import org.spine3.test.IntFieldId;
import org.spine3.test.LongFieldId;
import org.spine3.test.NestedMessageId;
import org.spine3.test.SeveralFieldsId;
import org.spine3.test.StringFieldId;
import org.spine3.test.TimestampFieldId;

import javax.annotation.Nullable;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.*;
import static org.spine3.base.Identifiers.*;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "DuplicateStringLiteralInspection"})
public class IdentifiersShould {

    private static final String TEST_ID = "someTestId 1234567890 !@#$%^&()[]{}-+=_";

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Identifiers.class));
    }

    @SuppressWarnings("UnnecessaryBoxing") // OK as we want to show types clearly.
    @Test
    public void convert_to_string_number_ids() {
        assertEquals("10", idToString(Integer.valueOf(10)));
        assertEquals("100", idToString(Long.valueOf(100)));
    }

    @Test
    public void return_string_id_as_is() {
        assertEquals(TEST_ID, idToString(TEST_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_object_of_unsuppored_class_passed() {
        //noinspection UnnecessaryBoxing
        idToString(Boolean.valueOf(true));
    }

    @Test
    public void return_same_string_when_convert_string_wrapped_into_message() {

        final StringFieldId id = newTestIdWithStringField(TEST_ID);

        final String result = idToString(id);

        assertEquals(TEST_ID, result);
    }

    @Test
    public void convert_to_string_integer_id_wrapped_into_message() {
        final Integer value = 1024;
        final IntFieldId id = IntFieldId.newBuilder().setId(value).build();
        final String expected = value.toString();

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_long_id_wrapped_into_message() {
        final Long value = 100500L;
        final LongFieldId id = LongFieldId.newBuilder().setId(value).build();
        final String expected = value.toString();

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_string_field() {
        final StringFieldId id = newTestIdWithStringField(TEST_ID);

        final String result = idToString(id);

        assertEquals(TEST_ID, result);
    }

    @Test
    public void convert_to_string_message_id_with_timestamp_field() {
        final Timestamp currentTime = getCurrentTime();
        final TimestampFieldId id = TimestampFieldId.newBuilder().setId(currentTime).build();
        final String expected = timestampToIdString(currentTime);

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_message_field() {
        final StringFieldId value = newTestIdWithStringField(TEST_ID);
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
                .setMessage(newTestIdWithStringField(nestedString))
                .build();

        final String expected =
                "string=\"" + outerString + '\"' +
                " int=" + number +
                " message { id=\"" + nestedString + "\" }";

        final String actual = idToString(idToConvert);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_wrapped_in_Any() {

        final StringFieldId messageToWrap = newTestIdWithStringField(TEST_ID);
        final Any any = toAny(messageToWrap);

        final String result = idToString(any);

        assertEquals(TEST_ID, result);
    }

    private static StringFieldId newTestIdWithStringField(String nestedString) {
        return StringFieldId.newBuilder().setId(nestedString).build();
    }

    private static final Function<IdWithStructure, String> ID_TO_STRING_CONVERTER = new Function<IdWithStructure, String>() {
        @Override
        public String apply(@Nullable IdWithStructure id) {
            if (id == null) {
                return NULL_ID_OR_FIELD;
            }
            return id.getName();
        }
    };

    @Test
    public void convert_to_string_registered_id_message_type() {
        ConverterRegistry.getInstance().register(IdWithStructure.class, ID_TO_STRING_CONVERTER);

        final IdWithStructure id = IdWithStructure.newBuilder().setName(TEST_ID).build();
        final String result = idToString(id);

        assertEquals(TEST_ID, result);
    }

    @Test
    public void generate_new_UUID() {
        // We have non-empty values.
        assertTrue(Identifiers.newUuid().length() > 0);

        // Values are random.
        assertNotEquals(Identifiers.newUuid(), Identifiers.newUuid());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_convert_unsupported_ID_type_to_Any() {
        //noinspection UnnecessaryBoxing
        idToAny(Boolean.valueOf(false));
    }

    @Test
    public void handle_null_in_standard_converters() {
        final ConverterRegistry registry = ConverterRegistry.getInstance();

        assertEquals(Identifiers.NULL_ID_OR_FIELD,
                registry.getConverter(Timestamp.getDefaultInstance()).apply(null));

        assertEquals(Identifiers.NULL_ID_OR_FIELD,
                registry.getConverter(EventId.getDefaultInstance()).apply(null));

        assertEquals(Identifiers.NULL_ID_OR_FIELD,
                registry.getConverter(CommandId.getDefaultInstance()).apply(null));
    }
}
