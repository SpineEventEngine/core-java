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

package org.spine3.client;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.client.ColumnFilter.Operator;

import static com.google.protobuf.util.Timestamps.add;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.client.ColumnFilter.Operator.EQUAL;
import static org.spine3.client.ColumnFilter.Operator.GREATER_OR_EQUAL;
import static org.spine3.client.ColumnFilter.Operator.GREATER_THAN;
import static org.spine3.client.ColumnFilter.Operator.LESS_OR_EQUAL;
import static org.spine3.client.ColumnFilter.Operator.LESS_THAN;
import static org.spine3.client.QueryOperators.compare;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.test.Tests.nullRef;
import static org.spine3.time.Durations2.seconds;
import static org.spine3.time.Time.getCurrentTime;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("Duplicates")     // Comparison tests are similar but cannot be simplified to one
public class QueryOperatorsShould { // effectively and keeping the 8

    @Test
    public void have_private_util_ctor() {
        assertHasPrivateParameterlessCtor(QueryOperators.class);
    }

    @Test
    public void not_accept_nulls() {
        new NullPointerTester().testAllPublicStaticMethods(QueryOperators.class);
    }

    @Test
    public void retrieve_operator_symbolic_repr() {
        assertSymbol(EQUAL, "=");
        assertSymbol(GREATER_THAN, ">");
        assertSymbol(LESS_THAN, "<");
        assertSymbol(GREATER_OR_EQUAL, ">=");
        assertSymbol(LESS_OR_EQUAL, "<=");
    }

    @SuppressWarnings("RedundantStringConstructorCall") // We need an equal but not the same object
    @Test
    public void compare_equal_instances() {
        final String left = "myobject";
        final Object right = new String(left);
        final Object third = new String(left);

        // The checks taken from the java.lang.Object.equals Javadoc
        assertTrue("basic", compare(left, EQUAL, right));
        assertTrue("symmetric", compare(right, EQUAL, left));
        assertTrue("reflective", compare(left, EQUAL, left));
        assertTrue("transitive", compare(left, EQUAL, third) || compare(third, EQUAL, right));
        assertTrue("nullable", compare(null, EQUAL, null));
        assertTrue("consistent", compare(left, EQUAL, right));
    }

    @Test
    public void compare_not_equal_instances() {
        final Object left = "one!";
        final Object right = "another!";

        assertFalse("direct order check", compare(left, EQUAL, right));
        assertFalse("reverse order check", compare(right, EQUAL, left));
    }

    @Test
    public void compare_timestamps_by_GT() {
        final Duration delta = seconds(5);
        final Timestamp small = getCurrentTime();
        final Timestamp medium = add(small, delta);
        final Timestamp big = add(medium, delta);

        assertTrue(compare(medium, GREATER_THAN, small));
        assertTrue(compare(big, GREATER_THAN, medium));
        assertTrue(compare(big, GREATER_THAN, small));

        assertFalse(compare(small, GREATER_THAN, small));
        assertFalse(compare(small, GREATER_THAN, big));
        assertFalse(compare(nullRef(), GREATER_THAN, small));
    }

    @Test
    public void compare_timestamps_by_GE() {
        final Duration delta = seconds(5);
        final Timestamp small = getCurrentTime();
        final Timestamp medium = add(small, delta);
        final Timestamp big = add(medium, delta);

        assertTrue(compare(medium, GREATER_OR_EQUAL, small));
        assertTrue(compare(big, GREATER_OR_EQUAL, medium));
        assertTrue(compare(big, GREATER_OR_EQUAL, small));
        assertTrue(compare(small, GREATER_OR_EQUAL, small));

        assertFalse(compare(small, GREATER_OR_EQUAL, big));
        assertFalse(compare(nullRef(), GREATER_OR_EQUAL, small));
    }

    @Test
    public void compare_timestamps_by_LT() {
        final Duration delta = seconds(5);
        final Timestamp small = getCurrentTime();
        final Timestamp medium = add(small, delta);
        final Timestamp big = add(medium, delta);

        assertTrue(compare(medium, LESS_THAN, big));
        assertTrue(compare(small, LESS_THAN, medium));
        assertTrue(compare(small, LESS_THAN, big));

        assertFalse(compare(big, LESS_THAN, big));
        assertFalse(compare(big, LESS_THAN, small));
        assertFalse(compare(nullRef(), LESS_THAN, nullRef()));
    }

    @Test
    public void compare_timestamps_by_LE() {
        final Duration delta = seconds(5);
        final Timestamp small = getCurrentTime();
        final Timestamp medium = add(small, delta);
        final Timestamp big = add(medium, delta);

        assertTrue(compare(medium, LESS_OR_EQUAL, big));
        assertTrue(compare(small, LESS_OR_EQUAL, medium));
        assertTrue(compare(small, LESS_OR_EQUAL, big));

        assertTrue(compare(medium, LESS_OR_EQUAL, medium));
        assertFalse(compare(big, LESS_OR_EQUAL, small));
        assertFalse(compare(medium, LESS_OR_EQUAL, nullRef()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void fail_to_compare_unsupported_types_by_GT() {
        compare("foa", GREATER_THAN, "bor");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void fail_to_compare_unsupported_types_by_GE() {
        compare("boo", GREATER_OR_EQUAL, "far");

    }

    @Test(expected = UnsupportedOperationException.class)
    public void fail_to_compare_unsupported_types_by_LT() {
        compare("roo", LESS_THAN, "baf");

    }

    @Test(expected = UnsupportedOperationException.class)
    public void fail_to_compare_unsupported_types_by_LE() {
        compare("bfo", LESS_OR_EQUAL, "oar");

    }

    private static void assertSymbol(Operator operator, String symbol) {
        assertEquals(symbol, QueryOperators.toString(operator));
    }
}
