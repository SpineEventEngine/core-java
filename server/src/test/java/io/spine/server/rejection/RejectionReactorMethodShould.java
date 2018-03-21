/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static io.spine.protobuf.AnyPacker.pack;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("unused")     // some of tests address just the fact of method declaration.
public class RejectionReactorMethodShould {

    private static final CommandContext emptyCommandContext = CommandContext.getDefaultInstance();

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Any.class, Any.getDefaultInstance())
                .setDefault(CommandContext.class, emptyCommandContext)
                .setDefault(RejectionContext.class, RejectionContext.getDefaultInstance())
                .testAllPublicStaticMethods(RejectionReactorMethod.class);
    }

    @Test
    public void invoke_reactor_method() throws InvocationTargetException {
        final RValidThreeParams reactorObject = new RValidThreeParams();
        final RejectionReactorMethod reactor =
                new RejectionReactorMethod(reactorObject.getMethod());
        final InvalidProjectName rejectionMessage = Given.RejectionMessage.invalidProjectName();

        final RejectionContext.Builder builder = RejectionContext.newBuilder();
        final CommandContext commandContext =
                CommandContext.newBuilder()
                              .setTargetVersion(3040)
                              .build();
        final RjUpdateProjectName commandMessage = RjUpdateProjectName.getDefaultInstance();
        builder.setCommand(Command.newBuilder()
                                  .setMessage(pack(commandMessage))
                                  .setContext(commandContext));
        final RejectionContext rejectionContext = builder.build();

        reactor.invoke(reactorObject, rejectionMessage, rejectionContext);

        assertEquals(rejectionMessage, reactorObject.getLastRejectionMessage());
        assertEquals(commandMessage, reactorObject.getLastCommandMessage());
        assertEquals(commandContext, reactorObject.getLastCommandContext());
    }

    @Test
    public void consider_reactor_with_two_msg_param_valid() {
        final Method reactor = new RValidTwoParams().getMethod();

        assertIsRejectionReactor(reactor, true);
    }

    @Test
    public void consider_reactor_with_both_messages_and_context_params_valid() {
        final Method reactor = new RValidThreeParams().getMethod();

        assertIsRejectionReactor(reactor, true);
    }

    @Test
    public void consider_not_public_reactor_valid() {
        final Method method = new RValidButPrivate().getMethod();

        assertIsRejectionReactor(method, true);
    }

    @Test
    public void consider_not_annotated_reactor_invalid() {
        final Method reactor = new RInvalidNoAnnotation().getMethod();

        assertIsRejectionReactor(reactor, false);
    }

    @Test
    public void consider_reactor_without_params_invalid() {
        final Method reactor = new RInvalidNoParams().getMethod();

        assertIsRejectionReactor(reactor, false);
    }

    @Test
    public void consider_reactor_with_too_many_params_invalid() {
        final Method reactor = new RInvalidTooManyParams().getMethod();

        assertIsRejectionReactor(reactor, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_on_attempt_to_create_instance_for_a_method_with_too_many_params() {
        final Method illegalMethod = new RInvalidTooManyParams().getMethod();

        new RejectionReactorMethod(illegalMethod);
    }

    @Test
    public void consider_reactor_with_one_invalid_param_invalid() {
        final Method reactor = new RInvalidOneNotMsgParam().getMethod();

        assertIsRejectionReactor(reactor, false);
    }

    @Test
    public void consider_reactor_with_first_not_message_param_invalid() {
        final Method reactor = new RInvalidTwoParamsFirstInvalid().getMethod();

        assertIsRejectionReactor(reactor, false);
    }

    @Test
    public void consider_reactor_with_second_not_context_param_invalid() {
        final Method reactor = new RInvalidTwoParamsSecondInvalid().getMethod();

        assertIsRejectionReactor(reactor, false);
    }

    @Test
    public void consider_not_void_reactor_invalid() {
        final Method reactor = new RInvalidNotMessage().getMethod();

        assertIsRejectionReactor(reactor, false);
    }

    private static void assertIsRejectionReactor(Method reactor, boolean isReactor) {
        assertEquals(isReactor, RejectionReactorMethod.predicate()
                                                      .apply(reactor));
    }
}
