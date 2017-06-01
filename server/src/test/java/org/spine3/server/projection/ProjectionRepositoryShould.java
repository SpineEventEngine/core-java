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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.spine3.annotation.Subscribe;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.base.Version;
import org.spine3.base.Versions;
import org.spine3.server.BoundedContext;
import org.spine3.server.entity.RecordBasedRepository;
import org.spine3.server.entity.RecordBasedRepositoryShould;
import org.spine3.server.entity.idfunc.EventTargetsFunction;
import org.spine3.server.event.EventStore;
import org.spine3.server.projection.ProjectionRepository.Status;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.StorageFactorySwitch;
import org.spine3.server.storage.memory.grpc.InMemoryGrpcServer;
import org.spine3.test.EventTests;
import org.spine3.test.Given;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.test.TestEventFactory;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.ProjectTaskNames;
import org.spine3.test.projection.ProjectTaskNamesValidatingBuilder;
import org.spine3.test.projection.event.ProjectCreated;
import org.spine3.test.projection.event.ProjectStarted;
import org.spine3.test.projection.event.TaskAdded;
import org.spine3.testdata.TestBoundedContextFactory;
import org.spine3.time.Time;
import org.spine3.type.EventClass;
import org.spine3.users.TenantId;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.server.projection.ProjectionRepository.Status.CATCHING_UP;
import static org.spine3.server.projection.ProjectionRepository.Status.CLOSED;
import static org.spine3.server.projection.ProjectionRepository.Status.CREATED;
import static org.spine3.server.projection.ProjectionRepository.Status.ONLINE;
import static org.spine3.test.Verify.assertContainsAll;
import static org.spine3.time.Time.getCurrentTime;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({
        "ClassWithTooManyMethods",
        "OverlyCoupledClass"})
