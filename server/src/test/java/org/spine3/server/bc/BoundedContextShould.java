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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Response;
import org.spine3.option.EntityOption;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.BoundedContext;
import org.spine3.server.bc.given.BoundedContextTestEnv;
import org.spine3.server.bc.given.BoundedContextTestEnv.AnotherProjectAggregateRepository;
import org.spine3.server.bc.given.BoundedContextTestEnv.ProjectAggregateRepository;
import org.spine3.server.bc.given.BoundedContextTestEnv.ProjectPmRepo;
import org.spine3.server.bc.given.BoundedContextTestEnv.ProjectReportRepository;
import org.spine3.server.bc.given.BoundedContextTestEnv.TestEventSubscriber;
import org.spine3.server.bc.given.Given;
import org.spine3.server.commandbus.CommandBus;
import org.spine3.server.entity.Repository;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventStore;
import org.spine3.server.integration.IntegrationEvent;
import org.spine3.server.stand.Stand;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.StorageFactorySwitch;
import org.spine3.test.Spy;
import org.spine3.test.bc.Project;
import org.spine3.test.bc.SecretProject;
import org.spine3.test.bc.event.ProjectCreated;
import org.spine3.testdata.TestBoundedContextFactory.MultiTenant;

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
        final ProjectAggregateRepository repo = new ProjectAggregateRepository();
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
                new ProjectAggregateRepository();
        repository.initStorage(storageFactory);
        boundedContext.register(repository);
    }

    @Test(expected = IllegalStateException.class)
    public void not_allow_two_aggregate_repositories_with_aggregates_with_the_same_state() {
        final ProjectAggregateRepository repository =
                new ProjectAggregateRepository();
        repository.initStorage(storageFactory);
        boundedContext.register(repository);

        final AnotherProjectAggregateRepository anotherRepo =
                new AnotherProjectAggregateRepository();
        repository.initStorage(storageFactory);
        boundedContext.register(anotherRepo);
    }

    @Test
    public void register_ProcessManagerRepository() {
        final ProjectPmRepo repository = new ProjectPmRepo();
        boundedContext.register(repository);
    }

    @Test
    public void register_ProjectionRepository() {
        final ProjectReportRepository repository = new ProjectReportRepository();
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
        assertEquals(subscriber.getHandledEvent(), msg);
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

        assertNull(sub.getHandledEvent());
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
                new ProjectAggregateRepository();
        boundedContext.register(repository);
        assertTrue(repository.isStorageAssigned());
    }

    @Test
    public void not_change_storage_during_registration_if_a_repository_has_one() {
        final ProjectAggregateRepository repository =
                new ProjectAggregateRepository();
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
                new ProjectAggregateRepository();
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
        boundedContext.register(new BoundedContextTestEnv.SecretProjectRepository());

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

        private Response getResponseHandled() {
            return responseHandled;
        }
    }
}
