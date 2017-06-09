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

package io.spine.server.bc.given;

import com.google.common.collect.Lists;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.annotation.Subscribe;
import io.spine.base.CommandContext;
import io.spine.base.EventContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.EventSubscriber;
import io.spine.server.procman.CommandRouted;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.test.bc.Project;
import io.spine.test.bc.ProjectId;
import io.spine.test.bc.ProjectVBuilder;
import io.spine.test.bc.SecretProject;
import io.spine.test.bc.SecretProjectVBuilder;
import io.spine.test.bc.command.AddTask;
import io.spine.test.bc.command.CreateProject;
import io.spine.test.bc.command.StartProject;
import io.spine.test.bc.event.ProjectCreated;
import io.spine.test.bc.event.ProjectStarted;
import io.spine.test.bc.event.TaskAdded;
import io.spine.validate.EmptyVBuilder;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class BoundedContextTestEnv {

    private BoundedContextTestEnv() {}

    public static class ProjectAggregate
            extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        private ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            return Given.EventMessage.projectCreated(cmd.getProjectId());
        }

        @Assign
        public TaskAdded handle(AddTask cmd, CommandContext ctx) {
            return Given.EventMessage.taskAdded(cmd.getProjectId());
        }

        @Assign
        public List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            final ProjectStarted message = Given.EventMessage.projectStarted(cmd.getProjectId());
            return newArrayList(message);
        }

        @Apply
        private void event(ProjectCreated event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.CREATED);
        }

        @Apply
        private void event(TaskAdded event) {
            // NOP
        }

        @Apply
        private void event(ProjectStarted event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.STARTED)
                    .build();
        }
    }

    public static class ProjectAggregateRepository
                  extends AggregateRepository<ProjectId, ProjectAggregate> {
    }

    public static class TestEventSubscriber extends EventSubscriber {

        private Message handledEvent;

        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            this.handledEvent = event;
        }

        @Subscribe
        public void on(TaskAdded event, EventContext context) {
        }

        @Subscribe
        public void on(ProjectStarted event, EventContext context) {
        }

        public Message getHandledEvent() {
            return handledEvent;
        }
    }

    public static class SecretProjectAggregate
            extends Aggregate<String, SecretProject, SecretProjectVBuilder> {
        private SecretProjectAggregate(String id) {
            super(id);
        }

        @Assign
        public List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            return Lists.newArrayList();
        }
    }

    public static class SecretProjectRepository
            extends AggregateRepository<String, SecretProjectAggregate> {
    }

    public static class ProjectProcessManager
            extends ProcessManager<ProjectId, Empty, EmptyVBuilder> {

        // a ProcessManager constructor must be public because it is used via reflection
        @SuppressWarnings("PublicConstructorInNonPublicClass")
        public ProjectProcessManager(ProjectId id) {
            super(id);
        }

        @Assign
        @SuppressWarnings({"UnusedParameters", "unused"}) // OK for test method
        public CommandRouted handle(CreateProject command, CommandContext ctx) {
            return CommandRouted.getDefaultInstance();
        }

        @SuppressWarnings("UnusedParameters") // OK for test method
        @Subscribe
        public void on(ProjectCreated event, EventContext ctx) {
            // Do nothing, just watch.
        }
    }

    public static class ProjectPmRepo
            extends ProcessManagerRepository<ProjectId, ProjectProcessManager, Empty> {
    }

    public static class ProjectReport
            extends Projection<ProjectId, Empty, EmptyVBuilder> {

        @SuppressWarnings("PublicConstructorInNonPublicClass")
        // Public constructor is a part of projection public API. It's called by a repository.
        public ProjectReport(ProjectId id) {
            super(id);
        }

        @SuppressWarnings("UnusedParameters") // OK for test method.
        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            // Do nothing. We have the method so that there's one event class exposed
            // by the repository.
        }
    }

    public static class ProjectReportRepository
            extends ProjectionRepository<ProjectId, ProjectReport, Empty> {
    }

    public static class AnotherProjectAggregate
            extends Aggregate<ProjectId, Project, ProjectVBuilder> {
        protected AnotherProjectAggregate(ProjectId id) {
            super(id);
        }
    }

    public static class AnotherProjectAggregateRepository
            extends AggregateRepository<ProjectId, AnotherProjectAggregate> {
    }
}
