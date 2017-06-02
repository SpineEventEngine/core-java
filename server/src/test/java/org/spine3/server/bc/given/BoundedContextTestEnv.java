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

package org.spine3.server.bc.given;

import com.google.common.collect.Lists;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import org.spine3.annotation.Subscribe;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;
import org.spine3.server.event.EventSubscriber;
import org.spine3.server.procman.CommandRouted;
import org.spine3.server.procman.ProcessManager;
import org.spine3.server.procman.ProcessManagerRepository;
import org.spine3.server.projection.Projection;
import org.spine3.server.projection.ProjectionRepository;
import org.spine3.test.bc.Project;
import org.spine3.test.bc.ProjectId;
import org.spine3.test.bc.ProjectValidatingBuilder;
import org.spine3.test.bc.SecretProject;
import org.spine3.test.bc.SecretProjectValidatingBuilder;
import org.spine3.test.bc.command.AddTask;
import org.spine3.test.bc.command.CreateProject;
import org.spine3.test.bc.command.StartProject;
import org.spine3.test.bc.event.ProjectCreated;
import org.spine3.test.bc.event.ProjectStarted;
import org.spine3.test.bc.event.TaskAdded;
import org.spine3.validate.EmptyValidatingBuilder;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class BoundedContextTestEnv {

    private BoundedContextTestEnv() {}

    @SuppressWarnings({"unused", "TypeMayBeWeakened"})
    public static class ProjectAggregate
            extends Aggregate<ProjectId, Project, ProjectValidatingBuilder> {

        private boolean isCreateProjectCommandHandled = false;
        private boolean isAddTaskCommandHandled = false;
        private boolean isStartProjectCommandHandled = false;

        private boolean isProjectCreatedEventApplied = false;
        private boolean isTaskAddedEventApplied = false;
        private boolean isProjectStartedEventApplied = false;

        private ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            isCreateProjectCommandHandled = true;
            return Given.EventMessage.projectCreated(cmd.getProjectId());
        }

        @Assign
        public TaskAdded handle(AddTask cmd, CommandContext ctx) {
            isAddTaskCommandHandled = true;
            return Given.EventMessage.taskAdded(cmd.getProjectId());
        }

        @Assign
        public List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            isStartProjectCommandHandled = true;
            final ProjectStarted message = Given.EventMessage.projectStarted(cmd.getProjectId());
            return newArrayList(message);
        }

        @Apply
        private void event(ProjectCreated event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.CREATED);

            isProjectCreatedEventApplied = true;
        }

        @Apply
        private void event(TaskAdded event) {
            isTaskAddedEventApplied = true;
        }

        @Apply
        private void event(ProjectStarted event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.STARTED)
                    .build();

            isProjectStartedEventApplied = true;
        }
    }

    public static class ProjectAggregateRepository
            extends AggregateRepository<ProjectId, ProjectAggregate> {
        public ProjectAggregateRepository() {
            super();
        }
    }

    @SuppressWarnings("UnusedParameters") // It is intended in this empty handler class.
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
            extends Aggregate<String, SecretProject, SecretProjectValidatingBuilder> {
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
        public SecretProjectRepository() {
            super();
        }
    }

    public static class ProjectProcessManager
            extends ProcessManager<ProjectId, Empty, EmptyValidatingBuilder> {

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

        public ProjectPmRepo() {
            super();
        }
    }

    public static class ProjectReport
            extends Projection<ProjectId, Empty, EmptyValidatingBuilder> {

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
        public ProjectReportRepository() {
            super();
        }
    }

    public static class AnotherProjectAggregate
            extends Aggregate<ProjectId, Project, ProjectValidatingBuilder> {
        protected AnotherProjectAggregate(ProjectId id) {
            super(id);
        }
    }

    public static class AnotherProjectAggregateRepository
            extends AggregateRepository<ProjectId, AnotherProjectAggregate> {
        public AnotherProjectAggregateRepository() {
            super();
        }
    }
}
