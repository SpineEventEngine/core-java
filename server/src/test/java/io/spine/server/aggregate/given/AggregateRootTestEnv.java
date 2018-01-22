/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.aggregate.given;

import io.spine.server.BoundedContext;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.AggregatePartRepository;
import io.spine.server.aggregate.AggregateRoot;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.aggregate.ProjectDefinition;
import io.spine.test.aggregate.ProjectDefinitionVBuilder;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectLifecycle;
import io.spine.test.aggregate.ProjectLifecycleVBuilder;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggProjectStarted;

public class AggregateRootTestEnv {

    private AggregateRootTestEnv() {
    }

    public static class AnAggregateRoot extends AggregateRoot<String> {
        protected AnAggregateRoot(BoundedContext boundedContext, String id) {
            super(boundedContext, id);
        }
    }

    static class ProjectDefinitionPart extends AggregatePart<ProjectId,
            ProjectDefinition,
            ProjectDefinitionVBuilder,
            ProjectRoot> {

        private ProjectDefinitionPart(ProjectRoot root) {
            super(root);
        }

        @Assign
        AggProjectCreated handle(AggCreateProject msg) {
            final AggProjectCreated result = AggProjectCreated.newBuilder()
                                                              .setProjectId(msg.getProjectId())
                                                              .setName(msg.getName())
                                                              .build();
            return result;
        }

        @Apply
        private void apply(AggProjectCreated event) {
            getBuilder().setId(event.getProjectId())
                        .setName(event.getName());
        }
    }

    public static class ProjectDefinitionRepository
            extends AggregatePartRepository<ProjectId, ProjectDefinitionPart, ProjectRoot> {
    }

    static class ProjectLifeCyclePart extends AggregatePart<ProjectId,
            ProjectLifecycle,
            ProjectLifecycleVBuilder,
            ProjectRoot> {

        protected ProjectLifeCyclePart(ProjectRoot root) {
            super(root);
        }

        @Assign
        AggProjectStarted handle(AggStartProject msg) {
            final AggProjectStarted result = AggProjectStarted.newBuilder()
                                                        .setProjectId(msg.getProjectId())
                                                        .build();
            return result;
        }

        @Apply
        private void apply(AggProjectStarted event) {
            getBuilder().setStatus(Status.STARTED);
        }
    }

    public static class ProjectLifeCycleRepository
            extends AggregatePartRepository<ProjectId, ProjectLifeCyclePart, ProjectRoot> {
    }

    public static class ProjectRoot extends AggregateRoot<ProjectId> {

        public ProjectRoot(BoundedContext boundedContext, ProjectId id) {
            super(boundedContext, id);
        }
    }
}
