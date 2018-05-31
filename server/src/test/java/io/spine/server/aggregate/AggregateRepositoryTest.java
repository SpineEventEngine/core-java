/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import com.google.protobuf.UInt64Value;
import io.grpc.stub.StreamObserver;
import io.spine.base.Identifier;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Iterator;
import java.util.Set;

import static io.spine.core.given.GivenTenantId.newUuid;
import static io.spine.server.aggregate.AggregateRepository.DEFAULT_SNAPSHOT_TRIGGER;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("AggregateRepository should")
public class AggregateRepositoryTest {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private BoundedContext boundedContext;
    private AggregateRepository<ProjectId, ProjectAggregate> repository;

    @BeforeEach
    void setUp() {
        ModelTests.clearModel();
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        repository = new ProjectAggregateRepository();
        boundedContext.register(repository);
    }

    @AfterEach
    void tearDown() throws Exception {
        repository.close();
        boundedContext.close();
    }

    @Test
    @DisplayName("not create new aggregates on find")
    void notCreateNewAggregatesOnFind() {
        final ProjectId newId = Sample.messageOfType(ProjectId.class);
        final Optional<ProjectAggregate> optional = repository.find(newId);
        assertFalse(optional.isPresent());
    }

    @Test
    @DisplayName("store and load aggregate")
    void storeAndLoadAggregate() {
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
    @DisplayName("restore aggregate using snapshot")
    void restoreAggregateUsingSnapshot() {
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
    @DisplayName("store snapshot and set event count to zero if needed")
    void storeSnapshotAndManageEventCount() {
        final ProjectAggregate aggregate = GivenAggregate.withUncommittedEvents();
        // This should make the repository write the snapshot.
        repository.setSnapshotTrigger(aggregate.uncommittedEventsCount());

        repository.store(aggregate);
        final AggregateStateRecord record = readRecord(aggregate);
        assertTrue(record.hasSnapshot());
        assertEquals(0, repository.aggregateStorage()
                                  .readEventCountAfterLastSnapshot(aggregate.getId()));
    }

    @Test
    @DisplayName("not store snapshot if not needed")
    void notStoreSnapshotIfNotNeeded() {
        final ProjectAggregate aggregate = GivenAggregate.withUncommittedEvents();

        repository.store(aggregate);
        final AggregateStateRecord record = readRecord(aggregate);
        assertFalse(record.hasSnapshot());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("pass initial snapshot trigger to AggregateReadRequest")
    void passInitialSnapshotTrigger() {
        final AggregateRepository<ProjectId, ProjectAggregate> repositorySpy = spy(repository);
        final AggregateStorage<ProjectId> storageSpy = spy(repositorySpy.aggregateStorage());
        doReturn(storageSpy).when(repositorySpy).aggregateStorage();

        final ProjectId id = Sample.messageOfType(ProjectId.class);
        repositorySpy.loadOrCreate(id);

        final ArgumentCaptor<AggregateReadRequest<ProjectId>> requestCaptor =
                ArgumentCaptor.forClass(AggregateReadRequest.class);
        verify(storageSpy).read(requestCaptor.capture());

        final AggregateReadRequest<ProjectId> passedRequest = requestCaptor.getValue();
        assertEquals(id, passedRequest.getRecordId());
        assertEquals(repositorySpy.getSnapshotTrigger(), passedRequest.getBatchSize());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("pass updated snapshot trigger to AggregateReadRequest")
    void passUpdatedSnapshotTrigger() {
        final AggregateRepository<ProjectId, ProjectAggregate> repositorySpy = spy(repository);
        final AggregateStorage<ProjectId> storageSpy = spy(repositorySpy.aggregateStorage());
        doReturn(storageSpy).when(repositorySpy).aggregateStorage();

        final int nonDefaultSnapshotTrigger = DEFAULT_SNAPSHOT_TRIGGER * 2;
        repositorySpy.setSnapshotTrigger(nonDefaultSnapshotTrigger);
        final ProjectId id = Sample.messageOfType(ProjectId.class);
        repositorySpy.loadOrCreate(id);

        final ArgumentCaptor<AggregateReadRequest<ProjectId>> requestCaptor =
                ArgumentCaptor.forClass(AggregateReadRequest.class);
        verify(storageSpy).read(requestCaptor.capture());

        final AggregateReadRequest<ProjectId> passedRequest = requestCaptor.getValue();
        assertEquals(id, passedRequest.getRecordId());
        assertEquals(nonDefaultSnapshotTrigger, passedRequest.getBatchSize());
    }

    @Test
    @DisplayName("return aggregate class")
    void returnAggregateClass() {
        assertEquals(ProjectAggregate.class, repository.getEntityClass());
    }

    @Test
    @DisplayName("have default value for snapshot trigger")
    void have_default_value_for_snapshot_trigger() {
        assertEquals(DEFAULT_SNAPSHOT_TRIGGER, repository.getSnapshotTrigger());
    }

    @Test
    @DisplayName("not accept negative snapshot trigger")
    void notAcceptNegativeSnapshotTrigger() {
        assertThrows(IllegalArgumentException.class, () -> repository.setSnapshotTrigger(-1));
    }

    @Test
    @DisplayName("not accept zero snapshot trigger")
    void notAcceptZeroSnapshotTrigger() {
        assertThrows(IllegalArgumentException.class, () -> repository.setSnapshotTrigger(0));
    }

    @Test
    @DisplayName("allow to change snapshot trigger")
    void allowToChangeSnapshotTrigger() {
        final int newSnapshotTrigger = 1000;

        repository.setSnapshotTrigger(newSnapshotTrigger);

        assertEquals(newSnapshotTrigger, repository.getSnapshotTrigger());
    }

    @Test
    @DisplayName("expose classes of commands of its aggregate")
    void exposeAggregateCommandClasses() {
        final Set<CommandClass> aggregateCommands =
                Model.getInstance()
                     .asAggregateClass(ProjectAggregate.class)
                     .getCommands();
        final Set<CommandClass> exposedByRepository = repository.getMessageClasses();

        assertTrue(exposedByRepository.containsAll(aggregateCommands));
    }

    @Test
    @DisplayName("find archived aggregates")
    void findArchivedAggregates() {
        final ProjectAggregate aggregate = givenStoredAggregate();

        final AggregateTransaction tx = AggregateTransaction.start(aggregate);
        aggregate.setArchived(true);
        tx.commit();
        repository.store(aggregate);

        assertTrue(repository.find(aggregate.getId())
                              .isPresent());
    }

    @Test
    @DisplayName("find deleted aggregates")
    void findDeletedAggregates() {
        final ProjectAggregate aggregate = givenStoredAggregate();

        final AggregateTransaction tx = AggregateTransaction.start(aggregate);
        aggregate.setDeleted(true);
        tx.commit();

        repository.store(aggregate);

        assertTrue(repository.find(aggregate.getId())
                              .isPresent());
    }

    @Test
    @DisplayName("throw ISE if unable to load entity by id from storage index")
    void throwWhenUnableToLoadEntity() {
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
        assertThrows(IllegalStateException.class, () -> Lists.newArrayList(iterator));
    }

    @Test
    @DisplayName("expose event classes on which aggregates react")
    void expose_event_classes_on_which_aggregates_react() {
        final Set<EventClass> eventClasses = repository.getEventClasses();
        assertTrue(eventClasses.contains(EventClass.of(AggProjectArchived.class)));
        assertTrue(eventClasses.contains(EventClass.of(AggProjectDeleted.class)));
    }

    @Test
    @DisplayName("route events to aggregates")
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
        final Optional<ProjectAggregate> childAfterArchive = repository.find(child.getId());
        assertTrue(childAfterArchive.isPresent());
        assertTrue(childAfterArchive.get()
                                    .isArchived());
        // The parent should not be archived since the dispatch route uses only
        // child aggregates from the `ProjectArchived` event.
        final Optional<ProjectAggregate> parentAfterArchive = repository.find(parent.getId());
        assertTrue(parentAfterArchive.isPresent());
        assertFalse(parentAfterArchive.get()
                                      .isArchived());
    }

    @Test
    @DisplayName("log error when event reaction fails")
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
    @DisplayName("not pass command rejection to onError")
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

    @Test
    @DisplayName("now allow anemic aggregates")
    public void now_allow_anemic_aggregates() {
        assertThrows(IllegalStateException.class,
                     () -> boundedContext.register(new AnemicAggregateRepository()));
    }

    @Test
    @DisplayName("allow aggregates react on events")
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
    @DisplayName("allow aggregates react on rejections")
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

    private AggregateStateRecord readRecord(ProjectAggregate aggregate) {
        final AggregateReadRequest<ProjectId> request =
                new AggregateReadRequest<>(aggregate.getId(), DEFAULT_SNAPSHOT_TRIGGER);
        return repository.aggregateStorage()
                         .read(request)
                         .get();
    }
}
