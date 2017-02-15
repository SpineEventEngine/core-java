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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Durations;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.BoundedContext;
import org.spine3.server.entity.EntityWithStatusRepository;
import org.spine3.server.entity.EntityWithStatusRepositoryShould;
import org.spine3.server.entity.idfunc.IdSetEventFunction;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.Subscribe;
import org.spine3.server.projection.ProjectionRepository.Status;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.EventClass;
import org.spine3.test.Given;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.event.ProjectCreated;
import org.spine3.test.projection.event.ProjectStarted;
import org.spine3.test.projection.event.TaskAdded;
import org.spine3.testdata.Sample;
import org.spine3.testdata.TestBoundedContextFactory;
import org.spine3.testdata.TestEventBusFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.spine3.server.projection.ProjectionRepository.Status.CATCHING_UP;
import static org.spine3.server.projection.ProjectionRepository.Status.CLOSED;
import static org.spine3.server.projection.ProjectionRepository.Status.CREATED;
import static org.spine3.server.projection.ProjectionRepository.Status.ONLINE;
import static org.spine3.server.projection.ProjectionRepository.Status.STORAGE_ASSIGNED;
import static org.spine3.test.Verify.assertContainsAll;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public class ProjectionRepositoryShould
        extends EntityWithStatusRepositoryShould<ProjectionRepositoryShould.TestProjection, ProjectId, Project> {

    private static final ProjectId ID = Sample.messageOfType(ProjectId.class);

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
    protected EntityWithStatusRepository<ProjectId, TestProjection, Project> createRepository() {
        boundedContext = newBoundedContext();
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
                        .setId(String.format("test-projection-%s", i))
                        .build();
    }

    @Before
    public void setUp() {
        initRepository();
        repository.initStorage(InMemoryStorageFactory.getInstance());
        TestProjection.clearMessageDeliveryHistory();
    }

    // Tests
    //-------------------------

    /**
     * As long as {@link TestProjectionRepository#initStorage(StorageFactory)} is called in {@link #setUp()},
     * the catch-up should be automatically triggered.
     *
     * <p>The repository should become {@code ONLINE} after the catch-up.
     **/
    @Test
    public void become_online_automatically_after_init_storage() {
        assertTrue(repository().isOnline());
    }

    /**
     * As long as {@code ManualCatchupProjectionRepository} has automatic catch-up disabled, it does not become online
     * automatically after {@link ManualCatchupProjectionRepository#initStorage(StorageFactory)} is called.
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
        final Event event = Events.createEvent(eventMessage, createEventContext(ID));
        repository().dispatch(event);
        assertTrue(TestProjection.processed(eventMessage));
    }

    private void checkDoesNotDispatchEventWith(Status status) {
        repository().setStatus(status);
        final ProjectCreated eventMsg = projectCreated();
        final Event event = Events.createEvent(eventMsg, createEventContext(ID));

        repository().dispatch(event);

        assertFalse(TestProjection.processed(eventMsg));
    }

    @Test(expected = RuntimeException.class)
    public void throw_exception_if_dispatch_unknown_event() {
        final StringValue unknownEventMessage = StringValue.getDefaultInstance();

        final Event event = Events.createEvent(unknownEventMessage, EventContext.getDefaultInstance());

        repository().dispatch(event);
    }

    @Test
    public void return_event_classes() {
        final Set<EventClass> eventClasses = repository().getEventClasses();
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
        final TestProjectionRepository repository = new TestProjectionRepository(newBoundedContext());

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

    @Test
    public void catches_up_from_EventStorage_even_if_automatic_catchup_disabled() {
        final ManualCatchupProjectionRepository repo = repoWithManualCatchup();
        repo.setOnline();

        ensureCatchesUpFromEventStorage(repo);
    }

    private void ensureCatchesUpFromEventStorage(ProjectionRepository<ProjectId, TestProjection, Project> repo) {
        final EventStore eventStore = boundedContext.getEventBus()
                                                    .getEventStore();

        // Put events into the EventStore.
        final Event projectCreatedEvent = Sample.eventBy(ID, ProjectCreated.class);
        eventStore.append(projectCreatedEvent);

        final Event taskAddedEvent = Sample.eventBy(ID, TaskAdded.class);
        eventStore.append(taskAddedEvent);

        final Event projectStartedEvent = Sample.eventBy(ID, ProjectStarted.class);
        eventStore.append(projectStartedEvent);

        repo.catchUp();

        assertTrue(TestProjection.processed(Events.getMessage(projectCreatedEvent)));
        assertTrue(TestProjection.processed(Events.getMessage(taskAddedEvent)));
        assertTrue(TestProjection.processed(Events.getMessage(projectStartedEvent)));
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

        final Event event = Sample.eventBy(ID, projectCreated());
        repository().dispatch(event);

        final ProjectCreated expectedEventMessage = Events.getMessage(event);
        final EventContext context = event.getContext();
        verify(idSetFunction).apply(eq(expectedEventMessage), eq(context));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // because the test checks that the function is present.
    @Test
    public void obtain_id_set_function_after_put() {
        repository().addIdSetFunction(ProjectCreated.class, idSetForCreateProject);

        final Optional<IdSetEventFunction<ProjectId, ProjectCreated>> func =
                repository().getIdSetFunction(ProjectCreated.class);

        assertTrue(func.isPresent());
        assertEquals(idSetForCreateProject, func.get());
    }

    @SuppressWarnings("unchecked") // Due to mockito matcher usage
    @Test
    public void perform_bulk_catch_up_if_required() {
        // Set up bounded context
        final BoundedContext boundedContext = TestBoundedContextFactory.newBoundedContext(
                TestEventBusFactory.create());
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId("mock-project-id")
                                             .build();
        final Message eventMessage = ProjectCreated.newBuilder()
                                                   .setProjectId(projectId)
                                                   .build();
        final EventContext context = EventContext.newBuilder()
                                                 .setEventId(EventId.newBuilder()
                                                                    .setUuid("mock-event"))
                                                 .setProducerId(AnyPacker.pack(projectId))
                                                 .setTimestamp(Timestamps.getCurrentTime())
                                                 .build();
        final Event event = Events.createEvent(eventMessage, context);
        boundedContext.getEventBus()
                      .getEventStore()
                      .append(event);
        // Set up repository
        final Duration duration = Durations.seconds(10L);
        final ProjectionRepository repository = spy(new ManualCatchupProjectionRepository(boundedContext, duration));
        repository.initStorage(InMemoryStorageFactory.getInstance());
        repository.catchUp();

        // Check bulk write
        verify(repository).store(any(Collection.class));
        verify(repository, never()).store(any(TestProjection.class));
    }

    @SuppressWarnings("unchecked") // Due to mockito matcher usage
    @Test
    public void skip_all_the_events_after_catch_up_outdated() throws InterruptedException {
        // Set up bounded context
        final BoundedContext boundedContext = TestBoundedContextFactory.newBoundedContext(
                TestEventBusFactory.create());
        final int eventsCount = 10;
        final EventStore eventStore = boundedContext.getEventBus()
                                                    .getEventStore();
        for (int i = 0; i < eventsCount; i++) {
            final ProjectId projectId = ProjectId.newBuilder()
                                                 .setId(String.valueOf(i))
                                                 .build();
            final Message eventMessage = ProjectCreated.newBuilder()
                                                       .setProjectId(projectId)
                                                       .build();
            final EventContext context = EventContext.newBuilder()
                                                     .setEventId(EventId.newBuilder()
                                                                        .setUuid(String.valueOf(i)))
                                                     .setProducerId(AnyPacker.pack(projectId))
                                                     .setTimestamp(Timestamps.getCurrentTime())
                                                     .build();
            final Event event = Events.createEvent(eventMessage, context);
            eventStore.append(event);
        }
        // Set up repository
        final Duration duration = Durations.nanos(1L);
        final ProjectionRepository repository = spy(new ManualCatchupProjectionRepository(boundedContext, duration));
        repository.initStorage(InMemoryStorageFactory.getInstance());
        repository.catchUp();

        // Check bulk write
        verify(repository, never()).store(any(Projection.class));
    }

    @Test
    public void remove_id_set_function_after_put() {
        repository().addIdSetFunction(ProjectCreated.class, idSetForCreateProject);

        repository().removeIdSetFunction(ProjectCreated.class);
        final Optional<IdSetEventFunction<ProjectId, ProjectCreated>> out = repository().getIdSetFunction(ProjectCreated.class);

        assertFalse(out.isPresent());
    }

    private ManualCatchupProjectionRepository repoWithManualCatchup() {
        final ManualCatchupProjectionRepository repo = new ManualCatchupProjectionRepository(boundedContext);
        repo.initStorage(InMemoryStorageFactory.getInstance());
        return repo;
    }

    /** The projection stub used in tests. */
    static class TestProjection extends Projection<ProjectId, Project> {

        /** The event message history we store for inspecting in delivery tests. */
        private static final Multimap<ProjectId, Message> eventMessagesDelivered = HashMultimap.create();

        public TestProjection(ProjectId id) {
            super(id);
        }

        private void keep(Message eventMessage) {
            eventMessagesDelivered.put(getState().getId(), eventMessage);
        }

        static boolean processed(Message eventMessage) {
            final boolean result = eventMessagesDelivered.containsValue(eventMessage);
            return result;
        }

        static void clearMessageDeliveryHistory() {
            eventMessagesDelivered.clear();
        }

        @Subscribe
        public void on(ProjectCreated event) {
            // Keep the event message for further inspection in tests.
            keep(event);

            final Project newState = getState().toBuilder()
                                               .setId(event.getProjectId())
                                               .setStatus(Project.Status.CREATED)
                                               .build();
            incrementState(newState);
        }

        @Subscribe
        public void on(TaskAdded event) {
            keep(event);
            final Project newState = getState().toBuilder()
                                               .addTask(event.getTask())
                                               .build();
            incrementState(newState);
        }

        /* EventContext parameter left to show that a projection subscriber can have two parameters. */
        @Subscribe
        public void on(ProjectStarted event, @SuppressWarnings("UnusedParameters") EventContext ignored) {
            keep(event);
            final Project newState = getState().toBuilder()
                                               .setStatus(Project.Status.STARTED)
                                               .build();
            incrementState(newState);
        }
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

    /** Stub projection repository. */
    private static class TestProjectionRepository extends ProjectionRepository<ProjectId, TestProjection, Project> {
        protected TestProjectionRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }

        protected TestProjectionRepository(BoundedContext boundedContext, Duration catchUpMaxDuration) {
            super(boundedContext, true, catchUpMaxDuration);
        }

        @Subscribe
        public void apply(ProjectCreated event, EventContext eventContext) {
            // NOP
        }
    }

    /** Stub projection repository with the disabled automatic catch-up */
    private static class ManualCatchupProjectionRepository extends ProjectionRepository<ProjectId, TestProjection, Project> {
        protected ManualCatchupProjectionRepository(BoundedContext boundedContext) {
            super(boundedContext, false);
        }

        protected ManualCatchupProjectionRepository(BoundedContext boundedContext, Duration catchUpMaxDuration) {
            super(boundedContext, false, catchUpMaxDuration);
        }

        @Subscribe
        public void apply(ProjectCreated event, EventContext eventContext) {
            // NOP
        }
    }
}
