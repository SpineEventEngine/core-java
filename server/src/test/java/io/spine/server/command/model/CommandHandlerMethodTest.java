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

package io.spine.server.command.model;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.HandlerReturnsEmpty;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.HandlerReturnsEmptyList;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerNoAnnotation;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerNoParams;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerOneNotMsgParam;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerReturnsVoid;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerTooManyParams;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerTwoParamsFirstInvalid;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.InvalidHandlerTwoParamsSecondInvalid;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.ProcessManagerDoingNothing;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.RejectingAggregate;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.RejectingHandler;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.ValidHandlerButPrivate;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.ValidHandlerOneParam;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.ValidHandlerOneParamReturnsList;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.ValidHandlerTwoParams;
import io.spine.server.command.given.CommandHandlerMethodTestEnv.ValidHandlerTwoParamsReturnsList;
import io.spine.server.model.HandlerMethodFailedException;
import io.spine.server.procman.ProcessManager;
import io.spine.test.reflect.ProjectId;
import io.spine.test.reflect.command.RefCreateProject;
import io.spine.test.reflect.event.RefProjectCreated;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.aggregate.AggregateMessageDispatcher;
import io.spine.testing.server.model.ModelTests;
import io.spine.testing.server.procman.PmDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.collect.testing.Helpers.assertEmpty;
import static io.spine.server.command.model.CommandHandlerMethod.from;
import static io.spine.server.model.given.Given.CommandMessage.createProject;
import static io.spine.server.model.given.Given.CommandMessage.startProject;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
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
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("CommandHandlerMethod should")
class CommandHandlerMethodTest {

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(CommandHandlerMethodTest.class);

    private static final CommandContext emptyContext = CommandContext.getDefaultInstance();

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
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

    @Nested
    @DisplayName("invoke handler method which returns")
    class InvokeHandlerMethod {

        @Test
        @DisplayName("one Message")
        void returningMessage() {
            ValidHandlerTwoParams handlerObject = spy(new ValidHandlerTwoParams());
            CommandHandlerMethod handler = from(handlerObject.getHandler());
            RefCreateProject cmd = createProject();

            CommandHandlerMethod.Result result = handler.invoke(handlerObject, cmd, emptyContext);
            List<? extends Message> events = result.asMessages();

            verify(handlerObject, times(1))
                    .handleTest(cmd, emptyContext);
            assertEquals(1, events.size());
            RefProjectCreated event = (RefProjectCreated) events.get(0);
            assertEquals(cmd.getProjectId(), event.getProjectId());
        }

        @Test
        @DisplayName("Message list")
        void returningMessageList() {
            ValidHandlerOneParamReturnsList handlerObject =
                    spy(new ValidHandlerOneParamReturnsList());
            CommandHandlerMethod handler = from(handlerObject.getHandler());
            RefCreateProject cmd = createProject();

            CommandHandlerMethod.Result result = handler.invoke(handlerObject, cmd, emptyContext);
            List<? extends Message> events = result.asMessages();

            verify(handlerObject, times(1)).handleTest(cmd);
            assertEquals(1, events.size());
            RefProjectCreated event = (RefProjectCreated) events.get(0);
            assertEquals(cmd.getProjectId(), event.getProjectId());
        }
    }

    @Nested
    @DisplayName("throw ISE when invoked method produces")
    class ThrowWhenProduces {

        @Test
        @DisplayName("no events")
        void noEvents() {
            HandlerReturnsEmptyList handlerObject = new HandlerReturnsEmptyList();
            CommandHandlerMethod handler = from(handlerObject.getHandler());
            RefCreateProject cmd = createProject();

            assertThrows(IllegalStateException.class,
                         () -> handler.invoke(handlerObject, cmd, emptyContext));
        }

        @Test
        @DisplayName("`Empty` event")
        void emptyEvent() {
            HandlerReturnsEmpty handlerObject = new HandlerReturnsEmpty();
            CommandHandlerMethod handler = from(handlerObject.getHandler());
            RefCreateProject cmd = createProject();

            assertThrows(IllegalStateException.class,
                         () -> handler.invoke(handlerObject, cmd, emptyContext));
        }
    }

    @Test
    @DisplayName("allow `ProcessManager` methods producing `Empty`")
    void allowEmptyInProcman() {
        RefCreateProject commandMessage = createProject();
        ProcessManager<ProjectId, ?, ?> entity =
                new ProcessManagerDoingNothing(commandMessage.getProjectId());
        CommandEnvelope cmd = requestFactory.createEnvelope(commandMessage);
        List<Event> events = PmDispatcher.dispatch(entity, cmd);
        assertEmpty(events);
    }

