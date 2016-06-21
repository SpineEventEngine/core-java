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

package org.spine3.type;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt64Value;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.client.CommandFactory;
import org.spine3.client.test.TestCommandFactory;
import org.spine3.test.RunTest;
import org.spine3.test.Tests;

import static org.junit.Assert.*;
import static org.spine3.protobuf.Values.newStringValue;

@SuppressWarnings("InstanceMethodNamingConvention")
public class TypeNameShould {

    private static final String STRING_VALUE_TYPE_NAME = "google.protobuf.StringValue";

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_value() {
        TypeName.of(Tests.<String>nullRef());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_empty_string() {
        TypeName.of("");
    }

    @Test
    public void return_type_url() {
        final TypeName test = TypeName.of(newStringValue("return_type_url"));

        assertFalse(test.toTypeUrl().isEmpty());
    }

    @Test
    public void do_not_accept_Any_with_malformed_type_url() {
        final Any any = Any.newBuilder().setTypeUrl("do_not_accept_Any_with_malformed_type_url").build();
        try {
            TypeName.ofEnclosed(any);
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof InvalidProtocolBufferException);
        }
    }

    @Test
    public void return_name_only() {
        assertEquals("UInt64Value", TypeName.of(UInt64Value.getDefaultInstance()).nameOnly());
    }

    @Test
    public void return_name_if_no_package() {
        final String name = TypeNameShould.class.getSimpleName();
        assertEquals(name, TypeName.of(name).nameOnly());
    }

    @Test
    public void create_by_descriptor() {
        final Descriptors.Descriptor descriptor = StringValue.getDefaultInstance().getDescriptorForType();

        final TypeName typeName = TypeName.of(descriptor);
        assertEquals(STRING_VALUE_TYPE_NAME, typeName.value());
    }

    @Test
    public void obtain_type_of_command() {
        final CommandFactory factory = TestCommandFactory.newInstance(TypeNameShould.class);
        final RunTest message = RunTest.newBuilder()
                                       .setMethodName("obtain_type_of_command")
                                       .build();
        final Command command = factory.create(message);

        final TypeName typeName = TypeName.ofCommand(command);
        assertEquals(TypeName.of(message), typeName);
    }

    @Test
    public void create_instance_by_class() {
        final TypeName typeName = TypeName.of(StringValue.class);

        assertEquals(STRING_VALUE_TYPE_NAME, typeName.value());
    }
}
