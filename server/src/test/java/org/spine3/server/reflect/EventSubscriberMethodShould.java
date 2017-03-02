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
import org.spine3.base.EventContext;
import org.spine3.server.event.Subscribe;
import org.spine3.test.reflect.event.ProjectCreated;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("unused") // OK as we have some
public class EventSubscriberMethodShould {

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .testAllPublicStaticMethods(EventSubscriberMethod.class);
    }
    
    @Test
    public void invoke_subscriber_method() throws InvocationTargetException {
        final ValidSubscriberTwoParams subscriberObject = spy(new ValidSubscriberTwoParams());
        final EventSubscriberMethod subscriber =
                EventSubscriberMethod.from(subscriberObject.getMethod());
        final ProjectCreated msg = Given.EventMessage.projectCreated();

        subscriber.invoke(subscriberObject, msg, EventContext.getDefaultInstance());

        verify(subscriberObject, times(1))
                .handle(msg, EventContext.getDefaultInstance());
    }

    @Test
    public void consider_subscriber_with_one_msg_param_valid() {
        final Method subscriber = new ValidSubscriberOneParam().getMethod();

        assertIsEventSubscriber(subscriber, true);
    }

    @Test
    public void consider_subscriber_with_msg_and_context_params_valid() {
        final Method subscriber = new ValidSubscriberTwoParams().getMethod();

        assertIsEventSubscriber(subscriber, true);
    }

    @Test
    public void consider_not_public_subscriber_valid() {
        final Method method = new ValidSubscriberButPrivate().getMethod();

        assertIsEventSubscriber(method, true);
    }

    @Test
    public void consider_not_annotated_subscriber_invalid() {
        final Method subscriber = new InvalidSubscriberNoAnnotation().getMethod();

        assertIsEventSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_without_params_invalid() {
        final Method subscriber = new InvalidSubscriberNoParams().getMethod();

        assertIsEventSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_too_many_params_invalid() {
        final Method subscriber = new InvalidSubscriberTooManyParams().getMethod();

        assertIsEventSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_one_invalid_param_invalid() {
        final Method subscriber = new InvalidSubscriberOneNotMsgParam().getMethod();

        assertIsEventSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_first_not_message_param_invalid() {
        final Method subscriber = new InvalidSubscriberTwoParamsFirstInvalid().getMethod();

        assertIsEventSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_second_not_context_param_invalid() {
        final Method subscriber = new InvalidSubscriberTwoParamsSecondInvalid().getMethod();

        assertIsEventSubscriber(subscriber, false);
    }

    @Test
    public void consider_not_void_subscriber_invalid() {
        final Method subscriber = new InvalidSubscriberNotVoid().getMethod();

        assertIsEventSubscriber(subscriber, false);
    }

    private static void assertIsEventSubscriber(Method subscriber, boolean isSubscriber) {
        assertEquals(isSubscriber, EventSubscriberMethod.predicate().apply(subscriber));
    }

    /*
     * Valid subscribers
     */

    private static class ValidSubscriberOneParam extends TestEventSubscriber {
        @Subscribe
        public void handle(ProjectCreated event) {
        }
    }

    private static class ValidSubscriberTwoParams extends TestEventSubscriber {
        @Subscribe
        public void handle(ProjectCreated event, EventContext context) {
        }
    }

    private static class ValidSubscriberButPrivate extends TestEventSubscriber {
        @Subscribe
        private void handle(ProjectCreated event) {
        }
    }

    /*
     * Invalid subscribers
     */

    /**
     * The subscriber with a method which is not annotated.
     */
    private static class InvalidSubscriberNoAnnotation extends TestEventSubscriber {
        @SuppressWarnings("unused")
        public void handle(ProjectCreated event, EventContext context) {
        }
    }

    /**
     * The subscriber with a method which does not have parameters.
     */
    private static class InvalidSubscriberNoParams extends TestEventSubscriber {
        @Subscribe
        public void handle() {
        }
    }

    /**
     * The subscriber which has too many parameters.
     */
    private static class InvalidSubscriberTooManyParams extends TestEventSubscriber {
        @Subscribe
        public void handle(ProjectCreated event, EventContext context, Object redundant) {
        }
    }

    /**
     * The subscriber which has invalid single argument.
     */
    private static class InvalidSubscriberOneNotMsgParam extends TestEventSubscriber {
        @Subscribe
        public void handle(Exception invalid) {
        }
    }

    /**
     * The subscriber with a method with first invalid parameter.
     */
    private static class InvalidSubscriberTwoParamsFirstInvalid extends TestEventSubscriber {
        @Subscribe
        public void handle(Exception invalid, EventContext context) {
        }
    }

    /**
     * The subscriber which has invalid second parameter.
     */
    private static class InvalidSubscriberTwoParamsSecondInvalid extends TestEventSubscriber {
        @Subscribe
        public void handle(ProjectCreated event, Exception invalid) {
        }
    }

    /**
     * The class with event subscriber that returns {@code Object} instead of {@code void}.
     */
    private static class InvalidSubscriberNotVoid extends TestEventSubscriber {
        @Subscribe
        public Object handle(ProjectCreated event) {
            return event;
        }
    }

    /**
     * The abstract base for test subscriber classes.
     *
     * <p>The purpose of this class is to obtain a reference to a
     * {@linkplain #HANDLER_METHOD_NAME single subscriber method}.
     * This reference will be later used for assertions.
     */
    private abstract static class TestEventSubscriber {

        @SuppressWarnings("DuplicateStringLiteralInspection")
        private static final String HANDLER_METHOD_NAME = "handle";

        Method getMethod() {
            final Method[] methods = getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(HANDLER_METHOD_NAME)) {
                    return method;
                }
            }
            throw new RuntimeException("No subscriber method found: " + HANDLER_METHOD_NAME);
        }
    }
}
