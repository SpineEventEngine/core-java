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

package io.spine.server.procman;

import com.google.common.collect.Lists;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.annotation.Subscribe;
import io.spine.base.Command;
import io.spine.base.Event;
import io.spine.base.EventContext;
import io.spine.envelope.CommandEnvelope;
import io.spine.envelope.EventEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.command.EventFactory;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.RecordBasedRepositoryShould;
import io.spine.server.event.EventSubscriber;
import io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.TestProcessManager;
import io.spine.server.procman.given.ProcessManagerRepositoryTestEnv.TestProcessManagerRepository;
import io.spine.test.EventTests;
import io.spine.test.Given;
import io.spine.test.TestActorRequestFactory;
import io.spine.test.procman.Project;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.AddTask;
import io.spine.test.procman.command.CreateProject;
import io.spine.test.procman.command.StartProject;
import io.spine.test.procman.event.ProjectCreated;
import io.spine.test.procman.event.ProjectStarted;
import io.spine.test.procman.event.TaskAdded;
import io.spine.testdata.Sample;
import io.spine.testdata.TestBoundedContextFactory;
import io.spine.type.CommandClass;
import io.spine.type.EventClass;
import io.spine.users.TenantId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import static io.spine.base.Identifier.newUuid;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public class ProcessManagerRepositoryShould
        extends RecordBasedRepositoryShould<TestProcessManager,
        ProjectId,
        Project> {

    private static final ProjectId ID = Sample.messageOfType(ProjectId.class);

    private BoundedContext boundedContext;

    /**
     * Creates an instance of {@link Event} for the passed message.
     *
     * <p>Under normal circumstances an event is produced via {@link EventFactory}.
     * Processing of events in a {@link ProcessManagerRepository} is based on event messages
     * and does not need a properly configured {@link EventContext}. That's why this factory method
     * is sufficient for the purpose of this test suite.
     */
    private static Event createEvent(Message eventMessage) {
        return EventTests.createContextlessEvent(eventMessage);
    }

    private static CreateProject createProject() {
        return ((CreateProject.Builder) Sample.builderForType(CreateProject.class))
                .setProjectId(ID)
                .build();
    }

    private static StartProject startProject() {
        return ((StartProject.Builder) Sample.builderForType(StartProject.class))
                .setProjectId(ID)
                .build();
    }

    private static AddTask addTask() {
        return ((AddTask.Builder) Sample.builderForType(AddTask.class))
                .setProjectId(ID)
                .build();
    }

    private static ProjectStarted projectStarted() {
        return ((ProjectStarted.Builder) Sample.builderForType(ProjectStarted.class))
                .setProjectId(ID)
                .build();
    }

    private static ProjectCreated projectCreated() {
        return ((ProjectCreated.Builder) Sample.builderForType(ProjectCreated.class))
                .setProjectId(ID)
                .build();
    }

    private static TaskAdded taskAdded() {
        return ((TaskAdded.Builder) Sample.builderForType(TaskAdded.class))
                .setProjectId(ID)
                .build();
    }

    @Override
    protected RecordBasedRepository<ProjectId, TestProcessManager, Project> createRepository() {
        boundedContext = TestBoundedContextFactory.MultiTenant.newBoundedContext();
        final TestProcessManagerRepository repo = new TestProcessManagerRepository();
        boundedContext.register(repo);
        return repo;
    }

    @Override
    protected TestProcessManager createEntity() {
        final ProjectId id = ProjectId.newBuilder()
                                      .setId("123-id")
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

    // Tests
    //----------------------------

    @Override
    protected ProjectId createId(int value) {
        return ProjectId.newBuilder()
                        .setId(format("procman-number-%s", value))
                        .build();
    }

    @Override
    @Before
    public void setUp() {
        super.setUp();
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();

        boundedContext.getCommandBus()
                      .register(new CommandDispatcher() {
                          @Override
                          public Set<CommandClass> getMessageClasses() {
                              return CommandClass.setOf(AddTask.class);
                          }

                          @Override
                          public void dispatch(CommandEnvelope envelope) {
                              /* Simply swallow the command. We need this dispatcher for allowing
                                 Process Manager under test to route the AddTask command. */
                          }
                      });

        TestProcessManager.clearMessageDeliveryHistory();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        boundedContext.close();
        super.tearDown();
    }

    ProcessManagerRepository<?, ?, ?> repository() {
        return (ProcessManagerRepository<?, ?, ?>) repository;
    }

    private void testDispatchEvent(Message eventMessage) {
        final Event event = createEvent(eventMessage);
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
    public void dispatch_command() throws InvocationTargetException {
        testDispatchCommand(addTask());
    }

    @Test
    public void dispatch_several_commands() throws InvocationTargetException {
        testDispatchCommand(createProject());
        testDispatchCommand(addTask());
        testDispatchCommand(startProject());
    }

    private void testDispatchCommand(Message cmdMsg) throws InvocationTargetException {
        final TenantId generatedTenantId = TenantId.newBuilder()
                                                   .setValue(newUuid())
                                                   .build();
        final Command cmd =
                TestActorRequestFactory.newInstance(ProcessManagerRepositoryShould.class,
                                                    generatedTenantId)
                                       .command()
                                       .create(cmdMsg);

        repository().dispatchCommand(CommandEnvelope.of(cmd));
        assertTrue(TestProcessManager.processed(cmdMsg));
    }

    @Test
    public void dispatch_command_and_post_events() throws InvocationTargetException {
        final Subscriber subscriber = new Subscriber();
        boundedContext.getEventBus()
                      .register(subscriber);

        testDispatchCommand(addTask());

        final TaskAdded message = subscriber.remembered;
        assertEquals(ID, message.getProjectId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_dispatch_unknown_command() throws InvocationTargetException {
        final TestActorRequestFactory factory = TestActorRequestFactory.newInstance(getClass());
        final Command unknownCommand = factory.createCommand(Int32Value.getDefaultInstance());
        final CommandEnvelope request = CommandEnvelope.of(unknownCommand);
        repository().dispatchCommand(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_dispatch_unknown_event() {
        final StringValue unknownEventMessage = StringValue.getDefaultInstance();
        final Event event = createEvent(unknownEventMessage);
        repository().dispatch(EventEnvelope.of(event));
    }

    @Test
    public void return_command_classes() {
        final Set<CommandClass> commandClasses = repository().getCommandClasses();
        assertTrue(commandClasses.contains(CommandClass.of(CreateProject.class)));
        assertTrue(commandClasses.contains(CommandClass.of(AddTask.class)));
        assertTrue(commandClasses.contains(CommandClass.of(StartProject.class)));
    }

    @Test
    public void return_event_classes() {
        final Set<EventClass> eventClasses = repository().getMessageClasses();
        assertTrue(eventClasses.contains(EventClass.of(ProjectCreated.class)));
        assertTrue(eventClasses.contains(EventClass.of(TaskAdded.class)));
        assertTrue(eventClasses.contains(EventClass.of(ProjectStarted.class)));
    }

    private static class Subscriber extends EventSubscriber {

        private TaskAdded remembered;

        @Subscribe
        void on(TaskAdded msg) {
            remembered = msg;
        }
    }
}
