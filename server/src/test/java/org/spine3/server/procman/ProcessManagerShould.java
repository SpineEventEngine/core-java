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

import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.protobuf.Messages;
import org.spine3.server.command.Assign;
import org.spine3.server.command.CommandBus;
import org.spine3.server.event.Subscribe;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;
import org.spine3.testdata.TestCommands;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testdata.TestAggregateIdFactory.newProjectId;
import static org.spine3.testdata.TestCommands.*;
import static org.spine3.testdata.TestEventMessageFactory.*;

@SuppressWarnings("InstanceMethodNamingConvention")
public class ProcessManagerShould {

    private static final ProjectId ID = newProjectId();
    private static final EventContext EVENT_CONTEXT = EventContext.getDefaultInstance();
    private static final CommandContext COMMAND_CONTEXT = CommandContext.getDefaultInstance();

    private CommandBus commandBus;

    private TestProcessManager processManager;

    @Before
    public void setUp() {
        commandBus = Mockito.mock(CommandBus.class);

        processManager = new TestProcessManager(ID);
    }

    @Test
    public void have_default_state_initially() throws InvocationTargetException {
        assertEquals(processManager.getDefaultState(), processManager.getState());
    }

    @Test
    public void dispatch_event() throws InvocationTargetException {
        testDispatchEvent(projectCreatedMsg());
    }

    @Test
    public void dispatch_several_events() throws InvocationTargetException {
        testDispatchEvent(projectCreatedMsg());
        testDispatchEvent(taskAddedMsg());
        testDispatchEvent(projectStartedMsg());
    }

    private void testDispatchEvent(Message event) throws InvocationTargetException {
        processManager.dispatchEvent(event, EVENT_CONTEXT);
        assertEquals(toAny(event), processManager.getState());
    }

    @Test
    public void dispatch_command() throws InvocationTargetException {
        testDispatchCommand(addTaskMsg(ID));
    }

    @Test
    public void dispatch_several_commands() throws InvocationTargetException {
        processManager.setCommandBus(commandBus);
        testDispatchCommand(createProjectMsg(ID));
        testDispatchCommand(addTaskMsg(ID));
        testDispatchCommand(startProjectMsg(ID));
    }

    private List<Event> testDispatchCommand(Message command) throws InvocationTargetException {
        final List<Event> events = processManager.dispatchCommand(command, COMMAND_CONTEXT);
        assertEquals(toAny(command), processManager.getState());
        return events;
    }

    @Test
    public void dispatch_command_and_return_events() throws InvocationTargetException {
        final List<Event> events = testDispatchCommand(createProjectMsg(ID));

        assertEquals(1, events.size());
        final Event event = events.get(0);
        assertNotNull(event);
        final ProjectCreated message = fromAny(event.getMessage());
        assertEquals(ID, message.getProjectId());
    }

    /**
     * Tests command routing.
     *
     * @see TestProcessManager#handle(StartProject, CommandContext)
     */
    @Test
    public void route_commands() throws InvocationTargetException {
        processManager.setCommandBus(commandBus);
        final List<Event> events = testDispatchCommand(startProjectMsg(ID));

        // There's only one event generated.
        assertEquals(1, events.size());

        final Event event = events.get(0);

        // The producer of the event is our Process Manager.
        assertEquals(processManager.getId(), Events.getProducer(event.getContext()));

        final Message message = Messages.fromAny(event.getMessage());

        // The event type is CommandRouted.
        assertTrue(message instanceof CommandRouted);

        final CommandRouted commandRouted = (CommandRouted) message;

        // The source of the command is StartProject.
        assertTrue(Commands.getMessage(commandRouted.getSource()) instanceof StartProject);

        // The produced command was posted to CommandBus once, and the same command is in the generated event.
        verify(commandBus, times(1)).post(commandRouted.getProduced(0));
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_dispatch_unknown_command() throws InvocationTargetException {
        final Int32Value unknownCommand = Int32Value.getDefaultInstance();
        processManager.dispatchCommand(unknownCommand, COMMAND_CONTEXT);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_dispatch_unknown_event() throws InvocationTargetException {
        final StringValue unknownEvent = StringValue.getDefaultInstance();
        processManager.dispatchEvent(unknownEvent, EVENT_CONTEXT);
    }

    @Test
    public void return_handled_command_classes() {
        final Set<Class<? extends Message>> classes = ProcessManager.getHandledCommandClasses(TestProcessManager.class);
        assertEquals(3, classes.size());
        assertTrue(classes.contains(CreateProject.class));
        assertTrue(classes.contains(AddTask.class));
        assertTrue(classes.contains(StartProject.class));
    }

    @Test
    public void return_handled_event_classes() {
        final Set<Class<? extends Message>> classes = ProcessManager.getHandledEventClasses(TestProcessManager.class);
        assertEquals(3, classes.size());
        assertTrue(classes.contains(ProjectCreated.class));
        assertTrue(classes.contains(TaskAdded.class));
        assertTrue(classes.contains(ProjectStarted.class));
    }

    @SuppressWarnings("UnusedParameters") // OK for test class.
    private static class TestProcessManager extends ProcessManager<ProjectId, Any> {

        private TestProcessManager(ProjectId id) {
            super(id);
        }

        @Override
        @SuppressWarnings("RefusedBequest")
        protected Any getDefaultState() {
            return Any.getDefaultInstance();
        }

        @Subscribe
        public void on(ProjectCreated event, EventContext ignored) {
            incrementState(toAny(event));
        }

        @Subscribe
        public void on(TaskAdded event, EventContext ignored) {
            incrementState(toAny(event));
        }

        @Subscribe
        public void on(ProjectStarted event, EventContext ignored) {
            incrementState(toAny(event));
        }

        @Assign
        public ProjectCreated handle(CreateProject command, CommandContext ignored) {
            incrementState(toAny(command));
            return projectCreatedMsg(command.getProjectId());
        }

        @Assign
        public TaskAdded handle(AddTask command, CommandContext ignored) {
            incrementState(toAny(command));
            return taskAddedMsg(command.getProjectId());
        }

        @Assign
        public CommandRouted handle(StartProject command, CommandContext context) {
            incrementState(toAny(command));

            final Message startProject = TestCommands.startProjectMsg(command.getProjectId());
            return newRouter().of(command, context)
                    .add(startProject)
                    .route();
        }
    }
}
