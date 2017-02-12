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

import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.server.event.EventFilter;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Illia Shepilov
 */
public class NullToleranceTestShould {

    private static final Object DEFAULT_VALUE = new Object();
    private static final String METHOD_NAME_WITHOUT_CHECK = "methodWithoutCheck";

    @Test
    public void return_false_for_class_with_methods_accepting_non_declared_null_parameters() {
        final NullToleranceTest nullToleranceTest =
                NullToleranceTest.newBuilder()
                                 .setClass(UtilityClassWithReferenceParameters.class)
                                 .addDefaultValue(new Object())
                                 .build();
        final boolean passed = nullToleranceTest.check();
        assertFalse(passed);
    }

    @Test
    public void return_true_for_class_with_ignored_method_without_check() {
        final NullToleranceTest nullToleranceTest =
                NullToleranceTest.newBuilder()
                                 .setClass(UtilityClassWithReferenceParameters.class)
                                 .excludeMethod(METHOD_NAME_WITHOUT_CHECK)
                                 .excludeMethod("methodWithParameterTypeWhichDoesNotCheckInside")
                                 .addDefaultValue(DEFAULT_VALUE)
                                 .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }

    @Test
    public void return_true_for_class_with_methods_with_primitive_parameters() {
        final NullToleranceTest nullToleranceTest =
                NullToleranceTest.newBuilder()
                                 .setClass(UtilityClassWithPrimitiveParameters.class)
                                 .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }

