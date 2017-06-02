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

package org.spine3.server.aggregate.given;

import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.AggregatePart;
import org.spine3.server.aggregate.AggregatePartRepository;
import org.spine3.server.aggregate.AggregateRoot;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;
import org.spine3.test.aggregate.ProjectDefinition;
import org.spine3.test.aggregate.ProjectDefinitionValidatingBuilder;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.ProjectLifecycle;
import org.spine3.test.aggregate.ProjectLifecycleValidatingBuilder;
import org.spine3.test.aggregate.Status;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.command.StartProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.ProjectStarted;

public class AggregateRootTestEnv {

    private AggregateRootTestEnv() {
    }

    public static class AnAggregateRoot extends AggregateRoot<String> {
        protected AnAggregateRoot(BoundedContext boundedContext, String id) {
            super(boundedContext, id);
        }
    }

    @SuppressWarnings("TypeMayBeWeakened") // Exact message classes without OrBuilder are needed.
    public static class ProjectDefinitionPart extends AggregatePart<ProjectId,
            ProjectDefinition,
            ProjectDefinitionValidatingBuilder,
            ProjectRoot> {

        private ProjectDefinitionPart(ProjectRoot root) {
            super(root);
        }

        @Assign
        ProjectCreated handle(CreateProject msg) {
            final ProjectCreated result = ProjectCreated.newBuilder()
                                                        .setProjectId(msg.getProjectId())
                                                        .setName(msg.getName())
                                                        .build();
            return result;
        }

        @Apply
        private void apply(ProjectCreated event) {
            getBuilder().setId(event.getProjectId())
                        .setName(event.getName());
        }
    }

    public static class ProjectDefinitionRepository
            extends AggregatePartRepository<ProjectId, ProjectDefinitionPart, ProjectRoot> {

        public ProjectDefinitionRepository() {
            super();
        }
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    // Static field in the instance method is used for the test simplification.
    public static class ProjectLifeCyclePart extends AggregatePart<ProjectId,
            ProjectLifecycle,
            ProjectLifecycleValidatingBuilder,
            ProjectRoot> {

        protected ProjectLifeCyclePart(ProjectRoot root) {
            super(root);
        }

        @Assign
        ProjectStarted handle(StartProject msg) {
            final ProjectStarted result = ProjectStarted.newBuilder()
                                                        .setProjectId(msg.getProjectId())
                                                        .build();
            return result;
        }

        @Apply
        private void apply(ProjectStarted event) {
            getBuilder().setStatus(Status.STARTED);
        }
    }

    public static class ProjectLifeCycleRepository
            extends AggregatePartRepository<ProjectId, ProjectLifeCyclePart, ProjectRoot> {

        public ProjectLifeCycleRepository() {
            super();
        }
    }

    /**
     * Created by sanders on 6/2/17.
     */
    public static class ProjectRoot extends AggregateRoot<ProjectId> {

        public ProjectRoot(BoundedContext boundedContext, ProjectId id) {
            super(boundedContext, id);
        }
    }
}
