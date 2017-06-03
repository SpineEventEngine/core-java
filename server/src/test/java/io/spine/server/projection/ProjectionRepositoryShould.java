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
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.Event;
import io.spine.base.EventContext;
import io.spine.base.Events;
import io.spine.envelope.EventEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.RecordBasedRepositoryShould;
import io.spine.server.entity.idfunc.IdSetEventFunction;
import io.spine.server.event.EventStore;
import io.spine.server.projection.ProjectionRepository.Status;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.ManualCatchupProjectionRepository;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.NoOpTaskNamesRepository;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.TestProjection;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.TestProjectionRepository;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.test.EventTests;
import io.spine.test.Given;
import io.spine.test.TestActorRequestFactory;
import io.spine.test.TestEventFactory;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.event.ProjectCreated;
import io.spine.test.projection.event.ProjectStarted;
import io.spine.test.projection.event.TaskAdded;
import io.spine.testdata.TestBoundedContextFactory;
import io.spine.time.Durations2;
import io.spine.time.Time;
import io.spine.type.EventClass;
import io.spine.users.TenantId;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.projection.ProjectionRepository.Status.CATCHING_UP;
import static io.spine.server.projection.ProjectionRepository.Status.CLOSED;
import static io.spine.server.projection.ProjectionRepository.Status.CREATED;
import static io.spine.server.projection.ProjectionRepository.Status.ONLINE;
import static io.spine.server.projection.ProjectionRepository.Status.STORAGE_ASSIGNED;
import static io.spine.test.Verify.assertContainsAll;
import static io.spine.testdata.TestBoundedContextFactory.MultiTenant.newBoundedContext;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public class ProjectionRepositoryShould
        extends RecordBasedRepositoryShould<TestProjection,
                                            ProjectId,
                                            Project> {

    private static final ProjectId ID = ProjectId.newBuilder()
                                                 .setId("p-123")
                                                 .build();
    private static final Any PRODUCER_ID = pack(ID);

    private BoundedContext boundedContext;

    private ProjectionRepository<ProjectId, TestProjection, Project> repository() {
        return (ProjectionRepository<ProjectId, TestProjection, Project>) repository;
    }

    /**
     * {@code IdSetFunction} used for testing add/get/remove.
     */
    private static final IdSetEventFunction<ProjectId, ProjectCreated> idSetForCreateProject =
            new IdSetEventFunction<ProjectId, ProjectCreated>() {
                @Override
                public Set<ProjectId> apply(ProjectCreated message, EventContext context) {
                    return newHashSet();
                }
            };

    @Override
    protected RecordBasedRepository<ProjectId, TestProjection, Project> createRepository() {
        return new TestProjectionRepository();
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

    @Override
    @Before
    public void setUp() {
        boundedContext = newBoundedContext();
        super.setUp();
        boundedContext.register(repository());
        TestProjection.clearMessageDeliveryHistory();
    }

    private TestEventFactory newEventFactory(Any producerId) {
        final TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(getClass(), tenantId());
        return TestEventFactory.newInstance(producerId, requestFactory);
    }

    private Event createEvent(Any producerId, Message eventMessage) {
        return newEventFactory(producerId).createEvent(eventMessage);
    }

    private void appendEvent(EventStore eventStore, Event event) {
        eventStore.append(event);
        keepTenantIdFromEvent(event);
    }

    // Tests
    //-------------------------

    /**
     * As long as {@link TestProjectionRepository#initStorage(StorageFactory)} is called in
     * {@link #setUp()}, the catch-up should be automatically triggered.
     *
     * <p>The repository should become {@code ONLINE} after the catch-up.
     **/
    @Test
    public void become_online_automatically_after_init_storage() {
        assertTrue(repository().isOnline());
    }

    @Ignore //TODO:2017-06-02:alexander.yevsyukov: Enable back after separation of test suites
    /**
     * As long as {@code ManualCatchupProjectionRepository} has automatic catch-up disabled,
     * it does not become online automatically after
     * {@link ManualCatchupProjectionRepository#initStorage(StorageFactory)} is called.
     **/
    @Test
    public void not_become_online_automatically_after_init_storage_if_auto_catch_up_disabled() {
        final ManualCatchupProjectionRepository repo = repoWithManualCatchup();
        assertEquals(STORAGE_ASSIGNED, repo.getStatus());
        assertFalse(repo.isOnline());
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
        final TestEventFactory eventFactory = newEventFactory(PRODUCER_ID);
        final Event event = eventFactory.createEvent(eventMessage);

        keepTenantIdFromEvent(event);

        repository().dispatch(EventEnvelope.of(event));
        assertTrue(TestProjection.processed(eventMessage));
    }

    /**
     * Simulates updating TenantIndex, which occurs during command processing
     * in multi-tenant context.
     */
    private void keepTenantIdFromEvent(Event event) {
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
        final Event event = createEvent(PRODUCER_ID, eventMsg);

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
        ensureCatchesUpFromEventStorage(repository());
    }

    @Ignore //TODO:2017-06-02:alexander.yevsyukov: Enable back after separation of test suites
    @Test
    public void catches_up_from_EventStorage_even_if_automatic_catchup_disabled() {
        final ManualCatchupProjectionRepository repo = repoWithManualCatchup();
        boundedContext.register(repo);
        repo.setOnline();

        ensureCatchesUpFromEventStorage(repo);
    }

    private void ensureCatchesUpFromEventStorage(
            ProjectionRepository<ProjectId, TestProjection, Project> repo) {
        final EventStore eventStore = boundedContext.getEventBus()
                                                    .getEventStore();

        final TestEventFactory eventFactory = newEventFactory(PRODUCER_ID);

        // Put events into the EventStore.
        final ProjectCreated projectCreated = ProjectCreated.newBuilder()
                                                            .setProjectId(ID)
                                                            .build();
        final Event projectCreatedEvent = eventFactory.createEvent(projectCreated);
        appendEvent(eventStore, projectCreatedEvent);

        final TaskAdded taskAdded = TaskAdded.newBuilder()
                                             .setProjectId(ID)
                                             .build();
        final Event taskAddedEvent = eventFactory.createEvent(taskAdded);
        appendEvent(eventStore, taskAddedEvent);

        final ProjectStarted projectStarted = ProjectStarted.newBuilder()
                                                            .setProjectId(ID)
                                                            .build();
        final Event projectStartedEvent = eventFactory.createEvent(projectStarted);
        appendEvent(eventStore, projectStartedEvent);

        repo.catchUp();

        assertTrue(TestProjection.processed(projectCreated));
        assertTrue(TestProjection.processed(taskAdded));
        assertTrue(TestProjection.processed(projectStarted));
    }

    @Test
    public void use_id_set_function() {
        final IdSetEventFunction<ProjectId, ProjectCreated> delegateFn =
                new IdSetEventFunction<ProjectId, ProjectCreated>() {
                    @Override
                    public Set<ProjectId> apply(ProjectCreated message, EventContext context) {
                        return newHashSet();
                    }
                };

        final IdSetEventFunction<ProjectId, ProjectCreated> idSetFunction = spy(delegateFn);
        repository().addIdSetFunction(ProjectCreated.class, idSetFunction);

        final Event event = createEvent(PRODUCER_ID, projectCreated());
        repository().dispatch(EventEnvelope.of(event));

        final ProjectCreated expectedEventMessage = Events.getMessage(event);
        final EventContext context = event.getContext();
        verify(idSetFunction).apply(eq(expectedEventMessage), eq(context));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
        // because the test checks that the function is present.
    @Test
    public void obtain_id_set_function_after_put() {
        repository().addIdSetFunction(ProjectCreated.class, idSetForCreateProject);

        final Optional<IdSetEventFunction<ProjectId, ProjectCreated>> func =
                repository().getIdSetFunction(ProjectCreated.class);

        assertTrue(func.isPresent());
        assertEquals(idSetForCreateProject, func.get());
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
        final Event event = createEvent(pack(projectId), eventMessage);

        appendEvent(boundedContext.getEventBus()
                                  .getEventStore(), event);
        // Set up repository
        final Duration duration = Durations2.seconds(10L);
        final ProjectionRepository repository = spy(
                new ManualCatchupProjectionRepository(duration));
        boundedContext.register(repository);
        repository.catchUp();

        // Check bulk write
        verify(repository).store(any(Collection.class));
        verify(repository, never()).store(any(TestProjection.class));
    }

    @SuppressWarnings("unchecked") // Due to mockito matcher usage
    @Test
    public void skip_all_the_events_after_catch_up_outdated() throws InterruptedException {
        // Set up bounded context
        final BoundedContext boundedContext =
                TestBoundedContextFactory.MultiTenant.newBoundedContext();
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
                spy(new ManualCatchupProjectionRepository(duration));
        boundedContext.register(repository);
        repository.catchUp();

        // Check bulk write
        verify(repository, never()).store(any(Projection.class));
    }

    @Test
    public void remove_id_set_function_after_put() {
        repository().addIdSetFunction(ProjectCreated.class, idSetForCreateProject);

        repository().removeIdSetFunction(ProjectCreated.class);
        final Optional<IdSetEventFunction<ProjectId, ProjectCreated>> out =
                repository().getIdSetFunction(ProjectCreated.class);

        assertFalse(out.isPresent());
    }

    @Test
    public void convert_null_timestamp_to_default() {
        final Timestamp timestamp = Time.getCurrentTime();
        assertEquals(timestamp, ProjectionRepository.nullToDefault(timestamp));
        assertEquals(Timestamp.getDefaultInstance(), ProjectionRepository.nullToDefault(null));
    }

    @Test
    public void do_not_create_record_if_entity_isnt_updated() {
        final NoOpTaskNamesRepository repo = new NoOpTaskNamesRepository();
        boundedContext.register(repo);

        assertTrue(repo.loadAll().isEmpty());

        final Event event = createEvent(PRODUCER_ID, projectCreated());
        repo.dispatch(EventEnvelope.of(event));

        final ImmutableCollection<ProjectionRepositoryTestEnv.NoOpTaskNamesProjection> items = repo.loadAll();
        assertTrue(items.isEmpty());
    }

    private static ManualCatchupProjectionRepository repoWithManualCatchup() {
        final ManualCatchupProjectionRepository repo =
                new ManualCatchupProjectionRepository();
        return repo;
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
