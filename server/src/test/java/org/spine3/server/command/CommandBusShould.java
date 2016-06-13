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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandValidationError;
import org.spine3.base.Error;
import org.spine3.base.Errors;
import org.spine3.base.FailureThrowable;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.client.CommandFactory;
import org.spine3.client.test.TestCommandFactory;
import org.spine3.protobuf.Durations;
import org.spine3.server.command.error.CommandException;
import org.spine3.server.command.error.InvalidCommandException;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.server.event.EventBus;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.CommandClass;
import org.spine3.server.users.CurrentTenant;
import org.spine3.test.Tests;
import org.spine3.test.failures.Failures;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.Math.abs;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;
import static org.spine3.base.CommandStatus.SCHEDULED;
import static org.spine3.base.CommandValidationError.*;
import static org.spine3.base.Commands.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Durations.minutes;
import static org.spine3.protobuf.Timestamps.minutesAgo;
import static org.spine3.server.command.error.CommandExpiredException.commandExpiredError;
import static org.spine3.testdata.TestCommands.*;
import static org.spine3.testdata.TestEventFactory.newEventBus;

@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods", "OverlyCoupledClass"})
public class CommandBusShould {

    private CommandBus commandBus;
    private CommandStore commandStore;
    private CommandFactory commandFactory;
    private ProblemLog log;
    private EventBus eventBus;
    private ExecutorCommandScheduler scheduler;
    private CreateProjectHandler createProjectHandler;
    private TestResponseObserver responseObserver;

    @Before
    public void setUp() {
        final InMemoryStorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        commandStore = spy(new CommandStore(storageFactory.createCommandStorage()));
        scheduler = spy(new ExecutorCommandScheduler());
        log = spy(new ProblemLog());
        // Do not create a spy of the command bus because it would be impossible to debug its code
        commandBus = new CommandBus(commandStore, scheduler, log);
        eventBus = newEventBus(storageFactory);
        commandFactory = TestCommandFactory.newInstance(CommandBusShould.class);
        createProjectHandler = new CreateProjectHandler(newUuid());
        responseObserver = new TestResponseObserver();
    }

    @After
    public void tearDown() throws Exception {
        if (commandStore.isOpen()) { // then command bus is opened, too
            commandBus.close();
        }
        eventBus.close();
    }


    /*
     * Creation tests.
     *********************/

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_CommandStore_on_construction() {
        CommandBus.newInstance(Tests.<CommandStore>nullRef());
    }

    @Test
    public void create_new_instance() {
        final CommandBus commandBus = CommandBus.newInstance(commandStore);
        assertNotNull(commandBus);
    }

    /*
     * Registration tests.
     *********************/