public class ProjectionRepositoryShould
        extends RecordBasedRepositoryShould<TestProjection,
                                            ProjectId,
                                            Project> {

    private static final ProjectId ID = ProjectId.newBuilder()
                                                 .setId("p-123")
                                                 .build();
    private static final Any PRODUCER_ID = pack(ID);

    private BoundedContext boundedContext;
    private InMemoryGrpcServer grpcServer;

    private ProjectionRepository<ProjectId, TestProjection, Project> repository() {
        return (ProjectionRepository<ProjectId, TestProjection, Project>) repository;
    }

    /**
     * {@link EventTargetsFunction} used for testing add/get/remove of functions.
     */
    private static final EventTargetsFunction<ProjectId, ProjectCreated> creteProjectTargets =
            new EventTargetsFunction<ProjectId, ProjectCreated>() {
                private static final long serialVersionUID = 0L;

                @Override
                public Set<ProjectId> apply(ProjectCreated message, EventContext context) {
                    return newHashSet(message.getProjectId());
                }
            };

    @Override
    protected RecordBasedRepository<ProjectId, TestProjection, Project> createRepository() {
        return new TestProjectionRepository(boundedContext);
    }

    @Override
    protected TestProjection createEntity() {
        final TestProjection projection = Given.projectionOfClass(TestProjection.class)
                                               .withId(createId(42))
                                               .build();
        return projection;
    }

    @Override
    protected List<TestProjection> createEntities(int count) {
        final List<TestProjection> projections = new LinkedList<>();

        for (int i = 0; i < count; i++) {
            final TestProjection projection = Given.projectionOfClass(TestProjection.class)
                                                                   .withId(createId(i))
                                                                   .build();
            projections.add(projection);
        }

        return projections;
    }

    @Override
    protected ProjectId createId(int i) {
        return ProjectId.newBuilder()
                        .setId(format("test-projection-%s", i))
                        .build();
    }

    @AfterClass
    public static void afterClass() throws Exception {
    }

    @Override
    @Before
    public void setUp() {
        boundedContext = TestBoundedContextFactory.MultiTenant.newBoundedContext();
        grpcServer = InMemoryGrpcServer.startOn(boundedContext);
        super.setUp();

        repository.initStorage(storageFactory());
        boundedContext.register(repository);

        TestProjection.clearMessageDeliveryHistory();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        boundedContext.close();
        grpcServer.shutdown();
        super.tearDown();
    }

    private static TestEventFactory newEventFactory(TenantId tenantId, Any producerId) {
        final TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(ProjectionRepositoryShould.class, tenantId);
        return TestEventFactory.newInstance(producerId, requestFactory);
    }

    private static Event createEvent(TenantId tenantId, Message eventMessage, Any producerId) {
        final Version version = Versions.increment(Versions.create());
        return newEventFactory(tenantId, producerId).createEvent(eventMessage,
                                                                 version,
                                                                 Time.getCurrentTime());
    }

    private static void appendEvent(BoundedContext boundedContext, Event event) {
        final EventStore eventStore = boundedContext.getEventBus()
                                                    .getEventStore();
        eventStore.append(event);
        keepTenantIdFromEvent(boundedContext, event);
    }

    /*
     * Tests
     ************/

    /**
     * Tests that the repository become {@code ONLINE} after the catch-up.
     *
     * <p>As long as {@link TestProjectionRepository#initStorage(StorageFactory)} is called in
     * {@link #setUp()}, the catch-up should be automatically triggered.
     */
    @Test
    public void become_online_automatically_after_init_storage() {
        assertTrue(repository().isOnline());
    }


    @Test
    public void dispatch_event_and_load_projection() {
        checkDispatchesEvent(projectStarted());
    }

    @Test
    public void not_dispatch_event_if_is_not_online() {
        for (Status status : Status.values()) {
            if (status != ONLINE) {
                checkDoesNotDispatchEventWith(status);
            }
        }
    }

    @Test
    public void dispatch_several_events() {
        checkDispatchesEvent(projectCreated());
        checkDispatchesEvent(taskAdded());
        checkDispatchesEvent(projectStarted());
    }

    private void checkDispatchesEvent(Message eventMessage) {
        final TestEventFactory eventFactory = newEventFactory(tenantId(), PRODUCER_ID);
        final Event event = eventFactory.createEvent(eventMessage);

        keepTenantIdFromEvent(boundedContext, event);

        repository().dispatch(event);
        assertTrue(TestProjection.processed(eventMessage));
    }

    /**
     * Simulates updating TenantIndex, which occurs during command processing
     * in multi-tenant context.
     */
    private static void keepTenantIdFromEvent(BoundedContext boundedContext,
                                              Event event) {
        final TenantId tenantId = event.getContext()
                                       .getCommandContext()
                                       .getActorContext()
                                       .getTenantId();
        if (boundedContext.isMultitenant()) {
            boundedContext.getTenantIndex().keep(tenantId);
        }
    }

    private void checkDoesNotDispatchEventWith(Status status) {
        repository().setStatus(status);
        final ProjectCreated eventMsg = projectCreated();
        final Event event = createEvent(tenantId(), eventMsg, PRODUCER_ID);

        repository().dispatch(event);

        assertFalse(TestProjection.processed(eventMsg));
    }

    @Test(expected = RuntimeException.class)
    public void throw_exception_if_dispatch_unknown_event() {
        final StringValue unknownEventMessage = StringValue.getDefaultInstance();

        final Event event = EventTests.createContextlessEvent(unknownEventMessage);

        repository().dispatch(event);
    }

    @Test
    public void return_event_classes() {
        final Set<EventClass> eventClasses = repository().getMessageClasses();
        assertContainsAll(eventClasses,
                          EventClass.of(ProjectCreated.class),
                          EventClass.of(TaskAdded.class),
                          EventClass.of(ProjectStarted.class));
    }

    @Test
    public void return_entity_storage() {
        final RecordStorage<ProjectId> recordStorage = repository().recordStorage();
        assertNotNull(recordStorage);
    }

    @Test
    public void have_CREATED_status_by_default() {
        final BoundedContext bc = TestBoundedContextFactory.MultiTenant.newBoundedContext();
        final TestProjectionRepository repository = new TestProjectionRepository(bc);

        assertEquals(CREATED, repository.getStatus());
    }

    @Test
    public void update_status() {
        final Status status = CATCHING_UP;

        repository().setStatus(status);

        assertEquals(status, repository().getStatus());
    }

    @Test
    public void updates_status_to_CLOSED_on_close() throws Exception {
        repository.close();

        assertEquals(CLOSED, repository().getStatus());
    }

    @Test
    public void return_false_if_status_is_not_ONLINE() {
        repository().setStatus(CLOSED);

        assertFalse(repository().isOnline());
    }

    @Test
    public void return_true_if_explicitly_set_ONLINE() {
        repository().setStatus(CLOSED);
        repository().setOnline();
        assertTrue(repository().isOnline());
    }

    @Test
    public void catches_up_from_EventStorage() {
        ensureCatchesUpFromEventStorage(tenantId(), repository(), boundedContext);
    }

    static void ensureCatchesUpFromEventStorage(
            TenantId tenantId, ProjectionRepository<ProjectId, TestProjection, Project> repo,
            BoundedContext boundedContext) {

        final TestEventFactory eventFactory = newEventFactory(tenantId, PRODUCER_ID);

        // Put events into the EventStore.
        final ProjectCreated projectCreated = ProjectCreated.newBuilder()
                                                            .setProjectId(ID)
                                                            .build();
        Version version = Versions.newVersion(1, getCurrentTime());
        final Event projectCreatedEvent = eventFactory.createEvent(projectCreated, version);
        appendEvent(boundedContext, projectCreatedEvent);

        final TaskAdded taskAdded = TaskAdded.newBuilder()
                                             .setProjectId(ID)
                                             .build();
        version = Versions.increment(version);
        final Event taskAddedEvent = eventFactory.createEvent(taskAdded, version);
        appendEvent(boundedContext, taskAddedEvent);

        final ProjectStarted projectStarted = ProjectStarted.newBuilder()
                                                            .setProjectId(ID)
                                                            .build();
        version = Versions.increment(version);
        final Event projectStartedEvent = eventFactory.createEvent(projectStarted, version);
        appendEvent(boundedContext, projectStartedEvent);

        repo.catchUp();

        assertTrue(TestProjection.processed(projectCreated));
        assertTrue(TestProjection.processed(taskAdded));
        assertTrue(TestProjection.processed(projectStarted));

        // Assert that the projection was stored and has correct state.
        final Optional<TestProjection> optional = repo.find(ID);
        assertTrue(optional.isPresent());
        final TestProjection actual = optional.get();

        assertEquals(Project.Status.STARTED, actual.getState()
                                                   .getStatus());

        // Assert that the timestamp of the last event was stored.
        final Timestamp lastEventTimestamp = repo.readLastHandledEventTime();
        assertEquals(projectStartedEvent.getContext()
                                        .getTimestamp(), lastEventTimestamp);
    }

    @Test
    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass") // OK for this test.
    public void use_id_set_function() {
        final EventTargetsFunction<ProjectId, ProjectCreated> delegateFn =
                new EventTargetsFunction<ProjectId, ProjectCreated>() {
                    private static final long serialVersionUID = 0L;
                    @Override
                    public Set<ProjectId> apply(ProjectCreated message, EventContext context) {
                        return newHashSet();
                    }
                };

        final EventTargetsFunction<ProjectId, ProjectCreated> idSetFunction = spy(delegateFn);
        repository().addIdSetFunction(ProjectCreated.class, idSetFunction);

        final Event event = createEvent(tenantId(), projectCreated(), PRODUCER_ID);
        repository().dispatch(event);

        final ProjectCreated expectedEventMessage = Events.getMessage(event);
        final EventContext context = event.getContext();
        verify(idSetFunction).apply(eq(expectedEventMessage), eq(context));
    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
        // because the test checks that the function is present.
    public void obtain_id_set_function_after_put() {
        repository().addIdSetFunction(ProjectCreated.class, creteProjectTargets);

        final Optional<EventTargetsFunction<ProjectId, ProjectCreated>> func =
                repository().getIdSetFunction(ProjectCreated.class);

        assertTrue(func.isPresent());
        assertEquals(creteProjectTargets, func.get());
    }

    @Test
    public void remove_id_set_function_after_put() {
        repository().addIdSetFunction(ProjectCreated.class, creteProjectTargets);

        repository().removeIdSetFunction(ProjectCreated.class);
        final Optional<EventTargetsFunction<ProjectId, ProjectCreated>> out =
                repository().getIdSetFunction(ProjectCreated.class);

        assertFalse(out.isPresent());
    }

    @Test
    public void convert_null_timestamp_to_default() {
        final Timestamp timestamp = getCurrentTime();
        assertEquals(timestamp, ProjectionRepository.nullToDefault(timestamp));
        assertEquals(Timestamp.getDefaultInstance(), ProjectionRepository.nullToDefault(null));
    }

    @Test
    public void do_not_create_record_if_entity_is_not_updated() {
        final NoopTaskNamesRepository repo = new NoopTaskNamesRepository(boundedContext);
        repo.initStorage(storageFactory());

        assertTrue(repo.loadAll().isEmpty());

        final Event event = createEvent(tenantId(), projectCreated(), PRODUCER_ID);
        repo.dispatch(event);

        final ImmutableCollection<NoopTaskNamesProjection> items = repo.loadAll();
        assertTrue(items.isEmpty());
    }


    private StorageFactory storageFactory() {
        return StorageFactorySwitch.get(boundedContext.isMultitenant());
    }

    /**
     * The projection stub with the event subscribing methods that do nothing.
     *
     * <p>Such a projection allows to reproduce a use case, when the event-handling method
     * does not modify the state of an {@code Entity}. For the newly created entities it could lead
     * to an invalid entry created in the storage.
     */
    @SuppressWarnings("unused") // OK as event subscriber methods do nothing in this class.
    static class NoopTaskNamesProjection extends Projection<ProjectId,
                                                            ProjectTaskNames,
                                                            ProjectTaskNamesValidatingBuilder> {

        public NoopTaskNamesProjection(ProjectId id) {
            super(id);
        }

        @Subscribe
        public void on(ProjectCreated event) {
            // do nothing.
        }

        @Subscribe
        public void on(TaskAdded event) {
            // do nothing
        }
    }

    private static ProjectStarted projectStarted() {
        return ProjectStarted.newBuilder()
                             .setProjectId(ID)
                             .build();
    }

    private static ProjectCreated projectCreated() {
        return ProjectCreated.newBuilder()
                             .setProjectId(ID)
                             .build();
    }

    private static TaskAdded taskAdded() {
        return TaskAdded.newBuilder()
                        .setProjectId(ID)
                        .build();
    }

    /** Stub projection repository. */
    private static class TestProjectionRepository
            extends ProjectionRepository<ProjectId, TestProjection, Project> {
        private TestProjectionRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    /** Stub projection repository. */
    private static class NoopTaskNamesRepository
            extends ProjectionRepository<ProjectId, NoopTaskNamesProjection, ProjectTaskNames> {
        private NoopTaskNamesRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }
}
