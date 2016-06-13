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
import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class ValuesShould {

    public static final double DELTA = 0.01;

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Values.class));
    }

    @Test
    public void create_new_StringValue() {
        final String value = newUuid();
        final StringValue msg = Values.newStringValue(value);
        assertEquals(value, msg.getValue());
    }

    @Test
    public void create_new_Any_from_String() {
        final String value = newUuid();
        final Any msg = Values.pack(value);
        final StringValue unpackedMsg= fromAny(msg);
        assertEquals(value, unpackedMsg.getValue());
    }

    @Test
    public void create_new_DoubleValue() {
        final double value = 0.5;
        final DoubleValue msg = Values.newDoubleValue(value);
        assertEquals(value, msg.getValue(), 0);
    }

    @Test
    public void create_new_Any_from_double() {
        final double value = 0.5;
        final Any msg = Values.pack(value);
        final DoubleValue unpackedMsg= fromAny(msg);
        assertEquals(value, unpackedMsg.getValue(), DELTA);
    }

    @Test
    public void create_new_FloatValue() {
        final float value = 0.5F;
        final FloatValue msg = Values.newFloatValue(value);
        assertEquals(value, msg.getValue(), 0);
    }

    @Test
    public void create_new_Any_from_float() {
        final float value = 0.5F;
        final Any msg = Values.pack(value);
        final FloatValue unpackedMsg= fromAny(msg);
        assertEquals(value, unpackedMsg.getValue(), DELTA);
    }

    @Test
    public void create_new_Int32Value() {
        final int value = 2;
        final Int32Value msg = Values.newIntegerValue(value);
        assertEquals(value, msg.getValue());
    }

    @Test
    public void create_new_Any_from_int32() {
        final int value = 2;
        final Any msg = Values.pack(value);
        final Int32Value unpackedMsg= fromAny(msg);
        assertEquals(value, unpackedMsg.getValue());
    }

    @Test
    public void create_new_Int64Value() {
        final long value = 2L;
        final Int64Value msg = Values.newLongValue(value);
        assertEquals(value, msg.getValue());
    }

    @Test
    public void create_new_Any_from_int64() {
        final long value = 2L;
        final Any msg = Values.pack(value);
        final Int64Value unpackedMsg= fromAny(msg);
        assertEquals(value, unpackedMsg.getValue());
    }

    @Test
    public void create_new_BoolValue() {
        final boolean value = true;
        final BoolValue msg = Values.newBoolValue(value);
        assertEquals(value, msg.getValue());
    }

    @Test
    public void create_new_Any_from_boolean() {
        final boolean value = true;
        final Any msg = Values.pack(value);
        final BoolValue unpackedMsg= fromAny(msg);
        assertEquals(value, unpackedMsg.getValue());
    }
}
