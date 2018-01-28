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

package io.spine.server.event.given;

import com.google.common.collect.ImmutableList;
import io.grpc.stub.StreamObserver;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.React;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.route.EventRoute;
import io.spine.test.event.Project;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectId;
import io.spine.test.event.ProjectVBuilder;
import io.spine.test.event.Task;
import io.spine.test.event.TaskAdded;
import io.spine.test.event.Team;
import io.spine.test.event.TeamId;
import io.spine.test.event.TeamProjectAdded;
import io.spine.test.event.TeamVBuilder;
import io.spine.test.event.command.AddTasks;
import io.spine.test.event.command.CreateProject;
import io.spine.testdata.Sample;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singleton;
import static org.junit.Assert.fail;

/**
 * @author Mykhailo Drachuk
 */
public class EventRootCommandIdTestEnv {

    private EventRootCommandIdTestEnv() {
        // Prevent instantiation.
    }

    public static ProjectId projectId() {
        return (ProjectId) Sample.builderForType(ProjectId.class)
                                 .build();
    }

    public static TeamId teamId() {
        return (TeamId) Sample.builderForType(TeamId.class)
                              .build();
    }

    public static CreateProject createProject(ProjectId projectId, TeamId teamId) {
        checkNotNull(projectId);
        checkNotNull(teamId);

        return CreateProject.newBuilder()
                            .setProjectId(projectId)
                            .setTeamId(teamId)
                            .build();
    }

    public static CreateProject createProject(ProjectId id) {
        return createProject(id, teamId());
    }

    public static AddTasks addTasks(ProjectId id, int count) {
        checkNotNull(id);

        final AddTasks.Builder builder = AddTasks.newBuilder();
        for (int i = 0; i < count; i++) {
            final Task task = Task.getDefaultInstance();
            builder.addTask(task);
        }
        final AddTasks command = builder.setProjectId(id)
                                        .build();
        return command;
    }

    public static EventStreamQuery newStreamQuery() {
        return EventStreamQuery.newBuilder()
                               .build();
    }

    public static ResponseObserver newStreamObserver() {
        return new ResponseObserver();
    }

    /**
     * A stream observer for store querying.
     */
    public static class ResponseObserver implements StreamObserver<Event> {

        private final Collection<Event> resultStorage;

        private ResponseObserver() {
            this.resultStorage = newArrayList();
        }

        @Override
        public void onNext(Event value) {
            resultStorage.add(value);
        }

        @Override
        public void onError(Throwable t) {
            fail(t.getMessage());
        }

        @Override
        public void onCompleted() {
            // Do nothing.
        }

        public List<Event> getResults() {
            return ImmutableList.copyOf(resultStorage);
        }
    }

    public static class ProjectAggregateRepository
            extends AggregateRepository<ProjectId, ProjectAggregate> { }

    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    public static class TeamAggregateRepository
            extends AggregateRepository<TeamId, TeamAggregate> {
        public TeamAggregateRepository() {
            getEventRouting()
                    .route(ProjectCreated.class,
                           new EventRoute<TeamId, ProjectCreated>() {
                               private static final long serialVersionUID = 0L;

                               @Override
                               public Set<TeamId> apply(ProjectCreated msg, EventContext ctx) {
                                   return singleton(msg.getTeamId());
                               }
                           });
        }
    }

    static class ProjectAggregate extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        private ProjectAggregate(ProjectId id) {
            super(id);
        }

        private static ProjectCreated projectCreated(ProjectId projectId) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(projectId)
                                 .build();
        }

        private static TaskAdded taskAdded(ProjectId projectId, Task task) {
            return TaskAdded.newBuilder()
                            .setProjectId(projectId)
                            .setTask(task)
                            .build();
        }

        @Assign
        ProjectCreated on(CreateProject cmd, CommandContext ctx) {
            final ProjectCreated event = projectCreated(cmd.getProjectId());
            return event;
        }

        @Assign
        List<TaskAdded> on(AddTasks cmd, CommandContext ctx) {
            final List<Task> tasks = cmd.getTaskList();
            final ImmutableList.Builder<TaskAdded> events = ImmutableList.builder();

            for (Task task : tasks) {
                final TaskAdded event = taskAdded(cmd.getProjectId(), task);
                events.add(event);
            }

            return events.build();
        }

        @Apply
        private void event(ProjectCreated event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.CREATED);
        }

        @Apply
        private void event(TaskAdded event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .addTask(event.getTask());
        }
    }

    static class TeamAggregate extends Aggregate<TeamId, Team, TeamVBuilder> {

        private TeamAggregate(TeamId id) {
            super(id);
        }

        @React
        Iterable<TeamProjectAdded> on(ProjectCreated cmd, EventContext ctx) {
            final TeamProjectAdded event = TeamProjectAdded.newBuilder()
                                                           .setProjectId(cmd.getProjectId())
                                                           .build();
            return singleton(event);
        }

        @Apply
        private void event(TeamProjectAdded event) {
            getBuilder()
                    .setId(event.getTeamId())
                    .addProjectId(event.getProjectId());
        }
    }
}
