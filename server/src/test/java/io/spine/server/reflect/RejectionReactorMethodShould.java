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
import com.google.protobuf.Any;
import io.spine.core.CommandContext;
import io.spine.core.RejectionEnvelope;
import io.spine.server.reflect.given.Given;
import io.spine.server.reflect.given.RejectionReactorMethodTestEnv.InvalidNoAnnotation;
import io.spine.server.reflect.given.RejectionReactorMethodTestEnv.InvalidNoParams;
import io.spine.server.reflect.given.RejectionReactorMethodTestEnv.InvalidNotMessage;
import io.spine.server.reflect.given.RejectionReactorMethodTestEnv.InvalidOneNotMsgParam;
import io.spine.server.reflect.given.RejectionReactorMethodTestEnv.InvalidTooManyParams;
import io.spine.server.reflect.given.RejectionReactorMethodTestEnv.InvalidTwoParamsFirstInvalid;
import io.spine.server.reflect.given.RejectionReactorMethodTestEnv.InvalidTwoParamsSecondInvalid;
import io.spine.server.reflect.given.RejectionReactorMethodTestEnv.ValidButPrivate;
import io.spine.server.reflect.given.RejectionReactorMethodTestEnv.ValidThreeParams;
import io.spine.server.reflect.given.RejectionReactorMethodTestEnv.ValidTwoParams;
import io.spine.server.rejection.given.FaultyReactor;
import io.spine.server.rejection.given.VerifiableReactor;
import io.spine.test.reflect.ReflectRejections.InvalidProjectName;
import io.spine.test.rejection.command.UpdateProjectName;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static io.spine.server.rejection.given.Given.invalidProjectNameRejection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings("unused")     // some of tests address just the fact of method declaration.
public class RejectionReactorMethodShould {

    private static final CommandContext emptyContext = CommandContext.getDefaultInstance();

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Any.class, Any.getDefaultInstance())
                .setDefault(CommandContext.class, emptyContext)
                .testAllPublicStaticMethods(RejectionReactorMethod.class);
    }

    @Test
    public void invoke_subscriber_method() throws InvocationTargetException {
        final ValidThreeParams subscriberObject = spy(new ValidThreeParams());
        final RejectionReactorMethod subscriber =
                new RejectionReactorMethod(subscriberObject.getMethod());
        final InvalidProjectName msg = Given.RejectionMessage.invalidProjectName();

        subscriber.invoke(subscriberObject,
                          msg,
                          UpdateProjectName.getDefaultInstance(),
                          emptyContext);

        verify(subscriberObject, times(1))
                .handle(msg, UpdateProjectName.getDefaultInstance(), emptyContext);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void not_allow_invoking_inherited_invoke_method() {
        final ValidThreeParams subscriberObject = new ValidThreeParams();
        final RejectionReactorMethod subscriber =
                new RejectionReactorMethod(subscriberObject.getMethod());

        final InvalidProjectName msg = Given.RejectionMessage.invalidProjectName();

        // This should fail.
        subscriber.invoke(subscriberObject, msg, emptyContext);
        
        fail("Exception not thrown");
    }

    @Test
    public void catch_exceptions_caused_by_subscribers() {
        final VerifiableReactor faultySubscriber = new FaultyReactor();

        faultySubscriber.dispatch(RejectionEnvelope.of(invalidProjectNameRejection()));

        assertTrue(faultySubscriber.isMethodCalled());
    }

    @Test
    public void consider_subscriber_with_two_msg_param_valid() {
        final Method subscriber = new ValidTwoParams().getMethod();

        assertIsRejectionSubscriber(subscriber, true);
    }

    @Test
    public void consider_subscriber_with_both_messages_and_context_params_valid() {
        final Method subscriber = new ValidThreeParams().getMethod();

        assertIsRejectionSubscriber(subscriber, true);
    }

    @Test
    public void consider_not_public_subscriber_valid() {
        final Method method = new ValidButPrivate().getMethod();

        assertIsRejectionSubscriber(method, true);
    }

    @Test
    public void consider_not_annotated_subscriber_invalid() {
        final Method subscriber = new InvalidNoAnnotation().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_without_params_invalid() {
        final Method subscriber = new InvalidNoParams().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_too_many_params_invalid() {
        final Method subscriber = new InvalidTooManyParams().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_on_attempt_to_create_instance_for_a_method_with_too_many_params() {
        final Method illegalMethod = new InvalidTooManyParams().getMethod();

        new RejectionReactorMethod(illegalMethod);
    }

    @Test
    public void consider_subscriber_with_one_invalid_param_invalid() {
        final Method subscriber = new InvalidOneNotMsgParam().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_first_not_message_param_invalid() {
        final Method subscriber = new InvalidTwoParamsFirstInvalid().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    @Test
    public void consider_subscriber_with_second_not_context_param_invalid() {
        final Method subscriber = new InvalidTwoParamsSecondInvalid().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    @Test
    public void consider_not_void_subscriber_invalid() {
        final Method subscriber = new InvalidNotMessage().getMethod();

        assertIsRejectionSubscriber(subscriber, false);
    }

    private static void assertIsRejectionSubscriber(Method subscriber, boolean isSubscriber) {
        assertEquals(isSubscriber, RejectionReactorMethod.predicate().apply(subscriber));
    }
}
