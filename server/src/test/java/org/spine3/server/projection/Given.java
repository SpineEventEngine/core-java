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

import org.spine3.base.EventContext;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.command.AddTask;
import org.spine3.test.projection.command.CreateProject;
import org.spine3.test.projection.command.StartProject;
import org.spine3.test.projection.event.ProjectCreated;
import org.spine3.test.projection.event.ProjectStarted;
import org.spine3.test.projection.event.TaskAdded;

import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;


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

        public static TaskAdded taskAddedMsg(ProjectId id) {
            return TaskAdded.newBuilder().setProjectId(id).build();
        }

        public static ProjectStarted projectStartedMsg(ProjectId id) {
            return ProjectStarted.newBuilder().setProjectId(id).build();
        }

    }

    /* package */ static class Command{

        private Command() {
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
         * Creates a new {@link org.spine3.base.Event} with the given projectId.
         */
        public static org.spine3.base.Event projectCreatedEvent(ProjectId projectId) {
            return projectCreatedEvent(projectId, createEventContext(projectId));
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId and eventContext.
         */
        public static org.spine3.base.Event projectCreatedEvent(ProjectId projectId, EventContext eventContext) {

            final ProjectCreated eventMessage = EventMessage.projectCreatedMsg(projectId);

            final org.spine3.base.Event.Builder builder = org.spine3.base.Event.newBuilder()
                                                                               .setContext(eventContext)
                                                                               .setMessage(toAny(eventMessage));
            return builder.build();
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId.
         */
        public static org.spine3.base.Event taskAddedEvent(ProjectId projectId) {
            return taskAddedEvent(projectId, createEventContext(projectId));
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId and eventContext.
         */
        public static org.spine3.base.Event taskAddedEvent(ProjectId projectId, EventContext eventContext) {
            final TaskAdded event = EventMessage.taskAddedMsg(projectId);
            final org.spine3.base.Event.Builder builder = org.spine3.base.Event.newBuilder()
                                                                               .setContext(eventContext)
                                                                               .setMessage(toAny(event));
            return builder.build();
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId.
         */
        public static org.spine3.base.Event projectStartedEvent(ProjectId projectId) {
            return projectStartedEvent(projectId, createEventContext(projectId));
        }

        /**
         * Creates a new {@link org.spine3.base.Event} with the given projectId and eventContext.
         */
        public static org.spine3.base.Event projectStartedEvent(ProjectId projectId, EventContext eventContext) {
            final ProjectStarted event = EventMessage.projectStartedMsg(projectId);
            final org.spine3.base.Event.Builder builder = org.spine3.base.Event.newBuilder()
                                                                               .setContext(eventContext)
                                                                               .setMessage(toAny(event));
            return builder.build();
        }
    }

}
