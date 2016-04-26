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

package org.spine3.testdata;

import com.google.common.util.concurrent.MoreExecutors;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventStore;
import org.spine3.server.storage.EventStorage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;

import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testdata.TestAggregateIdFactory.newProjectId;
import static org.spine3.testdata.TestContextFactory.createEventContext;
import static org.spine3.testdata.TestEventMessageFactory.*;

/**
 * The utility class which is used for creating Events for tests.
 *
 * @author Mikhail Mikhaylov
 */
public class TestEventFactory {

    private static final ProjectId STUB_PROJECT_ID = newProjectId();
    private static final EventContext STUB_EVENT_CONTEXT = createEventContext();


    private TestEventFactory() {}


    /**
     * Creates a new {@link Event} with default properties.
     */
    public static Event projectCreatedEvent() {
        return projectCreatedEvent(STUB_PROJECT_ID, STUB_EVENT_CONTEXT);
    }

    /**
     * Creates a new {@link Event} with the given projectId.
     */
    public static Event projectCreatedEvent(ProjectId projectId) {
        return projectCreatedEvent(projectId, createEventContext(projectId));
    }

    /**
     * Creates a new {@link Event} with the given projectId and eventContext.
     */
    public static Event projectCreatedEvent(ProjectId projectId, EventContext eventContext) {

        final ProjectCreated event = projectCreatedMsg(projectId);
        final Event.Builder builder = Event.newBuilder().setContext(eventContext).setMessage(toAny(event));
        return builder.build();
    }

    /**
     * Creates a new {@link Event} with default properties.
     */
    public static Event taskAddedEvent() {
        return taskAddedEvent(STUB_PROJECT_ID, STUB_EVENT_CONTEXT);
    }

    /**
     * Creates a new {@link Event} with the given projectId and eventContext.
     */
    public static Event taskAddedEvent(ProjectId projectId, EventContext eventContext) {

        final TaskAdded event = taskAddedMsg(projectId);
        final Event.Builder builder = Event.newBuilder().setContext(eventContext).setMessage(toAny(event));
        return builder.build();
    }

    /**
     * Creates a new {@link Event} with the given projectId.
     */
    public static Event taskAddedEvent(ProjectId projectId) {
        return taskAddedEvent(projectId, createEventContext(projectId));
    }

    /**
     * Creates a new {@link Event} with default properties.
     */
    public static Event projectStartedEvent() {
        return projectStartedEvent(STUB_PROJECT_ID, STUB_EVENT_CONTEXT);
    }

    /**
     * Creates a new {@link Event} with the given projectId.
     */
    public static Event projectStartedEvent(ProjectId projectId) {
        return projectStartedEvent(projectId, createEventContext(projectId));
    }

    /**
     * Creates a new {@link Event} with the given projectId and eventContext.
     */
    public static Event projectStartedEvent(ProjectId projectId, EventContext eventContext) {

        final ProjectStarted event = projectStartedMsg(projectId);
        final Event.Builder builder = Event.newBuilder().setContext(eventContext).setMessage(toAny(event));
        return builder.build();
    }

    /**
     * Creates a new event bus with the given storage factory.
     */
    public static EventBus newEventBus(StorageFactory storageFactory) {
        final EventStore store = EventStore.newBuilder()
                                           .setStreamExecutor(MoreExecutors.directExecutor())
                                           .setStorage(storageFactory.createEventStorage())
                                           .build();
        final EventBus eventBus = EventBus.newInstance(store);
        return eventBus;
    }

    /**
     * Creates a new event bus with the {@link InMemoryStorageFactory}.
     */
    public static EventBus newEventBus() {
        final EventStorage storage = InMemoryStorageFactory.getInstance().createEventStorage();
        final EventStore store = EventStore.newBuilder()
                                           .setStreamExecutor(MoreExecutors.directExecutor())
                                           .setStorage(storage)
                                           .build();
        final EventBus eventBus = EventBus.newInstance(store);
        return eventBus;
    }
}
