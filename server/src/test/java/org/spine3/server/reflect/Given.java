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

package org.spine3.server.reflect;

import com.google.common.util.concurrent.MoreExecutors;
import org.spine3.base.EventContext;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventStore;
import org.spine3.server.storage.EventStorage;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.reflect.ProjectId;
import org.spine3.test.reflect.command.CreateProject;
import org.spine3.test.reflect.event.ProjectCreated;
import org.spine3.test.reflect.event.ProjectStarted;
import org.spine3.test.reflect.event.TaskAdded;

import static org.spine3.base.Identifiers.newUuid;

@SuppressWarnings("EmptyClass")
/* package */ class Given {

    /* package */ static class AggregateId {

        private AggregateId() {
        }

        public static ProjectId newProjectId() {
            final String uuid = newUuid();
            return ProjectId.newBuilder().setId(uuid).build();
        }

    }

    /* package */ static class EventMessage {

        private static final ProjectId DUMMY_PROJECT_ID = AggregateId.newProjectId();
        private static final ProjectCreated PROJECT_CREATED = projectCreatedMsg(DUMMY_PROJECT_ID);

        private EventMessage() {
        }

        public static ProjectCreated projectCreatedMsg() {
            return PROJECT_CREATED;
        }

        public static ProjectCreated projectCreatedMsg(ProjectId id) {
            return ProjectCreated.newBuilder().setProjectId(id).build();
        }

    }

    /* package */ static class Command{

        private Command() {
        }

        /**
         * Creates a new {@link org.spine3.test.reflect.command.CreateProject} command with the generated project ID.
         */
        public static CreateProject createProjectMsg() {
            return CreateProject.newBuilder().setProjectId(AggregateId.newProjectId()).build();
        }

        /**
         * Creates a new {@link CreateProject} command with the given project ID.
         */
        public static CreateProject createProjectMsg(ProjectId id) {
            return CreateProject.newBuilder().setProjectId(id).build();
        }

    }

    /* package */ static class Event{

        private Event() {
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

}
