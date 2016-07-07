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
import org.spine3.testdata.BoundedContextTestStubs;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.Subscribe;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.EventClass;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.event.ProjectCreated;
import org.spine3.test.projection.event.ProjectStarted;
import org.spine3.test.projection.event.TaskAdded;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import static org.junit.Assert.*;
import static org.spine3.test.Verify.assertContainsAll;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class ProjectionRepositoryShould {

    private static final ProjectId ID = Given.AggregateId.newProjectId();

    /** The projection stub used in tests. */
    private static class TestProjection extends Projection<ProjectId, Project> {

        /** The event message history we store for inspecting in delivery tests. */
        private static final Multimap<ProjectId, Message> eventMessagesDelivered = HashMultimap.create();

        public TestProjection(ProjectId id) {
            super(id);
        }

        private void keep(Message eventMessage) {
            eventMessagesDelivered.put(getState().getId(), eventMessage);
        }

        /* package */ static boolean processed(Message eventMessage) {
            final boolean result = eventMessagesDelivered.containsValue(eventMessage);
            return result;
        }

        /* package */ static void clearMessageDeliveryHistory() {
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
        public void on(ProjectStarted event, EventContext ignored) {
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

    private BoundedContext boundedContext;

    private ProjectionRepository<ProjectId, TestProjection, Project> repository;

    @Before
    public void setUp() {
        final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        boundedContext = BoundedContextTestStubs.create(storageFactory);
        repository = new TestProjectionRepository(boundedContext);
        repository.initStorage(storageFactory);
        repository.setOnline();
        TestProjection.clearMessageDeliveryHistory();
    }

    @Test
    public void load_empty_projection_by_default() throws InvocationTargetException {
        final TestProjection projection = repository.load(ID);
        assertEquals(Project.getDefaultInstance(), projection.getState());
    }

    @Test
    public void dispatch_event_and_load_projection() throws InvocationTargetException {
        testDispatchEvent(Given.EventMessage.projectCreated(ID));
    }

    @Test
    public void dispatch_several_events() throws InvocationTargetException {
        testDispatchEvent(Given.EventMessage.projectCreated(ID));
        testDispatchEvent(Given.EventMessage.taskAdded(ID));
        testDispatchEvent(Given.EventMessage.projectStarted(ID));
    }

    private void testDispatchEvent(Message eventMessage) throws InvocationTargetException {
        final Event event = Events.createEvent(eventMessage, createEventContext(ID));
        repository.dispatch(event);
        assertTrue(TestProjection.processed(eventMessage));
    }

    @Test(expected = RuntimeException.class)
    public void throw_exception_if_dispatch_unknown_event() throws InvocationTargetException {
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
        final ProjectId actual = repository.getEntityId(Given.EventMessage.projectCreated(ID), createEventContext(ID));
        assertEquals(ID, actual);
    }

    @Test
    public void return_entity_storage() {
        final EntityStorage<ProjectId> entityStorage = repository.entityStorage();
        assertNotNull(entityStorage);
    }

    @Test
    public void catches_up_from_EventStorage() {
        final EventStore eventStore = boundedContext.getEventBus()
                                                    .getEventStore();

        // Put events into the EventStore.
        final Event projectCreatedEvent = Given.Event.projectCreated(ID);
        eventStore.append(projectCreatedEvent);

        final Event taskAddedEvent = Given.Event.taskAdded(ID);
        eventStore.append(taskAddedEvent);

        final Event projectStartedEvent = Given.Event.projectStarted(ID);
        eventStore.append(projectStartedEvent);

        repository.catchUp();

        assertTrue(TestProjection.processed(Events.getMessage(projectCreatedEvent)));
        assertTrue(TestProjection.processed(Events.getMessage(taskAddedEvent)));
        assertTrue(TestProjection.processed(Events.getMessage(projectStartedEvent)));
    }


}