    @Test
    public void return_false_for_class_with_method_with_primitive_and_reference_parameters_without_check() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(UtilityClassWithMixedParameters.class)
                                                                     .addDefaultValue(DEFAULT_VALUE)
                                                                     .build();
        final boolean passed = nullToleranceTest.check();
        assertFalse(passed);
    }

    @Test
    public void return_true_for_class_with_method_with_primitive_and_reference_types_with_check() {
        final NullToleranceTest nullToleranceTest =
                NullToleranceTest.newBuilder()
                                 .setClass(UtilityClassWithMixedParameters.class)
                                 .excludeMethod("methodWithMixedParameterTypesWithoutCheck")
                                 .addDefaultValue(new Object())
                                 .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }

    @Test(expected = RuntimeException.class)
    public void throw_exception_when_invoke_method_which_throws_exception() {
        final NullToleranceTest nullToleranceTest =
                NullToleranceTest.newBuilder()
                                 .setClass(UtilityClassWithThrownExceptionInTheMethod.class)
                                 .excludeMethod("methodWhichThrowsNPE")
                                 .build();
        nullToleranceTest.check();
    }

    @Test
    public void return_true_when_check_class_with_util_and_non_util_method() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(ClassWithNonUtilMethod.class)
                                                                     .addDefaultValue(DEFAULT_VALUE)
                                                                     .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }

    @Test
    public void pass_the_check_when_invoke_method_with_private_modifier() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(UtilityClassWithPrivateMethod.class)
                                                                     .addDefaultValue(DEFAULT_VALUE)
                                                                     .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }

    @Test
    public void ignore_the_method_with_nullable_parameters() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(ClassWithNullableMethodParameters.class)
                                                                     .excludeMethod("methodWithMixedParameters")
                                                                     .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }

    @Test
    public void pass_the_check_when_method_contains_nullable_and_not_nullable_parameters() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(ClassWithNullableMethodParameters.class)
                                                                     .addDefaultValue(DEFAULT_VALUE)
                                                                     .addDefaultValue(new NullToleranceTestShould())
                                                                     .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_when_parameter_of_invokable_method_does_not_have_default_value() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(ClassWithNullableMethodParameters.class)
                                                                     .build();
        nullToleranceTest.check();
    }

    @Test
    public void pass_the_check_when_method_contains_message_argument_type() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(ClassWithMessageMethodParameter.class)
                                                                     .build();
        final boolean result = nullToleranceTest.check();
        assertTrue(result);
    }

    @Test
    public void not_pass_the_check_when_not_null_check_is_not_present() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(UtilityClassWithReferenceParameters.class)
                                                                     .excludeMethod(METHOD_NAME_WITHOUT_CHECK)
                                                                     .build();
        final boolean result = nullToleranceTest.check();
        assertFalse(result);
    }

    @Test
    public void not_pass_the_check_when_stack_trace_does_not_match_expected() {
        final NullToleranceTest nullToleranceTest =
                NullToleranceTest.newBuilder()
                                 .setClass(UtilityClassWithThrownExceptionInTheMethod.class)
                                 .excludeMethod("methodWhichThrowsException")
                                 .build();
        final boolean result = nullToleranceTest.check();
        assertFalse(result);
    }

    @Test
    public void pass_the_check_when_method_arguments_are_wrapper_types() {
        final NullToleranceTest nullToleranceTest =
                NullToleranceTest.newBuilder()
                                 .setClass(ClassWithMethodWithWrapperTypeArguments.class)
                                 .build();
        final boolean result = nullToleranceTest.check();
        assertTrue(result);
    }

    @Test
    public void pass_the_check_when_method_has_varargs_parameter() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(ClassWithVarargsMethodParameters.class)
                                                                     .build();
        final boolean result = nullToleranceTest.check();
        assertTrue(result);
    }

    @Test
    public void not_check_synthetic_methods() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(ClassWithSyntheticMethods.class)
                                                                     .build();
        final boolean result = nullToleranceTest.check();
        assertTrue(result);
    }

    /*
     * Test utility classes
     ******************************/

    @SuppressWarnings("unused") // accessed via reflection.
    private static class UtilityClassWithReferenceParameters {

        public static void methodWithoutCheck(Object param) {
        }

        public static void methodWithCheck(Object first, Object second) {
            checkNotNull(first);
            checkNotNull(second);
        }

        public static void methodWithParameterTypeWhichDoesNotCheckInside(Object first, Object second) {
            methodWithCheck(first, second);
        }
    }

    @SuppressWarnings("unused") // accessed via reflection.
    private static class UtilityClassWithPrimitiveParameters {

        public static void methodWithPrimitiveParams(int first, double second) {
        }
    }

    @SuppressWarnings("unused") // accessed via reflection.
    private static class UtilityClassWithMixedParameters {

        public static void methodWithMixedParameterTypesWithoutCheck(long first, Object second) {
        }

        public static void methodWithMixedParameterTypesWithCheck(float first, Object second) {
            checkNotNull(second);
        }
    }

    @SuppressWarnings("unused") // accessed via reflection.
    private static class UtilityClassWithThrownExceptionInTheMethod {

        public static void methodWhichThrowsException(Object param) {
            throw new RuntimeException("Occurred exception.");
        }

        @SuppressWarnings("ProhibitedExceptionThrown") // need to test the matching of the stack trace element.
        public static void methodWhichThrowsNPE(Object param) {
            throw new NullPointerException("Occurred NPE");
        }
    }

    @SuppressWarnings("unused") // accessed via reflection.
    private static class ClassWithNonUtilMethod {
        public void nonUtilMethod(Object param) {
        }

        public static void utilMethod(Object param) {
            checkNotNull(param);
        }
    }

    @SuppressWarnings("unused") // accessed via reflection.
    private static class UtilityClassWithPrivateMethod {

        private static void privateMethod(Object first, Object second) {
        }

        public static void nonPrivateMethod(Object first, Object second) {
            checkNotNull(first);
            checkNotNull(second);
        }
    }

    @SuppressWarnings("unused") // accessed via reflection.
    private static class ClassWithNullableMethodParameters {

        public static void nullableParamsMethod(@Nullable Object first, @Nullable Object second) {
        }

        public static void methodWithMixedParameters(@Nullable Object first, NullToleranceTestShould second) {
            checkNotNull(second);
        }
    }

    @SuppressWarnings("unused") // accessed via reflection.
    private static class ClassWithMessageMethodParameter {

        public static void messageParamsMethod(EventFilter first, EventFilter second) {
            checkNotNull(first);
            checkNotNull(second);
        }

        public static void messageParamMethod(Message first) {
            checkNotNull(first);
        }
    }

    @SuppressWarnings("unused") // accessed via reflection.
    private static class ClassWithMethodWithWrapperTypeArguments {
        public static void wrapperArgumentsMethod(Integer first, Boolean second) {
            checkNotNull(first);
            checkNotNull(second);
        }
    }

    @SuppressWarnings("unused") // accessed via reflection.
    private static class ClassWithVarargsMethodParameters {
        public static void methodWithVarargsArgument(Object... args) {
            checkNotNull(args);
        }

        public static void methodWithVarargsArgument(int... args) {
            checkNotNull(args);
        }

        public static void methodWithVarargsArgument(Object obj, Integer... args) {
            checkNotNull(obj);
            checkNotNull(args);
        }
    }

    @SuppressWarnings("unused") // accessed via reflection.
    private static class ClassWithSyntheticMethods {

        private Object withSyntheticAccessor;

        private Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // Simulate some work
                if (withSyntheticAccessor == null) {
                    withSyntheticAccessor = new Object();
                }
            }
        };
    }
}
