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
import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandValidationError;
import org.spine3.base.Commands;
import org.spine3.base.Error;
import org.spine3.base.Errors;
import org.spine3.base.Response;
import org.spine3.client.CommandFactory;
import org.spine3.client.test.TestCommandFactory;
import org.spine3.server.command.error.InvalidCommandException;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.server.event.EventBus;
import org.spine3.server.failure.FailureThrowable;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.CommandClass;
import org.spine3.test.failures.Failures;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;
import org.spine3.testdata.TestEventFactory;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.base.Responses.ok;
import static org.spine3.protobuf.Durations.milliseconds;
import static org.spine3.protobuf.Durations.minutes;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.testdata.TestCommands.*;

@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods", "OverlyCoupledClass"})
public class CommandBusShould {

    private CommandBus commandBus;
    private CommandStore commandStore;
    private CommandFactory commandFactory;
    private CommandBus.ProblemLog log;
    private EventBus eventBus;
    private ExecutorCommandScheduler scheduler;
    private CreateProjectHandler createProjectHandler;
    private TestResponseObserver responseObserver;

    @Before
    public void setUp() {
        final InMemoryStorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        commandStore = spy(new CommandStore(storageFactory.createCommandStorage()));
        scheduler = spy(new ExecutorCommandScheduler());
        commandBus = spy(newCommandBus(commandStore, scheduler));
        log = spy(new CommandBus.ProblemLog());
        commandBus.setProblemLog(log);
        eventBus = spy(TestEventFactory.newEventBus(storageFactory));
        commandFactory = TestCommandFactory.newInstance(CommandBusShould.class);
        createProjectHandler = new CreateProjectHandler(newUuid());
        responseObserver = new TestResponseObserver();
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_CommandStore_on_construction() {
        // noinspection ConstantConditions
        CommandBus.newBuilder()
                  .setCommandStore(null)
                  .build();
    }

    /*
     * Registration tests.
     ******************************/

    @Test
    public void state_that_no_commands_are_supported_if_nothing_registered() {
        assertAllCommandsAreSupported(false);
    }

    @Test
    public void register_command_dispatcher() {
        commandBus.register(new AllCommandDispatcher());

        assertAllCommandsAreSupported(true);
    }

    @Test
    public void unregister_command_dispatcher() {
        final CommandDispatcher dispatcher = new AllCommandDispatcher();

        commandBus.register(dispatcher);
        commandBus.unregister(dispatcher);

        assertAllCommandsAreSupported(false);
    }

    @Test
    public void register_command_handler() {
        commandBus.register(new AllCommandHandler(newUuid()));

        assertAllCommandsAreSupported(true);
    }

    @Test
    public void unregister_command_handler() {
        final AllCommandHandler handler = new AllCommandHandler(newUuid());

        commandBus.register(handler);
        commandBus.unregister(handler);

        assertAllCommandsAreSupported(false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_empty_dispatchers() {
        commandBus.register(new EmptyDispatcher());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_command_handlers_without_methods() {
        commandBus.register(new EmptyCommandHandler(newUuid()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_another_dispatcher_for_already_registered_commands() {
        commandBus.register(new AllCommandDispatcher());
        commandBus.register(new AllCommandDispatcher());
    }

    /*
     * Tests for not overriding handlers by dispatchers and vice versa.
     ******************************/

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_to_register_dispatcher_for_the_command_with_registered_handler() {
        final CommandDispatcher createProjectDispatcher = new CreateProjectDispatcher();
        commandBus.register(createProjectHandler);
        commandBus.register(createProjectDispatcher);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_to_register_handler_for_the_command_with_registered_dispatcher() {
        final CommandDispatcher createProjectDispatcher = new CreateProjectDispatcher();
        commandBus.register(createProjectDispatcher);
        commandBus.register(createProjectHandler);
    }

    @Test
    public void unregister_handler() {
        commandBus.register(createProjectHandler);
        commandBus.unregister(createProjectHandler);
        assertIsSupportedCmd(CreateProject.class, false);
    }

    @Test
    public void validate_commands_both_dispatched_and_handled() {
        commandBus.register(createProjectHandler);
        commandBus.register(new AddTaskDispatcher());

        assertAreSupportedCommands(true, CreateProject.class, AddTask.class);
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
        commandBus.register(createProjectHandler);

        commandBus.close();
        assertIsSupportedCmd(CreateProject.class, false);
    }

    /*
     * Command validation tests.
     ********************/

    @Test
    public void verify_namespace_attribute_if_multitenant() {
        commandBus.setMultitenant(true);
        commandBus.register(createProjectHandler);

        final TestResponseObserver observer = new TestResponseObserver();
        final Command cmd = createProjectCmd();

        commandBus.post(cmd, observer);

        final Throwable cause = observer.getThrowable()
                                        .getCause();
        assertEquals(InvalidCommandException.class, cause.getClass());

        final InvalidCommandException exception = (InvalidCommandException) cause;
        assertEquals(CommandValidationError.NAMESPACE_UNKNOWN.getNumber(), exception.getError()
                                                                                    .getCode());
    }

    @Test
    public void return_UnsupportedCommandException_when_there_is_neither_handler_nor_dispatcher() {
        final Command command = commandFactory.create(addTaskMsg(newUuid()));

        commandBus.post(command, responseObserver);

        final Throwable cause = responseObserver.getThrowable()
                                                .getCause();
        assertEquals(UnsupportedCommandException.class, cause.getClass());

        final UnsupportedCommandException exception = (UnsupportedCommandException) cause;
        assertEquals(CommandValidationError.UNSUPPORTED_COMMAND.getNumber(), exception.getError()
                                                                                      .getCode());

    }

    @Test
    public void return_InvalidCommandException_if_command_is_invalid() {
        commandBus.register(createProjectHandler);
        final Command command = createProjectCmd();
        final Command commandWithNoContext = command.toBuilder()
                                                    .setContext(CommandContext.getDefaultInstance())
                                                    .build();

        commandBus.post(commandWithNoContext, responseObserver);

        final Throwable throwable = responseObserver.getThrowable();
        assertNotNull(throwable);

        final Throwable cause = responseObserver.getThrowable()
                                                .getCause();
        assertEquals(InvalidCommandException.class, cause.getClass());

        final InvalidCommandException exception = (InvalidCommandException) cause;
        final Error error = exception.getError();

        assertEquals(CommandValidationError.getDescriptor().getFullName(), error.getType());
        assertEquals(CommandValidationError.INVALID_COMMAND.getNumber(), error.getCode());
    }

    private static class TestResponseObserver implements StreamObserver<Response> {

        private Response responseHandled;
        private boolean completed;
        private Throwable throwable;

        @Override
        public void onNext(Response response) {
            this.responseHandled = response;
        }

        @Override
        public void onError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }

        /* package */ Response getResponseHandled() {
            return responseHandled;
        }

        /* package */ Throwable getThrowable() {
            return this.throwable;
        }

        /* package */ boolean isCompleted() {
            return this.completed;
        }
    }

    /*
     * Command processing tests.
     *********************/

    @Test
    public void invoke_handler_when_command_posted() {
        commandBus.register(createProjectHandler);

        final Command command = commandFactory.create(createProjectMsg(newUuid()));
        commandBus.post(command, responseObserver);

        assertTrue(createProjectHandler.wasHandlerInvoked());
    }

    @Test
    public void invoke_dispatcher_when_command_posted() {
        final AddTaskDispatcher dispatcher = new AddTaskDispatcher();
        commandBus.register(dispatcher);

        final Command command = commandFactory.create(addTaskMsg(newUuid()));

        commandBus.post(command, responseObserver);

        assertTrue(dispatcher.wasDispatcherInvoked());
    }

    @Test
    public void set_command_status_to_OK_when_handler_returns() {
        commandBus.register(createProjectHandler);
        final Command command = commandFactory.create(createProjectMsg(newUuid()));

        commandBus.post(command, responseObserver);

        // See that we called CommandStore only once with the right command ID.
        verify(commandStore, times(1)).setCommandStatusOk(command.getContext()
                                                                 .getCommandId());
    }

    @Test
    public void set_command_status_to_error_when_dispatcher_throws() throws Exception {
        final ThrowingDispatcher dispatcher = new ThrowingDispatcher();
        commandBus.register(dispatcher);
        final Command command = commandFactory.create(createProjectMsg(newUuid()));

        commandBus.post(command, responseObserver);

        verify(commandStore, times(1)).updateStatus(eq(command.getContext().getCommandId()), eq(dispatcher.exception));
        verify(log, times(1)).errorDispatching(eq(dispatcher.exception), eq(command));
    }

    @Test
    public void set_command_status_to_failure_when_handler_throws_failure() throws TestFailure, TestThrowable {
        final TestFailure failure = new TestFailure();
        final Command command = givenThrowingHandler(failure);
        final CommandId commandId = command.getContext()
                                           .getCommandId();
        final Message commandMessage = Commands.getMessage(command);

        commandBus.post(command, responseObserver);

        verify(commandStore, times(1)).updateStatus(eq(commandId), eq(failure.toMessage()));
        verify(log, times(1)).failureHandling(eq(failure), eq(commandMessage), eq(commandId));
    }

    @Test
    public void set_command_status_to_failure_when_handler_throws_exception() throws TestFailure, TestThrowable {
        final RuntimeException exception = new IllegalStateException("handler throws");
        final Command command = givenThrowingHandler(exception);
        final CommandId commandId = command.getContext()
                                           .getCommandId();
        final Message commandMessage = Commands.getMessage(command);

        commandBus.post(command, responseObserver);

        verify(commandStore, times(1)).updateStatus(eq(commandId), eq(exception));
        verify(log, times(1)).errorHandling(eq(exception), eq(commandMessage), eq(commandId));
    }

    @Test
    public void set_command_status_to_failure_when_handler_throws_unknown_Throwable() throws TestFailure, TestThrowable {
        final Throwable throwable = new TestThrowable();
        final Command command = givenThrowingHandler(throwable);
        final CommandId commandId = command.getContext()
                                           .getCommandId();
        final Message commandMessage = Commands.getMessage(command);

        commandBus.post(command, responseObserver);

        verify(commandStore, times(1)).updateStatus(eq(commandId), eq(Errors.fromThrowable(throwable)));
        verify(log, times(1)).errorHandlingUnknown(eq(throwable), eq(commandMessage), eq(commandId));
    }

    private <E extends Throwable> Command givenThrowingHandler(E throwable) {
        final CommandHandler handler = new ThrowingCreateProjectHandler(throwable);
        commandBus.register(handler);
        final CreateProject msg = createProjectMsg(newUuid());
        final Command command = commandFactory.create(msg);
        return command;
    }

    /*
     * Tests of storage
     **********************/

    @Test
    public void stores_the_command_when_posted() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProjectCmd();
        final TestResponseObserver observer = new TestResponseObserver();

        commandBus.post(cmd, observer);

        assertEquals(ok(), observer.getResponseHandled());
        verify(commandBus, times(1)).store(cmd);
    }

    @Test
    public void store_invalid_command_with_error_status() {
        final TestResponseObserver responseObserver = new TestResponseObserver();

        final Command cmd = createProjectCmd();
        commandBus.post(cmd, responseObserver);

        verify(commandBus, times(1)).storeWithErrorStatus(eq(cmd), any(InvalidCommandException.class));
    }

    //TODO:2016-05-08:alexander.yevsyukov: We may want to change this to make the command delivery more reliable.
    @Test
    public void do_not_store_command_if_command_is_scheduled() {
        commandBus.register(createProjectHandler);
        final Command cmd = newCreateProjectCommand(/*delay=*/minutes(1));

        commandBus.post(cmd, responseObserver);

        verify(commandStore, never()).store(cmd);
        assertTrue(responseObserver.isCompleted());
    }

    /*
     * Scheduling tests.
     ********************/

    @Test
    public void schedule_command_if_delay_is_set() {
        commandBus.register(createProjectHandler);

        final int delayMsec = 1100;
        final Command cmd = newCreateProjectCommand(/*delay=*/milliseconds(delayMsec));

        commandBus.post(cmd, responseObserver);

        verify(scheduler, times(1)).schedule(cmd);
        verify(scheduler, never()).post(cmd);
        verify(scheduler, after(delayMsec).times(1)).post(cmd);
    }

    @Test
    public void do_not_schedule_command_if_no_scheduling_options_are_set() {
        commandBus.register(new CreateProjectHandler(newUuid()));
        final Command cmd = commandFactory.create(createProjectMsg(newUuid()));

        commandBus.post(cmd, responseObserver);

        verify(scheduler, never()).schedule(cmd);
        assertTrue(responseObserver.isCompleted());
    }

    @Ignore // Decide on the below comment.
    //TODO:2016-05-08:alexander.yevsyukov: Shouldn't we return Status.FAILED_PRECONDITION in response instead?
    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_post_scheduled_cmd_and_no_scheduler_is_set() {
        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setCommandStore(commandStore)
                                                .build();
        final Command cmd = newCreateProjectCommand(/*delay=*/minutes(1));

        commandBus.post(cmd, responseObserver);
    }

    /*
     * Utility methods.
     ********************/

    private static Command newCreateProjectCommand(Duration delay) {
        final CommandContext context = createCommandContext(delay);
        return Commands.create(createProjectMsg(newUuid()), context);
    }

    private void assertAllCommandsAreSupported(boolean areSupported) {
        assertAreSupportedCommands(areSupported, CreateProject.class, AddTask.class, StartProject.class);
    }

    @SafeVarargs
    private final void assertAreSupportedCommands(boolean areSupported, Class<? extends Message>... cmdClasses) {
        for (Class<? extends Message> clazz : cmdClasses) {
            assertIsSupportedCmd(clazz, areSupported);
        }
    }

    private void assertIsSupportedCmd(Class<? extends Message> cmdClass, boolean isSupported) {
        final CommandClass clazz = CommandClass.of(cmdClass);
        if (isSupported) {
            assertTrue(commandBus.isSupportedCommand(clazz));
        } else {
            assertFalse(commandBus.isSupportedCommand(clazz));
        }
    }

    /*
     * Test command dispatchers.
     ********************/

    private static class EmptyDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getCommandClasses() {
            return Collections.emptySet();
        }

        @Override
        public void dispatch(Command request) throws Exception {
        }
    }

    private static class AllCommandDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getCommandClasses() {
            return CommandClass.setOf(CreateProject.class, StartProject.class, AddTask.class);
        }

        @Override
        public void dispatch(Command request) throws Exception {
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

    private static class ThrowingDispatcher implements CommandDispatcher {

        @SuppressWarnings("ThrowableInstanceNeverThrown")
        private final RuntimeException exception = new RuntimeException("Some dispatching exception.");

        @Override
        public Set<CommandClass> getCommandClasses() {
            return CommandClass.setOf(CreateProject.class, StartProject.class, AddTask.class);
        }

        @Override
        public void dispatch(Command request) throws Exception {
            throw exception;
        }
    }

    /*
     * Test command handlers.
     ********************/

    private class CreateProjectHandler extends CommandHandler {

        private boolean handlerInvoked = false;

        protected CreateProjectHandler(String id) {
            super(id, eventBus);
        }

        @Assign
        @SuppressWarnings("unused")
        public ProjectCreated handle(CreateProject command, CommandContext ctx) {
            handlerInvoked = true;
            return ProjectCreated.getDefaultInstance();
        }

        public boolean wasHandlerInvoked() {
            return handlerInvoked;
        }
    }

    @SuppressWarnings("unused")
    private class AllCommandHandler extends CommandHandler {

        protected AllCommandHandler(String id) {
            super(id, eventBus);
        }

        @Assign
        public ProjectCreated handle(CreateProject command, CommandContext ctx) {
            return ProjectCreated.getDefaultInstance();
        }

        @Assign
        public TaskAdded handle(AddTask command) {
            return TaskAdded.getDefaultInstance();
        }

        @Assign
        public ProjectStarted handle(StartProject command) {
            return ProjectStarted.getDefaultInstance();
        }
    }

    /**
     * A stub handler that throws passed `Throwable` in the command handler method.
     *
     * @see #set_command_status_to_failure_when_handler_throws_failure
     * @see #set_command_status_to_failure_when_handler_throws_exception
     * @see #set_command_status_to_failure_when_handler_throws_unknown_Throwable
     */
    private class ThrowingCreateProjectHandler extends CommandHandler {

        private final Throwable throwable;

        protected ThrowingCreateProjectHandler(Throwable throwable) {
            super(newUuid(), eventBus);
            this.throwable = throwable;
        }

        @Assign
        @SuppressWarnings("unused")
        public ProjectCreated handle(CreateProject msg, CommandContext context) throws Throwable {
            //noinspection ProhibitedExceptionThrown
            throw throwable;
        }
    }

    private class EmptyCommandHandler extends CommandHandler {
        protected EmptyCommandHandler(String id) {
            super(id, eventBus);
        }
    }

    /*
     * Throwables.
     ********************/

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
}
