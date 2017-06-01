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

package org.spine3.server.projection;

import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.Versions;
import org.spine3.server.BoundedContext;
import org.spine3.server.event.EventStore;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.StorageFactorySwitch;
import org.spine3.server.storage.memory.grpc.InMemoryGrpcServer;
import org.spine3.server.tenant.TenantAwareTest;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.test.TestEventFactory;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.event.ProjectCreated;
import org.spine3.time.Durations2;
import org.spine3.time.Time;
import org.spine3.users.TenantId;

import java.util.Collection;

import static java.lang.String.valueOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.server.projection.ProjectionRepository.Status.STORAGE_ASSIGNED;
import static org.spine3.server.projection.ProjectionRepositoryShould.ensureCatchesUpFromEventStorage;
import static org.spine3.test.Tests.newTenantUuid;

/**
 * @author Alexander Yevsyukov
 */
public class ProjectionRepositoryManualCatchupShould extends TenantAwareTest {

    private ProjectionRepository<ProjectId, TestProjection, Project> repository;
    private BoundedContext boundedContext;
    private InMemoryGrpcServer grpcServer;

    @Before
    public void setUp() {
        setCurrentTenant(newTenantUuid());

        boundedContext = BoundedContext.newBuilder()
                                       .setName(getClass().getSimpleName())
                                       .setMultitenant(true)
                                       .build();
        grpcServer = InMemoryGrpcServer.startOn(boundedContext);

        repository = new ManualCatchupProjectionRepository(boundedContext);
        repository.initStorage(storageFactory());
        boundedContext.register(repository);

        TestProjection.clearMessageDeliveryHistory();
    }

    @After
    public void tearDown() throws Exception {
        boundedContext.close();
        grpcServer.shutdown();
    }

    private TestEventFactory newEventFactory(Any producerId) {
        final TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(getClass(), tenantId());
        return TestEventFactory.newInstance(producerId, requestFactory);
    }

    private Event createEvent(Any producerId, Message eventMessage) {
        return newEventFactory(producerId).createEvent(eventMessage,
                                                       Versions.increment(Versions.create()),
                                                       Time.getCurrentTime());
    }

    private void appendEvent(EventStore eventStore, Event event) {
        eventStore.append(event);
        keepTenantIdFromEvent(event);
    }

    private StorageFactory storageFactory() {
        return StorageFactorySwitch.get(boundedContext.isMultitenant());
    }

    private void keepTenantIdFromEvent(Event event) {
        final TenantId tenantId = event.getContext()
                                       .getCommandContext()
                                       .getActorContext()
                                       .getTenantId();
        if (boundedContext.isMultitenant()) {
            boundedContext.getTenantIndex().keep(tenantId);
        }
    }

    /**
     * Tests that the repository does not go {@code ONLINE} if automatic catch-up is disabled.
     *
     * <p>As long as {@code ManualCatchupProjectionRepository} has automatic catch-up disabled,
     * it does not become online automatically after
     * {@link ManualCatchupProjectionRepository#initStorage(StorageFactory)} is called.
     */
    @Test
    public void not_become_online_automatically_after_init_storage_if_auto_catch_up_disabled() {
        assertEquals(STORAGE_ASSIGNED, repository.getStatus());
        assertFalse(repository.isOnline());
    }

    @Test
    public void catches_up_from_EventStorage_even_if_automatic_catchup_disabled() {
        repository.setOnline();
        ensureCatchesUpFromEventStorage(tenantId(), repository, boundedContext);
    }

    @Test
    @SuppressWarnings("unchecked") // Due to mockito matcher usage
    public void skip_all_the_events_after_catch_up_outdated() throws InterruptedException {
        final int eventsCount = 10;
        final EventStore eventStore = boundedContext.getEventBus()
                                                    .getEventStore();
        for (int i = 0; i < eventsCount; i++) {
            final ProjectId projectId = ProjectId.newBuilder()
                                                 .setId(valueOf(i))
                                                 .build();
            final Message eventMessage = ProjectCreated.newBuilder()
                                                       .setProjectId(projectId)
                                                       .build();
            final Event event = createEvent(pack(projectId), eventMessage);
            appendEvent(eventStore, event);
        }
        // Set up repository
        final Duration duration = Durations2.nanos(1L);
        final ProjectionRepository repository =
                spy(new ManualCatchupProjectionRepository(boundedContext, duration));

        repository.initStorage(storageFactory());

        repository.catchUp();

        // Check bulk write
        verify(repository, never()).store(any(Projection.class));
    }

    @Ignore
        //TODO:2017-06-01:alexander.yevsyukov: Delete this test after Beam-based catch-up is finished.
        // Since Beam-based catch-up performs writing in multiple workers this test won't be
        // applicable after migration to Beam.
    @Test
    @SuppressWarnings("unchecked") // Due to mockito matcher usage
    public void perform_bulk_catch_up_if_required() {
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId("mock-project-id")
                                             .build();
        final Message eventMessage = ProjectCreated.newBuilder()
                                                   .setProjectId(projectId)
                                                   .build();
        final Event event = createEvent(pack(projectId), eventMessage);

        appendEvent(boundedContext.getEventBus()
                                  .getEventStore(), event);
        // Set up repository
        final Duration duration = Durations2.seconds(10L);
        final ProjectionRepository repository = spy(
                new ManualCatchupProjectionRepository(boundedContext, duration));
        repository.initStorage(storageFactory());
        repository.catchUp();

        // Check bulk write
        verify(repository).store(any(Collection.class));
        verify(repository, never()).store(any(TestProjection.class));
    }

    /** Stub projection repository with the disabled automatic catch-up */
    private static class ManualCatchupProjectionRepository
            extends ProjectionRepository<ProjectId, TestProjection, Project> {
        private ManualCatchupProjectionRepository(BoundedContext boundedContext) {
            super(boundedContext, false);
        }

        private ManualCatchupProjectionRepository(BoundedContext boundedContext,
                                                  Duration catchUpMaxDuration) {
            super(boundedContext, false, catchUpMaxDuration);
        }
    }
}
