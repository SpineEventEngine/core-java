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

import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import io.spine.base.FieldPath;
import io.spine.test.validate.msg.MapRequiredMsgFieldValue;
import io.spine.test.validate.msg.RepeatedRequiredMsgFieldValue;
import io.spine.test.validate.msg.RequiredByteStringFieldValue;
import io.spine.test.validate.msg.RequiredEnumFieldValue;
import io.spine.test.validate.msg.RequiredMsgFieldValue;
import org.junit.Test;

import java.util.Collections;

import static com.google.protobuf.Descriptors.FieldDescriptor;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 */
public class FieldValidatorFactoryShould {

    private static final FieldPath FIELD_PATH = FieldPath.getDefaultInstance();

    @Test
    public void create_map_field_validator() {
        final FieldDescriptor field = MapRequiredMsgFieldValue.getDescriptor().getFields().get(0);

        final Object fieldValue = Collections.singletonMap(StringValue.getDefaultInstance(), StringValue.getDefaultInstance());

        final FieldValidator validator = FieldValidatorFactory.create(field, fieldValue, FIELD_PATH);

        assertTrue(validator instanceof MessageFieldValidator);
    }

    @Test
    public void create_repeated_field_validator() {
        final FieldDescriptor field = RepeatedRequiredMsgFieldValue.getDescriptor().getFields().get(0);

        final Object fieldValue = Collections.singletonList(StringValue.getDefaultInstance());

        final FieldValidator validator = FieldValidatorFactory.create(field, fieldValue, FIELD_PATH);

        assertTrue(validator instanceof MessageFieldValidator);
    }

    @Test
    public void create_message_field_validator() {
        final FieldDescriptor field = RequiredMsgFieldValue.getDescriptor().getFields().get(0);

        final FieldValidator validator = FieldValidatorFactory.create(field, StringValue.getDefaultInstance(), FIELD_PATH);

        assertTrue(validator instanceof MessageFieldValidator);
    }

    @Test
    public void create_integer_field_validator() {
        final FieldDescriptor field = Int32Value.getDescriptor().getFields().get(0);

        final FieldValidator validator = FieldValidatorFactory.create(field, 0, FIELD_PATH);

        assertTrue(validator instanceof IntegerFieldValidator);
    }

    @Test
    public void create_long_field_validator() {
        final FieldDescriptor field = Int64Value.getDescriptor().getFields().get(0);

        final FieldValidator validator = FieldValidatorFactory.create(field, 0, FIELD_PATH);

        assertTrue(validator instanceof LongFieldValidator);
    }

    @Test
    public void create_float_field_validator() {
        final FieldDescriptor field = FloatValue.getDescriptor().getFields().get(0);

        final FieldValidator validator = FieldValidatorFactory.create(field, 0, FIELD_PATH);

        assertTrue(validator instanceof FloatFieldValidator);
    }

    @Test
    public void create_double_field_validator() {
        final FieldDescriptor field = DoubleValue.getDescriptor().getFields().get(0);

        final FieldValidator validator = FieldValidatorFactory.create(field, 0, FIELD_PATH);

        assertTrue(validator instanceof DoubleFieldValidator);
    }

    @Test
    public void create_String_field_validator() {
        final FieldDescriptor field = StringValue.getDescriptor().getFields().get(0);

        final FieldValidator validator = FieldValidatorFactory.create(field, "", FIELD_PATH);

        assertTrue(validator instanceof StringFieldValidator);
    }

    @Test
    public void create_ByteString_field_validator() {
        final FieldDescriptor field = RequiredByteStringFieldValue.getDescriptor().getFields().get(0);

        final FieldValidator validator = FieldValidatorFactory.create(field, new Object(), FIELD_PATH);

        assertTrue(validator instanceof ByteStringFieldValidator);
    }

    @Test
    public void create_Enum_field_validator() {
        final FieldDescriptor field = RequiredEnumFieldValue.getDescriptor().getFields().get(0);

        final FieldValidator validator = FieldValidatorFactory.create(field, new Object(), FIELD_PATH);

        assertTrue(validator instanceof EnumFieldValidator);
    }

    @Test
    public void create_Boolean_field_validator() {
        final FieldDescriptor field = BoolValue.getDescriptor().getFields().get(0);

        final FieldValidator validator = FieldValidatorFactory.create(field, new Object(), FIELD_PATH);

        assertTrue(validator instanceof BooleanFieldValidator);
    }
}
