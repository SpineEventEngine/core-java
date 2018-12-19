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

package io.spine.server.event.model.given;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.Rejections.ProjectAlreadyExists;
import io.spine.test.event.command.CreateProject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test environment for {@link io.spine.server.event.model.declare.SubscriberSignatureTest}.
 */
public class SubscriberSignatureTestEnv {

    private static final String MESSAGE_ONLY = "messageOnly";

    /**
     * Prevents the utility class instantiation.
     */
    private SubscriberSignatureTestEnv() {
    }

    public static Method findMessageOnly() {
        Method method = findValidMethod(MESSAGE_ONLY);
        return method;
    }

    public static Method findMessageAndContext() {
        Method method = findValidMethod("messageAndContext");
        return method;
    }

    public static Method findMessageAndCmdContext() {
        Method method = findValidMethod("messageAndCmdContext");
        return method;
    }

    public static Method findMessageAndCommand() {
        Method method = findValidMethod("messageAndCommand");
        return method;
    }

    public static Method findMessageAndCommandMessageAndContext() {
        Method method = findValidMethod("messageAndCommandMessageAndContext");
        return method;
    }

    public static Method findReturnsValue() {
        Method method = findInvalidMethod("returnsValue");
        return method;
    }

    public static Method findContextArgumentMismatch() {
        Method method = findInvalidMethod("contextArgumentMismatch");
        return method;
    }

    public static Method findFirstArgumentMismatch() {
        Method method = findInvalidMethod("firstArgumentMismatch");
        return method;
    }

    public static Method findThrowsUnchckedException() {
        Method method = findInvalidMethod("throwsUncheckedException");
        return method;
    }

    private static Method findValidMethod(String name) {
        return findMethod(ValidEventReceiver.class, name);
    }

    private static Method findInvalidMethod(String name) {
        return findMethod(InvalidEventReceiver.class, name);
    }

    private static Method findMethod(Class<?> declaringClass, String name) {
        Method result = Stream.of(declaringClass.getDeclaredMethods())
                              .filter(method -> method.getName()
                                                      .equals(name))
                              .findAny()
                              .orElseGet(() -> fail(format("Method %s not found.", name)));
        return result;
    }

    @SuppressWarnings("unused") // Reflective method access.
    public static final class ValidEventReceiver {

        private Message event;
        private Message command;
        private EventContext eventContext;
        private CommandContext commandContext;

        @Subscribe
        public void messageOnly(ProjectCreated event) {
            this.event = event;
        }

        @Subscribe
        private void messageAndContext(ProjectCreated event, EventContext eventContext) {
            this.event = event;
            this.eventContext = eventContext;
        }

        @SuppressWarnings("ProtectedMemberInFinalClass") // Required for access modifier testing.
        @Subscribe
        protected void messageAndCmdContext(ProjectAlreadyExists rejection,
                                            CommandContext commandContext) {
            this.event = rejection;
            this.commandContext = commandContext;
        }

        @Subscribe
        void messageAndCommand(ProjectAlreadyExists rejection,
                               CreateProject command) {
            this.event = rejection;
            this.command = command;
        }

        @Subscribe
        public void messageAndCommandMessageAndContext(ProjectAlreadyExists rejection,
                                                       CreateProject command,
                                                       CommandContext commandContext) {
            this.event = rejection;
            this.command = command;
            this.commandContext = commandContext;
        }

        public void assertEvent(Message expected) {
            assertEquals(expected, event);
        }

        public void assertCommand(@Nullable Message expected) {
            assertEquals(expected, command);
        }

        public void assertEventContext(@Nullable EventContext expected) {
            assertEquals(expected, eventContext);
        }

        public void assertCommandContext(@Nullable CommandContext expected) {
            assertEquals(expected, commandContext);
        }
    }

    @SuppressWarnings({"unused", "MethodMayBeStatic"}) // Reflective method access.
    private static final class InvalidEventReceiver {

        private static final String SHOULD_NEVER_BE_CALLED = "Should never be called.";

        @Subscribe
        private Object returnsValue(ProjectCreated event) {
            return halt();
        }

        @Subscribe
        private void contextArgumentMismatch(ProjectCreated event, Object wrongParameter) {
            halt();
        }

        @Subscribe
        private void firstArgumentMismatch(Object event) {
            halt();
        }

        @Subscribe
        private void throwsUncheckedException(ProjectAlreadyExists rejection) throws IOException {
            IOException throwable = halt();
            throw throwable;
        }

        /**
         * Always fail a test.
         *
         * @param <T> generic parameter used for covariance of return type for failing a test
         *            from methods that return a value
         * @return nothing ever
         */
        @CanIgnoreReturnValue
        @SuppressWarnings("TypeParameterUnusedInFormals") // See Javadoc.
        private static <T> T halt() {
            return fail(SHOULD_NEVER_BE_CALLED);
        }
    }
}
