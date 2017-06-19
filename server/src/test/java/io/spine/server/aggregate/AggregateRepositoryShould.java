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
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import io.spine.base.CommandClass;
import io.spine.base.CommandContext;
import io.spine.client.TestActorRequestFactory;
import io.spine.envelope.CommandEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.command.Assign;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.test.Given;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AddTask;
import io.spine.test.aggregate.command.CreateProject;
import io.spine.test.aggregate.command.StartProject;
import io.spine.test.aggregate.event.ProjectCreated;
import io.spine.test.aggregate.event.ProjectStarted;
import io.spine.test.aggregate.event.TaskAdded;
import io.spine.testdata.Sample;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.GreaterThan;

import java.util.Iterator;
import java.util.Set;

import static io.spine.server.aggregate.AggregateCommandDispatcher.dispatch;
import static io.spine.test.Values.newTenantUuid;
import static io.spine.validate.Validate.isDefault;
import static io.spine.validate.Validate.isNotDefault;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass", "ConstantConditions"})
public class AggregateRepositoryShould {

    private AggregateRepository<ProjectId, ProjectAggregate> repository;

    /** Use spy only when it is required to avoid problems,
     * make tests faster and make it easier to debug. */
    private AggregateRepository<ProjectId, ProjectAggregate> repositorySpy;

    private static final TestActorRequestFactory factory =
            TestActorRequestFactory.newInstance(AggregateRepositoryShould.class);

    @Before
    public void setUp() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        repository = new ProjectAggregateRepository();
        boundedContext.register(repository);
        repositorySpy = spy(repository);
    }

    @After
    public void tearDown() throws Exception {
        repository.close();
    }

    @Test
    public void call_get_aggregate_constructor_method_only_once(){
        final ProjectId id = Sample.messageOfType(ProjectId.class);
        repositorySpy.create(id);
        repositorySpy.create(id);

        verify(repositorySpy, times(1)).findEntityConstructor();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // OK as the aggregate is created if missing.
    @Test
    public void create_aggregate_with_default_state_if_no_aggregate_found() {
        final ProjectAggregate aggregate = repository.find(Sample.messageOfType(ProjectId.class))
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
        final ProjectAggregate actual = repository.find(id)
                                                  .get();

        assertTrue(isNotDefault(actual.getState()));
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getState(), actual.getState());
    }

    @Test(expected = IllegalStateException.class)
    public void reject_invalid_AggregateStateRecord_instance() {
        final ProjectId id = Sample.messageOfType(ProjectId.class);
        final AggregateStorage<ProjectId> storage = givenAggregateStorageMock();

        doReturn(Optional.of(AggregateStateRecord.getDefaultInstance()))
                .when(storage)
                .read(id);

        repositorySpy.loadOrCreate(id);
    }

    @Test
    public void restore_aggregate_using_snapshot() {
        final ProjectId id = Sample.messageOfType(ProjectId.class);
        final ProjectAggregate expected = givenAggregateWithUncommittedEvents(id);

        repository.setSnapshotTrigger(expected.getUncommittedEvents()
                                              .size());
        repository.store(expected);

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        final ProjectAggregate actual = repository.find(id)
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
        verify(storage).writeEventCountAfterLastSnapshot(any(ProjectId.class),
                                                         intThat(new GreaterThan<>(0)));
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
        final Set<CommandClass> aggregateCommands =
                Aggregate.TypeInfo.getCommandClasses(ProjectAggregate.class);
        final Set<CommandClass> exposedByRepository = repository.getMessageClasses();

        assertTrue(exposedByRepository.containsAll(aggregateCommands));
    }

    @Test
    public void mark_aggregate_archived() {
        final ProjectAggregate aggregate = createAndStoreAggregate();

        final AggregateTransaction tx = AggregateTransaction.start(aggregate);
        // archive the aggregate.
        aggregate.setArchived(true);
        tx.commit();
        repository.store(aggregate);

        assertFalse(repository.find(aggregate.getId())
                              .isPresent());
    }

    @Test
    public void mark_aggregate_deleted() {
        final ProjectAggregate aggregate = createAndStoreAggregate();

        final AggregateTransaction tx = AggregateTransaction.start(aggregate);
        aggregate.setDeleted(true);
        tx.commit();

        repository.store(aggregate);

        assertFalse(repository.find(aggregate.getId())
                              .isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_ISE_if_unable_to_load_entity_by_id_from_storage_index() {
        createAndStoreAggregate();

        // Store a troublesome entity, which cannot be loaded.
        final TenantAwareOperation op = new TenantAwareOperation(newTenantUuid()) {
            @Override
            public void run() {
                createAndStore(ProjectAggregateRepository.troublesome.getId());
            }
        };
        op.execute();

        final Iterator<ProjectAggregate> iterator = repository.iterator(
                Predicates.<ProjectAggregate>alwaysTrue());

        // This should iterate through all.
        Lists.newArrayList(iterator);
    }

    /*
     * Utility methods.
     ****************************/

    private static ProjectAggregate givenAggregateWithUncommittedEvents() {
        return givenAggregateWithUncommittedEvents(Sample.messageOfType(ProjectId.class));
    }

    private static CommandEnvelope env(Message commandMessage) {
        return CommandEnvelope.of(factory.command().create(commandMessage));
    }

    private static ProjectAggregate givenAggregateWithUncommittedEvents(ProjectId id) {
        final ProjectAggregate aggregate = Given.aggregateOfClass(ProjectAggregate.class)
                                                .withId(id)
                                                .build();

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

        dispatch(aggregate, env(createProject));
        dispatch(aggregate, env(addTask));
        dispatch(aggregate, env(startProject));

        return aggregate;
    }

    private ProjectAggregate createAndStoreAggregate() {
        final ProjectId id = Sample.messageOfType(ProjectId.class);
        final ProjectAggregate aggregate = givenAggregateWithUncommittedEvents(id);

        repository.store(aggregate);
        return aggregate;
    }

    private void createAndStore(String id) {
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId(id)
                                             .build();
        final ProjectAggregate aggregate = givenAggregateWithUncommittedEvents(projectId);

        repository.store(aggregate);
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

    @SuppressWarnings("RedundantMethodOverride")
    private static class ProjectAggregate
            extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        private ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        ProjectCreated handle(CreateProject msg, CommandContext context) {
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
            return TaskAdded.newBuilder()
                            .setProjectId(msg.getProjectId())
                            .setTask(msg.getTask())
                            .build();
        }

        @Apply
        private void apply(TaskAdded event) {
            getBuilder().setId(event.getProjectId())
                        .addTask(event.getTask());
        }

        @Assign
        ProjectStarted handle(StartProject msg, CommandContext context) {
            return ProjectStarted.newBuilder()
                                 .setProjectId(msg.getProjectId())
                                 .build();
        }

        @Apply
        private void apply(ProjectStarted event) {
            getBuilder().setStatus(Status.STARTED);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Overrides to open the method to this test suite.
         */
        @Override
        protected void setArchived(boolean archived) {
            super.setArchived(archived);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Overrides to open the method to this test suite.
         */
        @Override
        protected void setDeleted(boolean deleted) {
            super.setDeleted(deleted);
        }
    }

    private static class ProjectAggregateRepository
                   extends AggregateRepository<ProjectId, ProjectAggregate> {

        private static final ProjectId troublesome = ProjectId.newBuilder()
                                                              .setId("CANNOT_BE_LOADED")
                                                              .build();
        @Override
        public Optional<ProjectAggregate> find(ProjectId id) {
            if (id.equals(troublesome)) {
                return Optional.absent();
            }
            return super.find(id);
        }
    }
}
