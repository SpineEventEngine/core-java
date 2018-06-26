/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.reflect;

import com.google.common.base.Optional;
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
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.FieldFilter;
import io.spine.core.Command;
import io.spine.protobuf.Messages;
import io.spine.test.reflect.MessageWithStringValue;
import io.spine.test.reflect.TestEnum;
import io.spine.test.reflect.TestEnumValue;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Alexander Yevsyukov
 */
class FieldTest {

    private static void assertReturnsFieldClass(Class<?> expectedClass, Descriptor msgDescriptor) {
        FieldDescriptor field = msgDescriptor.getFields()
                                             .get(0);

        assertEquals(expectedClass, Field.getFieldClass(field));
    }

    @Test
    @DisplayName("pass null tolerance check")
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(FieldFilter.class, FieldFilter.getDefaultInstance())
                .testAllPublicStaticMethods(Field.class);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // OK as the field is present in this type.
    @Test
    @DisplayName("return name")
    void returnName() {
        String fieldName = "seconds";
        assertEquals(fieldName, Field.newField(Timestamp.class, fieldName)
                                     .get()
                                     .getName());
    }

    @Test
    @DisplayName("return absent for missing field")
    void returnAbsentForMissingField() {
        assertFalse(Field.newField(Timestamp.class, "min")
                         .isPresent());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // OK since we know the field is present.
    @Test
    @DisplayName("return absent for default Any field")
    void returnAbsentForDefaultAnyField() {
        Field messageField = Field.newField(Command.class, "message")
                                  .get();
        Optional<Message> value = messageField.getValue(Command.getDefaultInstance());
        assertFalse(value.isPresent());
    }

    @Test
    @DisplayName("reject field filter without a field name")
    void rejectFieldFilterWithoutAFieldName() {
        assertThrows(IllegalArgumentException.class,
                     () -> Field.forFilter(Timestamp.class, FieldFilter.getDefaultInstance()));
    }

    @Test
    @DisplayName("return int msg field class by descriptor")
    void returnIntMsgFieldClassByDescriptor() {
        assertReturnsFieldClass(Integer.class, Int32Value.getDescriptor());
    }

    @Test
    @DisplayName("return long msg field class by descriptor")
    void returnLongMsgFieldClassByDescriptor() {
        assertReturnsFieldClass(Long.class, Int64Value.getDescriptor());
    }

    @Test
    @DisplayName("return float msg field class by descriptor")
    void returnFloatMsgFieldClassByDescriptor() {
        assertReturnsFieldClass(Float.class, FloatValue.getDescriptor());
    }

    @Test
    @DisplayName("return double msg field class by descriptor")
    void returnDoubleMsgFieldClassByDescriptor() {
        assertReturnsFieldClass(Double.class, DoubleValue.getDescriptor());
    }

    @Test
    @DisplayName("return boolean msg field class by descriptor")
    void returnBooleanMsgFieldClassByDescriptor() {
        assertReturnsFieldClass(Boolean.class, BoolValue.getDescriptor());
    }

    @Test
    @DisplayName("return string msg field class by descriptor")
    void returnStringMsgFieldClassByDescriptor() {
        assertReturnsFieldClass(String.class, StringValue.getDescriptor());
    }

    @Test
    @DisplayName("return byte string msg field class by descriptor")
    void returnByteStringMsgFieldClassByDescriptor() {
        assertReturnsFieldClass(ByteString.class, BytesValue.getDescriptor());
    }

    @Test
    @DisplayName("return enum msg field class by descriptor")
    void returnEnumMsgFieldClassByDescriptor() {
        assertReturnsFieldClass(TestEnum.class, TestEnumValue.getDescriptor());
    }

    @Test
    @DisplayName("return msg field class by descriptor")
    void returnMsgFieldClassByDescriptor() {
        assertReturnsFieldClass(StringValue.class, MessageWithStringValue.getDescriptor());
    }

    @Test
    @DisplayName("pass the null tolerance check")
    void passTheNullToleranceCheck() {
        FieldDescriptor defaultFieldDescriptor = StringValue.getDefaultInstance()
                                                            .getDescriptorForType()
                                                            .getFields()
                                                            .get(0);
        new NullPointerTester()
                .setDefault(TypeUrl.class, TypeUrl.of(StringValue.class))
                .setDefault(FieldDescriptor.class, defaultFieldDescriptor)
                .testAllPublicStaticMethods(Messages.class);
    }
}
