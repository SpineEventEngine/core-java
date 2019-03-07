/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import io.spine.base.CommandMessage;
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.model.given.handler.HandlerReturnsEmptyList;
import io.spine.server.command.model.given.handler.HandlerReturnsNothing;
import io.spine.server.command.model.given.handler.InvalidHandlerNoAnnotation;
import io.spine.server.command.model.given.handler.InvalidHandlerNoParams;
import io.spine.server.command.model.given.handler.InvalidHandlerOneNotMsgParam;
import io.spine.server.command.model.given.handler.InvalidHandlerReturnsVoid;
import io.spine.server.command.model.given.handler.InvalidHandlerTooManyParams;
import io.spine.server.command.model.given.handler.InvalidHandlerTwoParamsFirstInvalid;
import io.spine.server.command.model.given.handler.InvalidHandlerTwoParamsSecondInvalid;
import io.spine.server.command.model.given.handler.ProcessManagerDoingNothing;
import io.spine.server.command.model.given.handler.RejectingAggregate;
import io.spine.server.command.model.given.handler.RejectingHandler;
import io.spine.server.command.model.given.handler.ValidHandlerButPrivate;
import io.spine.server.command.model.given.handler.ValidHandlerOneParam;
import io.spine.server.command.model.given.handler.ValidHandlerOneParamReturnsList;
import io.spine.server.command.model.given.handler.ValidHandlerTwoParams;
import io.spine.server.command.model.given.handler.ValidHandlerTwoParamsReturnsList;
import io.spine.server.model.HandlerMethodFailedException;
import io.spine.server.model.declare.SignatureMismatchException;
import io.spine.server.procman.ProcessManager;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.reflect.ProjectId;
import io.spine.test.reflect.command.RefCreateProject;
import io.spine.test.reflect.event.RefProjectCreated;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.aggregate.AggregateMessageDispatcher;
import io.spine.testing.server.model.ModelTests;
import io.spine.testing.server.procman.PmDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.collect.testing.Helpers.assertEmpty;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.model.given.Given.CommandMessage.createProject;
import static io.spine.server.model.given.Given.CommandMessage.startProject;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("CommandHandlerMethod should")
class CommandHandlerMethodTest {

    private static final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(CommandHandlerMethodTest.class);

