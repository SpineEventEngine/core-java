/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.core.MessageEnvelope;
import io.spine.core.Subscribe;
import io.spine.server.entity.TestEntityWithStringColumn;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectTaskNames;
import io.spine.test.projection.ProjectTaskNamesVBuilder;
import io.spine.test.projection.ProjectVBuilder;
import io.spine.test.projection.event.PrjProjectArchived;
import io.spine.test.projection.event.PrjProjectCreated;
import io.spine.test.projection.event.PrjProjectDeleted;
import io.spine.test.projection.event.PrjProjectStarted;
import io.spine.test.projection.event.PrjTaskAdded;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

/**
 * @author Alexander Yevsyukov
 * @author Alexander Aleksandrov
 * @author Dmytro Grankin
 * @author Alex Tymchenko
 */
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
    public static class NoOpTaskNamesProjection
            extends Projection<ProjectId, ProjectTaskNames, ProjectTaskNamesVBuilder> {

        public NoOpTaskNamesProjection(ProjectId id) {
            super(id);
        }

        @Subscribe
        void on(PrjProjectCreated event) {
            // do nothing.
        }

        @Subscribe
        void on(PrjTaskAdded event) {
            // do nothing
        }
    }

    /** Stub projection repository. */
    public static class TestProjectionRepository
            extends ProjectionRepository<ProjectId, TestProjection, Project> {

        private @Nullable MessageEnvelope lastErrorEnvelope;
        private @Nullable RuntimeException lastException;

        @Subscribe
        void apply(PrjProjectCreated event, EventContext eventContext) {
            // NOP
        }

        @Override
        protected void logError(String msgFormat,
                                MessageEnvelope envelope,
                                RuntimeException exception) {
            super.logError(msgFormat, envelope, exception);
            lastErrorEnvelope = envelope;
            lastException = exception;
        }

        public @Nullable MessageEnvelope getLastErrorEnvelope() {
            return lastErrorEnvelope;
        }

        public @Nullable RuntimeException getLastException() {
            return lastException;
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
            boolean result = eventMessagesDelivered.containsValue(eventMessage);
            return result;
        }

        /**
         * Returns the IDs of projection instances, which processed the given message.
         *
         * <p>Empty set is returned if no instance processed the given message.
         */
        public static Set<ProjectId> whoProcessed(Message eventMessage) {
            ImmutableSet.Builder<ProjectId> builder = ImmutableSet.builder();
            for (ProjectId projectId : eventMessagesDelivered.keySet()) {
                if(eventMessagesDelivered.get(projectId)
                                         .contains(eventMessage)) {
                    builder.add(projectId);
                }
            }
            return builder.build();
        }

        public static void clearMessageDeliveryHistory() {
            eventMessagesDelivered.clear();
        }

        private void keep(Message eventMessage) {
            eventMessagesDelivered.put(getId(), eventMessage);
        }

        @Subscribe
        void on(PrjProjectCreated event) {
            // Keep the event message for further inspection in tests.
            keep(event);

            Project newState = getState().toBuilder()
                                         .setId(event.getProjectId())
                                         .setStatus(Project.Status.CREATED)
                                         .build();
            getBuilder().mergeFrom(newState);
        }

        @Subscribe
        void on(PrjTaskAdded event) {
            keep(event);
            Project newState = getState().toBuilder()
                                         .addTask(event.getTask())
                                         .build();
            getBuilder().mergeFrom(newState);
        }

        /**
         * Handles the {@link PrjProjectStarted} event.
         *
         * @param event   the event message
         * @param ignored this parameter is left to show that a projection subscriber
         *                can have two parameters
         */
        @Subscribe
        void on(PrjProjectStarted event,
                       @SuppressWarnings("UnusedParameters") EventContext ignored) {
            keep(event);
            Project newState = getState().toBuilder()
                                         .setStatus(Project.Status.STARTED)
                                         .build();
            getBuilder().mergeFrom(newState);
        }

        @Subscribe
        void on(PrjProjectArchived event) {
            keep(event);
            setArchived(true);
        }

        @Subscribe
        void on(PrjProjectDeleted event) {
            keep(event);
            setDeleted(true);
        }

        @Override
        public String getIdString() {
            return getId().toString();
        }
    }

    public static class GivenEventMessage {

        public static final ProjectId ENTITY_ID = ProjectId.newBuilder()
                                                           .setId("p-123")
                                                           .build();

        private GivenEventMessage() {
            // Prevent instantiation of this utility class.
        }

        public static PrjProjectStarted projectStarted() {
            return PrjProjectStarted.newBuilder()
                                    .setProjectId(ENTITY_ID)
                                    .build();
        }

        public static PrjProjectCreated projectCreated() {
            return PrjProjectCreated.newBuilder()
                                    .setProjectId(ENTITY_ID)
                                    .build();
        }

        public static PrjTaskAdded taskAdded() {
            return PrjTaskAdded.newBuilder()
                               .setProjectId(ENTITY_ID)
                               .build();
        }

        public static PrjProjectArchived projectArchived() {
            return PrjProjectArchived.newBuilder()
                                     .setProjectId(ENTITY_ID)
                                     .build();
        }

        public static PrjProjectDeleted projectDeleted() {
            return PrjProjectDeleted.newBuilder()
                                    .setProjectId(ENTITY_ID)
                                    .build();
        }
    }

    /**
     * A projection, that handles no messages.
     *
     * <p>It should not be able to register repositories for such classes.
     */
    public static class SensoryDeprivedProjection
            extends Projection<ProjectId, Project, ProjectVBuilder> {

        protected SensoryDeprivedProjection(ProjectId id) {
            super(id);
        }
    }

    /**
     * A repository, that cannot be registered in {@code BoundedContext},
     * since no messages are declared to handle by the {@linkplain SensoryDeprivedProjection
     * projection class}.
     */
    public static class SensoryDeprivedProjectionRepository
            extends ProjectionRepository<ProjectId, SensoryDeprivedProjection, Project> {
    }
}
