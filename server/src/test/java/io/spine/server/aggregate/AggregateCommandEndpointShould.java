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

package io.spine.server.aggregate;

import com.google.common.base.Optional;
import io.spine.annotation.Subscribe;
import io.spine.base.Command;
import io.spine.base.CommandContext;
import io.spine.base.Identifiers;
import io.spine.envelope.CommandEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHistory;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.event.EventSubscriber;
import io.spine.server.storage.StorageFactorySwitch;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectValidatingBuilder;
import io.spine.test.aggregate.command.AddTask;
import io.spine.test.aggregate.command.CreateProject;
import io.spine.test.aggregate.command.StartProject;
import io.spine.test.aggregate.event.ProjectCreated;
import io.spine.test.aggregate.event.ProjectStarted;
import io.spine.test.aggregate.event.TaskAdded;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static io.spine.server.aggregate.Given.ACommand.addTask;
import static io.spine.server.aggregate.Given.ACommand.createProject;
import static io.spine.server.aggregate.Given.ACommand.startProject;
import static io.spine.testdata.TestBoundedContextFactory.MultiTenant.newBoundedContext;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AggregateCommandEndpointShould {

    private AggregateRepository<ProjectId, ProjectAggregate> repository;

    /**
     * The spy to be used when it is required to avoid problems, make tests faster and easier.
     */
    private AggregateRepository<ProjectId, ProjectAggregate> repositorySpy;

    private ProjectId projectId;
    private Subscriber subscriber;

    @Before
    public void setUp() {
        projectId = ProjectId.newBuilder()
                             .setId(Identifiers.newUuid())
                             .build();

        final CommandStore commandStore = mock(CommandStore.class);
        final CommandBus.Builder commandBus = CommandBus.newBuilder()
                                                        .setMultitenant(true)
                                                        .setCommandStore(commandStore);
        final BoundedContext boundedContext = newBoundedContext(commandBus);
        subscriber = new Subscriber();

        boundedContext.getEventBus().register(subscriber);

        repository = new ProjectAggregateRepository(boundedContext);
        boundedContext.register(repository);
        repositorySpy = spy(repository);
    }

    private static class Subscriber extends EventSubscriber {

        private ProjectCreated remembered;

        @Subscribe
        void on(ProjectCreated msg) {
            remembered = msg;
        }
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

        final ProjectCreated msg = subscriber.remembered;
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
        doReturn(0, 1)
                .when(storage).readEventCountAfterLastSnapshot(projectId);
        doReturn(Optional.absent())
                .when(storage).read(projectId);
        doReturn(storage)
                .when(repositorySpy).aggregateStorage();
        doReturn(Optional.absent())
                .when(storage).readLifecycleFlags(projectId);

        repositorySpy.dispatch(cmd);

        // Load should be executed twice due to repeated dispatching.
        verify(repositorySpy, times(2))
                .loadOrCreate(projectId);

        // Reading event count is executed 2 times per dispatch (so 2 * 2)
        // plus once upon storing the state.
        verify(storage, times(2 * 2 + 1))
                .readEventCountAfterLastSnapshot(projectId);
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

    private static ProjectAggregate verifyAggregateStored(AggregateRepository<ProjectId,
            AggregateCommandEndpointShould.ProjectAggregate> repository) {
        final ArgumentCaptor<AggregateCommandEndpointShould.ProjectAggregate> aggregateCaptor =
                ArgumentCaptor.forClass(ProjectAggregate.class);
        verify(repository).store(aggregateCaptor.capture());
        return aggregateCaptor.getValue();
    }

    private void assertDispatches(Command cmd) {
        final CommandEnvelope envelope = CommandEnvelope.of(cmd);
        repository.dispatch(envelope);
        ProjectAggregate.assertHandled(cmd);
    }

    /*
     * Test environment classes and utilities
     *****************************************/

    private static class ProjectAggregate
            extends Aggregate<ProjectId, Project, ProjectValidatingBuilder> {

        // Needs to be `static` to share the state updates in scope of the test.
        private static final CommandHistory commandsHandled = new CommandHistory();

        private ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        ProjectCreated handle(CreateProject msg, CommandContext context) {
            commandsHandled.add(msg, context);
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
            commandsHandled.add(msg, context);
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
            commandsHandled.add(msg, context);
            return ProjectStarted.newBuilder()
                                 .setProjectId(msg.getProjectId())
                                 .build();
        }

        @Apply
        private void apply(ProjectStarted event) {
        }

        private static void assertHandled(Command expected) {
            commandsHandled.assertHandled(expected);
        }

        static void clearCommandsHandled() {
            commandsHandled.clear();
        }
    }

    private static class ProjectAggregateRepository
        extends AggregateRepository<ProjectId, AggregateCommandEndpointShould.ProjectAggregate> {
        protected ProjectAggregateRepository(BoundedContext boundedContext) {
            super();
            initStorage(StorageFactorySwitch.get(boundedContext.isMultitenant()));
        }
    }
}
