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

package io.spine.server.command;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateMessageDispatcher;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerNoAnnotation;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerNoParams;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerOneNotMsgParam;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerReturnsVoid;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerTooManyParams;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerTwoParamsFirstInvalid;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerTwoParamsSecondInvalid;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.RejectingAggregate;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.RejectingHandler;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.ValidHandlerButPrivate;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.ValidHandlerOneParam;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.ValidHandlerOneParamReturnsList;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.ValidHandlerTwoParams;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.ValidHandlerTwoParamsReturnsList;
import io.spine.server.model.HandlerMethodFailedException;
import io.spine.server.model.ModelTests;
import io.spine.test.reflect.ProjectId;
import io.spine.test.reflect.command.RefCreateProject;
import io.spine.test.reflect.event.RefProjectCreated;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Throwables.getRootCause;
import static io.spine.server.command.CommandHandlerMethod.from;
import static io.spine.server.command.CommandHandlerMethod.predicate;
import static io.spine.server.model.given.Given.CommandMessage.createProject;
import static io.spine.server.model.given.Given.CommandMessage.startProject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public class CommandHandlerMethodShould {

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(CommandHandlerMethodShould.class);

    private static final CommandContext emptyContext = CommandContext.getDefaultInstance();

    private static void assertIsCommandHandler(Method handler, boolean isHandler) {
        assertEquals(isHandler, predicate().apply(handler));
    }

    private static void assertCauseAndId(HandlerMethodFailedException e, Object handlerId) {
        final Throwable cause = getRootCause(e);

        assertTrue(cause instanceof ThrowableMessage);
        final ThrowableMessage thrown = (ThrowableMessage) cause;

        assertTrue(thrown.producerId()
                         .isPresent());
        assertEquals(handlerId, Identifier.unpack(thrown.producerId()
                                                        .get()));
    }

    @Before
    public void setUp() {
        ModelTests.clearModel();
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(CommandEnvelope.class,
                            requestFactory.generateEnvelope())
                .setDefault(CommandContext.class, emptyContext)
                .setDefault(Any.class, Any.getDefaultInstance())
                .testAllPublicStaticMethods(CommandHandlerMethod.class);
    }

    @Test
    public void invoke_handler_method_which_returns_one_message() {
        final ValidHandlerTwoParams handlerObject = spy(new ValidHandlerTwoParams());
        final CommandHandlerMethod handler = from(handlerObject.getHandler());
        final RefCreateProject cmd = createProject();

        final List<? extends Message> events = handler.invoke(handlerObject, cmd, emptyContext);

        verify(handlerObject, times(1))
                .handleTest(cmd, emptyContext);
        assertEquals(1, events.size());
        final RefProjectCreated event = (RefProjectCreated) events.get(0);
        assertEquals(cmd.getProjectId(), event.getProjectId());
    }

    @Test
    public void invoke_handler_method_and_return_message_list() {
        final ValidHandlerOneParamReturnsList handlerObject =
                spy(new ValidHandlerOneParamReturnsList());
        final CommandHandlerMethod handler = from(handlerObject.getHandler());
        final RefCreateProject cmd = createProject();

        final List<? extends Message> events = handler.invoke(handlerObject, cmd, emptyContext);

        verify(handlerObject, times(1)).handleTest(cmd);
        assertEquals(1, events.size());
        final RefProjectCreated event = (RefProjectCreated) events.get(0);
        assertEquals(cmd.getProjectId(), event.getProjectId());
    }

    @Test
    public void consider_handler_with_one_msg_param_valid() {
        final Method handler = new ValidHandlerOneParam().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    public void consider_handler_with_one_msg_param_which_returns_list_valid() {
        final Method handler = new ValidHandlerOneParamReturnsList().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    public void consider_handler_with_msg_and_context_params_valid() {
        final Method handler = new ValidHandlerTwoParams().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    public void consider_handler_with_msg_and_context_params_which_returns_list_valid() {
        final Method handler = new ValidHandlerTwoParamsReturnsList().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    public void consider_not_public_handler_valid() {
        final Method method = new ValidHandlerButPrivate().getHandler();

        assertIsCommandHandler(method, true);
    }

    @Test
    public void consider_not_annotated_handler_invalid() {
        final Method handler = new InvalidHandlerNoAnnotation().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_without_params_invalid() {
        final Method handler = new InvalidHandlerNoParams().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_with_too_many_params_invalid() {
        final Method handler = new InvalidHandlerTooManyParams().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_with_one_invalid_param_invalid() {
        final Method handler = new InvalidHandlerOneNotMsgParam().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_with_first_not_message_param_invalid() {
        final Method handler = new InvalidHandlerTwoParamsFirstInvalid().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_handler_with_second_not_context_param_invalid() {
        final Method handler = new InvalidHandlerTwoParamsSecondInvalid().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    public void consider_void_handler_invalid() {
        final Method handler = new InvalidHandlerReturnsVoid().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_ISE_for_not_handled_command_type() {
        final CommandHandler handler = new ValidHandlerOneParam();
        final CommandEnvelope cmd = requestFactory.createEnvelope(startProject());
        handler.dispatch(cmd);
    }

    @Test
    public void set_producer_ID_if_command_handler() {
        final CommandHandler handler = new RejectingHandler();
        final CommandEnvelope envelope = requestFactory.createEnvelope(createProject());
        try {
            handler.dispatch(envelope);
        } catch (HandlerMethodFailedException e) {
            assertCauseAndId(e, handler.getId());
        }
    }

    @Test
    public void set_producer_ID_if_entity() {
        final RefCreateProject commandMessage = createProject();
        final Aggregate<ProjectId, ?, ?> entity =
                new RejectingAggregate(commandMessage.getProjectId());
        final CommandEnvelope cmd = requestFactory.createEnvelope(commandMessage);
        try {
            AggregateMessageDispatcher.dispatchCommand(entity, cmd);
        } catch (HandlerMethodFailedException e) {
            assertCauseAndId(e, entity.getId());
        }
    }
}
