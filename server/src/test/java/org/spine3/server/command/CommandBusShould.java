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

import com.google.common.base.Optional;
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
import org.spine3.base.CommandValidationError;
import org.spine3.base.Error;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.protobuf.Durations;
import org.spine3.server.command.error.CommandException;
import org.spine3.server.command.error.InvalidCommandException;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.server.event.EventBus;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.CommandClass;
import org.spine3.server.users.CurrentTenant;
import org.spine3.test.Tests;
import org.spine3.test.command.AddTask;
import org.spine3.test.command.CreateProject;
import org.spine3.test.command.event.ProjectCreated;
import org.spine3.testdata.TestEventBusFactory;
import org.spine3.users.TenantId;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
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
import static org.spine3.base.Commands.setSchedule;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Durations.minutes;
import static org.spine3.protobuf.Timestamps.minutesAgo;
import static org.spine3.server.command.Given.Command.addTask;
import static org.spine3.server.command.Given.Command.createProject;
import static org.spine3.server.command.Given.Command.startProject;

public class CommandBusShould {

    private CommandBus commandBus;
    private CommandStore commandStore;
    private EventBus eventBus;
    private ExecutorCommandScheduler scheduler;
    private CreateProjectHandler createProjectHandler;
    private TestResponseObserver responseObserver;

    @Before
    public void setUp() {
        final InMemoryStorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        commandStore = spy(new CommandStore(storageFactory.createCommandStorage()));
        scheduler = spy(new ExecutorCommandScheduler());
        commandBus = CommandBus.newBuilder()
                               .setCommandStore(commandStore)
                               .setCommandScheduler(scheduler)
                               .setThreadSpawnAllowed(true)
                               .setAutoReschedule(false)
                               .build();
        eventBus = TestEventBusFactory.create(storageFactory);
        createProjectHandler = new CreateProjectHandler(newUuid());
        responseObserver = new TestResponseObserver();
    }

    @After
    public void tearDown() throws Exception {
        if (commandStore.isOpen()) { // then CommandBus is opened, too
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
        final Command command = addTask();
        commandBus.post(command, responseObserver);

        checkCommandError(responseObserver.getThrowable(),
                          UNSUPPORTED_COMMAND,
                          UnsupportedCommandException.class, command);
        assertTrue(responseObserver.getResponses()
                                   .isEmpty());
    }

    private static <E extends CommandException> void checkCommandError(Throwable throwable,
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
        commandBus.register(new CreateProjectHandler(newUuid()));

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
        return new CreateProjectHandler(newUuid());
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

        assertResponseOkAndCompleted(responseObserver);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // we check in assertion
    @Test
    public void post_command_and_set_current_tenant_if_multitenant() {
        commandBus.setMultitenant(true);
        commandBus.register(createProjectHandler);
        final Command cmd = createProject();

        commandBus.post(cmd, responseObserver);

        final Optional<TenantId> currentTenant = CurrentTenant.get();
        assertTrue(currentTenant.isPresent());
        assertEquals(cmd.getContext()
                        .getTenantId(), currentTenant.get());
    }

    @Test
    public void post_command_and_do_not_set_current_tenant_if_not_multitenant() {
        commandBus.register(createProjectHandler);

        commandBus.post(createProject(), responseObserver);

        assertFalse(CurrentTenant.get().isPresent());
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
        final Command cmd = createProjectCmdWithoutContext();

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
    public void set_command_status_to_OK_when_handler_returns() {
        commandBus.register(createProjectHandler);

        final Command command = createProject();
        commandBus.post(command, responseObserver);

        // See that we called CommandStore only once with the right command ID.
        verify(commandStore).setCommandStatusOk(getId(command));
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
        final Command cmd = createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, responseObserver);

        verify(scheduler).schedule(cmd);
    }

    @Test
    public void store_scheduled_command_and_return_OK() {
        commandBus.register(createProjectHandler);
        final Command cmd = createProject(/*delay=*/minutes(1));

        commandBus.post(cmd, responseObserver);

        verify(commandStore).store(cmd, SCHEDULED);
        assertResponseOkAndCompleted(responseObserver);
    }

    @Test
    public void not_schedule_command_if_no_scheduling_options_are_set() {
        commandBus.register(new CreateProjectHandler(newUuid()));

        commandBus.post(createProject(), responseObserver);

        verify(scheduler, never()).schedule(createProject());
        assertResponseOkAndCompleted(responseObserver);
    }

    @Test
    public void reschedule_commands_from_storage() {
        final Timestamp schedulingTime = minutesAgo(3);
        final Duration delayPrimary = Durations.ofMinutes(5);
        final Duration newDelayExpected = Durations.ofMinutes(2); // = 5 - 3
        final List<Command> commandsPrimary = newArrayList(createProject(), addTask(), startProject());
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
            throw new IllegalStateException(e);
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
        final Command cmdWithSchedule = setSchedule(createProject(), delayPrimary, schedulingTime);
        commandStore.store(cmdWithSchedule, SCHEDULED);
    }

    /**
     * Creates a new thread-aware scheduler spied by Mockito.
     *
     * <p>The method is not {@code static} to allow Mockito spy on the created anonymous class instance.
     *
     * @param targetThreadName the builder of the thread name that will be created upon command scheduling
     * @return newly created instance
     */
    @SuppressWarnings("MethodMayBeStatic")
    private CommandScheduler threadAwareScheduler(final StringBuilder targetThreadName) {
        return spy(new ExecutorCommandScheduler() {
            @Override
            public void schedule(Command command) {
                super.schedule(command);
                targetThreadName.append(Thread.currentThread().getName());
            }
        });
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
        final Command cmd = createProject();
        final Command invalidCmd = cmd.toBuilder()
                                      .setContext(CommandContext.getDefaultInstance())
                                      .build();
        return invalidCmd;
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

    /**
     * The dispatcher that remembers that {@link #dispatch(Command)} was called.
     */
    private static class AddTaskDispatcher implements CommandDispatcher {

        private boolean dispatcherInvoked = false;

        @Override
        public Set<CommandClass> getCommandClasses() {
            return CommandClass.setOf(AddTask.class);
        }

        @Override
        public void dispatch(Command request) {
            dispatcherInvoked = true;
        }

        public boolean wasDispatcherInvoked() {
            return dispatcherInvoked;
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
        ProjectCreated handle(CreateProject command, CommandContext ctx) {
            handlerInvoked = true;
            return ProjectCreated.getDefaultInstance();
        }

        public boolean wasHandlerInvoked() {
            return handlerInvoked;
        }
    }

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

        List<Response> getResponses() {
            return ImmutableList.copyOf(responses);
        }

        Throwable getThrowable() {
            return throwable;
        }

        boolean isCompleted() {
            return this.completed;
        }

        boolean isError() {
            return throwable != null;
        }
    }
}
