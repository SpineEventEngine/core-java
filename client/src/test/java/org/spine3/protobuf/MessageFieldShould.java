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

package org.spine3.protobuf;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Test;

import static com.google.protobuf.Descriptors.FieldDescriptor;
import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import static org.junit.Assert.assertEquals;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Values.newStringValue;

@SuppressWarnings("InstanceMethodNamingConvention")
public class MessageFieldShould {

    private static final int STR_VALUE_FIELD_INDEX = 0;

    @SuppressWarnings("DuplicateStringLiteralInspection")
    public static final String STR_VALUE_FIELD_NAME = "value";

    private final StringValue stringValue = newStringValue(newUuid());

    @Test
    public void accept_positive_index() {
        final int index = 5;

        final MessageField field = new TestMessageField(index);

        assertEquals(index, field.getIndex());
    }

    @Test
    public void accept_zero_index() {
        final int index = 0;

        final MessageField field = new TestMessageField(index);

        assertEquals(index, field.getIndex());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_field_index_is_negative() {
        // noinspection ResultOfObjectAllocationIgnored
        new TestMessageField(-5);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_field_is_not_available() {
        final TestMessageField field = new TestMessageField(STR_VALUE_FIELD_INDEX);
        field.setIsFieldAvailable(false);

        field.getValue(stringValue);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void throw_exception_if_no_field_by_given_index() {
        final TestMessageField field = new TestMessageField(Integer.MAX_VALUE);

        field.getValue(stringValue);
    }

    @Test
    public void return_field_value() {
        final TestMessageField field = new TestMessageField(STR_VALUE_FIELD_INDEX);

        final Object value = field.getValue(stringValue);

        assertEquals(stringValue.getValue(), value);
    }

    @Test
    public void return_field_descriptor() {
        final FieldDescriptor descriptor = MessageField.getFieldDescriptor(stringValue, STR_VALUE_FIELD_INDEX);

        assertEquals(JavaType.STRING, descriptor.getJavaType());
    }

    @Test
    public void return_field_name() {
        final String fieldName = MessageField.getFieldName(stringValue, STR_VALUE_FIELD_INDEX);

        assertEquals(STR_VALUE_FIELD_NAME, fieldName);
    }

    @Test
    public void convert_field_name_to_method_name() {
        assertEquals("getUserId", MessageField.toAccessorMethodName("user_id"));
        assertEquals("getId", MessageField.toAccessorMethodName("id"));
        assertEquals("getAggregateRootId", MessageField.toAccessorMethodName("aggregate_root_id"));
    }

    private static class TestMessageField extends MessageField {

        private boolean isFieldAvailable = true;

        protected TestMessageField(int index) {
            super(index);
        }

        void setIsFieldAvailable(boolean isFieldAvailable) {
            this.isFieldAvailable = isFieldAvailable;
        }

        @Override
        protected RuntimeException createUnavailableFieldException(Message message, String fieldName) {
            return new IllegalStateException("");
        }

        @Override
        protected boolean isFieldAvailable(Message message) {
            return isFieldAvailable;
        }
    }
}
