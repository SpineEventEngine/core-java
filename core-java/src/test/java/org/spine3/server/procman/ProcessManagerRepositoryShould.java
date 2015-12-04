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

import com.google.common.collect.Multimap;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.server.procman.error.MissingProcessManagerIdException;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.server.procman.ProcessManagerShould.TestProcessManager;
import static org.spine3.testdata.TestAggregateIdFactory.createProjectId;
import static org.spine3.testdata.TestCommandFactory.*;
import static org.spine3.testdata.TestEventFactory.*;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class ProcessManagerRepositoryShould {

    private static final ProjectId ID = createProjectId("project78");
    private static final EventContext EVENT_CONTEXT = EventContext.getDefaultInstance();
    private static final CommandContext COMMAND_CONTEXT = CommandContext.getDefaultInstance();

    private final TestProcessManagerRepository repository = new TestProcessManagerRepository();

    @Before
    public void setUpTest() {
        final EntityStorage<ProjectId, Message> storage = InMemoryStorageFactory.getInstance().createEntityStorage(TestProcessManager.class);
        repository.assignStorage(storage);
    }

    @Test
    public void load_empty_manager_by_default() throws InvocationTargetException {
        final TestProcessManager manager = repository.load(ID);
        assertEquals(manager.getDefaultState(), manager.getState());
    }

    @Test
    public void dispatch_event_and_load_manager() throws InvocationTargetException {
        testDispatchEvent(projectCreatedEvent(ID));
    }

    @Test
    public void dispatch_several_events() throws InvocationTargetException {
        testDispatchEvent(projectCreatedEvent(ID));
        testDispatchEvent(taskAddedEvent(ID));
        testDispatchEvent(projectStartedEvent(ID));
    }

    private void testDispatchEvent(Message event) throws InvocationTargetException {
        repository.dispatchEvent(event, EVENT_CONTEXT);
        final TestProcessManager manager = repository.load(ID);
        assertEquals(event, manager.getState());
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

    private List<EventRecord> testDispatchCommand(Message command) throws InvocationTargetException {
        final List<EventRecord> records = repository.dispatchCommand(command, COMMAND_CONTEXT);
        final TestProcessManager manager = repository.load(ID);
        assertEquals(command, manager.getState());
        return records;
    }

    @Test
    public void dispatch_command_and_return_events() throws InvocationTargetException {
        final List<EventRecord> records = testDispatchCommand(addTask(ID));

        assertEquals(1, records.size());
        final EventRecord record = records.get(0);
        assertNotNull(record);
        final TaskAdded event = fromAny(record.getEvent());
        assertEquals(ID, event.getProjectId());
    }

    @Test(expected = MissingProcessManagerIdException.class)
    public void throw_exception_if_dispatch_unknown_command() throws InvocationTargetException {
        final Int32Value unknownCommand = Int32Value.getDefaultInstance();
        repository.dispatchCommand(unknownCommand, COMMAND_CONTEXT);
    }

    @Test(expected = MissingProcessManagerIdException.class)
    public void throw_exception_if_dispatch_unknown_event() throws InvocationTargetException {
        final StringValue unknownEvent = StringValue.getDefaultInstance();
        repository.dispatchEvent(unknownEvent, EVENT_CONTEXT);
    }

    @Test
    public void return_command_and_event_handlers() {
        final Multimap<Method, Class<? extends Message>> result = repository.getHandlers();

        assertEquals(2,  result.keySet().size());
        assertEquals(6,  result.values().size());
        assertContainsCommandClasses(result);
        assertContainsEventClasses(result);
    }

    private static void assertContainsEventClasses(Multimap<Method, Class<? extends Message>> result) {
        assertTrue(result.containsValue(ProjectCreated.class));
        assertTrue(result.containsValue(TaskAdded.class));
        assertTrue(result.containsValue(ProjectStarted.class));
    }

    private static void assertContainsCommandClasses(Multimap<Method, Class<? extends Message>> result) {
        assertTrue(result.containsValue(CreateProject.class));
        assertTrue(result.containsValue(AddTask.class));
        assertTrue(result.containsValue(StartProject.class));
    }

    @Test
    public void return_id_from_command_message() {
        final ProjectId actual = repository.getId(createProject(ID));
        assertEquals(ID, actual);
    }

    @Test
    public void return_id_from_event_message() {
        final ProjectId actual = repository.getId(projectCreatedEvent(ID));
        assertEquals(ID, actual);
    }

    private static class TestProcessManagerRepository
            extends ProcessManagerRepository<ProjectId, ProcessManagerShould.TestProcessManager, Message> {
    }
}
