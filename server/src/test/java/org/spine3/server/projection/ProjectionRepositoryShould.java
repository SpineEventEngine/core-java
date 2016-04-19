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

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.server.BoundedContext;
import org.spine3.server.BoundedContextTestStubs;
import org.spine3.server.Subscribe;
import org.spine3.server.reflect.EventClass;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import static org.junit.Assert.*;
import static org.spine3.testdata.TestAggregateIdFactory.newProjectId;
import static org.spine3.testdata.TestContextFactory.createEventContext;
import static org.spine3.testdata.TestEventMessageFactory.*;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class ProjectionRepositoryShould {

    private static final ProjectId ID = newProjectId();

    private ProjectionRepository<ProjectId, TestProjection, Project> repository;

    @Before
    public void setUp() {
        final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        final BoundedContext boundedContext = BoundedContextTestStubs.create(storageFactory);
        repository = new TestProjectionRepository(boundedContext);
        repository.initStorage(storageFactory);
    }

    @Test
    public void load_empty_projection_by_default() throws InvocationTargetException {
        final TestProjection projection = repository.load(ID);
        assertEquals(Project.getDefaultInstance(), projection.getState());
    }

    @Test
    public void dispatch_event_and_load_projection() throws InvocationTargetException {
        testDispatchEvent(projectCreatedEvent(ID));
    }

    @Test
    public void dispatch_several_events() throws InvocationTargetException {
        testDispatchEvent(projectCreatedEvent(ID));
        testDispatchEvent(taskAddedEvent(ID));
        testDispatchEvent(projectStartedEvent(ID));
    }

    private void testDispatchEvent(Message eventMessage) throws InvocationTargetException {
        final Event event = Events.createEvent(eventMessage, createEventContext(ID));
        repository.dispatch(event);
        final TestProjection projection = repository.load(ID);
        assertEquals(toState(eventMessage), projection.getState());
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
        assertTrue(eventClasses.contains(EventClass.of(ProjectCreated.class)));
        assertTrue(eventClasses.contains(EventClass.of(TaskAdded.class)));
        assertTrue(eventClasses.contains(EventClass.of(ProjectStarted.class)));
    }

    @Test
    public void return_id_from_event_message() {
        final ProjectId actual = repository.getEntityId(projectCreatedEvent(ID), createEventContext(ID));
        assertEquals(ID, actual);
    }

    @Test
    public void return_entity_storage() {
        final EntityStorage<ProjectId> entityStorage = repository.entityStorage();
        assertNotNull(entityStorage);
    }

    private static Project toState(Message status) {
        final String statusStr = status.getClass().getName();
        final Project.Builder project = Project.newBuilder()
                .setProjectId(ID)
                .setStatus(statusStr);
        return project.build();
    }

    private static class TestProjection extends Projection<ProjectId, Project> {

        @SuppressWarnings("PublicConstructorInNonPublicClass")
        // Public constructor is a part of projection public API. It's called by a repository.
        public TestProjection(ProjectId id) {
            super(id);
        }

        @Subscribe
        public void on(ProjectCreated event, EventContext ignored) {
            incrementState(toState(event));
        }

        @Subscribe
        public void on(TaskAdded event, EventContext ignored) {
            incrementState(toState(event));
        }

        @Subscribe
        public void on(ProjectStarted event, EventContext ignored) {
            incrementState(toState(event));
        }
    }

    private static class TestProjectionRepository extends ProjectionRepository<ProjectId, TestProjection, Project> {

        protected TestProjectionRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }
}
