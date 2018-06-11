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

    private static ProjectId givenAggregateId(String id) {
        return ProjectId.newBuilder()
                        .setId(id)
                        .build();
    }

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
        ProjectId newId = Sample.messageOfType(ProjectId.class);
        Optional<ProjectAggregate> optional = repository.find(newId);
        assertFalse(optional.isPresent());
    }

    @Test
    @DisplayName("store and load aggregate")
    void storeAndLoadAggregate() {
        ProjectId id = Sample.messageOfType(ProjectId.class);
        ProjectAggregate expected = GivenAggregate.withUncommittedEvents(id);

        repository.store(expected);
        ProjectAggregate actual = assertFound(id);

        assertTrue(isNotDefault(actual.getState()));
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getState(), actual.getState());
    }

    private ProjectAggregate assertFound(ProjectId id) {
        Optional<ProjectAggregate> optional = repository.find(id);
        assertTrue(optional.isPresent());
        return optional.get();
    }

    @Test
    @DisplayName("restore aggregate using snapshot")
    void restoreAggregateUsingSnapshot() {
        ProjectId id = Sample.messageOfType(ProjectId.class);
        ProjectAggregate expected = GivenAggregate.withUncommittedEvents(id);

        repository.setSnapshotTrigger(expected.uncommittedEventsCount());
        repository.store(expected);

        ProjectAggregate actual = assertFound(id);

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getState(), actual.getState());
    }

    @Test
    @DisplayName("store snapshot and set event count to zero if needed")
    void storeSnapshotAndManageEventCount() {
        ProjectAggregate aggregate = GivenAggregate.withUncommittedEvents();
        // This should make the repository write the snapshot.
        repository.setSnapshotTrigger(aggregate.uncommittedEventsCount());

        repository.store(aggregate);
        AggregateStateRecord record = readRecord(aggregate);
        assertTrue(record.hasSnapshot());
        assertEquals(0, repository.aggregateStorage()
                                  .readEventCountAfterLastSnapshot(aggregate.getId()));
    }

    @Test
    @DisplayName("not store snapshot if not needed")
    void notStoreSnapshotIfNotNeeded() {
        ProjectAggregate aggregate = GivenAggregate.withUncommittedEvents();

        repository.store(aggregate);
        AggregateStateRecord record = readRecord(aggregate);
        assertFalse(record.hasSnapshot());
    }

    @SuppressWarnings({"unchecked", "CheckReturnValue" /* calling mock */})
    @Test
    @DisplayName("pass initial snapshot trigger to AggregateReadRequest")
    void passInitialSnapshotTrigger() {
        AggregateRepository<ProjectId, ProjectAggregate> repositorySpy = spy(repository);
        AggregateStorage<ProjectId> storageSpy = spy(repositorySpy.aggregateStorage());
        doReturn(storageSpy).when(repositorySpy)
                            .aggregateStorage();

        ProjectId id = Sample.messageOfType(ProjectId.class);
        repositorySpy.loadOrCreate(id);

        ArgumentCaptor<AggregateReadRequest<ProjectId>> requestCaptor =
                ArgumentCaptor.forClass(AggregateReadRequest.class);
        verify(storageSpy).read(requestCaptor.capture());

        AggregateReadRequest<ProjectId> passedRequest = requestCaptor.getValue();
        assertEquals(id, passedRequest.getRecordId());
        assertEquals(repositorySpy.getSnapshotTrigger(), passedRequest.getBatchSize());
    }

    @SuppressWarnings({"unchecked", "CheckReturnValue" /* calling mock */})
    @Test
    @DisplayName("pass updated snapshot trigger to AggregateReadRequest")
    void passUpdatedSnapshotTrigger() {
        AggregateRepository<ProjectId, ProjectAggregate> repositorySpy = spy(repository);
        AggregateStorage<ProjectId> storageSpy = spy(repositorySpy.aggregateStorage());
        doReturn(storageSpy).when(repositorySpy)
                            .aggregateStorage();

        int nonDefaultSnapshotTrigger = DEFAULT_SNAPSHOT_TRIGGER * 2;
        repositorySpy.setSnapshotTrigger(nonDefaultSnapshotTrigger);
        ProjectId id = Sample.messageOfType(ProjectId.class);
        repositorySpy.loadOrCreate(id);

        ArgumentCaptor<AggregateReadRequest<ProjectId>> requestCaptor =
                ArgumentCaptor.forClass(AggregateReadRequest.class);
        verify(storageSpy).read(requestCaptor.capture());

        AggregateReadRequest<ProjectId> passedRequest = requestCaptor.getValue();
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
        int newSnapshotTrigger = 1000;

        repository.setSnapshotTrigger(newSnapshotTrigger);

        assertEquals(newSnapshotTrigger, repository.getSnapshotTrigger());
    }

    @Test
    @DisplayName("expose classes of commands of its aggregate")
    void exposeAggregateCommandClasses() {
        Set<CommandClass> aggregateCommands =
                Model.getInstance()
                     .asAggregateClass(ProjectAggregate.class)
                     .getCommands();
        Set<CommandClass> exposedByRepository = repository.getMessageClasses();

        assertTrue(exposedByRepository.containsAll(aggregateCommands));
    }

    @Test
    @DisplayName("find archived aggregates")
    void findArchivedAggregates() {
        ProjectAggregate aggregate = givenStoredAggregate();

        AggregateTransaction tx = AggregateTransaction.start(aggregate);
        aggregate.setArchived(true);
        tx.commit();
        repository.store(aggregate);

        assertTrue(repository.find(aggregate.getId())
                             .isPresent());
    }

    @Test
    @DisplayName("find deleted aggregates")
    void findDeletedAggregates() {
        ProjectAggregate aggregate = givenStoredAggregate();

        AggregateTransaction tx = AggregateTransaction.start(aggregate);
        aggregate.setDeleted(true);
        tx.commit();

        repository.store(aggregate);

        assertTrue(repository.find(aggregate.getId())
                             .isPresent());
    }

    @SuppressWarnings("CheckReturnValue") // The returned value is not used in this test.
    @Test
    @DisplayName("throw ISE if unable to load entity by id from storage index")
    void throwWhenUnableToLoadEntity() {
        // Store a valid aggregate.
        givenStoredAggregate();

        // Store a troublesome entity, which cannot be loaded.
        TenantAwareOperation op = new TenantAwareOperation(newUuid()) {
            @Override
            public void run() {
                givenStoredAggregateWithId(ProjectAggregateRepository.troublesome.getId());
            }
        };
        op.execute();

        Iterator<ProjectAggregate> iterator =
                repository.iterator(Predicates.alwaysTrue());

        // This should iterate through all and fail.
        assertThrows(IllegalStateException.class, () -> Lists.newArrayList(iterator));
    }

    @Test
    @DisplayName("expose event classes on which aggregates react")
    void exposeAggregateEventClasses() {
        Set<EventClass> eventClasses = repository.getEventClasses();
        assertTrue(eventClasses.contains(EventClass.of(AggProjectArchived.class)));
        assertTrue(eventClasses.contains(EventClass.of(AggProjectDeleted.class)));
    }

    @Test
    @DisplayName("route events to aggregates")
    void routeEventsToAggregates() {
        ProjectAggregate parent = givenStoredAggregate();
        ProjectAggregate child = givenStoredAggregate();

        assertTrue(repository.find(parent.getId())
                             .isPresent());
        assertTrue(repository.find(child.getId())
                             .isPresent());

        TestEventFactory factory = TestEventFactory.newInstance(getClass());
        AggProjectArchived msg = AggProjectArchived.newBuilder()
                                                   .setProjectId(parent.getId())
                                                   .addChildProjectId(child.getId())
                                                   .build();
        Event event = factory.createEvent(msg);

        boundedContext.getEventBus()
                      .post(event);

        // Check that the child aggregate was archived.
        Optional<ProjectAggregate> childAfterArchive = repository.find(child.getId());
        assertTrue(childAfterArchive.isPresent());
        assertTrue(childAfterArchive.get()
                                    .isArchived());
        // The parent should not be archived since the dispatch route uses only
        // child aggregates from the `ProjectArchived` event.
        Optional<ProjectAggregate> parentAfterArchive = repository.find(parent.getId());
        assertTrue(parentAfterArchive.isPresent());
        assertFalse(parentAfterArchive.get()
                                      .isArchived());
    }

    @Test
    @DisplayName("log error when event reaction fails")
    void logErrorWhenEventReactionFails() {
        FailingAggregateRepository repository = new FailingAggregateRepository();
        boundedContext.register(repository);

        TestEventFactory factory = TestEventFactory.newInstance(getClass());

        // Passing negative float value should cause an exception.
        EventEnvelope envelope =
                EventEnvelope.of(factory.createEvent(FloatValue.newBuilder()
                                                               .setValue(-412.0f)
                                                               .build()));
        boundedContext.getEventBus()
                      .post(envelope.getOuterObject());

        assertTrue(repository.isErrorLogged());
        RuntimeException lastException = repository.getLastException();
        assertTrue(lastException instanceof HandlerMethodFailedException);

        HandlerMethodFailedException methodFailedException =
                (HandlerMethodFailedException) lastException;

        assertEquals(envelope.getMessage(), methodFailedException.getDispatchedMessage());
        assertEquals(envelope.getEventContext(), methodFailedException.getMessageContext());

        MessageEnvelope lastErrorEnvelope = repository.getLastErrorEnvelope();
        assertNotNull(lastErrorEnvelope);
        assertTrue(lastErrorEnvelope instanceof EventEnvelope);
        assertEquals(envelope.getMessage(), lastErrorEnvelope.getMessage());
    }

    @Test
    @DisplayName("not pass command rejection to `onError`")
    void notPassCommandRejectionToOnError() {
        FailingAggregateRepository repository = new FailingAggregateRepository();
        boundedContext.register(repository);

        TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(getClass());

        // Passing negative long value to `FailingAggregate` should cause a rejection.
        CommandEnvelope ce = CommandEnvelope.of(
                requestFactory.createCommand(UInt64Value.newBuilder()
                                                        .setValue(-100_000_000L)
                                                        .build()));
        boundedContext.getCommandBus()
                      .post(ce.getCommand(), StreamObservers.<Ack>noOpObserver());

        assertFalse(repository.isErrorLogged());
    }

    @Test
    @DisplayName("not allow anemic aggregates")
    void notAllowAnemicAggregates() {
        assertThrows(IllegalStateException.class,
                     () -> boundedContext.register(new AnemicAggregateRepository()));
    }

    @Test
    @DisplayName("allow aggregates to react on events")
    void allowAggregatesReactOnEvents() {
        ReactingRepository repository = new ReactingRepository();
        boundedContext.register(repository);

        ProjectId parentId = givenAggregateId("parent");
        ProjectId childId = givenAggregateId("child");

        /**
         * Create event factory for which producer ID would be the `parentId`.
         * Custom routing set by {@linkplain ReactingRepository()} would use
         * child IDs from the event.
         */
        TestEventFactory factory = TestEventFactory.newInstance(Identifier.pack(parentId),
                                                                getClass());
        AggProjectArchived msg = AggProjectArchived.newBuilder()
                                                   .setProjectId(parentId)
                                                   .addChildProjectId(childId)
                                                   .build();
        Event event = factory.createEvent(msg);

        // Posting this event should archive the aggregate.
        boundedContext.getEventBus()
                      .post(event);

        // Check that the aggregate marked itself as `archived`, and therefore became invisible
        // to regular queries.
        Optional<ReactingAggregate> optional = repository.find(childId);

        // The aggregate was created because of dispatching.
        assertTrue(optional.isPresent());

        // The proper method was called, which we check by the state the aggregate got.
        assertEquals(ReactingAggregate.PROJECT_ARCHIVED,
                     optional.get()
                             .getState()
                             .getValue());
    }

    /*
     * Test environment methods that use internals of this test suite
     * (and because of that are not moved to the test environment outside of this class).
     **************************************************************************************/

    @Test
    @DisplayName("allow aggregates to react on rejections")
    void allowAggregatesReactOnRejections() {
        boundedContext.register(new RejectingRepository());
        RejectionReactingRepository repository = new RejectionReactingRepository();
        boundedContext.register(repository);

        ProjectId parentId = givenAggregateId("rejectingParent");
        ProjectId childId1 = givenAggregateId("acceptingChild-1");
        ProjectId childId2 = givenAggregateId("acceptingChild-2");
        ProjectId childId3 = givenAggregateId("acceptingChild-3");

        StreamObserver<Ack> observer = StreamObservers.noOpObserver();
        CommandBus commandBus = boundedContext.getCommandBus();

        // Create the parent project.
        ImmutableSet<ProjectId> childProjects = ImmutableSet.of(childId1, childId2, childId3);
        Command createParent = requestFactory.createCommand(
                AggCreateProjectWithChildren.newBuilder()
                                            .setProjectId(parentId)
                                            .addAllChildProjectId(childProjects)
                                            .build()
        );
        commandBus.post(createParent, observer);

        // Fire a command which would cause rejection.
        Command startProject = requestFactory.createCommand(
                AggStartProjectWithChildren.newBuilder()
                                           .setProjectId(parentId)
                                           .build()
        );
        commandBus.post(startProject, observer);

        for (ProjectId childProject : childProjects) {
            Optional<RejectionReactingAggregate> optional = repository.find(childProject);
            assertTrue(optional.isPresent());

            // Check that all the aggregates:
            // 1. got Rejections.AggCannotStartArchivedProject;
            // 2. produced the state the event;
            // 3. applied the event.
            String value = optional.get()
                                   .getState()
                                   .getValue();
            assertEquals(RejectionReactingAggregate.PARENT_ARCHIVED, value);
        }
    }

    private ProjectAggregate givenStoredAggregate() {
        ProjectId id = Sample.messageOfType(ProjectId.class);
        ProjectAggregate aggregate = GivenAggregate.withUncommittedEvents(id);

        repository.store(aggregate);
        return aggregate;
    }

    private void givenStoredAggregateWithId(String id) {
        ProjectId projectId = givenAggregateId(id);
        ProjectAggregate aggregate = GivenAggregate.withUncommittedEvents(projectId);

        repository.store(aggregate);
    }

    private AggregateStateRecord readRecord(ProjectAggregate aggregate) {
        AggregateReadRequest<ProjectId> request =
                new AggregateReadRequest<>(aggregate.getId(), DEFAULT_SNAPSHOT_TRIGGER);
        Optional<AggregateStateRecord> optional = repository.aggregateStorage()
                                                            .read(request);
        assertTrue(optional.isPresent());
        return optional.get();
    }
}
