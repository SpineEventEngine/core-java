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

package org.spine3.server.entity.storage;

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
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.test.entity.Project;
import org.spine3.testdata.Sample;

import static org.junit.Assert.assertEquals;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Dmytro Dashenkov
 */
public class ProtoToJavaMapperShould {

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(ProtoToJavaMapper.class);
    }

    @Test
    public void pass_null_check() {
        new NullPointerTester()
                .setDefault(Any.class, Any.getDefaultInstance())
                .testStaticMethods(ProtoToJavaMapper.class,
                                   NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void map_arbitrary_message_to_itself() {
        final Project message = Sample.messageOfType(Project.class);
        final Any wrapped = AnyPacker.pack(message);
        final Message result = ProtoToJavaMapper.map(wrapped, Message.class);
        assertEquals(message, result);
    }

    @Test
    public void map_Int32Value_to_int() {
        final int rowValue = 42;
        final Message value = Int32Value.newBuilder()
                                        .setValue(rowValue)
                                        .build();
        final Any wrapped = AnyPacker.pack(value);
        final Object result = ProtoToJavaMapper.map(wrapped, int.class);
        assertEquals(rowValue, result);
    }

    @Test
    public void map_Int64Value_to_long() {
        final long rowValue = 42;
        final Message value = Int64Value.newBuilder()
                                        .setValue(rowValue)
                                        .build();
        final Any wrapped = AnyPacker.pack(value);
        final Object result = ProtoToJavaMapper.map(wrapped, long.class);
        assertEquals(rowValue, result);
    }

    @Test
    public void map_UInt32Value_to_int() {
        final int rowValue = 42;
        final Message value = UInt32Value.newBuilder()
                                         .setValue(rowValue)
                                         .build();
        final Any wrapped = AnyPacker.pack(value);
        final Object result = ProtoToJavaMapper.map(wrapped, int.class);
        assertEquals(rowValue, result);
    }

    @Test
    public void map_UInt64Value_to_long() {
        final long rowValue = 42L;
        final Message value = UInt64Value.newBuilder()
                                         .setValue(rowValue)
                                         .build();
        final Any wrapped = AnyPacker.pack(value);
        final Object result = ProtoToJavaMapper.map(wrapped, long.class);
        assertEquals(rowValue, result);
    }

    @Test
    public void map_FloatValue_to_float() {
        final float rowValue = 42.0f;
        final Message value = FloatValue.newBuilder()
                                        .setValue(rowValue)
                                        .build();
        final Any wrapped = AnyPacker.pack(value);
        final Object result = ProtoToJavaMapper.map(wrapped, float.class);
        assertEquals(rowValue, result);
    }

    @Test
    public void map_DoubleValue_to_double() {
        final double rowValue = 42.0;
        final Message value = DoubleValue.newBuilder()
                                         .setValue(rowValue)
                                         .build();
        final Any wrapped = AnyPacker.pack(value);
        final Object result = ProtoToJavaMapper.map(wrapped, double.class);
        assertEquals(rowValue, result);
    }

    @Test
    public void map_BoolValue_to_boolean() {
        final boolean rowValue = true;
        final Message value = BoolValue.newBuilder()
                                       .setValue(rowValue)
                                       .build();
        final Any wrapped = AnyPacker.pack(value);
        final Object result = ProtoToJavaMapper.map(wrapped, boolean.class);
        assertEquals(rowValue, result);
    }

    @Test
    public void map_StringValue_to_String() {
        final String rowValue = "Hello";
        final Message value = StringValue.newBuilder()
                                         .setValue(rowValue)
                                         .build();
        final Any wrapped = AnyPacker.pack(value);
        final Object result = ProtoToJavaMapper.map(wrapped, String.class);
        assertEquals(rowValue, result);
    }

    @Test
    public void map_BytesValue_to_ByteString() {
        final ByteString rowValue = ByteString.copyFrom("Hello!", Charsets.UTF_8);
        final Message value = BytesValue.newBuilder()
                                        .setValue(rowValue)
                                        .build();
        final Any wrapped = AnyPacker.pack(value);
        final Object result = ProtoToJavaMapper.map(wrapped, ByteString.class);
        assertEquals(rowValue, result);
    }
}
