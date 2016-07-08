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
import org.spine3.protobuf.TypeUrl;
import org.spine3.test.Tests;
import org.spine3.validate.internal.RequiredOption;

import static org.junit.Assert.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Values.newStringValue;

@SuppressWarnings("InstanceMethodNamingConvention")
public class TypeUrlShould {

    // TODO:2016-07-08:alexander.litus: add tests

    private static final String STRING_VALUE_TYPE_NAME = "google.protobuf.StringValue";

    private static final String STRING_VALUE_TYPE_URL =
            TypeUrl.GOOGLE_TYPE_URL_PREFIX + TypeUrl.SEPARATOR + STRING_VALUE_TYPE_NAME;

    @Test(expected = NullPointerException.class)
    public void not_accept_null_value() {
        TypeUrl.of(Tests.<String>nullRef());
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_empty_string() {
        TypeUrl.of("");
    }

    @Test
    public void create_from_message() {
        final TypeUrl typeUrl = TypeUrl.of(newStringValue(newUuid()));

        assertIsStringValueUrl(typeUrl);
    }

    @Test
    public void do_not_accept_Any_with_malformed_type_url() {
        final Any any = Any.newBuilder().setTypeUrl("do_not_accept_Any_with_malformed_type_url").build();
        try {
            TypeUrl.ofEnclosed(any);
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof InvalidProtocolBufferException);
        }
    }

    @Test
    public void return_simple_type_name() {
        assertEquals("UInt64Value", TypeUrl.of(UInt64Value.getDefaultInstance()).getSimpleName());
    }

    @Test
    public void return_simple_name_if_no_package() {
        // A type without Protobuf package:
        final String name = RequiredOption.class.getSimpleName();
        final TypeUrl typeUrl = TypeUrl.of(name);

        final String actual = typeUrl.getSimpleName();

        assertEquals(name, actual);
    }

    @Test
    public void create_by_descriptor() {
        final Descriptors.Descriptor descriptor = StringValue.getDefaultInstance().getDescriptorForType();

        final TypeUrl typeUrl = TypeUrl.of(descriptor);
        assertIsStringValueUrl(typeUrl);
    }

    @Test
    public void obtain_type_of_command() {
        final CommandFactory factory = TestCommandFactory.newInstance(TypeUrlShould.class);
        final StringValue message = newStringValue(newUuid());
        final Command command = factory.create(message);

        final TypeUrl typeUrl = TypeUrl.ofCommand(command);
        assertEquals(TypeUrl.of(message), typeUrl);
    }

    @Test
    public void create_instance_by_class() {
        final TypeUrl typeUrl = TypeUrl.of(StringValue.class);

        assertIsStringValueUrl(typeUrl);
    }

    private static void assertIsStringValueUrl(TypeUrl typeUrl) {
        assertEquals(STRING_VALUE_TYPE_URL, typeUrl.value());
        assertEquals(TypeUrl.GOOGLE_TYPE_URL_PREFIX, typeUrl.getPrefix());
        assertEquals(STRING_VALUE_TYPE_NAME, typeUrl.getTypeName());
        assertEquals(StringValue.class.getSimpleName(), typeUrl.getSimpleName());
    }
}