    @Test
    public void state_that_no_commands_are_supported_if_nothing_registered() {
        assertNotSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test
    public void register_command_dispatcher() {
        commandBus.register(new AllCommandDispatcher());

        assertSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test
    public void unregister_command_dispatcher() {
        final CommandDispatcher dispatcher = new AllCommandDispatcher();

        commandBus.register(dispatcher);
        commandBus.unregister(dispatcher);

        assertNotSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test
    public void register_command_handler() {
        commandBus.register(new AllCommandHandler());

        assertSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test
    public void unregister_command_handler() {
        final AllCommandHandler handler = new AllCommandHandler();

        commandBus.register(handler);
        commandBus.unregister(handler);

        assertNotSupported(CreateProject.class, AddTask.class, StartProject.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_empty_dispatchers() {
        commandBus.register(new EmptyDispatcher());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_command_handlers_without_methods() {
        commandBus.register(new EmptyCommandHandler());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_allow_another_dispatcher_for_already_registered_commands() {
        commandBus.register(new AllCommandDispatcher());
        commandBus.register(new AllCommandDispatcher());
    }

    /*
     * Tests for not overriding handlers by dispatchers and vice versa.
     ******************************************************************/

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
        assertNotSupported(CreateProject.class);
    }

    @Test
    public void validate_commands_both_dispatched_and_handled() {
        commandBus.register(createProjectHandler);
        commandBus.register(new AddTaskDispatcher());

        assertSupported(CreateProject.class, AddTask.class);
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

        verify(commandStore).close();
    }

    @Test
    public void shutdown_CommandScheduler_when_closed() throws Exception {
        commandBus.close();

        verify(scheduler).shutdown();
    }

    @Test
    public void remove_all_handlers_on_close() throws Exception {
        commandBus.register(createProjectHandler);

        commandBus.close();
        assertNotSupported(CreateProject.class);
    }

    /*
     * Command validation tests.
     ***************************/

    @Test
    public void verify_tenant_id_attribute_if_multitenant() {
        commandBus.setMultitenant(true);
        commandBus.register(createProjectHandler);
        final Command cmd = createProjectCmdWithoutContext();

        commandBus.post(cmd, responseObserver);

        checkCommandError(responseObserver.getThrowable(), TENANT_UNKNOWN, InvalidCommandException.class, cmd);
        assertTrue(responseObserver.getResponses().isEmpty());
    }

    @Test
    public void return_InvalidCommandException_if_command_is_invalid() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProjectCmdWithoutContext();

        commandBus.post(cmd, responseObserver);

        checkCommandError(responseObserver.getThrowable(), INVALID_COMMAND, InvalidCommandException.class, cmd);
        assertTrue(responseObserver.getResponses().isEmpty());
    }

    @Test
    public void return_UnsupportedCommandException_when_there_is_neither_handler_nor_dispatcher() {
        final Command cmd = commandFactory.create(addTaskMsg(newUuid()));

        commandBus.post(cmd, responseObserver);

        checkCommandError(responseObserver.getThrowable(), UNSUPPORTED_COMMAND, UnsupportedCommandException.class, cmd);
        assertTrue(responseObserver.getResponses()
                                   .isEmpty());
    }

    private static <E extends CommandException> void checkCommandError(
            Throwable throwable,
            CommandValidationError validationError,
            Class<E> exceptionClass,
            Command cmd) {
        final Throwable cause = throwable.getCause();
        assertEquals(exceptionClass, cause.getClass());
        @SuppressWarnings("unchecked")
        final E exception = (E) cause;
        assertEquals(cmd, exception.getCommand());
        final Error error = exception.getError();
        assertEquals(CommandValidationError.getDescriptor()
                                           .getFullName(), error.getType());
        assertEquals(validationError.getNumber(), error.getCode());
        assertFalse(error.getMessage()
                         .isEmpty());
        if (validationError == INVALID_COMMAND) {
            assertFalse(error.getValidationError()
                             .getConstraintViolationList()
                             .isEmpty());
        }
    }

    /*
     * Command processing tests.
     ***************************/

    @Test
    public void post_command_and_return_OK_response() {
        commandBus.register(createProjectHandler);

        commandBus.post(createProjectCmd(), responseObserver);

        assertResponseOkAndCompleted(responseObserver);
    }

    @Test
    public void post_command_and_set_current_tenant_if_multitenant() {
        commandBus.setMultitenant(true);
        commandBus.register(createProjectHandler);
        final Command cmd = createProjectCmd();

        commandBus.post(cmd, responseObserver);

        assertEquals(cmd.getContext()
                        .getTenantId(), CurrentTenant.get());
    }

    @Test
    public void post_command_and_do_not_set_current_tenant_if_not_multitenant() {
        commandBus.register(createProjectHandler);

        commandBus.post(createProjectCmd(), responseObserver);

        assertNull(CurrentTenant.get());
    }

    @Test
    public void store_command_when_posted() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProjectCmd();

        commandBus.post(cmd, responseObserver);

        verify(commandStore).store(cmd);
    }

    @Test
    public void store_invalid_command_with_error_status() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProjectCmdWithoutContext();

        commandBus.post(cmd, responseObserver);

        verify(commandStore).store(eq(cmd), isA(Error.class));
    }

    @Test
    public void invoke_handler_when_command_posted() {
        commandBus.register(createProjectHandler);
        final Command command = commandFactory.create(createProjectMsg());

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
        final Command command = commandFactory.create(createProjectMsg());

        commandBus.post(command, responseObserver);

        // See that we called CommandStore only once with the right command ID.
        verify(commandStore).setCommandStatusOk(getId(command));
    }

    @Test
    public void set_command_status_to_error_when_dispatcher_throws() throws Exception {
        final ThrowingDispatcher dispatcher = new ThrowingDispatcher();
        commandBus.register(dispatcher);
        final Command command = commandFactory.create(createProjectMsg());

        commandBus.post(command, responseObserver);

        verify(commandStore).updateStatus(getId(command), dispatcher.exception);
        verify(log).errorDispatching(dispatcher.exception, command);
    }

    @Test
    public void set_command_status_to_failure_when_handler_throws_failure() throws TestFailure, TestThrowable {
        final TestFailure failure = new TestFailure();
        final Command command = givenThrowingHandler(failure);
        final CommandId commandId = getId(command);
        final Message commandMessage = getMessage(command);

        commandBus.post(command, responseObserver);

        verify(commandStore).updateStatus(eq(commandId), eq(failure.toMessage()));
        verify(log).failureHandling(eq(failure), eq(commandMessage), eq(commandId));
    }

    @Test
    public void set_command_status_to_failure_when_handler_throws_exception() throws TestFailure, TestThrowable {
        final RuntimeException exception = new IllegalStateException("handler throws");
        final Command command = givenThrowingHandler(exception);
        final CommandId commandId = getId(command);
        final Message commandMessage = getMessage(command);

        commandBus.post(command, responseObserver);

        verify(commandStore).updateStatus(eq(commandId), eq(exception));
        verify(log).errorHandling(eq(exception), eq(commandMessage), eq(commandId));
    }

    @Test
    public void set_command_status_to_failure_when_handler_throws_unknown_Throwable() throws TestFailure, TestThrowable {
        final Throwable throwable = new TestThrowable();
        final Command command = givenThrowingHandler(throwable);
        final CommandId commandId = getId(command);
        final Message commandMessage = getMessage(command);

        commandBus.post(command, responseObserver);

        verify(commandStore).updateStatus(eq(commandId), eq(Errors.fromThrowable(throwable)));
        verify(log).errorHandlingUnknown(eq(throwable), eq(commandMessage), eq(commandId));
    }

    private <E extends Throwable> Command givenThrowingHandler(E throwable) {
        final CommandHandler handler = new ThrowingCreateProjectHandler(throwable);
        commandBus.register(handler);
        final CreateProject msg = createProjectMsg();
        final Command command = commandFactory.create(msg);
        return command;
    }

    @Test
    public void return_supported_classes() {
        commandBus.register(createProjectHandler);
        commandBus.register(new AddTaskDispatcher());

        final Set<CommandClass> cmdClasses = commandBus.getSupportedCommandClasses();

        assertTrue(cmdClasses.contains(CommandClass.of(CreateProject.class)));
        assertTrue(cmdClasses.contains(CommandClass.of(AddTask.class)));
    }

    /*
     * Scheduling tests.
     ********************/

    @Test
    public void schedule_command_if_delay_is_set() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProjectCmd(/*delay=*/minutes(1));

        commandBus.post(cmd, responseObserver);

        verify(scheduler).schedule(cmd);
    }

    @Test
    public void store_scheduled_command_and_return_OK() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProjectCmd(/*delay=*/minutes(1));

        commandBus.post(cmd, responseObserver);

        verify(commandStore).store(cmd, SCHEDULED);
        assertResponseOkAndCompleted(responseObserver);
    }

    @Test
    public void do_not_schedule_command_if_no_scheduling_options_are_set() {
        commandBus.register(new CreateProjectHandler(newUuid()));
        final Command cmd = commandFactory.create(createProjectMsg());

        commandBus.post(cmd, responseObserver);

        verify(scheduler, never()).schedule(cmd);
        assertResponseOkAndCompleted(responseObserver);
    }

    @Test
    public void reschedule_commands_from_storage() {
        final Timestamp schedulingTime = minutesAgo(3);
        final Duration delayPrimary = Durations.ofMinutes(5);
        final Duration newDelayExpected = Durations.ofMinutes(2); // = 5 - 3
        final List<Command> commandsPrimary = newArrayList(createProjectCmd(), addTaskCmd(), startProjectCmd());
        storeAsScheduled(commandsPrimary, delayPrimary, schedulingTime);

        commandBus.getRescheduler().rescheduleCommands();

        final ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(scheduler, times(commandsPrimary.size())).schedule(commandCaptor.capture());
        final List<Command> commandsRescheduled = commandCaptor.getAllValues();
        for (Command cmd : commandsRescheduled) {
            final long actualDelay = getDelaySeconds(cmd);
            assertSecondsEqual(newDelayExpected.getSeconds(), actualDelay, /*maxDiffSec=*/1);
        }
    }

    @Test
    public void set_expired_scheduled_command_status_to_error_if_time_to_post_them_passed() {
        final List<Command> commands = newArrayList(createProjectCmd(), addTaskCmd(), startProjectCmd());
        final Duration delay = Durations.ofMinutes(5);
        final Timestamp schedulingTime = minutesAgo(10); // time to post passed
        storeAsScheduled(commands, delay, schedulingTime);

        commandBus.getRescheduler().rescheduleCommands();

        for (Command cmd : commands) {
            final Message msg = getMessage(cmd);
            final CommandId id = getId(cmd);
            verify(commandStore).updateStatus(id, commandExpiredError(msg));
            verify(log).errorExpiredCommand(msg, id);
        }
    }

    /*
     * Test utility methods.
     ***********************/

    private static void assertResponseOkAndCompleted(TestResponseObserver observer) {
        final List<Response> responses = observer.getResponses();
        assertEquals(1, responses.size());
        assertEquals(Responses.ok(), responses.get(0));
        assertTrue(observer.isCompleted());
        assertNull(observer.getThrowable());
    }

    private static Command createProjectCmdWithoutContext() {
        final Command cmd = createProjectCmd();
        final Command invalidCmd = cmd.toBuilder()
                                      .setContext(CommandContext.getDefaultInstance())
                                      .build();
        return invalidCmd;
    }

    @SafeVarargs
    private final void assertSupported(Class<? extends Message>... cmdClasses) {
        for (Class<? extends Message> clazz : cmdClasses) {
            final CommandClass cmdClass = CommandClass.of(clazz);
            assertTrue(commandBus.isSupportedCommand(cmdClass));
        }
    }

    @SafeVarargs
    private final void assertNotSupported(Class<? extends Message>... cmdClasses) {
        for (Class<? extends Message> clazz : cmdClasses) {
            final CommandClass cmdClass = CommandClass.of(clazz);
            assertFalse(commandBus.isSupportedCommand(cmdClass));
        }
    }

    private static void assertSecondsEqual(long expectedSec, long actualSec, long maxDiffSec) {
        final long diffSec = abs(expectedSec - actualSec);
        assertTrue(diffSec <= maxDiffSec);
    }

    private static long getDelaySeconds(Command cmd) {
        final long delaySec = cmd
                .getContext()
                .getSchedule()
                .getDelay()
                .getSeconds();
        return delaySec;
    }

    private void storeAsScheduled(Iterable<Command> commands, Duration delay, Timestamp schedulingTime) {
        for (Command cmd : commands) {
            final Command cmdWithSchedule = setSchedule(cmd, delay, schedulingTime);
            commandStore.store(cmdWithSchedule, SCHEDULED);
        }
    }

    /*
     * Test command dispatchers.
     ***************************/

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
     ************************/

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

        protected AllCommandHandler() {
            super(newUuid(), eventBus);
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
        protected EmptyCommandHandler() {
            super(newUuid(), eventBus);
        }
    }

    /*
     * Response observers.
     *********************/

    private static class TestResponseObserver implements StreamObserver<Response> {

        private final List<Response> responses = newLinkedList();
        private Throwable throwable;
        private boolean completed = false;

        @Override
        public void onNext(Response response) {
            responses.add(response);
        }

        @Override
        public void onError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }

        /* package */ List<Response> getResponses() {
            return ImmutableList.copyOf(responses);
        }

        /* package */ Throwable getThrowable() {
            return throwable;
        }

        /* package */ boolean isCompleted() {
            return this.completed;
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
