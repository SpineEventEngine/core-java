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
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.junit.Test;
import org.spine3.test.*;
import org.spine3.test.project.ProjectId;

import javax.annotation.Nullable;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.util.Identifiers.idToString;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public class IdentifiersShould {

    @Test
    public void convert_to_string_registered_project_id_message() {

        Identifiers.IdConverterRegistry.instance().register(ProjectId.class, ID_TO_STRING_CONVERTER);

        final String expected = "project123";
        ProjectId id = ProjectId.newBuilder().setId(expected).build();
        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_string_field() {

        final String expected = "user1234";
        final TestIdWithStringField id = TestIdWithStringField.newBuilder().setId(expected).build();

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_integer_field() {

        final Integer value = 256;
        final String expected = value.toString();
        final TestIdWithIntField id = TestIdWithIntField.newBuilder().setId(value).build();

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_long_field() {

        final Long value = 256L;
        final String expected = value.toString();
        final TestIdWithLongField id = TestIdWithLongField.newBuilder().setId(value).build();

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_timestamp_field() {

        final Timestamp currentTime = getCurrentTime();
        final String expected = TimeUtil.toString(currentTime);
        final TestIdWithTimestampField id = TestIdWithTimestampField.newBuilder().setId(currentTime).build();

        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_message_field() {

        final String expected = "user123123";
        final TestIdWithStringField value = TestIdWithStringField.newBuilder().setId(expected).build();
        final TestIdWithMessageField idToConvert = TestIdWithMessageField.newBuilder().setId(value).build();

        final String actual = idToString(idToConvert);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_to_string_message_id_with_several_fields() {

        final String nestedString = "inner_string";
        final String outerString = "outer_string";
        final Integer number = 256;

        final TestIdWithStringField message = TestIdWithStringField.newBuilder().setId(nestedString).build();
        final TestIdWithMultipleFields idToConvert = TestIdWithMultipleFields.newBuilder().setString(outerString).setInt(number).setMessage(message).build();

        final String actual = idToString(idToConvert);

        assertTrue(actual.contains(outerString));
        assertTrue(actual.contains(nestedString));
        assertTrue(actual.contains(number.toString()));
    }

    private static final Function<ProjectId, String> ID_TO_STRING_CONVERTER = new Function<ProjectId, String>() {
        @Override
        public String apply(@Nullable ProjectId projectId) {
            return projectId != null ? projectId.getId() : Identifiers.NULL_ID_OR_FIELD;
        }
    };
}
