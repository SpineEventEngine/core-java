/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.command;

import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandClass;
import org.spine3.base.CommandEnvelope;
import org.spine3.base.Error;
import org.spine3.base.Response;
import org.spine3.server.command.error.InvalidCommandException;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.test.Tests;
import org.spine3.test.command.AddTask;
import org.spine3.test.command.CreateProject;

import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.verify;
import static org.spine3.base.CommandValidationError.TENANT_UNKNOWN;
import static org.spine3.base.CommandValidationError.UNSUPPORTED_COMMAND;
import static org.spine3.server.command.Given.Command.addTask;
import static org.spine3.server.command.Given.Command.createProject;

public class MultitenantCommandBusShould extends AbstractCommandBusTestSuite {

    public MultitenantCommandBusShould() {
        super(true);
    }

    @Test
    public void have_log() {
        assertNotNull(Log.log());
    }

    @Test
    public void have_command_status_service() {
        assertNotNull(commandBus.getCommandStatusService());
    }

    @Test
    public void close_CommandStore_when_closed() throws Exception {
        commandBus.close();

        verify(commandStore).close();
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

        commandBus.post(cmd, responseObserver);

        checkCommandError(responseObserver.getThrowable(),
                          TENANT_UNKNOWN,
                          InvalidCommandException.class,
                          cmd);

        assertTrue(responseObserver.getResponses().isEmpty());
    }

    @Test
    public void return_UnsupportedCommandException_when_there_is_neither_handler_nor_dispatcher() {
        final Command command = addTask();
        commandBus.post(command, responseObserver);

        checkCommandError(responseObserver.getThrowable(),
                          UNSUPPORTED_COMMAND,
                          UnsupportedCommandException.class, command);
        assertTrue(responseObserver.getResponses()
                                   .isEmpty());
    }

    /*
     * Registration and un-registration tests
     *****************************************/
    @Test
    public void register_command_dispatcher() {
        commandBus.register(new AddTaskDispatcher());

        commandBus.post(addTask(), responseObserver);

        assertTrue(responseObserver.isCompleted());
    }

    @Test
    public void unregister_command_dispatcher() {
        final CommandDispatcher dispatcher = new AddTaskDispatcher();
        commandBus.register(dispatcher);
        commandBus.unregister(dispatcher);

        commandBus.post(addTask(), responseObserver);

        assertTrue(responseObserver.isError());
    }

    @Test
    public void register_command_handler() {
        commandBus.register(new CreateProjectHandler());

        commandBus.post(createProject(), responseObserver);

        assertTrue(responseObserver.isCompleted());
    }

    @Test
    public void unregister_command_handler() {
        final CommandHandler handler = newCommandHandler();

        commandBus.register(handler);
        commandBus.unregister(handler);

        commandBus.post(createProject(), responseObserver);

        assertTrue(responseObserver.isError());
    }

    CreateProjectHandler newCommandHandler() {
        return new CreateProjectHandler();
    }

    /*
     * Test of illegal arguments for post()
     ***************************************/

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_command() {
        commandBus.post(Tests.<Command>nullRef(), responseObserver);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_observer() {
        commandBus.post(createProject(), Tests.<StreamObserver<Response>>nullRef());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_default_command() {
        commandBus.post(Command.getDefaultInstance(), responseObserver);
    }

    /*
     * Command processing tests
     ***************************/

    @Test
    public void post_command_and_return_OK_response() {
        commandBus.register(createProjectHandler);

        commandBus.post(createProject(), responseObserver);

        responseObserver.assertResponseOkAndCompleted();
    }

    @Test
    public void store_command_when_posted() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProject();

        commandBus.post(cmd, responseObserver);

        verify(commandStore).store(cmd);
    }

    @Test
    public void store_invalid_command_with_error_status() {
        commandBus.register(createProjectHandler);
        final Command cmd = newCommandWithoutContext();

        commandBus.post(cmd, responseObserver);

        verify(commandStore).store(eq(cmd), isA(Error.class));
    }

    @Test
    public void invoke_handler_when_command_posted() {
        commandBus.register(createProjectHandler);

        commandBus.post(createProject(), responseObserver);

        assertTrue(createProjectHandler.wasHandlerInvoked());
    }

    @Test
    public void invoke_dispatcher_when_command_posted() {
        final AddTaskDispatcher dispatcher = new AddTaskDispatcher();
        commandBus.register(dispatcher);

        commandBus.post(addTask(), responseObserver);

        assertTrue(dispatcher.wasDispatcherInvoked());
    }

    @Test
    public void return_supported_classes() {
        commandBus.register(createProjectHandler);
        commandBus.register(new AddTaskDispatcher());

        final Set<CommandClass> cmdClasses = commandBus.getRegisteredCommandClasses();

        assertTrue(cmdClasses.contains(CommandClass.of(CreateProject.class)));
        assertTrue(cmdClasses.contains(CommandClass.of(AddTask.class)));
    }

    /*
     * Test utility methods.
     ***********************/

    /**
     * The dispatcher that remembers that
     * {@link CommandDispatcher#dispatch(org.spine3.base.MessageEnvelope) dispatch()} was called.
     */
    private static class AddTaskDispatcher implements CommandDispatcher {

        private boolean dispatcherInvoked = false;

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(AddTask.class);
        }

        @Override
        public void dispatch(CommandEnvelope envelope) {
            dispatcherInvoked = true;
        }

        public boolean wasDispatcherInvoked() {
            return dispatcherInvoked;
        }
    }
}
