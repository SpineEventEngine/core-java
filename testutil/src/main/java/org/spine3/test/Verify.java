/*
 * Copyright (c) 2015 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.spine3.test;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.junit.Assert;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.google.common.collect.Sets.newHashSet;

/**
 * An extension of the {@link Assert} class, which adds useful additional "assert" methods.
 * You can import this class instead of Assert, and use it thus, e.g.:
 * <pre>
 *     Verify.assertEquals("fred", name);  // from original Assert class
 *     Verify.assertContains("fred", nameList);  // from new extensions
 *     Verify.assertBefore("fred", "jim", orderedNamesList);  // from new extensions
 * </pre>
 *
 * Is based on
 * <a href="https://github.com/eclipse/eclipse-collections/blob/master/eclipse-collections-testutils/src/main/java/org/eclipse/collections/impl/test/Verify.java">
 * org.eclipse.collections.impl.test.Verify</a> class.
 *
 * @author Alexander Litus
 */
@SuppressWarnings({"ExtendsUtilityClass", "ClassWithTooManyMethods", "OverlyComplexClass", "ErrorNotRethrown", "DuplicateStringLiteralInspection", "unused"})
public final class Verify extends Assert {

    private static final String SHOULD_NOT_BE_EQUAL = " should not be equal:<";

    private static final String EXPECTED_ITEMS_IN_ASSERTION_MESSAGE = "Expected items in assertion";
    private static final String ITERABLE = "iterable";
    private static final String SHOULD_NOT_BE_EMPTY_MESSAGE = " should be non-empty, but was empty";

    @SuppressWarnings("AccessOfSystemProperties")
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");


    private Verify() {}

    /**
     * Mangles the stack trace of {@link AssertionError} so that it looks like its been thrown from the line that
     * called to a custom assertion.
     *
     * This method behaves identically to {@link #mangledException(AssertionError, int)} and is provided
     * for convenience for assert methods that only want to pop two stack frames. The only time that you would want to
     * call the other {@link #mangledException(AssertionError, int)} method is if you have a custom assert
     * that calls another custom assert i.e. the source line calling the custom asserts is more than two stack frames
     * away
     *
     * @param e The exception to mangle.
     * @see #mangledException(AssertionError, int)
     */
    public static AssertionError mangledException(AssertionError e) {
        /*
         * Note that we actually remove 3 frames from the stack trace because
         * we wrap the real method doing the work: e.fillInStackTrace() will
         * include us in the exceptions stack frame.
         */
        throw mangledException(e, 3);
    }

    /**
     * Mangles the stack trace of {@link AssertionError} so that it looks like
     * its been thrown from the line that called to a custom assertion.
     * <p>
     * This is useful for when you are in a debugging session and you want to go to the source
     * of the problem in the test case quickly. The regular use case for this would be something
     * along the lines of:
     * <pre>
     * public class TestFoo extends junit.framework.TestCase
     * {
     *   public void testFoo() throws Exception
     *   {
     *     Foo foo = new Foo();
     *     ...
     *     assertFoo(foo);
     *   }
     *
     *   // Custom assert
     *   private static void assertFoo(Foo foo)
     *   {
     *     try
     *     {
     *       assertEquals(...);
     *       ...
     *       assertSame(...);
     *     }
     *     catch (AssertionFailedException e)
     *     {
     *       AssertUtils.mangledException(e, 2);
     *     }
     *   }
     * }
     * </pre>
     * <p>
     * Without the {@code try ... catch} block around lines 11-13 the stack trace following a test failure
     * would look a little like:
     * <p>
     * <pre>
     * java.lang.AssertionError: ...
     *  at TestFoo.assertFoo(TestFoo.java:11)
     *  at TestFoo.testFoo(TestFoo.java:5)
     *  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     *  at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
     *  at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
     *  at java.lang.reflect.Method.invoke(Method.java:324)
     *  ...
     * </pre>
     * <p>
     * Note that the source of the error isn't readily apparent as the first line in the stack trace
     * is the code within the custom assert. If we were debugging the failure we would be more interested
     * in the second line of the stack trace which shows us where in our tests the assert failed.
     * <p>
     * With the {@code try ... catch} block around lines 11-13 the stack trace would look like the
     * following:
     * <p>
     * <pre>
     * java.lang.AssertionError: ...
     *  at TestFoo.testFoo(TestFoo.java:5)
     *  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     *  at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
     *  at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
     *  at java.lang.reflect.Method.invoke(Method.java:324)
     *  ...
     * </pre>
     * <p>
     * Here the source of the error is more visible as we can instantly see that the testFoo test is
     * failing at line 5.
     *
     * @param e           The exception to mangle.
     * @param framesToPop The number of frames to remove from the stack trace.
     * @throws AssertionError that was given as an argument with its stack trace mangled.
     */
    public static AssertionError mangledException(AssertionError e, int framesToPop) {
        e.fillInStackTrace();
        final StackTraceElement[] stackTrace = e.getStackTrace();
        final StackTraceElement[] newStackTrace = new StackTraceElement[stackTrace.length - framesToPop];
        System.arraycopy(stackTrace, framesToPop, newStackTrace, 0, newStackTrace.length);
        e.setStackTrace(newStackTrace);
        throw e;
    }

    public static void fail(String message, Throwable cause) {
        final AssertionError failedException = new AssertionError(message, cause);
        throw mangledException(failedException);
    }

