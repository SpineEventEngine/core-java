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

package io.spine.protobuf;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.junit.Test;

import static io.spine.protobuf.Wrapper.forBoolean;
import static io.spine.protobuf.Wrapper.forBytes;
import static io.spine.protobuf.Wrapper.forDouble;
import static io.spine.protobuf.Wrapper.forFloat;
import static io.spine.protobuf.Wrapper.forInteger;
import static io.spine.protobuf.Wrapper.forLong;
import static io.spine.protobuf.Wrapper.forString;
import static io.spine.protobuf.Wrapper.forUnsignedInteger;
import static io.spine.protobuf.Wrapper.forUnsignedLong;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public class WrapperShould {

    private static <T, W extends Message> void assertUnwrap(Wrapper<T, W> wrapper, W value) {
        final T unwrapped = wrapper.reverse()
                                   .convert(value);
        assertEquals(value, wrapper.convert(unwrapped));
    }

    private static <T, W extends Message> void assertPackUnpack(Wrapper<T, W> wrapper, T value) {
        assertEquals(value, wrapper.unpack(wrapper.pack(value)));
    }

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Wrappers.class);
    }

    @Test
    public void wrap_string() {
        assertUnwrap(forString(), forString(getClass().getName()));        
    }

    @Test
    public void pack_string() {
        assertPackUnpack(forString(), getClass().getName());
    }

    @Test
    public void wrap_double() {
        assertUnwrap(forDouble(), forDouble(1020.3040));
    }

    @Test
    public void pack_double() {
        assertPackUnpack(forDouble(), 98765.4321);
    }

    @Test
    public void wrap_float() {
        assertUnwrap(forFloat(), forFloat(45.67f));
    }

    @Test
    public void pack_float() {
        assertPackUnpack(forFloat(), 13.30f);
    }

    @Test
    public void wrap_integer() {
        assertUnwrap(forInteger(), forInteger(6057));
    }

    @Test
    public void pack_integer() {
        assertPackUnpack(forInteger(), 654321);
    }

    @Test
    public void wrap_long() {
        assertUnwrap(forLong(), forLong(12345L));
    }

    @Test
    public void pack_long() {
        assertPackUnpack(forLong(), 100500L);
    }

    @Test
    public void wrap_boolean() {
        assertUnwrap(forBoolean(), forBoolean(Boolean.TRUE));
        assertUnwrap(forBoolean(), forBoolean(Boolean.FALSE));
    }

    @Test
    public void pack_boolean() {
        assertPackUnpack(forBoolean(), Boolean.TRUE);
        assertPackUnpack(forBoolean(), Boolean.FALSE);
    }

    @SuppressWarnings("ImplicitNumericConversion")
    @Test
    public void wrap_bytes() {
        assertUnwrap(forBytes(), forBytes(ByteString.copyFrom(new byte[] { 10, 9, 8, 7, 6 })));
    }

    @SuppressWarnings("ImplicitNumericConversion")
    @Test
    public void pack_bytes() {
        assertPackUnpack(forBytes(), ByteString.copyFrom(new byte[] { 1, 2, 3, 4, 5 }));
    }

    @Test
    public void wrap_unsigned_integer() {
        assertUnwrap(forUnsignedInteger(), forUnsignedInteger(7896));
    }

    @Test
    public void unpack_unsigned_integer() {
        assertPackUnpack(forUnsignedInteger(), 1020);
    }

    @Test
    public void wrap_unsigned_long() {
        assertUnwrap(forUnsignedLong(), forUnsignedLong(9669L));
    }

    @Test
    public void unpack_unsigned_long() {
        assertPackUnpack(forUnsignedLong(), 1020L);
    }

    @Test
    public void have_custom_toString() {
        assertCustomToString(Wrapper.forBoolean());
        assertCustomToString(Wrapper.forBytes());
        assertCustomToString(Wrapper.forDouble());
        assertCustomToString(Wrapper.forFloat());
        assertCustomToString(Wrapper.forInteger());
        assertCustomToString(Wrapper.forLong());
        assertCustomToString(Wrapper.forString());
        assertCustomToString(Wrapper.forUnsignedInteger());
        assertCustomToString(Wrapper.forUnsignedLong());
    }

    private static void assertCustomToString(Wrapper wrapper) {
        assertTrue(wrapper.toString().startsWith("Wrapper.for"));
    }
}
