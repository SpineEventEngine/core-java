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
import io.spine.client.TestActorRequestFactory;
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
import io.spine.core.Subscribe;
import io.spine.core.TenantId;
import io.spine.core.given.GivenEvent;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.RecordBasedRepositoryShould;
import io.spine.server.entity.given.Given;
import io.spine.server.entity.rejection.StandardRejections;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyDeleted;
import io.spine.server.event.EventSubscriber;
import io.spine.server.procman.given.ProcessManagerRepositoryTestEnv;
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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Set;

import static io.spine.base.Identifier.newUuid;
import static io.spine.core.Rejections.createRejection;
import static io.spine.server.TestCommandClasses.assertContains;
import static io.spine.server.TestEventClasses.assertContains;
import static io.spine.server.TestRejectionClasses.assertContains;
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
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public class ProcessManagerRepositoryShould
        extends RecordBasedRepositoryShould<TestProcessManager,
        ProjectId,
        Project> {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass(),
                                                TenantId.newBuilder()
                                                        .setValue(newUuid())
                                                        .build());
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private BoundedContext boundedContext;

    @Override
    protected RecordBasedRepository<ProjectId, TestProcessManager, Project> createRepository() {
        final TestProcessManagerRepository repo = new TestProcessManagerRepository();
        return repo;
    }

    @Override
    protected TestProcessManager createEntity() {
        final ProjectId id = ProjectId.newBuilder()
                                      .setId(newUuid())
                                      .build();
        final TestProcessManager result = Given.processManagerOfClass(TestProcessManager.class)
                                               .withId(id)
                                               .build();
        return result;
    }

    @Override
    protected List<TestProcessManager> createEntities(int count) {
        final List<TestProcessManager> procmans = Lists.newArrayList();

        for (int i = 0; i < count; i++) {
            final ProjectId id = createId(i);

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

    /*
     * Tests
     *************/

    @Override
    @Before
    public void setUp() {
        super.setUp();
        setCurrentTenant(requestFactory.getTenantId());
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();
        boundedContext.register(repository);
        TestProcessManager.clearMessageDeliveryHistory();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        boundedContext.close();
        super.tearDown();
    }

    ProcessManagerRepository<ProjectId, TestProcessManager, Project> repository() {
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

    @Test
    public void dispatch_event_and_load_manager() {
        testDispatchEvent(projectCreated());
    }

    @Test
    public void dispatch_several_events() {
        testDispatchEvent(projectCreated());
        testDispatchEvent(taskAdded());
        testDispatchEvent(projectStarted());
    }

    @Test
    public void dispatch_event_to_archived_process_manager() {
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

    @Test
    public void dispatch_event_to_deleted_process_manager() {
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

    @Test
    public void dispatch_command() {
        testDispatchCommand(addTask());
    }

    @Test
    public void dispatch_several_commands() {
        testDispatchCommand(createProject());
        testDispatchCommand(addTask());
        testDispatchCommand(startProject());
    }

    @Test
    public void dispatch_command_to_archived_process_manager() {
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
    public void dispatch_command_to_deleted_process_manager() {
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
    public void allow_ProcMan_have_unmodified_state_after_command_handling() {
        testDispatchCommand(doNothing());
    }

    @Test
    public void dispatch_command_and_post_events() {
        RememberingSubscriber subscriber = new RememberingSubscriber();
        boundedContext.getEventBus()
                      .register(subscriber);

        testDispatchCommand(addTask());

        PmTaskAdded message = subscriber.remembered;
        assertNotNull(message);
        assertEquals(ID, message.getProjectId());
    }

    @Test
    public void throw_exception_if_dispatch_unknown_command() {
        Command unknownCommand =
                requestFactory.createCommand(Int32Value.getDefaultInstance());
        CommandEnvelope request = CommandEnvelope.of(unknownCommand);
        thrown.expect(IllegalArgumentException.class);
        repository().dispatchCommand(request);
    }

    @Test
    public void return_command_classes() {
        Set<CommandClass> commandClasses = repository().getCommandClasses();

        assertContains(commandClasses,
                       PmCreateProject.class, PmCreateProject.class, PmStartProject.class);
    }

    @Test
    public void return_event_classes() {
        Set<EventClass> eventClasses = repository().getMessageClasses();

        assertContains(eventClasses,
                       PmProjectCreated.class, PmTaskAdded.class, PmProjectStarted.class);
    }

    @Test
    public void return_rejection_classes() {
        Set<RejectionClass> rejectionClasses = repository().getRejectionClasses();

        assertContains(rejectionClasses,
                       EntityAlreadyArchived.class, EntityAlreadyDeleted.class);
    }

    @Test
    public void post_command_rejections() {
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
    public void dispatch_rejection() {
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

    @Test
    public void throw_exception_on_attempt_to_register_in_bc_with_no_messages_handled() {
        SensoryDeprivedPmRepository repo = new SensoryDeprivedPmRepository();
        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .setMultitenant(false)
                                                      .build();
        repo.setBoundedContext(boundedContext);
        thrown.expect(IllegalStateException.class);
        repo.onRegistered();
    }

    /**
     * Helper event subscriber which remembers an event message.
     */
    private static class RememberingSubscriber extends EventSubscriber {

        private @Nullable PmTaskAdded remembered;

        @Subscribe
        void on(PmTaskAdded msg) {
            remembered = msg;
        }
    }
}
