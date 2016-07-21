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

package org.spine3.server.bc;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Response;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;
import org.spine3.server.entity.IdFunction;
import org.spine3.server.entity.Repository;
import org.spine3.server.event.EventBus;
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
import org.spine3.test.bc.Project;
import org.spine3.test.bc.ProjectId;
import org.spine3.test.bc.command.AddTask;
import org.spine3.test.bc.command.CreateProject;
import org.spine3.test.bc.command.StartProject;
import org.spine3.test.bc.event.ProjectCreated;
import org.spine3.test.bc.event.ProjectStarted;
import org.spine3.test.bc.event.TaskAdded;
import org.spine3.testdata.TestCommandBusFactory;
import org.spine3.testdata.TestEventBusFactory;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.spine3.base.Responses.ok;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class BoundedContextShould {

    private final TestEventSubscriber subscriber = new TestEventSubscriber();

    private final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();

    private final BoundedContext boundedContext = newBoundedContext();

    private boolean handlersRegistered = false;

    @After
    public void tearDown() throws Exception {
        if (handlersRegistered) {
            boundedContext.getEventBus().unsubscribe(subscriber);
        }
        boundedContext.close();
    }

    /** Registers all test repositories, handlers etc. */
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
    public void notify_integration_event_subscriber() {
        registerAll();
        final TestResponseObserver observer = new TestResponseObserver();
        final IntegrationEvent event = Given.IntegrationEvent.projectCreated();
        final Message msg = unpack(event.getMessage());

        boundedContext.notify(event, observer);

        assertEquals(ok(), observer.getResponseHandled());
        assertEquals(subscriber.eventHandled, msg);
    }

    @Test
    @SuppressWarnings("unchecked") // have to cast to use Mockito
    public void not_notify_integration_event_subscriber_if_event_is_invalid() {
        final EventBus eventBus = mock(EventBus.class);
        doReturn(false).when(eventBus)
                       .validate(any(Message.class), (StreamObserver<Response>) any());
        final BoundedContext boundedContext = newBoundedContext(eventBus);
        final IntegrationEvent event = Given.IntegrationEvent.projectCreated();

        boundedContext.notify(event, new TestResponseObserver());

        verify(eventBus, never()).post(any(Event.class));
    }

    @Test
    public void tell_if_set_multitenant() {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setStorageFactory(InMemoryStorageFactory.getInstance())
                                                .setCommandBus(TestCommandBusFactory.create(storageFactory))
                                                .setEventBus(TestEventBusFactory.create(storageFactory))
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
            return Given.EventMessage.projectCreated(cmd.getProjectId());
        }

        @Assign
        public TaskAdded handle(AddTask cmd, CommandContext ctx) {
            isAddTaskCommandHandled = true;
            return Given.EventMessage.taskAdded(cmd.getProjectId());
        }

        @Assign
        public List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            isStartProjectCommandHandled = true;
            final ProjectStarted message = Given.EventMessage.projectStarted(cmd.getProjectId());
            return newArrayList(message);
        }

        @Apply
        private void event(ProjectCreated event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.CREATED);

            isProjectCreatedEventApplied = true;
        }

        @Apply
        private void event(TaskAdded event) {
            isTaskAddedEventApplied = true;
        }

        @Apply
        private void event(ProjectStarted event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.STARTED)
                    .build();

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

    @Test
    public void assign_storage_during_registration_if_repository_does_not_have_storage() {
        final ProjectAggregateRepository repository = new ProjectAggregateRepository(boundedContext);
        boundedContext.register(repository);
        assertTrue(repository.storageAssigned());
    }

    @Test
    public void do_not_change_storage_during_registration_if_a_repository_has_one() {
        final ProjectAggregateRepository repository = new ProjectAggregateRepository(boundedContext);
        repository.initStorage(storageFactory);

        final Repository spy = spy(repository);
        boundedContext.register(repository);
        verify(spy, never()).initStorage(any(StorageFactory.class));
    }
}
