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

package org.spine3.server.procman;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.server.BoundedContext;
import org.spine3.server.BoundedContextTestStubs;
import org.spine3.server.command.Assign;
import org.spine3.server.command.CommandDispatcher;
import org.spine3.server.entity.IdFunction;
import org.spine3.server.event.GetProducerIdFromEvent;
import org.spine3.server.event.Subscribe;
import org.spine3.server.failure.FailureThrowable;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.CommandClass;
import org.spine3.server.type.EventClass;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.Task;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;
import org.spine3.testdata.TestCommands;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.testdata.TestAggregateIdFactory.newProjectId;
import static org.spine3.testdata.TestCommands.*;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.testdata.TestEventMessageFactory.*;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class ProcessManagerRepositoryShould {

    private static final ProjectId ID = newProjectId();

    private static final CommandContext CMD_CONTEXT = createCommandContext();

    private BoundedContext boundedContext;
    private TestProcessManagerRepository repository;

    @Before
    public void setUp() {
        boundedContext = BoundedContextTestStubs.create();

        boundedContext.getCommandBus().register(new CommandDispatcher() {
            @Override
            public Set<CommandClass> getCommandClasses() {
                return CommandClass.setOf(AddTask.class);
            }

            @Override
            public void dispatch(Command request) throws Exception {
                // Simply swallow the command. We need this dispatcher for allowing Process Manager
                // under test to route the AddTask command.
            }
        });

        repository = new TestProcessManagerRepository(boundedContext);
        repository.initStorage(InMemoryStorageFactory.getInstance());
        TestProcessManager.clearMessageDeliveryHistory();
    }

    @Test
    public void load_empty_manager_by_default() throws InvocationTargetException {
        final TestProcessManager manager = repository.load(ID);
        assertEquals(manager.getDefaultState(), manager.getState());
    }

    @Test
    public void dispatch_event_and_load_manager() throws InvocationTargetException {
        testDispatchEvent(projectCreatedMsg(ID));
    }

    @Test
    public void dispatch_several_events() throws InvocationTargetException {
        testDispatchEvent(projectCreatedMsg(ID));
        testDispatchEvent(taskAddedMsg(ID));
        testDispatchEvent(projectStartedMsg(ID));
    }

    private void testDispatchEvent(Message eventMessage) throws InvocationTargetException {
        final Event event = Events.createEvent(eventMessage, EventContext.getDefaultInstance());
        repository.dispatch(event);
        assertTrue(TestProcessManager.processed(eventMessage));
    }

    @Test
    public void dispatch_command() throws InvocationTargetException, FailureThrowable {
        testDispatchCommand(addTaskMsg(ID));
    }

    @Test
    public void dispatch_several_commands() throws InvocationTargetException, FailureThrowable {
        testDispatchCommand(createProjectMsg(ID));
        testDispatchCommand(addTaskMsg(ID));
        testDispatchCommand(startProjectMsg(ID));
    }

    private void testDispatchCommand(Message cmdMsg) throws InvocationTargetException, FailureThrowable {
        final Command cmd = Commands.create(cmdMsg, CMD_CONTEXT);
        repository.dispatch(cmd);
        assertTrue(TestProcessManager.processed(cmdMsg));
    }

    @Test
    public void dispatch_command_and_return_events() throws InvocationTargetException, FailureThrowable {
        testDispatchCommand(addTaskMsg(ID));

        final ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);

        verify(boundedContext.getEventBus(), times(1)).post(argumentCaptor.capture());

        final Event event = argumentCaptor.getValue();

        assertNotNull(event);
        final TaskAdded message = fromAny(event.getMessage());
        assertEquals(ID, message.getProjectId());
    }

    @Test
    public void return_id_function_for_event_class() {
        final IdFunction function = repository.getIdFunction(EventClass.of(ProjectCreated.class));
        assertNotNull(function);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_dispatch_unknown_command() throws InvocationTargetException, FailureThrowable {
        final Int32Value unknownCommand = Int32Value.getDefaultInstance();
        final Command request = Commands.create(unknownCommand, CommandContext.getDefaultInstance());
        repository.dispatch(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_dispatch_unknown_event() throws InvocationTargetException {
        final StringValue unknownEventMessage = StringValue.getDefaultInstance();
        final Event event = Events.createEvent(unknownEventMessage, EventContext.getDefaultInstance());
        repository.dispatch(event);
    }

    @Test
    public void return_command_classes() {
        final Set<CommandClass> commandClasses = repository.getCommandClasses();
        assertTrue(commandClasses.contains(CommandClass.of(CreateProject.class)));
        assertTrue(commandClasses.contains(CommandClass.of(AddTask.class)));
        assertTrue(commandClasses.contains(CommandClass.of(StartProject.class)));
    }

    @Test
    public void return_event_classes() {
        final Set<EventClass> eventClasses = repository.getEventClasses();
        assertTrue(eventClasses.contains(EventClass.of(ProjectCreated.class)));
        assertTrue(eventClasses.contains(EventClass.of(TaskAdded.class)));
        assertTrue(eventClasses.contains(EventClass.of(ProjectStarted.class)));
    }

    private static class TestProcessManagerRepository
            extends ProcessManagerRepository<ProjectId, TestProcessManager, Project> {

        private TestProcessManagerRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        public IdFunction<ProjectId, ? extends Message, EventContext> getIdFunction(EventClass eventClass) {
            return GetProducerIdFromEvent.newInstance(0);
        }
    }

    private static class TestProcessManager extends ProcessManager<ProjectId, Project> {

        /**
         * The event message we store for inspecting in delivery tests.
         */
        private static final Multimap<ProjectId, Message> messagesDelivered = HashMultimap.create();

        @SuppressWarnings("PublicConstructorInNonPublicClass") /* A Process Manager constructor must be public by
                convention. It is used by reflection and is part of public API of process managers. */
        public TestProcessManager(ProjectId id) {
            super(id);
        }

        private void keep(Message commandOrEventMsg) {
            messagesDelivered.put(getState().getId(), commandOrEventMsg);
        }

        /* package */ static boolean processed(Message eventMessage) {
            final boolean result = messagesDelivered.containsValue(eventMessage);
            return result;
        }

        /* package */ static void clearMessageDeliveryHistory() {
            messagesDelivered.clear();
        }

        // is overridden to make it accessible from tests
        @Override
        @SuppressWarnings("RefusedBequest")
        protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }

        @SuppressWarnings("UnusedParameters") /* The parameter left to show that a projection subscriber
                                                 can have two parameters. */
        @Subscribe
        public void on(ProjectCreated event, EventContext ignored) {
            // Keep the event message for further inspection in tests.
            keep(event);

            handleProjectCreated(event.getProjectId());
        }

        private void handleProjectCreated(ProjectId projectId) {
            final Project newState = getState().toBuilder()
                                               .setId(projectId)
                                               .setStatus(Project.Status.CREATED)
                                               .build();
            incrementState(newState);
        }

        @Subscribe
        public void on(TaskAdded event) {
            keep(event);

            final Task task = event.getTask();
            handleTaskAdded(task);
        }

        private void handleTaskAdded(Task task) {
            final Project newState = getState().toBuilder()
                                               .addTask(task)
                                               .build();
            incrementState(newState);
        }

        @Subscribe
        public void on(ProjectStarted event) {
            keep(event);

            handleProjectStarted();
        }

        private void handleProjectStarted() {
            final Project newState = getState().toBuilder()
                                               .setStatus(Project.Status.STARTED)
                                               .build();
            incrementState(newState);
        }

        @Assign
        public ProjectCreated handle(CreateProject command, CommandContext ignored) {
            keep(command);

            handleProjectCreated(command.getProjectId());
            return projectCreatedMsg(command.getProjectId());
        }

        @Assign
        public TaskAdded handle(AddTask command, CommandContext ignored) {
            keep(command);

            handleTaskAdded(command.getTask());
            return taskAddedMsg(command.getProjectId());
        }

        @Assign
        public CommandRouted handle(StartProject command, CommandContext context) {
            keep(command);

            handleProjectStarted();
            final Message addTask = TestCommands.addTaskMsg(command.getProjectId());

            return newRouter().of(command, context)
                    .add(addTask)
                    .route();
        }
    }
}
