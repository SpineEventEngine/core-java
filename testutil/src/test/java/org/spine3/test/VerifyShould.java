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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass"})
public class VerifyShould {

    private static final String emptyString = "";
    private static final String notEmptyString = "Not empty string";
    private static final String mapName = "map";

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
    public void fail_if_float_values_are_positive_infinity() {
        final float anyDeltaAcceptable = 0.0f;
        Verify.assertNotEquals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, anyDeltaAcceptable);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_float_values_are_negative_infinity() {
        final float anyDeltaAcceptable = 0.0f;
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
    public void fail_if_iterable_is_null_in_iterable_empty() {
        Verify.assertIterableEmpty(null);
    }

    @Test
    public void pass_if_iterable_is_empty() {
        Verify.assertIterableEmpty(FluentIterable.of());
    }

    @Test(expected = AssertionError.class)
    public void fail_if_map_is_not_empty() {
        Verify.assertEmpty(Collections.singletonMap(1, 1));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_map_is_null_in_assert_empty() {
        Verify.assertEmpty((Map) null);
    }

    @Test
    public void pass_if_map_is_empty() {
        Verify.assertEmpty(Collections.emptyMap());
    }

    @Test(expected = AssertionError.class)
    public void fail_if_multimap_is_not_empty() {
        final Multimap<Integer, Integer> multimap = ArrayListMultimap.create();

        multimap.put(1, 1);

        Verify.assertEmpty(multimap);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_multimap_is_null_in_assert_empty() {
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

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_iterable_is_null_in_iterable_not_empty() {
        Verify.assertIterableNotEmpty(null);
    }

    @Test
    public void pass_if_iterable_is_not_empty() {
        Verify.assertNotEmpty(FluentIterable.of(1));
    }

    @Test(expected = AssertionError.class)
    public void fail_if_map_is_empty() {
        Verify.assertNotEmpty(Collections.emptyMap());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_map_is_null_in_not_empty() {
        Verify.assertNotEmpty((Map) null);
    }

    @Test
    public void pass_if_map_is_not_empty() {
        Verify.assertNotEmpty(Collections.singletonMap(1, 1));
    }

    @Test(expected = AssertionError.class)
    public void fail_if_multimap_is_empty() {
        Verify.assertNotEmpty(ArrayListMultimap.create());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_multimap_is_null_in_assert_not_empty() {
        Verify.assertNotEmpty((Multimap) null);
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
        Verify.assertNotEmpty(new Integer[0]);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_array_is_null_in_assert_not_empty() {
        Verify.assertNotEmpty((Integer[]) null);
    }

    @Test
    public void pass_if_array_is_not_empty() {
        final Integer[] array = {1, 2, 3};
        Verify.assertNotEmpty(array);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_object_array_size_is_not_equal() {
        Verify.assertSize(-1, new Object[1]);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_array_is_null_in_assert_size() {
        Verify.assertSize(0, (Integer[]) null);
    }

    @Test
    public void pass_if_object_array_size_is_equal() {
        final int size = 0;
        Verify.assertSize(size, new Object[size]);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_iterable_size_is_not_equal() {
        Verify.assertSize(-1, FluentIterable.of(1));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_iterable_is_null_in_assert_size() {
        Verify.assertSize(0, (Iterable) null);
    }

    @Test
    public void pass_if_iterable_size_is_equal() {
        Verify.assertSize(0, FluentIterable.of());
    }

    @Test(expected = AssertionError.class)
    public void fail_if_map_size_is_not_equal() {
        Verify.assertSize(-1, Collections.emptyMap());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_map_is_null_in_assert_size() {
        Verify.assertSize(0, (Map) null);
    }

    @Test
    public void pass_if_map_size_is_equal() {
        Verify.assertSize(0, Collections.emptyMap());
    }

    @Test(expected = AssertionError.class)
    public void fail_if_multimap_size_is_not_equal() {
        Verify.assertSize(-1, ArrayListMultimap.create());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_multimap_is_null_in_assert_size() {
        Verify.assertSize(0, (Multimap) null);
    }

    @Test
    public void pass_if_multimap_size_is_equal() {
        Verify.assertSize(0, ArrayListMultimap.create());
    }

    @Test(expected = AssertionError.class)
    public void fail_if_collection_size_is_not_equal() {
        Verify.assertSize(-1, Collections.emptyList());
    }

    @Test
    public void pass_if_collection_size_is_equal() {
        Verify.assertSize(0, Collections.emptyList());
    }

    @Test(expected = AssertionError.class)
    public void fail_if_string_not_contains_char_sequence() {
        Verify.assertContains(notEmptyString, emptyString);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_char_sequence_is_null_in_contains() {
        Verify.assertContains(null, emptyString);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_string_is_null_in_contains() {
        Verify.assertContains(emptyString, (String) null);
    }

    @SuppressWarnings({"ConstantConditions", "ErrorNotRethrown"})
    @Test(expected = AssertionError.class)
    public void fail_if_contains_char_sequence_or_string_is_null() {
        final String nullString = null;

        try {
            Verify.assertContains(null, emptyString);
        } catch (AssertionError e) {
            Verify.assertContains(emptyString, nullString);
        }
    }

    @Test
    public void pass_if_string_contains_char_sequence() {
        Verify.assertContains(emptyString, notEmptyString);
    }

    @Test(expected = AssertionError.class)
    public void fail_is_string_contains_char_sequence() {
        Verify.assertNotContains(emptyString, notEmptyString);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_char_sequence_is_null_in_not_contains() {
        Verify.assertNotContains(null, emptyString);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_string_is_null_in_not_contains() {
        Verify.assertNotContains(emptyString, (String) null);
    }

    @Test
    public void pass_if_string_not_contains_char_sequence() {
        Verify.assertNotContains(notEmptyString, emptyString);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_collection_not_contains_item() {
        Verify.assertContains(1, Collections.emptyList());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_collections_is_null_in_contains() {
        Verify.assertContains(1, (Collection) null);
    }

    @Test
    public void pass_if_collection_contains_item() {
        final Integer item = 1;
        Verify.assertContains(item, Collections.singletonList(item));
    }

    @Test(expected = AssertionError.class)
    public void fail_if_immutable_collection_not_contains_item() {
        Verify.assertContains(1, ImmutableList.of());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_immutable_collections_is_null_in_contains() {
        Verify.assertContains(1, null);
    }

    @Test
    public void pass_if_immutable_collection_contains_item() {
        final Integer item = 1;
        Verify.assertContains(item, ImmutableList.of(item));
    }

    @Test(expected = AssertionError.class)
    public void fail_if_iterable_not_contains_all() {
        Verify.assertContainsAll(Collections.emptyList(), 1);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_iterable_is_null_in_contains_all() {
        Verify.assertContainsAll(null);
    }

    @Test
    public void pass_if_iterable_contains_all() {
        final Integer item = 1;
        Verify.assertContainsAll(Collections.singletonList(item), item);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_map_are_not_equal() {
        final Map<Integer, Map<Integer, Integer>> firstOne = Collections.singletonMap(1, Collections.singletonMap(1, 1));
        final Map<Integer, Map<Integer, Integer>> secondOne = Collections.singletonMap(1, Collections.singletonMap(1, 2));

        Verify.assertMapsEqual(firstOne, secondOne, mapName);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void pass_if_maps_are_null() {
        Verify.assertMapsEqual(null, null, mapName);
    }

    @Test
    public void pass_if_maps_are_equal() {
        final Map<Integer, Map<Integer, Integer>> firstOne = Collections.singletonMap(1, Collections.singletonMap(1, 1));
        final Map<Integer, Map<Integer, Integer>> secondOne = new HashMap<>(firstOne);

        Verify.assertMapsEqual(firstOne, secondOne, mapName);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_sets_are_not_equal() {
        final Set<Integer> firstOne = Sets.newHashSet(1, 2, 3, 4);
        final Set<Integer> secondOne = Sets.newHashSet(1, 2, 4);

        Verify.assertSetsEqual(firstOne, secondOne);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void pass_if_sets_are_null() {
        Verify.assertSetsEqual(null, null);
    }

    @Test
    public void pass_if_sets_are_equal() {
        final Set<Integer> firstOne = Sets.newHashSet(1, 2, 3);
        final Set<Integer> secondOne = Sets.newHashSet(firstOne);

        Verify.assertSetsEqual(firstOne, secondOne);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_multimap_not_contains_entry() {
        Verify.assertContainsEntry(1, 1, ArrayListMultimap.<Integer, Integer>create());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_multimap_is_null_in_contains_entry() {
        Verify.assertContainsEntry(1, 1, null);
    }

    @Test
    public void pass_if_multimap_contains_entry() {
        final Integer entryKey = 1;
        final Integer entryValue = 1;

        final Multimap<Integer, Integer> multimap = ArrayListMultimap.create();
        multimap.put(entryKey, entryValue);

        Verify.assertContainsEntry(entryKey, entryValue, multimap);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_map_not_contains_key() {
        Verify.assertContainsKey(1, Collections.emptyMap());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_map_is_null_in_contains_key() {
        Verify.assertContainsKey(1, null);
    }

    @Test
    public void pass_if_map_contains_key() {
        final Integer key = 1;
        Verify.assertContainsKey(key, Collections.singletonMap(key, 1));
    }

    @Test(expected = AssertionError.class)
    public void fail_if_map_contains_denied_key() {
        final Integer key = 1;
        Verify.denyContainsKey(key, Collections.singletonMap(key, 1));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_map_is_null_in_deny_contains_key() {
        Verify.denyContainsKey(1, null);
    }

    @Test
    public void pass_if_map_not_contains_denied_key() {
        Verify.denyContainsKey(1, Collections.emptyMap());
    }

    @Test(expected = AssertionError.class)
    public void fail_if_map_not_contains_entry() {
        final Integer key = 0;
        Verify.assertContainsKeyValue(key, 0, Collections.singletonMap(key, 1));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_map_is_null_in_contains_key_value() {
        Verify.assertContainsKeyValue(1, 1, null);
    }

    @Test
    public void pass_if_map_contains_entry() {
        final Integer key = 1;
        final Integer value = 1;

        Verify.assertContainsKeyValue(key, value, Collections.singletonMap(key, value));
    }

    @Test(expected = AssertionError.class)
    public void fail_if_collection_contains_item() {
        final Integer item = 1;
        Verify.assertNotContains(item, Collections.singletonList(item));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_collection_is_null_in_not_contains() {
        Verify.assertNotContains(1, null);
    }

    @Test
    public void pass_if_collection_not_contains_item() {
        Verify.assertNotContains(1, Collections.emptyList());
    }

    @Test(expected = AssertionError.class)
    public void fail_if_iterable_contains_item() {
        final Integer item = 1;
        Verify.assertNotContains(item, FluentIterable.of(item));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_iterable_is_null_in_contains_item() {
        Verify.assertNotContains(1, (Iterable) null);
    }

    @Test
    public void pass_if_iterable_not_contains_item() {
        Verify.assertNotContains(1, FluentIterable.of());
    }

    @Test(expected = AssertionError.class)
    public void fail_if_map_contains_key() {
        final Integer key = 1;
        Verify.assertNotContainsKey(key, Collections.singletonMap(key, 1));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_map_is_null_in_not_contains_key() {
        Verify.assertNotContainsKey(1, null);
    }

    @Test
    public void pass_if_map_not_contains_key() {
        Verify.assertNotContainsKey(1, Collections.emptyMap());
    }

}
