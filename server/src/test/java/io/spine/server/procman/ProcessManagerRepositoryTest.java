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

package io.spine.server.procman;

import com.google.protobuf.Any;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.client.EntityId;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.TenantId;
import io.spine.core.given.GivenEvent;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.DuplicateCommandException;
import io.spine.server.entity.EventFilter;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.RecordBasedRepositoryTest;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyDeleted;
import io.spine.server.event.DuplicateEventException;
import io.spine.server.procman.given.delivery.GivenMessage;
import io.spine.server.procman.given.repo.EventDiscardingProcManRepository;
import io.spine.server.procman.given.repo.RememberingSubscriber;
import io.spine.server.procman.given.repo.SensoryDeprivedPmRepository;
import io.spine.server.procman.given.repo.TestProcessManager;
import io.spine.server.procman.given.repo.TestProcessManagerRepository;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.EntityStateChanged;
import io.spine.test.procman.PmDontHandle;
import io.spine.test.procman.Project;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.ProjectVBuilder;
import io.spine.test.procman.Task;
import io.spine.test.procman.command.PmArchiveProject;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmCreateProjectVBuilder;
import io.spine.test.procman.command.PmDeleteProject;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.command.PmThrowEntityAlreadyArchived;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.entity.given.Given;
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

import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.core.Commands.getMessage;
import static io.spine.core.Events.getMessage;
import static io.spine.protobuf.AnyPacker.pack;
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
import static io.spine.testing.client.blackbox.Count.count;
import static io.spine.testing.server.Assertions.assertCommandClasses;
import static io.spine.testing.server.Assertions.assertEventClasses;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvent;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProcessManagerRepository should")
class ProcessManagerRepositoryTest
        extends RecordBasedRepositoryTest<TestProcessManager, ProjectId, Project> {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass(),
                                                TenantId.newBuilder()
                                                        .setValue(newUuid())
                                                        .build());
    private BoundedContext boundedContext;

    @Override
    protected RecordBasedRepository<ProjectId, TestProcessManager, Project> createRepository() {
        TestProcessManagerRepository repo = new TestProcessManagerRepository();
        return repo;
    }

    @Override
    protected TestProcessManager createEntity(ProjectId id) {
        TestProcessManager result = Given.processManagerOfClass(TestProcessManager.class)
                                         .withId(id)
                                         .build();
        return result;
    }

    @Override
    protected List<TestProcessManager> createEntities(int count) {
        return createEntitiesWithState(count, id -> Project.getDefaultInstance());
    }

    @Override
    protected List<TestProcessManager> createNamed(int count, Supplier<String> nameSupplier) {
        return createEntitiesWithState(count, id -> ProjectVBuilder.newBuilder()
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
        return entity.getState()
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
        setCurrentTenant(requestFactory.getTenantId());
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();
        boundedContext.register(repository());
        TestProcessManager.clearMessageDeliveryHistory();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        boundedContext.close();
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
    private void dispatchEvent(Event origin) {
        // EventContext should have CommandContext with appropriate TenantId to avoid usage
        // of different storages during command and event dispatching.
        CommandContext commandContext = requestFactory.createCommandContext();
        EventContext eventContextWithTenantId =
                GivenEvent.context()
                          .toBuilder()
                          .setCommandContext(commandContext)
                          .build();
        Event event = origin.toBuilder()
                            .setContext(eventContextWithTenantId)
                            .build();
        repository().dispatch(EventEnvelope.of(event));
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
        boundedContext.registerEventDispatcher(subscriber);

        testDispatchCommand(addTask());

        PmTaskAdded message = subscriber.getRemembered();
        assertNotNull(message);
        assertEquals(ID, message.getProjectId());
    }

    @Nested
    @MuteLogging
    @DisplayName("not dispatch duplicate")
    class AvoidDuplicates {

        @Test
        @DisplayName("events")
        void events() {
            Event event = GivenMessage.projectStarted();

            dispatchEvent(event);
            assertTrue(TestProcessManager.processed(getMessage(event)));

            dispatchEvent(event);
            RuntimeException exception = repository().getLatestException();
            assertNotNull(exception);
            assertThat(exception, instanceOf(DuplicateEventException.class));
        }

        @Test
        @DisplayName("commands")
        void commands() {
            Command command = GivenMessage.createProject();

            dispatchCommand(command);
            assertTrue(TestProcessManager.processed(getMessage(command)));

            dispatchCommand(command);
            RuntimeException exception = repository().getLatestException();
            assertNotNull(exception);
            assertThat(exception, instanceOf(DuplicateCommandException.class));
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
            List<Task> addedTasks = processManager.getState()
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
            List<Task> addedTasks = processManager.getState()
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
            List<Task> addedTasks = processManager.getState()
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
            List<Task> addedTasks = processManager.getState()
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
    @DisplayName("throw ISE when dispatching unknown command")
    @MuteLogging
    void throwOnUnknownCommand() {
        Command unknownCommand = requestFactory.createCommand(PmDontHandle.getDefaultInstance());
        CommandEnvelope request = CommandEnvelope.of(unknownCommand);
        ProjectId id = createId(42);
        ProcessManagerRepository<ProjectId, ?, ?> repo = repository();
        Throwable exception = assertThrows(RuntimeException.class,
                                           () -> repo.dispatchNowTo(id, request));
        assertThat(getRootCause(exception), instanceOf(IllegalStateException.class));
    }

    @Nested
    @DisplayName("return classes of")
    class ReturnClasses {

        @Test
        @DisplayName("commands")
        void command() {
            Set<CommandClass> commandClasses = repository().getCommandClasses();

            assertCommandClasses(
                    commandClasses,
                    PmCreateProject.class, PmCreateProject.class, PmStartProject.class
            );
        }

        @Test
        @DisplayName("events")
        void event() {
            Set<EventClass> eventClasses = repository().getMessageClasses();

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
    @DisplayName("throw ISE on registering to BC if repo is not subscribed to any messages")
    void notRegisterIfSubscribedToNothing() {
        SensoryDeprivedPmRepository repo = new SensoryDeprivedPmRepository();
        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .setMultitenant(false)
                                                      .build();
        repo.setBoundedContext(boundedContext);
        assertThrows(IllegalStateException.class, repo::onRegistered);
    }

    @Test
    @DisplayName("provide EventFilter which discards EntityStateChanged events")
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
        assertTrue(filter.filter(arbitraryEvent)
                         .isPresent());

        Any newState = pack(getCurrentTime());
        EntityHistoryId historyId = EntityHistoryId
                .newBuilder()
                .setTypeUrl(TypeUrl.ofEnclosed(newState).value())
                .setEntityId(EntityId.newBuilder().setId(pack(projectId)))
                .build();
        EventMessage discardedEvent = EntityStateChanged
                .newBuilder()
                .setId(historyId)
                .setNewState(newState)
                .build();
        assertFalse(filter.filter(discardedEvent).isPresent());
    }

    @Test
    @DisplayName("post all domain events through an EventFilter")
    void postEventsThroughFilter() {
        ProjectId projectId = ProjectId
                .newBuilder()
                .setId(newUuid())
                .build();
        PmCreateProject command = PmCreateProjectVBuilder
                .newBuilder()
                .setProjectId(projectId)
                .build();
        BlackBoxBoundedContext
                .singleTenant()
                .with(new EventDiscardingProcManRepository())
                .receivesCommand(command)
                .assertThat(emittedEvent(count(0)));
    }
}
