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
package org.spine3.server.reflect;

import com.google.common.testing.NullPointerTester;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.base.Subscribe;
import org.spine3.test.failure.command.UpdateProjectName;
import org.spine3.test.reflect.ReflectFailures;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings("unused")     // as testing the method declarations, not implementation details.
public class FailureSubscriberMethodShould {

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(CommandContext.class, CommandContext.getDefaultInstance())
                .testAllPublicStaticMethods(FailureSubscriberMethod.class);
    }

    @Test
    public void invoke_subscriber_method() throws InvocationTargetException {
        final ValidSubscriberThreeParams subscriberObject = spy(new ValidSubscriberThreeParams());
        final FailureSubscriberMethod subscriber =
                FailureSubscriberMethod.from(subscriberObject.getMethod());
        final ReflectFailures.InvalidProjectName msg = Given.FailureMessage.invalidProjectName();

        subscriber.invoke(subscriberObject, msg,
                          UpdateProjectName.getDefaultInstance(),
                          CommandContext.getDefaultInstance());

        verify(subscriberObject, times(1))
                .handle(msg, UpdateProjectName.getDefaultInstance(),
                        CommandContext.getDefaultInstance());
    }

    @Test
    public void consider_subscriber_with_two_msg_param_valid() {
        final Method subscriber = new ValidSubscriberTwoParams().getMethod();

        assertIsFailureSubscriber(subscriber, true);
    }

    @Test
    public void consider_subscriber_with_both_messages_and_context_params_valid() {
        final Method subscriber = new ValidSubscriberThreeParams().getMethod();

        assertIsFailureSubscriber(subscriber, true);
    }

    @Test
    public void consider_not_public_subscriber_valid() {
        final Method method = new ValidSubscriberButPrivate().getMethod();

        assertIsFailureSubscriber(method, true);
    }

    @Test
    public void consider_not_annotated_subscriber_invalid() {
        final Method subscriber = new InvalidSubscriberNoAnnotation().getMethod();

        assertIsFailureSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_without_params_invalid() {
        final Method subscriber = new InvalidSubscriberNoParams().getMethod();

        assertIsFailureSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_too_many_params_invalid() {
        final Method subscriber = new InvalidSubscriberTooManyParams().getMethod();

        assertIsFailureSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_one_invalid_param_invalid() {
        final Method subscriber = new InvalidSubscriberOneNotMsgParam().getMethod();

        assertIsFailureSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_first_not_message_param_invalid() {
        final Method subscriber = new InvalidSubscriberTwoParamsFirstInvalid().getMethod();

        assertIsFailureSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_second_not_context_param_invalid() {
        final Method subscriber = new InvalidSubscriberTwoParamsSecondInvalid().getMethod();

        assertIsFailureSubscriber(subscriber, false);
    }

    @Test
    public void consider_not_void_subscriber_invalid() {
        final Method subscriber = new InvalidSubscriberNotVoid().getMethod();

        assertIsFailureSubscriber(subscriber, false);
    }

    private static void assertIsFailureSubscriber(Method subscriber, boolean isSubscriber) {
        assertEquals(isSubscriber, FailureSubscriberMethod.predicate().apply(subscriber));
    }

    /*
     * Valid subscribers
     */

    private static class ValidSubscriberTwoParams extends TestFailureSubscriber {
        @Subscribe
        public void handle(ReflectFailures.InvalidProjectName failure, UpdateProjectName command) {
        }
    }

    private static class ValidSubscriberThreeParams extends TestFailureSubscriber {
        @Subscribe
        public void handle(ReflectFailures.InvalidProjectName failure,
                           UpdateProjectName command, CommandContext context) {
        }
    }

    private static class ValidSubscriberButPrivate extends TestFailureSubscriber {
        @Subscribe
        private void handle(ReflectFailures.InvalidProjectName failure, UpdateProjectName command) {
        }
    }

    /*
     * Invalid subscribers
     */

    /**
     * The subscriber with a method which is not annotated.
     */
    private static class InvalidSubscriberNoAnnotation extends TestFailureSubscriber {
        @SuppressWarnings("unused")
        public void handle(ReflectFailures.InvalidProjectName failure,  UpdateProjectName command) {
        }
    }

    /**
     * The subscriber with a method which does not have parameters.
     */
    private static class InvalidSubscriberNoParams extends TestFailureSubscriber {
        @Subscribe
        public void handle() {
        }
    }

    /**
     * The subscriber which has too many parameters.
     */
    private static class InvalidSubscriberTooManyParams extends TestFailureSubscriber {
        @Subscribe
        public void handle(ReflectFailures.InvalidProjectName failure,
                           UpdateProjectName command,
                           CommandContext context,
                           Object redundant) {
        }
    }

    /**
     * The subscriber which has invalid single argument.
     */
    private static class InvalidSubscriberOneNotMsgParam extends TestFailureSubscriber {
        @Subscribe
        public void handle(Exception invalid) {
        }
    }

    /**
     * The subscriber with a method with first invalid parameter.
     */
    private static class InvalidSubscriberTwoParamsFirstInvalid extends TestFailureSubscriber {
        @Subscribe
        public void handle(Exception invalid,  UpdateProjectName command) {
        }
    }

    /**
     * The subscriber which has invalid second parameter.
     */
    private static class InvalidSubscriberTwoParamsSecondInvalid extends TestFailureSubscriber {
        @Subscribe
        public void handle(ReflectFailures.InvalidProjectName failure, Exception invalid) {
        }
    }

    /**
     * The class with failure subscriber that returns {@code Object} instead of {@code void}.
     */
    private static class InvalidSubscriberNotVoid extends TestFailureSubscriber {
        @Subscribe
        public Object handle(ReflectFailures.InvalidProjectName failure, UpdateProjectName command) {
            return failure;
        }
    }

    /**
     * The abstract base for test subscriber classes.
     *
     * <p>The purpose of this class is to obtain a reference to a
     * {@linkplain #HANDLER_METHOD_NAME single subscriber method}.
     * This reference will be later used for assertions.
     */
    private abstract static class TestFailureSubscriber {

        @SuppressWarnings("DuplicateStringLiteralInspection")
        private static final String HANDLER_METHOD_NAME = "handle";

        Method getMethod() {
            final Method[] methods = getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(HANDLER_METHOD_NAME)) {
                    return method;
                }
            }
            throw new RuntimeException("No failure subscriber method found " + HANDLER_METHOD_NAME);
        }
    }
}
