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

package org.spine3.protobuf;

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.test.Tests;
import org.spine3.type.TypeUrl;
import org.spine3.users.UserId;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.test.Tests.newUserId;
import static org.spine3.test.Tests.newUuidValue;

public class AnyPackerShould {

    /** A message with type URL standard to Google Protobuf. */
    private final StringValue googleMsg = newStringValue(newUuid());

    /** A message with different type URL. */
    private final UserId spineMsg = newUserId(newUuid());

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(AnyPacker.class);
    }

    @Test
    public void pack_spine_message_to_Any() {
        final Any actual = AnyPacker.pack(spineMsg);
        final TypeUrl typeUrl = TypeUrl.of(spineMsg);


        assertEquals(Any.pack(spineMsg).getValue(), actual.getValue());
        assertEquals(typeUrl.value(), actual.getTypeUrl());
    }

    @Test
    public void unpack_spine_message_from_Any() {
        final Any any = AnyPacker.pack(spineMsg);

        final UserId actual = AnyPacker.unpack(any);

        assertEquals(spineMsg, actual);
    }

    @Test
    public void pack_google_message_to_Any() {
        final Any expected = Any.pack(googleMsg);

        final Any actual = AnyPacker.pack(googleMsg);

        assertEquals(expected, actual);
    }

    @Test
    public void unpack_google_message_from_Any() {
        final Any any = Any.pack(googleMsg);

        final StringValue actual = AnyPacker.unpack(any);

        assertEquals(googleMsg, actual);
    }

    @Test
    public void return_Any_if_it_is_passed_to_pack() {
        final Any any = Any.pack(googleMsg);

        assertSame(any, AnyPacker.pack(any));
    }

    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_pack_null() {
        AnyPacker.pack(Tests.<Message>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_unpack_null() {
        AnyPacker.unpack(Tests.<Any>nullRef());
    }

    @Test
    public void create_packing_iterator() {
        final Iterator<Message> iterator = Lists.<Message>newArrayList(newUuidValue()).iterator();
        assertNotNull(AnyPacker.pack(iterator));
    }

    @Test
    public void have_null_accepting_func() {
        assertNull(AnyPacker.unpackFunc()
                            .apply(null));
    }

    @Test
    public void have_unpacking_func() {
        final StringValue value = newUuidValue();

        assertEquals(value, AnyPacker.unpackFunc()
                                     .apply(Any.pack(value)));
    }
}
