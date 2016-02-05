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
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt64Value;
import org.junit.Test;

import static org.junit.Assert.*;

@SuppressWarnings("InstanceMethodNamingConvention")
public class TypeNameShould {

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_value() {
        //noinspection ConstantConditions
        TypeName.of((String)null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_emtpy_string() {
        TypeName.of("");
    }

    @Test
    public void return_type_url() {
        final TypeName test = TypeName.of(StringValue.newBuilder().setValue("return_type_url").build());

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
}
