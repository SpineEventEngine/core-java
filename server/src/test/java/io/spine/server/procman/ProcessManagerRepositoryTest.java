/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.procman;

import com.google.common.truth.Truth8;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.MessageId;
import io.spine.core.Origin;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.entity.EventFilter;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.RecordBasedRepositoryTest;
import io.spine.server.entity.given.Given;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyDeleted;
import io.spine.server.procman.given.delivery.GivenMessage;
import io.spine.server.procman.given.repo.EventDiscardingProcManRepository;
import io.spine.server.procman.given.repo.ProjectCompletion;
import io.spine.server.procman.given.repo.RememberingSubscriber;
import io.spine.server.procman.given.repo.SensoryDeprivedPmRepository;
import io.spine.server.procman.given.repo.TestProcessManager;
import io.spine.server.procman.given.repo.TestProcessManagerRepository;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.given.GivenEvent;
import io.spine.system.server.CannotDispatchDuplicateCommand;
import io.spine.system.server.CannotDispatchDuplicateEvent;
import io.spine.system.server.DiagnosticMonitor;
import io.spine.system.server.RoutingFailed;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.test.procman.PmDontHandle;
import io.spine.test.procman.Project;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.Task;
import io.spine.test.procman.command.PmArchiveProject;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmDeleteProject;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.command.PmThrowEntityAlreadyArchived;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.procman.given.repo.GivenCommandMessage.ID;
import static io.spine.server.procman.given.repo.GivenCommandMessage.addTask;
import static io.spine.server.procman.given.repo.GivenCommandMessage.archiveProject;
import static io.spine.server.procman.given.repo.GivenCommandMessage.createProject;
import static io.spine.server.procman.given.repo.GivenCommandMessage.deleteProject;
import static io.spine.server.procman.given.repo.GivenCommandMessage.doNothing;
import static io.spine.server.procman.given.repo.GivenCommandMessage.projectCreated;
import static io.spine.server.procman.given.repo.GivenCommandMessage.projectStarted;
import static io.spine.server.procman.given.repo.GivenCommandMessage.startProject;
import static io.spine.server.procman.given.repo.GivenCommandMessage.taskAdded;
import static io.spine.testing.TestValues.randomString;
import static io.spine.testing.server.Assertions.assertCommandClasses;
import static io.spine.testing.server.Assertions.assertEventClasses;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProcessManagerRepository should")
class ProcessManagerRepositoryTest
        extends RecordBasedRepositoryTest<TestProcessManager, ProjectId, Project> {

    private final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(getClass(), TenantId.newBuilder()
                                                            .setValue(newUuid())
                                                            .build());
    private BoundedContext context;

    @Override
    protected RecordBasedRepository<ProjectId, TestProcessManager, Project> createRepository() {
        return new TestProcessManagerRepository();
    }

    @Override
    protected TestProcessManager createEntity(ProjectId id) {
        Project state = Project
                .newBuilder()
                .setId(id)
                .build();
        TestProcessManager result =
                Given.processManagerOfClass(TestProcessManager.class)
                     .withId(id)
                     .withState(state)
                     .build();
        return result;
    }

    @Override
    protected List<TestProcessManager> createEntities(int count) {
        return createEntitiesWithState(count, id -> Project.getDefaultInstance());
    }

    @Override
    protected List<TestProcessManager> createNamed(int count, Supplier<String> nameSupplier) {
        return createEntitiesWithState(count, id -> Project.newBuilder()
                                                           .setId(id)
                                                           .setName(nameSupplier.get())
                                                           .build());
    }

    private List<TestProcessManager>
    createEntitiesWithState(int count, Function<ProjectId, Project> initialStateSupplier) {
        List<TestProcessManager> procmans = newArrayList();

        for (int i = 0; i < count; i++) {
            ProjectId id = createId(i);
            TestProcessManager procman = new TestProcessManager(id);
            setEntityState(procman, initialStateSupplier.apply(id));

            TestProcessManager pm =
                    Given.processManagerOfClass(TestProcessManager.class)
                         .withId(id)
                         .withState(Project.newBuilder()
                                           .setId(id)
                                           .setName("Test pm name" + randomString())
                                           .build())
                         .build();
            procmans.add(pm);
        }
        return procmans;
    }

    @Override
    protected List<TestProcessManager> orderedByName(List<TestProcessManager> entities) {
        return entities.stream()
                       .sorted(comparing(ProcessManagerRepositoryTest::entityName))
                       .collect(toList());
    }

    private static String entityName(TestProcessManager entity) {
        return entity.state()
                     .getName();
    }

    @Override
    protected ProjectId createId(int value) {
        return ProjectId.newBuilder()
                        .setId(format("procman-number-%s", value))
                        .build();
    }

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
        setCurrentTenant(requestFactory.tenantId());
        context = BoundedContextBuilder
                .assumingTests(true)
                .build();
        context.register(repository());
        TestProcessManager.clearMessageDeliveryHistory();
        repository().clearConfigureCalledFlag();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        context.close();
        super.tearDown();
    }

    @Override
    protected TestProcessManagerRepository repository() {
        return (TestProcessManagerRepository) super.repository();
    }

    @SuppressWarnings("CheckReturnValue")
    // We can ignore the ID of the PM handling the command in the calling tests.
    private void dispatchCommand(Command command) {
        repository().dispatchCommand(CommandEnvelope.of(command));
    }

    private void testDispatchCommand(CommandMessage cmdMsg) {
        Command cmd = requestFactory.command()
                                    .create(cmdMsg);
        dispatchCommand(cmd);
        assertTrue(TestProcessManager.processed(cmdMsg));
    }

    private void testDispatchEvent(EventMessage eventMessage) {
        Event event = GivenEvent.withMessage(eventMessage);
        dispatchEvent(event);
        assertTrue(TestProcessManager.processed(eventMessage));
    }

    @SuppressWarnings("CheckReturnValue") // can ignore IDs of target PMs in this test.
    private void dispatchEvent(Event event) {
        CommandId randomCommandId = CommandId.generate();
        MessageId originId = MessageId
                .newBuilder()
                .setId(pack(randomCommandId))
                .setTypeUrl("example.org/example.test.InjectEvent")
                .buildPartial();
        ActorContext actor = requestFactory.newActorContext();
        Origin origin = Origin
                .newBuilder()
                .setActorContext(actor)
                .setMessage(originId)
                .buildPartial();
        EventContext eventContextWithTenantId =
                GivenEvent.context()
                          .toBuilder()
                          .setPastMessage(origin)
                          .buildPartial();
        Event eventWithTenant = event.toBuilder()
                                     .setContext(eventContextWithTenantId)
                                     .vBuild();
        repository().dispatch(EventEnvelope.of(eventWithTenant));
    }

    @Test
    @DisplayName("allow customizing command routing")
    void setupOfCommandRouting() {
        ProjectCompletion.Repository repo = new ProjectCompletion.Repository();
        context.register(repo);
        assertTrue(repo.callbackCalled());
    }

    @Nested
    @DisplayName("dispatch")
    class Dispatch {

        @Test
        @DisplayName("command")
        void command() {
            testDispatchCommand(addTask());
        }

        @Test
        @DisplayName("event")
        void event() {
            testDispatchEvent(projectCreated());
        }
    }

    @Test
    @DisplayName("dispatch command and post events")
    void dispatchCommandAndPostEvents() {
        RememberingSubscriber subscriber = new RememberingSubscriber();
        context.registerEventDispatcher(subscriber);

        testDispatchCommand(addTask());

        PmTaskAdded message = subscriber.getRemembered();
        assertNotNull(message);
        assertThat(message.getProjectId())
             .isEqualTo(ID);
    }

    @Nested
    @MuteLogging
    @DisplayName("not dispatch duplicate")
    class AvoidDuplicates {

        @Test
        @DisplayName("events")
        void events() {
            DiagnosticMonitor monitor = new DiagnosticMonitor();
            context.registerEventDispatcher(monitor);
            Event event = GivenMessage.projectStarted();

            dispatchEvent(event);
            assertTrue(TestProcessManager.processed(event.enclosedMessage()));
            dispatchEvent(event);

            List<CannotDispatchDuplicateEvent> duplicateEventEvents = monitor.duplicateEventEvents();
            assertThat(duplicateEventEvents).hasSize(1);
            CannotDispatchDuplicateEvent systemEvent = duplicateEventEvents.get(0);
            assertThat(systemEvent.getDuplicateEvent())
                    .comparingExpectedFieldsOnly()
                    .isEqualTo(event.messageId());
            PmProjectStarted eventMessage = (PmProjectStarted) event.enclosedMessage();
            assertThat(unpack(systemEvent.getEntity().getId()))
                      .comparingExpectedFieldsOnly()
                      .isEqualTo(eventMessage.getProjectId());
        }

        @Test
        @DisplayName("commands")
        void commands() {
            DiagnosticMonitor monitor = new DiagnosticMonitor();
            context.registerEventDispatcher(monitor);
            Command command = GivenMessage.createProject();

            dispatchCommand(command);
            assertTrue(TestProcessManager.processed(command.enclosedMessage()));
            dispatchCommand(command);

            List<CannotDispatchDuplicateCommand> duplicateCommandEvents =
                    monitor.duplicateCommandEvents();
            assertThat(duplicateCommandEvents).hasSize(1);
            CannotDispatchDuplicateCommand event = duplicateCommandEvents.get(0);
            assertThat(event.getDuplicateCommand())
                    .isEqualTo(command.messageId());
            PmCreateProject commandMessage = (PmCreateProject) command.enclosedMessage();
            assertThat(unpack(event.getEntity().getId()))
                    .isEqualTo(commandMessage.getProjectId());
        }
    }

    @Nested
    @DisplayName("dispatch several")
    class DispatchSeveral {

        @Test
        @DisplayName("commands")
        void commands() {
            testDispatchCommand(createProject());
            testDispatchCommand(addTask());
            testDispatchCommand(startProject());
        }

        @Test
        @DisplayName("events")
        void events() {
            testDispatchEvent(projectCreated());
            testDispatchEvent(taskAdded());
            testDispatchEvent(projectStarted());
        }
    }

    @Nested
    @DisplayName("given archived process manager, dispatch")
    class DispatchToArchivedProcman {

        @Test
        @DisplayName("command")
        void command() {
            PmArchiveProject archiveProject = archiveProject();
            testDispatchCommand(archiveProject);
            ProjectId projectId = archiveProject.getProjectId();
            TestProcessManager processManager = repository().findOrCreate(projectId);
            assertTrue(processManager.isArchived());

            // Dispatch a command to the deleted process manager.
            testDispatchCommand(addTask());
            processManager = repository().findOrCreate(projectId);
            List<Task> addedTasks = processManager.state()
                                                  .getTaskList();
            assertFalse(addedTasks.isEmpty());

            // Check that the process manager was not re-created before dispatching.
            assertTrue(processManager.isArchived());
        }

        @Test
        @DisplayName("event")
        void event() {
            PmArchiveProject archiveProject = archiveProject();
            testDispatchCommand(archiveProject);
            ProjectId projectId = archiveProject.getProjectId();
            TestProcessManager processManager = repository().findOrCreate(projectId);
            assertTrue(processManager.isArchived());

            // Dispatch an event to the archived process manager.
            testDispatchEvent(taskAdded());
            processManager = repository().findOrCreate(projectId);
            List<Task> addedTasks = processManager.state()
                                                  .getTaskList();
            assertFalse(addedTasks.isEmpty());

            // Check that the process manager was not re-created before dispatching.
            assertTrue(processManager.isArchived());
        }
    }

    @Nested
    @DisplayName("given deleted process manager, dispatch")
    class DispatchToDeletedProcman {

        @Test
        @DisplayName("command")
        void command() {
            PmDeleteProject deleteProject = deleteProject();
            testDispatchCommand(deleteProject);
            ProjectId projectId = deleteProject.getProjectId();
            TestProcessManager processManager = repository().findOrCreate(projectId);
            assertTrue(processManager.isDeleted());

            // Dispatch a command to the archived process manager.
            testDispatchCommand(addTask());
            processManager = repository().findOrCreate(projectId);
            List<Task> addedTasks = processManager.state()
                                                  .getTaskList();
            assertFalse(addedTasks.isEmpty());

            // Check that the process manager was not re-created before dispatching.
            assertTrue(processManager.isDeleted());
        }

        @Test
        @DisplayName("event")
        void event() {
            PmDeleteProject deleteProject = deleteProject();
            testDispatchCommand(deleteProject);
            ProjectId projectId = deleteProject.getProjectId();
            TestProcessManager processManager = repository().findOrCreate(projectId);
            assertTrue(processManager.isDeleted());

            // Dispatch an event to the deleted process manager.
            testDispatchEvent(taskAdded());
            processManager = repository().findOrCreate(projectId);
            List<Task> addedTasks = processManager.state()
                                                  .getTaskList();
            assertFalse(addedTasks.isEmpty());

            // Check that the process manager was not re-created before dispatching.
            assertTrue(processManager.isDeleted());
        }
    }

    @Test
    @DisplayName("allow process manager have unmodified state after command handling")
    void allowUnmodifiedStateAfterCommand() {
        testDispatchCommand(doNothing());
    }

    @Test
    @DisplayName("produce RoutingFailed when dispatching unknown command")
    @MuteLogging
    void throwOnUnknownCommand() {
        Command unknownCommand = requestFactory.createCommand(PmDontHandle.getDefaultInstance());
        CommandEnvelope command = CommandEnvelope.of(unknownCommand);
        ProcessManagerRepository<ProjectId, ?, ?> repo = repository();
        DiagnosticMonitor monitor = new DiagnosticMonitor();
        context.registerEventDispatcher(monitor);
        repo.dispatchCommand(command);
        List<RoutingFailed> failures = monitor.routingFailures();
        assertThat(failures).hasSize(1);
        RoutingFailed failure = failures.get(0);
        assertThat(failure.getEntityType().getJavaClassName())
                .isEqualTo(repo.entityClass().getCanonicalName());
        assertThat(failure.getError().getType())
                .isEqualTo(IllegalStateException.class.getName());
    }

    @Nested
    @DisplayName("return classes of")
    class ReturnClasses {

        @Test
        @DisplayName("commands")
        void command() {
            Set<CommandClass> commandClasses = repository().commandClasses();

            assertCommandClasses(
                    commandClasses,
                    PmCreateProject.class, PmCreateProject.class, PmStartProject.class
            );
        }

        @Test
        @DisplayName("events")
        void event() {
            Set<EventClass> eventClasses = repository().messageClasses();

            assertEventClasses(
                    eventClasses,
                    PmProjectCreated.class, PmTaskAdded.class, PmProjectStarted.class,
                    EntityAlreadyArchived.class, EntityAlreadyDeleted.class
            );
        }
    }

    @Test
    @DisplayName("post command rejections")
    void postCommandRejections() {
        ProjectId id = ProjectId.newBuilder()
                                .setId(newUuid())
                                .build();
        PmThrowEntityAlreadyArchived commandMsg =
                PmThrowEntityAlreadyArchived.newBuilder()
                                            .setProjectId(id)
                                            .build();
        Command command = requestFactory.createCommand(commandMsg);
        dispatchCommand(command);
        EntityAlreadyArchived expected = EntityAlreadyArchived.newBuilder()
                                                              .setEntityId(pack(id))
                                                              .build();
        assertTrue(TestProcessManager.processed(expected));
    }

    @Test
    @DisplayName("check that its `ProcessManager` class is subscribed to at least one message")
    void notRegisterIfSubscribedToNothing() {
        SensoryDeprivedPmRepository repo = new SensoryDeprivedPmRepository();
        BoundedContext context = BoundedContextBuilder
                .assumingTests()
                .build();
        assertThrows(IllegalStateException.class, () -> repo.registerWith(context));
    }

    @Test
    @DisplayName("provide `EventFilter` which discards `EntityStateChanged` events")
    void discardEntityStateChangedEvents() {
        EventFilter filter = repository().eventFilter();
        ProjectId projectId = ProjectId
                .newBuilder()
                .setId(newUuid())
                .build();
        EventMessage arbitraryEvent = PmTaskAdded
                .newBuilder()
                .setProjectId(projectId)
                .build();
        Truth8.assertThat(filter.filter(arbitraryEvent))
              .isPresent();

        Any newState = pack(currentTime());
        Any oldState = pack(Timestamp.getDefaultInstance());
        MessageId entityId = MessageId
                .newBuilder()
                .setTypeUrl(TypeUrl.ofEnclosed(newState)
                                   .value())
                .setId(pack(projectId))
                .vBuild();
        EventMessage discardedEvent = EntityStateChanged
                .newBuilder()
                .setEntity(entityId)
                .setOldState(oldState)
                .setNewState(newState)
                .build();
        Truth8.assertThat(filter.filter(discardedEvent))
              .isEmpty();
    }

    @Test
    @DisplayName("post all domain events through an `EventFilter`")
    void postEventsThroughFilter() {
        ProjectId projectId = ProjectId
                .newBuilder()
                .setId(newUuid())
                .build();
        PmCreateProject command = PmCreateProject
                .newBuilder()
                .setProjectId(projectId)
                .build();
        BlackBoxBoundedContext
                .singleTenant()
                .with(new EventDiscardingProcManRepository())
                .receivesCommand(command)
                .assertEvents()
                .isEmpty();
    }

    @DisplayName("call `configure()` callback when a `ProcessManager` instance is created")
    @Nested
    class CallingConfigure {

        @Test
        void whenFinding() {
            TestProcessManagerRepository repository = repository();
            assertFalse(repository.configureCalled());

            TestProcessManager created = repository.create(ID);
            repository.store(created);

            repository.findOrCreate(ID);
            assertTrue(repository.configureCalled());
        }

        @Test
        void whenCreating() {
            TestProcessManagerRepository repository = repository();
            assertFalse(repository.configureCalled());

            repository.create(ID);
            assertTrue(repository.configureCalled());
        }
    }
}
