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

package org.spine3.base.stringifiers;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import org.junit.Test;
import org.spine3.base.stringifiers.Identifier.Type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.Values.newIntValue;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.protobuf.Values.newUInt64Value;
import static org.spine3.protobuf.Values.newUIntValue;

public class IdentifierShould {

    @Test
    public void getDefaultValue_by_class_id() {
        assertEquals(0L, Identifier.getDefaultValue(Long.class).longValue());
        assertEquals(0, Identifier.getDefaultValue(Integer.class).intValue());
        assertEquals("", Identifier.getDefaultValue(String.class));
        assertEquals(Timestamp.getDefaultInstance(), Identifier.getDefaultValue(Timestamp.class));
    }

    @Test
    public void create_values_by_type() {
        assertTrue(Identifier.from("").isString());
        assertTrue(Identifier.from(0).isInteger());
        assertTrue(Identifier.from(0L).isLong());
        assertTrue(Identifier.from(newIntValue(300)).isMessage());
    }

    @Test
    public void recognize_type_by_supported_message_type() {
        assertTrue(Type.INTEGER.matchMessage(newUIntValue(10)));
        assertTrue(Type.LONG.matchMessage(newUInt64Value(1020L)));
        assertTrue(Type.STRING.matchMessage(newStringValue("")));
        assertTrue(Type.MESSAGE.matchMessage(Timestamp.getDefaultInstance()));

        assertFalse(Type.MESSAGE.matchMessage(StringValue.getDefaultInstance()));
        assertFalse(Type.MESSAGE.matchMessage(UInt32Value.getDefaultInstance()));
        assertFalse(Type.MESSAGE.matchMessage(UInt64Value.getDefaultInstance()));
    }

    @Test
    public void create_values_depending_from_message_type() {
        assertEquals(10, Type.INTEGER.fromMessage(newUIntValue(10)));
        assertEquals(1024L, Type.LONG.fromMessage(newUInt64Value(1024L)));
        assertEquals("", Type.STRING.fromMessage(newStringValue("")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_unpack_unsupported_types () {
        Identifier.Type.unpack(Any.getDefaultInstance());
    }
}
