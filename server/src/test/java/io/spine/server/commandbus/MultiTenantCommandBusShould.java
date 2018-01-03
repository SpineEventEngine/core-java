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
import org.junit.Test;

import java.util.Set;

import static io.spine.core.CommandValidationError.TENANT_UNKNOWN;
import static io.spine.core.CommandValidationError.UNSUPPORTED_COMMAND;
import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.server.commandbus.Given.ACommand.addTask;
import static io.spine.server.commandbus.Given.ACommand.createProject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MultiTenantCommandBusShould extends AbstractCommandBusTestSuite {

    public MultiTenantCommandBusShould() {
        super(true);
    }

    @Test
    public void allow_to_specify_rejection_bus_via_builder() {
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
    public void have_log() {
        assertNotNull(Log.log());
    }

    @Test
    public void have_rejection_bus_if_no_custom_set() {
        final CommandBus bus = CommandBus.newBuilder()
                                           .setCommandStore(commandStore)
                                           .build();
        assertNotNull(bus.rejectionBus());
    }

    @Test
    public void close_CommandStore_when_closed() throws Exception {
        commandBus.close();

        verify(commandStore).close();
    }

    @Test
    public void close_RejectionBus_when_closed() throws Exception {
        commandBus.close();

        verify(rejectionBus).close();
    }

    @Test
    public void shutdown_CommandScheduler_when_closed() throws Exception {
        commandBus.close();

        verify(scheduler).shutdown();
    }

    /*
     * Command validation tests.
     ***************************/

    @Test
    public void verify_tenant_id_attribute_if_multitenant() {
        commandBus.register(createProjectHandler);
        final Command cmd = newCommandWithoutTenantId();

        commandBus.post(cmd, observer);

        checkCommandError(observer.firstResponse(),
                          TENANT_UNKNOWN,
                          InvalidCommandException.class,
                          cmd);
    }

    @Test
    public void return_UnsupportedCommandException_when_there_is_neither_handler_nor_dispatcher() {
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
    public void register_command_dispatcher() {
        commandBus.register(new AddTaskDispatcher());

        commandBus.post(addTask(), observer);

        assertTrue(observer.isCompleted());
    }

    @Test
    public void unregister_command_dispatcher() {
        final CommandDispatcher<Message> dispatcher = new AddTaskDispatcher();
        commandBus.register(dispatcher);
        commandBus.unregister(dispatcher);

        commandBus.post(addTask(), observer);

        assertEquals(ERROR, observer.firstResponse()
                                    .getStatus()
                                    .getStatusCase());
    }

    @Test
    public void register_command_handler() {
        commandBus.register(new CreateProjectHandler());

        commandBus.post(createProject(), observer);

        assertTrue(observer.isCompleted());
    }

    @Test
    public void unregister_command_handler() {
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
     * Test of illegal arguments for post()
     ***************************************/

    @Test
    public void not_accept_nulls() throws NoSuchMethodException {
        new NullPointerTester()
                .setDefault(Command.class, Command.getDefaultInstance())
                .setDefault(StreamObserver.class, StreamObservers.noOpObserver())
                .testAllPublicInstanceMethods(commandBus);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_default_command() {
        commandBus.post(Command.getDefaultInstance(), observer);
    }

    /*
     * Command processing tests
     ***************************/

    @Test
    public void post_command_and_return_OK_response() {
        commandBus.register(createProjectHandler);

        final Command command = createProject();
        commandBus.post(command, observer);

        checkResult(command);
    }

    @Test
    public void store_command_when_posted() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProject();

        commandBus.post(cmd, observer);

        verify(commandStore).store(cmd);
    }

    @Test
    public void store_invalid_command_with_error_status() {
        commandBus.register(createProjectHandler);
        final Command cmd = newCommandWithoutContext();

        commandBus.post(cmd, observer);

        verify(commandStore).store(eq(cmd), isA(Error.class));
    }

    @Test
    public void invoke_handler_when_command_posted() {
        commandBus.register(createProjectHandler);

        commandBus.post(createProject(), observer);

        assertTrue(createProjectHandler.wasHandlerInvoked());
    }

    @Test
    public void invoke_dispatcher_when_command_posted() {
        final AddTaskDispatcher dispatcher = new AddTaskDispatcher();
        commandBus.register(dispatcher);

        commandBus.post(addTask(), observer);

        assertTrue(dispatcher.wasDispatcherInvoked());
    }

    @Test
    public void return_supported_classes() {
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
