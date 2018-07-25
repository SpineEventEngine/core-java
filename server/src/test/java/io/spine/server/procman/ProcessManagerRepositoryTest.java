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

package io.spine.server.procman;

import com.google.common.collect.Lists;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.core.TenantId;
import io.spine.core.given.GivenEvent;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.RecordBasedRepositoryTest;
import io.spine.server.entity.rejection.StandardRejections;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyDeleted;
import io.spine.server.procman.given.ProcessManagerRepositoryTestEnv;
import io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.RememberingSubscriber;
import io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.SensoryDeprivedPmRepository;
import io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.TestProcessManager;
import io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.TestProcessManagerRepository;
import io.spine.test.procman.Project;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.Task;
import io.spine.test.procman.command.PmArchiveProcess;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmDeleteProcess;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.command.PmThrowEntityAlreadyArchived;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.entity.given.Given;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.spine.base.Identifier.newUuid;
import static io.spine.core.Rejections.createRejection;
import static io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.GivenCommandMessage.ID;
import static io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.GivenCommandMessage.addTask;
import static io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.GivenCommandMessage.archiveProcess;
import static io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.GivenCommandMessage.createProject;
import static io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.GivenCommandMessage.deleteProcess;
import static io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.GivenCommandMessage.doNothing;
import static io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.GivenCommandMessage.projectCreated;
import static io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.GivenCommandMessage.projectStarted;
import static io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.GivenCommandMessage.startProject;
import static io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.GivenCommandMessage.taskAdded;
import static io.spine.testing.server.Assertions.assertCommandClasses;
import static io.spine.testing.server.Assertions.assertEventClasses;
import static io.spine.testing.server.Assertions.assertRejectionClasses;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("ProcessManagerRepository should")
class ProcessManagerRepositoryTest
        extends RecordBasedRepositoryTest<TestProcessManager,
                                          ProjectId,
                                          Project> {

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
        List<TestProcessManager> procmans = Lists.newArrayList();

        for (int i = 0; i < count; i++) {
            ProjectId id = createId(i);

            procmans.add(new TestProcessManager(id));
        }
        return procmans;
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
        boundedContext.register(repository);
        TestProcessManager.clearMessageDeliveryHistory();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        boundedContext.close();
        super.tearDown();
    }

    private ProcessManagerRepository<ProjectId, TestProcessManager, Project> repository() {
        return (ProcessManagerRepository<ProjectId, TestProcessManager, Project>) repository;
    }

    @SuppressWarnings("CheckReturnValue")
    // We can ignore the ID of the PM handling the command in the calling tests.
    private void dispatchCommand(Command command) {
        repository().dispatchCommand(CommandEnvelope.of(command));
    }

    private void testDispatchCommand(Message cmdMsg) {
        Command cmd = requestFactory.command()
                                    .create(cmdMsg);
        dispatchCommand(cmd);
        assertTrue(TestProcessManager.processed(cmdMsg));
    }

    @SuppressWarnings("CheckReturnValue") // can ignore IDs of target PMs in this test.
    private void testDispatchEvent(Message eventMessage) {
        CommandContext commandContext = requestFactory.createCommandContext();

        // EventContext should have CommandContext with appropriate TenantId to avoid usage
        // of different storages during command and event dispatching.
        EventContext eventContextWithTenantId =
                GivenEvent.context()
                          .toBuilder()
                          .setCommandContext(commandContext)
                          .build();
        Event event =
                GivenEvent.withMessage(eventMessage)
                          .toBuilder()
                          .setContext(eventContextWithTenantId)
                          .build();
        repository().dispatch(EventEnvelope.of(event));
        assertTrue(TestProcessManager.processed(eventMessage));
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

        @Test
        @DisplayName("rejection")
        void rejection() {
            CommandEnvelope ce = requestFactory.generateEnvelope();
            EntityAlreadyArchived rejectionMessage =
                    EntityAlreadyArchived.newBuilder()
                                         .setEntityId(Identifier.pack(newUuid()))
                                         .build();
            Rejection rejection = createRejection(rejectionMessage,
                                                  ce.getCommand());
            ProjectId id = ProcessManagerRepositoryTestEnv.GivenCommandMessage.ID;
            Rejection.Builder builder =
                    rejection.toBuilder()
                             .setContext(rejection.getContext()
                                                  .toBuilder()
                                                  .setProducerId(Identifier.pack(id)));
            RejectionEnvelope re = RejectionEnvelope.of(builder.build());

            Set<?> delivered = repository().dispatchRejection(re);

            assertTrue(delivered.contains(id));

            assertTrue(TestProcessManager.processed(rejectionMessage));
        }
    }

    @Test
    @DisplayName("dispatch command and post events")
    void dispatchCommandAndPostEvents() {
        RememberingSubscriber subscriber = new RememberingSubscriber();
        boundedContext.getEventBus()
                      .register(subscriber);

        testDispatchCommand(addTask());

        PmTaskAdded message = subscriber.getRemembered();
        assertNotNull(message);
        assertEquals(ID, message.getProjectId());
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
            PmDeleteProcess deleteProcess = deleteProcess();
            testDispatchCommand(deleteProcess);
            ProjectId projectId = deleteProcess.getProjectId();
            TestProcessManager processManager = repository().findOrCreate(projectId);
            assertTrue(processManager.isDeleted());

            // Dispatch a command to the deleted process manager.
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
            PmArchiveProcess archiveProcess = archiveProcess();
            testDispatchCommand(archiveProcess);
            ProjectId projectId = archiveProcess.getProjectId();
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
            PmArchiveProcess archiveProcess = archiveProcess();
            testDispatchCommand(archiveProcess);
            ProjectId projectId = archiveProcess.getProjectId();
            TestProcessManager processManager = repository().findOrCreate(projectId);
            assertTrue(processManager.isArchived());

            // Dispatch a command to the archived process manager.
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
            PmDeleteProcess deleteProcess = deleteProcess();
            testDispatchCommand(deleteProcess);
            ProjectId projectId = deleteProcess.getProjectId();
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
    @DisplayName("throw IAE when dispatching unknown command")
    void throwOnUnknownCommand() {
        Command unknownCommand =
                requestFactory.createCommand(Int32Value.getDefaultInstance());
        CommandEnvelope request = CommandEnvelope.of(unknownCommand);
        assertThrows(IllegalArgumentException.class, () -> repository().dispatchCommand(request));
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
                    PmProjectCreated.class, PmTaskAdded.class, PmProjectStarted.class
            );
        }

        @Test
        @DisplayName("rejections")
        void rejection() {
            Set<RejectionClass> rejectionClasses = repository().getRejectionClasses();

            assertRejectionClasses(
                    rejectionClasses,
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
        StandardRejections.EntityAlreadyArchived expected =
                StandardRejections.EntityAlreadyArchived.newBuilder()
                                                        .setEntityId(AnyPacker.pack(id))
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
}
