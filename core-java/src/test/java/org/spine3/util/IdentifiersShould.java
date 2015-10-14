/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.util;

import com.google.common.base.Function;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.test.*;
import org.spine3.test.project.ProjectId;

import javax.annotation.Nullable;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.assertEquals;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.util.Identifiers.*;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods", "DuplicateStringLiteralInspection"})
public class IdentifiersShould {

    private static final String TEST_ID = "someTestId 1234567890 !@#$%^&()[]{}-+=_";

    @Test
    public void return_same_string_when_convert_string() {

        final TestIdWithStringField id = newTestIdWithStringField(TEST_ID);

        final String result = idToString(id);

        assertEquals(TEST_ID, result);
    }

    @Test
    public void convert_to_string_integer_id() {

        final Integer value = 256;
        final TestIdWithIntField id = TestIdWithIntField.newBuilder().setId(value).build();
        final String expected = value.toString();

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_long_id() {

        final Long value = 256L;
        final TestIdWithLongField id = TestIdWithLongField.newBuilder().setId(value).build();
        final String expected = value.toString();

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_registered_project_id_message() {

        IdConverterRegistry.instance().register(ProjectId.class, ID_TO_STRING_CONVERTER);

        ProjectId id = ProjectId.newBuilder().setId(TEST_ID).build();
        final String result = idToString(id);

        assertEquals(TEST_ID, result);
    }

    @Test
    public void convert_to_string_message_id_with_string_field() {

        final TestIdWithStringField id = newTestIdWithStringField(TEST_ID);

        final String result = idToString(id);

        assertEquals(TEST_ID, result);
    }

    @Test
    public void convert_to_string_message_id_with_integer_field() {

        final Integer value = 256;
        final TestIdWithIntField id = TestIdWithIntField.newBuilder().setId(value).build();
        final String expected = value.toString();

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_long_field() {

        final Long value = 256L;
        final TestIdWithLongField id = TestIdWithLongField.newBuilder().setId(value).build();
        final String expected = value.toString();

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_timestamp_field() {

        final Timestamp currentTime = getCurrentTime();
        final TestIdWithTimestampField id = TestIdWithTimestampField.newBuilder().setId(currentTime).build();
        final String expected = timestampToString(currentTime);

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_message_field() {

        final TestIdWithStringField value = newTestIdWithStringField(TEST_ID);
        final TestIdWithMessageField idToConvert = TestIdWithMessageField.newBuilder().setId(value).build();

        final String result = idToString(idToConvert);

        assertEquals(TEST_ID, result);
    }

    @Test
    public void convert_to_string_message_id_with_several_fields() {

        final String nestedString = "nested_string";
        final String outerString = "outer_string";
        final Integer number = 256;

        final TestIdWithMultipleFields idToConvert = TestIdWithMultipleFields.newBuilder()
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

        final TestIdWithStringField messageToWrap = newTestIdWithStringField(TEST_ID);
        final Any any = toAny(messageToWrap);

        final String result = idToString(any);

        assertEquals(TEST_ID, result);
    }

    private static TestIdWithStringField newTestIdWithStringField(String nestedString) {
        return TestIdWithStringField.newBuilder().setId(nestedString).build();
    }

    private static final Function<ProjectId, String> ID_TO_STRING_CONVERTER = new Function<ProjectId, String>() {
        @Override
        public String apply(@Nullable ProjectId projectId) {
            return projectId != null ? projectId.getId() : NULL_ID_OR_FIELD;
        }
    };
}
