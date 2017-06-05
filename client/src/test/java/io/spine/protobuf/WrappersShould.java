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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import io.spine.test.Tests;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Yevsyukov
 */
public class WrappersShould {

    @Test
    public void have_utility_ctor() {
        Tests.assertHasPrivateParameterlessCtor(Wrappers.class);
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .testAllPublicStaticMethods(Wrappers.class);
    }

    /*
     * The tests below simply check for non-default Any instance produced to add to the coverage.
     * The real tests of packing/unpacking are available in WrapperShould.
     */

    private static void assertAny(Any any) {
        assertFalse(Any.getDefaultInstance().equals(any));
    }

    @Test
    public void pack_string() {
        assertAny(Wrappers.pack(getClass().getName()));
    }

    @Test
    public void pack_double() {
        assertAny(Wrappers.pack(1020.3040));
    }

    @Test
    public void pack_float() {
        assertAny(Wrappers.pack(10.20f));
    }

    @Test
    public void pack_int() {
        assertAny(Wrappers.pack(1024));
    }

    @Test
    public void pack_long() {
        assertAny(Wrappers.pack(2048L));
    }

    @Test
    public void pack_boolean() {
        assertAny(Wrappers.pack(true));
        assertAny(Wrappers.pack(false));
    }

    @Test
    public void format() {
        final StringValue str = Wrappers.format("%d %d %d", 1, 2, 3);
        assertEquals(str.getValue(), "1 2 3");
    }

}
