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
import java.security.acl.AclNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass"})
public class VerifyShould {

    private static final String EMPTY_STRING = "";
    private static final String NON_EMPTY_STRING = "Non-empty string";
    private static final String MAP_NAME = "map";

    @Test
    public void extend_Assert_class() {
        final Type expectedSuperclass = Assert.class;
        final Type actualSuperclass = Verify.class.getGenericSuperclass();
        assertEquals(expectedSuperclass, actualSuperclass);
    }

    @Test
    public void has_private_ctor() {
        assertTrue(Tests.hasPrivateParameterlessCtor(Verify.class));
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
        Verify.assertInstanceOf(Integer.class, EMPTY_STRING);
    }

    @Test
    public void pass_if_object_is_instance_of_specified_type() {
        Verify.assertInstanceOf(EMPTY_STRING.getClass(), EMPTY_STRING);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_object_is_instance_of_specified_type() {
        Verify.assertNotInstanceOf(EMPTY_STRING.getClass(), EMPTY_STRING);
    }

    @Test
    public void pass_if_object_is_not_instance_of_specified_type() {
        Verify.assertNotInstanceOf(Integer.class, EMPTY_STRING);
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
        Verify.assertSize(0, new Object[1]);
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
        Verify.assertSize(0, FluentIterable.of(1));
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
        Verify.assertSize(1, Collections.emptyMap());
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
        Verify.assertSize(1, ArrayListMultimap.create());
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
        Verify.assertSize(1, Collections.emptyList());
    }

    @Test
    public void pass_if_collection_size_is_equal() {
        Verify.assertSize(0, Collections.emptyList());
    }

    @Test(expected = AssertionError.class)
    public void fail_if_string_not_contains_char_sequence() {
        Verify.assertContains(NON_EMPTY_STRING, EMPTY_STRING);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_char_sequence_is_null_in_contains() {
        Verify.assertContains(null, EMPTY_STRING);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_string_is_null_in_contains() {
        Verify.assertContains(EMPTY_STRING, (String) null);
    }

    @SuppressWarnings({"ConstantConditions", "ErrorNotRethrown"})
    @Test(expected = AssertionError.class)
    public void fail_if_contains_char_sequence_or_string_is_null() {
        final String nullString = null;

        try {
            Verify.assertContains(null, EMPTY_STRING);
        } catch (AssertionError e) {
            Verify.assertContains(EMPTY_STRING, nullString);
        }
    }

    @Test
    public void pass_if_string_contains_char_sequence() {
        Verify.assertContains(EMPTY_STRING, NON_EMPTY_STRING);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_string_contains_char_sequence() {
        Verify.assertNotContains(EMPTY_STRING, NON_EMPTY_STRING);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_char_sequence_is_null_in_not_contains() {
        Verify.assertNotContains(null, EMPTY_STRING);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_string_is_null_in_not_contains() {
        Verify.assertNotContains(EMPTY_STRING, (String) null);
    }

    @Test
    public void pass_if_string_not_contains_char_sequence() {
        Verify.assertNotContains(NON_EMPTY_STRING, EMPTY_STRING);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_collection_not_contains_item() {
        Verify.assertContains(1, Collections.emptyList());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_collection_is_null_in_contains() {
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
        final Map<Integer, Map<Integer, Integer>> firstOne = singletonMap(1, singletonMap(1, 1));
        final Map<Integer, Map<Integer, Integer>> secondOne = singletonMap(1, singletonMap(1, 2));

        Verify.assertMapsEqual(firstOne, secondOne, MAP_NAME);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void pass_if_maps_are_null() {
        Verify.assertMapsEqual(null, null, MAP_NAME);
    }

    @Test
    public void pass_if_maps_are_equal() {
        final Map<Integer, Map<Integer, Integer>> firstOne = singletonMap(1, singletonMap(1, 1));
        final Map<Integer, Map<Integer, Integer>> secondOne = new HashMap<>(firstOne);

        Verify.assertMapsEqual(firstOne, secondOne, MAP_NAME);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_sets_are_not_equal_by_size() {
        final Set<Integer> firstOne = Sets.newHashSet(1, 2, 3, 4);
        final Set<Integer> secondOne = Sets.newHashSet(1, 2, 4);

        Verify.assertSetsEqual(firstOne, secondOne);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_sets_are_not_equal_by_content() {
        final Set<Integer> firstOne = Sets.newHashSet(1, 2, 3);
        final Set<Integer> secondOne = Sets.newHashSet(1, 2, 777);

        Verify.assertSetsEqual(firstOne, secondOne);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_sets_are_equal_by_size_but_have_over_5_differences() {
        final Set<Integer> firstOne = Sets.newHashSet(1, 2, 3, 4, 5, 6);
        final Set<Integer> secondOne = Sets.newHashSet(11, 12, 13, 14, 15, 16);

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

        Verify.assertContainsKeyValue(key, EMPTY_STRING, Collections.singletonMap(key, EMPTY_STRING));
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

    @Test(expected = AssertionError.class)
    public void fail_if_former_goes_after_latter_in_list() {
        final Integer firstItem = 1;
        final Integer secondItem = 2;

        final List<Integer> list = Arrays.asList(firstItem, secondItem);

        Verify.assertBefore(secondItem, firstItem, list);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_former_and_latter_are_equal() {
        final Integer sameItem = 1;
        Verify.assertBefore(sameItem, sameItem, Collections.singletonList(sameItem));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_list_is_null_in_assert_befor() {
        Verify.assertBefore(1, 2, null);
    }

    @Test
    public void pass_if_former_goes_before_latter_in_list() {
        final Integer firstItem = 1;
        final Integer secondItem = 2;

        final List<Integer> list = Arrays.asList(firstItem, secondItem);

        Verify.assertBefore(firstItem, secondItem, list);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_list_item_not_at_index() {
        final Integer firstItem = 1;
        final Integer secondItem = 2;

        final List<Integer> list = Arrays.asList(firstItem, secondItem);

        Verify.assertItemAtIndex(firstItem, list.indexOf(secondItem), list);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_list_is_null_in_assert_item_at_index() {
        Verify.assertItemAtIndex(1, 1, (List) null);
    }

    @Test
    public void pass_if_list_item_at_index() {
        final Integer value = 1;
        final List<Integer> list = Collections.singletonList(value);

        Verify.assertItemAtIndex(value, list.indexOf(value), list);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_array_item_not_at_index() {
        final Integer firstItem = 1;
        final Integer secondItem = 2;

        final Object[] array = {firstItem, secondItem};

        Verify.assertItemAtIndex(firstItem, 1, array);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_array_is_null_in_assert_item_at_index() {
        Verify.assertItemAtIndex(1, 1, (Object[]) null);
    }

    @Test
    public void pass_if_array_item_at_index() {
        final Integer value = 1;
        final Object[] array = {value};

        Verify.assertItemAtIndex(value, 0, array);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_array_not_starts_with_items() {
        final Integer[] array = {1, 2, 3};
        final Integer notStartsWith = 777;

        Verify.assertStartsWith(array, notStartsWith);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_array_is_null_in_starts_with() {
        Verify.assertStartsWith((Integer[]) null, 1, 2, 3);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_items_is_empty_in_array_starts_with() {
        Verify.assertStartsWith(new Integer[1]);
    }

    @Test
    public void pass_if_array_starts_with_items() {
        final Integer[] array = {1, 2};
        final Integer firstItem = array[0];

        Verify.assertStartsWith(array, firstItem);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_list_not_starts_with_items() {
        final List<Integer> list = Arrays.asList(1, 2, 3);
        final Integer notStartsWith = 777;

        Verify.assertStartsWith(list, notStartsWith);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_list_is_null_in_starts_with() {
        Verify.assertStartsWith((List) null, 1, 2, 3);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_items_is_empty_in_list_starts_with() {
        Verify.assertStartsWith(Collections.emptyList());
    }

    @Test
    public void pass_if_list_starts_with_items() {
        final List<Integer> list = Arrays.asList(1, 2, 3);
        final Integer firstItem = list.get(0);

        Verify.assertStartsWith(list, firstItem);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_list_not_ends_with_items() {
        final List<Integer> list = Arrays.asList(1, 2, 3);
        final Integer notEndsWith = 777;

        Verify.assertEndsWith(list, notEndsWith);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_list_is_null_in_ends_with() {
        Verify.assertEndsWith((List) null, 1, 2, 3);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_items_is_empty_in_list_ends_with() {
        Verify.assertEndsWith(Collections.emptyList());
    }

    @Test
    public void pass_if_list_ends_with_items() {
        final List<Integer> list = Arrays.asList(1, 2, 3);
        final Integer lastItem = list.get(list.size() - 1);

        Verify.assertEndsWith(list, lastItem);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_array_not_ends_with_items() {
        final Integer[] array = {1, 2, 3};
        final Integer notEndsWith = 777;

        Verify.assertEndsWith(array, notEndsWith);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_array_is_null_in_ends_with() {
        Verify.assertEndsWith((Integer[]) null, 1, 2, 3);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_items_is_empty_in_array_ends_with() {
        Verify.assertEndsWith(new Integer[1]);
    }

    @Test
    public void pass_if_array_ends_with() {
        final Integer[] array = {1, 2, 3};
        final Integer lastItem = array[array.length - 1];

        Verify.assertEndsWith(array, lastItem);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_objects_are_not_equal() {
        Verify.assertEqualsAndHashCode(1, 2);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_objects_are_equal_but_hash_codes_are_not_equal() {
        final ClassThatViolateHashCodeAndCloneableContract objectA = new ClassThatViolateHashCodeAndCloneableContract(1);
        final ClassThatViolateHashCodeAndCloneableContract objectB = new ClassThatViolateHashCodeAndCloneableContract(1);

        Verify.assertEqualsAndHashCode(objectA, objectB);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_objects_are_null() {
        Verify.assertEqualsAndHashCode(null, null);
    }

    @Test
    public void pass_if_objects_and_their_hash_codes_are_equal() {
        Verify.assertEqualsAndHashCode(1, 1);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_value_is_not_negative() {
        Verify.assertNegative(1);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_value_is_zero_in_assert_negative() {
        Verify.assertNegative(0);
    }

    @Test
    public void pass_if_value_is_negative() {
        Verify.assertNegative(-1);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_value_is_not_positive() {
        Verify.assertPositive(-1);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_value_is_zero_in_assert_positive() {
        Verify.assertPositive(0);
    }

    @Test
    public void pass_if_value_is_positive() {
        Verify.assertPositive(1);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_value_is_positive() {
        Verify.assertZero(1);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_value_is_negative() {
        Verify.assertZero(-1);
    }

    @Test
    public void pass_if_value_is_zero() {
        Verify.assertZero(0);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_clone_returns_same_object() {
        Verify.assertShallowClone(new ClassThatViolateHashCodeAndCloneableContract(1));
    }

    @Test(expected = AssertionError.class)
    public void fail_if_clone_does_not_work_correctly() {
        Verify.assertShallowClone(new ClassThatImplementCloneableIncorrectly(1));
    }

    @Test
    public void pass_if_cloneable_equals_and_hash_code_overridden_correctly() {
        Verify.assertShallowClone(new ClassThatImplementCloneableCorrectly(1));
    }

    @Test(expected = AssertionError.class)
    public void fail_if_class_instantiable() {
        Verify.assertClassNonInstantiable(Object.class);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_class_instantiable_through_reflection() {
        Verify.assertClassNonInstantiable(ClassWithPrivateCtor.class);
    }

    @Test
    public void pass_if_new_instance_throw_instantiable_exception() {
        Verify.assertClassNonInstantiable(void.class);
    }

    @Test
    public void pass_if_class_non_instantiable_through_reflection() {
        Verify.assertClassNonInstantiable(ClassThatThrowExceptionInConstructor.class);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_runnable_not_throws_error() {
        final Runnable notThrowsException = new Runnable() {
            @Override
            public void run() {
            }
        };

        Verify.assertError(AssertionError.class, notThrowsException);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_runnable_not_throws_specified_error() {
        final Runnable throwsAssertionError = new Runnable() {
            @Override
            public void run() {
                throw new AssertionError();
            }
        };

        Verify.assertError(Error.class, throwsAssertionError);
    }

    @Test
    public void pass_if_runnable_throws_specified_error() {
        final Runnable throwsAssertionError = new Runnable() {
            @Override
            public void run() {
                throw new AssertionError();
            }
        };

        Verify.assertError(AssertionError.class, throwsAssertionError);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_callable_not_throws_exception() {
        final Callable notThrowsException = new Callable() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        };

        Verify.assertThrows(Exception.class, notThrowsException);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_callable_not_throws_specified_exception() {
        final Callable throwsEmptyStackException = new Callable() {
            @Override
            public Object call() throws Exception {
                throw new EmptyStackException();
            }
        };

        Verify.assertThrows(Exception.class, throwsEmptyStackException);
    }

    @Test
    public void pass_if_callable_throws_specified_exception() {
        final Callable throwsEmptyStackException = new Callable() {
            @Override
            public Object call() throws Exception {
                throw new EmptyStackException();
            }
        };

        Verify.assertThrows(EmptyStackException.class, throwsEmptyStackException);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_runnable_not_throws_exception() {
        final Runnable notThrowsException = new Runnable() {
            @Override
            public void run() {
            }
        };

        Verify.assertThrows(Exception.class, notThrowsException);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_runnable_not_throws_specified_exception() {
        final Runnable throwsEmptyStackException = new Runnable() {
            @Override
            public void run() {
                throw new EmptyStackException();
            }
        };

        Verify.assertThrows(Exception.class, throwsEmptyStackException);
    }

    @Test
    public void pass_if_runnable_throws_specified_exception() {
        final Runnable throwsEmptyStackException = new Runnable() {
            @Override
            public void run() {
                throw new EmptyStackException();
            }
        };

        Verify.assertThrows(EmptyStackException.class, throwsEmptyStackException);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_callable_not_throws_exception_with_cause() {
        final Callable notThrowsException = new Callable() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        };

        Verify.assertThrowsWithCause(Exception.class, Exception.class, notThrowsException);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_callable_throws_exception_with_different_causes() {
        final Throwable expectedCause = new EmptyStackException();
        final Throwable actualCause = new AclNotFoundException();
        final RuntimeException runtimeException = new RuntimeException(actualCause);
        final Callable throwsRuntimeException = new Callable() {
            @Override
            public Object call() {
                throw runtimeException;
            }
        };

        Verify.assertThrowsWithCause(runtimeException.getClass(), expectedCause.getClass(), throwsRuntimeException);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_callable_expected_cause_is_null() {
        final Callable throwsRuntimeException = new Callable() {
            @Override
            public Object call() {
                throw new RuntimeException(new EmptyStackException());
            }
        };

        Verify.assertThrowsWithCause(EmptyStackException.class, null, throwsRuntimeException);
    }

    @Test
    public void pass_if_callable_throws_specified_exception_with_specified_cause() {
        final Throwable cause = new EmptyStackException();
        final RuntimeException runtimeException = new RuntimeException(cause);
        final Callable throwsRuntimeException = new Callable() {
            @Override
            public Object call() {
                throw runtimeException;
            }
        };

        Verify.assertThrowsWithCause(runtimeException.getClass(), cause.getClass(), throwsRuntimeException);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_runnable_not_throws_exception_with_cause() {
        final Runnable notThrowsException = new Runnable() {
            @Override
            public void run() {
            }
        };

        Verify.assertThrowsWithCause(Exception.class, Exception.class, notThrowsException);
    }

    @Test(expected = AssertionError.class)
    public void fail_if_runnable_throws_exception_with_different_causes() {
        final Throwable expectedCause = new EmptyStackException();
        final Throwable actualCause = new AclNotFoundException();
        final RuntimeException runtimeException = new RuntimeException(actualCause);
        final Runnable throwsRuntimeException = new Runnable() {
            @Override
            public void run() {
                throw runtimeException;
            }
        };

        Verify.assertThrowsWithCause(runtimeException.getClass(), expectedCause.getClass(), throwsRuntimeException);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void fail_if_runnable_expected_cause_is_null() {
        final Runnable throwsRuntimeException = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException(new EmptyStackException());
            }
        };

        Verify.assertThrowsWithCause(EmptyStackException.class, null, throwsRuntimeException);
    }

    @Test
    public void pass_if_runnable_throws_specified_exception_with_specified_cause() {
        final Throwable cause = new EmptyStackException();
        final RuntimeException runtimeException = new RuntimeException(cause);
        final Runnable throwsRuntimeException = new Runnable() {
            @Override
            public void run() {
                throw runtimeException;
            }
        };

        Verify.assertThrowsWithCause(runtimeException.getClass(), cause.getClass(), throwsRuntimeException);
    }

    @SuppressWarnings("EqualsAndHashcode")
    private static class ClassThatViolateHashCodeAndCloneableContract implements Cloneable {
        private final int value;

        private ClassThatViolateHashCodeAndCloneableContract(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ClassThatViolateHashCodeAndCloneableContract that = (ClassThatViolateHashCodeAndCloneableContract) o;

            return value == that.value;
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        protected Object clone() throws CloneNotSupportedException {
            return this;
        }
    }

    private static class ClassThatImplementCloneableCorrectly implements Cloneable {
        private final int value;

        private ClassThatImplementCloneableCorrectly(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ClassThatImplementCloneableCorrectly that = (ClassThatImplementCloneableCorrectly) o;

            return value == that.value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    private static final class ClassThatImplementCloneableIncorrectly implements Cloneable {
        private final int value;

        private ClassThatImplementCloneableIncorrectly(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ClassThatImplementCloneableIncorrectly that = (ClassThatImplementCloneableIncorrectly) o;

            return value == that.value;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return new ClassThatImplementCloneableIncorrectly(value + 1);
        }
    }

    private static class ClassWithPrivateCtor {
        @SuppressWarnings("RedundantNoArgConstructor")
        private ClassWithPrivateCtor() {
        }
    }

    private static class ClassThatThrowExceptionInConstructor {
        private ClassThatThrowExceptionInConstructor() {
            throw new AssertionError("This is non-instantiable class");
        }
    }

}
