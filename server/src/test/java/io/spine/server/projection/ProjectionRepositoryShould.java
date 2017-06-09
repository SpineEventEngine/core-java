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

package io.spine.server.projection;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Subscribe;
import io.spine.base.Event;
import io.spine.base.EventContext;
import io.spine.base.Events;
import io.spine.base.Version;
import io.spine.base.Versions;
import io.spine.envelope.EventEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.RecordBasedRepositoryShould;
import io.spine.server.entity.idfunc.EventTargetsFunction;
import io.spine.server.event.EventStore;
import io.spine.server.projection.ProjectionRepository.Status;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.ManualCatchupProjectionRepository;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.NoOpTaskNamesRepository;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.TestProjection;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.TestProjectionRepository;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.grpc.InMemoryGrpcServer;
import io.spine.test.EventTests;
import io.spine.test.Given;
import io.spine.test.TestActorRequestFactory;
import io.spine.test.TestEventFactory;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectTaskNames;
import io.spine.test.projection.ProjectTaskNamesValidatingBuilder;
import io.spine.test.projection.event.ProjectCreated;
import io.spine.test.projection.event.ProjectStarted;
import io.spine.test.projection.event.TaskAdded;
import io.spine.testdata.TestBoundedContextFactory;
import io.spine.type.EventClass;
import io.spine.users.TenantId;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.protobuf.util.Timestamps.add;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.base.Identifier.newUuid;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.projection.ProjectionRepository.Status.CATCHING_UP;
import static io.spine.server.projection.ProjectionRepository.Status.CLOSED;
import static io.spine.server.projection.ProjectionRepository.Status.CREATED;
import static io.spine.server.projection.ProjectionRepository.Status.ONLINE;
import static io.spine.test.Verify.assertContainsAll;
import static io.spine.time.Durations2.nanos;
import static io.spine.time.Durations2.seconds;
import static io.spine.time.Time.getCurrentTime;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        return new TestProjectionRepository();
    }

    @Override
    protected TestProjection createEntity() {
        final TestProjection projection = Given.projectionOfClass(TestProjection.class)
                                               .withId(ProjectId.newBuilder()
                                                                .setId(newUuid())
                                                                .build())
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

    @Override
    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .setName(getClass().getSimpleName())
                                       .setMultitenant(true)
                                       .build();
        grpcServer = InMemoryGrpcServer.startOn(boundedContext);
        super.setUp();

        boundedContext.register(repository);

        TestProjection.clearMessageDeliveryHistory();
    }

    /**
     * Closes the BoundedContest and shuts down the gRPC service.
     *
     * <p>The {@link #tearDown()} method of the super class will be invoked by JUnit automatically
     * after calling this method.
     */
    @After
    public void shutDown() throws Exception {
        boundedContext.close();
        grpcServer.shutdown();
    }

    private static TestEventFactory newEventFactory(TenantId tenantId, Any producerId) {
        final TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(ProjectionRepositoryShould.class, tenantId);
        return TestEventFactory.newInstance(producerId, requestFactory);
    }

    private static Event createEvent(TenantId tenantId,
                                     Message eventMessage,
                                     Any producerId,
                                     Timestamp when) {
        final Version version = Versions.increment(Versions.create());
        return newEventFactory(tenantId, producerId).createEvent(eventMessage,
                                                                 version,
                                                                 when);
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

        repository().dispatch(EventEnvelope.of(event));
        assertTrue(TestProjection.processed(eventMessage));
    }

    /**
     * Simulates updating TenantIndex, which occurs during command processing
     * in multi-tenant context.
     */
    private static void keepTenantIdFromEvent(BoundedContext boundedContext, Event event) {
        final TenantId tenantId = event.getContext()
                                       .getCommandContext()
                                       .getActorContext()
                                       .getTenantId();
        if (boundedContext.isMultitenant()) {
            boundedContext.getTenantIndex()
                          .keep(tenantId);
        }
    }

    private void checkDoesNotDispatchEventWith(Status status) {
        repository().setStatus(status);
        final ProjectCreated eventMsg = projectCreated();
        final Event event = createEvent(tenantId(), eventMsg, PRODUCER_ID, getCurrentTime());

        repository().dispatch(EventEnvelope.of(event));

        assertFalse(TestProjection.processed(eventMsg));
    }

    @Test(expected = RuntimeException.class)
    public void throw_exception_if_dispatch_unknown_event() {
        final StringValue unknownEventMessage = StringValue.getDefaultInstance();

        final Event event = EventTests.createContextlessEvent(unknownEventMessage);

        repository().dispatch(EventEnvelope.of(event));
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
        final TestProjectionRepository repository = new TestProjectionRepository();
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
            TenantId tenantId,
            ProjectionRepository<ProjectId, TestProjection, Project> repo,
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

        final Event event = createEvent(tenantId(), projectCreated(), PRODUCER_ID, getCurrentTime());
        repository().dispatch(EventEnvelope.of(event));

        final ProjectCreated expectedEventMessage = Events.getMessage(event);
        final EventContext context = event.getContext();
        verify(idSetFunction).apply(eq(expectedEventMessage), eq(context));
    }

    @Test
    public void obtain_id_set_function_after_put() {
        repository().addIdSetFunction(ProjectCreated.class, creteProjectTargets);

        final Optional<EventTargetsFunction<ProjectId, ProjectCreated>> func =
                repository().getIdSetFunction(ProjectCreated.class);

        assertTrue(func.isPresent());
        assertEquals(creteProjectTargets, func.get());
    }

    @Ignore //TODO:2017-06-02:alexander.yevsyukov: Enable back after separation of test suites
    @Test
    @SuppressWarnings("unchecked") // Due to mockito matcher usage
    public void perform_bulk_catch_up_if_required() {
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId("mock-project-id")
                                             .build();
        final Message eventMessage = ProjectCreated.newBuilder()
                                                   .setProjectId(projectId)
                                                   .build();
        final Event event = createEvent(tenantId(),
                                        eventMessage,
                                        pack(projectId),
                                        getCurrentTime());

        appendEvent(boundedContext, event);
        // Set up repository
        final Duration duration = seconds(10L);
        final ProjectionRepository repository = spy(
                new ManualCatchupProjectionRepository(duration));
        boundedContext.register(repository);
        repository.catchUp();

        // Check bulk write
        verify(repository).store(any(Collection.class));
        verify(repository, never()).store(any(TestProjection.class));
    }

    @Ignore //TODO:2017-06-09:alexander.yevsyukov: Move this test into a separate suite.
    @SuppressWarnings("unchecked") // Due to mockito matcher usage
    @Test
    public void skip_all_the_events_after_catch_up_outdated() throws InterruptedException {
        // Set up bounded context
        final BoundedContext boundedContext =
                TestBoundedContextFactory.MultiTenant.newBoundedContext();
        final int eventCount = 10;
        setUpEvents(tenantId(), boundedContext, eventCount);
        // Set up repository
        final Duration duration = nanos(1L);
        final ProjectionRepository repository =
                spy(new ManualCatchupProjectionRepository(duration));
        boundedContext.register(repository);
        repository.catchUp();

        // Check bulk write
        verify(repository, never()).store(any(Projection.class));
    }

    @Ignore //TODO:2017-06-09:alexander.yevsyukov: Move manual catch-up testing into a separate suite.
    @SuppressWarnings("ConstantConditions") // argument matcher always returns null
    @Test
    public void catch_up_only_with_the_freshest_events() {
        final int oldEventsCount = 7;
        final int newEventsCount = 11;
        final Timestamp lastCatchUpTime = getCurrentTime();
        final Duration delta = seconds(1);
        final Timestamp oldEventsTime = subtract(lastCatchUpTime, delta);
        final Timestamp newEventsTime = add(lastCatchUpTime, delta);
        setUpEvents(tenantId(), boundedContext, oldEventsCount, oldEventsTime);
        final Collection<ProjectId> ids = setUpEvents(
                tenantId(),
                boundedContext,
                newEventsCount,
                newEventsTime);
        final ManualCatchupProjectionRepository repo =
                spy(new ManualCatchupProjectionRepository());
        repo.setBoundedContext(boundedContext);
        repo.projectionStorage().writeLastHandledEventTime(lastCatchUpTime);

        repo.catchUp();

        verify(repo, times(newEventsCount)).find(argThat(in(ids)));
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
        final NoOpTaskNamesRepository repo = new NoOpTaskNamesRepository();
        boundedContext.register(repo);

        assertTrue(repo.loadAll()
                       .isEmpty());

        final Event event = createEvent(tenantId(),
                                        projectCreated(),
                                        PRODUCER_ID,
                                        getCurrentTime());
        repo.dispatch(EventEnvelope.of(event));

        final ImmutableCollection<ProjectionRepositoryTestEnv.NoOpTaskNamesProjection> items = repo.loadAll();
        assertTrue(items.isEmpty());
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

    @CanIgnoreReturnValue
    private static Collection<ProjectId> setUpEvents(TenantId tenantId,
                                                     BoundedContext boundedContext,
                                                     int eventCount,
                                                     Timestamp when) {
        // Set up bounded context
        final EventStore eventStore = boundedContext.getEventBus()
                                                    .getEventStore();
        final Collection<ProjectId> ids = new LinkedList<>();
        for (int i = 0; i < eventCount; i++) {
            final ProjectId projectId = ProjectId.newBuilder()
                                                 .setId(valueOf(i))
                                                 .build();
            ids.add(projectId);
            final Message eventMessage = ProjectCreated.newBuilder()
                                                       .setProjectId(projectId)
                                                       .build();
            final Event event = createEvent(tenantId, eventMessage, pack(projectId), when);
            appendEvent(boundedContext, event);
        }
        return ids;
    }

    @CanIgnoreReturnValue
    private static Collection<ProjectId> setUpEvents(TenantId tenantId,
                                                     BoundedContext boundedContext,
                                                     int eventCount) {
        return setUpEvents(tenantId, boundedContext, eventCount, getCurrentTime());
    }

    private static ArgumentMatcher<ProjectId> in(final Collection<ProjectId> expectedValues) {
        return new ArgumentMatcher<ProjectId>() {
            @Override
            public boolean matches(ProjectId argument) {
                return expectedValues.contains(argument);
            }
        };
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
}
