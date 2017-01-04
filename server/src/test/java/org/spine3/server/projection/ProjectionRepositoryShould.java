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

package org.spine3.server.projection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.server.BoundedContext;
import org.spine3.server.entity.AbstractEntityRepositoryShould;
import org.spine3.server.entity.EntityRepository;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.Subscribe;
import org.spine3.server.projection.ProjectionRepository.Status;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.EventClass;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.event.ProjectCreated;
import org.spine3.test.projection.event.ProjectStarted;
import org.spine3.test.projection.event.TaskAdded;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
        extends AbstractEntityRepositoryShould<ProjectionRepositoryShould.TestProjection, ProjectId, Project> {

    private static final ProjectId ID = Given.AggregateId.newProjectId();

    private BoundedContext boundedContext;

    private ProjectionRepository<ProjectId, TestProjection, Project> repository;

    @Before
    public void setUp() {
        boundedContext = newBoundedContext();
        repository = new TestProjectionRepository(boundedContext);
        repository.initStorage(InMemoryStorageFactory.getInstance());
        TestProjection.clearMessageDeliveryHistory();
    }

    /**
     * As long as {@link TestProjectionRepository#initStorage(StorageFactory)} is called in {@link #setUp()},
     * the catch-up should be automatically triggered.
     *
     * <p>The repository should become {@code ONLINE} after the catch-up.
     **/
    @Test
    public void become_online_automatically_after_init_storage() {
        assertTrue(repository.isOnline());
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
    public void load_empty_projection_by_default() {
        final TestProjection projection = repository.load(ID);
        assertEquals(Project.getDefaultInstance(), projection.getState());
    }

    @Test
    public void dispatch_event_and_load_projection() {
        checkDispatchesEvent(Given.EventMessage.projectCreated(ID));
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
        checkDispatchesEvent(Given.EventMessage.projectCreated(ID));
        checkDispatchesEvent(Given.EventMessage.taskAdded(ID));
        checkDispatchesEvent(Given.EventMessage.projectStarted(ID));
    }

    private void checkDispatchesEvent(Message eventMessage) {
        final Event event = Events.createEvent(eventMessage, createEventContext(ID));
        repository.dispatch(event);
        assertTrue(TestProjection.processed(eventMessage));
    }

    private void checkDoesNotDispatchEventWith(Status status) {
        repository.setStatus(status);
        final ProjectCreated eventMsg = Given.EventMessage.projectCreated(ID);
        final Event event = Events.createEvent(eventMsg, createEventContext(ID));

        repository.dispatch(event);

        assertFalse(TestProjection.processed(eventMsg));
    }

    @Test(expected = RuntimeException.class)
    public void throw_exception_if_dispatch_unknown_event() {
        final StringValue unknownEventMessage = StringValue.getDefaultInstance();

        final Event event = Events.createEvent(unknownEventMessage, EventContext.getDefaultInstance());

        repository.dispatch(event);
    }

    @Test
    public void return_event_classes() {
        final Set<EventClass> eventClasses = repository.getEventClasses();
        assertContainsAll(eventClasses,
                          EventClass.of(ProjectCreated.class),
                          EventClass.of(TaskAdded.class),
                          EventClass.of(ProjectStarted.class));
    }

    @Test
    public void return_id_from_event_message() {
        final ProjectId actual = repository.getProjectionId(Given.EventMessage.projectCreated(ID), createEventContext(ID));
        assertEquals(ID, actual);
    }

    @Test
    public void return_entity_storage() {
        final RecordStorage<ProjectId> recordStorage = repository.recordStorage();
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

        repository.setStatus(status);

        assertEquals(status, repository.getStatus());
    }

    @Test
    public void updates_status_to_CLOSED_on_close() throws Exception {
        repository.close();

        assertEquals(CLOSED, repository.getStatus());
    }

    @Test
    public void return_false_if_status_is_not_ONLINE() {
        repository.setStatus(CLOSED);

        assertFalse(repository.isOnline());
    }

    @Test
    public void return_true_if_explicitly_set_ONLINE() {
        repository.setStatus(CLOSED);
        repository.setOnline();
        assertTrue(repository.isOnline());
    }

    @Test
    public void catches_up_from_EventStorage() {
        ensureCatchesUpFromEventStorage(repository);
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
        final Event projectCreatedEvent = Given.Event.projectCreated(ID);
        eventStore.append(projectCreatedEvent);

        final Event taskAddedEvent = Given.Event.taskAdded(ID);
        eventStore.append(taskAddedEvent);

        final Event projectStartedEvent = Given.Event.projectStarted(ID);
        eventStore.append(projectStartedEvent);

        repo.catchUp();

        assertTrue(TestProjection.processed(Events.getMessage(projectCreatedEvent)));
        assertTrue(TestProjection.processed(Events.getMessage(taskAddedEvent)));
        assertTrue(TestProjection.processed(Events.getMessage(projectStartedEvent)));
    }

    @Override
    protected EntityRepository<ProjectId, TestProjection, Project> repository() {
        return repository;
    }

    @Override
    protected TestProjection entity() {
        final TestProjection projection = new TestProjection(ProjectId.newBuilder()
                                                                      .setId("single-test-projection")
                                                                      .build());
        return projection;
    }

    @Override
    protected List<TestProjection> entities(int count) {
        final List<TestProjection> projections = new LinkedList<>();

        for (int i = 0; i < count; i++) {
            final TestProjection projection = new TestProjection(
                    ProjectId.newBuilder()
                             .setId(String.format("test-projection-%s", i))
                             .build());

            projections.add(projection);
        }

        return projections;
    }

    private ManualCatchupProjectionRepository repoWithManualCatchup() {
        final ManualCatchupProjectionRepository repo = new ManualCatchupProjectionRepository(boundedContext);
        repo.initStorage(InMemoryStorageFactory.getInstance());
        return repo;
    }

    /** The projection stub used in tests. */
    /* package */ static class TestProjection extends Projection<ProjectId, Project> {

        /** The event message history we store for inspecting in delivery tests. */
        private static final Multimap<ProjectId, Message> eventMessagesDelivered = HashMultimap.create();

        public TestProjection(ProjectId id) {
            super(id);
        }

        private void keep(Message eventMessage) {
            eventMessagesDelivered.put(getState().getId(), eventMessage);
        }

        /* package */
        static boolean processed(Message eventMessage) {
            final boolean result = eventMessagesDelivered.containsValue(eventMessage);
            return result;
        }

        /* package */
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

    /** Stub projection repository. */
    private static class TestProjectionRepository extends ProjectionRepository<ProjectId, TestProjection, Project> {
        protected TestProjectionRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    /** Stub projection repository with the disabled automatic catch-up */
    private static class ManualCatchupProjectionRepository extends ProjectionRepository<ProjectId, TestProjection, Project> {
        protected ManualCatchupProjectionRepository(BoundedContext boundedContext) {
            super(boundedContext, false);
        }
    }
}
