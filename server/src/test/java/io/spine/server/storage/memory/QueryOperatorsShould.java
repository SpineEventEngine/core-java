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

package io.spine.server.storage.memory;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.junit.Test;

import static com.google.protobuf.util.Timestamps.add;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static io.spine.client.ColumnFilter.Operator;
import static io.spine.client.ColumnFilter.Operator.EQUAL;
import static io.spine.client.ColumnFilter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.ColumnFilter.Operator.GREATER_THAN;
import static io.spine.client.ColumnFilter.Operator.LESS_OR_EQUAL;
import static io.spine.client.ColumnFilter.Operator.LESS_THAN;
import static io.spine.server.storage.memory.QueryOperators.eval;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Tests.nullRef;
import static io.spine.time.Durations2.seconds;
import static io.spine.time.Time.getCurrentTime;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("Duplicates")     // Comparison tests are similar but cannot be simplified to one
public class QueryOperatorsShould {

    @Test
    public void have_private_util_ctor() {
        assertHasPrivateParameterlessCtor(QueryOperators.class);
    }

    @Test
    public void not_accept_nulls() {
        new NullPointerTester().testAllPublicStaticMethods(QueryOperators.class);
    }

    @SuppressWarnings("RedundantStringConstructorCall") // We need an equal but not the same object
    @Test
    public void compare_equal_instances() {
        final String left = "myobject";
        final Object right = new String(left);
        final Object third = new String(left);

        // The checks taken from the java.lang.Object.equals Javadoc
        assertTrue("basic", eval(left, EQUAL, right));
        assertTrue("symmetric", eval(right, EQUAL, left));
        assertTrue("reflective", eval(left, EQUAL, left));
        assertTrue("transitive", eval(left, EQUAL, third) || eval(third, EQUAL, right));
        assertTrue("nullable", eval(null, EQUAL, null));
        assertTrue("consistent", eval(left, EQUAL, right));
    }

    @Test
    public void compare_not_equal_instances() {
        final Object left = "one!";
        final Object right = "another!";

        assertFalse("direct order check", eval(left, EQUAL, right));
        assertFalse("reverse order check", eval(right, EQUAL, left));
    }

    @Test
    public void compare_timestamps_by_GT() {
        final Duration delta = seconds(5);
        final Timestamp small = getCurrentTime();
        final Timestamp medium = add(small, delta);
        final Timestamp big = add(medium, delta);

        assertTrue(eval(medium, GREATER_THAN, small));
        assertTrue(eval(big, GREATER_THAN, medium));
        assertTrue(eval(big, GREATER_THAN, small));

        assertFalse(eval(small, GREATER_THAN, small));
        assertFalse(eval(small, GREATER_THAN, big));
        assertFalse(eval(nullRef(), GREATER_THAN, small));
    }

    @Test
    public void compare_timestamps_by_GE() {
        final Duration delta = seconds(5);
        final Timestamp small = getCurrentTime();
        final Timestamp medium = add(small, delta);
        final Timestamp big = add(medium, delta);

        assertTrue(eval(medium, GREATER_OR_EQUAL, small));
        assertTrue(eval(big, GREATER_OR_EQUAL, medium));
        assertTrue(eval(big, GREATER_OR_EQUAL, small));
        assertTrue(eval(small, GREATER_OR_EQUAL, small));

        assertFalse(eval(small, GREATER_OR_EQUAL, big));
        assertFalse(eval(nullRef(), GREATER_OR_EQUAL, small));
    }

    @Test
    public void compare_timestamps_by_LT() {
        final Duration delta = seconds(5);
        final Timestamp small = getCurrentTime();
        final Timestamp medium = add(small, delta);
        final Timestamp big = add(medium, delta);

        assertTrue(eval(medium, LESS_THAN, big));
        assertTrue(eval(small, LESS_THAN, medium));
        assertTrue(eval(small, LESS_THAN, big));

        assertFalse(eval(big, LESS_THAN, big));
        assertFalse(eval(big, LESS_THAN, small));
        assertFalse(eval(nullRef(), LESS_THAN, nullRef()));
    }

