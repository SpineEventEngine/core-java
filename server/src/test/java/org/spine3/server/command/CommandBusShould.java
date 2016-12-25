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
import com.google.protobuf.StringValue;
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
import org.spine3.base.EventContext;
import org.spine3.base.FailureThrowable;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.client.CommandFactory;
import org.spine3.protobuf.Durations;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.error.CommandException;
import org.spine3.server.command.error.InvalidCommandException;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.server.entity.IdFunction;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.GetProducerIdFromEvent;
import org.spine3.server.procman.ProcessManagerRepository;
import org.spine3.server.procman.ProcessManagerRepositoryShould;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.CommandClass;
import org.spine3.server.type.EventClass;
import org.spine3.server.users.CurrentTenant;
import org.spine3.test.TestCommandFactory;
import org.spine3.test.Tests;
import org.spine3.test.command.AddTask;
import org.spine3.test.command.CreateProject;
import org.spine3.test.command.StartProject;
import org.spine3.test.command.event.ProjectCreated;
import org.spine3.test.command.event.ProjectStarted;
import org.spine3.test.command.event.TaskAdded;
import org.spine3.test.procman.Project;
import org.spine3.test.procman.ProjectId;
import org.spine3.testdata.TestEventBusFactory;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Math.abs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.base.CommandStatus.SCHEDULED;
import static org.spine3.base.CommandValidationError.INVALID_COMMAND;
import static org.spine3.base.CommandValidationError.TENANT_UNKNOWN;
import static org.spine3.base.CommandValidationError.UNSUPPORTED_COMMAND;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Commands.getMessage;
import static org.spine3.base.Commands.setSchedule;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Durations.minutes;
import static org.spine3.protobuf.Timestamps.minutesAgo;
import static org.spine3.server.command.error.CommandExpiredException.commandExpiredError;
import static org.spine3.util.Exceptions.wrapped;

@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods", "OverlyCoupledClass"})
public class CommandBusShould {

    private CommandBus commandBus;
    private CommandStore commandStore;
    private CommandFactory commandFactory;
    private Log log;
    private EventBus eventBus;
    private ExecutorCommandScheduler scheduler;
    private CreateProjectHandler createProjectHandler;
    private TestResponseObserver responseObserver;

