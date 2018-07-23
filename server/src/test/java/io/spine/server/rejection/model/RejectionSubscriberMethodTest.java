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
package io.spine.server.rejection.model;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.RejectionContext;
import io.spine.core.RejectionEnvelope;
import io.spine.server.model.given.Given;
import io.spine.server.rejection.given.FaultySubscriber;
import io.spine.server.rejection.given.RejectionSubscriberMethodTestEnv.InvalidNoAnnotation;
import io.spine.server.rejection.given.RejectionSubscriberMethodTestEnv.InvalidNoParams;
import io.spine.server.rejection.given.RejectionSubscriberMethodTestEnv.InvalidNotMessage;
import io.spine.server.rejection.given.RejectionSubscriberMethodTestEnv.InvalidOneNotMsgParam;
import io.spine.server.rejection.given.RejectionSubscriberMethodTestEnv.InvalidTooManyParams;
import io.spine.server.rejection.given.RejectionSubscriberMethodTestEnv.InvalidTwoParamsFirstInvalid;
import io.spine.server.rejection.given.RejectionSubscriberMethodTestEnv.InvalidTwoParamsSecondInvalid;
import io.spine.server.rejection.given.RejectionSubscriberMethodTestEnv.ValidButPrivate;
import io.spine.server.rejection.given.RejectionSubscriberMethodTestEnv.ValidThreeParams;
import io.spine.server.rejection.given.RejectionSubscriberMethodTestEnv.ValidTwoParams;
import io.spine.server.rejection.given.VerifiableSubscriber;
import io.spine.test.reflect.ReflectRejections.InvalidProjectName;
import io.spine.test.rejection.command.RjUpdateProjectName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.rejection.given.Given.invalidProjectNameRejection;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names */})
@DisplayName("RejectionSubscriberMethod should")
class RejectionSubscriberMethodTest {

    private static final RejectionContext emptyContext = RejectionContext.getDefaultInstance();
    private static final CommandContext emptyCmdContext = CommandContext.getDefaultInstance();

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(Any.class, Any.getDefaultInstance())
                .setDefault(CommandContext.class, emptyCmdContext)
                .setDefault(RejectionContext.class, emptyContext)
                .testAllPublicStaticMethods(RejectionSubscriberMethod.class);
    }

    @SuppressWarnings("CheckReturnValue") // Can ignore the result of invoke() in this test.
    @Test
    @DisplayName("invoke subscriber method")
    void invokeSubscriberMethod() {
        ValidThreeParams subscriberObject = new ValidThreeParams();
        RejectionSubscriberMethod method =
                new RejectionSubscriberMethod(subscriberObject.getMethod());
        InvalidProjectName rejectionMessage = Given.RejectionMessage.invalidProjectName();

        CommandContext commandContext = CommandContext
                .newBuilder()
                .setTargetVersion(1020)
                .build();
        RjUpdateProjectName commandMessage = RjUpdateProjectName.getDefaultInstance();
        RejectionContext.Builder builder = RejectionContext
                .newBuilder()
                .setCommand(Command.newBuilder()
                                   .setMessage(pack(commandMessage))
                                   .setContext(commandContext));
        RejectionContext rejectionContext = builder.build();

        method.invoke(subscriberObject, rejectionMessage, rejectionContext);

        assertEquals(rejectionMessage, subscriberObject.getLastRejectionMessage());
        assertEquals(commandMessage, subscriberObject.getLastCommandMessage());
        assertEquals(commandContext, subscriberObject.getLastCommandContext());
    }

    @SuppressWarnings("CheckReturnValue") // Can ignore the result of dispatch() in this test.
    @Test
    @DisplayName("catch exceptions caused by subscribers")
    void catchSubscriberExceptions() {
        VerifiableSubscriber faultySubscriber = new FaultySubscriber();

        faultySubscriber.dispatch(RejectionEnvelope.of(invalidProjectNameRejection()));

        assertTrue(faultySubscriber.isMethodCalled());
    }

    @Nested
    @DisplayName("consider subscriber valid with")
    class ConsiderSubscriberValidWith {

        @Test
        @DisplayName("two Message params")
        void twoMsgParams() {
            Method subscriber = new ValidTwoParams().getMethod();

            assertIsRejectionSubscriber(subscriber, true);
        }

        @Test
        @DisplayName("both Messages and Context params")
        void msgAndContextParams() {
            Method subscriber = new ValidThreeParams().getMethod();

            assertIsRejectionSubscriber(subscriber, true);
        }

        @Test
        @DisplayName("non-public access")
        void nonPublicAccess() {
            Method method = new ValidButPrivate().getMethod();

            assertIsRejectionSubscriber(method, true);
        }
    }

    @Nested
    @DisplayName("consider reactor invalid with")
    class ConsiderReactorInvalidWith {

        @Test
        @DisplayName("no annotation")
        void noAnnotation() {
            Method subscriber = new InvalidNoAnnotation().getMethod();

            assertIsRejectionSubscriber(subscriber, false);
        }

        @Test
        @DisplayName("no params")
        void noParams() {
            Method subscriber = new InvalidNoParams().getMethod();

            assertIsRejectionSubscriber(subscriber, false);
        }

        @Test
        @DisplayName("too many params")
        void tooManyParams() {
            Method subscriber = new InvalidTooManyParams().getMethod();

            assertIsRejectionSubscriber(subscriber, false);
        }

        @Test
        @DisplayName("one invalid param")
        void oneInvalidParam() {
            Method subscriber = new InvalidOneNotMsgParam().getMethod();

            assertIsRejectionSubscriber(subscriber, false);
        }

        @Test
        @DisplayName("first non-Message param")
        void firstNonMessageParam() {
            Method subscriber = new InvalidTwoParamsFirstInvalid().getMethod();

            assertIsRejectionSubscriber(subscriber, false);
        }

        @Test
        @DisplayName("second non-Context param")
        void secondNonContextParam() {
            Method subscriber = new InvalidTwoParamsSecondInvalid().getMethod();

            assertIsRejectionSubscriber(subscriber, false);
        }

        @Test
        @DisplayName("non-void return type")
        void nonVoidReturnType() {
            Method subscriber = new InvalidNotMessage().getMethod();

            assertIsRejectionSubscriber(subscriber, false);
        }
    }

    @Test
    @DisplayName("throw IAE on attempt to create instance from invalid method")
    void throwOnInvalidMethod() {
        Method illegalMethod = new InvalidTooManyParams().getMethod();

        assertThrows(IllegalArgumentException.class,
                     () -> new RejectionSubscriberMethod(illegalMethod));
    }

    private static void assertIsRejectionSubscriber(Method subscriber, boolean isSubscriber) {
        assertEquals(isSubscriber,
                     RejectionSubscriberMethod.factory()
                                              .getPredicate()
                                              .test(subscriber));
    }
}