    /**
     * Asserts that two floats are not equal concerning a delta. If the expected value is infinity then the delta value
     * is ignored.
     */
    public static void assertNotEquals(String itemName, float notExpected, float actual, float delta) {
        try {
            // handle infinity specially since subtracting to infinite values gives NaN and the
            // the following test fails
            //noinspection FloatingPointEquality
            if (Float.isInfinite(notExpected) && notExpected == actual || Math.abs(notExpected - actual) <= delta) {
                Assert.fail(itemName + SHOULD_NOT_BE_EQUAL + notExpected + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Asserts that two floats are not equal concerning a delta. If the expected value is infinity then the delta value
     * is ignored.
     */
    public static void assertNotEquals(float expected, float actual, float delta) {
        try {
            assertNotEquals("float", expected, actual, delta);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Asserts that two booleans are not equal.
     */
    public static void assertNotEquals(String itemName, boolean notExpected, boolean actual) {
        try {
            if (notExpected == actual) {
                Assert.fail(itemName + SHOULD_NOT_BE_EQUAL + notExpected + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Asserts that two booleans are not equal.
     */
    public static void assertNotEquals(boolean notExpected, boolean actual) {
        try {
            assertNotEquals("boolean", notExpected, actual);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Asserts that two bytes are not equal.
     */
    public static void assertNotEquals(String itemName, byte notExpected, byte actual) {
        try {
            if (notExpected == actual) {
                Assert.fail(itemName + SHOULD_NOT_BE_EQUAL + notExpected + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Asserts that two bytes are not equal.
     */
    public static void assertNotEquals(byte notExpected, byte actual) {
        try {
            assertNotEquals("byte", notExpected, actual);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Asserts that two chars are not equal.
     */
    public static void assertNotEquals(String itemName, char notExpected, char actual) {
        try {
            if (notExpected == actual) {
                Assert.fail(itemName + SHOULD_NOT_BE_EQUAL + notExpected + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Asserts that two chars are not equal.
     */
    public static void assertNotEquals(char notExpected, char actual) {
        try {
            assertNotEquals("char", notExpected, actual);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Asserts that two shorts are not equal.
     */
    public static void assertNotEquals(String itemName, short notExpected, short actual) {
        try {
            if (notExpected == actual) {
                Assert.fail(itemName + SHOULD_NOT_BE_EQUAL + notExpected + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Asserts that two shorts are not equal.
     */
    public static void assertNotEquals(short notExpected, short actual) {
        try {
            assertNotEquals("short", notExpected, actual);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Iterable} is empty.
     */
    public static void assertIterableEmpty(Iterable<?> iterable) {
        try {
            assertIterableEmpty(ITERABLE, iterable);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Iterable} is empty.
     */
    public static void assertIterableEmpty(String iterableName, Iterable<?> iterable) {
        try {
            assertObjectNotNull(iterableName, iterable);

            final FluentIterable<?> fluentIterable = FluentIterable.from(iterable);
            if (!fluentIterable.isEmpty()) {
                Assert.fail(iterableName + " must be empty; actual size:<" + fluentIterable.size() + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given object is an instanceof expectedClassType.
     */
    public static void assertInstanceOf(Class<?> expectedClassType, Object actualObject) {
        try {
            assertInstanceOf(actualObject.getClass()
                                         .getName(), expectedClassType, actualObject);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given object is an instanceof expectedClassType.
     */
    public static void assertInstanceOf(String objectName, Class<?> expectedClassType, Object actualObject) {
        try {
            if (!expectedClassType.isInstance(actualObject)) {
                Assert.fail(objectName + " is not an instance of " + expectedClassType.getName());
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given object is not an instanceof expectedClassType.
     */
    public static void assertNotInstanceOf(Class<?> expectedClassType, Object actualObject) {
        try {
            assertNotInstanceOf(actualObject.getClass()
                                            .getName(), expectedClassType, actualObject);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given object is not an instanceof expectedClassType.
     */
    public static void assertNotInstanceOf(String objectName, Class<?> expectedClassType, Object actualObject) {
        try {
            if (expectedClassType.isInstance(actualObject)) {
                Assert.fail(objectName + " is an instance of " + expectedClassType.getName());
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Map} is empty.
     */
    public static void assertEmpty(Map<?, ?> actualMap) {
        try {
            assertEmpty("map", actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Multimap} is empty.
     */
    public static void assertEmpty(Multimap<?, ?> actualMultimap) {
        try {
            assertEmpty("multimap", actualMultimap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Multimap} is empty.
     */
    public static void assertEmpty(String multimapName, Multimap<?, ?> actualMultimap) {
        try {
            assertObjectNotNull(multimapName, actualMultimap);

            if (!actualMultimap.isEmpty()) {
                Assert.fail(multimapName + " should be empty; actual size:<" + actualMultimap.size() + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Map} is empty.
     */
    @SuppressWarnings("MethodWithMoreThanThreeNegations")
    public static void assertEmpty(String mapName, Map<?, ?> actualMap) {
        try {
            assertObjectNotNull(mapName, actualMap);

            final String errorMessage = " should be empty; actual size:<";
            if (!actualMap.isEmpty()) {
                Assert.fail(mapName + errorMessage + actualMap.size() + '>');
            }
            if (actualMap.size() != 0) {
                Assert.fail(mapName + errorMessage + actualMap.size() + '>');
            }
            if (actualMap.keySet()
                         .size() != 0) {
                Assert.fail(mapName + errorMessage + actualMap.keySet()
                                                              .size() + '>');
            }
            if (actualMap.values()
                         .size() != 0) {
                Assert.fail(mapName + errorMessage + actualMap.values()
                                                              .size() + '>');
            }
            if (actualMap.entrySet()
                         .size() != 0) {
                Assert.fail(mapName + errorMessage + actualMap.entrySet()
                                                              .size() + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Iterable} is <em>not</em> empty.
     */
    public static void assertNotEmpty(Iterable<?> actualIterable) {
        try {
            assertNotEmpty(ITERABLE, actualIterable);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Iterable} is <em>not</em> empty.
     */
    public static void assertNotEmpty(String iterableName, Iterable<?> actualIterable) {
        try {
            assertObjectNotNull(iterableName, actualIterable);
            final FluentIterable<?> fluentIterable = FluentIterable.from(actualIterable);
            Assert.assertFalse(iterableName + SHOULD_NOT_BE_EMPTY_MESSAGE, fluentIterable.isEmpty());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Iterable} is <em>not</em> empty.
     */
    public static void assertIterableNotEmpty(Iterable<?> iterable) {
        try {
            assertNotEmpty(ITERABLE, iterable);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Map} is <em>not</em> empty.
     */
    public static void assertNotEmpty(Map<?, ?> actualMap) {
        try {
            assertNotEmpty("map", actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Map} is <em>not</em> empty.
     */
    public static void assertNotEmpty(String mapName, Map<?, ?> actualMap) {
        try {
            assertObjectNotNull(mapName, actualMap);
            Assert.assertFalse(mapName + SHOULD_NOT_BE_EMPTY_MESSAGE, actualMap.isEmpty());
            Assert.assertNotEquals(mapName + SHOULD_NOT_BE_EMPTY_MESSAGE, 0, actualMap.size());
            Assert.assertNotEquals(mapName + SHOULD_NOT_BE_EMPTY_MESSAGE, 0, actualMap.keySet()
                                                                                      .size());
            Assert.assertNotEquals(mapName + SHOULD_NOT_BE_EMPTY_MESSAGE, 0, actualMap.values()
                                                                                      .size());
            Assert.assertNotEquals(mapName + SHOULD_NOT_BE_EMPTY_MESSAGE, 0, actualMap.entrySet()
                                                                                      .size());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Multimap} is <em>not</em> empty.
     */
    public static void assertNotEmpty(Multimap<?, ?> actualMultimap) {
        try {
            assertNotEmpty("multimap", actualMultimap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Multimap} is <em>not</em> empty.
     */
    public static void assertNotEmpty(String multimapName, Multimap<?, ?> actualMultimap) {
        try {
            assertObjectNotNull(multimapName, actualMultimap);
            Assert.assertFalse(multimapName + SHOULD_NOT_BE_EMPTY_MESSAGE, actualMultimap.isEmpty());
            Assert.assertNotEquals(multimapName + SHOULD_NOT_BE_EMPTY_MESSAGE, 0, actualMultimap.size());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static <T> void assertNotEmpty(String itemsName, T[] items) {
        try {
            assertObjectNotNull(itemsName, items);
            Assert.assertNotEquals(itemsName, 0, items.length);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static <T> void assertNotEmpty(T[] items) {
        try {
            assertNotEmpty("items", items);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert the size of the given array.
     */
    public static void assertSize(int expectedSize, Object[] actualArray) {
        try {
            assertSize("array", expectedSize, actualArray);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert the size of the given array.
     */
    public static void assertSize(String arrayName, int expectedSize, Object[] actualArray) {
        try {
            Assert.assertNotNull(arrayName + " should not be null", actualArray);

            final int actualSize = actualArray.length;
            if (actualSize != expectedSize) {
                Assert.fail("Incorrect size for "
                        + arrayName
                        + "; expected:<"
                        + expectedSize
                        + "> but was:<"
                        + actualSize
                        + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert the size of the given {@link Iterable}.
     */
    public static void assertSize(int expectedSize, Iterable<?> actualIterable) {
        try {
            assertSize(ITERABLE, expectedSize, actualIterable);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert the size of the given {@link Iterable}.
     */
    public static void assertSize(
            String iterableName,
            int expectedSize,
            Iterable<?> actualIterable) {
        try {
            assertObjectNotNull(iterableName, actualIterable);

            final FluentIterable<?> fluentIterable = FluentIterable.from(actualIterable);
            final int actualSize = fluentIterable.size();
            if (actualSize != expectedSize) {
                Assert.fail("Incorrect size for "
                        + iterableName
                        + "; expected:<"
                        + expectedSize
                        + "> but was:<"
                        + actualSize
                        + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert the size of the given {@link Iterable}.
     */
    public static void assertIterableSize(int expectedSize, Iterable<?> actualIterable) {
        try {
            assertIterableSize(ITERABLE, expectedSize, actualIterable);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert the size of the given {@link Iterable}.
     */
    public static void assertIterableSize(
            String iterableName,
            int expectedSize,
            Iterable<?> actualIterable) {
        try {
            assertObjectNotNull(iterableName, actualIterable);

            final FluentIterable<?> fluentIterable = FluentIterable.from(actualIterable);
            final int actualSize = fluentIterable.size();
            if (actualSize != expectedSize) {
                Assert.fail("Incorrect size for "
                        + iterableName
                        + "; expected:<"
                        + expectedSize
                        + "> but was:<"
                        + actualSize
                        + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert the size of the given {@link Map}.
     */
    public static void assertSize(String mapName, int expectedSize, Map<?, ?> actualMap) {
        try {
            assertSize(mapName, expectedSize, actualMap.keySet());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert the size of the given {@link Map}.
     */
    public static void assertSize(int expectedSize, Map<?, ?> actualMap) {
        try {
            assertSize("map", expectedSize, actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert the size of the given {@link Multimap}.
     */
    public static void assertSize(int expectedSize, Multimap<?, ?> actualMultimap) {
        try {
            assertSize("multimap", expectedSize, actualMultimap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert the size of the given {@link Multimap}.
     */
    public static void assertSize(String multimapName, int expectedSize, Multimap<?, ?> actualMultimap) {
        try {
            final int actualSize = actualMultimap.size();
            if (actualSize != expectedSize) {
                Assert.fail("Incorrect size for "
                        + multimapName
                        + "; expected:<"
                        + expectedSize
                        + "> but was:<"
                        + actualSize
                        + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert the size of the given {@link ImmutableSet}.
     */
    public static void assertSize(int expectedSize, Collection<?> actualImmutableSet) {
        try {
            assertSize("immutable set", expectedSize, actualImmutableSet);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert the size of the given {@link ImmutableSet}.
     */
    public static void assertSize(String immutableSetName, int expectedSize, Collection<?> actualImmutableSet) {
        try {
            final int actualSize = actualImmutableSet.size();
            if (actualSize != expectedSize) {
                Assert.fail("Incorrect size for "
                        + immutableSetName
                        + "; expected:<"
                        + expectedSize
                        + "> but was:<"
                        + actualSize
                        + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@code stringToFind} is contained within the {@code stringToSearch}.
     */
    public static void assertContains(CharSequence stringToFind, String stringToSearch) {
        try {
            assertContains("string", stringToFind, stringToSearch);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@code unexpectedString} is <em>not</em> contained within the {@code stringToSearch}.
     */
    public static void assertNotContains(CharSequence unexpectedString, String stringToSearch) {
        try {
            assertNotContains("string", unexpectedString, stringToSearch);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@code stringToFind} is contained within the {@code stringToSearch}.
     */
    public static void assertContains(String stringName, CharSequence stringToFind, String stringToSearch) {
        try {
            Assert.assertNotNull("stringToFind should not be null", stringToFind);
            Assert.assertNotNull("stringToSearch should not be null", stringToSearch);

            if (!stringToSearch.contains(stringToFind)) {
                Assert.fail(stringName
                        + " did not contain stringToFind:<"
                        + stringToFind
                        + "> in stringToSearch:<"
                        + stringToSearch
                        + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@code unexpectedString} is <em>not</em> contained within the {@code stringToSearch}.
     */
    public static void assertNotContains(String stringName, CharSequence unexpectedString, String stringToSearch) {
        try {
            Assert.assertNotNull("unexpectedString should not be null", unexpectedString);
            Assert.assertNotNull("stringToSearch should not be null", stringToSearch);

            if (stringToSearch.contains(unexpectedString)) {
                Assert.fail(stringName
                        + " contains unexpectedString:<"
                        + unexpectedString
                        + "> in stringToSearch:<"
                        + stringToSearch
                        + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Collection} contains the given item.
     */
    public static void assertContains(Object expectedItem, Collection<?> actualCollection) {
        try {
            assertContains("collection", expectedItem, actualCollection);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Collection} contains the given item.
     */
    public static void assertContains(
            String collectionName,
            Object expectedItem,
            Collection<?> actualCollection) {
        try {
            assertObjectNotNull(collectionName, actualCollection);

            if (!actualCollection.contains(expectedItem)) {
                Assert.fail(collectionName + " did not contain expectedItem:<" + expectedItem + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link ImmutableCollection} contains the given item.
     */
    public static void assertContains(Object expectedItem, ImmutableCollection<?> actualCollection) {
        try {
            assertContains("ImmutableCollection", expectedItem, actualCollection);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link ImmutableCollection} contains the given item.
     */
    public static void assertContains(
            String collectionName,
            Object expectedItem,
            ImmutableCollection<?> actualCollection) {
        try {
            assertObjectNotNull(collectionName, actualCollection);

            if (!actualCollection.contains(expectedItem)) {
                Assert.fail(collectionName + " did not contain expectedItem:<" + expectedItem + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    @SuppressWarnings("OverloadedVarargsMethod")
    @SafeVarargs
    public static<T> void assertContainsAll(
            Iterable<T> iterable,
            T... items) {
        try {
            assertObjectNotNull("Collection", iterable);

            assertNotEmpty(EXPECTED_ITEMS_IN_ASSERTION_MESSAGE, items);
            final FluentIterable<?> fluentIterable = FluentIterable.from(iterable);

            for (Object item : items) {
                Assert.assertTrue(fluentIterable.contains(item));
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static<K, V> void assertMapsEqual(Map<K, V> expectedMap, Map<K, V> actualMap, String actualMapName) {
        try {
            //noinspection ConstantConditions
            if (expectedMap == null) {
                Assert.assertNull(actualMapName + " should be null", actualMap);
                return;
            }

            Assert.assertNotNull(actualMapName + " should not be null", actualMap);

            final Set<? extends Map.Entry<K, V>> expectedEntries = expectedMap.entrySet();
            for (Map.Entry<K, V> expectedEntry : expectedEntries) {
                final K expectedKey = expectedEntry.getKey();
                final V expectedValue = expectedEntry.getValue();
                final V actualValue = actualMap.get(expectedKey);
                if (!Objects.equals(actualValue, expectedValue)) {
                    Assert.fail("Values differ at key " + expectedKey + " expected " + expectedValue + " but was " + actualValue);
                }
            }
            assertSetsEqual(expectedMap.keySet(), actualMap.keySet());
            assertSetsEqual(expectedMap.entrySet(), actualMap.entrySet());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static<T> void assertSetsEqual(Collection<T> expectedSet, Collection<T> actualSet) {
        try {
            //noinspection ConstantConditions
            if (expectedSet == null) {
                Assert.assertNull("Actual set should be null", actualSet);
                return;
            }

            assertObjectNotNull("actual set", actualSet);
            assertSize(expectedSet.size(), actualSet);

            if (!actualSet.equals(expectedSet)) {
                final Set<T> inExpectedOnlySet = newHashSet(expectedSet);
                inExpectedOnlySet.removeAll(actualSet);

                final int numberDifferences = inExpectedOnlySet.size();

                final int maxDifferences = 5;
                if (numberDifferences > maxDifferences) {
                    Assert.fail("Actual set: " + numberDifferences + " elements different.");
                }

                final Set<T> inActualOnlySet = newHashSet(actualSet);
                inActualOnlySet.removeAll(expectedSet);

                Assert.fail("Sets are not equal.");
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Multimap} contains an entry with the given key and value.
     */
    public static <K, V> void assertContainsEntry(
            K expectedKey,
            V expectedValue,
            Multimap<K, V> actualMultimap) {
        try {
            assertContainsEntry("multimap", expectedKey, expectedValue, actualMultimap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Multimap} contains an entry with the given key and value.
     */
    public static <K, V> void assertContainsEntry(
            String multimapName,
            K expectedKey,
            V expectedValue,
            Multimap<K, V> actualMultimap) {
        try {
            Assert.assertNotNull(multimapName, actualMultimap);

            if (!actualMultimap.containsEntry(expectedKey, expectedValue)) {
                Assert.fail(multimapName + " did not contain entry: <" + expectedKey + ", " + expectedValue + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Map} contains an entry with the given key.
     */
    public static void assertContainsKey(Object expectedKey, Map<?, ?> actualMap) {
        try {
            assertContainsKey("map", expectedKey, actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Map} contains an entry with the given key.
     */
    public static void assertContainsKey(String mapName, Object expectedKey, Map<?, ?> actualMap) {
        try {
            Assert.assertNotNull(mapName, actualMap);

            if (!actualMap.containsKey(expectedKey)) {
                Assert.fail(mapName + " did not contain expectedKey:<" + expectedKey + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Deny that the given {@link Map} contains an entry with the given key.
     */
    public static void denyContainsKey(Object unexpectedKey, Map<?, ?> actualMap) {
        try {
            denyContainsKey("map", unexpectedKey, actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Deny that the given {@link Map} contains an entry with the given key.
     */
    public static void denyContainsKey(String mapName, Object unexpectedKey, Map<?, ?> actualMap) {
        try {
            Assert.assertNotNull(mapName, actualMap);

            if (actualMap.containsKey(unexpectedKey)) {
                Assert.fail(mapName + " contained unexpectedKey:<" + unexpectedKey + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Map} contains an entry with the given key and value.
     */
    public static void assertContainsKeyValue(
            Object expectedKey,
            Object expectedValue,
            Map<?, ?> actualMap) {
        try {
            assertContainsKeyValue("map", expectedKey, expectedValue, actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Map} contains an entry with the given key and value.
     */
    public static void assertContainsKeyValue(
            String mapName,
            Object expectedKey,
            Object expectedValue,
            Map<?, ?> actualMap) {
        try {
            assertContainsKey(mapName, expectedKey, actualMap);

            final Object actualValue = actualMap.get(expectedKey);
            if (!Objects.equals(actualValue, expectedValue)) {
                Assert.fail(
                        mapName
                                + " entry with expectedKey:<"
                                + expectedKey
                                + "> "
                                + "did not contain expectedValue:<"
                                + expectedValue
                                + ">, "
                                + "but had actualValue:<"
                                + actualValue
                                + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Collection} does <em>not</em> contain the given item.
     */
    public static void assertNotContains(Object unexpectedItem, Collection<?> actualCollection) {
        try {
            assertNotContains("collection", unexpectedItem, actualCollection);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Collection} does <em>not</em> contain the given item.
     */
    public static void assertNotContains(
            String collectionName,
            Object unexpectedItem,
            Collection<?> actualCollection) {
        try {
            assertObjectNotNull(collectionName, actualCollection);

            if (actualCollection.contains(unexpectedItem)) {
                Assert.fail(collectionName + " should not contain unexpectedItem:<" + unexpectedItem + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Iterable} does <em>not</em> contain the given item.
     */
    public static void assertNotContains(Object unexpectedItem, Iterable<?> iterable) {
        try {
            assertNotContains(ITERABLE, unexpectedItem, iterable);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Iterable} does <em>not</em> contain the given item.
     */
    public static void assertNotContains(
            String collectionName,
            Object unexpectedItem,
            Iterable<?> iterable) {
        try {
            assertObjectNotNull(collectionName, iterable);

            final FluentIterable<?> fluentIterable = FluentIterable.from(iterable);
            if (fluentIterable.contains(unexpectedItem)) {
                Assert.fail(collectionName + " should not contain unexpectedItem:<" + unexpectedItem + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Collection} does <em>not</em> contain the given item.
     */
    public static void assertNotContainsKey(Object unexpectedKey, Map<?, ?> actualMap) {
        try {
            assertNotContainsKey("map", unexpectedKey, actualMap);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@link Collection} does <em>not</em> contain the given item.
     */
    public static void assertNotContainsKey(String mapName, Object unexpectedKey, Map<?, ?> actualMap) {
        try {
            assertObjectNotNull(mapName, actualMap);

            if (actualMap.containsKey(unexpectedKey)) {
                Assert.fail(mapName + " should not contain unexpectedItem:<" + unexpectedKey + '>');
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the formerItem appears before the latterItem in the given {@link Collection}.
     * Both the formerItem and the latterItem must appear in the collection, or this assert will fail.
     */
    public static<T> void assertBefore(T formerItem, T latterItem, List<T> actualList) {
        try {
            assertBefore("list", formerItem, latterItem, actualList);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the formerItem appears before the latterItem in the given {@link Collection}.
     * {@link #assertContains(String, Object, Collection)} will be called for both the formerItem and the
     * latterItem, prior to the "before" assertion.
     */
    public static<T> void assertBefore(
            String listName,
            T formerItem,
            T latterItem,
            List<T> actualList) {
        try {
            assertObjectNotNull(listName, actualList);
            Assert.assertNotEquals(
                    "Bad test, formerItem and latterItem are equal, listName:<" + listName + '>',
                    formerItem,
                    latterItem);
            assertContainsAll(actualList, formerItem, latterItem);
            final int formerPosition = actualList.indexOf(formerItem);
            final int latterPosition = actualList.indexOf(latterItem);
            if (latterPosition < formerPosition) {
                Assert.fail("Items in "
                        + listName
                        + " are in incorrect order; "
                        + "expected formerItem:<"
                        + formerItem
                        + "> "
                        + "to appear before latterItem:<"
                        + latterItem
                        + ">, but didn't");
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static void assertObjectNotNull(String objectName, Object actualObject) {
        try {
            Assert.assertNotNull(objectName + " should not be null", actualObject);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@code item} is at the {@code index} in the given {@link List}.
     */
    public static void assertItemAtIndex(Object expectedItem, int index, List<?> list) {
        try {
            assertItemAtIndex("list", expectedItem, index, list);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@code item} is at the {@code index} in the given {@code array}.
     */
    public static void assertItemAtIndex(Object expectedItem, int index, Object[] array) {
        try {
            assertItemAtIndex("array", expectedItem, index, array);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    @SafeVarargs
    @SuppressWarnings("OverloadedVarargsMethod")
    public static <T> void assertStartsWith(T[] array, T... items) {
        try {
            assertNotEmpty(EXPECTED_ITEMS_IN_ASSERTION_MESSAGE, items);

            for (int i = 0; i < items.length; i++) {
                final T item = items[i];
                assertItemAtIndex("array", item, i, array);
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    @SuppressWarnings("OverloadedVarargsMethod")
    @SafeVarargs
    public static <T> void assertStartsWith(List<T> list, T... items) {
        try {
            assertStartsWith("list", list, items);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    @SafeVarargs
    @SuppressWarnings("OverloadedVarargsMethod")
    public static <T> void assertStartsWith(String listName, List<T> list, T... items) {
        try {
            assertNotEmpty(EXPECTED_ITEMS_IN_ASSERTION_MESSAGE, items);

            for (int i = 0; i < items.length; i++) {
                final T item = items[i];
                assertItemAtIndex(listName, item, i, list);
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    @SafeVarargs
    @SuppressWarnings("OverloadedVarargsMethod")
    public static <T> void assertEndsWith(List<T> list, T... items) {
        try {
            assertNotEmpty(EXPECTED_ITEMS_IN_ASSERTION_MESSAGE, items);

            for (int i = 0; i < items.length; i++) {
                final T item = items[i];
                assertItemAtIndex("list", item, list.size() - items.length + i, list);
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    @SafeVarargs
    @SuppressWarnings("OverloadedVarargsMethod")
    public static <T> void assertEndsWith(T[] array, T... items) {
        try {
            assertNotEmpty(EXPECTED_ITEMS_IN_ASSERTION_MESSAGE, items);

            for (int i = 0; i < items.length; i++) {
                final T item = items[i];
                assertItemAtIndex("array", item, array.length - items.length + i, array);
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@code item} is at the {@code index} in the given {@link List}.
     */
    public static void assertItemAtIndex(
            String listName,
            Object expectedItem,
            int index,
            List<?> list) {
        try {
            assertObjectNotNull(listName, list);

            final Object actualItem = list.get(index);
            if (!Objects.equals(expectedItem, actualItem)) {
                Assert.assertEquals(
                        listName + " has incorrect element at index:<" + index + '>',
                        expectedItem,
                        actualItem);
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that the given {@code item} is at the {@code index} in the given {@link List}.
     */
    public static void assertItemAtIndex(
            String arrayName,
            Object expectedItem,
            int index,
            Object[] array) {
        try {
            Assert.assertNotNull(array);
            final Object actualItem = array[index];
            if (!Objects.equals(expectedItem, actualItem)) {
                Assert.assertEquals(
                        arrayName + " has incorrect element at index:<" + index + '>',
                        expectedItem,
                        actualItem);
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that {@code objectA} and {@code objectB} are equal (via the {@link Object#equals(Object)} method,
     * and that they both return the same {@link Object#hashCode()}.
     */
    public static void assertEqualsAndHashCode(Object objectA, Object objectB) {
        try {
            assertEqualsAndHashCode("objects", objectA, objectB);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Asserts that a value is negative.
     */
    public static void assertNegative(int value) {
        try {
            Assert.assertTrue(value + " is not negative", value < 0);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Asserts that a value is positive.
     */
    public static void assertPositive(int value) {
        try {
            Assert.assertTrue(value + " is not positive", value > 0);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Asserts that a value is positive.
     */
    public static void assertZero(int value) {
        try {
            Assert.assertEquals(0, value);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Assert that {@code objectA} and {@code objectB} are equal (via the {@link Object#equals(Object)} method,
     * and that they both return the same {@link Object#hashCode()}.
     */
    public static void assertEqualsAndHashCode(String itemNames, Object objectA, Object objectB) {
        try {
            //noinspection ConstantConditions
            if (objectA == null || objectB == null) {
                Assert.fail("Neither item should be null: <" + objectA + "> <" + objectB + '>');
            }

            Assert.assertNotEquals("Neither item should equal new Object()", objectA.equals(new Object()));
            Assert.assertNotEquals("Neither item should equal new Object()", objectB.equals(new Object()));
            Assert.assertEquals("Expected " + itemNames + " to be equal.", objectA, objectA);
            Assert.assertEquals("Expected " + itemNames + " to be equal.", objectB, objectB);
            Assert.assertEquals("Expected " + itemNames + " to be equal.", objectA, objectB);
            Assert.assertEquals("Expected " + itemNames + " to be equal.", objectB, objectA);
            Assert.assertEquals(
                    "Expected " + itemNames + " to have the same hashCode().",
                    objectA.hashCode(),
                    objectB.hashCode());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static void assertShallowClone(Cloneable object) {
        try {
            assertShallowClone("object", object);
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    public static void assertShallowClone(String itemName, Cloneable object) {
        try {
            //noinspection ConfusingArgumentToVarargsMethod
            final Method method = Object.class.getDeclaredMethod("clone", (Class<?>[]) null);
            method.setAccessible(true);
            final Object clone = method.invoke(object);
            final String prefix = itemName + " and its clone";
            Assert.assertNotSame(prefix, object, clone);
            assertEqualsAndHashCode(prefix, object, clone);
        } catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException | IllegalAccessException |
                AssertionError | SecurityException e) {
            throw new AssertionError(e.getLocalizedMessage(), e);
        }
    }

    public static <T> void assertClassNonInstantiable(Class<T> aClass) {
        try {
            try {
                //noinspection ClassNewInstance
                aClass.newInstance();
                Assert.fail("Expected class '" + aClass + "' to be non-instantiable");
            } catch (InstantiationException e) {
                // pass
            } catch (IllegalAccessException ignored) {
                if (canInstantiateThroughReflection(aClass)) {
                    Assert.fail("Expected constructor of non-instantiable class '" + aClass + "' to throw an exception, but didn't");
                }
            }
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    private static <T> boolean canInstantiateThroughReflection(Class<T> aClass) {
        try {
            final Constructor<T> declaredConstructor = aClass.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            declaredConstructor.newInstance();
            return true;
        } catch (NoSuchMethodException |
                InvocationTargetException |
                InstantiationException |
                IllegalAccessException |
                AssertionError ignored) {
            return false;
        }
    }

    public static void assertError(Class<? extends Error> expectedErrorClass, Runnable code) {
        try {
            code.run();
        } catch (Error ex) {
            try {
                Assert.assertSame(
                        "Caught error of type <"
                                + ex.getClass()
                                    .getName()
                                + ">, expected one of type <"
                                + expectedErrorClass.getName()
                                + '>',
                        expectedErrorClass,
                        ex.getClass());
                return;
            } catch (AssertionError e) {
                throw mangledException(e);
            }
        }

        try {
            Assert.fail("Block did not throw an error of type " + expectedErrorClass.getName());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Runs the {@link Callable} {@code code} and asserts that it throws an {@code Exception} of the type
     * {@code exceptionClass}.
     * <p>
     * {@code Callable} is most appropriate when a checked exception will be thrown.
     * If a subclass of {@link RuntimeException} will be thrown, the form
     * {@link #assertThrows(Class, Runnable)} may be more convenient.
     * <p>
     * e.g.
     * <pre>
     * Verify.<b>assertThrows</b>(StringIndexOutOfBoundsException.class, new Callable&lt;String&gt;()
     * {
     *    public String call() throws Exception
     *    {
     *        return "Craig".substring(42, 3);
     *    }
     * });
     * </pre>
     *
     * @see #assertThrows(Class, Runnable)
     */
    public static void assertThrows(
            Class<? extends Exception> exceptionClass,
            Callable<?> code) {
        try {
            code.call();
        } catch (Exception ex) {
            try {

                Assert.assertSame(
                        "Caught exception of type <"
                                + ex.getClass()
                                    .getName()
                                + ">, expected one of type <"
                                + exceptionClass.getName()
                                + '>'
                                + LINE_SEPARATOR
                                + "Exception Message: " + ex.getMessage()
                                + LINE_SEPARATOR,
                        exceptionClass,
                        ex.getClass());
                return;
            } catch (AssertionError e) {
                throw mangledException(e);
            }
        }

        try {
            Assert.fail("Block did not throw an exception of type " + exceptionClass.getName());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Runs the {@link Runnable} {@code code} and asserts that it throws an {@code Exception} of the type
     * {@code expectedClass}.
     * <p>
     * {@code Runnable} is most appropriate when a subclass of {@link RuntimeException} will be thrown.
     * If a checked exception will be thrown, the form {@link #assertThrows(Class, Callable)} may be more
     * convenient.
     * <p>
     * e.g.
     * <pre>
     * Verify.<b>assertThrows</b>(NullPointerException.class, new Runnable()
     * {
     *    public void run()
     *    {
     *        final Integer integer = null;
     *        LOGGER.info(integer.toString());
     *    }
     * });
     * </pre>
     *
     * @see #assertThrows(Class, Callable)
     */
    public static void assertThrows(
            Class<? extends Exception> expectedClass,
            Runnable code) {
        try {
            code.run();
        } catch (RuntimeException ex) {
            try {
                Assert.assertSame(
                        "Caught exception of type <"
                                + ex.getClass()
                                    .getName()
                                + ">, expected one of type <"
                                + expectedClass.getName()
                                + '>'
                                + LINE_SEPARATOR
                                + "Exception Message: " + ex.getMessage()
                                + LINE_SEPARATOR,
                        expectedClass,
                        ex.getClass());
                return;
            } catch (AssertionError e) {
                throw mangledException(e);
            }
        }

        try {
            Assert.fail("Block did not throw an exception of type " + expectedClass.getName());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Runs the {@link Callable} {@code code} and asserts that it throws an {@code Exception} of the type
     * {@code exceptionClass}, which contains a cause of type expectedCauseClass.
     * <p>
     * {@code Callable} is most appropriate when a checked exception will be thrown.
     * If a subclass of {@link RuntimeException} will be thrown, the form
     * {@link #assertThrowsWithCause(Class, Class, Runnable)} may be more convenient.
     * <p>
     * e.g.
     * <pre>
     * Verify.assertThrowsWithCause(RuntimeException.class, IOException.class, new Callable<Void>()
     * {
     *    public Void call() throws Exception
     *    {
     *        try
     *        {
     *            new File("").createNewFile();
     *        }
     *        catch (final IOException e)
     *        {
     *            throw new RuntimeException("Uh oh!", e);
     *        }
     *        return null;
     *    }
     * });
     * </pre>
     *
     * @see #assertThrowsWithCause(Class, Class, Runnable)
     */
    public static void assertThrowsWithCause(
            Class<? extends Exception> exceptionClass,
            Class<? extends Throwable> expectedCauseClass,
            Callable<?> code) {
        try {
            code.call();
        } catch (Exception ex) {
            try {
                Assert.assertSame(
                        "Caught exception of type <"
                                + ex.getClass()
                                    .getName()
                                + ">, expected one of type <"
                                + exceptionClass.getName()
                                + '>',
                        exceptionClass,
                        ex.getClass());
                final Throwable actualCauseClass = ex.getCause();
                Assert.assertNotNull(
                        "Caught exception with null cause, expected cause of type <"
                                + expectedCauseClass.getName()
                                + '>',
                        actualCauseClass);
                Assert.assertSame(
                        "Caught exception with cause of type<"
                                + actualCauseClass.getClass()
                                                  .getName()
                                + ">, expected cause of type <"
                                + expectedCauseClass.getName()
                                + '>',
                        expectedCauseClass,
                        actualCauseClass.getClass());
                return;
            } catch (AssertionError e) {
                throw mangledException(e);
            }
        }

        try {
            Assert.fail("Block did not throw an exception of type " + exceptionClass.getName());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }

    /**
     * Runs the {@link Runnable} {@code code} and asserts that it throws an {@code Exception} of the type
     * {@code exceptionClass}, which contains a cause of type expectedCauseClass.
     * <p>
     * {@code Runnable} is most appropriate when a subclass of {@link RuntimeException} will be thrown.
     * If a checked exception will be thrown, the form {@link #assertThrowsWithCause(Class, Class, Callable)}
     * may be more convenient.
     * <p>
     * e.g.
     * <pre>
     * Verify.assertThrowsWithCause(RuntimeException.class, StringIndexOutOfBoundsException.class, new Runnable()
     * {
     *    public void run()
     *    {
     *        try
     *        {
     *            LOGGER.info("Craig".substring(42, 3));
     *        }
     *        catch (final StringIndexOutOfBoundsException e)
     *        {
     *            throw new RuntimeException("Uh oh!", e);
     *        }
     *    }
     * });
     * </pre>
     *
     * @see #assertThrowsWithCause(Class, Class, Callable)
     */
    public static void assertThrowsWithCause(
            Class<? extends Exception> exceptionClass,
            Class<? extends Throwable> expectedCauseClass,
            Runnable code) {
        try {
            code.run();
        } catch (RuntimeException ex) {
            try {
                Assert.assertSame(
                        "Caught exception of type <"
                                + ex.getClass()
                                    .getName()
                                + ">, expected one of type <"
                                + exceptionClass.getName()
                                + '>',
                        exceptionClass,
                        ex.getClass());
                final Throwable actualCauseClass = ex.getCause();
                Assert.assertNotNull(
                        "Caught exception with null cause, expected cause of type <"
                                + expectedCauseClass.getName()
                                + '>',
                        actualCauseClass);
                Assert.assertSame(
                        "Caught exception with cause of type<"
                                + actualCauseClass.getClass()
                                                  .getName()
                                + ">, expected cause of type <"
                                + expectedCauseClass.getName()
                                + '>',
                        expectedCauseClass,
                        actualCauseClass.getClass());
                return;
            } catch (AssertionError e) {
                throw mangledException(e);
            }
        }

        try {
            Assert.fail("Block did not throw an exception of type " + exceptionClass.getName());
        } catch (AssertionError e) {
            throw mangledException(e);
        }
    }
}