    private static final CommandContext emptyContext = CommandContext.getDefaultInstance();

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(CommandEnvelope.class, generate())
                .setDefault(CommandContext.class, emptyContext)
                .setDefault(Any.class, Any.getDefaultInstance())
                .testAllPublicStaticMethods(CommandHandlerMethod.class);
    }

    private static CommandEnvelope generate() {
        return CommandEnvelope.of(requestFactory.generateCommand());
    }

    private static CommandEnvelope newCommand(CommandMessage msg) {
        return CommandEnvelope.of(requestFactory.createCommand(msg));
    }

    @Nested
    @MuteLogging /* Signature mismatch warnings are expected. */
    @DisplayName("invoke handler method which returns")
    class InvokeHandlerMethod {

        @Test
        @DisplayName("one Message")
        void returningMessage() {
            ValidHandlerTwoParams handlerObject = spy(new ValidHandlerTwoParams());

            Optional<CommandHandlerMethod> createdMethod =
                    new CommandHandlerSignature().create(handlerObject.getHandler());
            assertTrue(createdMethod.isPresent());
            CommandHandlerMethod handler = createdMethod.get();
            RefCreateProject cmd = createProject();
            CommandEnvelope envelope = envelope(cmd);

            CommandHandlerMethod.Result result = handler.invoke(handlerObject, envelope);
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
            Optional<CommandHandlerMethod> method =
                    new CommandHandlerSignature().create(handlerObject.getHandler());
            assertTrue(method.isPresent());
            CommandHandlerMethod handler = method.get();
            RefCreateProject cmd = createProject();
            CommandEnvelope envelope = envelope(cmd);

            CommandHandlerMethod.Result result = handler.invoke(handlerObject, envelope);
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
            Optional<CommandHandlerMethod> method =
                    new CommandHandlerSignature().create(handlerObject.getHandler());
            assertTrue(method.isPresent());
            CommandHandlerMethod handler = method.get();
            RefCreateProject cmd = createProject();
            CommandEnvelope envelope = envelope(cmd);

            assertThrows(IllegalStateException.class,
                         () -> handler.invoke(handlerObject, envelope));
        }

        @Test
        @DisplayName("`Nothing` event")
        void emptyEvent() {
            HandlerReturnsNothing handlerObject = new HandlerReturnsNothing();
            Optional<CommandHandlerMethod> method =
                    new CommandHandlerSignature().create(handlerObject.getHandler());
            assertTrue(method.isPresent());
            CommandHandlerMethod handler = method.get();
            RefCreateProject cmd = createProject();
            CommandEnvelope envelope = envelope(cmd);

            assertThrows(IllegalStateException.class,
                         () -> handler.invoke(handlerObject, envelope));
        }
    }

    @Test
    @DisplayName("allow `ProcessManager` methods producing `Empty`")
    void allowEmptyInProcman() {
        RefCreateProject commandMessage = createProject();
        ProcessManager<String, ?, ?> entity =
                new ProcessManagerDoingNothing(commandMessage.getProjectId()
                                                             .getId());
        CommandEnvelope cmd = newCommand(commandMessage);
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

            assertIsCommandHandler(handler);
        }

        @Test
        @MuteLogging /* Signature mismatch warnings are expected. */
        @DisplayName("one Message param and `List` return type")
        void messageParamAndListReturn() {
            Method handler = new ValidHandlerOneParamReturnsList().getHandler();

            assertIsCommandHandler(handler);
        }

        @Test
        @MuteLogging /* Signature mismatch warnings are expected. */
        @DisplayName("Message and Context params")
        void messageAndContextParam() {
            Method handler = new ValidHandlerTwoParams().getHandler();

            assertIsCommandHandler(handler);
        }

        @Test
        @DisplayName("Message and Context params, and `List` return type")
        void messageAndContextParamAndListReturn() {
            Method handler = new ValidHandlerTwoParamsReturnsList().getHandler();

            assertIsCommandHandler(handler);
        }

        @Test
        @MuteLogging /* Signature mismatch warnings are expected. */
        @DisplayName("non-public access")
        void nonPublicAccess() {
            Method method = new ValidHandlerButPrivate().getHandler();

            assertIsCommandHandler(method);
        }
    }

    @Nested
    @DisplayName("consider handler invalid with")
    class ConsiderHandlerInvalidWith {

        @Test
        @DisplayName("no annotation")
        void noAnnotation() {
            Method handler = new InvalidHandlerNoAnnotation().getHandler();
            assertFalse(new CommandHandlerSignature().matches(handler));
        }

        @Test
        @DisplayName("no params")
        void noParams() {
            assertThrows(SignatureMismatchException.class, InvalidHandlerNoParams::new);
        }

        @Test
        @DisplayName("too many params")
        void tooManyParams() {
            assertThrows(SignatureMismatchException.class, InvalidHandlerTooManyParams::new);
        }

        @Test
        @DisplayName("one invalid param")
        void oneInvalidParam() {
            assertThrows(SignatureMismatchException.class, InvalidHandlerOneNotMsgParam::new);
        }

        @Test
        @DisplayName("first non-Message param")
        void firstNonMessageParam() {
            assertThrows(SignatureMismatchException.class,
                         InvalidHandlerTwoParamsFirstInvalid::new);
        }

        @Test
        @DisplayName("second non-Context param")
        void secondNonContextParam() {
            assertThrows(SignatureMismatchException.class,
                         InvalidHandlerTwoParamsSecondInvalid::new);
        }

        @Test
        @DisplayName("void return type")
        void voidReturnType() {
            assertThrows(SignatureMismatchException.class, InvalidHandlerReturnsVoid::new);
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
            CommandEnvelope envelope = newCommand(createProject());
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
            CommandEnvelope cmd = newCommand(commandMessage);
            try {
                AggregateMessageDispatcher.dispatchCommand(entity, cmd);
            } catch (HandlerMethodFailedException e) {
                assertCauseAndId(e, entity.id());
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
        CommandEnvelope cmd = newCommand(startProject());

        assertThrows(IllegalStateException.class, () -> handler.dispatch(cmd));
    }

    private static void assertIsCommandHandler(Method handler) {
        assertTrue(new CommandHandlerSignature().matches(handler));
    }

    private static CommandEnvelope envelope(Message commandMessage) {
        Any cmd = pack(commandMessage);
        Command command = Command
                .newBuilder()
                .setMessage(cmd)
                .build();
        CommandEnvelope envelope = CommandEnvelope.of(command);
        return envelope;
    }
}
