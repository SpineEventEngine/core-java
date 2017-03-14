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

package org.spine3.type;

import com.google.common.testing.EqualsTester;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Field;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandValidationError;
import org.spine3.client.CommandFactory;
import org.spine3.test.TestCommandFactory;
import org.spine3.test.Tests;
import org.spine3.type.error.UnknownTypeException;
import org.spine3.users.UserId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.type.TypeUrl.composeTypeUrl;

public class TypeUrlShould {

    private static final String STRING_VALUE_TYPE_NAME = StringValue.getDescriptor()
                                                                    .getFullName();

    private static final String STRING_VALUE_TYPE_URL_STR = composeTypeUrl(
            TypeUrl.Prefix.GOOGLE_APIS.value(),
            STRING_VALUE_TYPE_NAME);

    private final TypeUrl stringValueTypeUrl = TypeUrl.from(StringValue.getDescriptor());

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
    public void create_from_type_name() {
        final TypeUrl typeUrl = TypeName.of(STRING_VALUE_TYPE_NAME)
                                        .toUrl();

        assertIsStringValueUrl(typeUrl);
    }

    @Test
    public void create_from_type_url_string() {
        final TypeUrl typeUrl = TypeUrl.of(STRING_VALUE_TYPE_URL_STR);

        assertIsStringValueUrl(typeUrl);
    }

    @Test
    public void do_not_accept_Any_with_malformed_type_url() {
        final Any any = Any.newBuilder().setTypeUrl("invalid_type_url").build();
        try {
            TypeUrl.ofEnclosed(any);
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof InvalidProtocolBufferException);
        }
    }

    @Test
    public void return_type_url_prefix() {
        assertEquals(TypeUrl.Prefix.GOOGLE_APIS.value(), stringValueTypeUrl.getPrefix());
    }

    @Test
    public void return_type_name() {
        assertEquals(STRING_VALUE_TYPE_NAME, stringValueTypeUrl.getTypeName());
    }

    @Test
    public void create_by_descriptor_of_google_msg() {
        final TypeUrl typeUrl = TypeUrl.from(StringValue.getDescriptor());

        assertIsStringValueUrl(typeUrl);
    }

    @Test
    public void create_by_descriptor_of_spine_msg() {
        final Descriptors.Descriptor descriptor = UserId.getDescriptor();
        final String expectedUrl = composeTypeUrl(TypeUrl.Prefix.SPINE.value(),
                                                  descriptor.getFullName());

        final TypeUrl typeUrl = TypeUrl.from(descriptor);

        assertEquals(expectedUrl, typeUrl.value());
    }

    @Test
    public void create_by_enum_descriptor_of_google_msg() {
        assertCreatesTypeUrlFromEnum(TypeUrl.Prefix.GOOGLE_APIS.value(),
                                     Field.Kind.getDescriptor());
    }

    @Test
    public void create_by_enum_descriptor_of_spine_msg() {
        assertCreatesTypeUrlFromEnum(TypeUrl.Prefix.SPINE.value(),
                                     CommandValidationError.getDescriptor());
    }

    private static void assertCreatesTypeUrlFromEnum(String typeUrlPrefix,
                                                     EnumDescriptor enumDescriptor) {
        final String expected = composeTypeUrl(typeUrlPrefix, enumDescriptor.getFullName());

        final TypeUrl typeUrl = TypeUrl.from(enumDescriptor);

        assertEquals(expected, typeUrl.value());
    }

    @Test
    public void obtain_type_of_command() {
        final CommandFactory factory = TestCommandFactory.newInstance(TypeUrlShould.class);
        final StringValue message = newStringValue(newUuid());
        final Command command = factory.createCommand(message);

        final TypeUrl typeUrl = TypeUrl.ofCommand(command);

        assertIsStringValueUrl(typeUrl);
    }

    @Test
    public void create_instance_by_class() {
        final TypeUrl typeUrl = TypeUrl.of(StringValue.class);

        assertIsStringValueUrl(typeUrl);
    }

    private static void assertIsStringValueUrl(TypeUrl typeUrl) {
        assertEquals(STRING_VALUE_TYPE_URL_STR, typeUrl.value());
        assertEquals(TypeUrl.Prefix.GOOGLE_APIS.value(), typeUrl.getPrefix());
        assertEquals(STRING_VALUE_TYPE_NAME, typeUrl.getTypeName());
        assertEquals(StringValue.class.getSimpleName(), TypeName.from(typeUrl)
                                                                .getSimpleName());
    }

    @Test
    public void use_fields_in_equality() {
        new EqualsTester()
                .addEqualityGroup(TypeUrl.of(StringValue.class), TypeUrl.of(StringValue.class))
                .addEqualityGroup(TypeUrl.of(Timestamp.class))
                .addEqualityGroup(TypeUrl.of(UInt32Value.class))
                .testEquals();
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_empty_prefix() {
        TypeUrl.of(" /package.Type");
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_empty_name() {
        TypeUrl.of("type.prefix/ ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_malformed_type_URL() {
        TypeUrl.of("prefix/prefix/type.Name");
    }

    @Test(expected = UnknownTypeException.class)
    public void throw_exception_for_unknown_Java_class() {
        final TypeUrl url = TypeUrl.of("unknown/JavaClass");
        url.toMessageClass();
    }

    @Test
    public void return_value() {
        assertTrue(TypeUrl.of(BoolValue.class)
                          .toString()
                          .contains(BoolValue.class.getSimpleName()));
    }

    @Test
    public void have_prefix_enumeration() {
        assertTrue(TypeUrl.Prefix.SPINE.toString()
                                       .contains("spine"));
    }
}
