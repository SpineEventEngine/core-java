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

package org.spine3.server.bc;

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.Response;
import org.spine3.base.Subscribe;
import org.spine3.option.EntityOption;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;
import org.spine3.server.commandbus.CommandBus;
import org.spine3.server.entity.Repository;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.EventSubscriber;
import org.spine3.server.integration.IntegrationEvent;
import org.spine3.server.procman.CommandRouted;
import org.spine3.server.procman.ProcessManager;
import org.spine3.server.procman.ProcessManagerRepository;
import org.spine3.server.projection.Projection;
import org.spine3.server.projection.ProjectionRepository;
import org.spine3.server.stand.Stand;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.StorageFactorySwitch;
import org.spine3.test.Spy;
import org.spine3.test.bc.Project;
import org.spine3.test.bc.ProjectId;
import org.spine3.test.bc.ProjectValidatingBuilder;
import org.spine3.test.bc.SecretProject;
import org.spine3.test.bc.SecretProjectValidatingBuilder;
import org.spine3.test.bc.command.AddTask;
import org.spine3.test.bc.command.CreateProject;
import org.spine3.test.bc.command.StartProject;
import org.spine3.test.bc.event.ProjectCreated;
import org.spine3.test.bc.event.ProjectStarted;
import org.spine3.test.bc.event.TaskAdded;
import org.spine3.testdata.TestBoundedContextFactory.MultiTenant;
import org.spine3.validate.ValidatingBuilders.EmptyValidatingBuilder;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.spine3.base.Responses.ok;
import static org.spine3.protobuf.AnyPacker.unpack;

