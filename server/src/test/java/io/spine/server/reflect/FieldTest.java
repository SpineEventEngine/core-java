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
import io.spine.test.reflect.MessageWithStringValue;
import io.spine.test.reflect.TestEnum;
import io.spine.test.reflect.TestEnumValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names */})
@DisplayName("Field should")
class FieldTest {

    private static void assertReturnsFieldClass(Class<?> expectedClass, Descriptor msgDescriptor) {
        FieldDescriptor field = msgDescriptor.getFields()
                                             .get(0);

        assertEquals(expectedClass, Field.getFieldClass(field));
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
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
    @DisplayName("reject field filter without field name")
    void rejectFilterWithoutName() {
        assertThrows(IllegalArgumentException.class,
                     () -> Field.forFilter(Timestamp.class, FieldFilter.getDefaultInstance()));
    }

    @Nested
    @DisplayName("return absent")
    class ReturnAbsent {

        @Test
        @DisplayName("for missing field")
        void forMissingField() {
            assertFalse(Field.newField(Timestamp.class, "min")
                             .isPresent());
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent") // OK since we know the field is present.
        @Test
        @DisplayName("for default Any field")
        void forDefaultAnyField() {
            Field messageField = Field.newField(Command.class, "message")
                                      .get();
            Optional<Message> value = messageField.getValue(Command.getDefaultInstance());
            assertFalse(value.isPresent());
        }
    }

    @Nested
    @DisplayName("retrieve field Java type via descriptor for")
    class ReturnFieldClass {

        @Test
        @DisplayName("Integer")
        void ofInteger() {
            assertReturnsFieldClass(Integer.class, Int32Value.getDescriptor());
        }

        @Test
        @DisplayName("Long")
        void ofLong() {
            assertReturnsFieldClass(Long.class, Int64Value.getDescriptor());
        }

        @Test
        @DisplayName("Float")
        void ofFloat() {
            assertReturnsFieldClass(Float.class, FloatValue.getDescriptor());
        }

        @Test
        @DisplayName("Double")
        void ofDouble() {
            assertReturnsFieldClass(Double.class, DoubleValue.getDescriptor());
        }

        @Test
        @DisplayName("Boolean")
        void ofBoolean() {
            assertReturnsFieldClass(Boolean.class, BoolValue.getDescriptor());
        }

        @Test
        @DisplayName("String")
        void ofString() {
            assertReturnsFieldClass(String.class, StringValue.getDescriptor());
        }

        @Test
        @DisplayName("ByteString")
        void ofByteString() {
            assertReturnsFieldClass(ByteString.class, BytesValue.getDescriptor());
        }

        @Test
        @DisplayName("Enum")
        void ofEnum() {
            assertReturnsFieldClass(TestEnum.class, TestEnumValue.getDescriptor());
        }

        @Test
        @DisplayName("Message")
        void ofMessage() {
            assertReturnsFieldClass(StringValue.class, MessageWithStringValue.getDescriptor());
        }
    }
}
