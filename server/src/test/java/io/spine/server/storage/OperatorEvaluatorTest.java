/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.storage;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import io.spine.server.storage.given.OperatorEvaluatorTestEnv.FaultyComparisonType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static com.google.protobuf.util.Timestamps.add;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.client.ColumnFilter.Operator;
import static io.spine.client.ColumnFilter.Operator.CFO_UNDEFINED;
import static io.spine.client.ColumnFilter.Operator.EQUAL;
import static io.spine.client.ColumnFilter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.ColumnFilter.Operator.GREATER_THAN;
import static io.spine.client.ColumnFilter.Operator.LESS_OR_EQUAL;
import static io.spine.client.ColumnFilter.Operator.LESS_THAN;
import static io.spine.server.storage.OperatorEvaluator.eval;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Tests.nullRef;
import static io.spine.time.Durations2.seconds;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings({
        "Duplicates" /* Comparison tests are similar but cannot be simplified to one. */,
        "ClassWithTooManyMethods" /* Many test cases required. */,
        "InnerClassMayBeStatic", "ClassCanBeStatic" /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names */
})
@DisplayName("OperatorEvaluator should")
class OperatorEvaluatorTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testStaticMethods(OperatorEvaluator.class, PACKAGE);
    }

    @SuppressWarnings("RedundantStringConstructorCall") // We need an equal but not the same object
    @Test
    @DisplayName("compare equal instances")
    void compareEqual() {
        String left = "myobject";
        Object right = new String(left);
        Object third = new String(left);

        // The checks taken from the java.lang.Object.equals Javadoc
        assertTrue(eval(left, EQUAL, right), "basic");
        assertTrue(eval(right, EQUAL, left), "symmetric");
        assertTrue(eval(left, EQUAL, left), "reflective");
        assertTrue(eval(left, EQUAL, third) || eval(third, EQUAL, right), "transitive");
        assertTrue(eval(null, EQUAL, null), "nullable");
        assertTrue(eval(left, EQUAL, right), "consistent");
    }

    @Test
    @DisplayName("compare non-equal instances")
    void compareNonEqual() {
        Object left = "one!";
        Object right = "another!";

        assertFalse(eval(left, EQUAL, right), "direct order check");
        assertFalse(eval(right, EQUAL, left), "reverse order check");
    }

    @Nested
    @DisplayName("compare timestamps by")
    class CompareTimestampsBy {

        @Test
        @DisplayName("`GREATER_THAN`")
        void gt() {
            Duration delta = seconds(5);
            Timestamp small = getCurrentTime();
            Timestamp medium = add(small, delta);
            Timestamp big = add(medium, delta);

            assertTrue(eval(medium, GREATER_THAN, small));
            assertTrue(eval(big, GREATER_THAN, medium));
            assertTrue(eval(big, GREATER_THAN, small));

            assertFalse(eval(small, GREATER_THAN, small));
            assertFalse(eval(small, GREATER_THAN, big));
            assertFalse(eval(nullRef(), GREATER_THAN, small));
        }

        @Test
        @DisplayName("`GREATER_OR_EQUAL`")
        void ge() {
            Duration delta = seconds(5);
            Timestamp small = getCurrentTime();
            Timestamp medium = add(small, delta);
            Timestamp big = add(medium, delta);

            assertTrue(eval(medium, GREATER_OR_EQUAL, small));
            assertTrue(eval(big, GREATER_OR_EQUAL, medium));
            assertTrue(eval(big, GREATER_OR_EQUAL, small));
            assertTrue(eval(small, GREATER_OR_EQUAL, small));

            assertFalse(eval(small, GREATER_OR_EQUAL, big));
            assertFalse(eval(nullRef(), GREATER_OR_EQUAL, small));
        }

        @Test
        @DisplayName("`LESS_THAN`")
        void lt() {
            Duration delta = seconds(5);
            Timestamp small = getCurrentTime();
            Timestamp medium = add(small, delta);
            Timestamp big = add(medium, delta);

            assertTrue(eval(medium, LESS_THAN, big));
            assertTrue(eval(small, LESS_THAN, medium));
            assertTrue(eval(small, LESS_THAN, big));

            assertFalse(eval(big, LESS_THAN, big));
            assertFalse(eval(big, LESS_THAN, small));
            assertFalse(eval(nullRef(), LESS_THAN, nullRef()));
        }

        @Test
        @DisplayName("`LESS_OR_EQUAL`")
        void le() {
            Duration delta = seconds(5);
            Timestamp small = getCurrentTime();
            Timestamp medium = add(small, delta);
            Timestamp big = add(medium, delta);

            assertTrue(eval(medium, LESS_OR_EQUAL, big));
            assertTrue(eval(small, LESS_OR_EQUAL, medium));
            assertTrue(eval(small, LESS_OR_EQUAL, big));

            assertTrue(eval(medium, LESS_OR_EQUAL, medium));
            assertFalse(eval(big, LESS_OR_EQUAL, small));
            assertFalse(eval(medium, LESS_OR_EQUAL, nullRef()));
        }
    }

    @Nested
    @DisplayName("compare ints by")
    class CompareIntsBy {

        @Test
        @DisplayName("`GREATER_THAN`")
        void gt() {
            assertGreater(42, 31);
        }

        @Test
        @DisplayName("`GREATER_OR_EQUAL`")
        void ge() {
            assertGreaterOrEqual(42, 41);
        }

        @Test
        @DisplayName("`LESS_THAN`")
        void lt() {
            assertLess(42, 314);
        }

        @Test
        @DisplayName("`LESS_OR_EQUAL`")
        void le() {
            assertLessOrEqual(42, 43);
        }
    }

    @Nested
    @DisplayName("compare strings by")
    class CompareStringsBy {

        @Test
        @DisplayName("`GREATER_THAN`")
        void gt() {
            assertGreater("a", "!");
        }

        @Test
        @DisplayName("`GREATER_OR_EQUAL`")
        void ge() {
            assertGreaterOrEqual("d", "c");
        }

        @Test
        @DisplayName("`LESS_THAN`")
        void lt() {
            assertLess("Z", "a");
        }

        @Test
        @DisplayName("`LESS_OR_EQUAL`")
        void le() {
            assertLessOrEqual("a", "b");
        }
    }

    @Nested
    @DisplayName("compare doubles by")
    class CompareDoublesBy {

        @Test
        @DisplayName("`GREATER_THAN`")
        void gt() {
            assertGreater(42, 31);
        }

        @Test
        @DisplayName("`GREATER_OR_EQUAL`")
        void ge() {
            assertGreaterOrEqual(42.1, 42.01);
        }

        @Test
        @DisplayName("`LESS_THAN`")
        void lt() {
            assertLess(42.81, 314.0);
        }

        @Test
        @DisplayName("`LESS_OR_EQUAL`")
        void le() {
            assertLessOrEqual(42.999, 43.0);
        }
    }

    @Nested
    @DisplayName("fail to compare unsupported types by")
    class NotCompareUnsupportedBy {

        @Test
        @DisplayName("`GREATER_THAN`")
        void gt() {
            assertThrows(IllegalArgumentException.class,
                         () -> eval(FaultyComparisonType.INSTANCE,
                                    GREATER_THAN,
                                    FaultyComparisonType.INSTANCE));
        }

        @Test
        @DisplayName("`GREATER_OR_EQUAL`")
        void ge() {
            assertThrows(IllegalArgumentException.class,
                         () -> eval(FaultyComparisonType.INSTANCE,
                                    GREATER_OR_EQUAL,
                                    FaultyComparisonType.INSTANCE));
        }

        @Test
        @DisplayName("`LESS_THAN`")
        void lt() {
            assertThrows(IllegalArgumentException.class,
                         () -> eval(FaultyComparisonType.INSTANCE,
                                    LESS_THAN,
                                    FaultyComparisonType.INSTANCE));
        }

        @Test
        @DisplayName("`LESS_OR_EQUAL`")
        void le() {
            assertThrows(IllegalArgumentException.class,
                         () -> eval(FaultyComparisonType.INSTANCE,
                                    LESS_OR_EQUAL,
                                    FaultyComparisonType.INSTANCE));
        }
    }

    @Test
    @DisplayName("fail to compare different types")
    void notCompareDifferentTypes() {
        assertThrows(IllegalArgumentException.class, () -> eval("7", GREATER_THAN, 6));
    }

    @Test
    @DisplayName("fail to compare by invalid operator")
    void notCompareByInvalidOperator() {
        assertThrows(IllegalArgumentException.class, () -> eval("a", CFO_UNDEFINED, "b"));
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
}
