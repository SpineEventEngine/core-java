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
package io.spine.server.reflect;

import com.google.common.testing.NullPointerTester;
import io.spine.core.CommandContext;
import io.spine.server.reflect.given.Given;
import io.spine.server.reflect.given.RejectionSubscriberMethodTestEnv.InvalidSubscriberNoAnnotation;
import io.spine.server.reflect.given.RejectionSubscriberMethodTestEnv.InvalidSubscriberNoParams;
import io.spine.server.reflect.given.RejectionSubscriberMethodTestEnv.InvalidSubscriberNotVoid;
import io.spine.server.reflect.given.RejectionSubscriberMethodTestEnv.InvalidSubscriberOneNotMsgParam;
import io.spine.server.reflect.given.RejectionSubscriberMethodTestEnv.InvalidSubscriberTooManyParams;
import io.spine.server.reflect.given.RejectionSubscriberMethodTestEnv.InvalidSubscriberTwoParamsFirstInvalid;
import io.spine.server.reflect.given.RejectionSubscriberMethodTestEnv.InvalidSubscriberTwoParamsSecondInvalid;
import io.spine.server.reflect.given.RejectionSubscriberMethodTestEnv.ValidSubscriberButPrivate;
import io.spine.server.reflect.given.RejectionSubscriberMethodTestEnv.ValidSubscriberThreeParams;
import io.spine.server.reflect.given.RejectionSubscriberMethodTestEnv.ValidSubscriberTwoParams;
import io.spine.test.reflect.ReflectRejections.InvalidProjectName;
import io.spine.test.rejection.command.UpdateProjectName;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings("unused")     // some of tests address just the fact of method declaration.
public class RejectionSubscriberMethodShould {

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(CommandContext.class, CommandContext.getDefaultInstance())
                .testAllPublicStaticMethods(RejectionSubscriberMethod.class);
    }

    @Test
    public void invoke_subscriber_method() throws InvocationTargetException {
        final ValidSubscriberThreeParams subscriberObject = spy(new ValidSubscriberThreeParams());
        final RejectionSubscriberMethod subscriber =
                new CommandAwareRejectionSubscriberMethod(subscriberObject.getMethod());
        final InvalidProjectName msg = Given.RejectionMessage.invalidProjectName();

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

        assertIsRejectionSubscriber(subscriber, true);
    }

    @Test
    public void consider_subscriber_with_both_messages_and_context_params_valid() {
        final Method subscriber = new ValidSubscriberThreeParams().getMethod();

        assertIsRejectionSubscriber(subscriber, true);
    }

    @Test
    public void consider_not_public_subscriber_valid() {
        final Method method = new ValidSubscriberButPrivate().getMethod();

        assertIsRejectionSubscriber(method, true);
    }

    @Test
    public void consider_not_annotated_subscriber_invalid() {
        final Method subscriber = new InvalidSubscriberNoAnnotation().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_without_params_invalid() {
        final Method subscriber = new InvalidSubscriberNoParams().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_too_many_params_invalid() {
        final Method subscriber = new InvalidSubscriberTooManyParams().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_one_invalid_param_invalid() {
        final Method subscriber = new InvalidSubscriberOneNotMsgParam().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_first_not_message_param_invalid() {
        final Method subscriber = new InvalidSubscriberTwoParamsFirstInvalid().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_second_not_context_param_invalid() {
        final Method subscriber = new InvalidSubscriberTwoParamsSecondInvalid().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    @Test
    public void consider_not_void_subscriber_invalid() {
        final Method subscriber = new InvalidSubscriberNotVoid().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    private static void assertIsRejectionSubscriber(Method subscriber, boolean isSubscriber) {
        assertEquals(isSubscriber, RejectionSubscriberMethod.predicate().apply(subscriber));
    }
}