    @Nested
    @DisplayName("consider handler valid with")
    class ConsiderHandlerValidWith {

        @Test
        @DisplayName("one Message param")
        void messageParam() {
            Method handler = new ValidHandlerOneParam().getHandler();

            assertIsCommandHandler(handler, true);
        }

        @Test
        @DisplayName("one Message param and `List` return type")
        void messageParamAndListReturn() {
            Method handler = new ValidHandlerOneParamReturnsList().getHandler();

            assertIsCommandHandler(handler, true);
        }

        @Test
        @DisplayName("Message and Context params")
        void messageAndContextParam() {
            Method handler = new ValidHandlerTwoParams().getHandler();

            assertIsCommandHandler(handler, true);
        }

        @Test
        @DisplayName("Message and Context params, and `List` return type")
        void messageAndContextParamAndListReturn() {
            Method handler = new ValidHandlerTwoParamsReturnsList().getHandler();

            assertIsCommandHandler(handler, true);
        }

        @Test
        @DisplayName("non-public access")
        void nonPublicAccess() {
            Method method = new ValidHandlerButPrivate().getHandler();

            assertIsCommandHandler(method, true);
        }
    }

    @Nested
    @DisplayName("consider handler invalid with")
    class ConsiderHandlerInvalidWith {

        @Test
        @DisplayName("no annotation")
        void noAnnotation() {
            Method handler = new InvalidHandlerNoAnnotation().getHandler();

            assertIsCommandHandler(handler, false);
        }

        @Test
        @DisplayName("no params")
        void noParams() {
            Method handler = new InvalidHandlerNoParams().getHandler();

            assertIsCommandHandler(handler, false);
        }

        @Test
        @DisplayName("too many params")
        void tooManyParams() {
            Method handler = new InvalidHandlerTooManyParams().getHandler();

            assertIsCommandHandler(handler, false);
        }

        @Test
        @DisplayName("one invalid param")
        void oneInvalidParam() {
            Method handler = new InvalidHandlerOneNotMsgParam().getHandler();

            assertIsCommandHandler(handler, false);
        }

        @Test
        @DisplayName("first non-Message param")
        void firstNonMessageParam() {
            Method handler = new InvalidHandlerTwoParamsFirstInvalid().getHandler();

            assertIsCommandHandler(handler, false);
        }

        @Test
        @DisplayName("second non-Context param")
        void secondNonContextParam() {
            Method handler = new InvalidHandlerTwoParamsSecondInvalid().getHandler();

            assertIsCommandHandler(handler, false);
        }

        @Test
        @DisplayName("void return type")
        void voidReturnType() {
            Method handler = new InvalidHandlerReturnsVoid().getHandler();

            assertIsCommandHandler(handler, false);
        }
    }

    @Nested
    @DisplayName("set producer ID when dispatching to")
    class SetProducerId {

        @SuppressWarnings("CheckReturnValue") // no need as the call to dispatch() throws
        @Test
        @DisplayName("command handler")
        void onDispatchToHandler() {
            AbstractCommandHandler handler = new RejectingHandler();
            CommandEnvelope envelope = requestFactory.createEnvelope(createProject());
            try {
                handler.dispatch(envelope);
            } catch (HandlerMethodFailedException e) {
                assertCauseAndId(e, handler.getId());
            }
        }

        @SuppressWarnings("CheckReturnValue") // no need as the call to dispatchCommand() throws
        @Test
        @DisplayName("entity")
        void onDispatchToEntity() {
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

        private void assertCauseAndId(HandlerMethodFailedException e, Object handlerId) {
            Throwable cause = getRootCause(e);

            assertTrue(cause instanceof ThrowableMessage);
            ThrowableMessage thrown = (ThrowableMessage) cause;

            assertTrue(thrown.producerId()
                             .isPresent());
            assertEquals(handlerId, Identifier.unpack(thrown.producerId()
                                                            .get()));
        }
    }

    @Test
    @DisplayName("throw ISE when dispatching command of non-handled type")
    void notDispatchNonHandledCmd() {
        AbstractCommandHandler handler = new ValidHandlerOneParam();
        CommandEnvelope cmd = requestFactory.createEnvelope(startProject());

        assertThrows(IllegalStateException.class, () -> handler.dispatch(cmd));
    }

    private static void assertIsCommandHandler(Method handler, boolean isHandler) {
        assertEquals(isHandler,
                     CommandHandlerMethod.factory()
                                         .getPredicate()
                                         .test(handler));
    }
}
