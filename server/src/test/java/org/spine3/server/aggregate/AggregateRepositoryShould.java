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

package org.spine3.server.aggregate;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.GreaterThan;
import org.spine3.base.CommandContext;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.Assign;
import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.CommandClass;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.TaskAdded;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.validate.Validate.isDefault;

@SuppressWarnings("InstanceMethodNamingConvention")
public class AggregateRepositoryShould {

    private AggregateRepository<ProjectId, ProjectAggregate> repository;

    @Before
    public void setUp() {
        final BoundedContext boundedContext = newBoundedContext();
        repository = new ProjectAggregateRepository(boundedContext);
        repository.initStorage(InMemoryStorageFactory.getInstance());
    }

    @Test
    public void return_default_aggregate_state_if_no_aggregate_found() {
        final ProjectAggregate aggregate = repository.load(Given.AggregateId.newProjectId());
        final Project state = aggregate.getState();

        assertTrue(isDefault(state));
    }

    @Test
    public void store_and_load_aggregate() {
        final ProjectId id = Given.AggregateId.newProjectId();
        final ProjectAggregate expected = givenAggregateWithAppliedEvents(id);

        repository.store(expected);
        final ProjectAggregate actual = repository.load(id);

        assertEquals(expected.getState(), actual.getState());
        assertEquals(expected.getId(), actual.getId());
    }


    @Test
    public void store_snapshot_if_needed() {
        final AggregateRepository<ProjectId, ProjectAggregate> repository = spy(this.repository);
        @SuppressWarnings("unchecked")
        final AggregateStorage<ProjectId> storage = mock(AggregateStorage.class);
        doReturn(storage).when(repository).aggregateStorage();

        final ProjectAggregate aggregate = givenAggregateWithAppliedEvents();
        repository.setSnapshotTrigger(1);
        repository.store(aggregate);

        verify(storage).write(any(ProjectId.class), any(Snapshot.class));
        verify(storage).writeEventCountAfterLastSnapshot(any(ProjectId.class), eq(0));
    }

    @Test
    public void not_store_snapshot_if_not_needed() {
        final AggregateRepository<ProjectId, ProjectAggregate> repository = spy(this.repository);
        @SuppressWarnings("unchecked")
        final AggregateStorage<ProjectId> storage = mock(AggregateStorage.class);
        doReturn(storage).when(repository).aggregateStorage();

        final ProjectAggregate aggregate = givenAggregateWithAppliedEvents();
        repository.store(aggregate);

        verify(storage, never()).write(any(ProjectId.class), any(Snapshot.class));
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
        final Set<CommandClass> aggregateCommands = CommandClass.setOf(Aggregate.getCommandClasses(ProjectAggregate.class));
        final Set<CommandClass> exposedByRepository = repository.getCommandClasses();

        assertTrue(exposedByRepository.containsAll(aggregateCommands));
    }

    private static ProjectAggregate givenAggregateWithAppliedEvents() {
        return givenAggregateWithAppliedEvents(Given.AggregateId.newProjectId());
    }

    private static ProjectAggregate givenAggregateWithAppliedEvents(ProjectId id) {
        final ProjectAggregate aggregate = new ProjectAggregate(id);
        aggregate.dispatchForTest(Given.CommandMessage.createProject(id), createCommandContext());
        aggregate.dispatchForTest(Given.CommandMessage.addTask(id), createCommandContext());
        return aggregate;
    }

    private static class ProjectAggregateRepository extends AggregateRepository<ProjectId, ProjectAggregate> {
        private ProjectAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    private static class ProjectAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

        public ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            return Given.EventMessage.projectCreated(cmd.getProjectId());
        }

        @Assign
        public TaskAdded handle(AddTask cmd, CommandContext ctx) {
            return Given.EventMessage.taskAdded(cmd.getProjectId());
        }

        @Apply
        private void apply(ProjectCreated event) {
            getBuilder().setId(event.getProjectId())
                        .setName("TestProject123");
        }

        @Apply
        private void apply(TaskAdded event) {
            getBuilder().setId(event.getProjectId())
                        .addTask(event.getTask());
        }
    }
}
