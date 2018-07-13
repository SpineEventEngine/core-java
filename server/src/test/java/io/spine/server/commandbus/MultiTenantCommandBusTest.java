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

package io.spine.server.commandbus;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.base.Error;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandValidationError;
import io.spine.grpc.StreamObservers;
import io.spine.server.command.CommandHandler;
import io.spine.server.commandbus.given.MultitenantCommandBusTestEnv.AddTaskDispatcher;
import io.spine.server.rejection.RejectionBus;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.CmdCreateProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.spine.core.CommandValidationError.TENANT_UNKNOWN;
import static io.spine.core.CommandValidationError.UNSUPPORTED_COMMAND;
import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.server.commandbus.Given.ACommand.addTask;
import static io.spine.server.commandbus.Given.ACommand.createProject;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("Multitenant CommandBus should")
class MultiTenantCommandBusTest extends AbstractCommandBusTestSuite {

    MultiTenantCommandBusTest() {
        super(true);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() throws NoSuchMethodException {
        new NullPointerTester()
                .setDefault(Command.class, Command.getDefaultInstance())
                .setDefault(StreamObserver.class, StreamObservers.noOpObserver())
                .testAllPublicInstanceMethods(commandBus);
    }

    @Test
    @DisplayName("not accept default command on post")
    void notAcceptDefaultCommand() {
        assertThrows(IllegalArgumentException.class,
                     () -> commandBus.post(Command.getDefaultInstance(), observer));
    }

    @Nested
    @DisplayName("have RejectionBus")
    class HaveRejectionBus {

        @Test
        @DisplayName("default if no custom one was specified")
        void setToDefault() {
            CommandBus bus = CommandBus.newBuilder()
                                             .setCommandStore(commandStore)
                                             .build();
            assertNotNull(bus.rejectionBus());
        }

        @Test
        @DisplayName("custom if one was specified via Builder")
        void setToCustom() {
            RejectionBus expectedRejectionBus = mock(RejectionBus.class);
            CommandBus commandBus = CommandBus.newBuilder()
                                                    .setCommandStore(commandStore)
                                                    .setRejectionBus(expectedRejectionBus)
                                                    .build();
            assertNotNull(commandBus);

            RejectionBus actualRejectionBus = commandBus.rejectionBus();
            assertEquals(expectedRejectionBus, actualRejectionBus);
        }
    }

    @Test
    @DisplayName("have log")
    void haveLog() {
        assertNotNull(Log.log());
    }

    @Nested
    @DisplayName("when closed, shutdown")
    class ShutdownWhenClosed {

        @Test
        @DisplayName("CommandStore")
        void commandStore() throws Exception {
            commandBus.close();

            verify(commandStore).close();
        }

        @Test
        @DisplayName("RejectionBus")
        void rejectionBus() throws Exception {
            commandBus.close();

            verify(rejectionBus).close();
        }

        @Test
        @DisplayName("CommandScheduler")
        void commandScheduler() throws Exception {
            commandBus.close();

            verify(scheduler).shutdown();
        }
    }

    @Test
    @DisplayName("verify tenant ID attribute if is multitenant")
    void requireTenantId() {
        commandBus.register(createProjectHandler);
        Command cmd = newCommandWithoutTenantId();

        commandBus.post(cmd, observer);

        checkCommandError(observer.firstResponse(),
                          TENANT_UNKNOWN,
                          InvalidCommandException.class,
                          cmd);
    }

    @Test
    @DisplayName("state command not supported when there is neither handler nor dispatcher for it")
    void requireHandlerOrDispatcher() {
        Command command = addTask();
        commandBus.post(command, observer);

        checkCommandError(observer.firstResponse(),
                          UNSUPPORTED_COMMAND,
                          CommandValidationError.getDescriptor().getFullName(),
                          command);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("command dispatcher")
        void commandDispatcher() {
            commandBus.register(new AddTaskDispatcher());

            commandBus.post(addTask(), observer);

            assertTrue(observer.isCompleted());
        }

        @Test
        @DisplayName("command handler")
        void commandHandler() {
            commandBus.register(new CreateProjectHandler());

            commandBus.post(createProject(), observer);

            assertTrue(observer.isCompleted());
        }
    }

    @Nested
    @DisplayName("unregister")
    class Unregister {

        @Test
        @DisplayName("command dispatcher")
        void commandDispatcher() {
            CommandDispatcher<Message> dispatcher = new AddTaskDispatcher();
            commandBus.register(dispatcher);
            commandBus.unregister(dispatcher);

            commandBus.post(addTask(), observer);

            assertEquals(ERROR, observer.firstResponse()
                                        .getStatus()
                                        .getStatusCase());
        }

        @Test
        @DisplayName("command handler")
        void commandHandler() {
            CommandHandler handler = newCommandHandler();

            commandBus.register(handler);
            commandBus.unregister(handler);

            commandBus.post(createProject(), observer);

            assertEquals(ERROR, observer.firstResponse()
                                        .getStatus()
                                        .getStatusCase());
        }

        CreateProjectHandler newCommandHandler() {
            return new CreateProjectHandler();
        }
    }

    @Test
    @DisplayName("post command and return `OK` response")
    void postCommand() {
        commandBus.register(createProjectHandler);

        Command command = createProject();
        commandBus.post(command, observer);

        checkResult(command);
    }

    @Nested
    @DisplayName("store")
    class StoreWhenPosted {

        @Test
        @DisplayName("valid command when it is posted")
        void validCommand() {
            commandBus.register(createProjectHandler);
            Command cmd = createProject();

            commandBus.post(cmd, observer);

            verify(commandStore).store(cmd);
        }

        @Test
        @DisplayName("invalid command with `ERROR` status")
        void invalidCommand() {
            commandBus.register(createProjectHandler);
            Command cmd = newCommandWithoutContext();

            commandBus.post(cmd, observer);

            verify(commandStore).store(eq(cmd), isA(Error.class));
        }
    }

    @Nested
    @DisplayName("when command is posted, invoke")
    class Invoke {

        @Test
        @DisplayName("command handler")
        void commandHandler() {
            commandBus.register(createProjectHandler);

            commandBus.post(createProject(), observer);

            assertTrue(createProjectHandler.wasHandlerInvoked());
        }

        @Test
        @DisplayName("command dispatcher")
        void commandDispatcher() {
            AddTaskDispatcher dispatcher = new AddTaskDispatcher();
            commandBus.register(dispatcher);

            commandBus.post(addTask(), observer);

            assertTrue(dispatcher.wasDispatcherInvoked());
        }
    }

    @Test
    @DisplayName("expose supported classes")
    void exposeSupportedClasses() {
        commandBus.register(createProjectHandler);
        commandBus.register(new AddTaskDispatcher());

        Set<CommandClass> cmdClasses = commandBus.getRegisteredCommandClasses();

        assertTrue(cmdClasses.contains(CommandClass.of(CmdCreateProject.class)));
        assertTrue(cmdClasses.contains(CommandClass.of(CmdAddTask.class)));
    }
}
