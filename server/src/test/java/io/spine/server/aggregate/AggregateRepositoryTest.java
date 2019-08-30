/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.aggregate.given.klasse.EngineAggregate;
import io.spine.server.aggregate.given.repo.AnemicAggregateRepository;
import io.spine.server.aggregate.given.repo.EventDiscardingAggregateRepository;
import io.spine.server.aggregate.given.repo.FailingAggregateRepository;
import io.spine.server.aggregate.given.repo.GivenAggregate;
import io.spine.server.aggregate.given.repo.ProjectAggregate;
import io.spine.server.aggregate.given.repo.ProjectAggregateRepository;
import io.spine.server.aggregate.given.repo.ReactingAggregate;
import io.spine.server.aggregate.given.repo.ReactingRepository;
import io.spine.server.aggregate.given.repo.RejectingRepository;
import io.spine.server.aggregate.given.repo.RejectionReactingAggregate;
import io.spine.server.aggregate.given.repo.RejectionReactingRepository;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.Repository;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.DiagnosticMonitor;
import io.spine.system.server.HandlerFailedUnexpectedly;
import io.spine.system.server.Mirror;
import io.spine.system.server.MirrorRepository;
import io.spine.test.aggregate.Project;
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
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;
import io.spine.testing.server.model.ModelTests;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.aggregate.AggregateRepository.DEFAULT_SNAPSHOT_TRIGGER;
import static io.spine.server.aggregate.given.repo.AggregateRepositoryTestEnv.boundedContext;
import static io.spine.server.aggregate.given.repo.AggregateRepositoryTestEnv.givenAggregateId;
import static io.spine.server.aggregate.given.repo.AggregateRepositoryTestEnv.givenStoredAggregate;
import static io.spine.server.aggregate.given.repo.AggregateRepositoryTestEnv.givenStoredAggregateWithId;
import static io.spine.server.aggregate.given.repo.AggregateRepositoryTestEnv.repository;
import static io.spine.server.aggregate.given.repo.AggregateRepositoryTestEnv.requestFactory;
import static io.spine.server.aggregate.given.repo.AggregateRepositoryTestEnv.resetBoundedContext;
import static io.spine.server.aggregate.given.repo.AggregateRepositoryTestEnv.resetRepository;
import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static io.spine.testing.core.given.GivenTenantId.generate;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
            assertEquals(ProjectAggregate.class, repository().entityClass());
        }

        @Test
        @DisplayName("command classes handled by aggregate")
        void aggregateCommandClasses() {
            Set<CommandClass> aggregateCommands =
                    asAggregateClass(ProjectAggregate.class)
                            .commands();
            Set<CommandClass> exposedByRepository = repository().messageClasses();

            assertTrue(exposedByRepository.containsAll(aggregateCommands));
        }

        @Test
        @DisplayName("event classes on which aggregate reacts")
        void aggregateEventClasses() {
            Set<EventClass> eventClasses = repository().domesticEvents();
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

            assertEquals(expected.id(), actual.id());
            assertEquals(expected.state(), actual.state());
        }

        @Test
        @DisplayName("without using snapshot")
        void notUsingSnapshot() {
            ProjectId id = Sample.messageOfType(ProjectId.class);
            ProjectAggregate expected = GivenAggregate.withUncommittedEvents(id);

            repository().store(expected);
            ProjectAggregate actual = assertFound(id);

            assertTrue(isNotDefault(actual.state()));
            assertEquals(expected.id(), actual.id());
            assertEquals(expected.state(), actual.state());
        }

        private ProjectAggregate assertFound(ProjectId id) {
            Optional<ProjectAggregate> optional = repository().find(id);
            assertTrue(optional.isPresent());
            return optional.get();
        }
    }

    @Nested
    @DisplayName("manage snapshots properly")
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
            AggregateHistory record = readRecord(aggregate);
            assertTrue(record.hasSnapshot());
            assertEquals(0, record.getEventCount());
        }

        @Test
        @DisplayName("when storing snapshot isn't needed")
        void whenStoreNotNeeded() {
            ProjectAggregate aggregate = GivenAggregate.withUncommittedEvents();

            repository().store(aggregate);
            AggregateHistory record = readRecord(aggregate);
            assertFalse(record.hasSnapshot());
        }

        private AggregateHistory readRecord(ProjectAggregate aggregate) {
            AggregateReadRequest<ProjectId> request =
                    new AggregateReadRequest<>(aggregate.id(), DEFAULT_SNAPSHOT_TRIGGER);
            Optional<AggregateHistory> optional = repository().aggregateStorage()
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
            assertEquals(DEFAULT_SNAPSHOT_TRIGGER, repository().snapshotTrigger());
        }

        @Test
        @DisplayName("set to specified value")
        void setToSpecifiedValue() {
            int newSnapshotTrigger = 1000;

            repository().setSnapshotTrigger(newSnapshotTrigger);

            assertEquals(newSnapshotTrigger, repository().snapshotTrigger());
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
    @DisplayName("pass snapshot trigger + 1 to AggregateReadRequest")
    class PassSnapshotTrigger {

        @SuppressWarnings({"unchecked", "CheckReturnValue" /* calling mock */})
        @Test
        @DisplayName("when it's set to default value")
        void whenItsDefault() {
            ProjectAggregateRepository repository = repository();
            AggregateStorage<ProjectId> storageSpy = spy(repository.aggregateStorage());
            repository.injectStorage(storageSpy);

            ProjectId id = Sample.messageOfType(ProjectId.class);
            loadOrCreate(repository, id);

            ArgumentCaptor<AggregateReadRequest<ProjectId>> requestCaptor =
                    ArgumentCaptor.forClass(AggregateReadRequest.class);
            verify(storageSpy).read(requestCaptor.capture());

            AggregateReadRequest<ProjectId> passedRequest = requestCaptor.getValue();
            assertEquals(id, passedRequest.recordId());
            assertEquals(repository.snapshotTrigger() + 1, passedRequest.batchSize());
        }

        @SuppressWarnings({"unchecked", "CheckReturnValue" /* calling mock */})
        @Test
        @DisplayName("when it's set to non-default value")
        void whenItsNonDefault() {
            ProjectAggregateRepository repository = repository();
            AggregateStorage<ProjectId> storageSpy = spy(repository.aggregateStorage());
            repository.injectStorage(storageSpy);

            int nonDefaultSnapshotTrigger = DEFAULT_SNAPSHOT_TRIGGER * 2;
            repository.setSnapshotTrigger(nonDefaultSnapshotTrigger);
            ProjectId id = Sample.messageOfType(ProjectId.class);
            loadOrCreate(repository, id);

            ArgumentCaptor<AggregateReadRequest<ProjectId>> requestCaptor =
                    ArgumentCaptor.forClass(AggregateReadRequest.class);
            verify(storageSpy).read(requestCaptor.capture());

            AggregateReadRequest<ProjectId> passedRequest = requestCaptor.getValue();
            assertEquals(id, passedRequest.recordId());
            assertEquals(nonDefaultSnapshotTrigger + 1, passedRequest.batchSize());
        }
    }

    @Test
    @DisplayName("pass snapshot trigger + 1 to `AggregateReadRequest`")
    void useSnapshotTriggerForRead() {
        ProjectAggregateRepository repository = repository();
        AggregateStorage<ProjectId> storageSpy = spy(repository.aggregateStorage());
        repository.injectStorage(storageSpy);
        int snapshotTrigger = repository.snapshotTrigger();

        ProjectId id = Sample.messageOfType(ProjectId.class);
        loadOrCreate(repository, id);

        @SuppressWarnings("unchecked") // Reflective mock creation.
                ArgumentCaptor<AggregateReadRequest<ProjectId>> requestCaptor =
                ArgumentCaptor.forClass(AggregateReadRequest.class);
        verify(storageSpy).read(requestCaptor.capture());

        AggregateReadRequest<ProjectId> passedRequest = requestCaptor.getValue();
        assertEquals(id, passedRequest.recordId());
        assertEquals(snapshotTrigger + 1, passedRequest.batchSize());
    }

    private static void loadOrCreate(AggregateRepository<ProjectId, ProjectAggregate> repository,
                                     ProjectId id) {
        repository.loadOrCreate(id);
    }

    @Nested
    @DisplayName("find aggregates with status flag")
    class FindWithStatusFlag {

        @Test
        @DisplayName("`archived`")
        void archived() {
            ProjectAggregate aggregate = givenStoredAggregate();

            AggregateTransaction tx = AggregateTransaction.start(aggregate);
            aggregate.archive();
            tx.commit();
            repository().store(aggregate);

            assertTrue(repository().find(aggregate.id())
                                   .isPresent());
        }

        @Test
        @DisplayName("`deleted`")
        void deleted() {
            ProjectAggregate aggregate = givenStoredAggregate();

            AggregateTransaction tx = AggregateTransaction.start(aggregate);
            aggregate.archive();
            tx.commit();

            repository().store(aggregate);

            assertTrue(repository().find(aggregate.id())
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
        TenantAwareOperation op = new TenantAwareOperation(generate()) {
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
            AggProjectArchived msg = AggProjectArchived
                    .newBuilder()
                    .setProjectId(parentId)
                    .addChildProjectId(childId)
                    .vBuild();
            Event event = factory.createEvent(msg);

            // Posting this event should archive the aggregate.
            boundedContext().eventBus()
                            .post(event);

            // Check that the aggregate marked itself as `archived`, and therefore became invisible
            // to regular queries.
            Optional<ReactingAggregate> optional = repository.find(childId);

            // The aggregate was created because of dispatching.
            assertTrue(optional.isPresent());

            // The proper method was called, which we check by the state the aggregate got.
            assertEquals(ReactingAggregate.PROJECT_ARCHIVED,
                         optional.get()
                                 .state()
                                 .getValue());
        }

        @Test
        @DisplayName("on rejections")
        void onRejections() {
            BoundedContext context = boundedContext();
            context.register(new RejectingRepository());
            RejectionReactingRepository repository = new RejectionReactingRepository();
            context.register(repository);

            ProjectId parentId = givenAggregateId("rejectingParent");
            ProjectId childId1 = givenAggregateId("acceptingChild-1");
            ProjectId childId2 = givenAggregateId("acceptingChild-2");
            ProjectId childId3 = givenAggregateId("acceptingChild-3");

            StreamObserver<Ack> observer = noOpObserver();
            CommandBus commandBus = context.commandBus();

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

                assertThat(optional).isPresent();

                // Check that all the aggregates:
                // 1. got Rejections.AggCannotStartArchivedProject;
                // 2. produced the state the event;
                // 3. applied the event.
                String value = optional.get()
                                       .state()
                                       .getValue();
                assertThat(value)
                        .isEqualTo(RejectionReactingAggregate.PARENT_ARCHIVED);
            }
        }
    }

    @Nested
    @MuteLogging
    @DisplayName("post produced events to EventBus")
    class PostEventsToBus {

        private BlackBoxBoundedContext<?> context;

        /**
         * Create a fresh instance of the repository since this nested class uses
         * {@code BlackBoxBoundedContext}. We cannot use the instance of the repository created by
         * {@link AggregateRepositoryTest#setUp()} because this method registers it with another
         * {@code BoundedContext}.
         */
        @BeforeEach
        void createAnotherRepository() {
            resetRepository();
            AggregateRepository<?, ?> repository = repository();
            context = BlackBoxBoundedContext
                    .singleTenant()
                    .with(repository);
        }

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
            context.receivesCommands(create, addTask, start);
            assertEventVersions(1, 2, 3);
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
            context.receivesCommands(create, start)
                   .receivesEvent(archived);
            assertEventVersions(
                    1, 2, // Results of commands.
                    3  // The result of the `archived` event.
            );
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
            SingleTenantBlackBoxContext context = BlackBoxBoundedContext
                    .singleTenant()
                    .with(new EventDiscardingAggregateRepository())
                    .receivesCommands(create, start)
                    .receivesEvent(archived);
            context.assertEvents().isEmpty();
            context.close();
        }

        private void assertEventVersions(int... expectedVersions) {
            List<Event> events = context.assertEvents().actual();
            assertThat(events).hasSize(expectedVersions.length);
            for (int i = 0; i < events.size(); i++) {
                Event event = events.get(i);
                int expectedVersion = expectedVersions[i];
                assertThat(event.context().getVersion().getNumber())
                        .isEqualTo(expectedVersion);
            }
        }
    }

    @Test
    @DisplayName("route events to aggregates")
    void routeEventsToAggregates() {
        ProjectAggregate parent = givenStoredAggregate();
        ProjectAggregate child = givenStoredAggregate();

        assertTrue(repository().find(parent.id())
                               .isPresent());
        assertTrue(repository().find(child.id())
                               .isPresent());

        TestEventFactory factory = TestEventFactory.newInstance(getClass());
        AggProjectArchived msg = AggProjectArchived.newBuilder()
                                                   .setProjectId(parent.id())
                                                   .addChildProjectId(child.id())
                                                   .build();
        Event event = factory.createEvent(msg);

        boundedContext().eventBus()
                        .post(event);

        // Check that the child aggregate was archived.
        Optional<ProjectAggregate> childAfterArchive = repository().find(child.id());
        assertTrue(childAfterArchive.isPresent());
        assertTrue(childAfterArchive.get()
                                    .isArchived());
        // The parent should not be archived since the dispatch route uses only
        // child aggregates from the `ProjectArchived` event.
        Optional<ProjectAggregate> parentAfterArchive = repository().find(parent.id());
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
        DiagnosticMonitor monitor = new DiagnosticMonitor();
        boundedContext().registerEventDispatcher(monitor);

        TestEventFactory factory = TestEventFactory.newInstance(getClass());

        // Passing negative float value should cause an exception.
        EventEnvelope envelope =
                EventEnvelope.of(factory.createEvent(FloatEncountered.newBuilder()
                                                                     .setNumber(-412.0f)
                                                                     .build()));
        boundedContext().eventBus()
                        .post(envelope.outerObject());

        List<HandlerFailedUnexpectedly> handlerFailureEvents = monitor.handlerFailureEvents();
        assertThat(handlerFailureEvents).hasSize(1);
        HandlerFailedUnexpectedly event = handlerFailureEvents.get(0);
        assertThat(event.getHandledSignal()).isEqualTo(envelope.messageId());
        assertThat(event.getEntity().getTypeUrl())
                .isEqualTo(repository.entityStateType().value());
        assertThat(event.getError().getType())
                .isEqualTo(IllegalArgumentException.class.getCanonicalName());
    }

    @Test
    @DisplayName("not pass command rejection to `onError`")
    void notPassCommandRejectionToOnError() {
        FailingAggregateRepository repository = new FailingAggregateRepository();
        boundedContext().register(repository);
        DiagnosticMonitor monitor = new DiagnosticMonitor();
        boundedContext().registerEventDispatcher(monitor);

        // Passing negative long value to `FailingAggregate` should cause a rejection.
        RejectNegativeLong rejectNegative = RejectNegativeLong
                .newBuilder()
                .setNumber(-100_000_000L)
                .vBuild();
        Command command = requestFactory().createCommand(rejectNegative);
        CommandEnvelope envelope = CommandEnvelope.of(
                command);
        boundedContext().commandBus()
                        .post(envelope.command(), noOpObserver());
        assertThat(monitor.handlerFailureEvents()).isEmpty();
    }

    @Test
    @DisplayName("not allow anemic aggregates")
    void notAllowAnemicAggregates() {
        assertThrows(IllegalStateException.class,
                     () -> boundedContext().register(new AnemicAggregateRepository()));
    }

    @Test
    @DisplayName("register self among mirrored types in `MirrorRepository`")
    void registerAsMirroredType() {
        MirrorRepository mirrorRepository = mirrorRepository(boundedContext());
        TypeUrl type = TypeUrl.of(Project.class);
        assertThat(mirrorRepository.isMirroring(type)).isTrue();
    }

    @Nested
    @DisplayName("not register self among mirrored types")
    class NotRegisterAsMirroredType {

        @Test
        @DisplayName("if the aggregate visibility is `NONE`")
        void ifVisibilityIsNone() {
            // `Engine` aggregate has default visibility, which is `NONE`.
            BoundedContext context = BoundedContextBuilder.assumingTests()
                                                          .add(EngineAggregate.class)
                                                          .build();
            MirrorRepository mirrorRepository = mirrorRepository(context);
            TypeUrl type = TypeUrl.of(Project.class);
            assertThat(mirrorRepository.isMirroring(type)).isFalse();
        }

        @Test
        @DisplayName("if aggregate querying is disabled")
        void ifAggregateQueryingDisabled() {
            BoundedContextBuilder builder = BoundedContextBuilder.assumingTests();
            builder.systemFeatures()
                   .disableAggregateQuerying();
            BoundedContext context = builder.add(ProjectAggregate.class)
                                            .build();
            BoundedContext systemContext = systemOf(context);
            assertThat(systemContext.hasEntitiesWithState(Mirror.class)).isFalse();
        }
    }

    private static MirrorRepository mirrorRepository(BoundedContext context) {
        BoundedContext systemContext = systemOf(context);
        Optional<Repository> repository = systemContext.findRepository(Mirror.class);
        assertThat(repository).isPresent();
        MirrorRepository result = (MirrorRepository) repository.get();
        return result;
    }
}
