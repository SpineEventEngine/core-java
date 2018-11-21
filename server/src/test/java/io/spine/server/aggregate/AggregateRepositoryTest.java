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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import io.grpc.stub.StreamObserver;
import io.spine.base.Identifier;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.MessageEnvelope;
import io.spine.grpc.StreamObservers;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.AnemicAggregateRepository;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.EventDiscardingAggregateRepository;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.FailingAggregateRepository;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.GivenAggregate;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.ProjectAggregate;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.ProjectAggregateRepository;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.ReactingAggregate;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.ReactingRepository;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.RejectingRepository;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.RejectionReactingAggregate;
import io.spine.server.aggregate.given.AggregateRepositoryTestEnv.RejectionReactingRepository;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.model.HandlerMethodFailedException;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.Task;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggCreateProjectWithChildren;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.command.AggStartProjectWithChildren;
import io.spine.test.aggregate.event.AggProjectArchived;
import io.spine.test.aggregate.event.AggProjectDeleted;
import io.spine.test.aggregate.number.FloatEncountered;
import io.spine.test.aggregate.number.RejectNegativeLong;
import io.spine.testdata.Sample;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.ShardingReset;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import static io.spine.server.aggregate.AggregateRepository.DEFAULT_SNAPSHOT_TRIGGER;
import static io.spine.server.aggregate.given.AggregateRepositoryTestEnv.boundedContext;
import static io.spine.server.aggregate.given.AggregateRepositoryTestEnv.givenAggregateId;
import static io.spine.server.aggregate.given.AggregateRepositoryTestEnv.givenStoredAggregate;
import static io.spine.server.aggregate.given.AggregateRepositoryTestEnv.givenStoredAggregateWithId;
import static io.spine.server.aggregate.given.AggregateRepositoryTestEnv.repository;
import static io.spine.server.aggregate.given.AggregateRepositoryTestEnv.requestFactory;
import static io.spine.server.aggregate.given.AggregateRepositoryTestEnv.resetBoundedContext;
import static io.spine.server.aggregate.given.AggregateRepositoryTestEnv.resetRepository;
import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;
import static io.spine.testing.core.given.GivenTenantId.newUuid;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvents;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEventsHadVersions;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names */})
@DisplayName("AggregateRepository should")
public class AggregateRepositoryTest {

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        resetBoundedContext();
        resetRepository();
        boundedContext().register(repository());
    }

    @AfterEach
    void tearDown() throws Exception {
        boundedContext().close();
    }

    @Nested
    @DisplayName("expose")
    class Expose {

        @Test
        @DisplayName("aggregate class")
        void aggregateClass() {
            assertEquals(ProjectAggregate.class, repository().getEntityClass());
        }

        @Test
        @DisplayName("command classes handled by aggregate")
        void aggregateCommandClasses() {
            Set<CommandClass> aggregateCommands =
                    asAggregateClass(ProjectAggregate.class)
                            .getCommands();
            Set<CommandClass> exposedByRepository = repository().getMessageClasses();

            assertTrue(exposedByRepository.containsAll(aggregateCommands));
        }

        @Test
        @DisplayName("event classes on which aggregate reacts")
        void aggregateEventClasses() {
            Set<EventClass> eventClasses = repository().getEventClasses();
            assertTrue(eventClasses.contains(EventClass.from(AggProjectArchived.class)));
            assertTrue(eventClasses.contains(EventClass.from(AggProjectDeleted.class)));
        }
    }

    @Nested
    @DisplayName("store and load aggregate")
    class StoreAndLoadAggregate {

        @Test
        @DisplayName("using snapshot")
        void usingSnapshot() {
            ProjectId id = Sample.messageOfType(ProjectId.class);
            ProjectAggregate expected = GivenAggregate.withUncommittedEvents(id);

            UncommittedEvents events = ((Aggregate<?, ?, ?>) expected).getUncommittedEvents();
            repository().setSnapshotTrigger(events.list()
                                                  .size());
            repository().store(expected);

            ProjectAggregate actual = assertFound(id);

            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getState(), actual.getState());
        }

        @Test
        @DisplayName("without using snapshot")
        void notUsingSnapshot() {
            ProjectId id = Sample.messageOfType(ProjectId.class);
            ProjectAggregate expected = GivenAggregate.withUncommittedEvents(id);

            repository().store(expected);
            ProjectAggregate actual = assertFound(id);

            assertTrue(isNotDefault(actual.getState()));
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getState(), actual.getState());
        }

        private ProjectAggregate assertFound(ProjectId id) {
            Optional<ProjectAggregate> optional = repository().find(id);
            assertTrue(optional.isPresent());
            return optional.get();
        }
    }

    @Nested
    @DisplayName("manage snapshots and event count properly")
    class ManageSnapshots {

        @Test
        @DisplayName("when it's required to store snapshot")
        void whenNeededToStore() {
            ProjectAggregate aggregate = GivenAggregate.withUncommittedEvents();
            // This should make the repository write the snapshot.
            UncommittedEvents events = ((Aggregate<?, ?, ?>) aggregate).getUncommittedEvents();
            repository().setSnapshotTrigger(events.list()
                                                  .size());

            repository().store(aggregate);
            AggregateStateRecord record = readRecord(aggregate);
            assertTrue(record.hasSnapshot());
            assertEquals(0, repository().aggregateStorage()
                                        .readEventCountAfterLastSnapshot(aggregate.getId()));
        }

        @Test
        @DisplayName("when storing snapshot isn't needed")
        void whenStoreNotNeeded() {
            ProjectAggregate aggregate = GivenAggregate.withUncommittedEvents();

            repository().store(aggregate);
            AggregateStateRecord record = readRecord(aggregate);
            assertFalse(record.hasSnapshot());
        }

        private AggregateStateRecord readRecord(ProjectAggregate aggregate) {
            AggregateReadRequest<ProjectId> request =
                    new AggregateReadRequest<>(aggregate.getId(), DEFAULT_SNAPSHOT_TRIGGER);
            Optional<AggregateStateRecord> optional = repository().aggregateStorage()
                                                                  .read(request);
            assertTrue(optional.isPresent());
            return optional.get();
        }
    }

    @Nested
    @DisplayName("have snapshot trigger")
    class HaveSnapshotTrigger {

        @Test
        @DisplayName("set to default value initially")
        void setToDefault() {
            assertEquals(DEFAULT_SNAPSHOT_TRIGGER, repository().getSnapshotTrigger());
        }

        @Test
        @DisplayName("set to specified value")
        void setToSpecifiedValue() {
            int newSnapshotTrigger = 1000;

            repository().setSnapshotTrigger(newSnapshotTrigger);

            assertEquals(newSnapshotTrigger, repository().getSnapshotTrigger());
        }

        @Test
        @DisplayName("never set to negative value")
        void notSetToNegative() {
            assertThrows(IllegalArgumentException.class, () -> repository().setSnapshotTrigger(-1));
        }

        @Test
        @DisplayName("never set to zero value")
        void notSetToZero() {
            assertThrows(IllegalArgumentException.class, () -> repository().setSnapshotTrigger(0));
        }
    }

    @Nested
    @DisplayName("pass snapshot trigger to AggregateReadRequest")
    class PassSnapshotTrigger {

        @SuppressWarnings({"unchecked", "CheckReturnValue" /* calling mock */})
        @Test
        @DisplayName("when it's set to default value")
        void whenItsDefault() {
            AggregateRepository<ProjectId, ProjectAggregate> repositorySpy = spy(repository());
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
        @DisplayName("when it's set to non-default value")
        void whenItsNonDefault() {
            AggregateRepository<ProjectId, ProjectAggregate> repositorySpy = spy(repository());
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
    }

    @Nested
    @DisplayName("find aggregates with status flag")
    class FindWithStatusFlag {

        @Test
        @DisplayName("`archived`")
        void archived() {
            ProjectAggregate aggregate = givenStoredAggregate();

            AggregateTransaction tx = AggregateTransaction.start(aggregate);
            aggregate.setArchived(true);
            tx.commit();
            repository().store(aggregate);

            assertTrue(repository().find(aggregate.getId())
                                   .isPresent());
        }

        @Test
        @DisplayName("`deleted`")
        void deleted() {
            ProjectAggregate aggregate = givenStoredAggregate();

            AggregateTransaction tx = AggregateTransaction.start(aggregate);
            aggregate.setDeleted(true);
            tx.commit();

            repository().store(aggregate);

            assertTrue(repository().find(aggregate.getId())
                                   .isPresent());
        }
    }

    @Test
    @DisplayName("not create new aggregates upon lookup")
    void notCreateNewAggregatesOnFind() {
        ProjectId newId = Sample.messageOfType(ProjectId.class);
        Optional<ProjectAggregate> optional = repository().find(newId);
        assertFalse(optional.isPresent());
    }

    @SuppressWarnings("CheckReturnValue") // The returned value is not used in this test.
    @Test
    @DisplayName("throw IllegalStateException if unable to load entity by id from storage index")
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
                repository().iterator(aggregate -> true);

        // This should iterate through all and fail.
        assertThrows(IllegalStateException.class, () -> Lists.newArrayList(iterator));
    }

    @Nested
    @DisplayName("allow aggregates to react")
    class AllowAggregatesReact {

        @Test
        @DisplayName("on events")
        void onEvents() {
            ReactingRepository repository = new ReactingRepository();
            boundedContext().register(repository);

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
            boundedContext().getEventBus()
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

        @Test
        @DisplayName("on rejections")
        void onRejections() {
            boundedContext().register(new RejectingRepository());
            RejectionReactingRepository repository = new RejectionReactingRepository();
            boundedContext().register(repository);

            ProjectId parentId = givenAggregateId("rejectingParent");
            ProjectId childId1 = givenAggregateId("acceptingChild-1");
            ProjectId childId2 = givenAggregateId("acceptingChild-2");
            ProjectId childId3 = givenAggregateId("acceptingChild-3");

            StreamObserver<Ack> observer = StreamObservers.noOpObserver();
            CommandBus commandBus = boundedContext().getCommandBus();

            // Create the parent project.
            ImmutableSet<ProjectId> childProjects = ImmutableSet.of(childId1, childId2, childId3);
            Command createParent = requestFactory().createCommand(
                    AggCreateProjectWithChildren.newBuilder()
                                                .setProjectId(parentId)
                                                .addAllChildProjectId(childProjects)
                                                .build()
            );
            commandBus.post(createParent, observer);

            // Fire a command which would cause rejection.
            Command startProject = requestFactory().createCommand(
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
    }

    @Nested
    @ExtendWith(ShardingReset.class)
    @MuteLogging
    @DisplayName("post produced events to EventBus")
    class PostEventsToBus {

        @Test
        @DisplayName("after command dispatching")
        void afterCommand() {
            ProjectId id = givenAggregateId(Identifier.newUuid());
            AggCreateProject create = AggCreateProject
                    .newBuilder()
                    .setProjectId(id)
                    .setName("Command Dispatching")
                    .build();
            Task task = Task
                    .newBuilder()
                    .setTitle("Dummy Task")
                    .setDescription("Dummy Task Description")
                    .build();
            AggAddTask addTask = AggAddTask
                    .newBuilder()
                    .setProjectId(id)
                    .setTask(task)
                    .build();
            AggStartProject start = AggStartProject
                    .newBuilder()
                    .setProjectId(id)
                    .build();
            BlackBoxBoundedContext.singletenant()
                                  .with(repository())
                                  .receivesCommands(create, addTask, start)
                                  .assertThat(emittedEventsHadVersions(1, 2, 3))
                                  .close();
        }

        @Test
        @DisplayName("after event dispatching")
        void afterEvent() {
            ProjectId id = givenAggregateId(Identifier.newUuid());
            AggCreateProject create = AggCreateProject
                    .newBuilder()
                    .setProjectId(id)
                    .setName("Command Dispatching")
                    .build();
            AggStartProject start = AggStartProject
                    .newBuilder()
                    .setProjectId(id)
                    .build();
            ProjectId parent = givenAggregateId(Identifier.newUuid());
            AggProjectArchived archived = AggProjectArchived
                    .newBuilder()
                    .setProjectId(parent)
                    .addChildProjectId(id)
                    .build();
            BlackBoxBoundedContext.singletenant()
                                  .with(repository())
                                  .receivesCommands(create, start)
                                  .receivesEvent(archived)
                                  .assertThat(emittedEventsHadVersions(
                                          1, 2, // Product creation
                                          0,    // Manually assembled event (`archived`)
                                          3     // Event produced in response to `archived` event
                                  ))
                                  .close();
        }

        @Test
        @DisplayName("through the repository EventFilter")
        void throughEventFilter() {
            ProjectId id = givenAggregateId(Identifier.newUuid());
            AggCreateProject create = AggCreateProject
                    .newBuilder()
                    .setProjectId(id)
                    .setName("Test Project")
                    .build();
            AggStartProject start = AggStartProject
                    .newBuilder()
                    .setProjectId(id)
                    .build();
            ProjectId parent = givenAggregateId(Identifier.newUuid());
            AggProjectArchived archived = AggProjectArchived
                    .newBuilder()
                    .setProjectId(parent)
                    .addChildProjectId(id)
                    .build();
            BlackBoxBoundedContext.singletenant()
                                  .with(new EventDiscardingAggregateRepository())
                                  .receivesCommands(create, start)
                                  .receivesEvent(archived)
                                  .assertThat(emittedEvents(AggProjectArchived.class))
                                  .close();
        }
    }

    @Test
    @DisplayName("route events to aggregates")
    void routeEventsToAggregates() {
        ProjectAggregate parent = givenStoredAggregate();
        ProjectAggregate child = givenStoredAggregate();

        assertTrue(repository().find(parent.getId())
                               .isPresent());
        assertTrue(repository().find(child.getId())
                               .isPresent());

        TestEventFactory factory = TestEventFactory.newInstance(getClass());
        AggProjectArchived msg = AggProjectArchived.newBuilder()
                                                   .setProjectId(parent.getId())
                                                   .addChildProjectId(child.getId())
                                                   .build();
        Event event = factory.createEvent(msg);

        boundedContext().getEventBus()
                        .post(event);

        // Check that the child aggregate was archived.
        Optional<ProjectAggregate> childAfterArchive = repository().find(child.getId());
        assertTrue(childAfterArchive.isPresent());
        assertTrue(childAfterArchive.get()
                                    .isArchived());
        // The parent should not be archived since the dispatch route uses only
        // child aggregates from the `ProjectArchived` event.
        Optional<ProjectAggregate> parentAfterArchive = repository().find(parent.getId());
        assertTrue(parentAfterArchive.isPresent());
        assertFalse(parentAfterArchive.get()
                                      .isArchived());
    }

    @Test
    @DisplayName("do nothing when event reaction fails")
    @MuteLogging
    void doNothingWhenEventReactionFails() {
        FailingAggregateRepository repository = new FailingAggregateRepository();
        boundedContext().register(repository);

        TestEventFactory factory = TestEventFactory.newInstance(getClass());

        // Passing negative float value should cause an exception.
        EventEnvelope envelope =
                EventEnvelope.of(factory.createEvent(FloatEncountered.newBuilder()
                                                                     .setNumber(-412.0f)
                                                                     .build()));
        boundedContext().getEventBus()
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
        boundedContext().register(repository);

        // Passing negative long value to `FailingAggregate` should cause a rejection.
        CommandEnvelope ce = CommandEnvelope.of(
                requestFactory().createCommand(RejectNegativeLong.newBuilder()
                                                                 .setNumber(-100_000_000L)
                                                                 .build()));
        boundedContext().getCommandBus()
                        .post(ce.getCommand(), StreamObservers.noOpObserver());

        assertFalse(repository.isErrorLogged());
    }

    @Test
    @DisplayName("not allow anemic aggregates")
    void notAllowAnemicAggregates() {
        assertThrows(IllegalStateException.class,
                     () -> boundedContext().register(new AnemicAggregateRepository()));
    }
}
