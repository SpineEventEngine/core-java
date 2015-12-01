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

package org.spine3.server.process;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.testdata.TestAggregateIdFactory.createProjectId;

@SuppressWarnings("InstanceMethodNamingConvention")
public class ProcessManagerShould {

    private TestProcessManager processManager;
    private static final ProjectId PROJECT_ID = createProjectId("testProject");

    @Before
    public void setUpTest() {
        processManager = new TestProcessManager(1);
    }

    @Test
    public void have_default_state_initially() throws InvocationTargetException {
        assertEquals(processManager.getDefaultState(), processManager.getState());
    }

    @Test
    public void dispatch_event() throws InvocationTargetException {
        final ProjectCreated event = projectCreated();

        processManager.dispatchEvent(event, EventContext.getDefaultInstance());

        assertEquals(event, processManager.getState());
    }

    @Test
    public void dispatch_several_events() throws InvocationTargetException {
        final ProjectCreated created = projectCreated();
        processManager.dispatchEvent(created, EventContext.getDefaultInstance());
        assertEquals(created, processManager.getState());

        final TaskAdded taskAdded = taskAdded();
        processManager.dispatchEvent(taskAdded, EventContext.getDefaultInstance());
        assertEquals(taskAdded, processManager.getState());

        final ProjectStarted projectStarted = projectStarted();
        processManager.dispatchEvent(projectStarted, EventContext.getDefaultInstance());
        assertEquals(projectStarted, processManager.getState());
    }

    @Test
    public void dispatch_command() throws InvocationTargetException {
        final AddTask command = addTask();

        processManager.dispatchCommand(command, CommandContext.getDefaultInstance());

        assertEquals(command, processManager.getState());
    }

    @Test
    public void dispatch_command_and_return_events() throws InvocationTargetException {
        final AddTask command = addTask();
        final List<EventRecord> records = processManager.dispatchCommand(command, CommandContext.getDefaultInstance());

        assertEquals(command, processManager.getState());
        assertEquals(1, records.size());

        final EventRecord record = records.get(0);
        assertNotNull(record);
        final TaskAdded event = fromAny(record.getEvent());
        assertEquals(PROJECT_ID, event.getProjectId());
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
            final TaskAdded event = TaskAdded.newBuilder().setProjectId(command.getProjectId()).build();
            return event;
        }

        @Override
        protected StringValue getDefaultState() {
            return StringValue.getDefaultInstance();
        }
    }

    private static ProjectCreated projectCreated() {
        return ProjectCreated.newBuilder().setProjectId(PROJECT_ID).build();
    }

    private static TaskAdded taskAdded() {
        return TaskAdded.newBuilder().setProjectId(PROJECT_ID).build();
    }

    private static ProjectStarted projectStarted() {
        return ProjectStarted.newBuilder().setProjectId(PROJECT_ID).build();
    }

    private static AddTask addTask() {
        return AddTask.newBuilder().setProjectId(PROJECT_ID).build();
    }
}
