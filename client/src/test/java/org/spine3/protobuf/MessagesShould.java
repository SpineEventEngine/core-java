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

import com.google.common.collect.Lists;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import org.junit.Test;
import org.spine3.test.Tests;
import org.spine3.test.messages.MessageWithStringValue;
import org.spine3.test.messages.TestEnum;
import org.spine3.test.messages.TestEnumValue;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.Values.newStringValue;

@SuppressWarnings("InstanceMethodNamingConvention")
public class MessagesShould {

    @Test
    public void have_private_utility_ctor() {
        assertTrue(Tests.hasPrivateParameterlessCtor(Messages.class));
    }

    @Test(expected = NullPointerException.class)
    public void toText_fail_on_null() {
        Messages.toText(Tests.<Message>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void toJson_fail_on_null() {
        Messages.toJson(Tests.<Message>nullRef());
    }


    @Test
    public void print_to_json() {
        final StringValue value = newStringValue("print_to_json");
        assertFalse(Messages.toJson(value)
                            .isEmpty());
    }

    @Test
    public void build_JsonFormat_registry_for_known_types() {
        final JsonFormat.TypeRegistry typeRegistry = Messages.forKnownTypes();

        final List<Descriptors.Descriptor> found = Lists.newLinkedList();
        for (TypeUrl typeUrl : KnownTypes.getTypeUrls()) {
            final Descriptors.Descriptor descriptor = typeRegistry.find(typeUrl.getTypeName());
            if (descriptor != null) {
                found.add(descriptor);
            }
        }

        assertFalse(found.isEmpty());
    }

    @Test
    public void return_descriptor_by_message_class() {
        assertEquals(StringValue.getDescriptor(), Messages.getClassDescriptor(StringValue.class));
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

    private static void assertReturnsFieldClass(Class<?> expectedClass, Descriptors.Descriptor msgDescriptor) {
        final FieldDescriptor field = msgDescriptor.getFields().get(0);

        assertEquals(expectedClass, Messages.getFieldClass(field));
    }
}
