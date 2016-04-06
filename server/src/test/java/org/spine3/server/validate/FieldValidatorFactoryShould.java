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

package org.spine3.server.validate;

import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.base.FieldPath;
import org.spine3.test.validation.msg.AnnotatedEnumFieldValue;
import org.spine3.test.validation.msg.RequiredByteStringFieldValue;
import org.spine3.test.validation.msg.RequiredMsgFieldValue;

import static com.google.protobuf.Descriptors.FieldDescriptor;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class FieldValidatorFactoryShould {

    private static final FieldPath FIELD_PATH = FieldPath.getDefaultInstance();

    private final FieldValidatorFactory factory = FieldValidatorFactory.newInstance();

    @Test
    public void create_message_field_validator() {
        final FieldDescriptor field = RequiredMsgFieldValue.getDescriptor().getFields().get(0);

        final FieldValidator validator = factory.create(field, StringValue.getDefaultInstance(), FIELD_PATH);

        assertTrue(validator instanceof MessageFieldValidator);
    }

    @Test
    public void create_integer_field_validator() {
        final FieldDescriptor field = Int32Value.getDescriptor().getFields().get(0);

        final FieldValidator validator = factory.create(field, 0, FIELD_PATH);

        assertTrue(validator instanceof IntegerFieldValidator);
    }

    @Test
    public void create_long_field_validator() {
        final FieldDescriptor field = Int64Value.getDescriptor().getFields().get(0);

        final FieldValidator validator = factory.create(field, 0, FIELD_PATH);

        assertTrue(validator instanceof LongFieldValidator);
    }

    @Test
    public void create_float_field_validator() {
        final FieldDescriptor field = FloatValue.getDescriptor().getFields().get(0);

        final FieldValidator validator = factory.create(field, 0, FIELD_PATH);

        assertTrue(validator instanceof FloatFieldValidator);
    }

    @Test
    public void create_double_field_validator() {
        final FieldDescriptor field = DoubleValue.getDescriptor().getFields().get(0);

        final FieldValidator validator = factory.create(field, 0, FIELD_PATH);

        assertTrue(validator instanceof DoubleFieldValidator);
    }

    @Test
    public void create_String_field_validator() {
        final FieldDescriptor field = StringValue.getDescriptor().getFields().get(0);

        final FieldValidator validator = factory.create(field, "", FIELD_PATH);

        assertTrue(validator instanceof StringFieldValidator);
    }

    @Test
    public void create_ByteString_field_validator() {
        final FieldDescriptor field = RequiredByteStringFieldValue.getDescriptor().getFields().get(0);

        final FieldValidator validator = factory.create(field, new Object(), FIELD_PATH);

        assertTrue(validator instanceof ByteStringFieldValidator);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_pass_enum_field() {
        final FieldDescriptor field = AnnotatedEnumFieldValue.getDescriptor().getFields().get(0);

        factory.create(field, new Object(), FIELD_PATH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_pass_boolean_field() {
        final FieldDescriptor field = BoolValue.getDescriptor().getFields().get(0);

        factory.create(field, new Object(), FIELD_PATH);
    }
}
