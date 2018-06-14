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

package io.spine.server.command;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Throwables.getRootCause;
import static io.spine.server.command.CommandHandlerMethod.from;
import static io.spine.server.command.CommandHandlerMethod.predicate;
import static io.spine.server.model.given.Given.CommandMessage.createProject;
import static io.spine.server.model.given.Given.CommandMessage.startProject;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
@DisplayName("CommandHandlerMethod should")
class CommandHandlerMethodTest {

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(CommandHandlerMethodTest.class);

    private static final CommandContext emptyContext = CommandContext.getDefaultInstance();

    private static void assertIsCommandHandler(Method handler, boolean isHandler) {
        assertEquals(isHandler, predicate().apply(handler));
    }

    private static void assertCauseAndId(HandlerMethodFailedException e, Object handlerId) {
        Throwable cause = getRootCause(e);

        assertTrue(cause instanceof ThrowableMessage);
        ThrowableMessage thrown = (ThrowableMessage) cause;

        assertTrue(thrown.producerId()
                         .isPresent());
        assertEquals(handlerId, Identifier.unpack(thrown.producerId()
                                                        .get()));
    }

    @BeforeEach
    void setUp() {
        ModelTests.clearModel();
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(CommandEnvelope.class,
                            requestFactory.generateEnvelope())
                .setDefault(CommandContext.class, emptyContext)
                .setDefault(Any.class, Any.getDefaultInstance())
                .testAllPublicStaticMethods(CommandHandlerMethod.class);
    }

    @Test
    @DisplayName("invoke handler method which returns one Message")
    void invokeMethodReturningOneMessage() {
        ValidHandlerTwoParams handlerObject = spy(new ValidHandlerTwoParams());
        CommandHandlerMethod handler = from(handlerObject.getHandler());
        RefCreateProject cmd = createProject();

        List<? extends Message> events = handler.invoke(handlerObject, cmd, emptyContext);

        verify(handlerObject, times(1))
                .handleTest(cmd, emptyContext);
        assertEquals(1, events.size());
        RefProjectCreated event = (RefProjectCreated) events.get(0);
        assertEquals(cmd.getProjectId(), event.getProjectId());
    }

    @Test
    @DisplayName("invoke handler method which returns Message list")
    void invokerMethodReturningMessageList() {
        ValidHandlerOneParamReturnsList handlerObject =
                spy(new ValidHandlerOneParamReturnsList());
        CommandHandlerMethod handler = from(handlerObject.getHandler());
        RefCreateProject cmd = createProject();

        List<? extends Message> events = handler.invoke(handlerObject, cmd, emptyContext);

        verify(handlerObject, times(1)).handleTest(cmd);
        assertEquals(1, events.size());
        RefProjectCreated event = (RefProjectCreated) events.get(0);
        assertEquals(cmd.getProjectId(), event.getProjectId());
    }

    @Test
    @DisplayName("consider handler with one Message param valid")
    void considerOneMessageParamValid() {
        Method handler = new ValidHandlerOneParam().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    @DisplayName("consider handler with one Message param which returns list valid")
    void considerOneMessageParamAndReturnListValid() {
        Method handler = new ValidHandlerOneParamReturnsList().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    @DisplayName("consider handler with Message and context params valid")
    void considerMessageAndContextParamsValid() {
        Method handler = new ValidHandlerTwoParams().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    @DisplayName("consider handler with Message and context params which returns list valid")
    void considerMessageAndContextParamWithListReturnValid() {
        Method handler = new ValidHandlerTwoParamsReturnsList().getHandler();

        assertIsCommandHandler(handler, true);
    }

    @Test
    @DisplayName("consider non-public handler valid")
    void considerNonPublicValid() {
        Method method = new ValidHandlerButPrivate().getHandler();

        assertIsCommandHandler(method, true);
    }

    @Test
    @DisplayName("consider not annotated handler invalid")
    void considerNotAnnotatedInvalid() {
        Method handler = new InvalidHandlerNoAnnotation().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    @DisplayName("consider handler without params invalid")
    void considerNoParamsInvalid() {
        Method handler = new InvalidHandlerNoParams().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    @DisplayName("consider handler with too many params invalid")
    void considerTooManyParamsInvalid() {
        Method handler = new InvalidHandlerTooManyParams().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    @DisplayName("consider handler with one invalid param invalid")
    void considerOneInvalidParamInvalid() {
        Method handler = new InvalidHandlerOneNotMsgParam().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    @DisplayName("consider handler with first non-Message param invalid")
    void considerFirstNonMessageParamInvalid() {
        Method handler = new InvalidHandlerTwoParamsFirstInvalid().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    @DisplayName("consider handler with second non-context param invalid")
    void considerSecondNonContextParamInvalid() {
        Method handler = new InvalidHandlerTwoParamsSecondInvalid().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    @DisplayName("consider handler with void return type invalid")
    void considerVoidReturnInvalid() {
        Method handler = new InvalidHandlerReturnsVoid().getHandler();

        assertIsCommandHandler(handler, false);
    }

    @Test
    @DisplayName("throw ISE for not handled command type")
    void throwForNotHandledCommandType() {
        CommandHandler handler = new ValidHandlerOneParam();
        CommandEnvelope cmd = requestFactory.createEnvelope(startProject());

        assertThrows(IllegalStateException.class, () -> handler.dispatch(cmd));
    }

    @SuppressWarnings("CheckReturnValue") // no need as the call to dispatch() throws
    @Test
    @DisplayName("set producer ID when dispatching to command handler")
    void setProducerIdIfCommandHandler() {
        CommandHandler handler = new RejectingHandler();
        CommandEnvelope envelope = requestFactory.createEnvelope(createProject());
        try {
            handler.dispatch(envelope);
        } catch (HandlerMethodFailedException e) {
            assertCauseAndId(e, handler.getId());
        }
    }

    @SuppressWarnings("CheckReturnValue") // no need as the call to dispatchCommand() throws
    @Test
    @DisplayName("set producer ID when dispatching to entity")
    void setProducerIdIfEntity() {
        RefCreateProject commandMessage = createProject();
        Aggregate<ProjectId, ?, ?> entity =
                new RejectingAggregate(commandMessage.getProjectId());
        CommandEnvelope cmd = requestFactory.createEnvelope(commandMessage);
        try {
            AggregateMessageDispatcher.dispatchCommand(entity, cmd);
        } catch (HandlerMethodFailedException e) {
            assertCauseAndId(e, entity.getId());
        }
    }
}
