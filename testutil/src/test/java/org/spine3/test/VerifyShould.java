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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("ClassWithTooManyMethods")
public class VerifyShould {

    @Test
    public void extend_Assert_class() {
        final Type expectedSuperclass = Assert.class;
        final Type actualSuperclass = Verify.class.getGenericSuperclass();
        assertEquals(expectedSuperclass, actualSuperclass);
    }

    @Test
    public void have_private_ctor() {
        assertTrue(Tests.hasPrivateUtilityConstructor(Verify.class));
    }

    @SuppressWarnings({"ThrowCaughtLocally", "ErrorNotRethrown"})
    @Test
    public void mangle_assertion_error() {
        final AssertionError sourceError = new AssertionError();
        final int framesBefore = sourceError.getStackTrace().length;

        try {
            throw Verify.mangledException(sourceError);
        } catch (AssertionError e) {
            final int framesAfter = e.getStackTrace().length;

            assertEquals(framesBefore - 1, framesAfter);
        }
    }

    @SuppressWarnings({"ThrowCaughtLocally", "ErrorNotRethrown"})
    @Test
    public void mangle_assertion_error_for_specified_frame_count() {
        final AssertionError sourceError = new AssertionError();
        final int framesBefore = sourceError.getStackTrace().length;
        final int framesToPop = 3;

        try {
            throw Verify.mangledException(sourceError, framesToPop);
        } catch (AssertionError e) {
            final int framesAfter = e.getStackTrace().length;

            assertEquals(framesBefore - framesToPop + 1, framesAfter);
        }
    }

    @SuppressWarnings("ErrorNotRethrown")
    @Test
    public void fail_with_specified_message_and_cause() {
        final String message = "Test failed";
        final Throwable cause = new Error();

        try {
            Verify.fail(message, cause);
            fail("Error was not thrown");
        } catch (AssertionError e) {
            assertEquals(message, e.getMessage());
            assertEquals(cause, e.getCause());
        }
    }

    @Test(expected = AssertionError.class)
    public void fail_if_float_values_are_same_types_of_infinity() {
        final float anyDeltaAcceptable = 0.0f;
        Verify.assertNotEquals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, anyDeltaAcceptable);
        Verify.assertNotEquals(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, anyDeltaAcceptable);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_float_values_are_NaN() {
        final float anyDeltaAcceptable = 0.0f;
        Verify.assertNotEquals(Float.NaN, Float.NaN, anyDeltaAcceptable);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_float_values_are_equal() {
        final float positiveValue = 5.0f;
        final float negativeValue = -positiveValue;
        final float equalToValuesDifference = positiveValue - negativeValue;
        Verify.assertNotEquals(positiveValue, negativeValue, equalToValuesDifference);
    }

    @Test
    public void pass_if_float_values_are_different_types_of_infinity() {
        final float anyDeltaAcceptable = 0.0f;
        Verify.assertNotEquals(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, anyDeltaAcceptable);
        Verify.assertNotEquals(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, anyDeltaAcceptable);
    }

    @Test
    public void pass_if_float_values_are_not_equal() {
        final float expected = 0.0f;
        final float actual = 1.0f;
        final float lessThanValuesDifference = Math.abs(expected - actual) - 0.1f;
        Verify.assertNotEquals(expected, actual, lessThanValuesDifference);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_bool_values_are_equal() {
        Verify.assertNotEquals(true, true);
    }

    @Test
    public void pass_if_bool_values_are_not_equal() {
        Verify.assertNotEquals(true, false);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_byte_values_are_equal() {
        Verify.assertNotEquals((byte) 0, (byte) 0);
    }

    @Test
    public void pass_if_byte_values_are_not_equal() {
        Verify.assertNotEquals((byte) 0, (byte) 1);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_char_values_are_equal() {
        Verify.assertNotEquals('a', 'a');
    }

    @Test
    public void pass_if_char_values_are_not_equal() {
        Verify.assertNotEquals('a', 'b');
    }

    @Test(expected = AssertionError.class)
    public void fail_if_short_values_are_equal() {
        Verify.assertNotEquals((short) 0, (short) 0);
    }

    @Test
    public void pass_if_short_values_are_not_equal() {
        Verify.assertNotEquals((short) 0, (short) 1);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_object_is_not_instance_of_specified_type() {
        Verify.assertInstanceOf(Integer.class, "");
    }

    @Test
    public void pass_if_object_is_instance_of_specified_type() {
        final String instance = "";
        Verify.assertInstanceOf(instance.getClass(), instance);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_object_is_instance_of_specified_type() {
        final String instance = "";
        Verify.assertNotInstanceOf(instance.getClass(), instance);
    }

    @Test
    public void pass_if_object_is_not_instance_of_specified_type() {
        Verify.assertNotInstanceOf(Integer.class, "");
    }

    @Test(expected = AssertionError.class)
    public void fail_if_iterable_is_not_empty() {
        Verify.assertIterableEmpty(FluentIterable.of(1));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_iterable_is_null() {
        Verify.assertIterableEmpty(null);
        Verify.assertIterableNotEmpty(null);
    }

    @Test
    public void pass_if_iterable_is_empty() {
        Verify.assertIterableEmpty(FluentIterable.of());
    }

    @Test(expected = AssertionError.class)
    public void fail_if_map_is_not_empty() {
        final Map<Integer, Integer> map = new HashMap<>();

        map.put(1, 1);

        Verify.assertEmpty(map);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_map_is_null() {
        Verify.assertEmpty((Map) null);
        Verify.assertNotEmpty((Map) null);
    }

    @Test
    public void pass_if_map_is_empty() {
        final Map emptyMap = new HashMap();
        Verify.assertEmpty(emptyMap);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_multimap_is_not_empty() {
        final Multimap<Integer, Integer> multimap = ArrayListMultimap.create();

        multimap.put(1, 1);

        Verify.assertEmpty(multimap);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_multimap_is_null() {
        Verify.assertEmpty((Multimap) null);
    }

    @Test
    public void pass_if_multimap_is_empty() {
        Verify.assertEmpty(ArrayListMultimap.create());
    }

    @Test(expected = AssertionError.class)
    public void fail_if_iterable_is_empty() {
        Verify.assertNotEmpty(FluentIterable.of());
    }

    @Test
    public void pass_if_iterable_is_not_empty() {
        Verify.assertNotEmpty(FluentIterable.of(1));
    }

    @Test(expected = AssertionError.class)
    public void fail_if_map_is_empty() {
        final Map emptyMap = new HashMap();
        Verify.assertNotEmpty(emptyMap);
    }

    @Test
    public void pass_if_map_is_not_empty() {
        final Map<Integer, Integer> map = new HashMap<>();

        map.put(1, 1);

        Verify.assertNotEmpty(map);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_multimap_is_empty() {
        Verify.assertNotEmpty(ArrayListMultimap.create());
    }

    @Test
    public void pass_if_multimap_is_not_empty() {
        final Multimap<Integer, Integer> multimap = ArrayListMultimap.create();

        multimap.put(1, 1);

        Verify.assertNotEmpty(multimap);
    }

    @SuppressWarnings("ZeroLengthArrayAllocation")
    @Test(expected = AssertionError.class)
    public void fail_if_array_is_empty() {
        final Integer[] array = {};
        Verify.assertNotEmpty(array);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_array_is_null() {
        Verify.assertNotEmpty((Integer[]) null);
    }

    @Test
    public void pass_if_array_is_not_empty() {
        final Integer[] array = {1, 2, 3};
        Verify.assertNotEmpty(array);
    }

}
