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

package io.spine.testing.server.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.testing.client.blackbox.Count;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.asList;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @param <E>
 * @author Mykhailo Drachuk
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ClassReferencesSubclass")
@VisibleForTesting
abstract class AbstractVerify<E extends EmittedMessages> implements Verify<E> {

    static <E extends EmittedMessages> Verify<E> count(Count expectedCount) {
        return new MessageCount<>(expectedCount);
    }

    static <E extends EmittedMessages> Verify<E>
    countAndClass(Count expected, Class<? extends Message> messageClass) {
        return new CountAndClass<>(expected, messageClass);
    }

    /**
     * Verifies that there were emitted messages of each of the provided class.
     */
    @SafeVarargs
    static <E extends EmittedMessages>
    AbstractVerify<E> classes(Class<? extends Message> first, Class<? extends Message>... other) {
        return classes(asList(first, other));
    }

    /**
     * Verifies that there were emitted messages of each of the provided class.
     */
    static <E extends EmittedMessages>
    AbstractVerify<E> classes(List<Class<? extends Message>> messageClasses) {
        return new MessageClasses<>(messageClasses);
    }

    /**
     * Compares two integers returning a string stating if the first value is less, more or
     * same number as the second.
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")
    static String compare(int firstValue, int secondValue) {
        if (firstValue > secondValue) {
            return "more";
        }
        if (firstValue < secondValue) {
            return "less";
        }
        return "same number";
    }

    /**
     * Verifies the count of emitted messages.
     */
    private static class MessageCount<E extends EmittedMessages> extends AbstractVerify<E> {

        private final Count expected;

        MessageCount(Count expected) {
            super();
            this.expected = expected;
        }

        @Override
        public void verify(E emittedMessages) {
            int actualCount = emittedMessages.count();
            int expectedCountValue = expected();
            String moreOrLess = compare(actualCount, expectedCountValue);
            assertEquals(
                    expectedCountValue,
                    actualCount,
                    format("Bounded Context emitted %s %s than expected",
                           moreOrLess,
                           emittedMessages.plural())
            );
        }

        protected int expected() {
            return expected.value();
        }
    }

    /**
     * Verifies that all emitted messages contain at least one message of the each passed type.
     */
    private static class MessageClasses<E extends EmittedMessages> extends AbstractVerify<E> {

        private final List<Class<? extends Message>> messageClasses;

        private MessageClasses(List<Class<? extends Message>> classes) {
            super();
            checkNotNull(classes);
            checkArgument(
                    classes.size() > 0,
                    "At least one class must be provided for checking types of emitted messages."
            );
            this.messageClasses = ImmutableList.copyOf(classes);
        }

        @Override
        public void verify(E emittedMessages) {
            for (Class<? extends Message> messageClass : messageClasses) {
                String className = messageClass.getName();
                if (!emittedMessages.contain(messageClass)) {
                    fail(format("Bounded Context did not emit %s %s",
                                className,
                                emittedMessages.singular())
                    );
                }
            }
        }
    }

    private static class CountAndClass<E extends EmittedMessages> extends MessageCount<E> {

        private final Class<? extends Message> messageClass;

        private CountAndClass(Count expected, Class<? extends Message> cls) {
            super(expected);
            messageClass = cls;
        }

        @Override
        public void verify(E emittedMessages) {
            String className = messageClass.getName();
            int actualCount = emittedMessages.count(messageClass);
            int expectedCount = expected();
            String moreOrLess = AbstractVerify.compare(actualCount, expectedCount);
            assertEquals(expectedCount, actualCount,
                         format("Bounded Context emitted %s %s %s than expected",
                                moreOrLess, className, emittedMessages.plural()));
        }
    }
}
