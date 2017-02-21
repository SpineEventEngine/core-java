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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.GreaterThan;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.Assign;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.CommandClass;
import org.spine3.test.Given;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.command.StartProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.ProjectStarted;
import org.spine3.test.aggregate.event.TaskAdded;
import org.spine3.testdata.Sample;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newConcurrentMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.intThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.spine3.server.command.CommandHandlingEntity.getCommandClasses;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.validate.Validate.isDefault;
import static org.spine3.validate.Validate.isNotDefault;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public class AggregateRepositoryShould {

    private AggregateRepository<ProjectId, ProjectAggregate> repository;

    /** Use spy only when it is required to avoid problems, make tests faster and make it easier to debug. */
    private AggregateRepository<ProjectId, ProjectAggregate> repositorySpy;

    @Before
    public void setUp() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        repository = new ProjectAggregateRepository(boundedContext);
        repositorySpy = spy(repository);
    }

    @After
    public void tearDown() throws Exception {
        ProjectAggregate.clearCommandsHandled();
        repository.close();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // OK as the aggregate is created if missing.
    @Test
    public void create_aggregate_with_default_state_if_no_aggregate_found() {
        final ProjectAggregate aggregate = repository.load(Sample.messageOfType(ProjectId.class))
                                                     .get();
        final Project state = aggregate.getState();

        assertTrue(isDefault(state));
    }

    @Test
    public void store_and_load_aggregate() {
        final ProjectId id = Sample.messageOfType(ProjectId.class);
        final ProjectAggregate expected = givenAggregateWithUncommittedEvents(id);

        repository.store(expected);
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        final ProjectAggregate actual = repository.load(id)
                                                  .get();

        assertTrue(isNotDefault(actual.getState()));
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getState(), actual.getState());
    }

    @Test
    public void restore_aggregate_using_snapshot() {
        final ProjectId id = Sample.messageOfType(ProjectId.class);
        final ProjectAggregate expected = givenAggregateWithUncommittedEvents(id);

        repository.setSnapshotTrigger(expected.getUncommittedEvents()
                                              .size());
        repository.store(expected);

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        final ProjectAggregate actual = repository.load(id)
                                                  .get();

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getState(), actual.getState());
    }

    @Test
    public void store_snapshot_and_set_event_count_to_zero_if_needed() {
        final AggregateStorage<ProjectId> storage = givenAggregateStorageMock();
        final ProjectAggregate aggregate = givenAggregateWithUncommittedEvents();
        repositorySpy.setSnapshotTrigger(aggregate.getUncommittedEvents()
                                                  .size());

        repositorySpy.store(aggregate);

        verify(storage).writeSnapshot(any(ProjectId.class), any(Snapshot.class));
        verify(storage).writeEventCountAfterLastSnapshot(any(ProjectId.class), eq(0));
    }

    @Test
    public void not_store_snapshot_if_not_needed() {
        final AggregateStorage<ProjectId> storage = givenAggregateStorageMock();
        final ProjectAggregate aggregate = givenAggregateWithUncommittedEvents();

        repositorySpy.store(aggregate);

        verify(storage, never()).writeSnapshot(any(ProjectId.class), any(Snapshot.class));
        verify(storage).writeEventCountAfterLastSnapshot(any(ProjectId.class), intThat(new GreaterThan<>(0)));
    }

    @Test
    public void return_aggregate_class() {
        assertEquals(ProjectAggregate.class, repository.getAggregateClass());
    }

    @Test
    public void have_default_value_for_snapshot_trigger() {
        assertEquals(AggregateRepository.DEFAULT_SNAPSHOT_TRIGGER, repository.getSnapshotTrigger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_negative_snapshot_trigger() {
        repository.setSnapshotTrigger(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_zero_snapshot_trigger() {
        repository.setSnapshotTrigger(0);
    }

    @Test
    public void allow_to_change_snapshot_trigger() {
        final int newSnapshotTrigger = 1000;

        repository.setSnapshotTrigger(newSnapshotTrigger);

        assertEquals(newSnapshotTrigger, repository.getSnapshotTrigger());
    }

    @Test
    public void expose_classes_of_commands_of_its_aggregate() {
        final Set<CommandClass> aggregateCommands = getCommandClasses(ProjectAggregate.class);
        final Set<CommandClass> exposedByRepository = repository.getCommandClasses();

        assertTrue(exposedByRepository.containsAll(aggregateCommands));
    }

    @Test
    public void marks_aggregate_archived() {
        final ProjectId id = createAndStoreAggregate();

        repository.markArchived(id);

        assertFalse(repository.load(id)
                              .isPresent());
    }

    @Test
    public void mark_aggregate_deleted() {
        final ProjectId id = createAndStoreAggregate();

        repository.markDeleted(id);

        assertFalse(repository.load(id)
                              .isPresent());
    }

    /*
     * Utility methods.
     ****************************/

    private static ProjectAggregate givenAggregateWithUncommittedEvents() {
        return givenAggregateWithUncommittedEvents(Sample.messageOfType(ProjectId.class));
    }

    private static ProjectAggregate givenAggregateWithUncommittedEvents(ProjectId id) {
        final ProjectAggregate aggregate = Given.aggregateOfClass(ProjectAggregate.class)
                                                .withId(id)
                                                .build();

        final CommandContext context = createCommandContext();
        final CreateProject createProject =
                ((CreateProject.Builder) Sample.builderForType(CreateProject.class))
                        .setProjectId(id)
                        .build();
        final AddTask addTask =
                ((AddTask.Builder) Sample.builderForType(AddTask.class))
                        .setProjectId(id)
                        .build();
        final StartProject startProject =
                ((StartProject.Builder) Sample.builderForType(StartProject.class))
                        .setProjectId(id)
                        .build();

        aggregate.dispatchForTest(createProject, context);
        aggregate.dispatchForTest(addTask, context);
        aggregate.dispatchForTest(startProject, context);
        return aggregate;
    }

    private ProjectId createAndStoreAggregate() {
        final ProjectId id = Sample.messageOfType(ProjectId.class);
        final ProjectAggregate aggregate = givenAggregateWithUncommittedEvents(id);

        repository.store(aggregate);
        return id;
    }

    private AggregateStorage<ProjectId> givenAggregateStorageMock() {
        @SuppressWarnings("unchecked")
        final AggregateStorage<ProjectId> storage = mock(AggregateStorage.class);
        doReturn(storage).when(repositorySpy)
                         .aggregateStorage();
        return storage;
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

        static void clearCommandsHandled() {
            commandsHandled.clear();
        }
    }

    private static class ProjectAggregateRepository extends AggregateRepository<ProjectId, ProjectAggregate> {
        protected ProjectAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
            initStorage(InMemoryStorageFactory.getInstance());
        }
    }
}
