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

package org.spine3.test;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkNotNull;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Illia Shepilov
 */
public class NullToleranceTestShould {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void return_false_when_check_class_with_method_without_check_not_null() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(FirstTestUtilityClass.class)
                                                                     .build();
        final boolean isValid = nullToleranceTest.check();
        assertFalse(isValid);
    }

    @Test
    public void return_true_when_check_class_when_method_without_check_is_ignored() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(FirstTestUtilityClass.class)
                                                                     .excludeMethod("methodWithoutCheck")
                                                                     .build();
        final boolean isValid = nullToleranceTest.check();
        assertTrue(isValid);
    }

    @Test
    public void return_true_when_check_class_with_methods_with_primitive_parameters() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(SecondTestUtilityClass.class)
                                                                     .build();
        final boolean isValid = nullToleranceTest.check();
        assertTrue(isValid);
    }

    @Test
    public void return_false_when_check_class_with_method_with_reference_and_primitive_types_without_check() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(ThirdTestUtilityClass.class)
                                                                     .build();
        final boolean isValid = nullToleranceTest.check();
        assertFalse(isValid);
    }

    @Test
    public void return_true_when_check_clkass_with_method_with_reference_and_primitive_types_with_check() {
        final NullToleranceTest nullToleranceTest =
                NullToleranceTest.newBuilder()
                                 .setClass(ThirdTestUtilityClass.class)
                                 .excludeMethod("methodWithMixedParameterTypesWithoutCheck")
                                 .build();
        final boolean isValid = nullToleranceTest.check();
        assertTrue(isValid);
    }

    /*
     * Test utility classes
     ******************************/

    private static class FirstTestUtilityClass {

        public static void methodWithoutCheck(Object obj) {
        }

        public static void methodWithCheck(Object first, Object second) {
            checkNotNull(first);
            checkNotNull(second);
        }
    }

    private static class SecondTestUtilityClass {

        public static void methodWithPrimitiveParams(int first, double second) {
        }
    }

    private static class ThirdTestUtilityClass {

        public static void methodWithMixedParameterTypesWithoutCheck(long first, Object second) {
        }

        public static void methodWithMixedParameterTypes_withCheck(float first, Object second) {
            checkNotNull(second);
        }
    }
}
