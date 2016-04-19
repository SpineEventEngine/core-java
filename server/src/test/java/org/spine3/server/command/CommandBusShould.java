/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.base.Errors;
import org.spine3.base.Responses;
import org.spine3.client.CommandFactory;
import org.spine3.client.test.TestCommandFactory;
import org.spine3.server.error.UnsupportedCommandException;
import org.spine3.server.event.EventBus;
import org.spine3.server.failure.FailureThrowable;
import org.spine3.server.type.CommandClass;
import org.spine3.test.failures.Failures;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.base.Responses.isUnsupportedCommand;
import static org.spine3.testdata.TestCommands.*;

@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public class CommandBusShould {

    private CommandBus commandBus;
    private CommandStore commandStore;
    private CommandFactory commandFactory;
    private CommandBus.ProblemLog log;
    private EventBus eventBus;

    @Before
    public void setUp() {
        commandStore = mock(CommandStore.class);

        commandBus = CommandBus.create(commandStore);
        log = mock(CommandBus.ProblemLog.class);
        commandBus.setProblemLog(log);
        eventBus = mock(EventBus.class);
        commandFactory = TestCommandFactory.newInstance(CommandBusShould.class);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_CommandStore_on_construction() {
        //noinspection ConstantConditions,ResultOfMethodCallIgnored
        CommandBus.create(null);
    }

    //
    // Test for empty handler
    //--------------------------

    private static class EmptyDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getCommandClasses() {
            return Collections.emptySet();
        }

        @Override
        public void dispatch(Command request) throws Exception {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_empty_dispatchers() {
        commandBus.register(new EmptyDispatcher());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_command_handlers_without_methods() {
        commandBus.register(new EmptyCommandHandler(eventBus));
    }

    @SuppressWarnings("EmptyClass")
    private static class EmptyCommandHandler extends CommandHandler {
        private EmptyCommandHandler(EventBus eventBus) {
            super(newUuid(), eventBus);
        }
    }

    //
    // Test for duplicate dispatchers
    //----------------------------------

    private static class TwoCommandDispatcher implements CommandDispatcher {

        @Override
        public Set<CommandClass> getCommandClasses() {
            return CommandClass.setOf(CreateProject.class, AddTask.class);
        }

        @Override
        public void dispatch(Command request) throws Exception {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_another_dispatcher_for_already_registered_commands() {
        commandBus.register(new TwoCommandDispatcher());
        commandBus.register(new TwoCommandDispatcher());
    }

    /**
     * Test for successful dispatcher registration.
     */
    @Test
    public void register_command_dispatcher() {
        commandBus.register(new CommandDispatcher() {
            @Override
            public Set<CommandClass> getCommandClasses() {
                return CommandClass.setOf(CreateProject.class, StartProject.class, AddTask.class);
            }

            @Override
            public void dispatch(Command request) throws Exception {
            }
        });

        final String projectId = newUuid();
        assertEquals(Responses.ok(), commandBus.validate(createProject(projectId)));
        assertEquals(Responses.ok(), commandBus.validate(startProject(projectId)));
        assertEquals(Responses.ok(), commandBus.validate(addTask(projectId)));
    }

    /**
     * Test that unregistering dispatcher makes commands unsupported.
     */
    @Test
    public void turn_commands_unsupported_when_dispatcher_unregistered() {
        final CommandDispatcher dispatcher = new CommandDispatcher() {
            @Override
            public Set<CommandClass> getCommandClasses() {
                return CommandClass.setOf(CreateProject.class, StartProject.class, AddTask.class);
            }

            @Override
            public void dispatch(Command request) throws Exception {
            }
        };

        commandBus.register(dispatcher);
        commandBus.unregister(dispatcher);

        final String projectId = newUuid();
        assertTrue(isUnsupportedCommand(commandBus.validate(createProject(projectId))));
        assertTrue(isUnsupportedCommand(commandBus.validate(startProject(projectId))));
        assertTrue(isUnsupportedCommand(commandBus.validate(addTask(projectId))));
    }

    //
    // Tests for not overriding handlers by dispatchers and vice versa
    //-------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_to_register_dispatcher_for_the_command_with_registered_handler() {

        final CommandHandler createProjectHandler = new CreateProjectHandler(eventBus);
        final CommandDispatcher createProjectDispatcher = new CreateProjectDispatcher();

        commandBus.register(createProjectHandler);

        commandBus.register(createProjectDispatcher);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_to_register_handler_for_the_command_with_registered_dispatcher() {

        final CommandHandler createProjectHandler = new CreateProjectHandler(eventBus);
        final CommandDispatcher createProjectDispatcher = new CreateProjectDispatcher();

        commandBus.register(createProjectDispatcher);

        commandBus.register(createProjectHandler);
    }

    private static class CreateProjectHandler extends CommandHandler {

        private boolean handlerInvoked = false;

        private CreateProjectHandler(EventBus eventBus) {
            super(newUuid(), eventBus);
        }

        @Assign
        public ProjectCreated handle(CreateProject command, CommandContext ctx)
                throws TestFailure, TestThrowable {
            handlerInvoked = true;
            return ProjectCreated.getDefaultInstance();
        }

        public boolean wasHandlerInvoked() {
            return handlerInvoked;
        }
    }

    private static class CreateProjectDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getCommandClasses() {
            return CommandClass.setOf(CreateProject.class);
        }

        @Override
        public void dispatch(Command request) throws Exception {
        }
    }

    @Test
    public void unregister_handler() {
        final CreateProjectHandler handler = new CreateProjectHandler(eventBus);
        commandBus.register(handler);
        commandBus.unregister(handler);
        final String projectId = newUuid();
        assertTrue(isUnsupportedCommand(commandBus.validate(createProject(projectId))));
    }

    @Test
    public void validate_commands_both_dispatched_and_handled() {
        final CreateProjectHandler handler = new CreateProjectHandler(eventBus);
        final AddTaskDispatcher dispatcher = new AddTaskDispatcher();
        commandBus.register(handler);
        commandBus.register(dispatcher);

        final String projectId = newUuid();
        assertEquals(Responses.ok(), commandBus.validate(createProject(projectId)));
        assertEquals(Responses.ok(), commandBus.validate(addTask(projectId)));
    }

    private static class AddTaskDispatcher implements CommandDispatcher {

        private boolean dispatcherInvoked = false;

        @Override
        public Set<CommandClass> getCommandClasses() {
            return CommandClass.setOf(AddTask.class);
        }

        @Override
        public void dispatch(Command request) throws Exception {
            dispatcherInvoked = true;
        }

        public boolean wasDispatcherInvoked() {
            return dispatcherInvoked;
        }
    }

    @Test // To improve coverage stats.
    public void have_log() {
        assertNotNull(CommandBus.log());
    }

    @Test
    public void close_CommandStore_when_closed() throws Exception {
        commandBus.close();

        verify(commandStore, times(1)).close();
    }

    @Test
    public void remove_all_handlers_on_close() throws Exception {
        final CreateProjectHandler handler = new CreateProjectHandler(eventBus);
        commandBus.register(handler);

        commandBus.close();
        assertTrue(isUnsupportedCommand(commandBus.validate(createProject(newUuid()))));
    }

    @Test
    public void invoke_handler_when_command_posted() {
        final CreateProjectHandler handler = new CreateProjectHandler(eventBus);
        commandBus.register(handler);

        final Command command = commandFactory.create(createProject(newUuid()));
        commandBus.post(command);

        assertTrue(handler.wasHandlerInvoked());
    }

    @Test
    public void invoke_dispatcher_when_command_posted() {
        final AddTaskDispatcher dispatcher = new AddTaskDispatcher();
        commandBus.register(dispatcher);

        final Command command = commandFactory.create(addTask(newUuid()));

        commandBus.post(command);

        assertTrue(dispatcher.wasDispatcherInvoked());
    }

    @Test(expected = UnsupportedCommandException.class)
    public void throw_exception_when_there_is_no_neither_handler_nor_dispatcher() {
        final Command command = commandFactory.create(addTask(newUuid()));

        commandBus.post(command);
    }

    @Test
    public void set_command_status_to_OK_when_handler_returns() {
        final CreateProjectHandler handler = new CreateProjectHandler(eventBus);
        commandBus.register(handler);
        final Command command = commandFactory.create(createProject(newUuid()));

        commandBus.post(command);

        // See that we called CommandStore only once with the right command ID.
        verify(commandStore, times(1)).setCommandStatusOk(command.getContext().getCommandId());
    }

    @Test
    public void set_command_status_to_error_when_dispatcher_throws() throws Exception {
        final CommandDispatcher throwingDispatcher = mock(CommandDispatcher.class);
        when(throwingDispatcher.getCommandClasses()).thenReturn(CommandClass.setOf(CreateProject.class));
        final IOException exception = new IOException("Unable to dispatch");
        doThrow(exception).when(throwingDispatcher)
                          .dispatch(any(Command.class));

        commandBus.register(throwingDispatcher);
        final Command command = commandFactory.create(createProject(newUuid()));

        commandBus.post(command);

        // Verify we updated the status.
        verify(commandStore, times(1)).updateStatus(eq(command.getContext()
                                                              .getCommandId()), eq(exception));
        // Verify we logged the error.
        verify(log, times(1)).errorDispatching(eq(exception), eq(command));
    }

    private static class TestFailure extends FailureThrowable {
        private static final long serialVersionUID = 1L;

        private TestFailure() {
            super(Failures.UnableToHandle.newBuilder()
                                         .setMessage(TestFailure.class.getName())
                                         .build());
        }
    }

    @SuppressWarnings("serial")
    private static class TestThrowable extends Throwable {
    }

    /**
     * A stub handler that throws passed `Throwable` in the command handler method.
     *
     * @see #set_command_status_to_failure_when_handler_throws_failure
     * @see #set_command_status_to_failure_when_handler_throws_exception
     * @see #set_command_status_to_failure_when_handler_throws_unknown_Throwable
     */
    private static class ThrowingCreateProjectHandler extends CommandHandler {

        private final Throwable throwable;

        protected ThrowingCreateProjectHandler(EventBus eventBus, Throwable throwable) {
            super(newUuid(), eventBus);
            this.throwable = throwable;
        }

        @Assign
        public ProjectCreated handle(CreateProject msg, CommandContext context) throws Throwable {
            //noinspection ProhibitedExceptionThrown
            throw throwable;
        }
    }

    @Test
    public void set_command_status_to_failure_when_handler_throws_failure() throws TestFailure, TestThrowable, InvocationTargetException {
        final FailureThrowable failure = new TestFailure();
        final CommandHandler handler = new ThrowingCreateProjectHandler(eventBus, failure);

        commandBus.register(handler);
        final Command command = commandFactory.create(createProject(newUuid()));
        final CommandId commandId = command.getContext()
                                           .getCommandId();
        final Message commandMessage = Commands.getMessage(command);

        commandBus.post(command);

        // Verify we updated the status.
        verify(commandStore, times(1)).updateStatus(eq(commandId), eq(failure.toMessage()));

        // Verify we logged the failure.
        verify(log, times(1)).failureHandling(eq(failure), eq(commandMessage), eq(commandId));
    }

    @Test
    public void set_command_status_to_failure_when_handler_throws_exception() throws TestFailure, TestThrowable {
        final RuntimeException exception = new IllegalStateException("handler throws");
        final CommandHandler handler = new ThrowingCreateProjectHandler(eventBus, exception);

        commandBus.register(handler);
        final Command command = commandFactory.create(createProject(newUuid()));
        final CommandId commandId = command.getContext()
                                           .getCommandId();
        final Message commandMessage = Commands.getMessage(command);

        commandBus.post(command);

        // Verify we updated the status.

        verify(commandStore, times(1)).updateStatus(eq(commandId), eq(exception));
        // Verify we logged the failure.
        verify(log, times(1)).errorHandling(eq(exception), eq(commandMessage), eq(commandId));
    }

    @Test
    public void set_command_status_to_failure_when_handler_throws_unknown_Throwable() throws TestFailure, TestThrowable {
        final Throwable throwable = new TestThrowable();
        final CommandHandler handler = new ThrowingCreateProjectHandler(eventBus, throwable);

        commandBus.register(handler);
        final Command command = commandFactory.create(createProject(newUuid()));
        final CommandId commandId = command.getContext()
                                           .getCommandId();
        final Message commandMessage = Commands.getMessage(command);

        commandBus.post(command);

        // Verify we updated the status.

        verify(commandStore, times(1)).updateStatus(eq(commandId), eq(Errors.fromThrowable(throwable)));
        // Verify we logged the failure.
        verify(log, times(1)).errorHandlingUnknown(eq(throwable), eq(commandMessage), eq(commandId));
    }
}
