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
package io.spine.server.rejection;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.RejectionContext;
import io.spine.server.model.given.Given;
import io.spine.server.rejection.given.RejectionReactorMethodTestEnv.RInvalidNoAnnotation;
import io.spine.server.rejection.given.RejectionReactorMethodTestEnv.RInvalidNoParams;
import io.spine.server.rejection.given.RejectionReactorMethodTestEnv.RInvalidNotMessage;
import io.spine.server.rejection.given.RejectionReactorMethodTestEnv.RInvalidOneNotMsgParam;
import io.spine.server.rejection.given.RejectionReactorMethodTestEnv.RInvalidTooManyParams;
import io.spine.server.rejection.given.RejectionReactorMethodTestEnv.RInvalidTwoParamsFirstInvalid;
import io.spine.server.rejection.given.RejectionReactorMethodTestEnv.RInvalidTwoParamsSecondInvalid;
import io.spine.server.rejection.given.RejectionReactorMethodTestEnv.RValidButPrivate;
import io.spine.server.rejection.given.RejectionReactorMethodTestEnv.RValidThreeParams;
import io.spine.server.rejection.given.RejectionReactorMethodTestEnv.RValidTwoParams;
import io.spine.test.reflect.ReflectRejections.InvalidProjectName;
import io.spine.test.rejection.command.RjUpdateProjectName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names */})
@DisplayName("RejectionReactorMethod should")
class RejectionReactorMethodTest {

    private static final CommandContext emptyCommandContext = CommandContext.getDefaultInstance();

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(Any.class, Any.getDefaultInstance())
                .setDefault(CommandContext.class, emptyCommandContext)
                .setDefault(RejectionContext.class, RejectionContext.getDefaultInstance())
                .testAllPublicStaticMethods(RejectionReactorMethod.class);
    }

    @SuppressWarnings("CheckReturnValue" 
            /* 1. Ignore builder method (setCommand()) call result.
               2. Ignore result of invoke() -- we check object internals instead. */)
    @Test
    @DisplayName("invoke reactor method")
    void invokeReactorMethod() {
        RValidThreeParams reactorObject = new RValidThreeParams();
        RejectionReactorMethod reactor =
                new RejectionReactorMethod(reactorObject.getMethod());
        InvalidProjectName rejectionMessage = Given.RejectionMessage.invalidProjectName();

        RejectionContext.Builder builder = RejectionContext.newBuilder();
        CommandContext commandContext =
                CommandContext.newBuilder()
                              .setTargetVersion(3040)
                              .build();
        RjUpdateProjectName commandMessage = RjUpdateProjectName.getDefaultInstance();
        builder.setCommand(Command.newBuilder()
                                  .setMessage(pack(commandMessage))
                                  .setContext(commandContext));
        RejectionContext rejectionContext = builder.build();

        reactor.invoke(reactorObject, rejectionMessage, rejectionContext);

        assertEquals(rejectionMessage, reactorObject.getLastRejectionMessage());
        assertEquals(commandMessage, reactorObject.getLastCommandMessage());
        assertEquals(commandContext, reactorObject.getLastCommandContext());
    }

    @Nested
    @DisplayName("consider reactor valid with")
    class ConsiderReactorValidWith {

        @Test
        @DisplayName("two Message params")
        void twoMsgParams() {
            Method reactor = new RValidTwoParams().getMethod();

            assertIsRejectionReactor(reactor, true);
        }

        @Test
        @DisplayName("both Messages and Context params")
        void msgAndContextParams() {
            Method reactor = new RValidThreeParams().getMethod();

            assertIsRejectionReactor(reactor, true);
        }

        @Test
        @DisplayName("non-public access")
        void nonPublicAccess() {
            Method method = new RValidButPrivate().getMethod();

            assertIsRejectionReactor(method, true);
        }
    }

    @Nested
    @DisplayName("consider reactor invalid with")
    class ConsiderReactorInvalidWith {

        @Test
        @DisplayName("no annotation")
        void noAnnotation() {
            Method reactor = new RInvalidNoAnnotation().getMethod();

            assertIsRejectionReactor(reactor, false);
        }

        @Test
        @DisplayName("no params")
        void noParams() {
            Method reactor = new RInvalidNoParams().getMethod();

            assertIsRejectionReactor(reactor, false);
        }

        @Test
        @DisplayName("too many params")
        void tooManyParams() {
            Method reactor = new RInvalidTooManyParams().getMethod();

            assertIsRejectionReactor(reactor, false);
        }

        @Test
        @DisplayName("one invalid param")
        void oneInvalidParam() {
            Method reactor = new RInvalidOneNotMsgParam().getMethod();

            assertIsRejectionReactor(reactor, false);
        }

        @Test
        @DisplayName("first non-Message param")
        void firstNonMessageParam() {
            Method reactor = new RInvalidTwoParamsFirstInvalid().getMethod();

            assertIsRejectionReactor(reactor, false);
        }

        @Test
        @DisplayName("second non-Context param")
        void secondNonContextParam() {
            Method reactor = new RInvalidTwoParamsSecondInvalid().getMethod();

            assertIsRejectionReactor(reactor, false);
        }

        @Test
        @DisplayName("non-void return type")
        void nonVoidReturnType() {
            Method reactor = new RInvalidNotMessage().getMethod();

            assertIsRejectionReactor(reactor, false);
        }
    }

    @Test
    @DisplayName("throw IAE on attempt to create instance from invalid method")
    void throwOnInvalidMethod() {
        Method illegalMethod = new RInvalidTooManyParams().getMethod();

        assertThrows(IllegalArgumentException.class,
                     () -> new RejectionReactorMethod(illegalMethod));
    }

    private static void assertIsRejectionReactor(Method reactor, boolean isReactor) {
        assertEquals(isReactor, RejectionReactorMethod.predicate()
                                                      .test(reactor));
    }
}