    @Before
    public void setUp() {
        final InMemoryStorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        commandStore = spy(new CommandStore(storageFactory.createCommandStorage()));
        scheduler = spy(new ExecutorCommandScheduler());
        log = spy(new Log());
        commandBus = CommandBus.newBuilder()
                .setCommandStore(commandStore)
                .setCommandScheduler(scheduler)
                .setThreadSpawnAllowed(true)
                .setLog(log)
                .setAutoReschedule(false)
                .build();
        eventBus = TestEventBusFactory.create(storageFactory);
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
    public void not_accept_null_CommandStore_in_builder() {
        CommandBus.newBuilder()
                  .setCommandStore(Tests.<CommandStore>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void not_allow_to_omit_setting_CommandStore_in_builder() {
        CommandBus.newBuilder()
                  .build();
    }


    @Test
    public void create_new_instance() {
        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setCommandStore(commandStore)
                                                .build();
        assertNotNull(commandBus);
    }

    @Test
    public void allow_to_specify_command_scheduler_via_builder() {
        final CommandScheduler expectedScheduler = mock(CommandScheduler.class);
        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setCommandStore(commandStore)
                                                .setCommandScheduler(expectedScheduler)
                                                .build();
        assertNotNull(commandBus);

        final CommandScheduler actualScheduler = commandBus.scheduler();
        assertEquals(expectedScheduler, actualScheduler);
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
    public void not_accept_empty_dispatchers() {
        commandBus.register(new EmptyDispatcher());
    }

    @Test
    public void accept_empty_process_manager_repository_dispatcher() {
        final ProcessManagerRepoDispatcher pmRepo = new ProcessManagerRepoDispatcher(mock(BoundedContext.class));
        commandBus.register(pmRepo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_command_handlers_without_methods() {
        commandBus.register(new EmptyCommandHandler());
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_allow_another_dispatcher_for_already_registered_commands() {
        commandBus.register(new AllCommandDispatcher());
        commandBus.register(new AllCommandDispatcher());
    }

    /*
     * Tests for not overriding handlers by dispatchers and vice versa.
     ******************************************************************/

    @Test(expected = IllegalArgumentException.class)
    public void not_allow_to_register_dispatcher_for_the_command_with_registered_handler() {
        final CommandDispatcher createProjectDispatcher = new CreateProjectDispatcher();
        commandBus.register(createProjectHandler);
        commandBus.register(createProjectDispatcher);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_allow_to_register_handler_for_the_command_with_registered_dispatcher() {
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
        assertNotNull(Log.log());
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
        final Command cmd = commandFactory.create(Given.CommandMessage.addTask(newUuid()));

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

        commandBus.post(Given.Command.createProject(), responseObserver);

        assertResponseOkAndCompleted(responseObserver);
    }

    @Test
    public void post_command_and_set_current_tenant_if_multitenant() {
        commandBus.setMultitenant(true);
        commandBus.register(createProjectHandler);
        final Command cmd = Given.Command.createProject();

        commandBus.post(cmd, responseObserver);

        assertEquals(cmd.getContext()
                        .getTenantId(), CurrentTenant.get());
    }

    @Test
    public void post_command_and_do_not_set_current_tenant_if_not_multitenant() {
        commandBus.register(createProjectHandler);

        commandBus.post(Given.Command.createProject(), responseObserver);

        assertNull(CurrentTenant.get());
    }

    @Test
    public void store_command_when_posted() {
        commandBus.register(createProjectHandler);
        final Command cmd = Given.Command.createProject();

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
        final Command command = commandFactory.create(Given.CommandMessage.createProject());

        commandBus.post(command, responseObserver);

        assertTrue(createProjectHandler.wasHandlerInvoked());
    }

    @Test
    public void invoke_dispatcher_when_command_posted() {
        final AddTaskDispatcher dispatcher = new AddTaskDispatcher();
        commandBus.register(dispatcher);
        final Command command = commandFactory.create(Given.CommandMessage.addTask(newUuid()));

        commandBus.post(command, responseObserver);

        assertTrue(dispatcher.wasDispatcherInvoked());
    }

    @Test
    public void set_command_status_to_OK_when_handler_returns() {
        commandBus.register(createProjectHandler);
        final Command command = commandFactory.create(Given.CommandMessage.createProject());

        commandBus.post(command, responseObserver);

        // See that we called CommandStore only once with the right command ID.
        verify(commandStore).setCommandStatusOk(getId(command));
    }

    @Test
    public void set_command_status_to_error_when_dispatcher_throws() throws Exception {
        final ThrowingDispatcher dispatcher = new ThrowingDispatcher();
        commandBus.register(dispatcher);
        final Command command = commandFactory.create(Given.CommandMessage.createProject());

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
        final CreateProject msg = Given.CommandMessage.createProject();
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
        final Command cmd = Given.Command.createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, responseObserver);

        verify(scheduler).schedule(cmd);
    }

    @Test
    public void store_scheduled_command_and_return_OK() {
        commandBus.register(createProjectHandler);
        final Command cmd = Given.Command.createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, responseObserver);

        verify(commandStore).store(cmd, SCHEDULED);
        assertResponseOkAndCompleted(responseObserver);
    }

    @Test
    public void not_schedule_command_if_no_scheduling_options_are_set() {
        commandBus.register(new CreateProjectHandler(newUuid()));
        final Command cmd = commandFactory.create(Given.CommandMessage.createProject());

        commandBus.post(cmd, responseObserver);

        verify(scheduler, never()).schedule(cmd);
        assertResponseOkAndCompleted(responseObserver);
    }

    @Test
    public void reschedule_commands_from_storage() {
        final Timestamp schedulingTime = minutesAgo(3);
        final Duration delayPrimary = Durations.ofMinutes(5);
        final Duration newDelayExpected = Durations.ofMinutes(2); // = 5 - 3
        final List<Command> commandsPrimary = newArrayList(Given.Command.createProject(), Given.Command.addTask(), Given.Command.startProject());
        storeAsScheduled(commandsPrimary, delayPrimary, schedulingTime);

        commandBus.rescheduler().doRescheduleCommands();

        final ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(scheduler, times(commandsPrimary.size())).schedule(commandCaptor.capture());
        final List<Command> commandsRescheduled = commandCaptor.getAllValues();
        for (Command cmd : commandsRescheduled) {
            final long actualDelay = getDelaySeconds(cmd);
            assertSecondsEqual(newDelayExpected.getSeconds(), actualDelay, /*maxDiffSec=*/1);
        }
    }

    @Test
    public void reschedule_commands_from_storage_in_parallel_on_build_if_thread_spawning_allowed() {
        final String mainThreadName = Thread.currentThread().getName();
        final StringBuilder threadNameUponScheduling = new StringBuilder(0);
        final CommandScheduler scheduler = threadAwareScheduler(threadNameUponScheduling);
        singleCommandForRescheduling();

        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setCommandStore(commandStore)
                                                .setCommandScheduler(scheduler)
                                                .setThreadSpawnAllowed(true)
                                                .setAutoReschedule(true)
                                                .build();
        assertNotNull(commandBus);

        // Sleep to ensure the commands have been rescheduled in parallel.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw wrapped(e);
        }

        // Ensure the scheduler has been called for a single command,
        final ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(scheduler, times(1)).schedule(commandCaptor.capture());

        // and the call has been made for a thread, different than main thread.
        final String actualThreadName = threadNameUponScheduling.toString();
        assertNotNull(actualThreadName);
        assertNotEquals(mainThreadName, actualThreadName);
    }

    @Test
    public void reschedule_commands_from_storage_synchronously_on_build_if_thread_spawning_NOT_allowed() {
        final String mainThreadName = Thread.currentThread().getName();
        final StringBuilder threadNameUponScheduling = new StringBuilder(0);
        final CommandScheduler scheduler = threadAwareScheduler(threadNameUponScheduling);
        singleCommandForRescheduling();

        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setCommandStore(commandStore)
                                                .setCommandScheduler(scheduler)
                                                .setThreadSpawnAllowed(false)
                                                .setAutoReschedule(true)
                                                .build();
        assertNotNull(commandBus);

        // Ensure the scheduler has been called for a single command,
        final ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(scheduler, times(1)).schedule(commandCaptor.capture());

        // and the call has been made for a thread, different than main thread.
        final String actualThreadName = threadNameUponScheduling.toString();
        assertNotNull(actualThreadName);
        assertEquals(mainThreadName, actualThreadName);
    }

    private void singleCommandForRescheduling() {
        final Timestamp schedulingTime = minutesAgo(3);
        final Duration delayPrimary = Durations.ofMinutes(5);
        final Command cmdWithSchedule = setSchedule(Given.Command.createProject(), delayPrimary, schedulingTime);
        commandStore.store(cmdWithSchedule, SCHEDULED);
    }

    // The method is not {@code static} to allow Mockito spy on anonymous class.
    @SuppressWarnings({"MethodParameterNamingConvention", "MethodMayBeStatic"})
    private CommandScheduler threadAwareScheduler(final StringBuilder threadNameDestination) {
        return spy(new ExecutorCommandScheduler() {
                @Override
                public void schedule(Command command) {
                    super.schedule(command);
                    threadNameDestination.append(Thread.currentThread().getName());
                }
            });
    }

    @Test
    public void set_expired_scheduled_command_status_to_error_if_time_to_post_them_passed() {
        final List<Command> commands = newArrayList(Given.Command.createProject(), Given.Command.addTask(), Given.Command.startProject());
        final Duration delay = Durations.ofMinutes(5);
        final Timestamp schedulingTime = minutesAgo(10); // time to post passed
        storeAsScheduled(commands, delay, schedulingTime);

        commandBus.rescheduler().doRescheduleCommands();

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
        final Command cmd = Given.Command.createProject();
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

    private static class ProcessManagerRepoDispatcher
            extends ProcessManagerRepository<ProjectId, ProcessManagerRepositoryShould.TestProcessManager, Project> {

        protected ProcessManagerRepoDispatcher(BoundedContext boundedContext) {
            super(boundedContext);
        }

        // Always return an empty set of command classes forwarded by this repository.
        @SuppressWarnings("RefusedBequest")
        @Override
        public Set<CommandClass> getCommandClasses() {
            return newHashSet();
        }

        @Override
        public IdFunction<ProjectId, ? extends Message, EventContext> getIdFunction(@Nonnull EventClass eventClass) {
            return GetProducerIdFromEvent.newInstance(0);
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
            super(StringValue.newBuilder()
                             .setValue(TestFailure.class.getName())
                             .build());
        }
    }

    @SuppressWarnings("serial")
    private static class TestThrowable extends Throwable {
    }
}
