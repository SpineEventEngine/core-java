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
package org.spine3.protobuf;

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import org.junit.Ignore;
import org.junit.Test;
import org.spine3.test.Tests;
import org.spine3.type.TypeName;
import org.spine3.users.UserId;

import java.util.List;

import static org.junit.Assert.*;
import static org.spine3.client.UserUtil.newUserId;
import static org.spine3.protobuf.Values.newStringValue;

/**
 * @author Mikhail Melnik
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class MessagesShould {

    private final UserId id = newUserId("messages_test");
    private final Any any = Any.pack(id);

    @Test
    public void have_private_utility_ctor() {
        assertTrue(Tests.hasPrivateUtilityConstructor(Messages.class));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void toText_fail_on_null() {
        Messages.toText(null);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void toJson_fail_on_null() {
        Messages.toJson(null);
    }

    @Test
    public void convert_id_to_Any() {
        final Any test = Messages.toAny(id);
        assertEquals(any, test);
    }


    @Test
    public void convert_ByteString_to_Any() {
        final StringValue message = newStringValue("convert_ByteString_to_Any");
        final ByteString byteString = message.toByteString();

        assertEquals(Any.pack(message), Messages.toAny(TypeName.of(message), byteString));
    }

    @Test
    public void convert_from_Any_to_id() {
        final UserId test = Messages.fromAny(any);
        assertEquals(id, test);
    }

    @Test
    public void convert_from_Any_to_protobuf_class() {

        final StringValue expected = newStringValue("test_value");
        final Any expectedAny = Any.pack(expected);
        final Message actual = Messages.fromAny(expectedAny);
        assertEquals(expected, actual);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_convert_null_id() {
        Messages.toAny(null);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_convert_from_null_Any() {
        Messages.fromAny(null);
    }

    //TODO:2016-02-06:alexander.yevsyukov: Enable when storing nested types to .properties is fixed.
    @Ignore
    @Test
    public void print_to_json() {
        final StringValue value = newStringValue("print_to_json");
        assertFalse(Messages.toJson(value).isEmpty());
    }

    //TODO:2016-02-06:alexander.yevsyukov: Enable when storing nested types to .properties is fixed.
    @Ignore
    @Test
    public void build_JsonFormat_registry_for_known_types() {
        final JsonFormat.TypeRegistry typeRegistry = Messages.forKnownTypes();

        final List<Descriptors.Descriptor> found = Lists.newLinkedList();
        for (TypeName typeName : TypeToClassMap.knownTypes()) {
            final Descriptors.Descriptor descriptor = typeRegistry.find(typeName.value());
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
}
