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

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.protobuf.error.UnknownTypeException;
import org.spine3.test.Tests;
import org.spine3.users.UserId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.client.UserUtil.newUserId;
import static org.spine3.protobuf.TypeUrl.*;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

public class AnyPackerShould {

    private final StringValue googleMsg = newStringValue(newUuid());
    private final UserId spineMsg = newUserId(newUuid());

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(AnyPacker.class));
    }

    @Test
    public void pack_spine_message_to_Any() {
        final Any actual = AnyPacker.pack(spineMsg);
        final String expectedUrl = composeTypeUrl(SPINE_TYPE_URL_PREFIX, UserId.getDescriptor().getFullName());

        assertEquals(Any.pack(spineMsg).getValue(), actual.getValue());
        assertEquals(expectedUrl, actual.getTypeUrl());
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

    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_pack_null() {
        AnyPacker.pack(Tests.<Message>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_unpack_null() {
        AnyPacker.unpack(Tests.<Any>nullRef());
    }
}
