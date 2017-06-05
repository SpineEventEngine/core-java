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

package io.spine.protobuf;

import com.google.common.base.Charsets;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import io.spine.test.commands.TestCommand;
import org.junit.Test;

import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;

/**
 * @author Dmytro Dashenkov
 */
public class TypeConverterShould {

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(TypeConverter.class);
    }

    @Test
    public void pass_null_check() {
        new NullPointerTester()
                .setDefault(Any.class, Any.getDefaultInstance())
                .testStaticMethods(TypeConverter.class,
                                   NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void map_arbitrary_message_to_itself() {
        final Message message = TestCommand.newBuilder()
                                           .setValue("my-command-message")
                                           .build();
        checkMapping(message, message);
    }

    @Test
    public void map_Int32Value_to_int() {
        final int rowValue = 42;
        final Message value = Int32Value.newBuilder()
                                        .setValue(rowValue)
                                        .build();
        checkMapping(rowValue, value);
    }

    @Test
    public void map_Int64Value_to_long() {
        final long rowValue = 42;
        final Message value = Int64Value.newBuilder()
                                        .setValue(rowValue)
                                        .build();
        checkMapping(rowValue, value);
    }

    @Test
    public void map_FloatValue_to_float() {
        final float rowValue = 42.0f;
        final Message value = FloatValue.newBuilder()
                                        .setValue(rowValue)
                                        .build();
        checkMapping(rowValue, value);
    }

    @Test
    public void map_DoubleValue_to_double() {
        final double rowValue = 42.0;
        final Message value = DoubleValue.newBuilder()
                                         .setValue(rowValue)
                                         .build();
        checkMapping(rowValue, value);
    }

    @Test
    public void map_BoolValue_to_boolean() {
        final boolean rowValue = true;
        final Message value = BoolValue.newBuilder()
                                       .setValue(rowValue)
                                       .build();
        checkMapping(rowValue, value);
    }

    @Test
    public void map_StringValue_to_String() {
        final String rowValue = "Hello";
        final Message value = StringValue.newBuilder()
                                         .setValue(rowValue)
                                         .build();
        checkMapping(rowValue, value);
    }

    @Test
    public void map_BytesValue_to_ByteString() {
        final ByteString rowValue = ByteString.copyFrom("Hello!", Charsets.UTF_8);
        final Message value = BytesValue.newBuilder()
                                        .setValue(rowValue)
                                        .build();
        checkMapping(rowValue, value);
    }

    @Test
    public void map_uint32_to_int() {
        final int value = 42;
        final UInt32Value wrapped = UInt32Value.newBuilder()
                                              .setValue(value)
                                              .build();
        final Any packed = AnyPacker.pack(wrapped);
        final int mapped = TypeConverter.toObject(packed, Integer.class);
        assertEquals(value, mapped);
    }

    @Test
    public void map_uint64_to_long() {
        final long value = 42L;
        final UInt64Value wrapped = UInt64Value.newBuilder()
                                               .setValue(value)
                                               .build();
        final Any packed = AnyPacker.pack(wrapped);
        final long mapped = TypeConverter.toObject(packed, Long.class);
        assertEquals(value, mapped);
    }

    private static void checkMapping(Object javaObject,
                                     Message protoObject) {
        final Any wrapped = AnyPacker.pack(protoObject);
        final Object mappedJavaObject = TypeConverter.toObject(wrapped, javaObject.getClass());
        assertEquals(javaObject, mappedJavaObject);
        final Any restoredWrapped = TypeConverter.toAny(mappedJavaObject);
        final Message restored = AnyPacker.unpack(restoredWrapped);
        assertEquals(protoObject, restored);
    }
}
