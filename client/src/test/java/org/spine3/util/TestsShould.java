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

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertNotEquals;

@SuppressWarnings({"InstanceMethodNamingConvention", "MethodWithTooExceptionsDeclared"})
public class TestsShould {

    @SuppressWarnings("RedundantNoArgConstructor")
    private static class TestsTest {
        private TestsTest() {}
    }

    @Test
    public void call_private_ctor_for_coverage() throws
            InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Tests.callPrivateUtilityConstructor(TestsTest.class);
    }

    private static class ClassWithPublicCtor {
        public ClassWithPublicCtor() {}
    }

    @Test(expected = IllegalStateException.class)
    public void throw_IllegalStateException_if_utlity_ctor_is_not_private()
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Tests.callPrivateUtilityConstructor(ClassWithPublicCtor.class);
    }

    @Test
    public void return_current_time_in_seconds() {
        assertNotEquals(0, Tests.currentTimeSeconds());
    }
}
