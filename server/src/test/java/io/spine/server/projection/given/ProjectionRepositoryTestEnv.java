/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import io.spine.base.Identifier;
import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.core.UserId;
import io.spine.server.organizations.OrganizationEstablished;
import io.spine.server.organizations.OrganizationId;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.type.MessageEnvelope;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectTaskNames;
import io.spine.test.projection.ProjectTaskNamesVBuilder;
import io.spine.test.projection.ProjectVBuilder;
import io.spine.test.projection.Task;
import io.spine.test.projection.event.PrjProjectArchived;
import io.spine.test.projection.event.PrjProjectCreated;
import io.spine.test.projection.event.PrjProjectDeleted;
import io.spine.test.projection.event.PrjProjectStarted;
import io.spine.test.projection.event.PrjTaskAdded;
import io.spine.testing.core.given.GivenUserId;
import org.checkerframework.checker.nullness.qual.Nullable;

import static io.spine.testing.TestValues.randomString;

public class ProjectionRepositoryTestEnv {

    /** Prevent instantiation of this utility class. */
    private ProjectionRepositoryTestEnv() {
    }

    /**
     * The projection stub with the event subscribing methods that do nothing.
     *
     * <p>Such a projection allows to reproduce a use case, when the event-handling method
     * does not modify the state of an {@code Entity}. For the newly created entities it could lead
     * to an invalid entry created in the storage.
     */
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
            extends TestProjection.Repository {

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
                                    .setName("Projection test " + randomString())
                                    .setProjectId(ENTITY_ID)
                                    .build();
        }

        public static PrjTaskAdded taskAdded() {
            Task task = Task
                    .newBuilder()
                    .setTitle("Test task " + randomString())
                    .build();
            return PrjTaskAdded.newBuilder()
                               .setProjectId(ENTITY_ID)
                               .setTask(task)
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

        public static OrganizationEstablished organizationEstablished() {
            OrganizationId id = Identifier.generate(OrganizationId.class);
            UserId head = GivenUserId.generated();
            return OrganizationEstablished
                    .newBuilder()
                    .setId(id)
                    .setHead(head)
                    .setName("Share holders")
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
