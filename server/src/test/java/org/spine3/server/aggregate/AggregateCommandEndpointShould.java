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

package org.spine3.server.aggregate;

import com.google.common.base.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandEnvelope;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Commands;
import org.spine3.base.Event;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.Assign;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandStore;
import org.spine3.server.event.EventBus;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.command.StartProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.ProjectStarted;
import org.spine3.test.aggregate.event.TaskAdded;
import org.spine3.testdata.Exemplum;

import java.util.Map;

import static com.google.common.collect.Maps.newConcurrentMap;
import static java.util.Collections.emptyIterator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.base.Events.getMessage;
import static org.spine3.server.aggregate.Given.Command.addTask;
import static org.spine3.server.aggregate.Given.Command.createProject;
import static org.spine3.server.aggregate.Given.Command.startProject;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;

public class AggregateCommandEndpointShould {

    private AggregateRepository<ProjectId, ProjectAggregate> repository;

    /** Use spy only when it is required to avoid problems, make tests faster and make it easier to debug. */
    private AggregateRepository<ProjectId, ProjectAggregate> repositorySpy;
    private EventBus eventBus;

    private final ProjectId projectId = Exemplum.messageOfType(ProjectId.class);

    @Before
    public void setUp() {
        eventBus = mock(EventBus.class);
        final CommandStore commandStore = mock(CommandStore.class);
        doReturn(emptyIterator()).when(commandStore)
                                 .iterator(any(CommandStatus.class)); // to avoid NPE
        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setCommandStore(commandStore)
                                                .build();
        final BoundedContext boundedContext = newBoundedContext(commandBus, eventBus);
        repository = new ProjectAggregateRepository(boundedContext);
        repositorySpy = spy(repository);
    }

    @After
    public void tearDown() throws Exception {
        ProjectAggregate.clearCommandsHandled();
        repository.close();
    }

    @Test
    public void post_events_on_command_dispatching() {
        final CommandEnvelope cmd = CommandEnvelope.of(createProject(projectId));

        repository.dispatch(cmd);

        final Event event = verifyEventPosted();
        final ProjectCreated msg = getMessage(event);
        assertEquals(projectId, msg.getProjectId());
    }

    @Test
    public void store_aggregate_on_command_dispatching() {
        final CommandEnvelope cmd = CommandEnvelope.of(createProject(projectId));
        final CreateProject msg = (CreateProject) cmd.getMessage();

        repositorySpy.dispatch(cmd);

        final ProjectAggregate aggregate = verifyAggregateStored(repositorySpy);
        assertEquals(projectId, aggregate.getId());
        assertEquals(msg.getName(), aggregate.getState()
                                             .getName());
    }

    @Test
    public void repeat_command_dispatching_if_event_count_is_changed_during_dispatching() {
        @SuppressWarnings("unchecked")
        final AggregateStorage<ProjectId> storage = mock(AggregateStorage.class);
        final CommandEnvelope cmd = CommandEnvelope.of(createProject(projectId));

        // Change reported event count upon the second invocation and trigger re-dispatch.
        doReturn(0, 1).when(storage).readEventCountAfterLastSnapshot(projectId);
        doReturn(Optional.of(AggregateStateRecord.getDefaultInstance())).when(storage).read(projectId);
        doReturn(storage).when(repositorySpy).aggregateStorage();
        doReturn(Optional.absent()).when(storage).readVisibility(projectId);

        repositorySpy.dispatch(cmd);

        // Load should be executed twice due to repeated dispatching.
        verify(repositorySpy, times(2)).loadOrCreate(projectId);

        // Reading event count is executed 2 times per dispatch (so 2 * 2) plus once upon storing the state.
        verify(storage, times(2 * 2 + 1)).readEventCountAfterLastSnapshot(projectId);
    }

    @Test
    public void dispatch_command() {
        assertDispatches(createProject());
    }

    @Test
    public void dispatch_several_commands() {
        assertDispatches(createProject(projectId));
        assertDispatches(addTask(projectId));
        assertDispatches(startProject(projectId));
    }

    /*
     * Utility methods.
     ****************************/

    private Event verifyEventPosted() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).post(eventCaptor.capture());
        return eventCaptor.getValue();
    }

    private static ProjectAggregate verifyAggregateStored(AggregateRepository<ProjectId,
            AggregateCommandEndpointShould.ProjectAggregate> repository) {
        final ArgumentCaptor<AggregateCommandEndpointShould.ProjectAggregate> aggregateCaptor = ArgumentCaptor.forClass(ProjectAggregate.class);
        verify(repository).store(aggregateCaptor.capture());
        return aggregateCaptor.getValue();
    }

    private void assertDispatches(Command cmd) {
        final CommandEnvelope envelope = CommandEnvelope.of(cmd);
        repository.dispatch(envelope);
        ProjectAggregate.assertHandled(cmd);
    }

    /*
     * Test environment classes
     ****************************/

    private static class ProjectAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

        // Needs to be `static` to share the state updates in scope of the test.
        private static final Map<CommandId, Command> commandsHandled = newConcurrentMap();

        private ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        ProjectCreated handle(CreateProject msg, CommandContext context) {
            final Command cmd = Commands.createCommand(msg, context);
            commandsHandled.put(context.getCommandId(), cmd);
            return ProjectCreated.newBuilder()
                                 .setProjectId(msg.getProjectId())
                                 .setName(msg.getName())
                                 .build();
        }

        @Apply
        private void apply(ProjectCreated event) {
            getBuilder().setId(event.getProjectId())
                        .setName(event.getName());
        }

        @Assign
        TaskAdded handle(AddTask msg, CommandContext context) {
            final Command cmd = Commands.createCommand(msg, context);
            commandsHandled.put(context.getCommandId(), cmd);
            return TaskAdded.newBuilder()
                            .setProjectId(msg.getProjectId())
                            .build();
        }

        @Apply
        private void apply(TaskAdded event) {
            getBuilder().setId(event.getProjectId());
        }

        @Assign
        ProjectStarted handle(StartProject msg, CommandContext context) {
            final Command cmd = Commands.createCommand(msg, context);
            commandsHandled.put(context.getCommandId(), cmd);
            return ProjectStarted.newBuilder()
                                 .setProjectId(msg.getProjectId())
                                 .build();
        }

        @Apply
        private void apply(ProjectStarted event) {
        }

        static void assertHandled(Command expected) {
            final CommandId id = Commands.getId(expected);
            final Command actual = commandsHandled.get(id);
            final String cmdName = Commands.getMessage(expected)
                                           .getClass()
                                           .getName();
            assertNotNull("No such command handled: " + cmdName, actual);
            assertEquals(expected, actual);
        }

        static void clearCommandsHandled() {
            commandsHandled.clear();
        }
    }

    private static class ProjectAggregateRepository
            extends AggregateRepository<ProjectId, AggregateCommandEndpointShould.ProjectAggregate> {
        protected ProjectAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
            initStorage(InMemoryStorageFactory.getInstance());
        }
    }
}
