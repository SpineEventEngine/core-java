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

package org.spine3.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("InstanceMethodNamingConvention")
public class MathShould {

    @Test
    public void have_private_constructor() throws Exception {
        Tests.callPrivateUtilityConstructor(Math.class);
    }

    @Test(expected = ArithmeticException.class)
    public void throw_ArithmethicException_multiply_MIN_VALUE_by_minus_one() {
        Math.safeMultiply(Long.MIN_VALUE, -1);
    }

    @Test
    public void quickly_return_minus_a_on_multiply_by_minus_one() {
        assertEquals(-100, Math.safeMultiply(100, -1));
    }

    @Test
    public void quickly_return_zero_when_multiplying_by_zero() {
        assertEquals(0, Math.safeMultiply(100, 0));
    }

    @Test
    public void quickly_return_same_value_when_multiplying_by_one() {
        assertEquals(8, Math.safeMultiply(8, 1));
    }
}
