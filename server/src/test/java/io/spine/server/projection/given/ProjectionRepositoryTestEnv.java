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

package io.spine.server.projection.given;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import io.spine.base.EventContext;
import io.spine.base.Subscribe;
import io.spine.server.entity.TestEntityWithStringColumn;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectTaskNames;
import io.spine.test.projection.ProjectTaskNamesVBuilder;
import io.spine.test.projection.ProjectVBuilder;
import io.spine.test.projection.event.ProjectCreated;
import io.spine.test.projection.event.ProjectStarted;
import io.spine.test.projection.event.TaskAdded;

public class ProjectionRepositoryTestEnv {

    private ProjectionRepositoryTestEnv() {
        // Prevent instantiation of this utility class.
    }

    /**
     * The projection stub with the event subscribing methods that do nothing.
     *
     * <p>Such a projection allows to reproduce a use case, when the event-handling method
     * does not modify the state of an {@code Entity}. For the newly created entities it could lead
     * to an invalid entry created in the storage.
     */
    @SuppressWarnings("unused")
    public static class NoOpTaskNamesProjection extends Projection<ProjectId,
            ProjectTaskNames,
            ProjectTaskNamesVBuilder> {

        public NoOpTaskNamesProjection(ProjectId id) {
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

    /** Stub projection repository. */
    public static class TestProjectionRepository
            extends ProjectionRepository<ProjectId, TestProjection, Project> {

        @Subscribe
        public void apply(ProjectCreated event, EventContext eventContext) {
            // NOP
        }
    }

    /** Stub projection repository. */
    public static class NoOpTaskNamesRepository
            extends ProjectionRepository<ProjectId, NoOpTaskNamesProjection, ProjectTaskNames> {
        public NoOpTaskNamesRepository() {
            super();
        }
    }

    /** The projection stub used in tests. */
    public static class TestProjection
            extends Projection<ProjectId, Project, ProjectVBuilder>
            implements TestEntityWithStringColumn {

        /** The event message history we store for inspecting in delivery tests. */
        private static final Multimap<ProjectId, Message> eventMessagesDelivered =
                HashMultimap.create();

        public TestProjection(ProjectId id) {
            super(id);
        }

        public static boolean processed(Message eventMessage) {
            final boolean result = eventMessagesDelivered.containsValue(eventMessage);
            return result;
        }

        public static void clearMessageDeliveryHistory() {
            eventMessagesDelivered.clear();
        }

        private void keep(Message eventMessage) {
            eventMessagesDelivered.put(getState().getId(), eventMessage);
        }

        @Subscribe
        public void on(ProjectCreated event) {
            // Keep the event message for further inspection in tests.
            keep(event);

            final Project newState = getState().toBuilder()
                                               .setId(event.getProjectId())
                                               .setStatus(Project.Status.CREATED)
                                               .build();
            getBuilder().mergeFrom(newState);
        }

        @Subscribe
        public void on(TaskAdded event) {
            keep(event);
            final Project newState = getState().toBuilder()
                                               .addTask(event.getTask())
                                               .build();
            getBuilder().mergeFrom(newState);
        }

        /**
         * Handles the {@link ProjectStarted} event.
         *
         * @param event   the event message
         * @param ignored this parameter is left to show that a projection subscriber
         *                can have two parameters
         */
        @Subscribe
        public void on(ProjectStarted event,
                       @SuppressWarnings("UnusedParameters") EventContext ignored) {
            keep(event);
            final Project newState = getState().toBuilder()
                                               .setStatus(Project.Status.STARTED)
                                               .build();
            getBuilder().mergeFrom(newState);
        }

        @Override
        public String getIdString() {
            return getId().toString();
        }
    }
}
