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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.protobuf.FloatValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import io.grpc.stub.StreamObserver;
import io.spine.Identifier;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.MessageEnvelope;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.AnemicAggregateRepository;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.FailingAggregateRepository;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.GivenAggregate;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.ProjectAggregate;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.ProjectAggregateRepository;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.ReactingAggregate;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.ReactingRepository;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.RejectingRepository;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.RejectionReactingAggregate;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.RejectionReactingRepository;
import io.spine.server.command.TestEventFactory;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.model.HandlerMethodFailedException;
import io.spine.server.model.Model;
import io.spine.server.model.ModelTests;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggCreateProjectWithChildren;
import io.spine.test.aggregate.command.AggStartProjectWithChildren;
import io.spine.test.aggregate.event.AggProjectArchived;
import io.spine.test.aggregate.event.AggProjectDeleted;
import io.spine.testdata.Sample;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.Set;

import static io.spine.core.given.GivenTenantId.newUuid;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AggregateRepositoryShould {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private BoundedContext boundedContext;
    private AggregateRepository<ProjectId, ProjectAggregate> repository;

    /**
     * Use spy only when it is required to avoid problems,
     * make tests faster and make it easier to debug.
     */
    private AggregateRepository<ProjectId, ProjectAggregate> repositorySpy;

    @Before
    public void setUp() {
        ModelTests.clearModel();
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        repository = new ProjectAggregateRepository();
        boundedContext.register(repository);
        repositorySpy = spy(repository);
    }

    @After
    public void tearDown() throws Exception {
        repository.close();
        boundedContext.close();
    }

    @Test
    public void call_get_aggregate_constructor_method_only_once() {
        final ProjectId id = Sample.messageOfType(ProjectId.class);
        repositorySpy.create(id);
        repositorySpy.create(id);

        verify(repositorySpy, times(1)).findEntityConstructor();
    }

    @Test
    public void do_not_create_new_aggregates_on_find() {
        final ProjectId newId = Sample.messageOfType(ProjectId.class);
        final Optional<ProjectAggregate> optional = repository.find(newId);
        assertFalse(optional.isPresent());
    }

    @Test
    public void store_and_load_aggregate() {
        final ProjectId id = Sample.messageOfType(ProjectId.class);
        final ProjectAggregate expected = GivenAggregate.withUncommittedEvents(id);

        repository.store(expected);
        final ProjectAggregate actual = repository.find(id)
                                                  .get();

        assertTrue(isNotDefault(actual.getState()));
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getState(), actual.getState());
    }

    @Test
    public void restore_aggregate_using_snapshot() {
        final ProjectId id = Sample.messageOfType(ProjectId.class);
        final ProjectAggregate expected = GivenAggregate.withUncommittedEvents(id);

        repository.setSnapshotTrigger(expected.uncommittedEventsCount());
        repository.store(expected);

        final ProjectAggregate actual = repository.find(id)
                                                  .get();

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getState(), actual.getState());
    }

    @Test
    public void store_snapshot_and_set_event_count_to_zero_if_needed() {
        final ProjectAggregate aggregate = GivenAggregate.withUncommittedEvents();
        // This should make the repository write the snapshot.
        repository.setSnapshotTrigger(aggregate.uncommittedEventsCount());

        repository.store(aggregate);
        final AggregateStateRecord record = repository.aggregateStorage()
                                                      .read(aggregate.getId())
                                                      .get();
        assertTrue(record.hasSnapshot());
        assertEquals(0, repository.aggregateStorage()
                                  .readEventCountAfterLastSnapshot(aggregate.getId()));
    }

    @Test
    public void not_store_snapshot_if_not_needed() {
        final ProjectAggregate aggregate = GivenAggregate.withUncommittedEvents();

        repository.store(aggregate);
        final AggregateStateRecord record = repository.aggregateStorage()
                                                      .read(aggregate.getId())
                                                      .get();
        assertFalse(record.hasSnapshot());
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
                Model.getInstance()
                     .asAggregateClass(ProjectAggregate.class)
                     .getCommands();
        final Set<CommandClass> exposedByRepository = repository.getMessageClasses();

        assertTrue(exposedByRepository.containsAll(aggregateCommands));
    }

    @Test
    public void not_find_archived_aggregates() {
        final ProjectAggregate aggregate = givenStoredAggregate();

        final AggregateTransaction tx = AggregateTransaction.start(aggregate);
        aggregate.setArchived(true);
        tx.commit();
        repository.store(aggregate);

        assertFalse(repository.find(aggregate.getId())
                              .isPresent());
    }

    @Test
    public void not_find_deleted_aggregates() {
        final ProjectAggregate aggregate = givenStoredAggregate();

        final AggregateTransaction tx = AggregateTransaction.start(aggregate);
        aggregate.setDeleted(true);
        tx.commit();

        repository.store(aggregate);

        assertFalse(repository.find(aggregate.getId())
                              .isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_ISE_if_unable_to_load_entity_by_id_from_storage_index() {
        // Store a valid aggregate.
        givenStoredAggregate();

        // Store a troublesome entity, which cannot be loaded.
        final TenantAwareOperation op = new TenantAwareOperation(newUuid()) {
            @Override
            public void run() {
                givenStoredAggregateWithId(ProjectAggregateRepository.troublesome.getId());
            }
        };
        op.execute();

        final Iterator<ProjectAggregate> iterator =
                repository.iterator(Predicates.<ProjectAggregate>alwaysTrue());

        // This should iterate through all and fail.
        Lists.newArrayList(iterator);
    }

    @Test
    public void expose_event_classes_on_which_aggregates_react() {
        final Set<EventClass> eventClasses = repository.getEventClasses();
        assertTrue(eventClasses.contains(EventClass.of(AggProjectArchived.class)));
        assertTrue(eventClasses.contains(EventClass.of(AggProjectDeleted.class)));
    }

    @Test
    public void route_events_to_aggregates() {
        final ProjectAggregate parent = givenStoredAggregate();
        final ProjectAggregate child = givenStoredAggregate();

        assertTrue(repository.find(parent.getId())
                             .isPresent());
        assertTrue(repository.find(child.getId())
                             .isPresent());

        final TestEventFactory factory = TestEventFactory.newInstance(getClass());
        final AggProjectArchived msg = AggProjectArchived.newBuilder()
                                                   .setProjectId(parent.getId())
                                                   .addChildProjectId(child.getId())
                                                   .build();
        final Event event = factory.createEvent(msg);

        boundedContext.getEventBus()
                      .post(event);

        // Check that the child aggregate was archived.
        assertFalse(repository.find(child.getId())
                              .isPresent());

        // The parent should not be archived since the dispatch route uses only
        // child aggregates from the `ProjectArchived` event.
        assertTrue(repository.find(parent.getId())
                             .isPresent());
    }

    @Test
    public void log_error_when_command_dispatching_fails() {
        final FailingAggregateRepository repository = new FailingAggregateRepository();
        boundedContext.register(repository);

        // Passing negative value to `FailingAggregate` should cause exception.
        final CommandEnvelope ce = CommandEnvelope.of(
                requestFactory.createCommand(UInt32Value.newBuilder()
                                                        .setValue(-100)
                                                        .build()));

        boundedContext.getCommandBus()
                      .post(ce.getCommand(), StreamObservers.<Ack>noOpObserver());

        assertTrue(repository.isErrorLogged());
        final RuntimeException lastException = repository.getLastException();
        assertTrue(lastException instanceof HandlerMethodFailedException);

        final HandlerMethodFailedException methodFailedException =
                (HandlerMethodFailedException) lastException;

        assertEquals(ce.getMessage(), methodFailedException.getDispatchedMessage());
        assertEquals(ce.getCommandContext(), methodFailedException.getMessageContext());

        final MessageEnvelope lastErrorEnvelope = repository.getLastErrorEnvelope();
        assertNotNull(lastErrorEnvelope);
        assertTrue(lastErrorEnvelope instanceof CommandEnvelope);
        assertEquals(ce.getMessage(), lastErrorEnvelope.getMessage());
    }

    @Test
    public void log_error_when_event_reaction_fails() {
        final FailingAggregateRepository repository = new FailingAggregateRepository();
        boundedContext.register(repository);

        final TestEventFactory factory = TestEventFactory.newInstance(getClass());

        // Passing negative float value should cause an exception.
        final EventEnvelope envelope =
                EventEnvelope.of(factory.createEvent(FloatValue.newBuilder()
                                                               .setValue(-412.0f)
                                                               .build()));
        boundedContext.getEventBus()
                      .post(envelope.getOuterObject());

        assertTrue(repository.isErrorLogged());
        final RuntimeException lastException = repository.getLastException();
        assertTrue(lastException instanceof HandlerMethodFailedException);

        final HandlerMethodFailedException methodFailedException =
                (HandlerMethodFailedException) lastException;

        assertEquals(envelope.getMessage(), methodFailedException.getDispatchedMessage());
        assertEquals(envelope.getEventContext(), methodFailedException.getMessageContext());

        final MessageEnvelope lastErrorEnvelope = repository.getLastErrorEnvelope();
        assertNotNull(lastErrorEnvelope);
        assertTrue(lastErrorEnvelope instanceof EventEnvelope);
        assertEquals(envelope.getMessage(), lastErrorEnvelope.getMessage());
    }

    @Test
    public void not_pass_command_rejection_to_onError() {
        final FailingAggregateRepository repository = new FailingAggregateRepository();
        boundedContext.register(repository);

        final TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(getClass());

        // Passing negative long value to `FailingAggregate` should cause a rejection.
        final CommandEnvelope ce = CommandEnvelope.of(
                requestFactory.createCommand(UInt64Value.newBuilder()
                                                        .setValue(-100_000_000L)
                                                        .build()));
        boundedContext.getCommandBus()
                      .post(ce.getCommand(), StreamObservers.<Ack>noOpObserver());

        assertFalse(repository.isErrorLogged());
    }

    @Test(expected = IllegalStateException.class)
    public void now_allow_anemic_aggregates() {
        boundedContext.register(new AnemicAggregateRepository());
    }

    @Test
    public void allow_aggregates_react_on_events() {
        final ReactingRepository repository = new ReactingRepository();
        boundedContext.register(repository);

        final ProjectId parentId = givenAggregateId("parent");
        final ProjectId childId = givenAggregateId("child");

        /**
         * Create event factory for which producer ID would be the `parentId`.
         * Custom routing set by {@linkplain ReactingRepository()} would use
         * child IDs from the event.
         */
        final TestEventFactory factory = TestEventFactory.newInstance(Identifier.pack(parentId),
                                                                      getClass());
        final AggProjectArchived msg = AggProjectArchived.newBuilder()
                                                         .setProjectId(parentId)
                                                         .addChildProjectId(childId)
                                                         .build();
        final Event event = factory.createEvent(msg);

        // Posting this event should archive the aggregate.
        boundedContext.getEventBus()
                      .post(event);

        // Check that the aggregate marked itself as `archived`, and therefore became invisible
        // to regular queries.
        final Optional<ReactingAggregate> optional = repository.find(childId);

        // The aggregate was created because of dispatching.
        assertTrue(optional.isPresent());

        // The proper method was called, which we check by the state the aggregate got.
        assertEquals(ReactingAggregate.PROJECT_ARCHIVED,
                     optional.get()
                             .getState()
                             .getValue());
    }

    @Test
    public void allow_aggregates_react_on_rejections() {
        boundedContext.register(new RejectingRepository());
        final RejectionReactingRepository repository = new RejectionReactingRepository();
        boundedContext.register(repository);

        final ProjectId parentId = givenAggregateId("rejectingParent");
        final ProjectId childId1 = givenAggregateId("acceptingChild-1");
        final ProjectId childId2 = givenAggregateId("acceptingChild-2");
        final ProjectId childId3 = givenAggregateId("acceptingChild-3");


        final StreamObserver<Ack> observer = StreamObservers.noOpObserver();
        final CommandBus commandBus = boundedContext.getCommandBus();

        // Create the parent project.
        final ImmutableSet<ProjectId> childProjects = ImmutableSet.of(childId1, childId2, childId3);
        final Command createParent = requestFactory.createCommand(
                AggCreateProjectWithChildren.newBuilder()
                                            .setProjectId(parentId)
                                            .addAllChildProjectId(childProjects)
                                            .build()
        );
        commandBus.post(createParent, observer);

        // Fire a command which would cause rejection.
        final Command startProject = requestFactory.createCommand(
                AggStartProjectWithChildren.newBuilder()
                                           .setProjectId(parentId)
                                           .build()
        );
        commandBus.post(startProject, observer);

        for (ProjectId childProject : childProjects) {
            final Optional<RejectionReactingAggregate> optional = repository.find(childProject);
            assertTrue(optional.isPresent());

            // Check that all the aggregates:
            // 1. got Rejections.AggCannotStartArchivedProject;
            // 2. produced the state the event;
            // 3. applied the event.
            final String value = optional.get()
                                         .getState()
                                         .getValue();
            assertEquals(RejectionReactingAggregate.PARENT_ARCHIVED, value);
        }
    }

    /*
     * Test environment methods that use internals of this test suite
     * (and because of that are not moved to the test environment outside of this class).
     **************************************************************************************/

    private ProjectAggregate givenStoredAggregate() {
        final ProjectId id = Sample.messageOfType(ProjectId.class);
        final ProjectAggregate aggregate = GivenAggregate.withUncommittedEvents(id);

        repository.store(aggregate);
        return aggregate;
    }

    private void givenStoredAggregateWithId(String id) {
        final ProjectId projectId = givenAggregateId(id);
        final ProjectAggregate aggregate = GivenAggregate.withUncommittedEvents(projectId);

        repository.store(aggregate);
    }

    private static ProjectId givenAggregateId(String id) {
        return ProjectId.newBuilder()
                        .setId(id)
                        .build();
    }
}
