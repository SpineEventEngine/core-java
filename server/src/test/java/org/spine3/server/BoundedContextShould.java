/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.Response;
import org.spine3.base.UserId;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;
import org.spine3.server.command.CommandBus;
import org.spine3.server.entity.IdFunction;
import org.spine3.server.event.EventSubscriber;
import org.spine3.server.event.GetProducerIdFromEvent;
import org.spine3.server.event.Subscribe;
import org.spine3.server.integration.IntegrationEvent;
import org.spine3.server.procman.CommandRouted;
import org.spine3.server.procman.ProcessManager;
import org.spine3.server.procman.ProcessManagerRepository;
import org.spine3.server.projection.Projection;
import org.spine3.server.projection.ProjectionRepository;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.EventClass;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;
import org.spine3.testdata.TestAggregateIdFactory;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.base.Responses.ok;
import static org.spine3.client.UserUtil.newUserId;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.testdata.TestCommands.createProjectCmd;
import static org.spine3.testdata.TestCommands.newCommandBus;
import static org.spine3.testdata.TestEventFactory.newEventBus;
import static org.spine3.testdata.TestEventFactory.projectCreatedIntegrationEvent;
import static org.spine3.testdata.TestEventMessageFactory.*;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class BoundedContextShould {

    private final UserId userId = newUserId(newUuid());
    private final ProjectId projectId = TestAggregateIdFactory.newProjectId();
    private final TestEventSubscriber subscriber = new TestEventSubscriber();

    private StorageFactory storageFactory;
    private BoundedContext boundedContext;
    private boolean handlersRegistered = false;

    @Before
    public void setUp() {
        storageFactory = InMemoryStorageFactory.getInstance();
        boundedContext = BoundedContextTestStubs.create(storageFactory);
    }

    @After
    public void tearDown() throws Exception {
        if (handlersRegistered) {
            boundedContext.getEventBus().unsubscribe(subscriber);
        }
        boundedContext.close();
    }

    /**
     * Registers all test repositories, handlers etc.
     */
    private void registerAll() {
        final ProjectAggregateRepository repository = new ProjectAggregateRepository(boundedContext);
        repository.initStorage(InMemoryStorageFactory.getInstance());
        boundedContext.register(repository);
        boundedContext.getEventBus().subscribe(subscriber);
        handlersRegistered = true;
    }

    @Test
    public void return_EventBus() {
        assertNotNull(boundedContext.getEventBus());
    }

    @Test
    public void return_CommandDispatcher() {
        assertNotNull(boundedContext.getCommandBus());
    }

    @Test
    public void register_AggregateRepository() {
        final ProjectAggregateRepository repository = new ProjectAggregateRepository(boundedContext);
        repository.initStorage(storageFactory);
        boundedContext.register(repository);
    }

    @Test
    public void register_ProcessManagerRepository() {
        final ProjectPmRepo repository = new ProjectPmRepo(boundedContext);
        repository.initStorage(storageFactory);
        boundedContext.register(repository);
    }

    @Test
    public void register_ProjectionRepository() {
        final ProjectReportRepository repository = new ProjectReportRepository(boundedContext);
        repository.initStorage(storageFactory);
        boundedContext.register(repository);
    }

    @Test
    public void post_commands_to_CommandBus() {
        final TestResponseObserver responseObserver = new TestResponseObserver();
        final CommandBus commandBus = boundedContext.getCommandBus();

        final Command cmd = createProjectCmd();
        boundedContext.post(cmd, responseObserver);

        verify(commandBus, times(1)).post(cmd, responseObserver);
    }

    @Test
    public void notify_integration_event_subscribers() {
        registerAll();
        final TestResponseObserver observer = new TestResponseObserver();
        final IntegrationEvent event = projectCreatedIntegrationEvent();
        final Message msg = fromAny(event.getMessage());

        boundedContext.notify(event, observer);

        assertEquals(ok(), observer.getResponseHandled());
        assertEquals(subscriber.eventHandled, msg);
    }

    @Test
    public void tell_if_set_multitenant() {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setStorageFactory(InMemoryStorageFactory.getInstance())
                                                .setCommandBus(newCommandBus(storageFactory))
                                                .setEventBus(newEventBus(storageFactory))
                                                .setMultitenant(true)
                                                .build();
        assertTrue(bc.isMultitenant());
    }

    private static class TestResponseObserver implements StreamObserver<Response> {

        private Response responseHandled;

        @Override
        public void onNext(Response response) {
            this.responseHandled = response;
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onCompleted() {
        }

        public Response getResponseHandled() {
            return responseHandled;
        }
    }

    @SuppressWarnings({"unused", "TypeMayBeWeakened"})
    private static class ProjectAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

        private boolean isCreateProjectCommandHandled = false;
        private boolean isAddTaskCommandHandled = false;
        private boolean isStartProjectCommandHandled = false;

        private boolean isProjectCreatedEventApplied = false;
        private boolean isTaskAddedEventApplied = false;
        private boolean isProjectStartedEventApplied = false;

        // an aggregate constructor must be public because it is used via reflection
        @SuppressWarnings("PublicConstructorInNonPublicClass")
        public ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            isCreateProjectCommandHandled = true;
            return projectCreatedMsg(cmd.getProjectId());
        }

        @Assign
        public TaskAdded handle(AddTask cmd, CommandContext ctx) {
            isAddTaskCommandHandled = true;
            return taskAddedMsg(cmd.getProjectId());
        }

        @Assign
        public List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            isStartProjectCommandHandled = true;
            final ProjectStarted message = projectStartedMsg(cmd.getProjectId());
            return newArrayList(message);
        }

        @Apply
        private void event(ProjectCreated event) {

            final Project newState = Project.newBuilder(getState())
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.CREATED)
                    .build();

            incrementState(newState);

            isProjectCreatedEventApplied = true;
        }

        @Apply
        private void event(TaskAdded event) {
            isTaskAddedEventApplied = true;
        }

        @Apply
        private void event(ProjectStarted event) {

            final Project newState = Project.newBuilder(getState())
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.STARTED)
                    .build();

            incrementState(newState);

            isProjectStartedEventApplied = true;
        }
    }

    private static class ProjectAggregateRepository extends AggregateRepository<ProjectId, ProjectAggregate> {
        private ProjectAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    @SuppressWarnings("UnusedParameters") // It is intended in this empty handler class.
    private static class TestEventSubscriber extends EventSubscriber {

        private ProjectCreated eventHandled;

        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            this.eventHandled = event;
        }

        @Subscribe
        public void on(TaskAdded event, EventContext context) {
        }

        @Subscribe
        public void on(ProjectStarted event, EventContext context) {
        }
    }

    private static class ProjectProcessManager extends ProcessManager<ProjectId, Empty> {

        // a ProcessManager constructor must be public because it is used via reflection
        @SuppressWarnings("PublicConstructorInNonPublicClass")
        public ProjectProcessManager(ProjectId id) {
            super(id);
        }

        @Assign
        @SuppressWarnings({"UnusedParameters", "unused"}) // OK for test method
        public CommandRouted handle(CreateProject command, CommandContext ctx) {
            return CommandRouted.getDefaultInstance();
        }

        @SuppressWarnings("UnusedParameters") // OK for test method
        @Subscribe
        public void on(ProjectCreated event, EventContext ctx) {
            // Do nothing, just watch.
        }
    }

    private static class ProjectPmRepo extends ProcessManagerRepository<ProjectId, ProjectProcessManager, Empty> {

        private ProjectPmRepo(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        public IdFunction<ProjectId, ? extends Message, EventContext> getIdFunction(EventClass eventClass) {
            return GetProducerIdFromEvent.newInstance(0);
        }
    }

    private static class ProjectReport extends Projection<ProjectId, Empty> {

        @SuppressWarnings("PublicConstructorInNonPublicClass")
        // Public constructor is a part of projection public API. It's called by a repository.
        public ProjectReport(ProjectId id) {
            super(id);
        }

        @SuppressWarnings("UnusedParameters") // OK for test method.
        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            // Do nothing. We have the method so that there's one event class exposed by the repository.
        }
    }

    private static class ProjectReportRepository extends ProjectionRepository<ProjectId, ProjectReport, Empty> {
        protected ProjectReportRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_repository_has_storage_assigned_upon_registration() {
        boundedContext.register(new ProjectAggregateRepository(boundedContext));
    }
}
