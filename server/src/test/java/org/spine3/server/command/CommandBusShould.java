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

import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.base.Errors;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.client.CommandFactory;
import org.spine3.client.test.TestCommandFactory;
import org.spine3.server.error.UnsupportedCommandException;
import org.spine3.server.event.EventBus;
import org.spine3.server.failure.FailureThrowable;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.CommandClass;
import org.spine3.server.validate.MessageValidator;
import org.spine3.test.failures.Failures;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.testdata.TestEventFactory;
import org.spine3.validate.options.ConstraintViolation;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.base.Responses.isInvalidCommand;
import static org.spine3.base.Responses.isUnsupportedCommand;
import static org.spine3.protobuf.Durations.milliseconds;
import static org.spine3.protobuf.Durations.minutes;
import static org.spine3.testdata.TestCommands.*;
import static org.spine3.testdata.TestContextFactory.createCommandContext;

@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods", "OverlyCoupledClass"})
public class CommandBusShould {

    private CommandBus commandBus;
    private CommandStore commandStore;
    private CommandFactory commandFactory;
    private CommandBus.ProblemLog log;
    private EventBus eventBus;
    private ExecutorCommandScheduler scheduler;
    private CreateProjectHandler handler;

    @Before
    public void setUp() {
        final InMemoryStorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        commandStore = spy(new CommandStore(storageFactory.createCommandStorage()));
        scheduler = spy(new ExecutorCommandScheduler());
        commandBus = newCommandBus(commandStore, scheduler);
        log = spy(new CommandBus.ProblemLog());
        commandBus.setProblemLog(log);
        eventBus = spy(TestEventFactory.newEventBus(storageFactory));
        commandFactory = TestCommandFactory.newInstance(CommandBusShould.class);
        handler = new CreateProjectHandler(newUuid(), eventBus);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_CommandStore_on_construction() {
        // noinspection ConstantConditions
        CommandBus.newBuilder()
                  .setCommandStore(null)
                  .build();
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
        commandBus.register(new EmptyCommandHandler(newUuid(), eventBus));
    }

    private static class EmptyCommandHandler extends CommandHandler {
        protected EmptyCommandHandler(String id, EventBus eventBus) {
            super(id, eventBus);
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
        assertEquals(Responses.ok(), commandBus.validate(createProjectMsg(projectId)));
        assertEquals(Responses.ok(), commandBus.validate(startProjectMsg(projectId)));
        assertEquals(Responses.ok(), commandBus.validate(addTaskMsg(projectId)));
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
        assertTrue(isUnsupportedCommand(commandBus.validate(createProjectMsg(projectId))));
        assertTrue(isUnsupportedCommand(commandBus.validate(startProjectMsg(projectId))));
        assertTrue(isUnsupportedCommand(commandBus.validate(addTaskMsg(projectId))));
    }

    @Test
    public void return_invalid_command_response_if_command_is_invalid() {
        commandBus.register(handler);
        final MessageValidator validator = mock(MessageValidator.class);
        doReturn(newArrayList(ConstraintViolation.getDefaultInstance()))
                .when(validator)
                .validate(any(Message.class));
        commandBus.setMessageValidator(validator);

        final Response response = commandBus.validate(createProjectMsg(newUuid()));
        assertTrue(isInvalidCommand(response));
    }

    //
    // Tests for not overriding handlers by dispatchers and vice versa
    //-------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_to_register_dispatcher_for_the_command_with_registered_handler() {
        final CommandDispatcher createProjectDispatcher = new CreateProjectDispatcher();
        commandBus.register(handler);
        commandBus.register(createProjectDispatcher);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_to_register_handler_for_the_command_with_registered_dispatcher() {
        final CommandDispatcher createProjectDispatcher = new CreateProjectDispatcher();
        commandBus.register(createProjectDispatcher);
        commandBus.register(handler);
    }

    private static class CreateProjectHandler extends CommandHandler {

        private boolean handlerInvoked = false;

        protected CreateProjectHandler(String id, EventBus eventBus) {
            super(id, eventBus);
        }

        @Assign
        public ProjectCreated handle(CreateProject command, CommandContext ctx) {
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
        commandBus.register(handler);
        commandBus.unregister(handler);
        final String projectId = newUuid();
        assertTrue(isUnsupportedCommand(commandBus.validate(createProjectMsg(projectId))));
    }

    @Test
    public void validate_commands_both_dispatched_and_handled() {
        final CommandDispatcher dispatcher = new AddTaskDispatcher();
        commandBus.register(handler);
        commandBus.register(dispatcher);

        final String projectId = newUuid();
        assertEquals(Responses.ok(), commandBus.validate(createProjectMsg(projectId)));
        assertEquals(Responses.ok(), commandBus.validate(addTaskMsg(projectId)));
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

    @Test // To improve coverage stats.
    public void have_command_status_service() {
        assertNotNull(commandBus.getCommandStatusService());
    }

    @Test
    public void close_CommandStore_when_closed() throws Exception {
        commandBus.close();

        verify(commandStore, times(1)).close();
    }

    @Test
    public void shutdown_CommandScheduler_when_closed() throws Exception {
        commandBus.close();

        verify(scheduler, times(1)).shutdown();
    }

    @Test
    public void remove_all_handlers_on_close() throws Exception {
        commandBus.register(handler);

        commandBus.close();
        assertTrue(isUnsupportedCommand(commandBus.validate(createProjectMsg(newUuid()))));
    }

    @Test
    public void invoke_handler_when_command_posted() {
        commandBus.register(handler);

        final Command command = commandFactory.create(createProjectMsg(newUuid()));
        commandBus.post(command);

        assertTrue(handler.wasHandlerInvoked());
    }

    @Test
    public void invoke_dispatcher_when_command_posted() {
        final AddTaskDispatcher dispatcher = new AddTaskDispatcher();
        commandBus.register(dispatcher);

        final Command command = commandFactory.create(addTaskMsg(newUuid()));

        commandBus.post(command);

        assertTrue(dispatcher.wasDispatcherInvoked());
    }

    @Test(expected = UnsupportedCommandException.class)
    public void throw_exception_when_there_is_no_neither_handler_nor_dispatcher() {
        final Command command = commandFactory.create(addTaskMsg(newUuid()));

        commandBus.post(command);
    }

    @Test
    public void set_command_status_to_OK_when_handler_returns() {
        commandBus.register(handler);
        final Command command = commandFactory.create(createProjectMsg(newUuid()));

        commandBus.post(command);

        // See that we called CommandStore only once with the right command ID.
        verify(commandStore, times(1)).setCommandStatusOk(command.getContext()
                                                                 .getCommandId());
    }

    @Test
    public void set_command_status_to_error_when_dispatcher_throws() throws Exception {
        final IOException exception = givenThrowingDispatcher();
        final Command command = commandFactory.create(createProjectMsg(newUuid()));

        commandBus.post(command);

        // Verify we updated the status.
        verify(commandStore, times(1)).updateStatus(eq(command.getContext()
                                                              .getCommandId()), eq(exception));
        // Verify we logged the error.
        verify(log, times(1)).errorDispatching(eq(exception), eq(command));
    }

    @Test
    public void set_command_status_to_failure_when_handler_throws_failure() throws TestFailure, TestThrowable {
        final TestFailure failure = new TestFailure();
        final Command command = givenThrowingHandler(failure);
        final CommandId commandId = command.getContext().getCommandId();
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
        final Command command = givenThrowingHandler(exception);
        final CommandId commandId = command.getContext().getCommandId();
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
        final Command command = givenThrowingHandler(throwable);
        final CommandId commandId = command.getContext().getCommandId();
        final Message commandMessage = Commands.getMessage(command);

        commandBus.post(command);

        // Verify we updated the status.
        verify(commandStore, times(1)).updateStatus(eq(commandId), eq(Errors.fromThrowable(throwable)));
        // Verify we logged the failure.
        verify(log, times(1)).errorHandlingUnknown(eq(throwable), eq(commandMessage), eq(commandId));
    }

    private <E extends Throwable> Command givenThrowingHandler(E throwable) {
        final CommandHandler handler = new ThrowingCreateProjectHandler(eventBus, throwable);
        commandBus.register(handler);
        final CreateProject msg = createProjectMsg(newUuid());
        final Command command = commandFactory.create(msg);
        return command;
    }

    private <E extends Exception> E givenThrowingDispatcher() throws Exception {
        final CommandDispatcher throwingDispatcher = mock(CommandDispatcher.class);
        when(throwingDispatcher.getCommandClasses()).thenReturn(CommandClass.setOf(CreateProject.class));
        final IOException exception = new IOException("Unable to dispatch");
        doThrow(exception)
                .when(throwingDispatcher)
                .dispatch(any(Command.class));
        commandBus.register(throwingDispatcher);
        @SuppressWarnings("unchecked")
        final E throwable = (E) exception;
        return throwable;
    }

    @Test
    public void schedule_command_if_delay_is_set() {
        final int delayMsec = 1100;
        final Command cmd = newCommand(/*delay=*/milliseconds(delayMsec));

        commandBus.post(cmd);

        verify(scheduler, times(1)).schedule(cmd);
        verify(scheduler, never()).post(cmd);
        verify(scheduler, after(delayMsec).times(1)).post(cmd);
    }

    @Test
    public void do_not_schedule_command_if_no_scheduling_options_are_set() {
        commandBus.register(new CreateProjectHandler(newUuid(), eventBus));
        final Command cmd = commandFactory.create(createProjectMsg(newUuid()));

        commandBus.post(cmd);

        verify(scheduler, never()).schedule(cmd);
    }

    @Test
    public void do_not_store_command_if_command_is_scheduled() {
        final Command cmd = newCommand(/*delay=*/minutes(1));

        commandBus.post(cmd);

        verify(commandStore, never()).store(cmd);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_post_scheduled_cmd_and_no_scheduler_is_set() {
        final CommandBus commandBus = CommandBus.newBuilder()
                .setCommandStore(commandStore)
                .build();
        final Command cmd = newCommand(/*delay=*/minutes(1));

        commandBus.post(cmd);
    }

    private static Command newCommand(Duration delay) {
        final CommandContext context = createCommandContext(delay);
        return Commands.create(createProjectMsg(newUuid()), context);
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
}
