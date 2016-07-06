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
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.test.Tests;
import org.spine3.type.TypeName;
import org.spine3.users.UserId;

import static org.junit.Assert.assertEquals;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.client.UserUtil.newUserId;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.protobuf.Values.newStringValue;

public class AnyPackerShould {
    private final UserId id = newUserId(newUuid());
    private final Any idAny = Any.pack(id);

    @Test
    public void convert_id_to_Any() {
        final Any test = AnyPacker.pack(id);
        assertEquals(idAny, test);
    }

    @Test
    public void convert_ByteString_to_Any() {
        final StringValue message = newStringValue(newUuid());
        final ByteString byteString = message.toByteString();

        assertEquals(Any.pack(message), AnyPacker.pack(TypeName.of(message), byteString));
    }

    @Test
    public void convert_from_Any_to_id() {
        final UserId test = unpack(idAny);
        assertEquals(id, test);
    }

    @Test
    public void convert_from_Any_to_protobuf_class() {
        final StringValue expected = newStringValue(newUuid());
        final Any packedValue = AnyPacker.pack(expected);
        final Message actual = unpack(packedValue);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_convert_null_id() {
        AnyPacker.pack(Tests.<Message>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_convert_from_null_Any() {
        unpack(Tests.<Any>nullRef());
    }


}
