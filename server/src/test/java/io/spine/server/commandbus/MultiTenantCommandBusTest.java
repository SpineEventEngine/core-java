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
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.base.Error;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandValidationError;
import io.spine.core.MessageEnvelope;
import io.spine.grpc.StreamObservers;
import io.spine.server.command.CommandHandler;
import io.spine.server.rejection.RejectionBus;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.CmdCreateProject;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("Multi tenant CommandBus should")
class MultiTenantCommandBusTest extends AbstractCommandBusTestSuite {

    MultiTenantCommandBusTest() {
        super(true);
    }

    /*
     * Test of illegal arguments for post()
     ***************************************/

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

    @Test
    @DisplayName("allow to specify rejection bus via builder")
    void setRejectionBusViaBuilder() {
        final RejectionBus expectedRejectionBus = mock(RejectionBus.class);
        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setCommandStore(commandStore)
                                                .setRejectionBus(expectedRejectionBus)
                                                .build();
        assertNotNull(commandBus);

        final RejectionBus actualRejectionBus = commandBus.rejectionBus();
        assertEquals(expectedRejectionBus, actualRejectionBus);
    }

    @Test
    @DisplayName("have log")
    void haveLog() {
        assertNotNull(Log.log());
    }

    @Test
    @DisplayName("have RejectionBus if no custom one was set")
    void haveRejectionBusIfNoCustomSet() {
        final CommandBus bus = CommandBus.newBuilder()
                                         .setCommandStore(commandStore)
                                         .build();
        assertNotNull(bus.rejectionBus());
    }

    @Test
    @DisplayName("close CommandStore when closed")
    void closeCommandStoreWhenClosed() throws Exception {
        commandBus.close();

        verify(commandStore).close();
    }

    @Test
    @DisplayName("close RejectionBus when closed")
    void closeRejectionBusWhenClosed() throws Exception {
        commandBus.close();

        verify(rejectionBus).close();
    }

    @Test
    @DisplayName("shutdown CommandScheduler when closed")
    void shutdownCommandSchedulerWhenClosed() throws Exception {
        commandBus.close();

        verify(scheduler).shutdown();
    }

    /*
     * Command validation tests.
     ***************************/

    @Test
    @DisplayName("verify tenant id attribute if multitenant")
    void requireTenantIdIfMultitenant() {
        commandBus.register(createProjectHandler);
        final Command cmd = newCommandWithoutTenantId();

        commandBus.post(cmd, observer);

        checkCommandError(observer.firstResponse(),
                          TENANT_UNKNOWN,
                          InvalidCommandException.class,
                          cmd);
    }

    @Test
    @DisplayName("return UnsupportedCommandException when there is neither handler nor dispatcher")
    void returnExceptionOnNoHandlerAndDispatcher() {
        final Command command = addTask();
        commandBus.post(command, observer);

        checkCommandError(observer.firstResponse(),
                          UNSUPPORTED_COMMAND,
                          CommandValidationError.getDescriptor().getFullName(),
                          command);
    }

    /*
     * Registration and un-registration tests
     *****************************************/
    @Test
    @DisplayName("register command dispatcher")
    void registerCommandDispatcher() {
        commandBus.register(new AddTaskDispatcher());

        commandBus.post(addTask(), observer);

        assertTrue(observer.isCompleted());
    }

    @Test
    @DisplayName("unregister command dispatcher")
    void unregisterCommandDispatcher() {
        final CommandDispatcher<Message> dispatcher = new AddTaskDispatcher();
        commandBus.register(dispatcher);
        commandBus.unregister(dispatcher);

        commandBus.post(addTask(), observer);

        assertEquals(ERROR, observer.firstResponse()
                                    .getStatus()
                                    .getStatusCase());
    }

    @Test
    @DisplayName("register command handler")
    void registerCommandHandler() {
        commandBus.register(new CreateProjectHandler());

        commandBus.post(createProject(), observer);

        assertTrue(observer.isCompleted());
    }

    @Test
    @DisplayName("unregister command handler")
    void unregisterCommandHandler() {
        final CommandHandler handler = newCommandHandler();

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

    /*
     * Command processing tests
     ***************************/

    @Test
    @DisplayName("post command and return OK response")
    void postCommandAndReturnOkResponse() {
        commandBus.register(createProjectHandler);

        final Command command = createProject();
        commandBus.post(command, observer);

        checkResult(command);
    }

    @Test
    @DisplayName("store command when posted")
    void storeCommandWhenPosted() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProject();

        commandBus.post(cmd, observer);

        verify(commandStore).store(cmd);
    }

    @Test
    @DisplayName("store invalid command with error status")
    void storeInvalidCommandWithErrorStatus() {
        commandBus.register(createProjectHandler);
        final Command cmd = newCommandWithoutContext();

        commandBus.post(cmd, observer);

        verify(commandStore).store(eq(cmd), isA(Error.class));
    }

    @Test
    @DisplayName("invoke handler when command posted")
    void invokeHandlerWhenCommandPosted() {
        commandBus.register(createProjectHandler);

        commandBus.post(createProject(), observer);

        assertTrue(createProjectHandler.wasHandlerInvoked());
    }

    @Test
    @DisplayName("invoke dispatcher when command posted")
    void invokeDispatcherWhenCommandPosted() {
        final AddTaskDispatcher dispatcher = new AddTaskDispatcher();
        commandBus.register(dispatcher);

        commandBus.post(addTask(), observer);

        assertTrue(dispatcher.wasDispatcherInvoked());
    }

    @Test
    @DisplayName("return supported classes")
    void returnSupportedClasses() {
        commandBus.register(createProjectHandler);
        commandBus.register(new AddTaskDispatcher());

        final Set<CommandClass> cmdClasses = commandBus.getRegisteredCommandClasses();

        assertTrue(cmdClasses.contains(CommandClass.of(CmdCreateProject.class)));
        assertTrue(cmdClasses.contains(CommandClass.of(CmdAddTask.class)));
    }

    /*
     * Test utility methods.
     ***********************/

    /**
     * The dispatcher that remembers that
     * {@link CommandDispatcher#dispatch(MessageEnvelope) dispatch()}
     * was called.
     */
    private static class AddTaskDispatcher implements CommandDispatcher<Message> {

        private boolean dispatcherInvoked = false;

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(CmdAddTask.class);
        }

        @Override
        public Message dispatch(CommandEnvelope envelope) {
            dispatcherInvoked = true;
            return Empty.getDefaultInstance();
        }

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }

        public boolean wasDispatcherInvoked() {
            return dispatcherInvoked;
        }
    }
}