/**
 * Messages used in this test suite are defined in:
 * <ul>
 *     <li>spine/test/bc/project.proto - data types
 *     <li>spine/test/bc/commands.proto — commands
 *     <li>spine/test/bc/events.proto — events.
 * </ul>
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public class BoundedContextShould {

    private final TestEventSubscriber subscriber = new TestEventSubscriber();

    private StorageFactory storageFactory;

    private BoundedContext boundedContext;

    private boolean handlersRegistered = false;

    @Before
    public void setUp() {
        boundedContext = MultiTenant.newBoundedContext();
        storageFactory = StorageFactorySwitch.get(boundedContext.isMultitenant());
    }

    @After
    public void tearDown() throws Exception {
        if (handlersRegistered) {
            boundedContext.getEventBus().unregister(subscriber);
        }
        boundedContext.close();
    }

    /** Registers all test repositories, handlers etc. */
    private void registerAll() {
        final ProjectAggregateRepository repo = new ProjectAggregateRepository(boundedContext);
        repo.initStorage(storageFactory);
        boundedContext.register(repo);
        boundedContext.getEventBus().register(subscriber);
        handlersRegistered = true;
    }

    @Test
    public void return_EventBus() {
        assertNotNull(boundedContext.getEventBus());
    }

    @Test
    public void return_FailureBus() {
        assertNotNull(boundedContext.getFailureBus());
    }

    @Test
    public void return_CommandDispatcher() {
        assertNotNull(boundedContext.getCommandBus());
    }

    @Test
    public void register_AggregateRepository() {
        final ProjectAggregateRepository repository =
                new ProjectAggregateRepository(boundedContext);
        repository.initStorage(storageFactory);
        boundedContext.register(repository);
    }

    @Test(expected = IllegalStateException.class)
    public void not_allow_two_aggregate_repositories_with_aggregates_with_the_same_state() {
        final ProjectAggregateRepository repository =
                new ProjectAggregateRepository(boundedContext);
        repository.initStorage(storageFactory);
        boundedContext.register(repository);

        final AnotherProjectAggregateRepository anotherRepo =
                new AnotherProjectAggregateRepository(boundedContext);
        repository.initStorage(storageFactory);
        boundedContext.register(anotherRepo);
    }

    private static class AnotherProjectAggregate
                   extends Aggregate<ProjectId, Project, ProjectValidatingBuilder> {
        protected AnotherProjectAggregate(ProjectId id) {
            super(id);
        }
    }

    private static class AnotherProjectAggregateRepository
                   extends AggregateRepository<ProjectId, AnotherProjectAggregate> {
        private AnotherProjectAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
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
        final IntegrationEvent event = Given.AnIntegrationEvent.projectCreated();
        final Message msg = unpack(event.getMessage());

        boundedContext.notify(event, observer);

        assertEquals(ok(), observer.getResponseHandled());
        assertEquals(subscriber.eventHandled, msg);
    }

    @Test
    public void not_notify_integration_event_subscriber_if_event_is_invalid() {
        final BoundedContext boundedContext = MultiTenant.newBoundedContext();
        final TestEventSubscriber sub = new TestEventSubscriber();
        boundedContext.getEventBus()
                      .register(sub);

        final Any invalidMsg = AnyPacker.pack(ProjectCreated.getDefaultInstance());
        final IntegrationEvent event =
                Given.AnIntegrationEvent.projectCreated()
                                        .toBuilder()
                                        .setMessage(invalidMsg)
                                        .build();

        boundedContext.notify(event, new TestResponseObserver());

        assertNull(sub.eventHandled);
    }

    @Test
    public void tell_if_set_multitenant() {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setMultitenant(true)
                                                .build();
        assertTrue(bc.isMultitenant());
    }

    @Test
    public void assign_storage_during_registration_if_repository_does_not_have_storage() {
        final ProjectAggregateRepository repository =
                new ProjectAggregateRepository(boundedContext);
        boundedContext.register(repository);
        assertTrue(repository.storageAssigned());
    }

    @Test
    public void not_change_storage_during_registration_if_a_repository_has_one() {
        final ProjectAggregateRepository repository =
                new ProjectAggregateRepository(boundedContext);
        repository.initStorage(storageFactory);

        final Repository spy = spy(repository);
        boundedContext.register(repository);
        verify(spy, never()).initStorage(any(StorageFactory.class));
    }

    @Test
    public void set_storage_factory_for_EventBus() {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setEventBus(EventBus.newBuilder())
                                                .build();
        assertNotNull(bc.getEventBus());
    }

    @Test
    public void do_not_set_storage_factory_if_EventStore_is_set() {
        final EventStore eventStore = mock(EventStore.class);
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setEventBus(EventBus.newBuilder()
                                                .setEventStore(eventStore))
                                                .build();
        assertEquals(eventStore, bc.getEventBus()
                                   .getEventStore());
    }

    @Test
    public void propagate_registered_repositories_to_stand() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        final Stand stand = Spy.ofClass(Stand.class)
                               .on(boundedContext);

        verify(stand, never()).registerTypeSupplier(any(Repository.class));

        final ProjectAggregateRepository repository =
                new ProjectAggregateRepository(boundedContext);
        boundedContext.register(repository);
        verify(stand).registerTypeSupplier(eq(repository));
    }

    @Test(expected = IllegalStateException.class)
    public void match_multi_tenancy_with_CommandBus() {
        BoundedContext.newBuilder()
                      .setMultitenant(true)
                      .setCommandBus(CommandBus.newBuilder()
                                               .setMultitenant(false))
                      .build();
    }

    @Test
    public void set_multi_tenancy_in_CommandBus() {
        BoundedContext bc = BoundedContext.newBuilder()
                                          .setMultitenant(true)
                                          .build();

        assertEquals(bc.isMultitenant(), bc.getCommandBus()
                                           .isMultitenant());

        bc = BoundedContext.newBuilder()
                           .setMultitenant(false)
                           .build();

        assertEquals(bc.isMultitenant(), bc.getCommandBus()
                                           .isMultitenant());
    }

    @Test(expected = IllegalStateException.class)
    public void match_multi_tenancy_with_Stand() {
        BoundedContext.newBuilder()
                      .setMultitenant(true)
                      .setStand(Stand.newBuilder()
                                     .setMultitenant(false))
                      .build();

    }

    @Test
    public void set_same_multitenancy_in_Stand() {
        BoundedContext bc = BoundedContext.newBuilder()
                      .setMultitenant(true)
                      .build();

        assertEquals(bc.isMultitenant(), bc.getStand()
                                           .isMultitenant());

        bc = BoundedContext.newBuilder()
                           .setMultitenant(false)
                           .build();

        assertEquals(bc.isMultitenant(), bc.getStand()
                                           .isMultitenant());
    }

    /**
     * Simply checks that the result isn't empty to cover the integration with
     * {@link org.spine3.server.entity.VisibilityGuard VisibilityGuard}.
     *
     * <p>See {@linkplain org.spine3.server.entity.VisibilityGuardShould tests of VisibilityGuard}
     * for how visibility filtering works.
     */
    @Test
    public void obtain_entity_types_by_visibility() {
        assertTrue(boundedContext.getEntityTypes(EntityOption.Visibility.FULL)
                                  .isEmpty());

        registerAll();

        assertFalse(boundedContext.getEntityTypes(EntityOption.Visibility.FULL)
                                 .isEmpty());
    }


    @Test(expected = IllegalStateException.class)
    public void throw_ISE_when_no_repository_registered() {
        // Attempt to get a repository without registering.
        boundedContext.getAggregateRepository(Project.class);
    }

    @Test
    public void do_not_expose_invisible_aggregate() {
        boundedContext.register(new SecretProjectRepository(boundedContext));

        assertFalse(boundedContext.getAggregateRepository(SecretProject.class)
                                  .isPresent());
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
    private static class ProjectAggregate
            extends Aggregate<ProjectId, Project, ProjectValidatingBuilder> {

        private boolean isCreateProjectCommandHandled = false;
        private boolean isAddTaskCommandHandled = false;
        private boolean isStartProjectCommandHandled = false;

        private boolean isProjectCreatedEventApplied = false;
        private boolean isTaskAddedEventApplied = false;
        private boolean isProjectStartedEventApplied = false;

        private ProjectAggregate(ProjectId id) {
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

    private static class ProjectAggregateRepository
            extends AggregateRepository<ProjectId, ProjectAggregate> {
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

    private static class SecretProjectAggregate
            extends Aggregate<String, SecretProject, SecretProjectValidatingBuilder> {
        private SecretProjectAggregate(String id) {
            super(id);
        }

        @Assign
        public List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            return Lists.newArrayList();
        }
    }

    private static class SecretProjectRepository
            extends AggregateRepository<String, SecretProjectAggregate> {
        private SecretProjectRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    private static class ProjectProcessManager
            extends ProcessManager<ProjectId, Empty, EmptyValidatingBuilder> {

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

    private static class ProjectPmRepo
            extends ProcessManagerRepository<ProjectId, ProjectProcessManager, Empty> {

        private ProjectPmRepo(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    private static class ProjectReport
            extends Projection<ProjectId, Empty, EmptyValidatingBuilder> {

        @SuppressWarnings("PublicConstructorInNonPublicClass")
        // Public constructor is a part of projection public API. It's called by a repository.
        public ProjectReport(ProjectId id) {
            super(id);
        }

        @SuppressWarnings("UnusedParameters") // OK for test method.
        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            // Do nothing. We have the method so that there's one event class exposed
            // by the repository.
        }
    }

    private static class ProjectReportRepository
            extends ProjectionRepository<ProjectId, ProjectReport, Empty> {
        protected ProjectReportRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }
}