    @Test
    public void compare_timestamps_by_LE() {
        final Duration delta = seconds(5);
        final Timestamp small = getCurrentTime();
        final Timestamp medium = add(small, delta);
        final Timestamp big = add(medium, delta);

        assertTrue(eval(medium, LESS_OR_EQUAL, big));
        assertTrue(eval(small, LESS_OR_EQUAL, medium));
        assertTrue(eval(small, LESS_OR_EQUAL, big));

        assertTrue(eval(medium, LESS_OR_EQUAL, medium));
        assertFalse(eval(big, LESS_OR_EQUAL, small));
        assertFalse(eval(medium, LESS_OR_EQUAL, nullRef()));
    }

    @Test
    public void compare_ints_by_GT() {
        assertGreater(42, 31);
    }

    @Test
    public void compare_ints_by_GE() {
        assertGreaterOrEqual(42, 41);
    }

    @Test
    public void compare_ints_by_LT() {
        assertLess(42, 314);
    }

    @Test
    public void compare_ints_by_LE() {
        assertLessOrEqual(42, 43);
    }

    @Test
    public void compare_strings_by_GT() {
        assertGreater("a", "!");
    }

    @Test
    public void compare_strings_by_GE() {
        assertGreaterOrEqual("d", "c");
    }

    @Test
    public void compare_strings_by_LT() {
        assertLess("Z", "a");
    }

    @Test
    public void compare_strings_by_LE() {
        assertLessOrEqual("a", "b");
    }

    @Test
    public void compare_doubles_by_GT() {
        assertGreater(42, 31);
    }

    @Test
    public void compare_doubles_by_GE() {
        assertGreaterOrEqual(42.1, 42.01);
    }

    @Test
    public void compare_doubles_by_LT() {
        assertLess(42.81, 314.0);
    }

    @Test
    public void compare_doubles_by_LE() {
        assertLessOrEqual(42.999, 43.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_compare_unsupported_types_by_GT() {
        eval(FaultyComparisonType.INSTANCE, GREATER_THAN, FaultyComparisonType.INSTANCE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_compare_unsupported_types_by_GE() {
        eval(FaultyComparisonType.INSTANCE, GREATER_OR_EQUAL, FaultyComparisonType.INSTANCE);

    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_compare_unsupported_types_by_LT() {
        eval(FaultyComparisonType.INSTANCE, LESS_THAN, FaultyComparisonType.INSTANCE);

    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_compare_unsupported_types_by_LE() {
        eval(FaultyComparisonType.INSTANCE, LESS_OR_EQUAL, FaultyComparisonType.INSTANCE);
    }

    private static void assertGreater(Object left, Object right) {
        assertStrict(left, right, GREATER_THAN);
    }

    private static void assertLess(Object left, Object right) {
        assertStrict(left, right, LESS_THAN);
    }

    private static void assertGreaterOrEqual(Object obj, Object less) {
        assertNotStrict(obj, less, GREATER_OR_EQUAL, GREATER_THAN);
    }

    private static void assertLessOrEqual(Object obj, Object less) {
        assertNotStrict(obj, less, LESS_OR_EQUAL, LESS_THAN);
    }

    private static void assertStrict(Object left, Object right, Operator operator) {
        assertTrue(eval(left, operator, right));
        assertFalse(eval(right, operator, left));
        assertFalse(eval(left, EQUAL, right));
    }

    private static void assertNotStrict(Object obj,
                                        Object other,
                                        Operator operator,
                                        Operator strictOperator) {
        assertStrict(obj, other, strictOperator);

        assertTrue(eval(obj, operator, obj));
        assertTrue(eval(obj, EQUAL, obj));
    }

    private static class FaultyComparisonType {
        @SuppressWarnings("InstantiationOfUtilityClass") // Not a utility class
        private static final FaultyComparisonType INSTANCE = new FaultyComparisonType();

        private FaultyComparisonType() {
            // Singleton type
        }
    }
}
