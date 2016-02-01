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
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.server.Assign;
import org.spine3.server.Subscribe;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.server.Identifiers.newUuid;
import static org.spine3.testdata.TestAggregateIdFactory.createProjectId;
import static org.spine3.testdata.TestCommands.*;
import static org.spine3.testdata.TestEventMessageFactory.*;

@SuppressWarnings("InstanceMethodNamingConvention")
public class ProcessManagerShould {

    private static final ProjectId ID = createProjectId(newUuid());
    private static final EventContext EVENT_CONTEXT = EventContext.getDefaultInstance();
    private static final CommandContext COMMAND_CONTEXT = CommandContext.getDefaultInstance();

    private TestProcessManager processManager;

    @Before
    public void setUp() {
        processManager = new TestProcessManager(ID);
    }

    @Test
    public void have_default_state_initially() throws InvocationTargetException {
        assertEquals(processManager.getDefaultState(), processManager.getState());
    }

    @Test
    public void dispatch_event() throws InvocationTargetException {
        testDispatchEvent(projectCreatedEvent());
    }

    @Test
    public void dispatch_several_events() throws InvocationTargetException {
        testDispatchEvent(projectCreatedEvent());
        testDispatchEvent(taskAddedEvent());
        testDispatchEvent(projectStartedEvent());
    }

    private void testDispatchEvent(Message event) throws InvocationTargetException {
        processManager.dispatchEvent(event, EVENT_CONTEXT);
        assertEquals(toAny(event), processManager.getState());
    }

    @Test
    public void dispatch_command() throws InvocationTargetException {
        testDispatchCommand(addTask(ID));
    }

    @Test
    public void dispatch_several_commands() throws InvocationTargetException {
        testDispatchCommand(createProject(ID));
        testDispatchCommand(addTask(ID));
        testDispatchCommand(startProject(ID));
    }

    private List<Event> testDispatchCommand(Message command) throws InvocationTargetException {
        final List<Event> events = processManager.dispatchCommand(command, COMMAND_CONTEXT);
        assertEquals(toAny(command), processManager.getState());
        return events;
    }

    @Test
    public void dispatch_command_and_return_events() throws InvocationTargetException {
        final List<Event> events = testDispatchCommand(createProject(ID));

        assertEquals(1, events.size());
        final Event event = events.get(0);
        assertNotNull(event);
        final ProjectCreated message = fromAny(event.getMessage());
        assertEquals(ID, message.getProjectId());
    }

    @Test
    public void dispatch_command_and_return_empty_event_list_if_handler_is_void() throws InvocationTargetException {
        final List<Event> events = testDispatchCommand(startProject(ID));
        assertTrue(events.isEmpty());
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

    private static class TestProcessManager extends ProcessManager<ProjectId, Any> {

        public TestProcessManager(ProjectId id) {
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
        public ProjectCreated handleCommand(CreateProject command, CommandContext ignored) {
            incrementState(toAny(command));
            return projectCreatedEvent(command.getProjectId());
        }

        @Assign
        public TaskAdded handleCommand(AddTask command, CommandContext ignored) {
            incrementState(toAny(command));
            return taskAddedEvent(command.getProjectId());
        }

        @Assign
        public void handleCommand(StartProject command, CommandContext ignored) {
            incrementState(toAny(command));
        }
    }
}
