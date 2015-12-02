/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.eventbus.Subscribe;
import org.spine3.server.Assign;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.testdata.TestAggregateIdFactory.createProjectId;
import static org.spine3.testdata.TestEventFactory.*;

@SuppressWarnings("InstanceMethodNamingConvention")
public class ProcessManagerShould {

    private final TestProcessManager processManager = new TestProcessManager(1);


    @Test
    public void have_default_state_initially() throws InvocationTargetException {
        assertEquals(processManager.getDefaultState(), processManager.getState());
    }

    @Test
    public void dispatch_event() throws InvocationTargetException {
        final ProjectCreated event = projectCreatedEvent();

        processManager.dispatchEvent(event, EventContext.getDefaultInstance());

        assertEquals(event, processManager.getState());
    }

    @Test
    public void dispatch_several_events() throws InvocationTargetException {
        final ProjectCreated created = projectCreatedEvent();
        processManager.dispatchEvent(created, EventContext.getDefaultInstance());
        assertEquals(created, processManager.getState());

        final TaskAdded taskAdded = taskAddedEvent();
        processManager.dispatchEvent(taskAdded, EventContext.getDefaultInstance());
        assertEquals(taskAdded, processManager.getState());

        final ProjectStarted projectStarted = projectStartedEvent();
        processManager.dispatchEvent(projectStarted, EventContext.getDefaultInstance());
        assertEquals(projectStarted, processManager.getState());
    }

    @Test
    public void dispatch_command() throws InvocationTargetException {
        final AddTask command = addTask(createProjectId("p123"));

        processManager.dispatchCommand(command, CommandContext.getDefaultInstance());

        assertEquals(command, processManager.getState());
    }

    @Test
    public void dispatch_command_and_return_events() throws InvocationTargetException {
        final ProjectId projectId = createProjectId("p12");
        final AddTask command = addTask(projectId);
        final List<EventRecord> records = processManager.dispatchCommand(command, CommandContext.getDefaultInstance());

        assertEquals(command, processManager.getState());
        assertEquals(1, records.size());

        final EventRecord record = records.get(0);
        assertNotNull(record);
        final TaskAdded event = fromAny(record.getEvent());
        assertEquals(projectId, event.getProjectId());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_dispatch_unknown_command() throws InvocationTargetException {
        final Int32Value unknownCommand = Int32Value.getDefaultInstance();
        processManager.dispatchCommand(unknownCommand, CommandContext.getDefaultInstance());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_dispatch_unknown_event() throws InvocationTargetException {
        final StringValue unknownEvent = StringValue.getDefaultInstance();
        processManager.dispatchEvent(unknownEvent, EventContext.getDefaultInstance());
    }

    @Test
    public void return_handled_command_classes() {
        final Set<Class<? extends Message>> classes = ProcessManager.getHandledCommandClasses(TestProcessManager.class);
        assertEquals(1, classes.size());
        assertTrue(classes.contains(AddTask.class));
    }

    @Test
    public void return_handled_event_classes() {
        final Set<Class<? extends Message>> classes = ProcessManager.getHandledEventClasses(TestProcessManager.class);
        assertEquals(3, classes.size());
        assertTrue(classes.contains(ProjectCreated.class));
        assertTrue(classes.contains(TaskAdded.class));
        assertTrue(classes.contains(ProjectStarted.class));
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public static class TestProcessManager extends ProcessManager<Integer, Message> {

        public TestProcessManager(Integer id) {
            super(id);
        }

        @Subscribe
        public void on(ProjectCreated event, EventContext ignored) {
            incrementState(event);
        }

        @Subscribe
        public void on(TaskAdded event, EventContext ignored) {
            incrementState(event);
        }

        @Subscribe
        public void on(ProjectStarted event, EventContext ignored) {
            incrementState(event);
        }

        @Assign
        public TaskAdded handleCommand(AddTask command, CommandContext ignored) {
            incrementState(command);
            return taskAddedEvent(command.getProjectId());
        }

        @Override
        protected StringValue getDefaultState() {
            return StringValue.getDefaultInstance();
        }
    }

    private static AddTask addTask(ProjectId id) {
        return AddTask.newBuilder().setProjectId(id).build();
    }
}
