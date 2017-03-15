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

package org.spine3.server.reflect;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.base.FieldFilter;
import org.spine3.protobuf.Messages;
import org.spine3.test.messages.MessageWithStringValue;
import org.spine3.test.messages.TestEnum;
import org.spine3.test.messages.TestEnumValue;
import org.spine3.type.TypeUrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Yevsyukov
 */
public class FieldShould {

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(FieldFilter.class, FieldFilter.getDefaultInstance())
                .testAllPublicStaticMethods(Field.class);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // OK as the field is present in this type.
    @Test
    public void return_name() {
        final String fieldName = "seconds";
        assertEquals(fieldName, Field.newField(Timestamp.class, fieldName)
                                     .get()
                                     .getName());
    }

    @Test
    public void return_absent_for_missing_field() {
        assertFalse(Field.newField(Timestamp.class, "min")
                         .isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_field_filter_without_a_field_name() {
        Field.forFilter(Timestamp.class, FieldFilter.getDefaultInstance());
    }

    private static void assertReturnsFieldClass(Class<?> expectedClass, Descriptor msgDescriptor) {
        final FieldDescriptor field = msgDescriptor.getFields()
                                                   .get(0);

        assertEquals(expectedClass, Field.getFieldClass(field));
    }

    @Test
    public void return_int_msg_field_class_by_descriptor() {
        assertReturnsFieldClass(Integer.class, Int32Value.getDescriptor());
    }

    @Test
    public void return_long_msg_field_class_by_descriptor() {
        assertReturnsFieldClass(Long.class, Int64Value.getDescriptor());
    }

    @Test
    public void return_float_msg_field_class_by_descriptor() {
        assertReturnsFieldClass(Float.class, FloatValue.getDescriptor());
    }

    @Test
    public void return_double_msg_field_class_by_descriptor() {
        assertReturnsFieldClass(Double.class, DoubleValue.getDescriptor());
    }

    @Test
    public void return_boolean_msg_field_class_by_descriptor() {
        assertReturnsFieldClass(Boolean.class, BoolValue.getDescriptor());
    }

    @Test
    public void return_string_msg_field_class_by_descriptor() {
        assertReturnsFieldClass(String.class, StringValue.getDescriptor());
    }

    @Test
    public void return_byte_string_msg_field_class_by_descriptor() {
        assertReturnsFieldClass(ByteString.class, BytesValue.getDescriptor());
    }

    @Test
    public void return_enum_msg_field_class_by_descriptor() {
        assertReturnsFieldClass(TestEnum.class, TestEnumValue.getDescriptor());
    }

    @Test
    public void return_msg_field_class_by_descriptor() {
        assertReturnsFieldClass(StringValue.class, MessageWithStringValue.getDescriptor());
    }

    @Test
    public void pass_the_null_tolerance_check() {
        final FieldDescriptor defaultFieldDescriptor = StringValue.getDefaultInstance()
                                                                  .getDescriptorForType()
                                                                  .getFields()
                                                                  .get(0);
        new NullPointerTester()
                .setDefault(TypeUrl.class, TypeUrl.of(StringValue.class))
                .setDefault(FieldDescriptor.class, defaultFieldDescriptor)
                .testAllPublicStaticMethods(Messages.class);
    }
}
